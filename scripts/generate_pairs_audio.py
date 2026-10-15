"""
scripts/generate_pairs_audio.py

Generates audio for SELECT_PAIRS exercises:
1. Exercise prompt audio (ex_<id>.m4a) in app/src/main/assets/audio/<lang>/
2. Interactive pair tiles audio (w_<slug>.m4a) in app/src/main/assets/audio/words/
3. Automatically updates generated_manifest.json and words_manifest.json
"""

import os
import sys
import argparse
import time
import shutil

# Ensure project root is in sys.path
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

from audio_generator.config import (
    BASE_AUDIO_DIR,
    WORDS_AUDIO_DIR,
    WORDS_MANIFEST_PATH,
    CURRICULUM_FILES,
    GCP_VOICES,
    EXERCISE_VOICES,
    DEFAULT_API_KEYS,
    load_vertex_credentials,
)
from audio_generator.curriculum_loader import (
    load_curriculum_lessons,
    get_exercise_prompt,
    normalize_word_token,
    get_word_slug,
    load_words_manifest,
    save_words_manifest,
    is_audio_already_present,
)
from audio_generator.gcp_tts_client import (
    VertexAuthManager,
    synthesize_with_gcp,
)
from audio_generator.gemini_client import synthesize_with_failover
from audio_generator.audio_processing import encode_pcm_to_aac
from audio_generator.pipeline import (
    load_manifest,
    save_manifest,
    process_single_audio_item,
    is_spanish_text,
)


def synthesize_word_item(
    token: str,
    output_dir: str,
    words_manifest: dict,
    manifest_path: str,
    args: argparse.Namespace,
    requests_state: dict,
    vertex_auth: VertexAuthManager | None = None,
) -> bool:
    """Synthesizes an individual word/phrase clip for pair tiles into audio/words/."""
    if not token or not token.strip():
        return True

    slug = get_word_slug(token)
    out_file = os.path.join(output_dir, f"{slug}.m4a")
    text_cache = requests_state.setdefault("text_cache", {})

    # Already present check
    if (token in words_manifest or is_audio_already_present(out_file)) and os.path.exists(out_file):
        words_manifest[token] = f"words/{slug}.m4a"
        text_cache[token] = out_file
        print(f"    [ALREADY EXISTS] Tile '{token}' -> words/{slug}.m4a", flush=True)
        return True

    # Dedup check
    cached_src = text_cache.get(token)
    if cached_src and os.path.exists(cached_src):
        shutil.copy2(cached_src, out_file)
        words_manifest[token] = f"words/{slug}.m4a"
        requests_state["saved"] = requests_state.get("saved", 0) + 1
        print(f"    [DEDUP] Tile '{token}' -> copied from {os.path.basename(cached_src)} (0 API calls)", flush=True)
        return True

    if requests_state["made"] >= args.limit:
        return False

    word_lang = "es" if is_spanish_text(token) else "en"
    voice_cfg = GCP_VOICES.get(word_lang, GCP_VOICES["en"])
    print(f"    Synthesizing Tile ({word_lang}): '{token}' -> words/{slug}.m4a", flush=True)

    try:
        if vertex_auth and args.engine in ("auto", "gcp"):
            raw_pcm = synthesize_with_gcp(
                text=token,
                lang=word_lang,
                voice_name=voice_cfg["primary"],
                vertex_auth=vertex_auth,
            )
        else:
            raw_pcm = synthesize_with_failover(
                text=token,
                api_key=args.api_key,
                voice_name=EXERCISE_VOICES[0],
                fallback_voice="Erinome",
                requests_state=requests_state,
                delay=args.delay,
                context_desc=f"Pair tile '{token}' ({word_lang})",
                vertex_auth=vertex_auth,
            )

        encode_pcm_to_aac(raw_pcm, out_file)
        words_manifest[token] = f"words/{slug}.m4a"
        save_words_manifest(manifest_path, words_manifest)
        text_cache[token] = out_file
        requests_state["made"] += 1
        file_size_kb = os.path.getsize(out_file) / 1024.0
        print(f"      -> Saved words/{slug}.m4a ({file_size_kb:.1f} KB AAC)", flush=True)
        time.sleep(args.delay)
        return True
    except Exception as e:
        print(f"    -> Error generating tile '{token}': {e}", flush=True)
        return False


def run_pairs_generator(args: argparse.Namespace):
    # Initialize Auth
    vertex_auth = None
    try:
        creds_dict = load_vertex_credentials(getattr(args, "vertex_creds", None))
        vertex_auth = VertexAuthManager(creds_dict)
    except Exception as e:
        if getattr(args, "vertex", False):
            print(f"[FATAL] Failed to initialize Vertex AI: {e}")
            return

    if getattr(args, "api_key", None) is None:
        args.api_key = DEFAULT_API_KEYS

    target_languages = ["es", "en"] if args.lang == "all" else [args.lang]

    requests_state = {
        "made": 0,
        "saved": 0,
        "model_idx": 0,
        "cooldowns": {},
    }

    words_manifest = load_words_manifest(WORDS_MANIFEST_PATH)
    os.makedirs(WORDS_AUDIO_DIR, exist_ok=True)

    print("======================================================", flush=True)
    print(" GrammarAI - Select Pairs Audio Generator", flush=True)
    print(f" Target Language(s): {args.lang}", flush=True)
    print(f" Max Lessons:        {args.max_lessons if args.max_lessons > 0 else 'All'}", flush=True)
    print(f" Module Filter:      {args.module or 'None'}", flush=True)
    print(f" TTS Engine Mode:    {args.engine.upper()}", flush=True)
    print("======================================================", flush=True)

    for lang in target_languages:
        curriculum_path = CURRICULUM_FILES.get(lang)
        if not curriculum_path or not os.path.exists(curriculum_path):
            print(f"\n[Language: {lang}] Curriculum file not found: {curriculum_path}")
            continue

        lang_output_dir = os.path.join(BASE_AUDIO_DIR, lang)
        manifest_path = os.path.join(lang_output_dir, "generated_manifest.json")
        lang_manifest = load_manifest(manifest_path)
        os.makedirs(lang_output_dir, exist_ok=True)

        lessons = load_curriculum_lessons(curriculum_path)
        if args.module is not None:
            lessons = [l for l in lessons if l.get("module") == args.module]
        if args.max_lessons > 0:
            lessons = lessons[:args.max_lessons]

        print(f"\n--- [Language: {lang.upper()}] Scanning first {len(lessons)} lesson(s) ---", flush=True)

        pair_exercises = []
        for l_idx, lesson in enumerate(lessons, 1):
            for ex in lesson.get("exercises", []):
                if ex.get("type", "").upper() == "SELECT_PAIRS":
                    pair_exercises.append((l_idx, lesson.get("title", ""), ex))

        if not pair_exercises:
            print(f"  [INFO] No SELECT_PAIRS exercises found in the first {len(lessons)} lessons of '{lang}'.", flush=True)
            continue

        print(f"  Found {len(pair_exercises)} SELECT_PAIRS exercise(s):", flush=True)

        for l_idx, lesson_title, ex in pair_exercises:
            ex_id = ex.get("id")
            pairs = ex.get("pairs", [])
            print(f"\n  * Lesson {l_idx}: '{lesson_title}' | Exercise: {ex_id} ({len(pairs)} pairs)", flush=True)

            # 1. Synthesize Prompt Audio (ex_<id>.m4a)
            prompt_text = get_exercise_prompt(ex)
            if prompt_text:
                process_single_audio_item(
                    item_id=ex_id,
                    text=prompt_text,
                    lang=lang,
                    output_dir=lang_output_dir,
                    manifest=lang_manifest,
                    manifest_path=manifest_path,
                    args=args,
                    requests_state=requests_state,
                    vertex_auth=vertex_auth,
                    custom_desc=f"{lang}/{ex_id} Prompt"
                )

            # 2. Synthesize Pair Tiles (Left & Right)
            for pair in pairs:
                left_raw = pair.get("left", "").strip()
                right_raw = pair.get("right", "").strip()
                is_alphabet_pair = len(left_raw) == 1 and left_raw.isalpha() and ("/" in right_raw or "(" in right_raw)

                if is_alphabet_pair:
                    # Synthesize left letter tile
                    left_token = normalize_word_token(left_raw)
                    if left_token:
                        synthesize_word_item(
                            token=left_token,
                            output_dir=WORDS_AUDIO_DIR,
                            words_manifest=words_manifest,
                            manifest_path=WORDS_MANIFEST_PATH,
                            args=args,
                            requests_state=requests_state,
                            vertex_auth=vertex_auth,
                        )
                        # Alias right phonetic tile to the left letter audio (a -> a at audio level)
                        left_clip = words_manifest.get(left_token)
                        if left_clip:
                            words_manifest[right_raw] = left_clip
                            words_manifest[normalize_word_token(right_raw)] = left_clip
                            print(f"    [ALIAS] Phonetic tile '{right_raw}' -> {left_clip}", flush=True)
                    continue

                for side in ("left", "right"):
                    raw_text = pair.get(side, "")
                    token = normalize_word_token(raw_text)
                    if not token:
                        continue

                    synthesize_word_item(
                        token=token,
                        output_dir=WORDS_AUDIO_DIR,
                        words_manifest=words_manifest,
                        manifest_path=WORDS_MANIFEST_PATH,
                        args=args,
                        requests_state=requests_state,
                        vertex_auth=vertex_auth,
                    )

                    if requests_state["made"] >= args.limit:
                        print(f"\nBatch limit of {args.limit} reached. Stopping.", flush=True)
                        save_manifest(manifest_path, lang_manifest)
                        save_words_manifest(WORDS_MANIFEST_PATH, words_manifest)
                        return

        save_manifest(manifest_path, lang_manifest)

    save_words_manifest(WORDS_MANIFEST_PATH, words_manifest)
    print("\n======================================================", flush=True)
    print(f"Finished! New audio requests made: {requests_state['made']}", flush=True)
    print(f"Requests saved via deduplication:   {requests_state['saved']}", flush=True)
    print(f"Total entries in words_manifest:    {len(words_manifest)}", flush=True)
    print("======================================================", flush=True)


def main():
    parser = argparse.ArgumentParser(description="Generate audio clips for SELECT_PAIRS exercises")
    parser.add_argument("--lang", default="all", choices=["es", "en", "all"], help="Language: es, en, or all")
    parser.add_argument("--max-lessons", type=int, default=2, help="Number of initial lessons to process (default: 2)")
    parser.add_argument("--module", type=int, default=None, help="Filter by module number")
    parser.add_argument("--engine", default="auto", choices=["auto", "gcp", "gemini"], help="TTS Engine")
    parser.add_argument("--limit", type=int, default=100, help="Max requests limit")
    parser.add_argument("--delay", type=float, default=0.5, help="Delay between requests")
    parser.add_argument("--api-key", default=None, help="Optional Gemini API key")
    parser.add_argument("--vertex-creds", default=None, help="Optional Vertex AI creds")
    args = parser.parse_args()

    run_pairs_generator(args)


if __name__ == "__main__":
    main()

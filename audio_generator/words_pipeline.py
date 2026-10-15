"""
audio_generator.words_pipeline
Pipeline for generating the unified vocabulary dictionary audio files and manifest.
"""

import os
import time
import shutil
import argparse
from .config import (
    WORDS_AUDIO_DIR,
    WORDS_MANIFEST_PATH,
    GCP_VOICES,
    EXERCISE_VOICES,
    is_spanish_text,
    get_curriculum_files
)
from .curriculum_loader import (
    extract_unique_vocabulary_words,
    get_word_slug,
    load_words_manifest,
    save_words_manifest,
    is_audio_already_present
)
from .gemini_client import synthesize_with_failover, VertexAuthManager
from .gcp_tts_client import synthesize_with_gcp
from .audio_processing import encode_pcm_to_aac


def process_scrambled_words(
    args: argparse.Namespace,
    requests_state: dict,
    vertex_auth: VertexAuthManager | None = None
):
    """
    Extracts all unique words across scrambled sentence, select_pairs, and flashcards in curricula,
    synthesizes each word via Google Cloud TTS (en-US-Journey-F or es-US-Neural2-A),
    and records them in words_manifest.json.
    """
    print("\n======================================================", flush=True)
    print(" Processing Unified Vocabulary Audio Dictionary...", flush=True)
    os.makedirs(WORDS_AUDIO_DIR, exist_ok=True)
    words_manifest = load_words_manifest(WORDS_MANIFEST_PATH)

    curriculum_source = getattr(args, "curriculum", "lessons")
    all_curriculum_files = []
    for lang in ("es", "en"):
        all_curriculum_files.extend(get_curriculum_files(lang, curriculum_source))
    # Deduplicate while preserving order
    seen_files = set()
    unique_curriculum_files = []
    for f in all_curriculum_files:
        if f not in seen_files and os.path.exists(f):
            seen_files.add(f)
            unique_curriculum_files.append(f)

    is_pre_a1_mode = getattr(args, "category", "") == "pre_a1"
    unique_words = extract_unique_vocabulary_words(unique_curriculum_files, filter_pre_a1=is_pre_a1_mode)
    print(f" Total Unique Vocabulary in Curriculum ({curriculum_source}): {len(unique_words)}", flush=True)
    print(f" Words Already in Manifest:              {len(words_manifest)}", flush=True)
    print("======================================================", flush=True)

    text_cache = requests_state.setdefault("text_cache", {})

    for idx, item in enumerate(unique_words, 1):
        if isinstance(item, (list, tuple)):
            word, detected_lang = item
        else:
            word, detected_lang = item, None

        if not word or not word.strip():
            continue

        word_lang = detected_lang or ("es" if is_spanish_text(word) else "en")
        slug = get_word_slug(word)
        out_file = os.path.join(WORDS_AUDIO_DIR, f"{slug}.m4a")

        if requests_state["made"] >= args.limit:
            print(f"\nBatch limit of {args.limit} requests reached. Stopping words batch cleanly.", flush=True)
            save_words_manifest(WORDS_MANIFEST_PATH, words_manifest)
            return

        if (word in words_manifest or is_audio_already_present(out_file)) and os.path.exists(out_file):
            words_manifest[word] = f"words/{slug}.m4a"
            text_cache[word] = out_file
            continue

        cached_src = text_cache.get(word)
        if cached_src and os.path.exists(cached_src):
            shutil.copy2(cached_src, out_file)
            words_manifest[word] = f"words/{slug}.m4a"
            requests_state["saved"] = requests_state.get("saved", 0) + 1
            print(f"  [{idx}/{len(unique_words)}] [DEDUP] '{word}' -> copied from {os.path.basename(cached_src)}", flush=True)
            continue

        voice_cfg = GCP_VOICES.get(word_lang, GCP_VOICES["en"])
        print(f"  [{idx}/{len(unique_words)}] Synthesizing word ({word_lang}): '{word}' -> {slug}.m4a", flush=True)
        try:
            if vertex_auth and args.engine in ("auto", "gcp"):
                raw_pcm = synthesize_with_gcp(
                    text=word,
                    lang=word_lang,
                    voice_name=voice_cfg["primary"],
                    vertex_auth=vertex_auth
                )
            else:
                raw_pcm = synthesize_with_failover(
                    text=word,
                    api_key=args.api_key,
                    voice_name=EXERCISE_VOICES[0],
                    fallback_voice="Erinome",
                    requests_state=requests_state,
                    delay=args.delay,
                    context_desc=f"Word '{word}' ({word_lang})",
                    vertex_auth=vertex_auth
                )

            encode_pcm_to_aac(raw_pcm, out_file)
            words_manifest[word] = f"words/{slug}.m4a"
            save_words_manifest(WORDS_MANIFEST_PATH, words_manifest)
            text_cache[word] = out_file
            requests_state["made"] += 1
            file_size_kb = os.path.getsize(out_file) / 1024.0
            print(f"      -> Saved words/{slug}.m4a ({file_size_kb:.1f} KB AAC)", flush=True)
            time.sleep(args.delay)
        except Exception as e:
            print(f"    -> Error generating audio for word '{word}': {e}", flush=True)
            save_words_manifest(WORDS_MANIFEST_PATH, words_manifest)
            break

    save_words_manifest(WORDS_MANIFEST_PATH, words_manifest)
    print(f"\nWord dictionary updated: {len(words_manifest)} total words in {WORDS_MANIFEST_PATH}", flush=True)

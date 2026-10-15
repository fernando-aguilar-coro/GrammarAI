"""
audio_generator.pipeline
Orchestrator for batch audio generation across all curricula and categories.
"""

import os
import argparse
from .config import (
    BASE_AUDIO_DIR,
    DEFAULT_VERTEX_LOCATION,
    GCP_VOICES,
    DEFAULT_API_KEYS,
    load_vertex_credentials,
    get_curriculum_files
)
from .curriculum_loader import load_manifest, load_all_manifests_for_lang
from .gemini_client import VertexAuthManager
from .words_pipeline import process_scrambled_words
from .prompts_pipeline import process_generic_prompts, process_onboarding
from .exercises_pipeline import process_exercises
from .theory_pipeline import process_theory


def process_target_language(
    lang: str,
    categories: list[str],
    args: argparse.Namespace,
    requests_state: dict,
    vertex_auth: VertexAuthManager | None = None
):
    """Processes all selected audio categories for a target language."""
    output_dir = os.path.join(BASE_AUDIO_DIR, lang)
    os.makedirs(output_dir, exist_ok=True)
    manifest_path = os.path.join(output_dir, "generated_manifest.json")
    manifest = load_all_manifests_for_lang(lang)
    curriculum_source = getattr(args, "curriculum", "lessons")
    curriculum_files = get_curriculum_files(lang, curriculum_source)

    print(f"\n======================================================", flush=True)
    print(f" Target Language:   [{lang.upper()}]", flush=True)
    print(f" Output Folder:     {output_dir}", flush=True)
    print(f" Curriculum Source: {curriculum_source} ({len(curriculum_files)} file(s))", flush=True)
    for cf in curriculum_files:
        print(f"   -> {cf}", flush=True)
    print(f" Manifest:          {len(manifest)} clips already recorded", flush=True)
    print(f"======================================================", flush=True)

    if "generic_prompts" in categories or "all" in categories:
        process_generic_prompts(lang, output_dir, manifest, manifest_path, args, requests_state, vertex_auth=vertex_auth)
        if requests_state["made"] >= args.limit:
            return

    if "onboarding" in categories or "all" in categories:
        process_onboarding(lang, output_dir, manifest, manifest_path, args, requests_state, vertex_auth=vertex_auth)
        if requests_state["made"] >= args.limit:
            return

    if "exercises" in categories or "all" in categories:
        for c_path in curriculum_files:
            if requests_state["made"] >= args.limit:
                break
            process_exercises(lang, output_dir, manifest, manifest_path, c_path, args, requests_state, vertex_auth=vertex_auth)
        if requests_state["made"] >= args.limit:
            return

    if "theory" in categories or "all" in categories:
        for c_path in curriculum_files:
            if requests_state["made"] >= args.limit:
                break
            process_theory(lang, output_dir, manifest, manifest_path, c_path, args, requests_state, vertex_auth=vertex_auth)
        if requests_state["made"] >= args.limit:
            return


def run_batch(args: argparse.Namespace):
    """Initializes auth, configuration, and runs requested batch jobs."""
    vertex_auth = None
    try:
        creds_dict = load_vertex_credentials(getattr(args, "vertex_creds", None))
        loc = getattr(args, "vertex_location", DEFAULT_VERTEX_LOCATION)
        vertex_auth = VertexAuthManager(creds_dict, location=loc)
    except Exception as e:
        if getattr(args, "vertex", False):
            print(f"\n[FATAL] Failed to initialize Vertex AI: {e}\n", flush=True)
            return

    if getattr(args, "api_key", None) is None:
        args.api_key = DEFAULT_API_KEYS

    active_keys = args.api_key if isinstance(args.api_key, list) else [k.strip() for k in str(args.api_key).split(",") if k.strip()]
    masked_keys = [f"...{k[-6:]}" if len(k) > 6 else k for k in active_keys]

    print("======================================================", flush=True)
    print(" GrammarAI - Hybrid Audio Generation Pipeline", flush=True)
    print(f" Base Audio Directory: {BASE_AUDIO_DIR}", flush=True)
    if getattr(args, "vertex", False) and vertex_auth:
        print(f" Gemini TTS/LLM:       Google Cloud Vertex AI (Project: {vertex_auth.project_id})", flush=True)
    else:
        print(f" Gemini TTS/LLM:       Google AI Studio ({len(active_keys)} key(s) rotating)", flush=True)
        print(f" Active API Keys:      {', '.join(masked_keys)}", flush=True)

    if vertex_auth:
        print(f" GCP Cloud TTS:        Active (Project: {vertex_auth.project_id})", flush=True)
        print(f" GCP English Voice:    {GCP_VOICES['en']['primary']}", flush=True)
        print(f" GCP Spanish Voice:    {GCP_VOICES['es']['primary']}", flush=True)
    else:
        print(f" GCP Cloud TTS:        Disabled (no credentials)", flush=True)
    print(f" TTS Engine Mode:      {args.engine.upper()}", flush=True)
    print(f" Curriculum Source:    {getattr(args, 'curriculum', 'lessons')}", flush=True)
    print(f" Target Language(s):   {args.lang}", flush=True)
    print(f" Target Category:      {args.category}", flush=True)
    print(f" Exercise Types Filter:{args.types}", flush=True)
    print(f" Include Options:      {getattr(args, 'include_options', True)}", flush=True)
    print(f" Correct Feedback:     {getattr(args, 'include_correct_feedback', True)}", flush=True)
    print(f" Batch Limit:          {args.limit}", flush=True)
    print("======================================================", flush=True)

    requests_state = {
        "made": 0,
        "saved": 0,
        "model_idx": 0,
        "text_model_idx": 0,
        "cooldowns": {},
        "text_cooldowns": {}
    }

    if args.category in ("words", "all", "pre_a1"):
        process_scrambled_words(args, requests_state, vertex_auth=vertex_auth)

    if args.category != "words":
        target_languages = ["es", "en"] if args.lang == "all" else [args.lang]
        if args.category == "all":
            target_categories = ["generic_prompts", "onboarding", "exercises", "theory"]
        elif args.category == "pre_a1":
            target_categories = ["exercises"]
        else:
            target_categories = [args.category]

        for lang in target_languages:
            if requests_state["made"] >= args.limit:
                break
            process_target_language(lang, target_categories, args, requests_state, vertex_auth=vertex_auth)

    print("\n======================================================", flush=True)
    print(f"Batch finished. New audio requests made: {requests_state['made']}", flush=True)
    print(f"Requests saved via deduplication: {requests_state['saved']}", flush=True)
    print("======================================================", flush=True)


def main():
    parser = argparse.ArgumentParser(description="GrammarAI Hybrid Audio Batch Generator")
    parser.add_argument("--api-key", default=None, help="Gemini API Key or comma-separated keys")
    parser.add_argument("--vertex", action="store_true", help="Force Google Cloud Vertex AI")
    parser.add_argument("--vertex-location", default=DEFAULT_VERTEX_LOCATION, help=f"Vertex AI region (default: {DEFAULT_VERTEX_LOCATION})")
    parser.add_argument("--vertex-creds", default=None, help="Base64 or JSON file path for service account credentials")
    parser.add_argument("--engine", default="auto", choices=["auto", "gcp", "gemini"], help="TTS engine: auto (GCP for exercises/words, Gemini for theory), gcp, or gemini")
    parser.add_argument("--curriculum", default="lessons", choices=["lessons", "situational", "all"], help="Curriculum source to process: lessons, situational, or all (default: lessons)")
    parser.add_argument("--lang", default="all", choices=["es", "en", "all"], help="Target languages")
    parser.add_argument("--category", default="all", choices=["generic_prompts", "onboarding", "exercises", "theory", "words", "pre_a1", "all"], help="Category")
    parser.add_argument("--types", default="all", help="Exercise types filter")
    parser.add_argument("--no-options", dest="include_options", action="store_false", help="Skip multiple choice options synthesis")
    parser.set_defaults(include_options=True)
    parser.add_argument("--no-correct-feedback", dest="include_correct_feedback", action="store_false", help="Skip correct answer feedback synthesis")
    parser.set_defaults(include_correct_feedback=True)
    parser.add_argument("--module", type=int, default=None, help="Filter lessons by module number")
    parser.add_argument("--max-lessons", type=int, default=0, help="Max lessons per language (0 for all)")
    parser.add_argument("--limit", type=int, default=100, help="Max API requests per run")
    parser.add_argument("--delay", type=float, default=0.5, help="Delay between requests in seconds")
    parser.add_argument("--force-script-regen", action="store_true", help="Force regenerating theory text script with LLM")

    args = parser.parse_args()
    run_batch(args)


if __name__ == "__main__":
    main()

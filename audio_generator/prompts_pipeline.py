"""
audio_generator.prompts_pipeline
Pipeline for generating generic instruction prompts and onboarding clips.
"""

import os
import time
import argparse
from .config import (
    GENERIC_PROMPTS,
    GCP_VOICES,
    ONBOARDING_MODEL,
    ONBOARDING_VOICE,
    ONBOARDING_FALLBACK_VOICE,
    VERTEX_ONBOARDING_MODEL
)
from .curriculum_loader import (
    get_onboarding_items,
    is_audio_already_present,
    save_manifest
)
from .gemini_client import synthesize_with_failover, VertexAuthManager
from .gcp_tts_client import synthesize_with_gcp
from .audio_processing import encode_pcm_to_aac


def process_generic_prompts(
    lang: str,
    output_dir: str,
    manifest: set[str],
    manifest_path: str,
    args: argparse.Namespace,
    requests_state: dict,
    vertex_auth: VertexAuthManager | None = None
):
    """Synthesizes generic drill instruction prompts (e.g. prompt_multiple_choice.m4a)."""
    print(f"\n[Language: {lang}] Processing Generic Prompt Audios...", flush=True)
    for item in GENERIC_PROMPTS.get(lang, []):
        item_id = item["id"]
        out_file = os.path.join(output_dir, f"{item_id}.m4a")

        if (item_id in manifest or is_audio_already_present(out_file)) and os.path.exists(out_file):
            print(f"  [ALREADY EXISTS] Generic {lang}/{item_id} -> Skipping.", flush=True)
            manifest.add(item_id)
            continue

        if requests_state["made"] >= args.limit:
            print(f"\nBatch limit of {args.limit} reached. Stopping batch.", flush=True)
            save_manifest(manifest_path, manifest)
            return

        try:
            if vertex_auth and args.engine in ("auto", "gcp"):
                raw_audio = synthesize_with_gcp(
                    text=item["text"],
                    lang=lang,
                    voice_name=GCP_VOICES.get(lang, GCP_VOICES["en"])["primary"],
                    vertex_auth=vertex_auth
                )
            else:
                gemini_vertex_auth = vertex_auth if getattr(args, "vertex", False) else None
                raw_audio = synthesize_with_failover(
                    text=item["text"],
                    api_key=args.api_key,
                    voice_name=ONBOARDING_VOICE,
                    fallback_voice=ONBOARDING_FALLBACK_VOICE,
                    requests_state=requests_state,
                    delay=args.delay,
                    context_desc=f"Generic Prompt {lang}/{item_id}",
                    vertex_auth=gemini_vertex_auth
                )
            encode_pcm_to_aac(raw_audio, out_file)
            manifest.add(item_id)
            save_manifest(manifest_path, manifest)
            requests_state["made"] += 1
            file_size_kb = os.path.getsize(out_file) / 1024.0
            print(f"      -> Saved {lang}/{item_id}.m4a ({file_size_kb:.1f} KB AAC)", flush=True)
            time.sleep(args.delay)
        except Exception as e:
            print(f"    -> Error generating generic prompt {item_id}: {e}", flush=True)
            break

    save_manifest(manifest_path, manifest)


def process_onboarding(
    lang: str,
    output_dir: str,
    manifest: set[str],
    manifest_path: str,
    args: argparse.Namespace,
    requests_state: dict,
    vertex_auth: VertexAuthManager | None = None
):
    """Synthesizes onboarding coach voice clips."""
    print(f"\n[Language: {lang}] Processing Onboarding Audios...", flush=True)
    for item in get_onboarding_items(lang):
        item_id = item["id"]
        out_file = os.path.join(output_dir, f"{item_id}.m4a")

        if (item_id in manifest or is_audio_already_present(out_file)) and os.path.exists(out_file):
            print(f"  [ALREADY EXISTS] Onboarding {lang}/{item_id} -> Skipping.", flush=True)
            manifest.add(item_id)
            continue

        if requests_state["made"] >= args.limit:
            print(f"\nBatch limit of {args.limit} reached. Stopping batch.", flush=True)
            save_manifest(manifest_path, manifest)
            return

        try:
            onboarding_vertex_auth = vertex_auth if getattr(args, "vertex", False) else None
            raw_audio = synthesize_with_failover(
                text=item["text"],
                api_key=args.api_key,
                voice_name=ONBOARDING_VOICE,
                fallback_voice=ONBOARDING_FALLBACK_VOICE,
                requests_state=requests_state,
                delay=args.delay,
                context_desc=f"Onboarding {lang}/{item_id}",
                fixed_model=VERTEX_ONBOARDING_MODEL if onboarding_vertex_auth else ONBOARDING_MODEL,
                vertex_auth=onboarding_vertex_auth
            )
            encode_pcm_to_aac(raw_audio, out_file)
            manifest.add(item_id)
            save_manifest(manifest_path, manifest)
            requests_state["made"] += 1
            file_size_kb = os.path.getsize(out_file) / 1024.0
            print(f"      -> Saved {lang}/{item_id}.m4a ({file_size_kb:.1f} KB AAC)", flush=True)
            time.sleep(args.delay)
        except Exception as e:
            print(f"    -> Error generating onboarding {item_id}: {e}", flush=True)
            break

    save_manifest(manifest_path, manifest)

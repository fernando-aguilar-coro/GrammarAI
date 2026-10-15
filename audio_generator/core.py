"""
audio_generator.core
Core reusable helpers for synthesis, deduplication, and manifest tracking.
"""

import os
import time
import shutil
import argparse
from .config import (
    EXERCISE_VOICES,
    GCP_VOICES
)
from .curriculum_loader import (
    is_audio_already_present,
    save_manifest
)
from .gemini_client import synthesize_with_failover, VertexAuthManager
from .gcp_tts_client import synthesize_with_gcp
from .audio_processing import encode_pcm_to_aac


def process_single_audio_item(
    item_id: str,
    text: str,
    lang: str,
    output_dir: str,
    manifest: set[str],
    manifest_path: str,
    args: argparse.Namespace,
    requests_state: dict,
    vertex_auth: VertexAuthManager | None = None,
    custom_desc: str = ""
) -> bool:
    """
    Helper to synthesize, deduplicate, encode, and record a single audio clip.
    Returns True if synthesized or deduplicated, False if stopped or error.
    """
    if not text or not text.strip():
        return True

    text_cache = requests_state.setdefault("text_cache", {})
    norm_key = text.strip().lower()
    out_file = os.path.join(output_dir, f"{item_id}.m4a")

    if item_id in manifest or is_audio_already_present(out_file):
        if not os.path.exists(out_file):
            from .asset_pack_router import find_audio_clip
            existing_clip = find_audio_clip(item_id, lang)
            if existing_clip and os.path.exists(existing_clip):
                os.makedirs(output_dir, exist_ok=True)
                if os.path.abspath(existing_clip) != os.path.abspath(out_file):
                    shutil.move(existing_clip, out_file)
        manifest.add(item_id)
        text_cache[norm_key] = out_file
        return True

    cached_src = text_cache.get(norm_key)
    if cached_src and os.path.exists(cached_src):
        shutil.copy2(cached_src, out_file)
        manifest.add(item_id)
        requests_state["saved"] = requests_state.get("saved", 0) + 1
        out_folder = os.path.basename(output_dir)
        print(f"    [DEDUP] {out_folder}/{item_id} copied from {os.path.basename(cached_src)} (0 API calls)", flush=True)
        return True

    if requests_state["made"] >= args.limit:
        return False

    desc = custom_desc or f"({requests_state['made']+1}/{args.limit}) {os.path.basename(output_dir)}/{item_id}"
    try:
        if vertex_auth and args.engine in ("auto", "gcp"):
            raw_audio = synthesize_with_gcp(
                text=text,
                lang=lang,
                voice_name=GCP_VOICES.get(lang, GCP_VOICES["en"])["primary"],
                vertex_auth=vertex_auth
            )
        else:
            chosen_voice = EXERCISE_VOICES[requests_state.get("model_idx", 0) % len(EXERCISE_VOICES)]
            gemini_vertex_auth = vertex_auth if getattr(args, "vertex", False) else None
            raw_audio = synthesize_with_failover(
                text=text,
                api_key=args.api_key,
                voice_name=chosen_voice,
                fallback_voice="Erinome",
                requests_state=requests_state,
                delay=args.delay,
                context_desc=desc,
                vertex_auth=gemini_vertex_auth
            )

        encode_pcm_to_aac(raw_audio, out_file)
        manifest.add(item_id)
        save_manifest(manifest_path, manifest)
        text_cache[norm_key] = out_file
        requests_state["made"] += 1
        file_size_kb = os.path.getsize(out_file) / 1024.0
        out_folder = os.path.basename(output_dir)
        print(f"      -> Saved {out_folder}/{item_id}.m4a ({file_size_kb:.1f} KB AAC)", flush=True)
        time.sleep(args.delay)
        return True
    except Exception as e:
        print(f"    -> Error generating {item_id}: {e}", flush=True)
        return False

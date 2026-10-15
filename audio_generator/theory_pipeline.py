"""
audio_generator.theory_pipeline
Pipeline for preparing and synthesizing lesson theory narration audios.
"""

import os
import time
import argparse
from .config import (
    BASE_AUDIO_DIR,
    THEORY_VOICE,
    THEORY_FALLBACK_VOICE
)
from .curriculum_loader import (
    get_theory_clip_id,
    is_audio_already_present,
    load_curriculum_lessons,
    save_manifest
)
from .gemini_client import synthesize_with_failover, VertexAuthManager
from .audio_processing import concatenate_and_encode_pcm_chunks
from .text_transformer import prepare_theory_narration_script, chunk_text_for_tts


def process_theory(
    lang: str,
    output_dir: str,
    manifest: set[str],
    manifest_path: str,
    curriculum_path: str,
    args: argparse.Namespace,
    requests_state: dict,
    vertex_auth: VertexAuthManager | None = None
):
    """
    Transforms markdown theory explanations into spoken narration scripts,
    chunks long scripts, synthesizes chunks via Gemini TTS, and concatenates
    them into a seamless theory_<lessonId>.m4a audio clip.
    """
    print(f"\n[Language: {lang}] Processing Lesson Theory & Explanation Audios (Gemini Narration)...", flush=True)
    lessons = load_curriculum_lessons(curriculum_path)
    if not lessons:
        print(f"Warning: {curriculum_path} not found or empty. Skipping theory for {lang}.", flush=True)
        save_manifest(manifest_path, manifest)
        return

    if getattr(args, "module", None) is not None:
        lessons = [l for l in lessons if l.get("module") == args.module]
        print(f"Filtered for Module {args.module}: {len(lessons)} lessons.", flush=True)

    if getattr(args, "max_lessons", 0) and args.max_lessons > 0:
        lessons = lessons[:args.max_lessons]
        print(f"Limited to first {len(lessons)} lesson(s).", flush=True)

    force_script = getattr(args, "force_script_regen", False)

    for idx, lesson in enumerate(lessons, 1):
        from .asset_pack_router import resolve_lesson_audio_dir, find_audio_clip
        import shutil

        output_dir, manifest_path = resolve_lesson_audio_dir(lesson, lang, "lessons")
        lesson_id = lesson.get("id", "")
        lesson_title = lesson.get("title", f"Lesson {idx}")
        theory_md = lesson.get("theoryMarkdown", "").strip()

        if not theory_md:
            print(f"  [NO THEORY] Lesson '{lesson_title}' ({lesson_id}) has empty theory. Skipping.", flush=True)
            continue

        clip_id = get_theory_clip_id(lesson_id)
        out_file = os.path.join(output_dir, f"{clip_id}.m4a")

        if clip_id in manifest or is_audio_already_present(out_file):
            if not os.path.exists(out_file):
                found = find_audio_clip(clip_id, lang)
                if found and os.path.exists(found):
                    os.makedirs(output_dir, exist_ok=True)
                    if os.path.abspath(found) != os.path.abspath(out_file):
                        shutil.move(found, out_file)
            print(f"  [ALREADY EXISTS] Theory {lang}/{clip_id} -> Skipping.", flush=True)
            manifest.add(clip_id)
            continue

        if requests_state["made"] >= args.limit:
            print(f"\nBatch limit of {args.limit} requests reached. Stopping batch cleanly.", flush=True)
            save_manifest(manifest_path, manifest)
            return

        print(f"\n  -- [{idx}/{len(lessons)}] Theory Narration: '{lesson_title}' ({clip_id}) --", flush=True)

        try:
            theory_vertex_auth = vertex_auth if getattr(args, "vertex", False) else None
            narration_script = prepare_theory_narration_script(
                lesson_id=lesson_id,
                lesson_title=lesson_title,
                theory_markdown=theory_md,
                lang=lang,
                api_key=args.api_key,
                requests_state=requests_state,
                base_audio_dir=BASE_AUDIO_DIR,
                force_regen=force_script,
                delay=args.delay,
                vertex_auth=theory_vertex_auth
            )

            chunks = chunk_text_for_tts(narration_script, max_chars=750)
            print(f"    [Prepared Script] {len(narration_script)} chars -> {len(chunks)} TTS chunk(s)", flush=True)

            pcm_chunks = []
            for chunk_idx, chunk_text in enumerate(chunks, 1):
                if requests_state["made"] >= args.limit:
                    print(f"\nBatch limit of {args.limit} reached during chunk synthesis.", flush=True)
                    save_manifest(manifest_path, manifest)
                    return

                context_desc = f"Theory ({lang}/{clip_id} part {chunk_idx}/{len(chunks)})"
                chunk_audio = synthesize_with_failover(
                    text=chunk_text,
                    api_key=args.api_key,
                    voice_name=THEORY_VOICE,
                    fallback_voice=THEORY_FALLBACK_VOICE,
                    requests_state=requests_state,
                    delay=args.delay,
                    context_desc=context_desc,
                    vertex_auth=theory_vertex_auth
                )
                pcm_chunks.append(chunk_audio)
                requests_state["made"] += 1
                time.sleep(args.delay)

            concatenate_and_encode_pcm_chunks(pcm_chunks, out_file)
            manifest.add(clip_id)
            save_manifest(manifest_path, manifest)

            file_size_kb = os.path.getsize(out_file) / 1024.0
            print(f"      -> Successfully saved {lang}/{clip_id}.m4a ({file_size_kb:.1f} KB AAC)", flush=True)

        except Exception as e:
            print(f"    -> Error generating theory audio for {lesson_id}: {e}", flush=True)
            save_manifest(manifest_path, manifest)
            break

    save_manifest(manifest_path, manifest)

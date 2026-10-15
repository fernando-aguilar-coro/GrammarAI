"""
audio_generator.exercises_pipeline
Pipeline for generating exercise prompt audios, target phrases, options, dialogues, flashcards, and correct answer feedback.
"""

import os
import time
import argparse
from .config import (
    EXERCISE_VOICES,
    is_spanish_text
)
from .curriculum_loader import (
    get_exercise_prompt,
    get_exercise_target_phrase,
    get_exercise_options,
    get_exercise_correct_answer,
    get_exercise_flashcards,
    get_exercise_dialogue_lines,
    is_audio_already_present,
    load_curriculum_lessons,
    save_manifest
)
from .core import process_single_audio_item
from .gemini_client import synthesize_with_failover, VertexAuthManager
from .gcp_tts_client import synthesize_correct_answer_feedback
from .audio_processing import encode_pcm_to_aac


def process_exercises(
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
    Processes all exercises within a curriculum file:
    1. Instruction prompt (ex_<id>.m4a)
    2. Spoken target phrase / reading passage (ex_<id>_phrase.m4a)
    3. Interlocutor dialogue context lines (ex_<id>_dialogue.m4a)
    4. Flashcard front words & example sentences (ex_<id>_fc_<idx>.m4a, ex_<id>_fc_ex_<idx>.m4a)
    5. Multiple-choice options (ex_<id>_opt_<idx>.m4a)
    6. Automated correct answer feedback (ex_<id>_correct.m4a)
    """
    print(f"\n[Language: {lang}] Processing Curriculum Exercises from {os.path.basename(curriculum_path)}...", flush=True)
    lessons = load_curriculum_lessons(curriculum_path)
    if not lessons:
        print(f"Warning: {curriculum_path} not found or empty. Skipping exercises for {lang}.", flush=True)
        save_manifest(manifest_path, manifest)
        return

    if getattr(args, "category", "") == "pre_a1":
        lessons = [l for l in lessons if str(l.get("cefr", "")).upper() in ("PRE-A1", "A0") or (l.get("module", 99) <= 12)]
        print(f"Filtered for Pre-A1 Foundational lessons: {len(lessons)} lessons.", flush=True)
    elif getattr(args, "module", None) is not None:
        lessons = [l for l in lessons if l.get("module") == args.module]
        print(f"Filtered for Module {args.module}: {len(lessons)} lessons.", flush=True)

    if getattr(args, "max_lessons", 0) and args.max_lessons > 0:
        lessons = lessons[:args.max_lessons]
        print(f"Limited to first {len(lessons)} lesson(s).", flush=True)

    allowed_types = None
    if args.types and args.types.lower() != "all":
        allowed_types = {t.strip().upper() for t in args.types.split(",")}
        print(f"Filtered by exercise types: {allowed_types}", flush=True)

    include_options = getattr(args, "include_options", True)
    include_correct_feedback = getattr(args, "include_correct_feedback", True)
    curriculum_source = getattr(args, "curriculum", "lessons")

    prefix_cache = requests_state.setdefault("prefix_cache", {})

    for lesson in lessons:
        from .asset_pack_router import resolve_lesson_audio_dir
        output_dir, manifest_path = resolve_lesson_audio_dir(lesson, lang, curriculum_source)

        lesson_title = lesson.get("title", "Untitled")
        lesson_id = lesson.get("id", "")
        exercises = lesson.get("exercises", [])
        from .asset_pack_router import PROJECT_ROOT
        module_name = os.path.relpath(output_dir, PROJECT_ROOT).split(os.sep)[0]
        print(f"\n  -- Lesson: '{lesson_title}' ({lesson_id}) [{len(exercises)} exercises] -> {module_name}/{lang} --", flush=True)

        for ex in exercises:
            if requests_state["made"] >= args.limit:
                print(f"\nBatch limit of {args.limit} requests reached. Stopping cleanly.", flush=True)
                save_manifest(manifest_path, manifest)
                return

            ex_id = ex.get("id")
            if not ex_id:
                continue

            ex_type = ex.get("type", "").upper()
            matching_types = {"REPEAT_AFTER_ME", "PRONUNCIATION"} if ex_type in ("REPEAT_AFTER_ME", "PRONUNCIATION") else {ex_type}
            if allowed_types and not (matching_types & allowed_types):
                continue

            # 1. Exercise Instruction Prompt (ex_<id>.m4a)
            prompt_text = get_exercise_prompt(ex)
            if prompt_text:
                prompt_lang = "es" if is_spanish_text(prompt_text) else lang
                ok = process_single_audio_item(
                    item_id=ex_id,
                    text=prompt_text,
                    lang=prompt_lang,
                    output_dir=output_dir,
                    manifest=manifest,
                    manifest_path=manifest_path,
                    args=args,
                    requests_state=requests_state,
                    vertex_auth=vertex_auth,
                    custom_desc=f"{lang}/{ex_id} Prompt: '{prompt_text[:30]}...'"
                )
                if not ok and requests_state["made"] >= args.limit:
                    save_manifest(manifest_path, manifest)
                    return

            # 2. Practice Target Phrase / Reading Passage (ex_<id>_phrase.m4a)
            target_phrase = get_exercise_target_phrase(ex)
            if target_phrase and target_phrase.lower() != prompt_text.lower():
                phrase_id = f"{ex_id}_phrase"
                phrase_lang = "es" if is_spanish_text(target_phrase) else "en"
                ok = process_single_audio_item(
                    item_id=phrase_id,
                    text=target_phrase,
                    lang=phrase_lang,
                    output_dir=output_dir,
                    manifest=manifest,
                    manifest_path=manifest_path,
                    args=args,
                    requests_state=requests_state,
                    vertex_auth=vertex_auth,
                    custom_desc=f"{lang}/{phrase_id} TargetPhrase ({phrase_lang}): '{target_phrase[:30]}...'"
                )
                if not ok and requests_state["made"] >= args.limit:
                    save_manifest(manifest_path, manifest)
                    return

            # 3. Interlocutor Dialogue Context (ex_<id>_dialogue.m4a)
            dialogue_lines = get_exercise_dialogue_lines(ex)
            for d_idx, d_line in enumerate(dialogue_lines):
                if requests_state["made"] >= args.limit:
                    save_manifest(manifest_path, manifest)
                    return
                # Only synthesize interlocutor / non-user lines
                if not d_line.get("is_user", False):
                    d_text = d_line.get("text", "")
                    if d_text:
                        d_id = f"{ex_id}_dialogue" if len(dialogue_lines) == 1 else f"{ex_id}_dialogue_{d_idx}"
                        d_lang = "es" if is_spanish_text(d_text) else "en"
                        ok = process_single_audio_item(
                            item_id=d_id,
                            text=d_text,
                            lang=d_lang,
                            output_dir=output_dir,
                            manifest=manifest,
                            manifest_path=manifest_path,
                            args=args,
                            requests_state=requests_state,
                            vertex_auth=vertex_auth,
                            custom_desc=f"{lang}/{d_id} Interlocutor ({d_lang}): '{d_text[:30]}...'"
                        )
                        if not ok and requests_state["made"] >= args.limit:
                            save_manifest(manifest_path, manifest)
                            return

            # 4. Flashcards (front vocabulary & example sentences)
            flashcards = get_exercise_flashcards(ex)
            for fc_idx, card in enumerate(flashcards):
                if requests_state["made"] >= args.limit:
                    save_manifest(manifest_path, manifest)
                    return
                front_text = card["front"]
                if front_text:
                    fc_id = f"{ex_id}_fc_{fc_idx}"
                    fc_lang = "es" if is_spanish_text(front_text) else "en"
                    ok = process_single_audio_item(
                        item_id=fc_id,
                        text=front_text,
                        lang=fc_lang,
                        output_dir=output_dir,
                        manifest=manifest,
                        manifest_path=manifest_path,
                        args=args,
                        requests_state=requests_state,
                        vertex_auth=vertex_auth,
                        custom_desc=f"{lang}/{fc_id} Flashcard ({fc_lang}): '{front_text[:25]}...'"
                    )
                    if not ok and requests_state["made"] >= args.limit:
                        save_manifest(manifest_path, manifest)
                        return

                example_text = card["example"]
                if example_text:
                    fc_ex_id = f"{ex_id}_fc_ex_{fc_idx}"
                    fc_ex_lang = "es" if is_spanish_text(example_text) else "en"
                    ok = process_single_audio_item(
                        item_id=fc_ex_id,
                        text=example_text,
                        lang=fc_ex_lang,
                        output_dir=output_dir,
                        manifest=manifest,
                        manifest_path=manifest_path,
                        args=args,
                        requests_state=requests_state,
                        vertex_auth=vertex_auth,
                        custom_desc=f"{lang}/{fc_ex_id} Flashcard Example ({fc_ex_lang}): '{example_text[:25]}...'"
                    )
                    if not ok and requests_state["made"] >= args.limit:
                        save_manifest(manifest_path, manifest)
                        return

            # 5. Options (every single multiple choice option)
            if include_options:
                options = get_exercise_options(ex)
                for opt_idx, opt_text in enumerate(options):
                    if requests_state["made"] >= args.limit:
                        save_manifest(manifest_path, manifest)
                        return
                    opt_id = f"{ex_id}_opt_{opt_idx}"
                    opt_lang = "es" if is_spanish_text(opt_text) else "en"
                    ok = process_single_audio_item(
                        item_id=opt_id,
                        text=opt_text,
                        lang=opt_lang,
                        output_dir=output_dir,
                        manifest=manifest,
                        manifest_path=manifest_path,
                        args=args,
                        requests_state=requests_state,
                        vertex_auth=vertex_auth,
                        custom_desc=f"{lang}/{opt_id} Option ({opt_lang}): '{opt_text[:25]}...'"
                    )
                    if not ok and requests_state["made"] >= args.limit:
                        save_manifest(manifest_path, manifest)
                        return

            # 6. Correct Answer Feedback Clip (Automated "Respuesta correcta:" / "Correct answer:" + correctAnswer)
            # Skip SELECT_PAIRS and FLASHCARDS
            if include_correct_feedback and ex_type not in ("SELECT_PAIRS", "FLASHCARDS", "FLASHCARD"):
                correct_ans = get_exercise_correct_answer(ex)
                if correct_ans and "FLASHCARDS" not in correct_ans and "=" not in correct_ans:
                    correct_id = f"{ex_id}_correct"
                    correct_file = os.path.join(output_dir, f"{correct_id}.m4a")

                    if (correct_id in manifest or is_audio_already_present(correct_file)) and os.path.exists(correct_file):
                        manifest.add(correct_id)
                    elif requests_state["made"] < args.limit:
                        try:
                            if vertex_auth and args.engine in ("auto", "gcp"):
                                raw_correct_pcm = synthesize_correct_answer_feedback(
                                    correct_answer=correct_ans,
                                    lang=lang,
                                    vertex_auth=vertex_auth,
                                    prefix_cache=prefix_cache
                                )
                            else:
                                prefix = "Respuesta correcta: " if lang == "es" else "Correct answer: "
                                gemini_vertex_auth = vertex_auth if getattr(args, "vertex", False) else None
                                raw_correct_pcm = synthesize_with_failover(
                                    text=f"{prefix}{correct_ans}",
                                    api_key=args.api_key,
                                    voice_name=EXERCISE_VOICES[0],
                                    fallback_voice="Erinome",
                                    requests_state=requests_state,
                                    delay=args.delay,
                                    context_desc=f"CorrectFeedback {lang}/{correct_id}",
                                    vertex_auth=gemini_vertex_auth
                                )

                            encode_pcm_to_aac(raw_correct_pcm, correct_file)
                            manifest.add(correct_id)
                            save_manifest(manifest_path, manifest)
                            requests_state["made"] += 1
                            file_size_kb = os.path.getsize(correct_file) / 1024.0
                            print(f"      -> Saved {lang}/{correct_id}.m4a ({file_size_kb:.1f} KB AAC)", flush=True)
                            time.sleep(args.delay)
                        except Exception as e:
                            print(f"    -> Error generating correct feedback for {ex_id}: {e}", flush=True)

    save_manifest(manifest_path, manifest)

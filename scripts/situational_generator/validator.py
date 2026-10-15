"""
scripts.situational_generator.validator
Automates all structural boilerplate, exercise IDs, and defaults,
expanding the minimal LLM JSON into the full GrammarAI curriculum schema
compatible with the app and audio_generator.
"""

import re

def validate_and_normalize_lesson(
    raw: dict,
    module_id: int,
    slug_id: str,
    part_num: int,
    cefr: str = "A1"
) -> dict:
    """
    Expands lean LLM output into the full situational lesson structure.
    Also supports input that already has the 'exercises' array.
    """
    lesson_id = f"sit_{slug_id}_p{part_num}"
    title = raw.get("title", f"Situación: Parte {part_num}")
    summary = raw.get("summary", "")

    # If already formatted with 'exercises' list (backward compatibility)
    if "exercises" in raw and isinstance(raw["exercises"], list) and len(raw["exercises"]) == 5:
        exercises = raw["exercises"]
        normalized_exercises = []
        for idx, ex in enumerate(exercises, start=1):
            normalized_exercises.append(_normalize_full_exercise(ex, slug_id, part_num, idx))
        return {
            "id": lesson_id,
            "module": module_id,
            "type": "SITUATIONAL_VOCABULARY",
            "cefr": cefr,
            "title": title,
            "summary": summary,
            "durationMinutes": 2,
            "exercises": normalized_exercises,
            "status": "READY",
            "score": 0,
            "youtubeId": None,
            "theoryMarkdown": ""
        }

    # Otherwise, build from minimal lean fields:
    # 1. Flashcards (ex_sit_{slug}_{part}01)
    raw_fcs = raw.get("flashcards", [])
    if not isinstance(raw_fcs, list) or len(raw_fcs) < 3:
        raise ValueError(f"Flashcards must contain at least 3 cards, got {len(raw_fcs)}")
    
    clean_fcs = []
    for fc in raw_fcs[:4]:
        clean_fcs.append({
            "front": str(fc.get("front", "")).strip(),
            "back": str(fc.get("back", "")).strip(),
            "phonetic": str(fc.get("phonetic", "")).strip(),
            "example": str(fc.get("example", "")).strip(),
            "exampleTranslation": str(fc.get("exampleTranslation", "")).strip()
        })

    ex_1 = {
        "id": f"ex_sit_{slug_id}_{part_num}01",
        "type": "flashcards",
        "category": "LEXICON",
        "prompt": "Tarjetas clave para esta situación:",
        "options": [],
        "scrambledWords": [],
        "correctAnswer": "FLASHCARDS_COMPLETED",
        "explanation": "Vocabulario clave para desenvolverte con seguridad en esta situación.",
        "expectedPattern": "",
        "readingPassage": "",
        "dialogueContext": "",
        "audioBase64": "",
        "targetPhrase": "",
        "phonetics": "",
        "pairs": [],
        "flashcards": clean_fcs
    }

    # 2. Select Pairs (ex_sit_{slug}_{part}02)
    raw_pairs = raw.get("pairs", [])
    if not isinstance(raw_pairs, list) or len(raw_pairs) < 3:
        raise ValueError(f"Pairs must contain at least 3 pairs, got {len(raw_pairs)}")

    clean_pairs = []
    pair_strings = []
    for p in raw_pairs[:4]:
        l = str(p.get("left", "")).strip()
        r = str(p.get("right", "")).strip()
        clean_pairs.append({"left": l, "right": r})
        pair_strings.append(f"{l} = {r}")

    ex_2 = {
        "id": f"ex_sit_{slug_id}_{part_num}02",
        "type": "select_pairs",
        "category": "LEXICON",
        "prompt": "Empareja cada palabra con su significado en español:",
        "options": [],
        "scrambledWords": [],
        "correctAnswer": "; ".join(pair_strings),
        "explanation": "Pares esenciales de vocabulario situacional.",
        "expectedPattern": "",
        "readingPassage": "",
        "dialogueContext": "",
        "audioBase64": "",
        "targetPhrase": "",
        "phonetics": "",
        "pairs": clean_pairs,
        "flashcards": []
    }

    # 3. Fill in the Blank (ex_sit_{slug}_{part}03)
    fb = raw.get("fill_blank", {})
    sentence = fb.get("sentence", fb.get("readingPassage", ""))
    fb_ans = str(fb.get("correctAnswer", "")).strip()
    
    # Ensure cloze blank '___' is present in readingPassage
    if "___" not in sentence:
        if fb_ans and fb_ans.lower() in sentence.lower():
            pattern = re.compile(re.escape(fb_ans), re.IGNORECASE)
            sentence = pattern.sub("___", sentence, count=1)
        elif "_" in sentence:
            sentence = re.sub(r"_+", "___", sentence)
        else:
            sentence = f"{sentence.rstrip('.')} ___."

    fb_options = fb.get("options", [])
    if not isinstance(fb_options, list):
        fb_options = [fb_ans]
    if fb_ans and fb_ans not in fb_options:
        fb_options.append(fb_ans)

    ex_3 = {
        "id": f"ex_sit_{slug_id}_{part_num}03",
        "type": "fill_blank",
        "category": "LEXICON",
        "prompt": "Completa la frase para comunicarte con claridad:",
        "options": fb_options,
        "scrambledWords": [],
        "correctAnswer": fb_ans,
        "explanation": fb.get("explanation", "Usa la palabra más adecuada para completar la oración."),
        "expectedPattern": "",
        "readingPassage": sentence,
        "dialogueContext": "",
        "audioBase64": "",
        "targetPhrase": sentence,
        "phonetics": "",
        "pairs": [],
        "flashcards": []
    }

    # 4. Multiple Choice Dialogue (ex_sit_{slug}_{part}04)
    mc = raw.get("multiple_choice", {})
    speaker = mc.get("speaker", "Interlocutor")
    dialogue_prompt = mc.get("dialoguePrompt", mc.get("targetPhrase", "Hello!"))
    mc_ans = mc.get("correctAnswer", "")
    distractors = mc.get("distractors", [])
    if not distractors and "options" in mc:
        distractors = [opt for opt in mc["options"] if opt != mc_ans]

    mc_options = [mc_ans] + [d for d in distractors if d != mc_ans]
    # Ensure exactly 4 options if possible
    mc_options = mc_options[:4]

    # Format dialogueContext so audio_generator and UI parse speaker & interlocutor speech correctly
    formatted_dialogue = f"{speaker}: '{dialogue_prompt}'\\nYou: '___'"

    ex_4 = {
        "id": f"ex_sit_{slug_id}_{part_num}04",
        "type": "multiple_choice",
        "category": "PRAGMATICS",
        "prompt": "La otra persona te habla. Elige la respuesta más natural y cortés:",
        "options": mc_options,
        "scrambledWords": [],
        "correctAnswer": mc_ans,
        "explanation": mc.get("explanation", "Esta fórmula es la más cortés y natural en este contexto."),
        "expectedPattern": mc_ans,
        "readingPassage": "",
        "dialogueContext": formatted_dialogue,
        "audioBase64": "",
        "targetPhrase": dialogue_prompt,  # Clean line spoken by interlocutor for audio_generator
        "phonetics": "",
        "pairs": [],
        "flashcards": []
    }

    # 5. Pronunciation (ex_sit_{slug}_{part}05)
    pr = raw.get("pronunciation", {})
    phrase = pr.get("phrase", pr.get("targetPhrase", pr.get("correctAnswer", "")))
    phonetic = pr.get("phonetic", pr.get("phonetics", f"[{phrase.lower()}]"))

    ex_5 = {
        "id": f"ex_sit_{slug_id}_{part_num}05",
        "type": "pronunciation",
        "category": "PRODUCTIVE",
        "prompt": "Escucha y pronuncia la frase con confianza:",
        "options": [],
        "scrambledWords": [],
        "correctAnswer": phrase,
        "explanation": pr.get("explanation", "Practica hablar de forma fluida y clara."),
        "expectedPattern": phrase,
        "readingPassage": "",
        "dialogueContext": "",
        "audioBase64": "",
        "targetPhrase": phrase,
        "phonetics": phonetic,
        "pairs": [],
        "flashcards": []
    }

    return {
        "id": lesson_id,
        "module": module_id,
        "type": "SITUATIONAL_VOCABULARY",
        "cefr": cefr,
        "title": title,
        "summary": summary,
        "durationMinutes": 2,
        "exercises": [ex_1, ex_2, ex_3, ex_4, ex_5],
        "status": "READY",
        "score": 0,
        "youtubeId": None,
        "theoryMarkdown": ""
    }

def _normalize_full_exercise(ex: dict, slug_id: str, part_num: int, idx: int) -> dict:
    """Helper for legacy/already-expanded exercise objects."""
    ex["id"] = f"ex_sit_{slug_id}_{part_num}0{idx}"
    ex.setdefault("audioBase64", "")
    ex.setdefault("expectedPattern", "")
    ex.setdefault("scrambledWords", [])
    ex.setdefault("pairs", [])
    ex.setdefault("flashcards", [])
    ex.setdefault("options", [])
    ex.setdefault("readingPassage", "")
    ex.setdefault("dialogueContext", "")
    ex.setdefault("phonetics", "")
    return ex

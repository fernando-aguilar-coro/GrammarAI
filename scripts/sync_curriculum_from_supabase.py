#!/usr/bin/env python3
"""
sync_curriculum_from_supabase.py
Fetches full curriculum data from Supabase backend (FeliNode-backend):
- Excludes test lessons (module 9 / placement tests).
- Spanish: 90 lessons with full theory, YouTube IDs, and all exercises (including select_pairs).
- English: 88 lessons with full theory, all exercises (including select_pairs), and shares Spanish youtube_id.
- curriculum_videos.json: Catalog of all curated video lessons.
"""

import os
import sys
import json
import urllib.request
import urllib.parse
import random

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
ASSETS_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets")

SUPABASE_URL = "https://tkandopbazibmvohvckm.supabase.co"
SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRrYW5kb3BiYXppYm12b2h2Y2ttIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjgyNjgzMDEsImV4cCI6MjA4Mzg0NDMwMX0.h45E9p-JKerJ0pUj8fV2UwjAbPCZUARcsqkLCuYk6SM"

HEADERS = {
    "apikey": SUPABASE_KEY,
    "Authorization": f"Bearer {SUPABASE_KEY}",
    "Content-Type": "application/json"
}

def supabase_get(endpoint: str, params: dict = None) -> list:
    url = f"{SUPABASE_URL}/rest/v1/{endpoint}"
    if params:
        url += "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))

def fetch_all(table: str, filter_query: str) -> list:
    """Fetches all rows paginating via range headers."""
    all_rows = []
    limit = 1000
    offset = 0
    while True:
        url = f"{SUPABASE_URL}/rest/v1/{table}?{filter_query}&limit={limit}&offset={offset}"
        req = urllib.request.Request(url, headers=HEADERS)
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            if not data:
                break
            all_rows.extend(data)
            if len(data) < limit:
                break
            offset += limit
    return all_rows

def extract_scrambled_words(content: dict) -> list:
    if content.get("segments"):
        return content["segments"]
    q = content.get("question", "")
    if "/" in q:
        parts = [p.strip().strip("_") for p in q.split("/") if p.strip().strip("_")]
        if parts and ":" in parts[0]:
            parts[0] = parts[0].split(":")[-1].strip().strip("_")
        if len(parts) > 1:
            return parts
    text = content.get("correct_answer") or content.get("phrase") or ""
    if not text:
        return []
    raw_words = text.split()
    words = []
    for w in raw_words:
        cleaned = w.strip(".,!?;:¿¡\"")
        if cleaned:
            words.append(cleaned)
        elif w:
            words.append(w)
    if len(words) > 1:
        rng = random.Random(sum(ord(c) for c in text))
        shuffled = list(words)
        for _ in range(10):
            rng.shuffle(shuffled)
            if [x.lower() for x in shuffled] != [x.lower() for x in words]:
                break
        return shuffled
    return words

def map_exercise(raw_ex: dict) -> dict:
    ex_id = str(raw_ex.get("id"))
    raw_type = raw_ex.get("type", "")
    instruction = (raw_ex.get("instruction") or "").strip()
    content = raw_ex.get("content") or {}

    # Keep exact DB exercise type name
    app_type = raw_type

    options = []
    scrambled_words = []
    correct_answer = ""
    reading_passage = ""
    target_phrase = ""
    pairs = []
    category = content.get("category") or raw_ex.get("category") or "GRAMMAR"
    explanation = content.get("explanation") or raw_ex.get("explanation") or ""
    expected_pattern = content.get("expected_pattern") or ""
    dialogue_context = content.get("dialogue_context") or ""
    phonetics = content.get("phonetics") or ""

    if raw_type == "multiple_choice":
        reading_passage = content.get("phrase", "")
        raw_options = content.get("options", [])
        for opt in raw_options:
            if isinstance(opt, dict):
                text = opt.get("option_text", "")
                options.append(text)
                if opt.get("is_correct"):
                    correct_answer = text
            elif isinstance(opt, str):
                options.append(opt)
        if not correct_answer and options:
            correct_answer = content.get("correct_answer") or options[0]

    elif raw_type == "fill_blank":
        phrase = content.get("phrase", "")
        reading_passage = phrase
        target_phrase = phrase
        correct_answer = content.get("correct_answer", "")
        if "options" in content and isinstance(content["options"], list):
            options = content["options"]

    elif raw_type in ("scrambled_sentence", "sentence_reconstruction"):
        correct_answer = content.get("correct_answer", "")
        scrambled_words = extract_scrambled_words(content)

    elif raw_type in ("pronunciation", "repeat_after_me"):
        phrase = content.get("phrase") or content.get("target_phrase") or ""
        target_phrase = content.get("target_phrase") or phrase
        correct_answer = content.get("correct_answer") or target_phrase
        if "options" in content and isinstance(content["options"], list):
            options = content["options"]

    elif raw_type == "select_pairs":
        raw_pairs = content.get("pairs", [])
        for p in raw_pairs:
            pairs.append({
                "left": str(p.get("left", "")),
                "right": str(p.get("right", ""))
            })
        correct_answer = content.get("correct_answer") or json.dumps(pairs)

    elif raw_type == "translate":
        target_phrase = content.get("phrase", "")
        reading_passage = content.get("phrase", "")
        correct_answer = content.get("correct_answer", "")

    elif raw_type == "conversation_dialogue":
        correct_answer = content.get("correct_answer", "")
        target_phrase = content.get("target_phrase", "")
        if "options" in content and isinstance(content["options"], list):
            options = content["options"]

    elif raw_type == "intonation_challenge":
        correct_answer = content.get("correct_answer", "")
        if "options" in content and isinstance(content["options"], list):
            options = content["options"]

    flashcards = []
    if raw_type in ("flashcards", "flashcard"):
        raw_cards = content.get("flashcards") or content.get("cards") or []
        for c in raw_cards:
            flashcards.append({
                "front": str(c.get("front", "")),
                "back": str(c.get("back", "")),
                "phonetic": str(c.get("phonetic", "")),
                "example": str(c.get("example", "")),
                "exampleTranslation": str(c.get("exampleTranslation") or c.get("example_translation", ""))
            })
        correct_answer = content.get("correct_answer") or "FLASHCARDS_COMPLETED"

    return {
        "id": f"ex_{ex_id}",
        "type": app_type,
        "category": category,
        "prompt": instruction,
        "options": options,
        "scrambledWords": scrambled_words,
        "correctAnswer": correct_answer,
        "explanation": explanation,
        "expectedPattern": expected_pattern,
        "readingPassage": reading_passage,
        "dialogueContext": dialogue_context,
        "audioBase64": "",
        "targetPhrase": target_phrase,
        "phonetics": phonetics,
        "pairs": pairs,
        "flashcards": flashcards
    }

def main():
    print("Fetching lessons and exercises from Supabase...")

    # 1. Fetch Spanish Lessons (modules 101 to 115)
    es_raw_lessons = fetch_all(
        "lessons",
        "module_id=gte.101&module_id=lte.115&order=module_id.asc,order_index.asc"
    )
    # Exclude lessons without youtube_id (the 3 without youtube_id leaving exactly 90)
    es_lessons_filtered = [l for l in es_raw_lessons if l.get("youtube_id")]
    print(f"Spanish lessons loaded: {len(es_lessons_filtered)} (excluded test module 9 and without youtube_id)")

    # 2. Fetch English Lessons (modules 701 to 715)
    en_raw_lessons = fetch_all(
        "lessons",
        "module_id=gte.701&module_id=lte.715&order=module_id.asc,order_index.asc"
    )
    print(f"English lessons loaded: {len(en_raw_lessons)}")

    # 3. Fetch Situational Lessons (module 800 or lesson_type = 'SITUATIONAL_VOCABULARY')
    situational_raw_lessons = fetch_all(
        "lessons",
        "lesson_type=eq.SITUATIONAL_VOCABULARY&order=order_index.asc"
    )
    if not situational_raw_lessons:
        situational_raw_lessons = fetch_all(
            "lessons",
            "module_id=eq.800&order=order_index.asc"
        )
    print(f"Situational lessons loaded: {len(situational_raw_lessons)}")

    # 4. Build YouTube ID mapping for English from Spanish
    # Group Spanish lessons by (module_offset = module - 100)
    es_by_mod_offset = {}
    for l in es_lessons_filtered:
        mod_offset = l["module_id"] - 100
        es_by_mod_offset.setdefault(mod_offset, []).append(l)

    # 5. Fetch all exercises for Spanish, English, and Situational lessons
    es_lesson_ids = [l["id"] for l in es_lessons_filtered]
    en_lesson_ids = [l["id"] for l in en_raw_lessons]
    situational_lesson_ids = [l["id"] for l in situational_raw_lessons]
    all_needed_lesson_ids = set(es_lesson_ids + en_lesson_ids + situational_lesson_ids)

    print("Fetching exercises...")
    all_raw_exercises = fetch_all("exercises", "order=lesson_id.asc,order_index.asc")
    print(f"Total raw exercises in Supabase: {len(all_raw_exercises)}")

    exercises_by_lesson = {}
    for ex in all_raw_exercises:
        lid = ex.get("lesson_id")
        if lid in all_needed_lesson_ids:
            exercises_by_lesson.setdefault(lid, []).append(ex)

    # 5. Build Spanish curriculum JSON
    es_curriculum = []
    video_catalog = []

    for l in es_lessons_filtered:
        lid = l["id"]
        mod_id = l["module_id"]
        title = l.get("title", "")
        theory = l.get("theory") or ""
        yt_id = l.get("youtube_id")
        cefr = l.get("cefr") or "A1"
        summary = l.get("summary") or ""
        duration = l.get("duration_minutes") or 5

        raw_exs = exercises_by_lesson.get(lid, [])
        mapped_exs = [map_exercise(e) for e in raw_exs]

        lesson_obj = {
            "id": lid,
            "module": mod_id,
            "type": "GRAMMAR_PILLAR",
            "cefr": cefr,
            "title": title,
            "summary": summary,
            "durationMinutes": duration,
            "exercises": mapped_exs,
            "status": "READY",
            "score": 0,
            "youtubeId": yt_id,
            "theoryMarkdown": theory
        }
        es_curriculum.append(lesson_obj)

        if yt_id:
            video_catalog.append({
                "id": lid,
                "module": mod_id,
                "type": "GRAMMAR_PILLAR",
                "cefr": cefr,
                "title": title,
                "summary": summary,
                "durationMinutes": duration,
                "youtubeId": yt_id,
                "theoryMarkdown": theory
            })

    # 6. Build English curriculum JSON
    # Map YouTube IDs from corresponding Spanish lessons
    en_curriculum = []
    for l in en_raw_lessons:
        lid = l["id"]
        mod_id = l["module_id"]
        mod_offset = mod_id - 700
        title = l.get("title", "")
        theory = l.get("theory") or ""
        cefr = l.get("cefr") or "A1"
        summary = l.get("summary") or ""
        duration = l.get("duration_minutes") or 5

        yt_id = l.get("youtube_id")

        raw_exs = exercises_by_lesson.get(lid, [])
        mapped_exs = [map_exercise(e) for e in raw_exs]

        lesson_obj = {
            "id": lid,
            "module": mod_id,
            "type": "GRAMMAR_PILLAR",
            "cefr": cefr,
            "title": title,
            "summary": summary,
            "durationMinutes": duration,
            "exercises": mapped_exs,
            "status": "READY",
            "score": 0,
            "youtubeId": yt_id,
            "theoryMarkdown": theory
        }
        en_curriculum.append(lesson_obj)

    # 7. Build Situational curriculum JSON
    situational_curriculum = []
    for l in situational_raw_lessons:
        lid = l["id"]
        mod_id = l.get("module_id") or 800
        title = l.get("title", "")
        theory = l.get("theory") or ""
        yt_id = l.get("youtube_id")
        cefr = l.get("cefr") or "A2"
        summary = l.get("summary") or ""
        duration = l.get("duration_minutes") or 4

        raw_exs = exercises_by_lesson.get(lid, [])
        mapped_exs = [map_exercise(e) for e in raw_exs]

        lesson_obj = {
            "id": lid,
            "module": mod_id,
            "type": "SITUATIONAL_VOCABULARY",
            "cefr": cefr,
            "title": title,
            "summary": summary,
            "durationMinutes": duration,
            "exercises": mapped_exs,
            "status": "READY",
            "score": 0,
            "youtubeId": yt_id,
            "theoryMarkdown": theory
        }
        situational_curriculum.append(lesson_obj)

    # 8. Write out JSON files
    os.makedirs(ASSETS_DIR, exist_ok=True)
    es_path = os.path.join(ASSETS_DIR, "curriculum_lessons.json")
    en_path = os.path.join(ASSETS_DIR, "curriculum_lessons_en.json")
    videos_path = os.path.join(ASSETS_DIR, "curriculum_videos.json")
    situational_path = os.path.join(ASSETS_DIR, "curriculum_situational.json")

    with open(es_path, "w", encoding="utf-8") as f:
        json.dump(es_curriculum, f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(es_curriculum)} lessons to {es_path}")

    with open(en_path, "w", encoding="utf-8") as f:
        json.dump(en_curriculum, f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(en_curriculum)} lessons to {en_path}")

    with open(videos_path, "w", encoding="utf-8") as f:
        json.dump(video_catalog, f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(video_catalog)} videos to {videos_path}")

    with open(situational_path, "w", encoding="utf-8") as f:
        json.dump(situational_curriculum, f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(situational_curriculum)} situational lessons to {situational_path}")

    # Stats summary
    es_pairs = sum(1 for l in es_curriculum for e in l["exercises"] if e["type"] in ("select_pairs", "SELECT_PAIRS"))
    en_pairs = sum(1 for l in en_curriculum for e in l["exercises"] if e["type"] in ("select_pairs", "SELECT_PAIRS"))
    sit_pairs = sum(1 for l in situational_curriculum for e in l["exercises"] if e["type"] in ("select_pairs", "SELECT_PAIRS"))
    es_total_ex = sum(len(l["exercises"]) for l in es_curriculum)
    en_total_ex = sum(len(l["exercises"]) for l in en_curriculum)
    sit_total_ex = sum(len(l["exercises"]) for l in situational_curriculum)

    print("\n--- Summary ---")
    print(f"Spanish: {len(es_curriculum)} lessons, {es_total_ex} exercises ({es_pairs} SELECT_PAIRS)")
    print(f"English: {len(en_curriculum)} lessons, {en_total_ex} exercises ({en_pairs} SELECT_PAIRS)")
    print(f"Situational: {len(situational_curriculum)} lessons, {sit_total_ex} exercises ({sit_pairs} SELECT_PAIRS)")
    print(f"Videos catalog: {len(video_catalog)} lessons with YouTube videos")

if __name__ == "__main__":
    main()

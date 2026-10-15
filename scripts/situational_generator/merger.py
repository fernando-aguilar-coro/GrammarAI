"""
scripts.situational_generator.merger
Safely merges generated lessons into curriculum_situational.json,
allocating new module IDs without conflicts.
"""

import os
import json

def load_existing_curriculum(curriculum_path: str) -> list[dict]:
    if not os.path.exists(curriculum_path):
        return []
    try:
        with open(curriculum_path, "r", encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        print(f"    [Merger] Warning: Failed to read {curriculum_path}: {e}", flush=True)
        return []

def get_next_module_id(lessons: list[dict], default_start: int = 800) -> int:
    existing_modules = [l.get("module") for l in lessons if isinstance(l.get("module"), int)]
    if not existing_modules:
        return default_start
    return max(existing_modules) + 1

def merge_lessons_into_curriculum(
    new_lessons: list[dict],
    curriculum_path: str,
    overwrite_existing_ids: bool = True
) -> tuple[int, int]:
    """
    Merges newly generated lessons into the target file.
    Returns (added_count, updated_count).
    """
    existing = load_existing_curriculum(curriculum_path)
    existing_map = {l["id"]: idx for idx, l in enumerate(existing) if "id" in l}

    added = 0
    updated = 0

    for lesson in new_lessons:
        lid = lesson.get("id")
        if lid in existing_map:
            if overwrite_existing_ids:
                idx = existing_map[lid]
                existing[idx] = lesson
                updated += 1
        else:
            existing.append(lesson)
            existing_map[lid] = len(existing) - 1
            added += 1

    os.makedirs(os.path.dirname(os.path.abspath(curriculum_path)), exist_ok=True)
    with open(curriculum_path, "w", encoding="utf-8") as f:
        json.dump(existing, f, ensure_ascii=False, indent=2)

    return added, updated

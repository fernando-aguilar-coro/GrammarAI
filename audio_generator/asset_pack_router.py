"""
audio_generator.asset_pack_router
Routes audio clips between Base APK assets (Lessons 1-10, Situational, Words, Prompts, Onboarding)
and On-Demand Asset Packs (10-lesson tiers: 11-20, 21-30, ... 81-90) separated by language.
"""

import os
import re
import json

PACKAGE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(PACKAGE_DIR)

BASE_AUDIO_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "audio")

CEFR_ORDER = {"PRE-A1": 0, "A0": 0, "A1": 1, "A2": 2, "B1": 3, "B2": 4, "C1": 5, "C2": 6}

# Cached sorted lessons and IDs per language
_SORTED_LESSONS: dict[str, list[dict]] = {}
_SORTED_LESSON_IDS: dict[str, list[str]] = {}

def get_sorted_curriculum_lessons(lang: str) -> list[dict]:
    """Returns sorted list of lesson dicts for the language."""
    if lang in _SORTED_LESSONS:
        return _SORTED_LESSONS[lang]

    curr_file = "curriculum_lessons_en.json" if lang == "en" else "curriculum_lessons.json"
    file_path = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", curr_file)
    if not os.path.exists(file_path):
        _SORTED_LESSONS[lang] = []
        _SORTED_LESSON_IDS[lang] = []
        return []

    try:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        sorted_lessons = sorted(
            data,
            key=lambda l: (CEFR_ORDER.get(str(l.get("cefr", "")).upper(), 99), l.get("module", 0))
        )
        _SORTED_LESSONS[lang] = sorted_lessons
        _SORTED_LESSON_IDS[lang] = [str(l.get("id", "")) for l in sorted_lessons if l.get("id")]
        return sorted_lessons
    except Exception as e:
        print(f"Warning: Failed to load curriculum for routing ({lang}): {e}")
        _SORTED_LESSONS[lang] = []
        _SORTED_LESSON_IDS[lang] = []
        return []

def get_sorted_curriculum_ids(lang: str) -> list[str]:
    """Returns sorted list of lesson IDs for the language."""
    get_sorted_curriculum_lessons(lang)
    return _SORTED_LESSON_IDS.get(lang, [])

def normalize_lesson_id(raw_id: str) -> str:
    """Normalizes lesson ID by removing quotes and normalizing spaces."""
    return raw_id.replace('"', '').replace(' ', '_').strip().lower()

def is_pre_a1_lesson(lesson: dict) -> bool:
    """Checks if a lesson dict is part of Pre-A1 foundational content."""
    cefr = str(lesson.get("cefr", "")).upper()
    mod = lesson.get("module", 99)
    return cefr in ("PRE-A1", "A0") or (isinstance(mod, int) and mod <= 12 and cefr in ("PRE-A1", "A0", ""))

def find_standard_lesson_info(lesson: dict | str, lang: str) -> tuple[bool, int]:
    """
    Determines if lesson is Pre-A1, and if not, returns (False, standard_lesson_index 0..89).
    If it is Pre-A1, returns (True, -1).
    """
    sorted_lessons = get_sorted_curriculum_lessons(lang)
    if not sorted_lessons:
        return False, 0

    if isinstance(lesson, dict):
        lid = str(lesson.get("id", ""))
        title = str(lesson.get("title", "")).strip().lower()
    else:
        lid = str(lesson)
        title = ""

    norm_lid = normalize_lesson_id(lid)
    uuid_match = re.search(r'[0-9a-fA-F-]{8,36}', lid)
    uuid_str = uuid_match.group(0).lower() if uuid_match else None

    std_lessons = [l for l in sorted_lessons if not is_pre_a1_lesson(l)]
    pre_a1_lessons = [l for l in sorted_lessons if is_pre_a1_lesson(l)]

    for cand in pre_a1_lessons:
        cand_id = str(cand.get("id", ""))
        cand_norm = normalize_lesson_id(cand_id)
        if cand_id == lid or cand_norm == norm_lid:
            return True, -1
        if uuid_str and uuid_str in cand_id.lower():
            return True, -1
        if title and str(cand.get("title", "")).strip().lower() == title:
            return True, -1

    for idx, cand in enumerate(std_lessons):
        cand_id = str(cand.get("id", ""))
        cand_norm = normalize_lesson_id(cand_id)
        if cand_id == lid or cand_norm == norm_lid:
            return False, idx
        if uuid_str and uuid_str in cand_id.lower():
            return False, idx
        if title and str(cand.get("title", "")).strip().lower() == title:
            return False, idx

    return False, 0

def find_lesson_index(lesson: dict | str, lang: str) -> int:
    """
    Robustly finds the CEFR-sorted index of a lesson across all lessons.
    Handles raw ID, cleaned ID, UUID suffix matching, and title matching.
    """
    sorted_lessons = get_sorted_curriculum_lessons(lang)
    if not sorted_lessons:
        return 0

    if isinstance(lesson, dict):
        lid = str(lesson.get("id", ""))
        title = str(lesson.get("title", "")).strip().lower()
    else:
        lid = str(lesson)
        title = ""

    norm_lid = normalize_lesson_id(lid)
    uuid_match = re.search(r'[0-9a-fA-F-]{8,36}', lid)
    uuid_str = uuid_match.group(0).lower() if uuid_match else None

    for idx, cand in enumerate(sorted_lessons):
        cand_id = str(cand.get("id", ""))
        cand_norm = normalize_lesson_id(cand_id)
        if cand_id == lid or cand_norm == norm_lid:
            return idx
        if uuid_str and uuid_str in cand_id.lower():
            return idx
        if title and str(cand.get("title", "")).strip().lower() == title:
            return idx

    return 0

def resolve_lesson_audio_dir(lesson: dict | str, lang: str, curriculum_source: str = "lessons") -> tuple[str, str]:
    """
    Resolves the destination directory and manifest path for a lesson's audio clips.
    - Situational / Videos / Words -> Base APK assets.
    - Pre-A1 Foundational Lessons -> Base APK assets.
    - Standard Lessons 1..10 (index 0..9) -> Base APK assets.
    - Standard Lessons 11..20 (index 10..19) -> audio_pack_<lang>_11_20 assets.
    - ...
    - Standard Lessons 81..90 (index 80+) -> audio_pack_<lang>_81_90 assets.
    """
    if curriculum_source in ("situational", "videos"):
        out_dir = os.path.join(BASE_AUDIO_DIR, lang)
        return out_dir, os.path.join(out_dir, "generated_manifest.json")

    is_pre_a1, std_idx = find_standard_lesson_info(lesson, lang)

    if is_pre_a1 or std_idx < 10:
        target_root = BASE_AUDIO_DIR
        mf_name = "generated_manifest.json"
    else:
        tier_idx = min((std_idx - 10) // 10, 7) # 0..7 for 8 tiers
        start = 11 + (tier_idx * 10)
        end = start + 9
        pack_name = f"audio_pack_{lang}_{start}_{end}"
        target_root = os.path.join(PROJECT_ROOT, pack_name, "src", "main", "assets", "audio")
        mf_name = f"manifest_{pack_name}.json"

    out_dir = os.path.join(target_root, lang)
    os.makedirs(out_dir, exist_ok=True)
    return out_dir, os.path.join(out_dir, mf_name)

def get_all_pack_dirs(lang: str) -> list[str]:
    """Dynamically finds all asset pack audio directories for this language."""
    dirs = []
    prefix = f"audio_pack_{lang}_"
    if os.path.exists(PROJECT_ROOT):
        for entry in os.listdir(PROJECT_ROOT):
            if entry.startswith(prefix) and os.path.isdir(os.path.join(PROJECT_ROOT, entry)):
                audio_dir = os.path.join(PROJECT_ROOT, entry, "src", "main", "assets", "audio", lang)
                if os.path.exists(audio_dir):
                    dirs.append(audio_dir)
    return dirs

def find_audio_clip(item_id: str, lang: str) -> str | None:
    """
    Looks for an existing audio clip across Base APK assets and all on-demand packs.
    Returns the absolute path if found, or None.
    """
    candidate_names = [f"{item_id}.m4a", f"{item_id}.wav"]
    check_dirs = [
        os.path.join(BASE_AUDIO_DIR, lang),
        os.path.join(BASE_AUDIO_DIR, "words")
    ]
    check_dirs.extend(get_all_pack_dirs(lang))

    for d in check_dirs:
        for name in candidate_names:
            p = os.path.join(d, name)
            if os.path.exists(p):
                sz = os.path.getsize(p)
                min_size = 8000 if name.endswith("_correct.m4a") else 1024
                if sz > min_size:
                    return p
    return None

def get_all_manifest_paths(lang: str) -> list[str]:
    """Returns all manifest paths for the language across base assets and asset packs."""
    manifests = [os.path.join(BASE_AUDIO_DIR, lang, "generated_manifest.json")]
    for p_dir in get_all_pack_dirs(lang):
        pack_name = os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(p_dir)))))
        manifests.append(os.path.join(p_dir, f"manifest_{pack_name}.json"))
    return manifests

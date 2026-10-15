#!/usr/bin/env python3
"""
scripts/setup_play_asset_packs.py
Partitions audio assets for GrammarAI into 10-lesson on-demand tiers:
- Base APK (Lessons 1-10, Words, Situational, Prompts, Onboarding) in app/src/main/assets/audio/
- On-Demand Packs (11-20, 21-30, 31-40, 41-50, 51-60, 61-70, 71-80, 81-90) for both Spanish and English.
"""

import os
import sys
import json
import shutil
import time
import re

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
BASE_DIR = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "audio")

CEFR_ORDER = {"PRE-A1": 0, "A0": 0, "A1": 1, "A2": 2, "B1": 3, "B2": 4, "C1": 5, "C2": 6}

TIERS = [
    (11, 20),
    (21, 30),
    (31, 40),
    (41, 50),
    (51, 60),
    (61, 70),
    (71, 80),
    (81, 90)
]

def is_pre_a1_lesson(lesson: dict) -> bool:
    """Checks if a lesson dict is part of Pre-A1 foundational content."""
    cefr = str(lesson.get("cefr", "")).upper()
    mod = lesson.get("module", 99)
    return cefr in ("PRE-A1", "A0") or (isinstance(mod, int) and mod <= 12 and cefr in ("PRE-A1", "A0", ""))

def get_pack_name(lang: str, start: int, end: int) -> str:
    return f"audio_pack_{lang}_{start}_{end}"

def ensure_pack_module(pack_name: str, lang: str):
    pack_dir = os.path.join(PROJECT_ROOT, pack_name)
    os.makedirs(pack_dir, exist_ok=True)
    
    # build.gradle.kts
    gradle_file = os.path.join(pack_dir, "build.gradle.kts")
    if not os.path.exists(gradle_file):
        content = f"""plugins {{
    id("com.android.asset-pack")
}}

assetPack {{
    packName.set("{pack_name}")
    dynamicDelivery {{
        deliveryType.set("on-demand")
    }}
}}
"""
        with open(gradle_file, "w", encoding="utf-8") as f:
            f.write(content)

    assets_audio_lang = os.path.join(pack_dir, "src", "main", "assets", "audio", lang)
    os.makedirs(assets_audio_lang, exist_ok=True)
    return assets_audio_lang

def build_lesson_stems(lesson: dict) -> list[str]:
    stems = []
    lid = str(lesson.get("id", ""))
    clean_lid = lid.replace('"', '').replace(' ', '_')
    py_slug = "".join([c if c.isalnum() or c in "_-" else "_" for c in lid]).strip("_")

    candidates = [
        lid, clean_lid, py_slug,
        f"theory_{lid}", f"theory_{clean_lid}", f"theory_{py_slug}"
    ]
    for c in candidates:
        if c and c.lower() not in stems:
            stems.append(c.lower())

    # UUID suffix if any
    uuid_match = re.search(r'[0-9a-fA-F-]{8,36}', lid)
    if uuid_match:
        stems.append(uuid_match.group(0).lower())

    for ex in lesson.get("exercises", []):
        eid = str(ex.get("id", "")).lower()
        if not eid:
            continue
        stems.append(eid)
        stems.append(f"{eid}_correct")
        stems.append(f"{eid}_phrase")
        stems.append(f"{eid}_dialogue")
        for i in range(10):
            stems.append(f"{eid}_dialogue_{i}")
            stems.append(f"{eid}_fc_{i}")
            stems.append(f"{eid}_fc_ex_{i}")
            stems.append(f"{eid}_opt_{i}")

    return stems

def partition_language(lang: str):
    curr_filename = "curriculum_lessons_en.json" if lang == "en" else "curriculum_lessons.json"
    curr_path = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", curr_filename)
    if not os.path.exists(curr_path):
        print(f"Curriculum not found: {curr_path}")
        return

    with open(curr_path, "r", encoding="utf-8") as f:
        lessons = json.load(f)

    sorted_lessons = sorted(
        lessons,
        key=lambda l: (CEFR_ORDER.get(str(l.get("cefr", "")).upper(), 99), l.get("module", 0))
    )

    base_lang_dir = os.path.join(BASE_DIR, lang)
    os.makedirs(base_lang_dir, exist_ok=True)

    # Initialize pack directories
    tier_dirs = []
    for start, end in TIERS:
        pack_name = get_pack_name(lang, start, end)
        dest_dir = ensure_pack_module(pack_name, lang)
        tier_dirs.append((start, end, pack_name, dest_dir))

    # Map stems to tier destination
    stem_to_dest = {}
    pre_a1_lessons = [l for l in sorted_lessons if is_pre_a1_lesson(l)]
    std_lessons = [l for l in sorted_lessons if not is_pre_a1_lesson(l)]

    for lesson in pre_a1_lessons:
        for s in build_lesson_stems(lesson):
            stem_to_dest[s] = base_lang_dir

    for std_idx, lesson in enumerate(std_lessons):
        if std_idx < 10:
            target_dest = base_lang_dir
        else:
            tier_idx = min((std_idx - 10) // 10, len(TIERS) - 1)
            target_dest = tier_dirs[tier_idx][3]

        for s in build_lesson_stems(lesson):
            stem_to_dest[s] = target_dest

    # Collect all audio files for this language across base, old packs, and all tier packs
    search_dirs = [
        base_lang_dir,
        os.path.join(PROJECT_ROOT, "audio_pack_31_60", "src", "main", "assets", "audio", lang),
        os.path.join(PROJECT_ROOT, "audio_pack_61_90", "src", "main", "assets", "audio", lang)
    ] + [d[3] for d in tier_dirs]

    found_files = [] # list of (fname, src_path)
    for d in search_dirs:
        if os.path.exists(d):
            for fname in os.listdir(d):
                if fname.endswith(".m4a") or fname.endswith(".wav"):
                    found_files.append((fname, os.path.join(d, fname)))

    print(f"\n[{lang.upper()}] Total audio clips located: {len(found_files)}")

    stats = {"base": 0}
    for _, _, pack_name, _ in tier_dirs:
        stats[pack_name] = 0

    for fname, src_file in found_files:
        if not os.path.exists(src_file):
            continue
        stem = fname.rsplit(".", 1)[0].lower()
        base_id = stem.split("_correct")[0].split("_phrase")[0].split("_dialogue")[0].split("_fc")[0].split("_opt")[0]

        target_dir = base_lang_dir
        # Check direct stem match, base_id match, or uuid match
        if stem in stem_to_dest:
            target_dir = stem_to_dest[stem]
        elif base_id in stem_to_dest:
            target_dir = stem_to_dest[base_id]
        else:
            for s, d in stem_to_dest.items():
                if len(s) >= 8 and s in stem:
                    target_dir = d
                    break

        dest_file = os.path.join(target_dir, fname)
        if os.path.abspath(src_file) != os.path.abspath(dest_file):
            if os.path.exists(dest_file):
                # If target already exists, keep the larger file and remove duplicate
                if os.path.getsize(src_file) > os.path.getsize(dest_file):
                    os.remove(dest_file)
                    shutil.move(src_file, dest_file)
                else:
                    os.remove(src_file)
            else:
                shutil.move(src_file, dest_file)

        if target_dir == base_lang_dir:
            stats["base"] += 1
        else:
            # find which pack
            for _, _, p_name, p_dir in tier_dirs:
                if target_dir == p_dir:
                    stats[p_name] += 1
                    break

    # Print summary
    print(f"[{lang.upper()}] Partitioning Results:")
    print(f"  Base APK (Lessons 1-10 + Onboarding/Prompts): {stats['base']} files")
    for start, end, p_name, _ in tier_dirs:
        print(f"  {p_name} ({start}-{end}): {stats[p_name]} files")

    # Generate manifests
    # Base manifest
    base_files = [f.rsplit(".", 1)[0] for f in os.listdir(base_lang_dir) if f.endswith(".m4a") or f.endswith(".wav")]
    base_mf = {
        "total_generated": len(base_files),
        "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "generated_ids": sorted(base_files)
    }
    with open(os.path.join(base_lang_dir, "generated_manifest.json"), "w", encoding="utf-8") as mf:
        json.dump(base_mf, mf, indent=2)

    # Pack manifests
    for start, end, p_name, p_dir in tier_dirs:
        p_files = [f.rsplit(".", 1)[0] for f in os.listdir(p_dir) if f.endswith(".m4a") or f.endswith(".wav")]
        p_mf = {
            "total_generated": len(p_files),
            "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "generated_ids": sorted(p_files)
        }
        mf_path = os.path.join(p_dir, f"manifest_{p_name}.json")
        with open(mf_path, "w", encoding="utf-8") as mf:
            json.dump(p_mf, mf, indent=2)

def cleanup_old_packs():
    for old_name in ["audio_pack_31_60", "audio_pack_61_90"]:
        p = os.path.join(PROJECT_ROOT, old_name)
        if os.path.exists(p):
            print(f"Removing old pack directory: {old_name}")
            shutil.rmtree(p, ignore_errors=True)

def main():
    print("=" * 60)
    print("GrammarAI: Partitioning Audio Assets into 10-Lesson Tiers")
    print("=" * 60)
    partition_language("es")
    partition_language("en")
    cleanup_old_packs()
    print("\nAudio partitioning successfully finished.")

if __name__ == "__main__":
    main()

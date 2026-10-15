"""
scripts.situational_generator.generator
Coordinates generation of multi-part situational lessons using Google AI Studio.
"""

import re
import sys
import subprocess
from .client import GeminiStudioClient
from .config import DEFAULT_CURRICULUM_PATH
from .prompt_templates import SYSTEM_INSTRUCTION, build_part_prompt
from .validator import validate_and_normalize_lesson
from .merger import load_existing_curriculum, get_next_module_id, merge_lessons_into_curriculum

def slugify(text: str) -> str:
    slug = re.sub(r"[^a-zA-Z0-9]+", "_", text.lower()).strip("_")
    return slug[:12] if slug else "topic"

class SituationalGenerator:
    def __init__(self, curriculum_path: str = DEFAULT_CURRICULUM_PATH):
        self.curriculum_path = curriculum_path
        self.client = GeminiStudioClient()

    def generate_situation(
        self,
        theme_title: str,
        topic_summary: str = "",
        cefr: str = "A1",
        num_parts: int = 3,
        slug_id: str | None = None,
        module_id: int | None = None,
        dry_run: bool = False,
        force: bool = False
    ) -> list[dict]:
        """Generates all parts for a given situation."""
        existing = load_existing_curriculum(self.curriculum_path)

        if not slug_id:
            slug_id = slugify(theme_title)

        # Check if this situation's parts already exist in curriculum
        target_ids = [f"sit_{slug_id}_p{p}" for p in range(1, num_parts + 1)]
        existing_ids = {l.get("id") for l in existing if "id" in l}
        if not force and all(tid in existing_ids for tid in target_ids):
            print(f"\n[Situational Generator] [SKIP] Situation '{theme_title}' (slug: {slug_id}) already exists in curriculum. Use --force to regenerate.", flush=True)
            return [l for l in existing if l.get("id") in target_ids]

        if module_id is None:
            module_id = get_next_module_id(existing)

        generated_parts = []
        print(f"\n[Situational Generator] Generating '{theme_title}' (CEFR {cefr}, Module {module_id}, {num_parts} parts)...", flush=True)

        for p_idx in range(1, num_parts + 1):
            part_summary = f"{topic_summary} - Parte {p_idx}" if topic_summary else f"Práctica situacional de {theme_title} (Parte {p_idx})"
            prompt = build_part_prompt(
                topic_title=f"{theme_title}: Parte {p_idx}",
                topic_summary=part_summary,
                cefr=cefr,
                part_num=p_idx,
                total_parts=num_parts
            )

            print(f"  -> Generating Part {p_idx}/{num_parts}...", flush=True)
            raw_data = self.client.generate_json(prompt, system_instruction=SYSTEM_INSTRUCTION)
            validated = validate_and_normalize_lesson(
                raw=raw_data,
                module_id=module_id,
                slug_id=slug_id,
                part_num=p_idx,
                cefr=cefr
            )
            generated_parts.append(validated)

        if not dry_run:
            added, updated = merge_lessons_into_curriculum(generated_parts, self.curriculum_path)
            print(f"  -> Successfully merged into {self.curriculum_path} (+{added} added, ~{updated} updated)", flush=True)
        else:
            print(f"  -> [DRY RUN] Generated {len(generated_parts)} parts without writing to file.", flush=True)

        return generated_parts

def run_audio_generator_for_situational():
    """Triggers audio synthesis for newly generated situational curriculum."""
    print("\n[Audio Generator] Triggering audio synthesis for situational curriculum...", flush=True)
    cmd = [
        sys.executable,
        "-m",
        "audio_generator",
        "--category",
        "exercises",
        "--curriculum",
        "situational"
    ]
    subprocess.run(cmd, check=False)

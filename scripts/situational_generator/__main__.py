"""
scripts.situational_generator.__main__
CLI entry point for generating situational lessons using Google AI Studio.
"""

import os
import json
import argparse
from .config import THEMES_PATH, DEFAULT_CURRICULUM_PATH
from .generator import SituationalGenerator, run_audio_generator_for_situational

def main():
    parser = argparse.ArgumentParser(
        description="GrammarAI Situational Curriculum Generator powered by Google AI Studio"
    )
    parser.add_argument(
        "--theme",
        type=str,
        help="Title or theme of the situation to generate (e.g., 'El Aeropuerto: Check-in y Embarque')"
    )
    parser.add_argument(
        "--cefr",
        type=str,
        default="A1",
        choices=["A1", "A2", "B1", "B2"],
        help="Target CEFR level (default: A1)"
    )
    parser.add_argument(
        "--parts",
        type=int,
        default=3,
        help="Number of parts per situation (default: 3)"
    )
    parser.add_argument(
        "--batch",
        type=int,
        nargs="?",
        const=999,
        help="Generate topics from themes.json in batch (optional limit of topics to generate)"
    )
    parser.add_argument(
        "--generate-audio",
        action="store_true",
        help="Automatically trigger audio_generator after generating curriculum"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Run generation and validation without modifying curriculum_situational.json"
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Force regeneration even if the situation already exists in curriculum"
    )
    parser.add_argument(
        "--curriculum-path",
        type=str,
        default=DEFAULT_CURRICULUM_PATH,
        help="Path to target curriculum JSON file"
    )

    args = parser.parse_args()
    generator = SituationalGenerator(curriculum_path=args.curriculum_path)

    if args.theme:
        generator.generate_situation(
            theme_title=args.theme,
            cefr=args.cefr,
            num_parts=args.parts,
            dry_run=args.dry_run,
            force=args.force
        )
    elif args.batch is not None:
        if not os.path.exists(THEMES_PATH):
            print(f"Error: Themes file not found at {THEMES_PATH}")
            return
        with open(THEMES_PATH, "r", encoding="utf-8") as f:
            catalog = json.load(f)

        to_run = catalog[:args.batch]
        print(f"Running batch generation for {len(to_run)} topics from themes catalog...")
        for item in to_run:
            generator.generate_situation(
                theme_title=item.get("theme", "Situación"),
                topic_summary=item.get("topic_en", ""),
                cefr=item.get("cefr", args.cefr),
                num_parts=len(item.get("parts", [])) or args.parts,
                slug_id=item.get("id"),
                dry_run=args.dry_run,
                force=args.force
            )
    else:
        print("Please provide --theme <title> or --batch to start generation.")
        parser.print_help()
        return

    if args.generate_audio and not args.dry_run:
        run_audio_generator_for_situational()

if __name__ == "__main__":
    main()

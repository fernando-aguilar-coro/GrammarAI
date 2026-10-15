import json
import re
import os

def clean_markdown_for_speech(text):
    if not text:
        return ""
    t = re.sub(r'```.*?```', '', text, flags=re.DOTALL)
    t = re.sub(r'#+\s*', '', t)
    t = re.sub(r'\*\*([^*]+)\*\*', r'\1', t)
    t = re.sub(r'\*([^*]+)\*', r'\1', t)
    t = re.sub(r'\|', ' ', t)
    t = re.sub(r'-{3,}', ' ', t)
    t = re.sub(r'\s+', ' ', t).strip()
    return t

def calculate_costs(items):
    uniq_items = set(i.strip() for i in items if i and i.strip())
    calls = len(uniq_items)
    uniq_chars = sum(len(x) for x in uniq_items)
    words = sum(len(x.split()) for x in uniq_items)
    
    # 145 words per minute
    duration_min = words / 145.0
    duration_hours = duration_min / 60.0
    
    # Storage based on actual AAC measured rates
    # 64 kbps mono = 0.50 MB/min
    # 32 kbps mono = 0.25 MB/min
    size_64k_mb = duration_min * 0.500
    size_32k_mb = duration_min * 0.250

    # API costs
    cost_gcp_std = (uniq_chars / 1_000_000) * 4.0
    cost_gcp_neural = (uniq_chars / 1_000_000) * 16.0
    cost_openai_std = (uniq_chars / 1_000_000) * 15.0
    cost_openai_hd = (uniq_chars / 1_000_000) * 30.0
    cost_gemini = duration_min * 0.06 # Gemini Flash / Multimodal Audio output
    cost_eleven = (uniq_chars / 1_000_000) * 200.0

    return {
        'calls': calls,
        'words': words,
        'uniq_chars': uniq_chars,
        'duration_min': duration_min,
        'duration_hours': duration_hours,
        'size_64k_mb': size_64k_mb,
        'size_32k_mb': size_32k_mb,
        'cost_gcp_std': cost_gcp_std,
        'cost_gcp_neural': cost_gcp_neural,
        'cost_openai_std': cost_openai_std,
        'cost_openai_hd': cost_openai_hd,
        'cost_gemini': cost_gemini,
        'cost_eleven': cost_eleven
    }

def extract_categories(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        lessons = json.load(f)

    theory = []
    prompts = []
    targets_readings = []
    options_answers = []
    pairs = []

    for l in lessons:
        if l.get('theoryMarkdown'):
            clean = clean_markdown_for_speech(l['theoryMarkdown'])
            if clean:
                theory.append(clean)
        if l.get('title'):
            theory.append(l['title'].strip())

        for ex in l.get('exercises', []):
            p = (ex.get('prompt') or '').strip()
            if p:
                prompts.append(p)

            tp = (ex.get('targetPhrase') or '').strip()
            rp = (ex.get('readingPassage') or '').strip()
            dc = (ex.get('dialogueContext') or '').strip()
            if tp: targets_readings.append(tp)
            if rp: targets_readings.append(rp)
            if dc: targets_readings.append(dc)

            ca = (ex.get('correctAnswer') or '').strip()
            if ca: options_answers.append(ca)
            for opt in ex.get('options') or []:
                if opt and opt.strip():
                    options_answers.append(opt.strip())

            for pair in ex.get('pairs') or []:
                if isinstance(pair, dict):
                    for v in pair.values():
                        if isinstance(v, str) and v.strip():
                            pairs.append(v.strip())

    return {
        'theory': theory,
        'prompts': prompts,
        'targets_readings': targets_readings,
        'options_answers': options_answers,
        'pairs': pairs
    }

es_cats = extract_categories(r'c:\fer\felinode\GrammarAI\app\src\main\assets\curriculum_lessons.json')
en_cats = extract_categories(r'c:\fer\felinode\GrammarAI\app\src\main\assets\curriculum_lessons_en.json')

def print_result(label, items):
    s = calculate_costs(items)
    print(f"[{label}]")
    print(f"  Clips / API Calls : {s['calls']:,}")
    print(f"  Characters        : {s['uniq_chars']:,} ({s['words']:,} words)")
    print(f"  Estimated Audio   : {s['duration_min']:.1f} min ({s['duration_hours']:.2f} hours)")
    print(f"  App Size (64k AAC): {s['size_64k_mb']:.1f} MB")
    print(f"  App Size (32k AAC): {s['size_32k_mb']:.1f} MB")
    print(f"  Cost OpenAI TTS-1 : ${s['cost_openai_std']:.2f} (HD: ${s['cost_openai_hd']:.2f})")
    print(f"  Cost Google Neural: ${s['cost_gcp_neural']:.2f} (Standard: ${s['cost_gcp_std']:.2f})")
    print(f"  Cost Gemini Flash : ${s['cost_gemini']:.2f}")
    print(f"  Cost ElevenLabs   : ${s['cost_eleven']:.2f}")
    print()

print("=================================================================")
print("             CURRICULUM AUDIO IMPACT ASSESSMENT                  ")
print("=================================================================")

print(">>> SPANISH CURRICULUM (curriculum_lessons.json)")
print_result("1. ES - 100% EVERYTHING (Theory + Prompts + Options + Answers + Pairs)",
             es_cats['theory'] + es_cats['prompts'] + es_cats['targets_readings'] + es_cats['options_answers'] + es_cats['pairs'])
print_result("2. ES - Theory Lessons Only (90 lessons)", es_cats['theory'])
print_result("3. ES - Practice Audio Only (Sentences, Targets, Answers, Options)",
             es_cats['targets_readings'] + es_cats['options_answers'] + es_cats['pairs'])
print_result("4. ES - Standard App Flow (Theory + Target/Sentences + Shared Prompts)",
             es_cats['theory'] + es_cats['targets_readings'] + es_cats['options_answers'])

print(">>> ENGLISH CURRICULUM (curriculum_lessons_en.json)")
print_result("1. EN - 100% EVERYTHING (Theory + Prompts + Options + Answers + Pairs)",
             en_cats['theory'] + en_cats['prompts'] + en_cats['targets_readings'] + en_cats['options_answers'] + en_cats['pairs'])
print_result("2. EN - Theory Lessons Only (88 lessons)", en_cats['theory'])
print_result("3. EN - Practice Audio Only (Sentences, Targets, Answers, Options)",
             en_cats['targets_readings'] + en_cats['options_answers'] + en_cats['pairs'])

print(">>> TOTAL COMBINED (ES + EN CURRICULA)")
all_both = (es_cats['theory'] + es_cats['prompts'] + es_cats['targets_readings'] + es_cats['options_answers'] + es_cats['pairs'] +
            en_cats['theory'] + en_cats['prompts'] + en_cats['targets_readings'] + en_cats['options_answers'] + en_cats['pairs'])
print_result("COMBINED 100% ALL TEXT (Both Languages)", all_both)

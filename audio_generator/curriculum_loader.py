"""
audio_generator.curriculum_loader
Loads curriculum lessons, extracts spoken text for exercises, onboarding, and theories,
and manages audio manifests and deduplication caching.
"""

import os
import re
import json
import time
from .asset_pack_router import find_audio_clip, get_all_manifest_paths

def load_manifest(manifest_path: str) -> set[str]:
    """Loads set of already generated audio IDs from the specified manifest file."""
    if os.path.exists(manifest_path):
        try:
            with open(manifest_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                return set(data.get("generated_ids", []))
        except Exception:
            return set()
    return set()

def load_all_manifests_for_lang(lang: str) -> set[str]:
    """Loads and combines all generated audio IDs across base assets and all packs."""
    combined = set()
    for p in get_all_manifest_paths(lang):
        combined.update(load_manifest(p))
    return combined

def save_manifest(manifest_path: str, manifest_set: set[str]):
    """Persists generated audio IDs to manifest."""
    os.makedirs(os.path.dirname(manifest_path), exist_ok=True)
    data = {
        "total_generated": len(manifest_set),
        "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "generated_ids": sorted(list(manifest_set))
    }
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

def is_audio_already_present(file_path: str) -> bool:
    """Checks whether valid audio file already exists on disk (directly or in any pack)."""
    if os.path.exists(file_path):
        size = os.path.getsize(file_path)
        min_size = 8000 if file_path.endswith("_correct.m4a") else 1024
        if size > min_size:
            return True

    # Cross-check across base assets and all asset packs
    base_name = os.path.basename(file_path)
    item_id = os.path.splitext(base_name)[0]
    parent_dir = os.path.basename(os.path.dirname(file_path))
    if parent_dir in ("es", "en", "words"):
        existing = find_audio_clip(item_id, parent_dir)
        if existing:
            return True
    return False

def sanitize_id_to_filename(raw_id: str) -> str:
    """Sanitizes any identifier into a filesystem-safe base name."""
    clean = re.sub(r'[^\w\-]', '_', raw_id)
    return clean.strip('_')

def get_theory_clip_id(lesson_id: str) -> str:
    """Returns unique manifest identifier for a lesson's theory clip."""
    return f"theory_{sanitize_id_to_filename(lesson_id)}"

def load_curriculum_lessons(file_path: str) -> list[dict]:
    """Loads curriculum lessons JSON file."""
    if not os.path.exists(file_path):
        return []
    with open(file_path, "r", encoding="utf-8") as f:
        return json.load(f)

def get_onboarding_items(lang: str) -> list[dict]:
    """
    Onboarding script matching OnboardingScriptHelper for Spanish ('es')
    and 100% English ('en').
    """
    if lang == "es":
        return [
            {
                "id": "onboarding_presentation",
                "text": "Hello! I am your AI English coach. I will help you speak English naturally and build lasting fluency with personalized daily practice."
            },
            {
                "id": "onboarding_native_lang",
                "text": "What is your native language?"
            },
            {
                "id": "onboarding_app_lang",
                "text": "Would you like to keep the app interface in English for full immersion, or switch it to Spanish?"
            },
            {
                "id": "onboarding_name",
                "text": "¡Mucho gusto! ¿Cómo te gustaría que te llame?"
            },
            {
                "id": "onboarding_goal",
                "text": "¡Mucho gusto! ¿Cuál es tu meta principal con el inglés?"
            },
            {
                "id": "onboarding_situations",
                "text": "¿De qué tipo de situaciones te gustaría hablar más a menudo?"
            },
            {
                "id": "onboarding_level",
                "text": "¿Cuál consideras que es tu nivel actual de inglés?"
            },
            {
                "id": "onboarding_weekly_goal",
                "text": "¿Cuántas lecciones te gustaría completar cada semana? Establecer una meta semanal crea constancia sin el estrés de una racha diaria."
            },
            {
                "id": "onboarding_reminder",
                "text": "La constancia es la clave para la fluidez. ¿Te gustaría activar un recordatorio diario para practicar?"
            },
            {
                "id": "onboarding_api_key_intro",
                "text": "GrammarAI utiliza Google Gemini para darte correcciones gramaticales en vivo, retroalimentación detallada y conversaciones fluidas de rol por voz con IA. Para disfrutar de estas funciones con mayor velocidad y privacidad, puedes conectar tu propia Gemini API Key gratuita. Es opcional: si prefieres no configurarla ahora, la app continuará usando una clave genérica compartida."
            },
            {
                "id": "onboarding_api_key_howto",
                "text": "Obtener tu clave gratuita de Gemini toma menos de 1 minuto: Entra con tu cuenta de Google a aistudio.google.com, presiona el botón azul 'Get API key', haz clic en 'Create API key' en un proyecto (es 100% gratuita), copia la clave generada y pégala aquí abajo. ¡Puedes tocar el botón de abajo para abrir Google AI Studio directamente!"
            },
            {
                "id": "onboarding_api_key_prompt",
                "text": "¡Excelente! Pega tu clave API de Gemini a continuación. Puedes probar la conexión para comprobar que funcione antes de guardar:"
            },
            {
                "id": "onboarding_sync",
                "text": "Protege tu progreso y sincroniza tus datos en cualquier dispositivo vinculando tu cuenta de Google."
            }
        ]
    else:
        return [
            {
                "id": "onboarding_presentation",
                "text": "Hello! I am your AI English coach. I will help you speak English naturally and build lasting fluency with personalized daily practice."
            },
            {
                "id": "onboarding_native_lang",
                "text": "What is your native language?"
            },
            {
                "id": "onboarding_app_lang",
                "text": "Would you like to keep the app interface in English for full immersion, or switch it to Spanish?"
            },
            {
                "id": "onboarding_name",
                "text": "Awesome! What should I call you?"
            },
            {
                "id": "onboarding_goal",
                "text": "Great to meet you! What is your primary learning goal?"
            },
            {
                "id": "onboarding_situations",
                "text": "For your interactive AI Roleplay Scenarios, which real-world situations would you like to master? Select between 3 and 5 topics for the most varied conversations:"
            },
            {
                "id": "onboarding_level",
                "text": "What is your current English level?"
            },
            {
                "id": "onboarding_weekly_goal",
                "text": "How many lessons would you like to target each week? Setting a weekly pace builds steady progress without daily streak pressure."
            },
            {
                "id": "onboarding_reminder",
                "text": "Consistency is key to fluency. Would you like a daily reminder to practice?"
            },
            {
                "id": "onboarding_api_key_intro",
                "text": "GrammarAI uses Google Gemini to give you real-time grammar feedback, exercise corrections, and realistic AI voice roleplay conversations. To unlock these features with dedicated speed, you can connect your own free Gemini API Key. It is optional: if you skip, the app will continue using our shared default key."
            },
            {
                "id": "onboarding_api_key_howto",
                "text": "Getting your free Gemini API Key takes less than 1 minute: Sign in with your Google account at aistudio.google.com, tap the blue 'Get API key' button, click 'Create API key' in a project (it's 100% free), copy the key and paste it below. Tap the button below to open Google AI Studio directly!"
            },
            {
                "id": "onboarding_api_key_prompt",
                "text": "Awesome! Paste your Gemini API Key below. You can test your connection to verify it works before saving:"
            },
            {
                "id": "onboarding_sync",
                "text": "Protect your progress and sync seamlessly across devices with Google & Supabase Cloud Backup."
            }
        ]

def get_exercise_prompt(ex: dict) -> str:
    """Extracts instructional prompt text for an exercise."""
    prompt = ex.get("prompt", "") or ""
    clean = prompt.replace("\n", " ").strip()
    return clean

def get_exercise_target_phrase(ex: dict) -> str:
    """
    Extracts the practice target phrase for an exercise.
    Uses targetPhrase or readingPassage, cleaning newlines and replacing underscores with 'blank'.
    """
    target = ex.get("targetPhrase", "") or ex.get("readingPassage", "") or ""
    if not target.strip():
        # Fallback to correctAnswer if targetPhrase is absent for pronunciation/translate
        ex_type = ex.get("type", "").upper()
        if ex_type in ("PRONUNCIATION", "REPEAT_AFTER_ME", "TRANSLATE"):
            target = ex.get("correctAnswer", "") or ""
    
    clean = target.replace("\n", " ").strip()
    clean = re.sub(r'_+', 'blank', clean)
    return clean

def get_exercise_text(ex: dict, allow_all_types: bool = False) -> str:
    """Legacy alias for backward compatibility, strictly using targetPhrase."""
    return get_exercise_target_phrase(ex)

def get_exercise_options(ex: dict) -> list[str]:
    """Extracts non-empty multiple choice options for an exercise."""
    options = ex.get("options", []) or []
    cleaned = []
    for opt in options:
        if isinstance(opt, str) and opt.strip():
            c = opt.replace("\n", " ").strip()
            c = re.sub(r'_+', 'blank', c)
            cleaned.append(c)
    return cleaned

def get_exercise_correct_answer(ex: dict) -> str:
    """Extracts clean correctAnswer for an exercise."""
    ans = ex.get("correctAnswer", "") or ""
    c = ans.replace("\n", " ").strip()
    c = re.sub(r'_+', 'blank', c)
    return c

def normalize_word_token(raw_word: str) -> str:
    """
    Normalizes a single word token from scrambledWords.
    Strips leading and trailing punctuation while preserving internal apostrophes.
    """
    if not raw_word:
        return ""
    # Strip whitespace and common punctuation from edges
    w = raw_word.strip()
    w = re.sub(r'^[^\w\']+|[^\w\']+$', '', w)
    return w.lower().strip()

def get_word_slug(word: str) -> str:
    """Creates a filesystem-safe slug for a word audio file."""
    slug = re.sub(r'[^\w\-]', '_', word)
    return f"w_{slug.strip('_')}"

from .config import is_spanish_text

def is_valid_vocabulary_item(text: str) -> bool:
    """Filters out definitions, long sentences, and grammatical formula notations."""
    if not text or len(text) > 40:
        return False
    if any(c in text for c in [':', ';', '|', '+', '=', '---', '...']):
        return False
    if '(' in text or ')' in text or '/' in text:
        return False
    words = text.strip().split()
    if len(words) > 4:
        return False
    return True

def extract_unique_vocabulary_words(curriculum_files: list[str], filter_pre_a1: bool = False) -> list[tuple[str, str]]:
    """
    Scans curriculum files and extracts all unique normalized words and phrases
    from scrambled_sentence, sentence_reconstruction, flashcards, and select_pairs exercises,
    together with their appropriate synthesis language ('en' or 'es').
    """
    vocabulary: dict[str, str] = {}
    for fpath in curriculum_files:
        is_sit = "situational" in os.path.basename(fpath).lower()
        lessons = load_curriculum_lessons(fpath)
        if filter_pre_a1:
            lessons = [l for l in lessons if str(l.get("cefr", "")).upper() in ("PRE-A1", "A0") or (l.get("module", 99) <= 12)]
        for lesson in lessons:
            for ex in lesson.get("exercises", []):
                ex_type = ex.get("type", "").upper()
                if ex_type in ("SCRAMBLED_SENTENCE", "SENTENCE_RECONSTRUCTION"):
                    for raw_word in ex.get("scrambledWords", []) or []:
                        clean = normalize_word_token(raw_word)
                        if clean and is_valid_vocabulary_item(clean):
                            vocabulary[clean] = "en"
                elif ex_type in ("FLASHCARDS", "FLASHCARD"):
                    for card in ex.get("flashcards", []) or []:
                        clean = normalize_word_token(card.get("front", ""))
                        if clean and is_valid_vocabulary_item(clean):
                            vocabulary[clean] = "en"
                elif ex_type == "SELECT_PAIRS":
                    for pair in ex.get("pairs", []) or []:
                        l_val = pair.get("left", "")
                        r_val = pair.get("right", "")
                        l_clean = normalize_word_token(l_val)
                        r_clean = normalize_word_token(r_val)
                        if is_sit:
                            if l_clean and is_valid_vocabulary_item(l_clean):
                                vocabulary[l_clean] = "en"
                            if r_clean and is_valid_vocabulary_item(r_clean):
                                vocabulary[r_clean] = "es"
                        else:
                            if l_clean and is_valid_vocabulary_item(l_clean):
                                vocabulary[l_clean] = "es" if is_spanish_text(l_clean) else "en"
                            if r_clean and is_valid_vocabulary_item(r_clean):
                                vocabulary[r_clean] = "es" if is_spanish_text(r_clean) else "en"

    return sorted(list(vocabulary.items()), key=lambda x: x[0])

def get_exercise_flashcards(ex: dict) -> list[dict]:
    """Extracts flashcard items with cleaned fields."""
    cards = ex.get("flashcards", []) or []
    cleaned = []
    for c in cards:
        if isinstance(c, dict):
            front = (c.get("front", "") or "").replace("\n", " ").strip()
            back = (c.get("back", "") or "").replace("\n", " ").strip()
            example = (c.get("example", "") or "").replace("\n", " ").strip()
            if front:
                cleaned.append({
                    "front": front,
                    "back": back,
                    "example": example,
                    "phonetic": c.get("phonetic", "")
                })
    return cleaned

def get_exercise_dialogue_lines(ex: dict) -> list[dict]:
    """
    Parses dialogueContext into structured speaker lines.
    Returns list of dicts: [{"speaker": "Barista", "text": "Good morning!...", "is_user": False}]
    """
    context = ex.get("dialogueContext", "") or ""
    if not context.strip():
        return []
    lines = [l.strip() for l in re.split(r'\\n|\n', context) if l.strip()]
    parsed = []
    for line in lines:
        if ":" in line:
            speaker, speech = line.split(":", 1)
            speaker = speaker.strip()
            speech = speech.strip().strip("'").strip('"')
            is_user = speaker.lower() in ("you", "tú", "tu")
            if speech and speech != "___":
                parsed.append({"speaker": speaker, "text": speech, "is_user": is_user})
    return parsed

def extract_unique_scrambled_words(curriculum_files: list[str]) -> list[str]:
    """Alias for extract_unique_vocabulary_words for backward compatibility."""
    return extract_unique_vocabulary_words(curriculum_files)

def load_words_manifest(manifest_path: str) -> dict:
    """Loads word mapping dictionary from JSON manifest."""
    if os.path.exists(manifest_path):
        try:
            with open(manifest_path, "r", encoding="utf-8") as f:
                data = json.load(f)
                return data.get("words", {})
        except Exception:
            return {}
    return {}

def save_words_manifest(manifest_path: str, words_dict: dict):
    """Saves word mapping dictionary to JSON manifest."""
    os.makedirs(os.path.dirname(manifest_path), exist_ok=True)
    payload = {
        "total_words": len(words_dict),
        "updated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "words": words_dict
    }
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(payload, f, indent=2, ensure_ascii=False)

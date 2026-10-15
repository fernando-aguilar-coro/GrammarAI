"""
audio_generator.config
Central configuration for models, voices, directories, and prompts.
"""

import os
import json
import base64
import re

PACKAGE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(PACKAGE_DIR)

def _load_env_file(dotenv_path: str):
    """Load variables from .env file into os.environ if not already present."""
    if not os.path.exists(dotenv_path):
        return
    try:
        with open(dotenv_path, "r", encoding="utf-8") as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                k, v = line.split("=", 1)
                k = k.strip()
                v = v.strip().strip('"').strip("'")
                if k not in os.environ:
                    os.environ[k] = v
    except Exception:
        pass

# Attempt to load from project root .env
_load_env_file(os.path.join(PROJECT_ROOT, ".env"))

def get_default_api_keys() -> list[str]:
    """Retrieves all available Gemini API keys from environment or .env."""
    keys: list[str] = []
    for var in ["GEMINI_API_KEY", "GEMINI_API_KEY_2"]:
        val = os.environ.get(var)
        if val and val.strip() and val.strip() not in keys:
            keys.append(val.strip())
    if not keys:
        return print("error 145: not api keys found")
    return keys

DEFAULT_API_KEYS = get_default_api_keys()
DEFAULT_API_KEY = DEFAULT_API_KEYS[0]

# Gemini TTS models for audio synthesis (rotated on quota 429)
TTS_MODELS = [
    "gemini-3.8-flash-tts",
    "gemini-3.1-flash-tts-preview",
    "gemini-3.8-flash-lite-tts"
]

# Gemini LLM models for text-to-speech script conversion (e.g. converting markdown tables)
TEXT_MODELS = [
    "gemini-3.8-flash",
    "gemini-3.5-flash",
    "gemini-3.1-flash-lite"
]

# Vertex AI models and settings
DEFAULT_VERTEX_LOCATION = "us-central1"
VERTEX_TTS_MODELS = [
    "gemini-3.1-flash-tts-preview"
]
VERTEX_TEXT_MODELS = [
    "gemini-2.5-flash",
    "gemini-2.5-pro"
]
VERTEX_ONBOARDING_MODEL = "gemini-3.1-flash-tts-preview"

def load_vertex_credentials(creds_input: str | None = None) -> dict:
    """
    Loads Google Cloud Service Account JSON credentials for Vertex AI.
    Sources checked in order:
      1. Explicit creds_input (file path, raw JSON, or base64 string)
      2. Environment variable G_JSON_CREDS (base64 string, file path, or raw JSON)
    """
    raw_val = creds_input or os.environ.get("G_JSON_CREDS", "")
    if not raw_val or not str(raw_val).strip():
        raise ValueError(
            "Vertex AI credentials not found. Either provide --vertex-creds or set G_JSON_CREDS in .env"
        )

    val = str(raw_val).strip().strip('"').strip("'")
    if os.path.isfile(val):
        with open(val, "r", encoding="utf-8") as f:
            return json.load(f)

    try:
        decoded = base64.b64decode(val).decode("utf-8")
        return json.loads(decoded)
    except Exception:
        pass

    return json.loads(val)

EXERCISE_VOICES = ["Despina", "Erinome"]

ONBOARDING_MODEL = "gemini-3.8-flash-tts"
ONBOARDING_VOICE = "Despina"
ONBOARDING_FALLBACK_VOICE = "Erinome"

THEORY_VOICE = "Despina"
THEORY_FALLBACK_VOICE = "Erinome"

from .asset_pack_router import (
    BASE_AUDIO_DIR,
    resolve_lesson_audio_dir,
    find_audio_clip,
    get_all_manifest_paths
)

CURRICULUM_FILES = {
    "es": os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "curriculum_lessons.json"),
    "en": os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "curriculum_lessons_en.json")
}

SITUATIONAL_CURRICULUM_FILES = {
    "es": os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "curriculum_situational.json"),
    "en": os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "curriculum_situational.json")
}

def get_curriculum_files(lang: str, curriculum_source: str = "lessons") -> list[str]:
    """Resolves curriculum file paths for the given language and source selection."""
    files = []
    if curriculum_source in ("lessons", "all"):
        f = CURRICULUM_FILES.get(lang)
        if f and os.path.exists(f):
            files.append(f)
    if curriculum_source in ("situational", "all"):
        f = SITUATIONAL_CURRICULUM_FILES.get(lang)
        if f and os.path.exists(f):
            files.append(f)
    return files

GENERIC_PROMPTS = {
    "es": [
        {"id": "prompt_multiple_choice", "text": "Selecciona la opción correcta."},
        {"id": "prompt_fill_blank", "text": "Completa el espacio en blanco."},
        {"id": "prompt_fill_in_blank", "text": "Completa el espacio en blanco."},
        {"id": "prompt_select_pairs", "text": "Empareja las opciones correspondientes."},
        {"id": "prompt_scrambled_sentence", "text": "Ordena las palabras para formar una oración correcta."},
        {"id": "prompt_sentence_reconstruction", "text": "Ordena las palabras para formar una oración correcta."},
        {"id": "prompt_translate", "text": "Traduce la siguiente frase."},
        {"id": "prompt_pronunciation", "text": "Escucha y practica tu pronunciación."},
        {"id": "prompt_repeat_after_me", "text": "Escucha y repite la frase."},
        {"id": "prompt_flashcards", "text": "Tarjetas clave de vocabulario. Toca para ver la traducción."}
    ],
    "en": [
        {"id": "prompt_multiple_choice", "text": "Select the correct option."},
        {"id": "prompt_fill_blank", "text": "Fill in the blank."},
        {"id": "prompt_fill_in_blank", "text": "Fill in the blank."},
        {"id": "prompt_select_pairs", "text": "Match the corresponding pairs."},
        {"id": "prompt_scrambled_sentence", "text": "Put the words in order to form a correct sentence."},
        {"id": "prompt_sentence_reconstruction", "text": "Put the words in order to form a correct sentence."},
        {"id": "prompt_translate", "text": "Translate the following phrase."},
        {"id": "prompt_pronunciation", "text": "Listen and practice your pronunciation."},
        {"id": "prompt_repeat_after_me", "text": "Listen and repeat the phrase."},
        {"id": "prompt_flashcards", "text": "Key vocabulary flashcards. Tap to see the translation."}
    ]
}

# Google Cloud Text-to-Speech Settings
GCP_TTS_ENDPOINT = "https://texttospeech.googleapis.com/v1/text:synthesize"
GCP_VOICES = {
    "en": {
        "primary": "en-US-Journey-F",
        "fallback": "en-US-Neural2-F"
    },
    "es": {
        "primary": "es-US-Neural2-A",
        "fallback": "es-US-Neural2-A"
    }
}

# Correct answer feedback prefix per language
CORRECT_ANSWER_PREFIX = {
    "es": "Respuesta correcta: ",
    "en": "Correct answer: "
}

WORDS_AUDIO_DIR = os.path.join(BASE_AUDIO_DIR, "words")
WORDS_MANIFEST_PATH = os.path.join(WORDS_AUDIO_DIR, "words_manifest.json")

SPANISH_CHARS = set("áéíóúñüÁÉÍÓÚÑÜ¿¡")

SPANISH_LEXICON = {
    "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "en", "para", "por",
    "con", "sin", "sobre", "tras", "hacia", "desde", "hasta", "entre", "contra", "se", "me", "te",
    "nos", "os", "le", "les", "lo", "que", "y", "o", "u", "e", "es", "son", "era", "eran", "fue",
    "fueron", "sea", "sean", "su", "sus", "mi", "mis", "tu", "tus", "nuestro", "nuestra", "nuestros",
    "nuestras", "este", "esta", "estos", "estas", "ese", "esa", "esos", "esas", "aquel", "aquella",
    "aquellos", "aquellas", "haber", "hay", "hacer", "hace", "hizo", "ir", "va", "van", "tener",
    "tiene", "tienen", "estar", "ser", "decir", "dice", "poder", "puede", "pueden", "dar", "da", "ver",
    "ve", "saber", "sabe", "querer", "quiere", "llegar", "pasar", "deber", "poner", "parecer", "quedar",
    "creer", "hablar", "llevar", "dejar", "seguir", "encontrar", "llamar", "venir", "pensar", "salir",
    "volver", "tomar", "conocer", "vivir", "sentir", "tratar", "mirar", "contar", "empezar", "esperar",
    "buscar", "existir", "entrar", "trabajar", "escribir", "perder", "producir", "ocurrir", "entender",
    "pedir", "recibir", "recordar", "terminar", "permitir", "aparecer", "conseguir", "comenzar", "servir",
    "sacar", "necesitar", "mantener", "resultar", "leer", "caer", "cambiar", "presentar", "crear", "abrir",
    "considerar", "oir", "acabar", "ganar", "formar", "traer", "partir", "morir", "aceptar", "realizar",
    "suponer", "comprender", "lograr", "explicar", "preguntar", "tocar", "reconocer", "estudiar",
    "alcanzar", "nacer", "dirigir", "correr", "utilizar", "pagar", "ayudar", "gustar", "jugar", "escuchar",
    "cumplir", "ofrecer", "descubrir", "levantar", "intentar", "respaldar", "rendirse", "continuar",
    "consejo", "muebles", "equipaje", "noticias", "clima", "dinero", "información", "trabajo", "ambos",
    "ninguno", "boleto", "billete", "viaje", "ida", "vuelta", "tarjeta", "cuenta", "mesa", "pedido",
    "camisa", "pantalones", "probador", "cajero", "recibo", "esquina", "semáforo", "intersección", "parada",
    "andén", "cruasán", "cruasan", "pastelito", "película", "entrada", "subtítulo", "farmacia", "analgésico",
    "receta", "renovación", "reserva", "acceso", "despertador", "efectivo", "cambio", "estación", "metro",
    "izquierda", "derecha", "leche", "avena", "dosis", "extra", "café", "azúcar", "favor", "autobús",
    "embarque", "vuelo", "tan", "como", "alto", "rápido", "inteligente", "mucho", "mucha", "muchos",
    "muchas", "gimnasio", "libro", "sentarme", "aquí", "entrar", "permiso", "habilidad", "exceso",
    "ritmo", "tiempo", "acción", "deseo", "pobre", "rico", "oración", "humano", "persona", "cualquier",
    "quienquiera", "adición", "concesión", "aconsejar", "quedese", "aprender", "español", "significa",
    "refiere", "grupo", "totalidad", "precio", "descuento", "tienda", "comida", "cena", "almuerzo",
    "desayuno", "habitacion", "llave", "puerta", "ventana", "ciudad", "calle", "aeropuerto", "hospital"
}

ENGLISH_COMMON_WORDS = {
    "the", "a", "an", "and", "or", "but", "if", "because", "as", "until", "while", "of", "at", "by",
    "for", "with", "about", "against", "between", "into", "through", "during", "before", "after",
    "above", "below", "to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again",
    "then", "once", "here", "there", "when", "where", "why", "how", "all", "any", "both", "each",
    "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only", "own", "same", "so",
    "than", "too", "very", "can", "will", "just", "don't", "should", "now", "i", "you", "he", "she",
    "it", "we", "they", "me", "him", "her", "us", "them", "my", "your", "his", "its", "our", "their",
    "mine", "yours", "hers", "ours", "theirs", "this", "that", "these", "those", "am", "is", "are",
    "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did", "would", "could",
    "go", "goes", "went", "gone", "get", "gets", "got", "make", "take", "see", "come", "know", "think",
    "look", "tell", "work", "call", "try", "ask", "need", "feel", "leave", "put", "keep", "let", "begin",
    "help", "talk", "turn", "start", "show", "hear", "play", "run", "like", "live", "believe", "hold",
    "bring", "must", "write", "sit", "stand", "lose", "pay", "meet", "set", "learn", "watch", "follow",
    "stop", "create", "speak", "read", "allow", "add", "spend", "grow", "open", "walk", "win", "offer",
    "buy", "wait", "serve", "send", "expect", "build", "stay", "fall", "cut", "reach", "coffee", "hotel",
    "airport", "flight", "restaurant", "check", "ticket", "station", "train", "subway", "cold", "colder",
    "small", "rich", "poor", "tall", "fast", "back", "give", "card", "cash", "receipt", "room", "bus",
    "movie", "tea", "water", "brother", "sister", "mother", "father", "doctor", "dash", "mile", "shot"
}

def is_spanish_text(text: str) -> bool:
    """Robustly detects if text is Spanish using character markers, lexicon, and morphology."""
    if not text or not text.strip():
        return False
    if any(c in SPANISH_CHARS for c in text):
        return True

    tokens = re.findall(r'[a-zA-ZáéíóúñüÁÉÍÓÚÑÜ]+', text.lower())
    if not tokens:
        return False

    sp_matches = sum(1 for t in tokens if t in SPANISH_LEXICON)
    en_matches = sum(1 for t in tokens if t in ENGLISH_COMMON_WORDS)

    if sp_matches == 0 and en_matches == 0:
        for t in tokens:
            if t.endswith(("arse", "erse", "irse", "ando", "iendo", "ación", "ición")):
                sp_matches += 1

    return sp_matches > en_matches

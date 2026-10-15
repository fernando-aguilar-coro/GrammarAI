"""
audio_generator.text_transformer
Transforms raw lesson theory markdown (including complex tables, phonetics, rules)
into natural, spoken tutor narration scripts via Gemini LLM, with local caching and chunking.
"""

import os
import re
from .gemini_client import generate_text_with_failover

SYSTEM_INSTRUCTION_ES = """\
Eres un tutor de inglés interactivo, dinámico y amigable para estudiantes hispanohablantes.
Tu tarea es convertir el contenido teórico de una lección de gramática (que contiene tablas markdown, reglas y ejemplos) en un guión narrado para audio.

REGLAS ESTRICTAS DE CONVERSIÓN:
1. TABLAS MARKDOWN: NUNCA leas barras (|), guiones (---) ni nombres de columnas en bruto. En su lugar, explica el contenido de la tabla como un profesor que habla en voz alta (ejemplo: "En la primera persona singular usamos I am o la contracción I'm, que significa soy o estoy...").
2. FORMATO LIMPIO: Elimina absolutamente TODO el formato markdown: encabezados (#, ##), negritas (**), cursivas (*), viñetas, bloques de código, citas (>), etiquetas HTML y emojis.
3. PRONUNCIACIÓN: Explica claramente cómo se pronuncian los ejemplos en inglés cuando la lección incluya guías fonéticas.
4. TONO: Cercano, alentador, conversacional y muy pedagógico.
5. CONCISIÓN: Elabora una explicación fluida y completa de entre 1.5 a 3 minutos hablados (aproximadamente 300 a 600 palabras). No omitas las reglas clave ni las excepciones comunes.
6. SALIDA: Devuelve ÚNICAMENTE el texto que debe ser leído en voz alta por el sintetizador de voz. Sin introducciones como "Aquí tienes el guión:".
"""

SYSTEM_INSTRUCTION_EN = """\
You are an engaging, friendly, and expert English coach for English language learners.
Your task is to transform the lesson theory markdown (which contains markdown tables, grammatical rules, and examples) into a natural spoken audio narration script.

STRICT CONVERSION RULES:
1. MARKDOWN TABLES: NEVER read pipes (|), dashes (---), or raw column headers. Instead, narrate each table row smoothly and conversationally (e.g., "For the first person singular, we use I am, or contracted as I'm, for example: I'm a doctor...").
2. CLEAN TEXT ONLY: Remove all markdown syntax: headers (#, ##), bold (**), italics (*), bullet points, code blocks, quote blocks (>), HTML tags, and emojis.
3. PRONUNCIATION & INTONATION: Integrate pronunciation tips smoothly into spoken sentences.
4. TONE: Warm, encouraging, crystal-clear, and pedagogical.
5. CONCISENESS: Produce a comprehensive spoken lesson between 1.5 and 3 minutes (around 300 to 600 words). Cover the essential rules, common mistakes, and practical examples.
6. OUTPUT: Output ONLY the plain narration text ready to be read aloud by the Text-to-Speech synthesizer. Do not include meta-chatter like "Here is your script:".
"""

def get_theory_script_path(base_audio_dir: str, lang: str, lesson_id: str) -> str:
    """Returns filesystem path for cached theory spoken script."""
    clean_id = re.sub(r'[^\w\-]', '_', lesson_id)
    return os.path.join(base_audio_dir, lang, "theory_scripts", f"{clean_id}.txt")

def clean_script_text(raw_text: str) -> str:
    """Post-processing cleanup to ensure zero markdown or meta-tags remain."""
    text = raw_text.strip()
    # Remove markdown headers
    text = re.sub(r'^#+\s*', '', text, flags=re.MULTILINE)
    # Remove bold / italic markdown
    text = re.sub(r'\*\*([^*]+)\*\*', r'\1', text)
    text = re.sub(r'\*([^*]+)\*', r'\1', text)
    text = re.sub(r'__([^_]+)__', r'\1', text)
    text = re.sub(r'_([^_]+)_', r'\1', text)
    # Remove inline backticks
    text = re.sub(r'`([^`]+)`', r'\1', text)
    # Remove HTML tags
    text = re.sub(r'<[^>]+>', ' ', text)
    # Remove table pipe remnants if any escaped LLM
    text = re.sub(r'\|', ' ', text)
    # Normalize excessive spaces and newlines
    text = re.sub(r'\n{3,}', '\n\n', text)
    text = re.sub(r'[ \t]{2,}', ' ', text)
    return text.strip()

def prepare_theory_narration_script(
    lesson_id: str,
    lesson_title: str,
    theory_markdown: str,
    lang: str,
    api_key: str,
    requests_state: dict,
    base_audio_dir: str,
    force_regen: bool = False,
    delay: float = 1.0,
    vertex_auth = None
) -> str:
    """
    Retrieves or generates the spoken narration script for a lesson's theory.
    Uses local file cache to prevent redundant LLM API calls.
    """
    cache_path = get_theory_script_path(base_audio_dir, lang, lesson_id)

    if not force_regen and os.path.exists(cache_path):
        try:
            with open(cache_path, "r", encoding="utf-8") as f:
                cached = f.read().strip()
                if len(cached) > 50:
                    return cached
        except Exception:
            pass

    # Build prompt for LLM
    if lang == "es":
        system_instruction = SYSTEM_INSTRUCTION_ES
        prompt = (
            f"Título de la lección: {lesson_title}\n\n"
            f"Contenido de teoría gramatical a transformar:\n"
            f"{theory_markdown}"
        )
    else:
        system_instruction = SYSTEM_INSTRUCTION_EN
        prompt = (
            f"Lesson Title: {lesson_title}\n\n"
            f"Grammar theory content to transform into spoken audio script:\n"
            f"{theory_markdown}"
        )

    context_desc = f"Theory Script ({lang}/{lesson_id})"
    raw_script = generate_text_with_failover(
        prompt=prompt,
        api_key=api_key,
        requests_state=requests_state,
        delay=delay,
        context_desc=context_desc,
        system_instruction=system_instruction,
        vertex_auth=vertex_auth
    )

    clean_script = clean_script_text(raw_script)

    os.makedirs(os.path.dirname(cache_path), exist_ok=True)
    with open(cache_path, "w", encoding="utf-8") as f:
        f.write(clean_script)

    return clean_script

def chunk_text_for_tts(text: str, max_chars: int = 600) -> list[str]:
    """
    Splits long narration text into natural, digestible chunks for TTS models,
    splitting on paragraph or sentence boundaries.
    """
    paragraphs = [p.strip() for p in text.split("\n") if p.strip()]
    chunks = []
    current_chunk = []
    current_len = 0

    for para in paragraphs:
        # If single paragraph exceeds max_chars, split by sentences
        if len(para) > max_chars:
            sentences = re.split(r'(?<=[.!?])\s+', para)
            for sentence in sentences:
                s = sentence.strip()
                if not s:
                    continue
                if current_len + len(s) + 1 > max_chars and current_chunk:
                    chunks.append(" ".join(current_chunk))
                    current_chunk = [s]
                    current_len = len(s)
                else:
                    current_chunk.append(s)
                    current_len += len(s) + 1
        else:
            if current_len + len(para) + 1 > max_chars and current_chunk:
                chunks.append(" ".join(current_chunk))
                current_chunk = [para]
                current_len = len(para)
            else:
                current_chunk.append(para)
                current_len += len(para) + 1

    if current_chunk:
        chunks.append(" ".join(current_chunk))

    return [c.strip() for c in chunks if c.strip()]

"""
audio_generator.gcp_tts_client
Direct REST client for Google Cloud Text-to-Speech API using OAuth2 tokens
acquired from the existing Google Cloud / Vertex AI Service Account credentials.
"""

import json
import re
import base64
import time
import ssl
import urllib.request
import urllib.error
from .config import (
    GCP_VOICES,
    GCP_TTS_ENDPOINT,
    CORRECT_ANSWER_PREFIX,
    is_spanish_text
)
from .gemini_client import VertexAuthManager

def create_silence_pcm(duration_ms: int = 300, sample_rate: int = 24000) -> bytes:
    """Generates silent 16-bit mono PCM bytes for the specified duration."""
    num_samples = int(sample_rate * (duration_ms / 1000.0))
    return b"\x00\x00" * num_samples

def synthesize_with_gcp(
    text: str,
    lang: str,
    voice_name: str | None = None,
    vertex_auth: VertexAuthManager | None = None,
    speaking_rate: float = 1.0,
    pitch: float = 0.0,
    sample_rate: int = 24000
) -> bytes:
    """
    Synthesizes speech using Google Cloud Text-to-Speech REST API.
    Returns raw 16-bit mono LINEAR16 PCM/WAV bytes.
    """
    if not text or not text.strip():
        raise ValueError("Cannot synthesize empty text.")

    if vertex_auth is None:
        raise ValueError("vertex_auth (VertexAuthManager) is required for Google Cloud TTS.")

    # Determine voice and language code
    lang_key = "en" if lang.startswith("en") else "es"
    voice_cfg = GCP_VOICES.get(lang_key, GCP_VOICES["en"])
    primary_voice = voice_name or voice_cfg.get("primary", "en-US-Journey-F")
    fallback_voice = voice_cfg.get("fallback", "en-US-Neural2-F")

    voices_to_try = [primary_voice]
    if fallback_voice and fallback_voice != primary_voice:
        voices_to_try.append(fallback_voice)

    last_error = None
    for cur_voice in voices_to_try:
        lang_code = "-".join(cur_voice.split("-")[:2]) # e.g. "en-US" or "es-ES"
        token = vertex_auth.get_token()

        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json; charset=utf-8"
        }

        stripped = text.strip()
        is_single_char = len(stripped) == 1 and stripped.isalpha()
        is_char_seq = bool(re.match(r'^[A-Za-z]([\s\-]+[A-Za-z])+$', stripped))

        input_dict = {"ssml": stripped} if stripped.startswith("<speak>") else (
            {"ssml": f'<speak><say-as interpret-as="characters">{stripped.upper()}</say-as></speak>'}
            if is_single_char
            else (
                {"ssml": f'<speak><say-as interpret-as="characters">{re.sub(r"[\\s\\-]+", " ", stripped).upper()}</say-as></speak>'}
                if is_char_seq
                else {"text": stripped}
            )
        )

        payload = {
            "input": input_dict,
            "voice": {
                "languageCode": lang_code,
                "name": cur_voice
            },
            "audioConfig": {
                "audioEncoding": "LINEAR16",
                "sampleRateHertz": sample_rate,
                "speakingRate": speaking_rate,
                "pitch": pitch
            }
        }

        body = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(GCP_TTS_ENDPOINT, data=body, headers=headers, method="POST")

        try:
            ctx = ssl.create_default_context()
            with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
                resp_json = json.loads(resp.read().decode("utf-8"))
                audio_b64 = resp_json.get("audioContent", "")
                if not audio_b64:
                    raise RuntimeError("GCP TTS returned empty audioContent.")
                return base64.b64decode(audio_b64)
        except urllib.error.HTTPError as e:
            err_body = e.read().decode("utf-8", errors="ignore")
            last_error = RuntimeError(f"GCP TTS HTTP {e.code} for voice {cur_voice}: {err_body}")
            # If 404 or voice error, try fallback voice
            if e.code in (400, 404):
                continue
            raise last_error
        except Exception as e:
            last_error = e
            continue

    raise last_error or RuntimeError(f"Failed to synthesize with GCP TTS for text: '{text[:40]}...'")

def synthesize_correct_answer_feedback(
    correct_answer: str,
    lang: str,
    vertex_auth: VertexAuthManager,
    prefix_cache: dict | None = None
) -> bytes:
    """
    Synthesizes automated correct answer feedback audio:
    '[Correct answer: / Respuesta correcta:] ' + 300ms pause + '[correct_answer]'
    Returns combined PCM bytes.
    """
    lang_key = "en" if lang.startswith("en") else "es"
    prefix_text = CORRECT_ANSWER_PREFIX.get(lang_key, CORRECT_ANSWER_PREFIX["en"])

    from .audio_processing import strip_wav_header_if_present

    # Reuse cached prefix PCM if available to save API calls
    prefix_pcm = None
    if prefix_cache is not None:
        prefix_pcm = prefix_cache.get(lang_key)

    if prefix_pcm is None:
        raw_prefix = synthesize_with_gcp(
            text=prefix_text,
            lang=lang_key,
            vertex_auth=vertex_auth
        )
        prefix_pcm = strip_wav_header_if_present(raw_prefix)
        if prefix_cache is not None:
            prefix_cache[lang_key] = prefix_pcm

    silence_pcm = create_silence_pcm(duration_ms=350, sample_rate=24000)

    ans_lang = "es" if is_spanish_text(correct_answer) else "en"
    raw_answer = synthesize_with_gcp(
        text=correct_answer,
        lang=ans_lang,
        vertex_auth=vertex_auth
    )
    answer_pcm = strip_wav_header_if_present(raw_answer)

    return prefix_pcm + silence_pcm + answer_pcm

def test_gcp_synthesis() -> bool:
    """Self-test function for Google Cloud TTS integration."""
    from .config import load_vertex_credentials
    creds = load_vertex_credentials()
    auth = VertexAuthManager(creds)
    raw = synthesize_with_gcp(
        text="Welcome to Grammar AI",
        lang="en",
        vertex_auth=auth
    )
    print(f"GCP TTS Self-Test Success! Received {len(raw)} PCM bytes.")
    return len(raw) > 1000

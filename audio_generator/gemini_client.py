"""
audio_generator.gemini_client
Client for Google Gemini API: speech synthesis (TTS) and text generation (LLM)
with quota tracking, dynamic model failover, and voice fallback.
"""

import os
import json
import time
import base64
import ssl
import urllib.request
import urllib.error
from .config import (
    TTS_MODELS,
    TEXT_MODELS,
    DEFAULT_API_KEYS,
    VERTEX_TTS_MODELS,
    VERTEX_TEXT_MODELS,
    DEFAULT_VERTEX_LOCATION
)

class VertexAuthManager:
    """
    Manages OAuth2 service account authentication for Google Cloud Vertex AI REST APIs.
    Automatically handles token acquisition and silent refresh upon expiration.
    """
    def __init__(self, creds_dict: dict, location: str = DEFAULT_VERTEX_LOCATION):
        self.creds_dict = creds_dict
        self.project_id = creds_dict.get("project_id", "")
        self.location = location
        self._credentials = None
        self._init_credentials()

    def _init_credentials(self):
        try:
            from google.oauth2 import service_account
        except ImportError:
            raise ImportError(
                "google-auth package is required to use Vertex AI. "
                "Install it via: pip install google-auth"
            )
        self._credentials = service_account.Credentials.from_service_account_info(
            self.creds_dict,
            scopes=["https://www.googleapis.com/auth/cloud-platform"]
        )

    def get_token(self) -> str:
        from google.auth.transport.requests import Request
        if not self._credentials.valid or self._credentials.expired:
            self._credentials.refresh(Request())
        return self._credentials.token

    def get_url(self, model_name: str) -> str:
        return (
            f"https://{self.location}-aiplatform.googleapis.com/v1beta1"
            f"/projects/{self.project_id}/locations/{self.location}"
            f"/publishers/google/models/{model_name}:generateContent"
        )

def _normalize_keys(api_key: str | list[str] | None) -> list[str]:
    """Normalizes input api_key into a clean list of keys."""
    if isinstance(api_key, str):
        keys = [k.strip() for k in api_key.split(",") if k.strip()]
    elif isinstance(api_key, (list, tuple)):
        keys = [k.strip() for k in api_key if k and k.strip()]
    else:
        keys = list(DEFAULT_API_KEYS)
    return keys if keys else list(DEFAULT_API_KEYS)

def _mask_key(key: str) -> str:
    """Masks key for safe logging, showing only last 6 characters."""
    if len(key) <= 8:
        return key
    return f"...{key[-6:]}"

def synthesize_text(
    text: str,
    api_key: str | None = None,
    model_name: str = "gemini-3.1-flash-tts-preview",
    voice_name: str = "Despina",
    fallback_voice: str = "Erinome",
    vertex_auth: VertexAuthManager | None = None
) -> bytes:
    """Calls Gemini API or Vertex AI for speech synthesis with automatic voice fallback."""
    voices_to_try = [voice_name]
    if fallback_voice and fallback_voice != voice_name:
        voices_to_try.append(fallback_voice)

    last_exc = None
    for cur_voice in voices_to_try:
        ctx = ssl.create_default_context()
        if vertex_auth:
            token = vertex_auth.get_token()
            url = vertex_auth.get_url(model_name)
            headers = {
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json"
            }
        else:
            url = f"https://generativelanguage.googleapis.com/v1beta/models/{model_name}:generateContent?key={api_key}"
            headers = {"Content-Type": "application/json"}

        payload = {
            "contents": [{
                "role": "user",
                "parts": [{"text": text}]
            }],
            "generationConfig": {
                "responseModalities": ["AUDIO"],
                "speechConfig": {
                    "voiceConfig": {
                        "prebuiltVoiceConfig": {
                            "voiceName": cur_voice
                        }
                    }
                }
            }
        }

        req = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers=headers
        )

        try:
            with urllib.request.urlopen(req, timeout=90, context=ctx) as resp:
                data = json.loads(resp.read().decode())
                candidate = data.get("candidates", [{}])[0]
                parts = candidate.get("content", {}).get("parts", [])
                audio_part = next((p for p in parts if "inlineData" in p), None)
                if not audio_part:
                    raise RuntimeError(f"No audio inlineData returned in response for '{text[:30]}...'")

                raw_b64 = audio_part["inlineData"].get("data", "")
                return base64.b64decode(raw_b64)
        except urllib.error.HTTPError as e:
            if e.code == 429:
                raise
            last_exc = e
            if cur_voice != voices_to_try[-1]:
                print(f"      [Voice {cur_voice} HTTP {e.code}, attempting fallback {fallback_voice}]", flush=True)
                continue
            raise
        except Exception as e:
            last_exc = e
            if cur_voice != voices_to_try[-1]:
                print(f"      [Voice {cur_voice} failed: {e}, attempting fallback {fallback_voice}]", flush=True)
                continue
            raise

    if last_exc:
        raise last_exc
    raise RuntimeError(f"Failed to synthesize '{text[:30]}...' with voices {voices_to_try}")

def synthesize_with_failover(
    text: str,
    api_key: str | list[str] | None,
    voice_name: str,
    fallback_voice: str,
    requests_state: dict,
    delay: float,
    context_desc: str = "",
    fixed_model: str | None = None,
    vertex_auth: VertexAuthManager | None = None
) -> bytes:
    """
    Attempts speech synthesis across available TTS models and API keys, or strictly with a fixed_model.
    When fixed_model is provided, always uses that exact model (e.g. for consistent onboarding voices).
    Supports Google Cloud Vertex AI if vertex_auth is supplied.
    Rotates round-robin between provided API keys and dynamically handles 429 quota cooldowns per model-key pair.
    """
    if vertex_auth:
        keys = ["vertex"]
        key_labels = [f"Vertex ({vertex_auth.project_id[:16]})"]
        if fixed_model:
            models_to_try = [fixed_model]
        else:
            models_to_try = VERTEX_TTS_MODELS
    else:
        keys = _normalize_keys(api_key)
        key_labels = [f"Key {i + 1}/{len(keys)} ({_mask_key(k)})" for i, k in enumerate(keys)]
        if fixed_model:
            models_to_try = [fixed_model]
        else:
            models_to_try = TTS_MODELS

    num_keys = len(keys)
    start_key_idx = requests_state.setdefault("key_idx", 0)
    total_models = len(models_to_try)
    start_model_idx = 0 if fixed_model else requests_state.get("model_idx", 0)

    cooldowns = requests_state.setdefault("cooldowns", {})

    max_attempts = 8 if fixed_model else 3
    for attempt in range(max_attempts):
        now = time.time()
        all_pairs_cooldown = [
            cooldowns.get(f"{m}::{k}", 0)
            for m in models_to_try
            for k in keys
            if cooldowns.get(f"{m}::{k}", 0) > now
        ]
        total_pairs = total_models * num_keys
        if len(all_pairs_cooldown) == total_pairs:
            min_cooldown = min(all_pairs_cooldown)
            wait_seconds = max(1.0, min_cooldown - now + 1.0)
            if wait_seconds <= 65.0:
                print(f"    [All {total_pairs} model/key quota combinations on 429 cooldown. Waiting {wait_seconds:.1f}s...]", flush=True)
                time.sleep(wait_seconds)
                now = time.time()
            elif not fixed_model:
                raise urllib.error.HTTPError(None, 429, "All TTS model/key quotas exhausted for >65s", None, None)

        for m_offset in range(total_models):
            candidate_model = models_to_try[(start_model_idx + m_offset) % total_models]

            for k_offset in range(num_keys):
                cur_key_idx = (start_key_idx + k_offset) % num_keys
                candidate_key = keys[cur_key_idx]
                cooldown_tag = f"{candidate_model}::{candidate_key}"

                if cooldowns.get(cooldown_tag, 0) > time.time():
                    continue

                key_label = key_labels[cur_key_idx]
                try:
                    print(f"    [TTS] {context_desc} [{candidate_model} | {key_label} | Voice: {voice_name}] -> '{text[:35].strip()}...'", flush=True)
                    raw_audio = synthesize_text(
                        text=text,
                        api_key=candidate_key if not vertex_auth else None,
                        model_name=candidate_model,
                        voice_name=voice_name,
                        fallback_voice=fallback_voice,
                        vertex_auth=vertex_auth
                    )
                    if not fixed_model:
                        requests_state["model_idx"] = (start_model_idx + m_offset + 1) % total_models
                    requests_state["key_idx"] = (cur_key_idx + 1) % num_keys
                    return raw_audio

                except urllib.error.HTTPError as e:
                    if e.code == 429:
                        retry_secs = 25.0
                        try:
                            err_json = json.loads(e.read().decode('utf-8', errors='ignore'))
                            for detail in err_json.get("error", {}).get("details", []):
                                if "retryDelay" in detail:
                                    retry_secs = max(5.0, float(detail["retryDelay"].rstrip("s")))
                        except Exception:
                            pass
                        cooldowns[cooldown_tag] = time.time() + retry_secs
                        print(f"      -> HTTP 429 on {key_label} (model {candidate_model}). Cooling down for {retry_secs:.1f}s. Trying next...", flush=True)
                        time.sleep(0.5)
                        continue
                    elif e.code in (500, 502, 503, 504):
                        for k in keys:
                            cooldowns[f"{candidate_model}::{k}"] = time.time() + 15.0
                        print(f"      -> HTTP {e.code} on TTS model {candidate_model}. Cooling down for 15s.", flush=True)
                        time.sleep(1.0)
                        break
                    else:
                        print(f"      -> HTTP Error {e.code} on {candidate_model}: {e.reason}", flush=True)
                        raise
                except Exception as e:
                    print(f"      -> Error with {candidate_model} on {key_label}: {e}", flush=True)
                    time.sleep(0.5)

    raise RuntimeError(f"Exhausted TTS models/keys for: '{text[:30]}...'")

def generate_text_with_failover(
    prompt: str,
    api_key: str | list[str] | None,
    requests_state: dict,
    delay: float = 1.0,
    context_desc: str = "",
    system_instruction: str = "",
    vertex_auth: VertexAuthManager | None = None
) -> str:
    """
    Calls Gemini Text Generation model to transform raw lesson markdown/tables
    into spoken scripts, with 429 quota rotation across models and available keys or Vertex AI.
    """
    if vertex_auth:
        keys = ["vertex"]
        key_labels = [f"Vertex ({vertex_auth.project_id[:16]})"]
        models_to_try = VERTEX_TEXT_MODELS
    else:
        keys = _normalize_keys(api_key)
        key_labels = [f"Key {i + 1}/{len(keys)} ({_mask_key(k)})" for i, k in enumerate(keys)]
        models_to_try = TEXT_MODELS

    num_keys = len(keys)
    start_key_idx = requests_state.setdefault("text_key_idx", 0)

    start_model_idx = requests_state.get("text_model_idx", 0)
    total_models = len(models_to_try)
    cooldowns = requests_state.setdefault("text_cooldowns", {})

    for attempt in range(4):
        now = time.time()
        all_pairs_cooldown = [
            cooldowns.get(f"{m}::{k}", 0)
            for m in models_to_try
            for k in keys
            if cooldowns.get(f"{m}::{k}", 0) > now
        ]
        total_pairs = total_models * num_keys
        if len(all_pairs_cooldown) == total_pairs:
            min_cooldown = min(all_pairs_cooldown)
            wait_seconds = max(1.0, min_cooldown - now + 1.0)
            if wait_seconds <= 65.0:
                print(f"    [All {total_pairs} text model/key combinations on cooldown. Waiting {wait_seconds:.1f}s...]", flush=True)
                time.sleep(wait_seconds)
                now = time.time()
            else:
                raise urllib.error.HTTPError(None, 429, "All text model/key quotas exhausted for >65s", None, None)

        for m_offset in range(total_models):
            candidate_model = models_to_try[(start_model_idx + m_offset) % total_models]

            for k_offset in range(num_keys):
                cur_key_idx = (start_key_idx + k_offset) % num_keys
                candidate_key = keys[cur_key_idx]
                cooldown_tag = f"{candidate_model}::{candidate_key}"

                if cooldowns.get(cooldown_tag, 0) > time.time():
                    continue

                key_label = key_labels[cur_key_idx]
                try:
                    print(f"    [LLM] {context_desc} [{candidate_model} | {key_label}] Generating spoken narration script...", flush=True)
                    ctx = ssl.create_default_context()
                    if vertex_auth:
                        url = vertex_auth.get_url(candidate_model)
                        headers = {
                            "Authorization": f"Bearer {vertex_auth.get_token()}",
                            "Content-Type": "application/json"
                        }
                    else:
                        url = f"https://generativelanguage.googleapis.com/v1beta/models/{candidate_model}:generateContent?key={candidate_key}"
                        headers = {"Content-Type": "application/json"}

                    payload = {
                        "contents": [{
                            "role": "user",
                            "parts": [{"text": prompt}]
                        }],
                        "generationConfig": {
                            "temperature": 0.3,
                            "topP": 0.95
                        }
                    }
                    if system_instruction:
                        payload["systemInstruction"] = {
                            "parts": [{"text": system_instruction}]
                        }

                    req = urllib.request.Request(
                        url,
                        data=json.dumps(payload).encode("utf-8"),
                        headers=headers
                    )

                    with urllib.request.urlopen(req, timeout=90, context=ctx) as resp:
                        data = json.loads(resp.read().decode())
                        candidate = data.get("candidates", [{}])[0]
                        parts = candidate.get("content", {}).get("parts", [])
                        text_part = next((p["text"] for p in parts if "text" in p), "")
                        if not text_part:
                            raise RuntimeError("No text generated in response")

                        requests_state["text_model_idx"] = (start_model_idx + m_offset + 1) % total_models
                        requests_state["text_key_idx"] = (cur_key_idx + 1) % num_keys
                        return text_part.strip()

                except urllib.error.HTTPError as e:
                    if e.code == 429:
                        retry_secs = 25.0
                        try:
                            err_json = json.loads(e.read().decode('utf-8', errors='ignore'))
                            for detail in err_json.get("error", {}).get("details", []):
                                if "retryDelay" in detail:
                                    retry_secs = max(5.0, float(detail["retryDelay"].rstrip("s")))
                        except Exception:
                            pass
                        cooldowns[cooldown_tag] = time.time() + retry_secs
                        print(f"      -> HTTP 429 on text {key_label} (model {candidate_model}). Cooldown for {retry_secs:.1f}s. Trying next...", flush=True)
                        time.sleep(0.5)
                        continue
                    elif e.code in (500, 502, 503, 504):
                        for k in keys:
                            cooldowns[f"{candidate_model}::{k}"] = time.time() + 15.0
                        print(f"      -> HTTP {e.code} on text model {candidate_model}. Cooling down for 15s.", flush=True)
                        time.sleep(1.0)
                        break
                    else:
                        print(f"      -> HTTP Error {e.code} on text model {candidate_model}: {e.reason}", flush=True)
                        raise
                except Exception as e:
                    print(f"      -> Error with text model {candidate_model} on {key_label}: {e}", flush=True)
                    time.sleep(0.5)

    raise RuntimeError("Exhausted all available text models/keys for script generation.")

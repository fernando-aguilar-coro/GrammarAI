"""
scripts.situational_generator.client
Google AI Studio client using the official google.genai SDK
with automatic key rotation and model failover.
"""

import json
import time
from google import genai
from google.genai import types
from .config import GEN_MODELS, get_api_keys

class GeminiStudioClient:
    """Handles content generation with Google AI Studio using google.genai SDK."""

    def __init__(self, api_keys: list[str] | None = None, models: list[str] | None = None):
        self.api_keys = api_keys or get_api_keys()
        if not self.api_keys:
            raise ValueError(
                "No Gemini API keys found. Set GEMINI_API_KEY or GEMINI_API_KEY_2 in .env"
            )
        self.models = models or GEN_MODELS
        self.key_idx = 0
        self.model_idx = 0
        self._init_sdk_client()

    def _init_sdk_client(self):
        self._genai_client = genai.Client(api_key=self.api_keys[self.key_idx])

    def _rotate_key(self):
        self.key_idx = (self.key_idx + 1) % len(self.api_keys)
        self._init_sdk_client()

    def generate_json(self, prompt: str, system_instruction: str = "") -> dict:
        """
        Generates structured JSON content using Gemini via google.genai SDK.
        Rotates keys on 429 quota exhaustion and fails over across models on errors.
        """
        total_keys = len(self.api_keys)
        total_models = len(self.models)
        last_error = None

        for m_offset in range(total_models):
            cur_model = self.models[(self.model_idx + m_offset) % total_models]

            for k_offset in range(total_keys):
                cur_key_idx = (self.key_idx + k_offset) % total_keys
                cur_key = self.api_keys[cur_key_idx]
                masked_key = f"...{cur_key[-6:]}" if len(cur_key) > 6 else cur_key

                try:
                    config = types.GenerateContentConfig(
                        response_mime_type="application/json",
                        temperature=0.3,
                        top_p=0.95
                    )
                    if system_instruction:
                        config.system_instruction = system_instruction

                    # Client initialized with cur_key
                    active_client = genai.Client(api_key=cur_key)

                    print(f"    [AI Studio SDK] Calling '{cur_model}' with key {masked_key}...", flush=True)
                    response = active_client.models.generate_content(
                        model=cur_model,
                        contents=prompt,
                        config=config
                    )
                    if response and response.text:
                        text = response.text.strip()
                        self.key_idx = cur_key_idx
                        self.model_idx = (self.model_idx + m_offset) % total_models
                        return json.loads(text)

                except Exception as exc:
                    exc_str = str(exc)
                    print(f"      -> SDK error on {cur_model} ({masked_key}): {exc_str[:120]}", flush=True)
                    last_error = exc
                    if "429" in exc_str or "RESOURCE_EXHAUSTED" in exc_str:
                        time.sleep(2.0)
                        continue
                    elif "503" in exc_str or "404" in exc_str:
                        break  # Break to next model

        raise RuntimeError(f"All models and keys failed to generate content via google.genai SDK. Last error: {last_error}")

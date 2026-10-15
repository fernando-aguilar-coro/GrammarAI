"""
scripts.situational_generator.config
Configuration for Google AI Studio models, keys, and curriculum file paths.
"""

import os

PACKAGE_DIR = os.path.dirname(os.path.abspath(__file__))
SCRIPTS_DIR = os.path.dirname(PACKAGE_DIR)
PROJECT_ROOT = os.path.dirname(SCRIPTS_DIR)

def _load_env_file(dotenv_path: str):
    """Loads environment variables from .env if present."""
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

# Load project .env
_load_env_file(os.path.join(PROJECT_ROOT, ".env"))

def get_api_keys() -> list[str]:
    """Retrieves all available Gemini API keys from environment."""
    keys: list[str] = []
    for var in ["GEMINI_API_KEY", "GEMINI_API_KEY_2"]:
        val = os.environ.get(var)
        if val and val.strip() and val.strip() not in keys:
            keys.append(val.strip())
    return keys

# Active models on Google AI Studio
# gemini-3.5-flash-lite is responsive and fast; gemini-3.8-flash as fallback
GEN_MODELS = [
    "gemini-3.5-flash-lite",
    "gemini-3.8-flash",
    "gemini-3.5-flash"
]

DEFAULT_CURRICULUM_PATH = os.path.join(
    PROJECT_ROOT, "app", "src", "main", "assets", "curriculum_situational.json"
)

THEMES_PATH = os.path.join(PACKAGE_DIR, "themes.json")

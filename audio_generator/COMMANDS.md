# GrammarAI Audio Generator — Command Reference & Usage Guide

The `audio_generator` package synthesizes high-quality, normalized AAC (`.m4a`) audio clips for the GrammarAI Android application. It utilizes a **hybrid routing architecture** balancing **Google Cloud TTS** (`en-US-Journey-F` / `es-ES-Neural2-F`) and **Gemini TTS** (`Despina` / `Erinome`) using your existing Vertex AI Service Account credentials.

---

## 🚀 Quick Recipes (Most Common Commands)

### 0. Generate Pre-A1 Foundational Audio (Alphabet A-Z, Numbers 1-20/100/1000, Elementary Realia)
Synthesizes all Pre-A1 letter sounds, numbers (1-20, tens, 100, 1000), flashcard clips, and exercises into **predownloaded base assets** (`app/src/main/assets/audio/words/` and `app/src/main/assets/audio/es/`) using **Google Cloud TTS** (`en-US-Journey-F`):
```bash
python -m audio_generator --category pre_a1 --engine gcp --lang es --limit 500
```
> **Output:** `app/src/main/assets/audio/words/w_<slug>.m4a`, `app/src/main/assets/audio/es/ex_pre_a1_*.m4a`, and updated manifests.

---

### 1. Generate the Scrambled Words Dictionary (914 words)
Synthesizes all unique vocabulary words across scrambled sentence exercises into `audio/words/` using Google Cloud TTS (`en-US-Journey-F`).
```bash
python -m audio_generator --category words --limit 1000
```
> **Output:** `app/src/main/assets/audio/words/w_<slug>.m4a` and `words_manifest.json`.

---

### 2. Generate Exercise Audio (Target Phrases, Options & Correct Feedback)

#### Standard Grammar Lessons (`curriculum_lessons.json`):
```bash
# English Curriculum:
python -m audio_generator --category exercises --lang en --limit 500

# Spanish Curriculum:
python -m audio_generator --category exercises --lang es --limit 500

python -m audio_generator --category exercises --lang all --limit 500
```

#### Situational Lessons (`curriculum_situational.json`):
Generates situational prompt audios, interlocutor dialogue lines (`ex_<id>_dialogue.m4a`), flashcard vocabulary items (`ex_<id>_fc_<idx>.m4a`), example sentences (`ex_<id>_fc_ex_<idx>.m4a`), options, and correct answers:
```bash
# Situational lessons (Spanish / bilingual context):
python -m audio_generator --category exercises --curriculum situational --lang es --limit 300

# Both standard and situational curricula in one run:
python -m audio_generator --category exercises --curriculum all --lang es --limit 500
```

---

### 3. Generate Lesson Theory Narrations (Gemini TTS)
Converts markdown lessons into natural spoken scripts using LLM preprocessing, then synthesizes via Gemini TTS.
> **Note**: Uses **Google AI Studio** API keys by default. To use **Google Cloud Vertex AI** instead, add the `--vertex` flag.

```bash
# English lessons (uses Google AI Studio by default)
python -m audio_generator --category theory --lang en --limit 100

# Spanish lessons (uses Google AI Studio by default)
python -m audio_generator --category theory --lang es --limit 100

# Using Google Cloud Vertex AI explicitly
python -m audio_generator --category theory --lang en --limit 100 --vertex

# Force regenerating the text narration scripts
python -m audio_generator --category theory --lang es --force-script-regen --limit 100
```

---

### 4. Generate Generic Prompts & Onboarding
Synthesizes instructional prompts (`prompt_multiple_choice.m4a`, etc.) and onboarding coach clips:
```bash
# Generic UI Prompts
python -m audio_generator --category generic_prompts --lang all

# Onboarding Coach Dialogues
python -m audio_generator --category onboarding --lang all
```

---

### 5. Filter by Module, Specific Lessons, or Exercise Types
```bash
# Only Module 101 lessons
python -m audio_generator --category exercises --lang en --module 101

# Only the first 3 lessons
python -m audio_generator --category exercises --lang es --max-lessons 3

# Only Pronunciation and Translation exercises
python -m audio_generator --category exercises --lang en --types pronunciation,translate
```

---

## ⚙️ Complete CLI Options Reference

| Argument | Default | Allowed Values | Description |
| :--- | :--- | :--- | :--- |
| `--category` | `all` | `words`, `exercises`, `theory`, `generic_prompts`, `onboarding`, `all` | Specific category of audio to process |
| `--curriculum` | `lessons` | `lessons`, `situational`, `all` | Curriculum source to process (`lessons`: standard, `situational`: real-world scenarios, `all`: both) |
| `--lang` | `all` | `en`, `es`, `all` | Target curriculum language |
| `--engine` | `auto` | `auto`, `gcp`, `gemini` | **`auto`**: Routes exercises & words to GCP TTS, and theory to Gemini TTS.<br/>**`gcp`**: Forces Google Cloud TTS.<br/>**`gemini`**: Forces Gemini TTS. |
| `--limit` | `100` | Integer | Maximum new API synthesis requests to make in this run |
| `--delay` | `0.5` | Float (seconds) | Pause between consecutive requests (prevents rate limits) |
| `--no-options` | `False` | Flag | Omits synthesizing multiple-choice options (`ex_<id>_opt_<idx>.m4a`) |
| `--no-correct-feedback` | `False` | Flag | Omits synthesizing automated correct answer clips (`ex_<id>_correct.m4a`) |
| `--module` | `None` | Integer (e.g. `101`, `800`) | Restricts processing to a specific module number |
| `--max-lessons` | `0` (all) | Integer | Restricts processing to the first N lessons |
| `--types` | `all` | Comma-separated | Filter exercise types (`multiple_choice,fill_blank,flashcards,translate,pronunciation`, etc.) |
| `--force-script-regen` | `False` | Flag | Re-runs the LLM script transformer on theory markdown even if script exists |
| `--vertex` | Auto-detected | Flag | Force Vertex AI backend |
| `--vertex-creds` | `None` | Path or Base64 | Explicit Service Account JSON (defaults to `G_JSON_CREDS` in `.env`) |
| `--api-key` | `None` | String | Gemini Developer API key(s) |

---

## 🎙️ Engine & Voice Routing Map

| Content Type | Engine | Primary Voice | Fallback Voice |
| :--- | :--- | :--- | :--- |
| **Scrambled & Vocabulary Words (`words/`)** | Google Cloud TTS | `en-US-Journey-F` | `en-US-Neural2-F` |
| **Exercise Target Phrases (`en`)** | Google Cloud TTS | `en-US-Journey-F` | `en-US-Neural2-F` |
| **Exercise Target Phrases (`es`)** | Google Cloud TTS | `es-ES-Neural2-F` | `es-US-Neural2-A` |
| **Flashcard Key Words (`ex_<id>_fc_<idx>.m4a`)** | Google Cloud TTS | `en-US-Journey-F` | `en-US-Neural2-F` |
| **Flashcard Examples (`ex_<id>_fc_ex_<idx>.m4a`)**| Google Cloud TTS | `en-US-Journey-F` | `en-US-Neural2-F` |
| **Interlocutor Dialogue (`ex_<id>_dialogue.m4a`)** | Google Cloud TTS | `en-US-Journey-F` | `en-US-Neural2-F` |
| **Multiple Choice Options** | Google Cloud TTS | `en-US-Journey-F` / `es-ES-Neural2-F` | Neural2 Fallback |
| **Correct Answer Feedback** | Google Cloud TTS | `en-US-Journey-F` / `es-ES-Neural2-F` | Neural2 Fallback |
| **Generic UI Prompts** | Google Cloud TTS | `en-US-Journey-F` / `es-ES-Neural2-F` | `Despina` |
| **Lesson Theory Narrations** | Gemini TTS | `Despina` | `Erinome` |
| **Onboarding Coach Dialogues**| Gemini TTS | `Despina` | `Erinome` |

---

## 📂 Output Asset Structure in Android App

```
app/src/main/assets/audio/
├── words/
│   ├── words_manifest.json         # Mapping: {"beautiful": "words/w_beautiful.m4a", ...}
│   ├── w_beautiful.m4a             # 32 kbps mono AAC word clip (~3 KB)
│   └── w_progress.m4a
├── en/
│   ├── generated_manifest.json     # Manifest tracking generated clip IDs
│   ├── prompt_<type>.m4a           # Generic instruction prompts
│   ├── ex_<id>.m4a                 # Exercise targetPhrase audio
│   ├── ex_<id>_phrase.m4a          # Target phrase / reading passage
│   ├── ex_<id>_dialogue.m4a        # Interlocutor line in dialogueContext
│   ├── ex_<id>_fc_0.m4a            # Flashcard front word audio
│   ├── ex_<id>_fc_ex_0.m4a         # Flashcard example sentence audio
│   ├── ex_<id>_opt_0.m4a           # Option audio clips
│   ├── ex_<id>_correct.m4a         # Automated "Correct answer: [answer]" feedback
│   └── theory_<lessonId>.m4a       # Full lesson theory narration
└── es/
    ├── generated_manifest.json
    ├── prompt_<type>.m4a
    ├── ex_<id>.m4a
    ├── ex_<id>_phrase.m4a
    ├── ex_<id>_dialogue.m4a
    ├── ex_<id>_fc_0.m4a
    ├── ex_<id>_fc_ex_0.m4a
    ├── ex_<id>_opt_0.m4a
    ├── ex_<id>_correct.m4a         # Automated "Respuesta correcta: [answer]" feedback
    └── theory_<lessonId>.m4a
```

---

## 💡 Deduplication & Caching

The generator automatically skips already synthesized files:
1. **Manifest & Disk Check:** If an ID exists in `generated_manifest.json` (or `words_manifest.json`) and the `.m4a` file is present on disk with valid size, it is skipped with **0 API calls**.
2. **Text-Level Deduplication:** If different exercises share the exact same target sentence, prompt, or option, the existing audio file is copied locally with **0 API calls**.

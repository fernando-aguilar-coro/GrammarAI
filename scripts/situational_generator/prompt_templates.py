"""
scripts.situational_generator.prompt_templates
Minimalist system prompts and concise JSON schema for situational lessons.
All redundant/static fields are omitted here and automatically populated by validator.py.
"""

SYSTEM_INSTRUCTION = """You are an expert English Language Teaching (ELT) curriculum designer for GrammarAI.
Generate situational English vocabulary and conversation practice for Spanish speakers.

Pedagogical rules:
- Authentic, real-world conversational English (CEFR A1/A2/B1).
- Explanations in natural, clear Spanish.
- Flashcards: 4 key terms with English front, Spanish back, IPA phonetics, and English example sentence with Spanish translation.
- Select pairs: 4 English words with their Spanish translation.
- Fill blank: Natural sentence containing '___' with missing target word, options (including the answer), and Spanish explanation.
- Multiple choice: Realistic speaker question/greeting, the natural correct reply, 3 distractors, and Spanish explanation.
- Pronunciation: High-frequency conversational phrase to say aloud with IPA phonetics.
"""

def build_part_prompt(topic_title: str, topic_summary: str, cefr: str, part_num: int, total_parts: int) -> str:
    """
    Returns a ultra-lean prompt asking ONLY for the creative content.
    All structural IDs, boilerplate defaults, and empty arrays are handled automatically by validator.py.
    """
    return f"""Generate content for Part {part_num} of {total_parts}:
Topic: {topic_title}
Focus: {topic_summary}
CEFR: {cefr}

Output a single JSON object with ONLY these fields:
{{
  "title": "{topic_title}",
  "summary": "{topic_summary}",
  "flashcards": [
    {{
      "front": "English term",
      "back": "Traducción",
      "phonetic": "/IPA/",
      "example": "English example.",
      "exampleTranslation": "Traducción del ejemplo."
    }}
  ],
  "pairs": [
    {{ "left": "English 1", "right": "Español 1" }},
    {{ "left": "English 2", "right": "Español 2" }},
    {{ "left": "English 3", "right": "Español 3" }},
    {{ "left": "English 4", "right": "Español 4" }}
  ],
  "fill_blank": {{
    "sentence": "English sentence with ___ where the answer goes.",
    "correctAnswer": "answer",
    "options": ["answer", "distractor1", "distractor2", "distractor3"],
    "explanation": "Breve explicación en español."
  }},
  "multiple_choice": {{
    "speaker": "Cashier",
    "dialoguePrompt": "What the interlocutor says to you in English",
    "correctAnswer": "Natural polite English response",
    "distractors": ["Unnatural 1", "Unnatural 2", "Unnatural 3"],
    "explanation": "Explicación en español de por qué es la respuesta más natural."
  }},
  "pronunciation": {{
    "phrase": "Conversational sentence to pronounce aloud",
    "phonetic": "[IPA transcription]",
    "explanation": "Consejo de pronunciación en español."
  }}
}}
"""

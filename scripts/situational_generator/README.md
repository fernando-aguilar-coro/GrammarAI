# GrammarAI Situational Generator Pipeline (Google AI Studio)

Pipeline modular para generación automatizada de currículum de inglés situacional alineado con CEFR (A1-B1) usando Google AI Studio (Gemini).

---

## Estructura

```
scripts/situational_generator/
├── __init__.py
├── __main__.py               # CLI entrypoint
├── config.py                 # Carga de API keys (.env), modelos y rutas
├── client.py                 # Cliente Gemini con Google GenAI SDK + failover REST
├── prompt_templates.py       # Prompts pedagógicos y schema estricto de 5 ejercicios
├── validator.py              # Validador y normalizador de esquemas
├── generator.py              # Orquestador de generación por partes
├── merger.py                 # Integrador seguro en curriculum_situational.json
└── themes.json               # Catálogo de temas situacionales listos para generar
```

---

## Requisitos Previos

- Claves de Gemini configuradas en `.env`:
  ```ini
  GEMINI_API_KEY=tu_clave_de_google_ai_studio
  GEMINI_API_KEY_2=segunda_clave_opcional
  ```

---

## Comandos y Ejemplos de Uso

### 1. Ayuda y opciones disponibles
```bash
python -m scripts.situational_generator --help
```

### 2. Generar un tema específico (3 partes por defecto)
```bash
python -m scripts.situational_generator --theme "En el Hotel: Registro y Estadía" --cefr "A1"
```

### 3. Modo prueba (Dry-run) sin escribir en el JSON
```bash
python -m scripts.situational_generator --theme "En el Restaurante: Ordenando Comida" --cefr "A1" --dry-run
```

### 4. Generación por lotes desde el catálogo `themes.json`
```bash
# Genera los primeros 2 temas del catálogo (p. ej. Aeropuerto y Hotel)
python -m scripts.situational_generator --batch 2
```

### 5. Generar currículum y sintetizar audio automáticamente
```bash
python -m scripts.situational_generator --theme "En la Farmacia: Medicamentos" --cefr "A2" --generate-audio
```

---

## Estructura de cada lección situacional generada

Cada parte de la situación incluye exactamente 5 ejercicios:
1. `flashcards`: 4 tarjetas clave (frente, reverso en español, fonética IPA, oración de ejemplo y traducción).
2. `select_pairs`: 4 pares interactivos inglés $\leftrightarrow$ español.
3. `fill_blank`: Oración cloze con `___` y `targetPhrase` alineado para TTS.
4. `multiple_choice`: Diálogo con interlocutor nativo (`targetPhrase` limpio para audio de contexto) y 4 opciones pragmáticas.
5. `pronunciation`: Frase clave de alta frecuencia con transcripción fonética IPA estricta.

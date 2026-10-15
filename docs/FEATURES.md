# GrammarAI — Feature Specification & Architecture (Pure BYOK & AI Cooperation)

---

## 📑 Table of Contents
1. [Executive Overview & Pedagogical Philosophy](#1-executive-overview--pedagogical-philosophy)
2. [Pure BYOK & Zero-Backend Architecture](#2-pure-byok--zero-backend-architecture)
3. [Fluently-Style Conversational Onboarding](#3-fluently-style-conversational-onboarding)
4. [Unified Progressive Feed & Situational Injection](#4-unified-progressive-feed--situational-injection)
5. [Lesson Flow: Video Masterclass to Conversational Practice](#5-lesson-flow-video-masterclass-to-conversational-practice)
6. [Live Conversational AI Engine & Situational Micro-Curriculum (Implemented)](#6-live-conversational-ai-engine--situational-micro-curriculum-implemented)
7. [Pedagogical Immersion & Dual Translation Engine](#7-pedagogical-immersion--dual-translation-engine)
8. [Multi-Skill Spiderweb Analytics (Listening, Speaking, Reading, Writing)](#8-multi-skill-spiderweb-analytics-listening-speaking-reading-writing)
9. [Curated Video Distribution Schema](#9-curated-video-distribution-schema)
10. [Android Native Tech Stack & MVI Architecture](#10-android-native-tech-stack--mvi-architecture)
11. [Multi-Track Curriculum Architecture (Foundation, Immersion, Situational)](#11-multi-track-curriculum-architecture-foundation-immersion-situational)

---

## 1. Executive Overview & Pedagogical Philosophy

**GrammarAI** is an adult-oriented English mastery platform built natively for Android using **Kotlin** and **Jetpack Compose**. It is specifically tailored for **Intermediate learners (CEFR B1 – B2)** who have acquired basic vocabulary but struggle with natural syntax, nuanced tenses, fluent spoken reactions, and everyday real-world confidence.

### Core Pedagogical & Design Pillars:
* **Intermediate English Immersion**: All explanations, video breakdowns, exercise prompts, and conversational scenarios are delivered directly in natural English. Native language is never used as an invasive crutch; it is reserved strictly for on-demand word/phrase translations and user-requested AI explanations.
* **100% Pure BYOK (Bring Your Own Key)**: Zero custom backend servers, zero subscription markups, and zero user tracking. The application connects directly from the user's Android device to Google Gemini APIs using the learner's personal API key stored securely on-device.
* **Online-Only by Nature (No Offline Mode)**: Due to the core architecture relying on real-time bidirectional audio streaming via Gemini 2.5 Flash Native Audio Dialog, instant syntactic evaluations via Gemini 3.5 Flash Lite, and streaming YouTube video masterclasses, the app requires an active internet connection to function. It does not offer an offline mode.
* **Fluently-Inspired Visual & Conversational Design**: Clean, high-impact aesthetic utilizing spacious, rounded rectangular cards, conversational onboarding, and distraction-free learning streams without gamified gimmicks (no hearts, no energy timers, no cartoon mascots).
* **Deep AI Cooperation & Conversational Drills**: Language practice is not static multiple-choice testing. Exercises integrate dynamic AI roleplay, "Repeat after me" shadowing drills, and "How would you respond to...?" contextual spoken dialogues powered by real-time audio AI.

---

## 2. Pure BYOK & Zero-Backend Architecture

GrammarAI eliminates all custom backends (no custom auth, no Supabase, no sync servers). The entire platform runs client-side on Android with direct external connections limited to:
1. **Google Gemini LLM Endpoints** (via user's BYOK key).
2. **Curated Video Links** (static YouTube masterclasses).

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID CLIENT APPLICATION                      │
│                                                                        │
│   ┌─────────────────────┐   ┌──────────────────────────────────────┐  │
│   │   Room Database     │   │     EncryptedSharedPreferences       │  │
│   │ (Curriculum, Notes, │   │    (BYOK Gemini API Key, Profile)    │  │
│   │   Skill Analytics)  │   └──────────────────────────────────────┘  │
│   └──────────┬──────────┘                      │                       │
│              │                                 │                       │
│   ┌──────────▼─────────────────────────────────▼───────────────────┐  │
│   │                 Encapsulated AI Client Layer                   │  │
│   │    ┌─────────────────────────┐     ┌──────────────────────┐    │  │
│   │    │  AudioDialogClient      │     │  LlmClient           │    │  │
│   │    │  (Gemini 2.5 Flash      │     │  (Gemini 3.5 Flash   │    │  │
│   │    │   Native Audio Dialog)  │     │   Lite - Text/JSON)  │    │  │
│   │    └────────────┬────────────┘     └──────────┬───────────┘    │  │
│   └─────────────────┼─────────────────────────────┼────────────────┘  │
└─────────────────────┼─────────────────────────────┼────────────────────┘
                      │                             │
                      ▼                             ▼
         [Gemini 2.5 Native Audio]       [Gemini 3.5 Flash Lite]
           (Bidirectional Voice)           (Corrections, Theory)
```

### Dedicated Model Allocation:
* **Gemini 2.5 Flash Native Audio Dialog**:
  * Powers real-time spoken conversational drills, shadowing exercises ("Repeat after me"), and interactive spoken roleplay with sub-second latency and natural prosody.
* **Gemini 3.5 Flash Lite**:
  * High-speed, cost-effective text and structured JSON engine.
  * Generates on-demand grammar explanations, error root-cause analyses, contextual sentence reformulations, and deep grammar breakdowns.
* **Encapsulated Architecture for Testability**:
  * Both models are accessed via clean Kotlin interfaces (`AudioDialogClient` and `LlmClient`).
  * Allows effortless offline mocking, local unit testing, and future extensibility without altering MVI ViewModels or presentation logic.

---

## 3. Fluently-Style Conversational Onboarding

Rather than static registration forms, first-time users are welcomed by a modern, conversational chat interface inspired by *Fluently: AI Language Learning*.

```
┌────────────────────────────────────────────────────────┐
│                CONVERSATIONAL ONBOARDING               │
│                                                        │
│  🤖 "Hi! I'm your AI English Co-Pilot. What should I   │
│      call you?"                                        │
│  👤 "Alex"                                             │
│                                                        │
│  🤖 "Nice to meet you, Alex! What is your native       │
│      language?"                                        │
│  [ Spanish 🇪🇸 ]  [ Portuguese 🇧🇷 ]  [ French 🇫🇷 ] ...   │
│                                                        │
│  🤖 "Got it! Where are you located?"                   │
│  [ Mexico ]  [ Spain ]  [ Colombia ]  [ Other... ]     │
│                                                        │
│  🤖 "What is your main English challenge right now?"   │
│  [ Speaking Fluency ]  [ Grammar Nuances ]  [ Work ]   │
│                                                        │
│  🤖 "Finally, connect your Gemini API Key to enable    │
│      live spoken drills. (You can also try demo mode)" │
│  [ 🔑 Paste AIzaSy... ]  [ Test Key ]  [ Skip for now] │
└────────────────────────────────────────────────────────┘
```

### Onboarding Mechanics:
1. **Interactive Step-by-Step Flow**:
   * Conversational agent asks one question at a time with smooth typing animations and quick-response chips.
   * Captured attributes: Name, Native Language, Country/Region, Current English Level (B1/B2 focus), Primary Goal (Job Interviews, Professional Meetings, Daily Living, Travel).
2. **BYOK Activation Step**:
   * As the final conversational step, the agent explains that GrammarAI uses personal BYOK to provide unlimited live audio at zero platform markup.
   * Provides immediate connectivity verification (`Test Key`) or allows entering with sample mock mode.
3. **Local Storage**:
   * Profile preferences and encrypted API key are stored locally in Jetpack DataStore / `EncryptedSharedPreferences`.
   * Subsequent app launches skip onboarding directly to the unified feed.

---

## 4. Unified Progressive Feed & Situational Injection

The main dashboard is organized as a **Unified Progressive Stream** of large rectangular cards inspired by *Fluently*. To reduce cognitive burnout and lower the friction to opening the app daily, grammar concepts are interleaved with **Situational Vocabulary Cards** (`curriculum_situational.json`) injected dynamically right beneath the learner's active lesson.

```
┌────────────────────────────────────────────────────────┐
│                     GRAMMARAI HUB                      │
│                                                        │
│  [Top Bar]  Greeting • B1 Intermediate • 🔑 BYOK Active │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 📘 GRAMMAR PILLAR • MODULE 3                     │  │
│  │ Past Continuous vs Simple Past                   │  │
│  │ Master background narrative vs interruptions     │  │
│  │ ⏱️ 6 mins  •  🎥 Video  •  🎙️ Spoken Practice   │  │
│  │ [ Open Lesson ➔ ]                                │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ ☕ CAFETERÍA • PARTE 1/3              ● ○ ○  2 min │  │
│  │ El Café: Orden Básica                           │  │
│  │ Pide tu café favorito en segundos con cortesía.   │  │
│  │ 4 ejercicios rápidos (Flashcards, Parejas)       │  │
│  │ [ Empezar ➔ ]                                    │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 📘 GRAMMAR PILLAR • MODULE 4                     │  │
│  │ Third Conditional: Regrets & Hypotheticals       │  │
│  │ Formulating past unfulfilled conditions (had+V3) │  │
│  │ ⏱️ 8 mins  •  🎥 Video  •  🎙️ Spoken Practice   │  │
│  │ [ Open Lesson ➔ ]                                │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### Card Anatomy (Fluently Style):
* **Grammar Pillar Card**: Deep conceptual foundation with video masterclass, syntax formulas, and structured drills.
* **Situational Vocabulary Card (`SituationalVocabularyCard`)**:
  * **Visual Design**: High-contrast dark purple/violet glowing gradient (`#261543` ➔ `#140D2B`) with amber/violet neon borders.
  * **Stepper Pips (`● ○ ○`)**: Visual progress indicators showing multi-part real-world thematic journeys (e.g., *Cafetería*, *El Metro*, *Aeropuerto*).
  * **Micro-Commitment**: Labeled as a 2-minute quick session ("2 min • 4-5 ejercicios rápidos") designed specifically to defeat psychological friction and study resistance.
* **Status Indicators**: Unstarted, in progress, or mastered with emerald accents.

---

## 5. Lesson Flow: Video Masterclass to Conversational Practice

When a user taps any large rectangle from the feed, the app opens the dedicated full-screen Lesson environment:

```
┌────────────────────────────────────────────────────────┐
│                      LESSON VIEW                       │
│                                                        │
│  [← Back]               Lesson 14: Past Continuous     │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  [ YouTube Masterclass Player Mock ]             │  │
│  │  "Past Continuous vs Simple Past in Real Life"   │  │
│  │  ⏱️ 04:15  •  Chapter 1: The Timeline Formula     │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  📖 CORE TAKEAWAY NOTES                                │
│  • Formula: was/were + verb-ing                        │
│  • Use for background action in progress               │
│  • Use Simple Past for the event that interrupts it    │
│  • Example: "I was sending an email when my boss called"│
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  🚀 START PRACTICE (Interactive Drills)         │  │
│  │     Includes "Repeat After Me" & AI Dialogue     │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### 2-Stage Flow:
1. **Stage 1: Curated Video Masterclass**:
   * Embedded YouTube player presenting concise native English instruction (3–6 minutes).
   * Key takeaway card beneath summarizing grammar formulas and native examples.
2. **Stage 2: "Start Practice" Button**:
   * Tapping the prominent button immediately launches the interactive exercise runner featuring conversational drills.

---

## 6. Live Conversational AI Engine & Situational Micro-Curriculum (Implemented)

The conversational system is **fully implemented and operational** natively within Android, connecting the user directly to Gemini via bidirectional real-time audio and structured micro-drills:

```
┌────────────────────────────────────────────────────────┐
│             LIVE AI VOICE CONVERSATION SESSION         │
│                                                        │
│  [ Topic: Ordering at a Coffee Shop ]  [ Change Topic] │
│                                                        │
│             (( 🌊 LiveVoiceVisualizer 🌊 ))            │
│                 [ AI Speaking / Listening ]            │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ 💬 LiveSpeechCaptionsBox                         │  │
│  │ AI: "What kind of roast do you prefer today?"     │  │
│  │ You: "Could I get a medium dark roast with oat?"  │  │
│  └──────────────────────────────────────────────────┘  │
│                                                        │
│  [ 🎙️ Mic Mute ]     [ ⏹️ End Session ]     [ ⚙️ Speed ]│
└────────────────────────────────────────────────────────┘
```

### 1. Live Voice Engine Architecture (`ui.practice.conversation` & `data.remote`)
* **Bidirectional PCM WebSocket Client (`GeminiLiveConversationSession`)**:
  * Communicates directly with Gemini 2.5 Flash Native Audio over secured WebSockets with personal BYOK.
  * Streams raw 16kHz PCM audio from the Android microphone and plays back incoming 24kHz synthesized speech with ultra-low latency (<800ms response time).
* **Live Session Orchestrator (`LiveConversationCoordinator`)**:
  * Manages audio focus, hardware microphone buffers, automatic silence detection / turn-taking, and session lifecycle tied to Android Jetpack Compose lifecycles.
* **Audio Wave Visualizer (`LiveVoiceVisualizer`)**:
  * Real-time audio waveform and pulsating ripple rings reacting smoothly to voice energy levels, providing immediate tactile reassurance during oral practice.
* **Live Speech Captions (`LiveSpeechCaptionsBox`)**:
  * On-screen bidirectional captions showing real-time transcribed user utterances and AI responses for auditory accessibility and comprehension scaffolding.
* **Dynamic Topic Switching (`LiveTopicSelectorBar` & `ChangeTopicDialog`)**:
  * Learners can switch conversation scenarios mid-session across curated tracks (`CuratedConversationTopics`) or input custom conversational scenarios via `CustomTopicInputSection`.
* **Post-Session Pedagogical Review (`ConversationReviewCard`)**:
  * Upon ending a conversation, Gemini analyzes the full dialogue transcript, returning a detailed review card with fluency scores, grammar error corrections, vocabulary expansions, and natural phrasing suggestions.

---

### 2. Situational Real-Life Micro-Curriculum (`curriculum_situational.json`)
To eliminate the psychological resistance of opening the app for long, academic grammar lessons, GrammarAI features a dedicated **Situational Micro-Curriculum**:
* **36 High-Impact Micro-Lessons**: Organized across real-world thematic units (e.g., *Cafetería*, *El Metro*, *Restaurante*, *Aeropuerto*, *Emergencias*, *Small Talk*).
* **180 Rapid-Fire Exercises**:
  * `flashcards`: Visual & audio vocabulary flashcards with IPA phonetics and contextual native translations.
  * `select_pairs`: Tactile word-to-translation matching pairs for instant retrieval.
  * `fill_blank`: High-frequency conversational sentence completions.
  * `pronunciation`: Spoken audio drills with phoneme guidance.
* **Bite-Sized Duration (2 Minutes)**: Each situational part takes under 120 seconds to complete, providing daily instant gratification and maintaining study momentum without fatigue.

---

## 7. Pedagogical Immersion & Dual Translation Engine

Because GrammarAI targets intermediate learners (B1–B2), the entire interface and curriculum are in English. However, encountering unknown vocabulary or nuanced expressions is inevitable. GrammarAI provides a **Dual Translation Engine** that acts strictly on-demand:

```
┌────────────────────────────────────────────────────────┐
│                 DUAL TRANSLATION ENGINE                │
│                                                        │
│  English Prompt:                                       │
│  "The legal team noticed an unexpected discrepancy."   │
│                                     [ 👆 TAP WORD ]    │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │ ⚡ ON-DEVICE ML KIT TRANSLATE (Instant Lookup)    │  │
│  │ "discrepancy" ➔ "discrepancia / divergencia"     │  │
│  │ Pronunciation: /dɪˈskrɛpənsi/ • Noun • B2        │  │
│  │                                                  │  │
│  │ 🤖 DEEP AI CONTEXT (Gemini 3.5 Flash Lite)        │  │
│  │ "In professional contracts, 'discrepancy' refers │  │
│  │ to an inconsistency between two sets of data."    │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────┘
```

### Dual Engine Workflow:
1. **Engine 1: On-Device Google ML Kit Translate (Instant Fast Lookup)**:
   * 100% free, zero translation API overhead.
   * Leverages on-device ML Kit language models for instantaneous word and clause translations.
   * Tapping any word or selecting any sentence snippet instantly displays the direct native translation.
2. **Engine 2: Deep LLM Context (Gemini 3.5 Flash Lite)**:
   * For idioms, phrasal verbs, or cultural nuance, tapping *"Deep AI Explanation"* prompts Gemini 3.5 Flash Lite to explain the pragmatic meaning, register, and contrast with false friends in the learner's native language. (Requires network connection).

---

## 8. Multi-Skill Spiderweb Analytics (Listening, Speaking, Reading, Writing, Vocabulary)

GrammarAI continuously tracks learner proficiency across five core language pillars: **Listening, Speaking, Reading, Writing, and Vocabulary**. Rather than traditional fragmented quiz scores, performance is rendered as an interactive 5-axis **Spiderweb (Radar) Chart** that immediately visualizes the user's highest-performing skill (top strength) alongside priority growth areas.

```
                      [VOCABULARY] (88%)
                             ▲
                            / \
      [WRITING] (74%) ◄────/───\────► [LISTENING] (85%)
                       \  /     \  /
                        \/       \/
                         \       /
                          ▼     ▼
          [READING] (92%)        [SPEAKING] (68%)
          ★ TOP STRENGTH         ⚡ GROWTH PRIORITY
```

### Macro-Skill Dimension Breakdown & AI Evaluation:
1. **Reading Comprehension (Score: 0–100%)**:
   * **Engine**: Powered by Gemini 3.5 Flash Lite generating dynamic 250–400 word intermediate reading passages (articles, corporate emails, workplace dilemmas, narrative stories).
   * **Evaluation**: Interactive multi-tier questions assessing main idea synthesis, inferential nuance, context-clue vocabulary, and tone recognition.
   * **Metrics**: Reading accuracy, comprehension rate, vocabulary depth, and contextual deduction.
2. **Vocabulary & Lexical Range (Score: 0–100%)**:
   * **Engine**: Powered by Gemini 3.5 Flash Lite evaluating contextual collocation, idiomatic precision, phrasal verbs, and CEFR tier discrimination (e.g., upgrading basic verbs to nuanced B2/C1 alternatives).
   * **Evaluation**: Interactive lexical discrimination drills, false-friend identification, prepositional collocations, and contextual cloze tests.
   * **Metrics**: Active lexical recall, polysemy awareness, phrasal verb precision, and idiomatic appropriateness.
3. **Listening Comprehension (Score: 0–100%)**:
   * **Engine**: Powered by Gemini 2.5 Flash Native Audio Dialog streaming natural native-speed audio with realistic cadence, intonation, and reduced speech forms (e.g., "gonna", "should've").
   * **Evaluation**: Spoken comprehension queries, implied intent detection, and auditory nuance identification without relying on pre-rendered subtitles.
   * **Metrics**: Auditory retention, phonological decoding, and contextual inference.
4. **Speaking & Oral Fluency (Score: 0–100%)**:
   * **Engine**: Bidirectional real-time voice streaming with Gemini 2.5 Flash Native Audio.
   * **Evaluation**: Conversational drills ("Repeat After Me", situational responses, roleplay scenarios) evaluated with sub-second feedback.
   * **Metrics**: Pronunciation clarity, phonetic approximation, response latency, and grammatical coherence under conversational pressure.
5. **Writing & Structural Accuracy (Score: 0–100%)**:
   * **Engine**: Gemini 3.5 Flash Lite analyzing written responses, essay prompts, and grammar formulations.
   * **Evaluation**: Real-time grammatical diagnostic, syntax enhancement suggestions, prepositional accuracy, and CEFR-level vocabulary alternatives.
   * **Metrics**: Syntactic precision, morphological accuracy, stylistic register, and clause variety.

### Spiderweb Diagnostic Insights:
* **Top Strength Identification**: Explicitly highlights the learner's highest-ranking skill (e.g., *"Top Strength: Reading (92%)"*).
* **Growth Priority Guidance**: Explicitly isolates the lowest radar axis (e.g., *"Growth Priority: Speaking (68%)"*) and auto-generates 1-tap practice recommendations.
* **Persistent Skill Evolution**: Metrics update reactively in Room Database (`SkillProficiencyEntity`) after every completed exercise session, enabling trend comparison over time.

---

## 9. Curated Video Distribution Schema

To ensure zero custom backend dependency while maintaining dynamic video updates, video metadata is stored in a structured JSON schema:

* **Primary Source**: Bundled asset `curriculum_videos.json` in Android APK `assets/`.
* **Optional Static Update URL**: The app can optionally check a public raw URL (e.g., GitHub Gist / Raw JSON CDN) to refresh video IDs and chapter timestamps without a new app release.

### Sample `curriculum_videos.json` Schema:
```json
[
  {
    "id": "lesson_14_past_continuous",
    "module": 3,
    "type": "GRAMMAR_PILLAR",
    "cefr": "B1",
    "title": "Past Continuous vs Simple Past",
    "summary": "Master background narrative vs interruptions in past events.",
    "durationMinutes": 6,
    "youtube": {
      "videoId": "dQw4w9WgXcQ",
      "startTimestampSeconds": 15,
      "endTimestampSeconds": 270,
      "channel": "English Grammar Masterclass"
    },
    "coreNotesMarkdown": "### Formula\n- `was/were + verb-ing` for continuous background.\n- Simple Past for the interrupting event.\n\n### Example\n* 'I was working when the power went out.'",
    "conversationalDrills": [
      {
        "type": "REPEAT_AFTER_ME",
        "phrase": "I was just reviewing the presentation when you called.",
        "phonetics": "aɪ wəz dʒəst rɪˈvjuːɪŋ ðə ˌprɛzənˈteɪʃən wɛn juː kɔːld"
      },
      {
        "type": "HOW_WOULD_YOU_RESPOND",
        "aiPrompt": "Your colleague asks: 'Why didn't you answer my Slack message at 3 PM?' Respond using the Past Continuous.",
        "expectedPattern": "was/were + verb-ing"
      }
    ]
  },
  {
    "id": "scenario_coffee_ordering",
    "module": 3,
    "type": "EVERYDAY_ACTION",
    "cefr": "B1",
    "title": "Ordering Coffee & Customizing Orders",
    "summary": "Polite requests and modal verbs in daily cafe interactions.",
    "durationMinutes": 4,
    "youtube": {
      "videoId": "dQw4w9WgXcQ",
      "startTimestampSeconds": 30,
      "endTimestampSeconds": 240,
      "channel": "Real English Daily"
    },
    "coreNotesMarkdown": "### Key Phrases\n- 'Could I get a latte with oat milk, please?'\n- 'Would you mind making that extra hot?'",
    "conversationalDrills": [
      {
        "type": "HOW_WOULD_YOU_RESPOND",
        "aiPrompt": "The barista asks: 'What can I get started for you today?' Make a polite order with one customization.",
        "expectedPattern": "Could I get / I'd like"
      }
    ]
  }
]
```

---

## 10. Android Native Tech Stack & MVI Architecture

### Tech Stack:
* **Language**: 100% Kotlin (Coroutines, StateFlow, Flow).
* **UI Framework**: **Jetpack Compose** (Material 3).
* **Architecture**: **MVI (Model-View-Intent)** with Unidirectional Data Flow.
* **Dependency Injection**: Hilt.
* **Local Persistence**: Android Room Database (SQLite) + Jetpack DataStore.
* **Security**: Android Jetpack Security (`EncryptedSharedPreferences` for BYOK keys).
* **AI Client**: Custom encapsulated `LlmClient` (Gemini 3.5 Flash Lite) and `AudioDialogClient` (Gemini 2.5 Flash Native Audio Dialog WebSocket/REST).
* **Translation**: Google ML Kit Digital Ink / Translation On-Device SDK + Gemini Contextual Fallback.
* **Video Playback**: Android YouTube Player / Compose Webview YouTube Embed.
* **i18n**: Android String Resources (`res/values/strings.xml`, `res/values-es/strings.xml`).

### MVI Pattern Structure:
```kotlin
// Immutable UI State
data class LessonUiState(
    val isLoading: Boolean = false,
    val lesson: LessonModel? = null,
    val currentDrillIndex: Int = 0,
    val isMicActive: Boolean = false,
    val liveTranscript: String = "",
    val aiFeedback: String? = null,
    val translationTooltip: TranslationModel? = null,
    val error: String? = null
)

// User Intent
sealed interface LessonUiIntent {
    data class LoadLesson(val lessonId: String) : LessonUiIntent
    object StartPractice : LessonUiIntent
    object ToggleMic : LessonUiIntent
    data class SubmitSpeech(val audioData: ByteArray) : LessonUiIntent
    data class RequestTranslation(val wordOrSentence: String) : LessonUiIntent
    data class AskAiCoPilot(val question: String) : LessonUiIntent
}

// One-off Side Effect
sealed interface LessonUiEffect {
    data class NavigateTo(val route: String) : LessonUiEffect
    data class ShowSnackbar(val messageResId: Int) : LessonUiEffect
    object PlayAudioChime : LessonUiEffect
}
```

---

## 11. Multi-Track Curriculum Architecture (Foundation, Immersion, Situational)

To balance academic mastery, advanced immersion, and psychological habit formation, GrammarAI implements a **three-tier curriculum architecture**:

| Curriculum Layer | Asset File | Scope & Volume | Target Audience & Purpose |
| :--- | :--- | :--- | :--- |
| **Foundation Pillar Track** | `curriculum_lessons.json` | 90 lessons, ~964 exercises across 15 modules | Learners mastering fundamental grammar rules with bilingual scaffolding and structured video masterclasses. |
| **Advanced English Immersion Track** | `curriculum_lessons_en.json` | 88 lessons, 100% in English | B1–B2 intermediate/advanced learners who want full immersion without Spanish crutches, focusing on nuanced syntax and native phrasing. |
| **Situational Micro-Curriculum** | `curriculum_situational.json` | 36 micro-lessons, 180 rapid exercises across real-world themes | Low-friction, 2-minute daily micro-habits (*Café*, *Metro*, *Airport*, etc.) specifically engineered to overcome cognitive resistance. |

### Pedagogical Psychology: Overcoming "App Opening Resistance"
Grammar apps often trigger cognitive resistance because grammar exercises feel like homework or exams after a tiring day. GrammarAI neutralizes this friction through:
1. **The 2-Minute Micro-Win**: Situational lessons are bite-sized (flashcards + rapid pairing), delivering instant dopamine and confidence in under 2 minutes.
2. **Prominent Feed Placement**: Injected right under the active grammar lesson so users can always choose a zero-stress warm-up before tackling theoretical grammar.
3. **Seamless Transition to Spontaneous Voice**: Once warmed up with situational vocabulary, users feel natural confidence to open a live voice dialogue with Gemini.


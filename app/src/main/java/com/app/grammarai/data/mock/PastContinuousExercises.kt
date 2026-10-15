package com.app.grammarai.data.mock

import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.model.SkillCategory

val pastContinuousExercises = listOf(
    GeneratedExercise(
        id = "ex_pc_1",
        type = ExerciseType.CONVERSATION_DIALOGUE,
        category = SkillCategory.FLUENCY,
        dialogueContext = "Manager: 'What were you working on when the server crashed yesterday?'\nDeveloper: '___'",
        prompt = "The AI asks about your past activity. Choose and speak your response out loud:",
        options = listOf(
            "I was updating the database when it crashed.",
            "I was reviewing logs when the notification popped up.",
            "I was running tests when the power cut out."
        ),
        correctAnswer = "I was updating the database when it crashed.",
        explanation = "'I was updating...' correctly uses Past Continuous for background activity.",
        expectedPattern = "was updating ... when"
    ),
    GeneratedExercise(
        id = "ex_pc_2",
        type = ExerciseType.REPEAT_AFTER_ME,
        category = SkillCategory.PRONUNCIATION,
        prompt = "Listen to the AI model the weak auxiliary form 'was' [wəz] and repeat:",
        targetPhrase = "I was walking down the street when I saw him.",
        phonetics = "[aɪ wəz ˈwɔːkɪŋ daʊn ðə striːt wɛn aɪ sɔː hɪm]",
        options = listOf(
            "I was walking down the street when I saw him.",
            "I walked down the street yesterday.",
            "I am walking right now."
        ),
        correctAnswer = "I was walking down the street when I saw him.",
        explanation = "Notice 'was' is unstressed and pronounced as a weak auxiliary [wəz].",
        expectedPattern = "was [wəz] + verb-ing"
    ),
    GeneratedExercise(
        id = "ex_pc_3",
        type = ExerciseType.INTONATION_CHALLENGE,
        category = SkillCategory.INTONATION,
        prompt = "In sentences with 'While... when...', how does natural sentence intonation shift at the clause boundary?",
        options = listOf(
            "Slight rising pitch pause at the comma before dropping on the main interruption",
            "Sharp drop to lowest pitch immediately on 'While'",
            "Monotone pitch with zero pauses or variation",
            "Whispered tone on auxiliary verbs"
        ),
        correctAnswer = "Slight rising pitch pause at the comma before dropping on the main interruption",
        explanation = "Introductory adverbial clauses carry a continuing rising tone followed by falling resolution.",
        expectedPattern = "Continuing rising intonation on clause"
    ),
    GeneratedExercise(
        id = "ex_pc_4",
        type = ExerciseType.SENTENCE_RECONSTRUCTION,
        category = SkillCategory.GRAMMAR,
        prompt = "Reconstruct the interrupted past continuous sentence:",
        scrambledWords = listOf("He", "was", "working", "when", "the", "power", "went", "out"),
        correctAnswer = "He was working when the power went out",
        explanation = "Subject + was + verb-ing + when + interrupting Simple Past clause.",
        expectedPattern = "was + V-ing when + simple past"
    ),
    GeneratedExercise(
        id = "ex_pc_5",
        type = ExerciseType.FILL_IN_BLANK,
        category = SkillCategory.VOCABULARY,
        prompt = "The fire alarm sounded while the keynote speaker was ___ the main presentation.",
        options = listOf("delivering", "throwing", "tossing", "sending"),
        correctAnswer = "delivering",
        explanation = "In formal professional English, speeches and presentations are 'delivered'.",
        expectedPattern = "delivering a presentation"
    ),
    GeneratedExercise(
        id = "ex_pc_6",
        type = ExerciseType.LONG_READING,
        category = SkillCategory.INTONATION,
        readingPassage = "Last night at 8 PM, Emma was reading a novel by the window. Outside, rain was tapping gently against the glass when suddenly, a heavy thunderstorm knocked out the electricity across the entire neighborhood.",
        prompt = "Notice the rhythm of continuous scene-setting: which verb phrase sets the primary ongoing mood?",
        options = listOf(
            "was reading ... was tapping",
            "knocked out",
            "went",
            "window"
        ),
        correctAnswer = "was reading ... was tapping",
        explanation = "Dual past continuous constructions establish concurrent narrative atmosphere.",
        expectedPattern = "was reading ... was tapping"
    ),
    GeneratedExercise(
        id = "ex_pc_7",
        type = ExerciseType.MULTIPLE_CHOICE,
        category = SkillCategory.GRAMMAR,
        prompt = "They _____ tennis when it suddenly began to pour.",
        options = listOf("were playing", "played", "are playing", "have played"),
        correctAnswer = "were playing",
        explanation = "'were playing' indicates the continuous activity in progress when the rain started.",
        expectedPattern = "were playing"
    )
)

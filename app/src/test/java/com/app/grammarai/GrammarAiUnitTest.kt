package com.app.grammarai

import com.app.grammarai.data.model.DialogueEvaluation
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.model.Lesson
import com.app.grammarai.data.model.LessonStatus
import com.app.grammarai.data.model.LessonType
import com.app.grammarai.data.model.ShadowingEvaluation
import com.app.grammarai.data.model.SkillMetrics
import com.app.grammarai.data.remote.GeminiApiClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.app.grammarai.ui.onboarding.ChatMessage
import com.app.grammarai.ui.onboarding.OnboardingStep
import com.app.grammarai.ui.onboarding.OnboardingUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrammarAiUnitTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSkillMetricsTopStrengthAndGrowthPriority() {
        val metrics = SkillMetrics(
            productive = 57,
            pragmatics = 84,
            receptive = 77,
            structure = 64,
            lexicon = 62,
            overallCefr = "B1"
        )

        val topStrength = metrics.getTopStrength()
        assertEquals("Pragmatics", topStrength.first)
        assertEquals(84, topStrength.second)

        val growthPriority = metrics.getGrowthPriority()
        assertEquals("Productive", growthPriority.first)
        assertEquals(57, growthPriority.second)
    }

    @Test
    fun testPointsBasedRadarCefrProgression() {
        val fresh = SkillMetrics()
        assertEquals(0, fresh.totalScore())
        assertEquals("A1", fresh.calculateCefr())

        val a2 = SkillMetrics(lexicon = 60, structure = 60, productive = 60) // 180 pts total
        assertEquals("A2", a2.calculateCefr())

        val b1 = SkillMetrics(lexicon = 100, structure = 100, receptive = 100, productive = 100, pragmatics = 100) // 500 pts total
        assertEquals("B1", b1.calculateCefr())

        val b2 = SkillMetrics(lexicon = 200, structure = 200, receptive = 200, productive = 200, pragmatics = 200) // 1000 pts total
        assertEquals("B2", b2.calculateCefr())

        val c1 = SkillMetrics(lexicon = 350, structure = 350, receptive = 350, productive = 350, pragmatics = 350) // 1750 pts total
        assertEquals("C1", c1.calculateCefr())
    }

    @Test
    fun testExerciseCategoryResolverPillars() {
        val listening = GeneratedExercise(
            id = "ex_listen",
            type = ExerciseType.LISTENING_PASSAGE,
            prompt = "Listen to audio"
        )
        assertEquals(com.app.grammarai.data.model.SkillCategory.RECEPTIVE, com.app.grammarai.data.util.ExerciseCategoryResolver.resolveCategory(listening))

        val reading = GeneratedExercise(
            id = "ex_read",
            type = ExerciseType.READING,
            prompt = "Read passage"
        )
        assertEquals(com.app.grammarai.data.model.SkillCategory.RECEPTIVE, com.app.grammarai.data.util.ExerciseCategoryResolver.resolveCategory(reading))

        val pairs = GeneratedExercise(
            id = "ex_pairs",
            type = ExerciseType.SELECT_PAIRS,
            prompt = "Match pairs"
        )
        assertEquals(com.app.grammarai.data.model.SkillCategory.LEXICON, com.app.grammarai.data.util.ExerciseCategoryResolver.resolveCategory(pairs))

        val pronunciation = GeneratedExercise(
            id = "ex_pron",
            type = ExerciseType.PRONUNCIATION,
            prompt = "Speak aloud"
        )
        assertEquals(com.app.grammarai.data.model.SkillCategory.PRODUCTIVE, com.app.grammarai.data.util.ExerciseCategoryResolver.resolveCategory(pronunciation))

        val sentence = GeneratedExercise(
            id = "ex_sentence",
            type = ExerciseType.SCRAMBLED_SENTENCE,
            prompt = "Reorder words"
        )
        assertEquals(com.app.grammarai.data.model.SkillCategory.STRUCTURE, com.app.grammarai.data.util.ExerciseCategoryResolver.resolveCategory(sentence))

        val convo = GeneratedExercise(
            id = "ex_conv",
            type = ExerciseType.CONVERSATION_DIALOGUE,
            prompt = "Chat with AI"
        )
        assertEquals(com.app.grammarai.data.model.SkillCategory.PRAGMATICS, com.app.grammarai.data.util.ExerciseCategoryResolver.resolveCategory(convo))
    }

    @Test
    fun testLessonModelSerialization() {
        val lesson = Lesson(
            id = "lesson_test",
            module = 1,
            type = LessonType.GRAMMAR_PILLAR,
            cefr = "B1",
            title = "Past Continuous vs Simple Past",
            summary = "Master background narrative vs interruptions.",
            durationMinutes = 6,
            exercises = emptyList(),
            status = LessonStatus.READY,
            score = 0
        )

        val encoded = json.encodeToString(lesson)
        val decoded = json.decodeFromString<Lesson>(encoded)

        assertEquals("lesson_test", decoded.id)
        assertEquals(LessonType.GRAMMAR_PILLAR, decoded.type)
        assertEquals("B1", decoded.cefr)
        assertEquals(LessonStatus.READY, decoded.status)
    }

    @Test
    fun testGeneratedExerciseSerialization() {
        val exercise = GeneratedExercise(
            id = "ex_1",
            type = ExerciseType.REPEAT_AFTER_ME,
            category = com.app.grammarai.data.model.SkillCategory.PRONUNCIATION,
            prompt = "Listen to the natural flow and repeat the phrase clearly.",
            options = emptyList(),
            correctAnswer = "I'd like a medium cappuccino to go, please.",
            explanation = "Focus on linking 'medium' and 'cappuccino'.",
            expectedPattern = "/aɪd laɪk ə ˈmiːdiəm ˌkæpʊˈtʃiːnoʊ tuː ɡoʊ pliːz/",
            targetPhrase = "I'd like a medium cappuccino to go, please.",
            phonetics = "/aɪd laɪk ə ˈmiːdiəm ˌkæpʊˈtʃiːnoʊ tuː ɡoʊ pliːz/"
        )

        val encoded = json.encodeToString(exercise)
        val decoded = json.decodeFromString<GeneratedExercise>(encoded)

        assertEquals("ex_1", decoded.id)
        assertEquals(ExerciseType.REPEAT_AFTER_ME, decoded.type)
        assertEquals(com.app.grammarai.data.model.SkillCategory.PRONUNCIATION, decoded.category)
        assertEquals("I'd like a medium cappuccino to go, please.", decoded.targetPhrase)
        assertEquals("/aɪd laɪk ə ˈmiːdiəm ˌkæpʊˈtʃiːnoʊ tuː ɡoʊ pliːz/", decoded.phonetics)
    }

    @Test
    fun testFallback7ExercisesCountAndAll5CategoriesCovered() {
        val client = GeminiApiClient(apiKeyProvider = { "" })
        val pcExercises = client.getFallback7Exercises("lesson_past_continuous", "Past Continuous")
        assertEquals(7, pcExercises.size)
        val pcCategories = pcExercises.map { it.category }.toSet()
        assertEquals(5, pcCategories.size)
        assertTrue(pcCategories.contains(com.app.grammarai.data.model.SkillCategory.PRONUNCIATION))
        assertTrue(pcCategories.contains(com.app.grammarai.data.model.SkillCategory.FLUENCY))
        assertTrue(pcCategories.contains(com.app.grammarai.data.model.SkillCategory.INTONATION))
        assertTrue(pcCategories.contains(com.app.grammarai.data.model.SkillCategory.GRAMMAR))
        assertTrue(pcCategories.contains(com.app.grammarai.data.model.SkillCategory.VOCABULARY))

        val coffeeExercises = client.getFallback7Exercises("lesson_coffee_ordering", "Ordering Coffee")
        assertEquals(7, coffeeExercises.size)
        val coffeeCategories = coffeeExercises.map { it.category }.toSet()
        assertEquals(5, coffeeCategories.size)

        // Exercise 1 must always be an Assisted Conversation
        assertEquals(ExerciseType.CONVERSATION_DIALOGUE, pcExercises.first().type)
        assertEquals(com.app.grammarai.data.model.SkillCategory.FLUENCY, pcExercises.first().category)
        assertTrue(pcExercises.first().dialogueContext.isNotBlank())

        assertEquals(ExerciseType.CONVERSATION_DIALOGUE, coffeeExercises.first().type)
        assertEquals(com.app.grammarai.data.model.SkillCategory.FLUENCY, coffeeExercises.first().category)
        assertTrue(coffeeExercises.first().dialogueContext.isNotBlank())
    }

    @Test
    fun testGeminiResponseCandidateExtraction() {
        val client = GeminiApiClient(apiKeyProvider = { "" })
        val sampleResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Extracted text from Gemini API"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val text = client.extractTextFromGeminiResponse(sampleResponse)
        assertEquals("Extracted text from Gemini API", text)
    }

    @Test
    fun testDialogueEvaluationParsing() {
        val sampleJson = """
            {
              "isCorrect": true,
              "feedback": "Great use of past continuous to express ongoing activity.",
              "grammarScore": 92,
              "naturalAlternative": "I was handling a client presentation at that time."
            }
        """.trimIndent()

        val parsed = json.decodeFromString<DialogueEvaluation>(sampleJson)
        assertTrue(parsed.isCorrect)
        assertEquals(92, parsed.grammarScore)
        assertNotNull(parsed.naturalAlternative)
    }

    @Test
    fun testShadowingEvaluationParsing() {
        val sampleJson = """
            {
              "accuracyScore": 88,
              "rhythmFeedback": "Smooth cadence with natural phrase linking.",
              "phoneticTips": "Soften weak auxiliaries."
            }
        """.trimIndent()

        val parsed = json.decodeFromString<ShadowingEvaluation>(sampleJson)
        assertEquals(88, parsed.accuracyScore)
        assertEquals("Soften weak auxiliaries.", parsed.phoneticTips)
    }

    @Test
    fun testOnboardingUiStateTransitionsAndDefaults() {
        val initialState = OnboardingUiState()
        assertTrue(initialState.isLoadingIntro)
        assertFalse(initialState.isAiTyping)
        assertFalse(initialState.isFinalizing)
        assertEquals(OnboardingStep.PRESENTATION, initialState.currentStep)

        val updatedState = initialState.copy(
            isLoadingIntro = false,
            isAiTyping = true,
            currentStep = OnboardingStep.NATIVE_LANGUAGE,
            messages = listOf(
                ChatMessage(
                    id = "msg_1",
                    isFromAi = true,
                    text = "Hello!",
                    chips = listOf("Spanish", "French"),
                    timestamp = "Just now"
                )
            )
        )

        assertFalse(updatedState.isLoadingIntro)
        assertTrue(updatedState.isAiTyping)
        assertEquals(OnboardingStep.NATIVE_LANGUAGE, updatedState.currentStep)
        assertEquals(1, updatedState.messages.size)
        assertEquals(2, updatedState.messages[0].chips.size)
        assertEquals("Just now", updatedState.messages[0].timestamp)
    }

    @Test
    fun testWavHeaderStructure() {
        val pcmData = ByteArray(1000)
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val sampleRate = 24000
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        val header = java.nio.ByteBuffer.allocate(44).apply {
            order(java.nio.ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1.toShort())
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(totalAudioLen)
        }.array()

        assertEquals(44, header.size)
        assertEquals('R'.code.toByte(), header[0])
        assertEquals('I'.code.toByte(), header[1])
        assertEquals('F'.code.toByte(), header[2])
        assertEquals('F'.code.toByte(), header[3])
        assertEquals('W'.code.toByte(), header[8])
        assertEquals('A'.code.toByte(), header[9])
        assertEquals('V'.code.toByte(), header[10])
        assertEquals('E'.code.toByte(), header[11])
    }

    @Test
    fun testConversationDrillsInjectionAtEnd() {
        val sampleLesson = Lesson(
            id = "lesson_test_101",
            module = 101,
            type = LessonType.GRAMMAR_PILLAR,
            cefr = "A1",
            title = "Present Simple Mastery",
            exercises = listOf(
                GeneratedExercise(
                    id = "ex_1",
                    type = ExerciseType.MULTIPLE_CHOICE,
                    prompt = "Choose the correct verb form."
                ),
                GeneratedExercise(
                    id = "ex_2",
                    type = ExerciseType.FILL_BLANK,
                    prompt = "Fill in the blank."
                )
            )
        )

        val injected = com.app.grammarai.data.repository.CurriculumRepository.injectConversations(sampleLesson, sampleLesson.exercises)

        // Original 2 exercises + 1 Outro conversation at end = 3 exercises
        assertEquals(3, injected.size)

        // Preceding exercises remain intact in order
        assertEquals(ExerciseType.MULTIPLE_CHOICE, injected[0].type)
        assertEquals(ExerciseType.FILL_BLANK, injected[1].type)

        // Check last exercise is Outro Conversation placed at the end as capstone challenge
        val outro = injected.last()
        assertEquals(ExerciseType.CONVERSATION_DIALOGUE, outro.type)
        assertEquals("lesson_test_101_outro_conv", outro.id)
        assertTrue(outro.prompt.contains("Interactive AI Roleplay Scenario"))
        assertTrue(outro.dialogueContext.contains("Interactive Roleplay Coach"))
    }

    @Test
    fun testSupabaseAuthAndSyncModels() {
        val authJson = """
            {
                "access_token": "mock_jwt_token",
                "token_type": "bearer",
                "expires_in": 3600,
                "refresh_token": "mock_refresh_token",
                "user": {
                    "id": "test-user-uuid-12345",
                    "email": "user@example.com"
                }
            }
        """.trimIndent()

        val parsedAuth = json.decodeFromString<com.app.grammarai.data.remote.supabase.SupabaseAuthResponse>(authJson)
        assertEquals("mock_jwt_token", parsedAuth.accessToken)
        assertEquals("test-user-uuid-12345", parsedAuth.user?.id)
        assertEquals("user@example.com", parsedAuth.user?.email)

        val syncPayload = com.app.grammarai.data.remote.supabase.SupabaseSyncPayload(
            userId = "test-user-uuid-12345",
            userEmail = "user@example.com",
            username = "TestUser",
            completedLessons = listOf("m1_lesson_1", "m1_lesson_2"),
            xp = 350
        )
        val encodedPayload = json.encodeToString(syncPayload)
        assertTrue(encodedPayload.contains("m1_lesson_1"))
        assertTrue(encodedPayload.contains("TestUser"))
    }

    @Test
    fun testExerciseUiIntentInputText() {
        val intent = com.app.grammarai.ui.practice.ExerciseUiIntent.InputText("testing text input")
        assertEquals("testing text input", intent.text)
        assertTrue(intent is com.app.grammarai.ui.practice.ExerciseUiIntent)
    }

    @Test
    fun testWeeklyGoalPercentageCanExceed100Percent() {
        // Goal = 5, Completed = 5 -> 100%
        val state100 = com.app.grammarai.ui.feed.FeedUiState(
            weeklyCompletedLessonsCount = 5,
            lessonGoalTarget = 5
        )
        assertEquals(100, state100.goalPercentage)
        assertEquals(1.0f, state100.goalProgressFraction, 0.001f)

        // Goal = 10, Completed = 50 -> 500%
        val state500 = com.app.grammarai.ui.feed.FeedUiState(
            weeklyCompletedLessonsCount = 50,
            lessonGoalTarget = 10
        )
        assertEquals(500, state500.goalPercentage)
        assertEquals(1.0f, state500.goalProgressFraction, 0.001f)

        // Goal = 5, Completed = 50 -> 1000%
        val state1000 = com.app.grammarai.ui.feed.FeedUiState(
            weeklyCompletedLessonsCount = 50,
            lessonGoalTarget = 5
        )
        assertEquals(1000, state1000.goalPercentage)
        assertEquals(1.0f, state1000.goalProgressFraction, 0.001f)

        // Goal = 5, Completed = 2 -> 40%
        val state40 = com.app.grammarai.ui.feed.FeedUiState(
            weeklyCompletedLessonsCount = 2,
            lessonGoalTarget = 5
        )
        assertEquals(40, state40.goalPercentage)
        assertEquals(0.4f, state40.goalProgressFraction, 0.001f)
    }
}

package com.app.grammarai.data.repository

import android.content.Context
import com.app.grammarai.GrammarApplication
import com.app.grammarai.data.mock.MockCurriculumData
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.data.model.Lesson
import com.app.grammarai.data.model.LessonStatus
import com.app.grammarai.data.model.SkillCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

internal fun expandFlashcardExercises(exercise: GeneratedExercise): List<GeneratedExercise> {
    return when {
        (exercise.type == ExerciseType.FLASHCARDS || exercise.type == ExerciseType.FLASHCARD) &&
            exercise.flashcards.size > 1 -> exercise.flashcards.mapIndexed { index, card ->
                exercise.copy(
                    id = "${exercise.id}_$index",
                    type = ExerciseType.FLASHCARD,
                    category = SkillCategory.LEXICON,
                    targetPhrase = card.front,
                    correctAnswer = card.back,
                    phonetics = card.phonetic,
                    explanation = card.exampleTranslation.ifBlank { card.example },
                    flashcards = listOf(card)
                )
            }
        exercise.type == ExerciseType.FLASHCARDS -> listOf(
            exercise.copy(type = ExerciseType.FLASHCARD, category = SkillCategory.LEXICON)
        )
        else -> listOf(exercise)
    }
}

class CurriculumRepository(
    private val context: Context,
    private val preferencesManager: com.app.grammarai.data.local.PreferencesManager? = null
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val parsedCache = ConcurrentHashMap<String, List<Lesson>>()
    private val loadMutex = Mutex()
    private val cefrOrder = mapOf("PRE-A1" to 0, "A0" to 0, "A1" to 1, "A2" to 2, "B1" to 3, "B2" to 4, "C1" to 5, "C2" to 6)

    init {
        // Precarga el currículo en segundo plano para evitar trabajo de lectura en el hilo de UI.
        repoScope.launch {
            loadLessonsFromAssetsInternal()
        }
    }

    private fun getCurriculumFileName(): String {
        val lang = preferencesManager?.appLanguage ?: "es"
        return if (lang.equals("en", ignoreCase = true)) {
            "curriculum_lessons_en.json"
        } else {
            "curriculum_lessons.json"
        }
    }

    private fun sanitizeExercises(exercises: List<GeneratedExercise>, lessonType: com.app.grammarai.data.model.LessonType? = null): List<GeneratedExercise> {
        return exercises.flatMap { rawEx ->
            val resolvedCategory = com.app.grammarai.data.util.ExerciseCategoryResolver.resolveCategory(rawEx, lessonType)
            val ex = if (rawEx.category != resolvedCategory) rawEx.copy(category = resolvedCategory) else rawEx

            // Normaliza formatos antiguos y convierte cada tarjeta de un mazo en un ejercicio independiente.
            if (ex.type == ExerciseType.FLASHCARDS || ex.type == ExerciseType.FLASHCARD) {
                expandFlashcardExercises(ex)
            } else if ((ex.type == ExerciseType.SCRAMBLED_SENTENCE || ex.type == ExerciseType.SENTENCE_RECONSTRUCTION || ex.type == ExerciseType.SCRAMBLE_LISTENING) && ex.scrambledWords.isEmpty()) {
                val text = ex.correctAnswer.ifBlank { ex.targetPhrase }
                val rawTokens = if (text.contains(" - ")) {
                    text.split("\\s*-\\s*".toRegex())
                } else {
                    text.split("\\s+".toRegex())
                }
                val tokens = rawTokens
                    .map { it.trim().trim('.', ',', '!', '?', ';', ':', '¿', '¡', '"') }
                    .filter { it.isNotEmpty() }
                val shuffled = if (tokens.size > 1) {
                    var s = tokens.shuffled()
                    var attempts = 0
                    while (s == tokens && attempts < 5) {
                        s = tokens.shuffled()
                        attempts++
                    }
                    s
                } else tokens
                listOf(ex.copy(scrambledWords = shuffled))
            } else {
                listOf(ex)
            }
        }
    }

    // Lee el archivo de idioma seleccionado y recalcula los estados usando el progreso guardado.
    private suspend fun loadLessonsFromAssetsInternal(): List<Lesson> = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            val fileName = getCurriculumFileName()
            try {
                // Preload shared situational lessons from assets
                com.app.grammarai.data.catalog.SituationalCatalog.loadFromContext(context)

                val cached = parsedCache[fileName]
                val parsed = if (cached != null) {
                    cached
                } else {
                    val inputStream = try {
                        context.assets.open(fileName)
                    } catch (e: Exception) {
                        context.assets.open("curriculum_lessons.json")
                    }
                    val content = inputStream.bufferedReader().use { it.readText() }
                    val decoded = json.decodeFromString<List<Lesson>>(content)
                    parsedCache[fileName] = decoded
                    decoded
                }

                val completed = preferencesManager?.completedLessons ?: emptySet()
                val scores = preferencesManager?.lessonScores ?: emptyMap()

                val sorted = parsed.sortedWith(
                    compareBy<Lesson> { cefrOrder[it.cefr.uppercase()] ?: 99 }
                        .thenBy { it.module }
                )

                val firstAvailableIndex = firstIncompleteLessonIndex(sorted, completed)

                val updated = sorted.mapIndexed { index, lesson ->
                    val baseExercises = if (lesson.exercises.isNotEmpty()) sanitizeExercises(lesson.exercises, lesson.type) else sanitizeExercises(getMockExercises(lesson.id), lesson.type)
                    val exercises = injectConversations(lesson, baseExercises)
                    val isCompleted = completed.contains(lesson.id)
                    val lessonScore = scores[lesson.id] ?: if (isCompleted) 100 else lesson.score
                    val status = resolveLessonStatus(index, isCompleted, firstAvailableIndex)
                    lesson.copy(
                        exercises = exercises,
                        status = status,
                        score = lessonScore
                    )
                }
                _lessons.value = updated
                updated
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun reloadCurriculum() {
        // La recarga es asíncrona; los suscriptores reciben la lista nueva mediante el StateFlow.
        repoScope.launch {
            loadLessonsFromAssetsInternal()
        }
    }

    suspend fun markPreA1LessonsCompleted(throughLessonId: String) {
        val sorted = loadLessonsFromAssetsInternal().sortedWith(
            compareBy<Lesson> { cefrOrder[it.cefr.uppercase()] ?: 99 }
                .thenBy { it.module }
        )
        val boundaryIndex = sorted.indexOfFirst { it.id == throughLessonId }
        if (boundaryIndex < 0 || !sorted[boundaryIndex].cefr.equals("PRE-A1", ignoreCase = true)) return

        val skippedLessons = sorted
            .take(boundaryIndex + 1)
            .filter { it.cefr.equals("PRE-A1", ignoreCase = true) }
        val prefs = preferencesManager ?: return
        val completedIds = skippedLessons.map { it.id }.toSet()
        prefs.completedLessons = prefs.completedLessons + completedIds
        prefs.lessonScores = prefs.lessonScores.toMutableMap().apply {
            completedIds.forEach { put(it, 100) }
        }
        _lessons.value = resolveLessonStatuses(sorted)
    }

    suspend fun loadLessons(): List<Lesson> = withContext(Dispatchers.IO) {
        // Reutiliza la lista ya resuelta; solo carga desde assets si aún no hay datos en memoria.
        if (_lessons.value.isNotEmpty()) return@withContext _lessons.value
        loadLessonsFromAssetsInternal()
    }

    suspend fun getLessonAsync(id: String): Lesson? = withContext(Dispatchers.IO) {
        val situational = com.app.grammarai.data.catalog.SituationalCatalog.getLesson(id)
            ?: com.app.grammarai.data.catalog.SituationalCatalog.loadFromContext(context).find { it.id == id }
        if (situational != null) {
            val isCompleted = preferencesManager?.completedLessons?.contains(id) == true
            val score = preferencesManager?.getLessonScore(id) ?: 0
            return@withContext situational.copy(
                exercises = sanitizeExercises(situational.exercises, situational.type),
                status = if (isCompleted) LessonStatus.MASTERED else situational.status,
                score = score
            )
        }

        if (_lessons.value.isEmpty()) {
            loadLessonsFromAssetsInternal()
        }
        val found = _lessons.value.find { it.id == id }
        if (found != null && found.exercises.isNotEmpty()) return@withContext found
        val fileName = getCurriculumFileName()
        val cached = parsedCache[fileName]?.find { it.id == id }
        val isCompleted = preferencesManager?.completedLessons?.contains(id) == true
        val score = preferencesManager?.getLessonScore(id) ?: 0
        if (cached != null) {
            val base = if (cached.exercises.isNotEmpty()) sanitizeExercises(cached.exercises, cached.type) else sanitizeExercises(getMockExercises(id), cached.type)
            val sorted = _lessons.value.ifEmpty { parsedCache[fileName] ?: emptyList() }.sortedWith(
                compareBy<Lesson> { cefrOrder[it.cefr.uppercase()] ?: 99 }.thenBy { it.module }
            )
            val lessonIndex = sorted.indexOfFirst { it.id == id }
            val completed = preferencesManager?.completedLessons ?: emptySet()
            val firstAvailableIndex = firstIncompleteLessonIndex(sorted, completed)
            val resolvedStatus = resolveLessonStatus(lessonIndex, isCompleted, firstAvailableIndex)
            return@withContext cached.copy(
                exercises = injectConversations(cached, base),
                status = resolvedStatus,
                score = score
            )
        }
        found?.let {
            it.copy(
                exercises = injectConversations(it, sanitizeExercises(getMockExercises(id), it.type)),
                status = if (isCompleted) LessonStatus.MASTERED else it.status,
                score = score
            )
        }
    }

    fun getLesson(id: String): Lesson? {
        val situational = com.app.grammarai.data.catalog.SituationalCatalog.getLesson(id)
            ?: com.app.grammarai.data.catalog.SituationalCatalog.loadFromContext(context).find { it.id == id }
        if (situational != null) {
            val isCompleted = preferencesManager?.completedLessons?.contains(id) == true
            val score = preferencesManager?.getLessonScore(id) ?: 0
            return situational.copy(
                exercises = sanitizeExercises(situational.exercises, situational.type),
                status = if (isCompleted) LessonStatus.MASTERED else situational.status,
                score = score
            )
        }

        val found = _lessons.value.find { it.id == id }
        if (found != null && found.exercises.isNotEmpty()) return found
        val fileName = getCurriculumFileName()
        val cached = parsedCache[fileName]?.find { it.id == id }
        val isCompleted = preferencesManager?.completedLessons?.contains(id) == true
        val score = preferencesManager?.getLessonScore(id) ?: 0
        if (cached != null) {
            val base = if (cached.exercises.isNotEmpty()) sanitizeExercises(cached.exercises, cached.type) else sanitizeExercises(getMockExercises(id), cached.type)
            val sorted = _lessons.value.ifEmpty { parsedCache[fileName] ?: emptyList() }.sortedWith(
                compareBy<Lesson> { cefrOrder[it.cefr.uppercase()] ?: 99 }.thenBy { it.module }
            )
            val lessonIndex = sorted.indexOfFirst { it.id == id }
            val completed = preferencesManager?.completedLessons ?: emptySet()
            val firstAvailableIndex = firstIncompleteLessonIndex(sorted, completed)
            val resolvedStatus = resolveLessonStatus(lessonIndex, isCompleted, firstAvailableIndex)
            return cached.copy(
                exercises = injectConversations(cached, base),
                status = resolvedStatus,
                score = score
            )
        }
        return found
    }

    fun updateLessonExercises(id: String, exercises: List<GeneratedExercise>) {
        _lessons.value = _lessons.value.map {
            if (it.id == id) it.copy(exercises = exercises) else it
        }
    }

    fun resolveLessonStatuses(lessons: List<Lesson>): List<Lesson> {
        val completed = preferencesManager?.completedLessons ?: emptySet()
        val scores = preferencesManager?.lessonScores ?: emptyMap()

        val sorted = lessons.sortedWith(
            compareBy<Lesson> { cefrOrder[it.cefr.uppercase()] ?: 99 }
                .thenBy { it.module }
        )

        val firstAvailableIndex = firstIncompleteLessonIndex(sorted, completed)

        // Reaplica el progreso y el frontier a la lista recibida, que puede venir con estados obsoletos.
        val resolved = sorted.mapIndexed { index, lesson ->
            val isCompleted = completed.contains(lesson.id)
            val lessonScore = scores[lesson.id] ?: if (isCompleted) 100 else lesson.score
            val status = resolveLessonStatus(index, isCompleted, firstAvailableIndex)
            lesson.copy(status = status, score = lessonScore)
        }

        // Precarga audio para las lecciones READY calculadas en esta resolución.
        val availableIndices = resolved.mapIndexedNotNull { idx, l ->
            if (l.status == LessonStatus.READY) idx else null
        }
        val currentLang = preferencesManager?.appLanguage?.ifBlank { "es" } ?: "es"
        runCatching {
            com.app.grammarai.GrammarApplication.instance.onDemandAudioPrefetcher.prefetchForAvailableIndices(
                availableIndices = availableIndices,
                lang = currentLang
            )
        }

        return resolved
    }

    fun markLessonCompleted(id: String, score: Int = 100) {
        val scorePercent = score.coerceIn(0, 100)
        val prefs = preferencesManager
        val sorted = (_lessons.value.ifEmpty {
            val fileName = getCurriculumFileName()
            parsedCache[fileName] ?: emptyList()
        }).sortedWith(
            compareBy<Lesson> { cefrOrder[it.cefr.uppercase()] ?: 99 }
                .thenBy { it.module }
        )
        val currentIndex = sorted.indexOfFirst { it.id == id }
        if (currentIndex >= 0) {
            val completedBefore = prefs?.completedLessons ?: emptySet()
            val implicitlyCompletedIds = sorted
                .take(currentIndex)
                .map { it.id }
                .filterNot { completedBefore.contains(it) }
            if (prefs != null && implicitlyCompletedIds.isNotEmpty()) {
                prefs.completedLessons = completedBefore + implicitlyCompletedIds
                prefs.lessonScores = prefs.lessonScores.toMutableMap().apply {
                    implicitlyCompletedIds.forEach { put(it, 100) }
                }
            }
            prefs?.advanceUnlockFrontier(currentIndex + AVAILABLE_LESSONS_COUNT)
        }
        if (prefs != null) {
            if (prefs.completedLessons.contains(id)) {
                prefs.lessonScores = prefs.lessonScores + (id to scorePercent)
            } else {
                prefs.recordLessonCompleted(id, scorePercent)
            }
        }
        _lessons.value = resolveLessonStatuses(sorted)
    }

    private fun firstIncompleteLessonIndex(lessons: List<Lesson>, completed: Set<String>): Int {
        val index = lessons.indexOfFirst { it.id !in completed }
        return if (index >= 0) index else lessons.size
    }

    private fun resolveLessonStatus(index: Int, isCompleted: Boolean, firstAvailableIndex: Int): LessonStatus = when {
        isCompleted -> LessonStatus.MASTERED
        index >= firstAvailableIndex && index < firstAvailableIndex + AVAILABLE_LESSONS_COUNT -> LessonStatus.READY
        else -> LessonStatus.LOCKED
    }

    fun unmarkLessonCompleted(id: String) {
        preferencesManager?.removeLessonCompleted(id)
        _lessons.value = resolveLessonStatuses(_lessons.value)
    }

    fun toggleLessonCompleted(id: String, score: Int = 100) {
        val isCompleted = preferencesManager?.completedLessons?.contains(id) == true ||
                _lessons.value.find { it.id == id }?.status == LessonStatus.MASTERED
        if (isCompleted) {
            unmarkLessonCompleted(id)
        } else {
            val existingScore = preferencesManager?.getLessonScore(id) ?: score
            val scoreToSet = if (existingScore > 0) existingScore else score
            markLessonCompleted(id, scoreToSet)
        }
    }

    fun getMockExercises(lessonId: String): List<GeneratedExercise> {
        return MockCurriculumData.getMockExercises(lessonId)
    }

    companion object {
        private const val AVAILABLE_LESSONS_COUNT = 3

        fun getMockExercises(lessonId: String): List<GeneratedExercise> {
            return MockCurriculumData.getMockExercises(lessonId)
        }

        fun createIntroConversationExercise(lesson: Lesson): GeneratedExercise {
            val title = lesson.title.ifBlank { "English Lesson" }
            return GeneratedExercise(
                id = "${lesson.id}_intro_conv",
                type = ExerciseType.CONVERSATION_DIALOGUE,
                category = SkillCategory.PRAGMATICS,
                prompt = "Conversational Intro & Practice: Learn '$title' interactively! Listen to your AI coach explain the concept, confirm understanding, practice spoken drills, and wrap up warmly.",
                dialogueContext = "Interactive Spoken Coach introducing and practicing '$title'. Provide a warm greeting, explain the grammar concept clearly, run interactive spoken exercises, and conclude with a warm farewell.",
                targetPhrase = title,
                options = listOf(
                    "¡Entendido, todo claro!",
                    "Sí, estoy listo para practicar.",
                    "¿Podrías darme un ejemplo?"
                ),
                explanation = "An interactive spoken lesson at the beginning starting with concept explanation and guided spoken practice before the drills."
            )
        }

        fun createOutroConversationExercise(lesson: Lesson): GeneratedExercise {
            val title = lesson.title.ifBlank { "English Lesson" }
            return GeneratedExercise(
                id = "${lesson.id}_outro_conv",
                type = ExerciseType.CONVERSATION_DIALOGUE,
                category = SkillCategory.PRAGMATICS,
                prompt = "Interactive AI Roleplay Scenario: Put '$title' into action! Chat naturally with your AI partner in a dynamic real-world simulation.",
                dialogueContext = "Interactive Roleplay Coach simulating a real-world scenario around '$title'. Prompt the learner into active speaking and reply dynamically with pedagogical recasting.",
                targetPhrase = title,
                options = listOf(
                    "I'm ready for the roleplay challenge!",
                    "Let's practice this scenario!",
                    "Could you set the scene for me?"
                ),
                explanation = "A realistic spoken roleplay scenario where you apply the target grammar rules in a natural dialogue."
            )
        }

        fun createTheoryExercise(lesson: Lesson): GeneratedExercise {
            val title = lesson.title.ifBlank { "Grammar Theory" }
            val cleanLessonId = lesson.id.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
            return GeneratedExercise(
                id = "theory_$cleanLessonId",
                type = ExerciseType.THEORY,
                category = SkillCategory.STRUCTURE,
                prompt = title,
                readingPassage = lesson.theoryMarkdown,
                targetPhrase = title,
                explanation = "Lee y escucha la explicación de la teoría antes de comenzar los ejercicios prácticos.",
                options = listOf("Continuar a los ejercicios")
            )
        }

        fun injectConversations(lesson: Lesson, baseExercises: List<GeneratedExercise>): List<GeneratedExercise> {
            val result = mutableListOf<GeneratedExercise>()

            // Orden del recorrido: teoría (si existe), ejercicios base y conversación final.
            if (lesson.theoryMarkdown.isNotBlank()) {
                result.add(createTheoryExercise(lesson))
            }

            val filtered = baseExercises.filter { it.type != ExerciseType.CONVERSATION_DIALOGUE && it.type != ExerciseType.THEORY }
            result.addAll(filtered)

            result.add(createOutroConversationExercise(lesson))

            return result
        }
    }
}

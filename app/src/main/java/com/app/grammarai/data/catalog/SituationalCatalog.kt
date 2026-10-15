package com.app.grammarai.data.catalog

import android.content.Context
import com.app.grammarai.GrammarApplication
import com.app.grammarai.data.model.Lesson
import kotlinx.serialization.json.Json

object SituationalCatalog {

    private val json = Json { ignoreUnknownKeys = true }
    private val _cachedLessons = mutableListOf<Lesson>()

    val situationalLessons: List<Lesson>
        get() {
            synchronized(this) {
                if (_cachedLessons.isEmpty()) {
                    runCatching {
                        loadFromContext(GrammarApplication.instance)
                    }
                }
                return _cachedLessons.toList()
            }
        }

    fun loadFromContext(context: Context): List<Lesson> {
        synchronized(this) {
            if (_cachedLessons.isNotEmpty()) return _cachedLessons.toList()
            return try {
                val inputStream = context.assets.open("curriculum_situational.json")
                val content = inputStream.bufferedReader().use { it.readText() }
                val loaded = json.decodeFromString<List<Lesson>>(content)
                _cachedLessons.clear()
                _cachedLessons.addAll(loaded)
                _cachedLessons.toList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun getRandomLesson(excludeId: String? = null): Lesson? {
        val list = situationalLessons
        if (list.isEmpty()) return null
        val candidates = list.filter { it.id != excludeId }
        return if (candidates.isNotEmpty()) candidates.random() else list.first()
    }

    fun getLesson(id: String): Lesson? {
        return situationalLessons.find { it.id == id }
    }

    fun getThemes(): List<SituationalTheme> {
        val lessons = situationalLessons
        return lessons.groupBy { it.module }.map { (module, parts) ->
            val themeTitle = when (module) {
                800 -> "Cafetería"
                801 -> "El Metro"
                else -> "Situación Real"
            }
            SituationalTheme(
                module = module,
                title = themeTitle,
                parts = parts
            )
        }
    }

    fun getActiveProgress(completedLessonIds: Set<String>): ActiveSituationalProgress? {
        val themes = getThemes()
        if (themes.isEmpty()) return null

        // Find the first theme that has an uncompleted part
        for (theme in themes) {
            val uncompletedPart = theme.parts.firstOrNull { !completedLessonIds.contains(it.id) }
            if (uncompletedPart != null) {
                val partIndex = theme.parts.indexOf(uncompletedPart) + 1
                return ActiveSituationalProgress(
                    theme = theme,
                    currentLesson = uncompletedPart,
                    partIndex = partIndex,
                    totalParts = theme.parts.size,
                    isThemeCompleted = false,
                    isAllCompleted = false
                )
            }
        }

        // All themes are completed
        val lastTheme = themes.last()
        return ActiveSituationalProgress(
            theme = lastTheme,
            currentLesson = lastTheme.parts.last(),
            partIndex = lastTheme.parts.size,
            totalParts = lastTheme.parts.size,
            isThemeCompleted = true,
            isAllCompleted = true
        )
    }
}

data class SituationalTheme(
    val module: Int,
    val title: String,
    val parts: List<Lesson>
)

data class ActiveSituationalProgress(
    val theme: SituationalTheme,
    val currentLesson: Lesson,
    val partIndex: Int,
    val totalParts: Int,
    val isThemeCompleted: Boolean,
    val isAllCompleted: Boolean
)

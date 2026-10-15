package com.app.grammarai.ui.onboarding

import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.repository.CurriculumRepository

class OnboardingStepHandler(
    private val prefs: PreferencesManager,
    private val curriculumRepository: CurriculumRepository,
    private val updateState: ((OnboardingUiState) -> OnboardingUiState) -> Unit
) {
    fun handleNativeLanguage(language: String): String {
        val isSpanish = language.contains("Español", ignoreCase = true) || language.contains("Spanish", ignoreCase = true)
        val selectedNative = if (isSpanish) "Spanish" else "English"
        prefs.nativeLanguage = selectedNative
        return selectedNative
    }

    fun handleAppLanguage(option: String): String {
        val wantsEnglish = option.contains("Inglés", ignoreCase = true) || option.contains("English", ignoreCase = true)
        val newLang = if (wantsEnglish) "en" else "es"
        prefs.appLanguage = newLang
        curriculumRepository.reloadCurriculum()
        return newLang
    }

    fun handleName(nameInput: String): String {
        val name = nameInput.trim().ifBlank { "neko" }
        prefs.userName = name
        return name
    }

    fun handleGoal(goal: String): String {
        prefs.primaryGoal = goal
        return goal
    }

    fun handleSituation(situation: String): String {
        prefs.targetSituations = situation
        return situation
    }

    fun toggleSituation(current: Set<String>, situation: String): Set<String> {
        return if (current.contains(situation)) current - situation else if (current.size < 5) current + situation else current
    }

    fun confirmSituations(selected: Set<String>): String {
        val isEn = prefs.appLanguage.equals("en", ignoreCase = true)
        val defaultList = if (isEn) {
            listOf("💼 Work & Business Meetings", "✈️ Travel, Airports & Hotels", "☕ Coffee Shops & Daily Life")
        } else {
            listOf("💼 Trabajo y Negocios", "✈️ Viajes, Aeropuertos y Hoteles", "☕ Cafeterías y Vida Cotidiana")
        }
        val joined = (if (selected.isEmpty()) defaultList else selected.toList()).joinToString(", ")
        prefs.targetSituations = joined
        return joined
    }

    fun handleLevel(level: String): String {
        prefs.englishLevel = level
        val isEnglishCurriculum = prefs.appLanguage.equals("en", ignoreCase = true)
        val shouldSkipPreA1 = isEnglishCurriculum || level.contains("saltar pre-a1", ignoreCase = true)
        val cefr = if (shouldSkipPreA1) "A1" else "Pre-A1"
        prefs.skillMetrics = prefs.skillMetrics.copy(overallCefr = cefr)
        return level
    }

    fun handleWeeklyGoal(option: String): Int {
        val numberRegex = Regex("""\d+""")
        val match = numberRegex.find(option)
        val target = match?.value?.toIntOrNull() ?: 5
        prefs.lessonGoalTarget = target
        return target
    }

    fun handleReminder(option: String): Boolean {
        val wantsReminder = option.contains("Sí", ignoreCase = true) || option.contains("Yes", ignoreCase = true)
        prefs.dailyReminderEnabled = wantsReminder
        val app = com.app.grammarai.GrammarApplication.instance
        if (wantsReminder) {
            val (hour, minute) = com.app.grammarai.core.notification.DailyReminderManager.parseReminderTime(prefs.dailyReminderTime)
            com.app.grammarai.core.notification.DailyReminderManager.scheduleDailyReminder(app, hour, minute)
        } else {
            com.app.grammarai.core.notification.DailyReminderManager.cancelDailyReminder(app)
        }
        return wantsReminder
    }

    fun handleApiKey(apiKeyInput: String): String {
        val trimmed = apiKeyInput.trim()
        if (trimmed.isNotBlank()) {
            prefs.apiKey = trimmed
        }
        return trimmed
    }

    fun createUserMessage(prefix: String, text: String): ChatMessage {
        return ChatMessage(id = "user_${prefix}_${System.currentTimeMillis()}", isFromAi = false, text = text)
    }
}

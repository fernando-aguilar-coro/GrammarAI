package com.app.grammarai.ui.navigation

import android.net.Uri

sealed class Route(val path: String) {
    object Onboarding : Route("onboarding")
    object Feed : Route("feed")
    object Lesson : Route("lesson/{lessonId}") {
        fun createRoute(lessonId: String) = "lesson/${Uri.encode(lessonId)}"
    }
    object Practice : Route("practice/{lessonId}") {
        fun createRoute(lessonId: String) = "practice/${Uri.encode(lessonId)}"
    }
    object SkillRadar : Route("skill_radar")
    object Settings : Route("settings")
}

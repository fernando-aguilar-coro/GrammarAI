package com.app.grammarai.data.mock

import com.app.grammarai.data.model.GeneratedExercise

object MockCurriculumData {

    fun getMockExercises(lessonId: String): List<GeneratedExercise> {
        return pastContinuousExercises
    }

    fun getFallbackConversationReply(topic: String, userUtterance: String): String {
        return if (topic.contains("coffee", ignoreCase = true)) {
            if (userUtterance.contains("latte", ignoreCase = true) || userUtterance.contains("cappuccino", ignoreCase = true)) {
                "Excellent choice! What size would you like, and do you prefer dairy or plant milk?"
            } else {
                "Sounds wonderful! Would you like any pastry or muffin to go with that?"
            }
        } else {
            "Got it! And what were you planning to do right after that happened?"
        }
    }

    fun getFallbackSuggestions(topic: String, lastAiUtterance: String): List<String> {
        return if (topic.contains("coffee", ignoreCase = true)) {
            listOf(
                "Could I get an iced oat latte to go, please?",
                "I'd like a medium cappuccino with oat milk, please.",
                "Can I just get an espresso and a glass of water?"
            )
        } else {
            listOf(
                "I was updating the database when it crashed.",
                "I was reviewing logs when the notification popped up.",
                "I was running tests when the power cut out."
            )
        }
    }
}

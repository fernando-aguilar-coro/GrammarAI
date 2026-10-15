package com.app.grammarai.ui.onboarding

object OnboardingScriptHelper {

    /**
     * Step 1: Presentation
     * English by default with Spanish translation available.
     */
    fun getPresentationMessage(): ChatMessage {
        return ChatMessage(
            id = "onboarding_presentation",
            isFromAi = true,
            text = "Hello! I am your AI English coach. I will help you speak English naturally and build lasting fluency with personalized daily practice.",
            translation = "¡Hola! Soy tu tutor personal de inglés con IA. Te ayudaré a dominar el idioma mediante conversaciones naturales y práctica diaria guiada.",
            isTranslationVisible = false,
            chips = listOf("Let's get started! 🚀")
        )
    }

    /**
     * Step 2: Native Language
     * English by default with Spanish translation available.
     */
    fun getNativeLanguageMessage(): ChatMessage {
        return ChatMessage(
            id = "onboarding_native_lang",
            isFromAi = true,
            text = "What is your native language?",
            translation = "¿Cuál es tu idioma nativo o lengua materna?",
            isTranslationVisible = false,
            chips = listOf("🇪🇸 Español", "🇺🇸 English")
        )
    }

    /**
     * Step 3: App Interface Language Preference
     * English by default with Spanish translation available.
     */
    fun getAppLanguageMessage(): ChatMessage {
        return ChatMessage(
            id = "onboarding_app_lang",
            isFromAi = true,
            text = "Would you like to keep the app interface in English for full immersion, or switch it to Spanish?",
            translation = "¿Prefieres mantener la interfaz en Inglés para una inmersión completa, o cambiarla a Español?",
            isTranslationVisible = false,
            chips = listOf("🇺🇸 Keep in English", "🇪🇸 Switch to Spanish / Cambiar a Español")
        )
    }

    /**
     * Step 4: User Name
     * Matches the appLanguage chosen in Step 3.
     */
    fun getNameMessage(appLang: String = "es"): ChatMessage {
        val isEn = appLang.equals("en", ignoreCase = true)
        return ChatMessage(
            id = "onboarding_name",
            isFromAi = true,
            text = if (isEn) "Awesome! What should I call you?" else "¡Mucho gusto! ¿Cómo te gustaría que te llame?",
            translation = if (isEn) "¡Mucho gusto! ¿Cómo te gustaría que te llame?" else "Awesome! What should I call you?",
            isTranslationVisible = false,
            chips = emptyList()
        )
    }

    /**
     * Step 5: English Goal
     */
    fun getGoalMessage(name: String, appLang: String = "es"): ChatMessage {
        val isEn = appLang.equals("en", ignoreCase = true)
        val chips = if (isEn) {
            listOf(
                "🗣️ Speaking fluency & confidence",
                "💼 Career growth & interviews",
                "✈️ Travel & everyday communication",
                "🎓 Exams & certifications"
            )
        } else {
            listOf(
                "🗣️ Fluidez oral y confianza",
                "💼 Trabajo y entrevistas laborales",
                "✈️ Viajes y vida cotidiana",
                "🎓 Exámenes y certificaciones"
            )
        }
        return ChatMessage(
            id = "onboarding_goal",
            isFromAi = true,
            text = if (isEn) "Great to meet you, $name! What is your primary learning goal?" else "¡Mucho gusto, $name! ¿Cuál es tu meta principal con el inglés?",
            translation = if (isEn) "¡Mucho gusto, $name! ¿Cuál es tu meta principal con el inglés?" else "Great to meet you, $name! What is your primary learning goal?",
            isTranslationVisible = false,
            chips = chips
        )
    }

    /**
     * Step 6: Preferred Roleplay Scenarios with stickers
     */
    fun getSituationsMessage(appLang: String = "es"): ChatMessage {
        val isEn = appLang.equals("en", ignoreCase = true)
        val chips = if (isEn) {
            listOf(
                "💼 Work & Business Meetings",
                "✈️ Travel, Airports & Hotels",
                "☕ Coffee Shops & Daily Life",
                "🎬 Movies, Shows & Music",
                "🍔 Restaurants & Dining Out",
                "🤝 Job Interviews & Networking",
                "🛍️ Shopping & City Errands"
            )
        } else {
            listOf(
                "💼 Trabajo y Negocios",
                "✈️ Viajes, Aeropuertos y Hoteles",
                "☕ Cafeterías y Vida Cotidiana",
                "🎬 Cine, Series y Música",
                "🍔 Restaurantes y Salidas",
                "🤝 Entrevistas de Trabajo y Networking",
                "🛍️ Compras y Trámites Urbanos"
            )
        }
        return ChatMessage(
            id = "onboarding_situations",
            isFromAi = true,
            text = if (isEn) {
                "For your interactive AI Roleplay Scenarios, which real-world situations would you like to master? Select between 3 and 5 topics for the most varied conversations:"
            } else {
                "Para tus escenarios de Roleplay y conversación con IA, ¿en qué situaciones te gustaría desenvolverte? Elige entre 3 y 5 categorías para tener conversaciones más variadas:"
            },
            translation = if (isEn) {
                "Para tus escenarios de Roleplay y conversación con IA, ¿en qué situaciones te gustaría desenvolverte? Elige entre 3 y 5 categorías para tener conversaciones más variadas:"
            } else {
                "For your interactive AI Roleplay Scenarios, which real-world situations would you like to master? Select between 3 and 5 topics for the most varied conversations:"
            },
            isTranslationVisible = false,
            chips = chips
        )
    }

    /** Pregunta solo en español si se empieza desde Pre-A1 o se salta ese nivel. */
    fun getLevelMessage(): ChatMessage {
        return ChatMessage(
            id = "onboarding_level",
            isFromAi = true,
            text = "¿Quieres empezar desde Pre-A1 o saltar ese nivel y comenzar en A1?",
            translation = "Would you like to start from Pre-A1 or skip it and begin at A1?",
            isTranslationVisible = false,
            chips = listOf(
                "🐣 Empezar desde Pre-A1",
                "⚡ Saltar Pre-A1 y comenzar en A1"
            )
        )
    }

    /**
     * Step 8: Weekly Lesson Goal
     */
    fun getWeeklyGoalMessage(appLang: String = "es"): ChatMessage {
        val isEn = appLang.equals("en", ignoreCase = true)
        val chips = if (isEn) {
            listOf(
                "🌱 2 lessons / week (Casual)",
                "⚡ 3 lessons / week (Regular)",
                "🎯 5 lessons / week (Recommended)",
                "🔥 7 lessons / week (Intensive)",
                "🚀 10 lessons / week (Speedrun)"
            )
        } else {
            listOf(
                "🌱 2 lecciones / semana (Casual)",
                "⚡ 3 lecciones / semana (Regular)",
                "🎯 5 lecciones / semana (Recomendado)",
                "🔥 7 lecciones / semana (Intensivo)",
                "🚀 10 lecciones / semana (Acelerado)"
            )
        }
        return ChatMessage(
            id = "onboarding_weekly_goal",
            isFromAi = true,
            text = if (isEn) {
                "How many lessons would you like to target each week? Setting a weekly pace builds steady progress without daily streak pressure."
            } else {
                "¿Cuántas lecciones te gustaría completar cada semana? Establecer una meta semanal crea constancia sin el estrés de una racha diaria."
            },
            translation = if (isEn) {
                "¿Cuántas lecciones te gustaría completar cada semana? Establecer una meta semanal crea constancia sin el estrés de una racha diaria."
            } else {
                "How many lessons would you like to target each week? Setting a weekly pace builds steady progress without daily streak pressure."
            },
            isTranslationVisible = false,
            chips = chips
        )
    }

    /**
     * Step 9: Daily Reminder
     */
    fun getReminderMessage(appLang: String = "es"): ChatMessage {
        val isEn = appLang.equals("en", ignoreCase = true)
        val chips = if (isEn) {
            listOf(
                "⏰ Yes, remind me at 8:00 PM",
                "🚀 Not now, I'll practice on my own"
            )
        } else {
            listOf(
                "⏰ Sí, recuérdame a las 20:00",
                "🚀 No por ahora, entraré por mi cuenta"
            )
        }
        return ChatMessage(
            id = "onboarding_reminder",
            isFromAi = true,
            text = if (isEn) "Consistency is key to fluency. Would you like a daily reminder to practice?" else "La constancia es la clave para la fluidez. ¿Te gustaría activar un recordatorio diario para practicar?",
            translation = if (isEn) "La constancia es la clave para la fluidez. ¿Te gustaría activar un recordatorio diario para practicar?" else "Consistency is key to fluency. Would you like a daily reminder to practice?",
            isTranslationVisible = false,
            chips = chips
        )
    }

    /**
     * Step 10: Gemini API Key (BYOK) Explanation & Choice
     */
    fun getApiKeyExplanationMessage(appLang: String = "es"): ChatMessage {
        val isEn = appLang.equals("en", ignoreCase = true)
        val chips = if (isEn) {
            listOf(
                "🔑 Enter my API Key",
                "📖 How do I get a free key?",
                "⚡ Continue with default key"
            )
        } else {
            listOf(
                "🔑 Configurar mi API Key",
                "📖 ¿Cómo consigo una gratis?",
                "⚡ Continuar con clave genérica"
            )
        }
        return ChatMessage(
            id = "onboarding_api_key_intro",
            isFromAi = true,
            text = if (isEn) {
                "GrammarAI uses Google Gemini to give you real-time grammar feedback, exercise corrections, and realistic AI voice roleplay conversations.\n\nTo unlock these features with dedicated speed, you can connect your own free Gemini API Key. It is optional: if you skip, the app will continue using our shared default key."
            } else {
                "GrammarAI utiliza Google Gemini para darte correcciones gramaticales en vivo, retroalimentación detallada y conversaciones fluidas de rol por voz con IA.\n\nPara disfrutar de estas funciones con mayor velocidad y privacidad, puedes conectar tu propia Gemini API Key gratuita. Es opcional: si prefieres no configurarla ahora, la app continuará usando una clave genérica compartida."
            },
            translation = if (isEn) {
                "GrammarAI utiliza Google Gemini para darte correcciones gramaticales en vivo, retroalimentación detallada y conversaciones fluidas de rol por voz con IA.\n\nPara disfrutar de estas funciones con mayor velocidad y privacidad, puedes conectar tu propia Gemini API Key gratuita. Es opcional: si prefieres no configurarla ahora, la app continuará usando una clave genérica compartida."
            } else {
                "GrammarAI uses Google Gemini to give you real-time grammar feedback, exercise corrections, and realistic AI voice roleplay conversations.\n\nTo unlock these features with dedicated speed, you can connect your own free Gemini API Key. It is optional: if you skip, the app will continue using our shared default key."
            },
            isTranslationVisible = false,
            chips = chips
        )
    }

    /**
     * Step 10b: How to get an API Key
     */
    fun getApiKeyHowToMessage(appLang: String = "es"): ChatMessage {
        val isEn = appLang.equals("en", ignoreCase = true)
        return ChatMessage(
            id = "onboarding_api_key_howto",
            isFromAi = true,
            text = if (isEn) {
                "Getting your free Gemini API Key takes less than 1 minute:\n\n1. Sign in with your Google account at aistudio.google.com\n2. Tap the blue 'Get API key' button\n3. Click 'Create API key' in a project (it's 100% free)\n4. Copy the key (starts with 'AIzaSy...') and paste it below\n\nTap the button below to open Google AI Studio directly!"
            } else {
                "Obtener tu clave gratuita de Gemini toma menos de 1 minuto:\n\n1. Entra con tu cuenta de Google a aistudio.google.com\n2. Presiona el botón azul 'Get API key'\n3. Haz clic en 'Create API key' en un proyecto (es 100% gratuita)\n4. Copia la clave generada (empieza por 'AIzaSy...') y pégala aquí abajo\n\n¡Puedes tocar el botón de abajo para abrir Google AI Studio directamente!"
            },
            translation = if (isEn) {
                "Obtener tu clave gratuita de Gemini toma menos de 1 minuto:\n\n1. Entra con tu cuenta de Google a aistudio.google.com\n2. Presiona el botón azul 'Get API key'\n3. Haz clic en 'Create API key' en un proyecto (es 100% gratuita)\n4. Copia la clave generada (empieza por 'AIzaSy...') y pégala aquí abajo\n\n¡Puedes tocar el botón de abajo para abrir Google AI Studio directamente!"
            } else {
                "Getting your free Gemini API Key takes less than 1 minute:\n\n1. Sign in with your Google account at aistudio.google.com\n2. Tap the blue 'Get API key' button\n3. Click 'Create API key' in a project (it's 100% free)\n4. Copy the key (starts with 'AIzaSy...') and paste it below\n\nTap the button below to open Google AI Studio directly!"
            },
            isTranslationVisible = false,
            chips = emptyList()
        )
    }

    /**
     * Step 10c: Direct Prompt for API Key
     */
    fun getApiKeyInputPromptMessage(appLang: String = "es"): ChatMessage {
        val isEn = appLang.equals("en", ignoreCase = true)
        return ChatMessage(
            id = "onboarding_api_key_prompt",
            isFromAi = true,
            text = if (isEn) {
                "Awesome! Paste your Gemini API Key below. You can test your connection to verify it works before saving:"
            } else {
                "¡Excelente! Pega tu clave API de Gemini a continuación. Puedes probar la conexión para comprobar que funcione antes de guardar:"
            },
            translation = if (isEn) {
                "¡Excelente! Pega tu clave API de Gemini a continuación. Puedes probar la conexión para comprobar que funcione antes de guardar:"
            } else {
                "Awesome! Paste your Gemini API Key below. You can test your connection to verify it works before saving:"
            },
            isTranslationVisible = false,
            chips = emptyList()
        )
    }

    /**
     * Step 11: Cloud Data Sync with Google & Supabase
     */
    fun getSyncAccountMessage(appLang: String = "es"): ChatMessage {
        val isEn = appLang.equals("en", ignoreCase = true)
        return ChatMessage(
            id = "onboarding_sync",
            isFromAi = true,
            text = if (isEn) {
                "Protect your progress and sync seamlessly across devices with Google & Supabase Cloud Backup."
            } else {
                "Protege tu progreso y sincroniza tus datos en cualquier dispositivo vinculando tu cuenta de Google."
            },
            translation = if (isEn) {
                "Protege tu progreso y sincroniza tus datos en cualquier dispositivo vinculando tu cuenta de Google."
            } else {
                "Protect your progress and sync seamlessly across devices with Google & Supabase Cloud Backup."
            },
            isTranslationVisible = false,
            chips = emptyList()
        )
    }
}

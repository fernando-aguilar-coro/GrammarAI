package com.app.grammarai.core.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun getLocale(languageCode: String): Locale {
        return if (languageCode.equals("en", ignoreCase = true)) {
            Locale.ENGLISH
        } else {
            Locale("es")
        }
    }

    fun createLocalizedContext(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            return context.createConfigurationContext(config)
        }
    }

    fun applyLanguage(context: Context, languageCode: String) {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)

        // 1. Android 13+ (API 33+) System App Locales
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = LocaleList(locale)
            }
        }

        // 2. Resources update for Activity context and Application context
        applyToResources(context, locale)
        if (context.applicationContext != null && context.applicationContext !== context) {
            applyToResources(context.applicationContext, locale)
        }
    }

    private fun applyToResources(context: Context, locale: Locale) {
        val res = context.resources
        val config = Configuration(res.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)
    }

    @Deprecated("Use applyLanguage instead", ReplaceWith("applyLanguage(context, languageCode)"))
    fun applyLanguageToResources(context: Context, languageCode: String) {
        applyLanguage(context, languageCode)
    }

    @Deprecated("Use createLocalizedContext instead", ReplaceWith("createLocalizedContext(context, languageCode)"))
    fun updateContextLocale(context: Context, languageCode: String): Context {
        return createLocalizedContext(context, languageCode)
    }
}

package com.app.grammarai.core.di

import android.content.Context
import com.app.grammarai.core.srs.SpacedRepetitionEngine
import com.app.grammarai.core.translation.OnDeviceTranslationManager
import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.remote.GeminiApiClient
import com.app.grammarai.data.repository.CurriculumRepository
import com.app.grammarai.data.repository.PracticeHistoryRepository
import com.app.grammarai.data.repository.SupabaseSyncRepository

interface AppContainer {
    val preferencesManager: PreferencesManager
    val geminiApiClient: GeminiApiClient
    val curriculumRepository: CurriculumRepository
    val practiceHistoryRepository: PracticeHistoryRepository
    val spacedRepetitionEngine: SpacedRepetitionEngine
    val supabaseSyncRepository: SupabaseSyncRepository
    val onDeviceTranslationManager: OnDeviceTranslationManager
    val playAssetDeliveryManager: com.app.grammarai.core.audio.delivery.PlayAssetDeliveryManager
    val onDemandAudioPrefetcher: com.app.grammarai.core.audio.delivery.OnDemandAudioPrefetcher
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val onDeviceTranslationManager: OnDeviceTranslationManager by lazy {
        OnDeviceTranslationManager()
    }

    override val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    override val geminiApiClient: GeminiApiClient by lazy {
        GeminiApiClient(
            apiKeyProvider = { preferencesManager.apiKey },
            suggestionsPromptProvider = { preferencesManager.aiSuggestionsPrompt }
        )
    }

    override val curriculumRepository: CurriculumRepository by lazy {
        CurriculumRepository(context, preferencesManager)
    }

    override val practiceHistoryRepository: PracticeHistoryRepository by lazy {
        PracticeHistoryRepository(context)
    }

    override val spacedRepetitionEngine: SpacedRepetitionEngine by lazy {
        SpacedRepetitionEngine(preferencesManager, practiceHistoryRepository)
    }

    override val supabaseSyncRepository: SupabaseSyncRepository by lazy {
        SupabaseSyncRepository(preferencesManager)
    }

    override val playAssetDeliveryManager: com.app.grammarai.core.audio.delivery.PlayAssetDeliveryManager by lazy {
        com.app.grammarai.core.audio.delivery.PlayAssetDeliveryManager(context)
    }

    override val onDemandAudioPrefetcher: com.app.grammarai.core.audio.delivery.OnDemandAudioPrefetcher by lazy {
        com.app.grammarai.core.audio.delivery.OnDemandAudioPrefetcher(playAssetDeliveryManager)
    }
}

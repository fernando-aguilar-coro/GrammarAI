package com.app.grammarai

import android.app.Application
import android.content.Context
import com.app.grammarai.core.di.AppContainer
import com.app.grammarai.core.di.DefaultAppContainer
import com.app.grammarai.core.util.LocaleHelper
import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.remote.GeminiApiClient
import com.app.grammarai.data.repository.CurriculumRepository

class GrammarApplication : Application() {

    lateinit var container: AppContainer
        private set

    val preferencesManager: PreferencesManager get() = container.preferencesManager
    val geminiApiClient: GeminiApiClient get() = container.geminiApiClient
    val curriculumRepository: CurriculumRepository get() = container.curriculumRepository
    val supabaseSyncRepository: com.app.grammarai.data.repository.SupabaseSyncRepository get() = container.supabaseSyncRepository
    val playAssetDeliveryManager: com.app.grammarai.core.audio.delivery.PlayAssetDeliveryManager get() = container.playAssetDeliveryManager
    val onDemandAudioPrefetcher: com.app.grammarai.core.audio.delivery.OnDemandAudioPrefetcher get() = container.onDemandAudioPrefetcher

    override fun attachBaseContext(base: Context) {
        val lang = runCatching { PreferencesManager(base).appLanguage }.getOrDefault("en")
        val localizedContext = LocaleHelper.createLocalizedContext(base, lang)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = DefaultAppContainer(this)
        val lang = preferencesManager.appLanguage
        LocaleHelper.applyLanguage(this, lang)
    }

    companion object {
        lateinit var instance: GrammarApplication
            private set
    }
}

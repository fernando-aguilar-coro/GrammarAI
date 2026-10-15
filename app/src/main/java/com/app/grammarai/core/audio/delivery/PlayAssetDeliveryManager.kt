package com.app.grammarai.core.audio.delivery

import android.content.Context
import android.util.Log
import com.google.android.play.core.assetpacks.AssetPackLocation
import com.google.android.play.core.assetpacks.AssetPackManager
import com.google.android.play.core.assetpacks.AssetPackManagerFactory
import com.google.android.play.core.assetpacks.AssetPackState
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener
import com.google.android.play.core.assetpacks.model.AssetPackStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class PlayAssetDeliveryManager(
    private val context: Context,
    private val assetPackManager: AssetPackManager = AssetPackManagerFactory.getInstance(context)
) {
    companion object {
        private const val TAG = "PlayAssetDeliveryMgr"

        fun getPackName(lang: String, lessonIndex: Int): String? {
            if (lessonIndex < 10) return null // Lessons 1-10 are in Base APK
            val tierIdx = (lessonIndex - 10).coerceAtLeast(0) / 10
            val boundedTier = tierIdx.coerceAtMost(7)
            val start = 11 + (boundedTier * 10)
            val end = start + 9
            val targetLang = if (lang.equals("en", ignoreCase = true)) "en" else "es"
            return "audio_pack_${targetLang}_${start}_${end}"
        }

        fun getPackNameForLesson(
            lang: String,
            lesson: com.app.grammarai.data.model.Lesson?,
            allLessons: List<com.app.grammarai.data.model.Lesson>
        ): String? {
            if (lesson == null) return null
            val cefr = lesson.cefr.uppercase()
            if (cefr == "PRE-A1" || cefr == "A0" || (lesson.module in 1..12 && cefr.isBlank())) {
                return null // Pre-A1 lessons are in Base APK
            }
            val standardIndex = allLessons
                .filter { it.cefr.uppercase() !in listOf("PRE-A1", "A0") && (it.module !in 1..12 || it.cefr.isNotBlank()) }
                .indexOfFirst { it.id == lesson.id }
            if (standardIndex < 0 || standardIndex < 10) return null
            return getPackName(lang, standardIndex)
        }

        val ALL_PACKS: List<String> = buildList {
            listOf("es", "en").forEach { lang ->
                for (tier in 0..7) {
                    val start = 11 + (tier * 10)
                    val end = start + 9
                    add("audio_pack_${lang}_${start}_${end}")
                }
            }
        }
    }

    private val _packStatuses = MutableStateFlow<Map<String, PackDeliveryStatus>>(emptyMap())
    val packStatuses: StateFlow<Map<String, PackDeliveryStatus>> = _packStatuses.asStateFlow()

    private val listener = AssetPackStateUpdateListener { state: AssetPackState ->
        val name = state.name()
        val status = when (state.status()) {
            AssetPackStatus.COMPLETED -> {
                Log.d(TAG, "AssetPack $name completed installation.")
                PackDeliveryStatus.Installed
            }
            AssetPackStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload()
                val downloaded = state.bytesDownloaded()
                val progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f
                PackDeliveryStatus.Downloading(progress)
            }
            AssetPackStatus.PENDING, AssetPackStatus.TRANSFERRING -> {
                PackDeliveryStatus.Downloading(0f)
            }
            AssetPackStatus.FAILED -> {
                Log.e(TAG, "AssetPack $name failed: code ${state.errorCode()}")
                PackDeliveryStatus.Failed(state.errorCode())
            }
            AssetPackStatus.CANCELED -> {
                PackDeliveryStatus.NotInstalled
            }
            else -> PackDeliveryStatus.NotInstalled
        }

        _packStatuses.value = _packStatuses.value + (name to status)
    }

    init {
        assetPackManager.registerListener(listener)
        checkInitialStatus()
    }

    private fun checkInitialStatus() {
        val initialMap = mutableMapOf<String, PackDeliveryStatus>()
        ALL_PACKS.forEach { packName ->
            val location = assetPackManager.getPackLocation(packName)
            if (location != null && location.assetsPath() != null) {
                initialMap[packName] = PackDeliveryStatus.Installed
            } else {
                initialMap[packName] = PackDeliveryStatus.NotInstalled
            }
        }
        _packStatuses.value = initialMap
    }

    fun isPackInstalled(packName: String): Boolean {
        val status = _packStatuses.value[packName]
        if (status is PackDeliveryStatus.Installed) return true
        val location = assetPackManager.getPackLocation(packName)
        val installed = location != null && location.assetsPath() != null
        if (installed) {
            _packStatuses.value = _packStatuses.value + (packName to PackDeliveryStatus.Installed)
        }
        return installed
    }

    fun requestPackDownload(packName: String) {
        if (isPackInstalled(packName)) {
            Log.d(TAG, "Pack $packName is already installed.")
            return
        }

        Log.d(TAG, "Requesting on-demand download for $packName via Play Asset Delivery...")
        _packStatuses.value = _packStatuses.value + (packName to PackDeliveryStatus.Downloading(0f))

        assetPackManager.fetch(listOf(packName))
            .addOnSuccessListener {
                Log.d(TAG, "Successfully initiated download for pack: $packName")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to initiate download for pack $packName: ${e.message}", e)
                _packStatuses.value = _packStatuses.value + (packName to PackDeliveryStatus.Failed(-1, e.message.orEmpty()))
            }
    }

    fun cancelPackDownload(packName: String) {
        assetPackManager.cancel(listOf(packName))
        _packStatuses.value = _packStatuses.value + (packName to PackDeliveryStatus.NotInstalled)
    }

    /**
     * Resolves an asset file path from installed asset packs.
     * @param relPath Relative path, e.g. "audio/es/ex_1397.m4a"
     * @return Existing File on disk if the pack is installed and contains the file, null otherwise.
     */
    fun getAssetFile(relPath: String): File? {
        ALL_PACKS.forEach { packName ->
            val location: AssetPackLocation? = assetPackManager.getPackLocation(packName)
            val basePath = location?.assetsPath()
            if (!basePath.isNullOrBlank()) {
                val candidate = File(basePath, relPath)
                if (candidate.exists() && candidate.length() > 0) {
                    return candidate
                }
            }
        }
        return null
    }

    /**
     * Lists file names present inside a relative directory across all installed asset packs.
     */
    fun listAssetPackFiles(relDir: String): Set<String> {
        val results = mutableSetOf<String>()
        ALL_PACKS.forEach { packName ->
            val location = assetPackManager.getPackLocation(packName)
            val basePath = location?.assetsPath()
            if (!basePath.isNullOrBlank()) {
                val dir = File(basePath, relDir)
                if (dir.exists() && dir.isDirectory) {
                    dir.list()?.let { results.addAll(it) }
                }
            }
        }
        return results
    }
}

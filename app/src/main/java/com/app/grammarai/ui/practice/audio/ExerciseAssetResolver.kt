package com.app.grammarai.ui.practice.audio

import android.content.Context
import com.app.grammarai.data.model.ExerciseType
import org.json.JSONObject

/**
 * Resolves asset paths, manifest lookups, and pre-generated audio files
 * stored in the Android assets directory.
 */
class ExerciseAssetResolver(
    private val context: Context
) {
    val wordsManifest: Map<String, String> by lazy {
        loadWordsManifest()
    }

    private fun loadWordsManifest(): Map<String, String> {
        return runCatching {
            context.assets.open("audio/words/words_manifest.json").use { stream ->
                val jsonStr = stream.bufferedReader().readText()
                val json = JSONObject(jsonStr)
                val wordsObj = json.optJSONObject("words") ?: return@use emptyMap<String, String>()
                val map = mutableMapOf<String, String>()
                wordsObj.keys().forEach { key ->
                    map[key.lowercase()] = wordsObj.getString(key)
                }
                map
            }
        }.getOrDefault(emptyMap())
    }

    /**
     * Checks whether an asset file exists in the bundle or installed asset packs.
     */
    fun assetExists(assetPath: String): Boolean {
        val packFile = runCatching {
            com.app.grammarai.GrammarApplication.instance.playAssetDeliveryManager.getAssetFile(assetPath)
        }.getOrNull()
        if (packFile != null && packFile.exists()) return true

        return runCatching {
            context.assets.open(assetPath).close()
            true
        }.getOrDefault(false)
    }

    /**
     * Resolves the relative asset path for a vocabulary word from words_manifest.json.
     */
    fun resolveWordAssetPath(cleanWord: String): String {
        val relPath = wordsManifest[cleanWord]
        return if (relPath != null) {
            if (relPath.startsWith("audio/")) relPath else "audio/$relPath"
        } else {
            val slug = cleanWord.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            "audio/words/w_$slug.m4a"
        }
    }

    fun resolveOptionAssetPath(exerciseId: String, optionIndex: Int, lang: String): String {
        return "audio/$lang/${exerciseId}_opt_${optionIndex}.m4a"
    }

    fun resolveFeedbackAssetPath(exerciseId: String, lang: String): String {
        return "audio/$lang/${exerciseId}_correct.m4a"
    }

    fun resolveGenericPromptAssetPath(exerciseType: ExerciseType, lang: String): String {
        return "audio/$lang/prompt_${exerciseType.name.lowercase()}.m4a"
    }

    fun resolvePhraseAssetPath(exerciseId: String, lang: String): String {
        return "audio/$lang/${exerciseId}_phrase.m4a"
    }

    fun resolveDialogueAssetPath(exerciseId: String, lang: String): String {
        return "audio/$lang/${exerciseId}_dialogue.m4a"
    }

    fun resolveSpecificPromptAssetPath(exerciseId: String, lang: String): String {
        return "audio/$lang/$exerciseId.m4a"
    }

    /**
     * Finds the asset path for an exercise audio clip, supporting exact matches,
     * quote/space variations, double vs single underscores, and manifest normalization.
     */
    fun findAssetPathForExercise(
        exerciseId: String,
        exerciseType: ExerciseType?,
        lang: String
    ): String? {
        val baseLangFiles = runCatching { context.assets.list("audio/$lang")?.toSet() ?: emptySet() }.getOrDefault(emptySet())
        val packLangFiles = runCatching {
            com.app.grammarai.GrammarApplication.instance.playAssetDeliveryManager.listAssetPackFiles("audio/$lang")
        }.getOrDefault(emptySet())
        val langFiles = baseLangFiles + packLangFiles

        val baseRootFiles = runCatching { context.assets.list("audio")?.toSet() ?: emptySet() }.getOrDefault(emptySet())
        val packRootFiles = runCatching {
            com.app.grammarai.GrammarApplication.instance.playAssetDeliveryManager.listAssetPackFiles("audio")
        }.getOrDefault(emptySet())
        val rootFiles = baseRootFiles + packRootFiles

        val rawId = exerciseId
        val cleanId = exerciseId.replace("\"", "").replace(" ", "_")
        val pySlug = exerciseId.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
        val singleUnderscoreId = pySlug.replace(Regex("_+"), "_")

        val baseSlugs = mutableSetOf(rawId, cleanId, pySlug, singleUnderscoreId)

        if (exerciseType == ExerciseType.THEORY || exerciseId.contains("theory")) {
            val stripped = listOf(
                cleanId.removePrefix("theory_"),
                pySlug.removePrefix("theory_"),
                singleUnderscoreId.removePrefix("theory_"),
                exerciseId.removePrefix("theory_")
            )
            stripped.forEach { s ->
                baseSlugs.add("theory_$s")
                baseSlugs.add("theory__$s")
                baseSlugs.add(s)
            }
        }

        // 1. Direct candidate matching
        for (slug in baseSlugs) {
            if (langFiles.contains("$slug.m4a")) return "audio/$lang/$slug.m4a"
            if (langFiles.contains("$slug.wav")) return "audio/$lang/$slug.wav"
            if (rootFiles.contains("$slug.m4a")) return "audio/$slug.m4a"
            if (rootFiles.contains("$slug.wav")) return "audio/$slug.wav"
        }

        // 2. Normalized matching (ignoring consecutive underscores and lowercase)
        fun normalizeName(name: String) = name.removeSuffix(".m4a").removeSuffix(".wav").replace("_", "").lowercase()
        val normalizedCandidates = baseSlugs.map { normalizeName(it) }.toSet()

        val matchedLang = langFiles.firstOrNull { normalizeName(it) in normalizedCandidates }
        if (matchedLang != null) return "audio/$lang/$matchedLang"

        val matchedRoot = rootFiles.firstOrNull { normalizeName(it) in normalizedCandidates }
        if (matchedRoot != null) return "audio/$matchedRoot"

        return null
    }

    /**
     * Resolves pre-generated spoken script for a theory exercise if available.
     */
    fun findTheoryScriptContent(exerciseId: String, lang: String): String? {
        val scriptDirs = listOf("audio/$lang/theory_scripts", "audio/es/theory_scripts", "audio/en/theory_scripts")
        fun normalizeName(name: String) = name.removeSuffix(".txt").replace("theory_", "").replace("_", "").lowercase()

        val rawClean = exerciseId.removePrefix("theory_").replace("\"", "").replace(" ", "_")
        val pySlug = exerciseId.removePrefix("theory_").replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_')
        val candidateSlugs = setOf(
            rawClean,
            pySlug,
            rawClean.replace(Regex("_+"), "_"),
            normalizeName(rawClean),
            normalizeName(pySlug)
        )

        for (dir in scriptDirs) {
            val files = runCatching { context.assets.list(dir)?.toList() ?: emptyList() }.getOrDefault(emptyList())
            val matched = files.firstOrNull { f ->
                val base = f.removeSuffix(".txt")
                base in candidateSlugs || normalizeName(f) in candidateSlugs
            }
            if (matched != null) {
                try {
                    context.assets.open("$dir/$matched").use { stream ->
                        val content = stream.bufferedReader().readText().trim()
                        if (content.isNotBlank()) return content
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }
}

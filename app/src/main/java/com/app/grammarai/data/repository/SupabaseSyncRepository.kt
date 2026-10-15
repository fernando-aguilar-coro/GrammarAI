package com.app.grammarai.data.repository

import com.app.grammarai.data.local.PreferencesManager
import com.app.grammarai.data.model.SkillMetrics
import com.app.grammarai.data.remote.supabase.SupabaseAuthResponse
import com.app.grammarai.data.remote.supabase.SupabaseSyncClient
import com.app.grammarai.data.remote.supabase.SupabaseSyncPayload
import com.app.grammarai.data.remote.supabase.SupabaseSyncResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupabaseSyncRepository(
    private val prefs: PreferencesManager,
    private val client: SupabaseSyncClient = SupabaseSyncClient()
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun signInWithGoogle(idToken: String): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        val authResult = client.signInWithGoogleIdToken(idToken)
        authResult.onSuccess { authResponse ->
            authResponse.accessToken?.let { prefs.supabaseAccessToken = it }
            authResponse.refreshToken?.let { prefs.supabaseRefreshToken = it }

            authResponse.user?.let { user ->
                prefs.userId = user.id
                user.email?.let { prefs.userEmail = it }

                val metadata = user.userMetadata
                val fullName = metadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                    ?: metadata?.get("name")?.jsonPrimitive?.contentOrNull
                if (!fullName.isNullOrBlank()) {
                    prefs.userName = fullName
                }

                val avatar = metadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
                    ?: metadata?.get("picture")?.jsonPrimitive?.contentOrNull
                if (!avatar.isNullOrBlank()) {
                    prefs.userAvatarUrl = avatar
                }

                // Immediately restore user cloud data to sync progress
                restoreFromCloud(user.id)
            }
        }
        authResult
    }

    fun signOut() {
        prefs.clearAuth()
    }

    suspend fun syncUserData(): Result<SupabaseSyncResponse> = withContext(Dispatchers.IO) {
        if (!prefs.isCloudSyncEnabled) {
            return@withContext Result.failure(IllegalStateException("Cloud sync is disabled in preferences."))
        }
        if (!prefs.isAuthenticated) {
            return@withContext Result.failure(IllegalStateException("User is not authenticated with Supabase."))
        }

        // 1. Fetch current cloud state and merge before pushing to prevent wiping cloud progress
        val fetchResult = client.fetchUserData(prefs.userId, prefs.supabaseAccessToken)
        fetchResult.onSuccess { cloudResponse ->
            if (cloudResponse.success) {
                applyResponseToLocal(cloudResponse)
            }
        }

        val todayStr = dateFormat.format(Date())
        val metrics = prefs.skillMetrics

        // 2. Prepare payload with the merged state
        val payload = SupabaseSyncPayload(
            userId = prefs.userId,
            userEmail = prefs.userEmail,
            username = prefs.userName,
            avatarUrl = prefs.userAvatarUrl,
            appLanguage = prefs.appLanguage,
            nativeLanguage = prefs.nativeLanguage,
            primaryGoal = prefs.primaryGoal,
            aiPersonaPrompt = prefs.aiPersonaPrompt,
            aiReadingPrompt = prefs.aiReadingTonePrompt,
            aiSuggestionsPrompt = prefs.aiSuggestionsPrompt,
            completedLessons = prefs.completedLessons.toList(),
            lessonScores = prefs.lessonScores,
            xp = prefs.userXp,
            currentStreak = prefs.streakDays,
            highestStreak = prefs.streakDays,
            targetLessonsCount = prefs.lessonGoalTarget,
            completedLessonsCount = prefs.completedLessons.size,
            lastActiveDate = todayStr,
            productive = metrics.productive,
            pragmatics = metrics.pragmatics,
            receptive = metrics.receptive,
            structure = metrics.structure,
            lexicon = metrics.lexicon,
            overallCefr = metrics.overallCefr
        )

        val result = client.upsertUserData(payload, prefs.supabaseAccessToken)
        result.onSuccess { response ->
            if (response.success) {
                prefs.lastSyncTimestamp = System.currentTimeMillis()
            }
        }
        result
    }

    suspend fun restoreFromCloud(userId: String): Result<SupabaseSyncResponse> = withContext(Dispatchers.IO) {
        val trimmed = userId.trim()
        val fetchResult = client.fetchUserData(
            userId = trimmed,
            accessToken = prefs.supabaseAccessToken
        )

        fetchResult.onSuccess { response ->
            if (response.success) {
                prefs.userId = trimmed
                applyResponseToLocal(response)
                prefs.lastSyncTimestamp = System.currentTimeMillis()
            }
        }
        fetchResult
    }

    private fun applyResponseToLocal(response: SupabaseSyncResponse) {
        response.userId?.let { if (it.isNotBlank()) prefs.userId = it }

        response.profile?.let { profile ->
            profile.username?.let { cloudName ->
                if (cloudName.isNotBlank()) {
                    val isOldAutoGenerated = cloudName.matches(Regex("""^neko_\d+$"""))
                    if (!isOldAutoGenerated || prefs.userName.isBlank() || prefs.userName == "neko") {
                        prefs.userName = if (isOldAutoGenerated) "neko" else cloudName
                    }
                }
            }
            profile.avatarUrl?.let { if (it.isNotBlank()) prefs.userAvatarUrl = it }
            profile.appLanguage?.let { if (it.isNotBlank()) prefs.appLanguage = it }
            profile.nativeLanguage?.let { if (it.isNotBlank()) prefs.nativeLanguage = it }
            profile.primaryGoal?.let { if (it.isNotBlank()) prefs.primaryGoal = it }
            profile.aiPersonaPrompt?.let { if (it.isNotBlank()) prefs.aiPersonaPrompt = it }
            profile.aiReadingPrompt?.let { if (it.isNotBlank()) prefs.aiReadingTonePrompt = it }
            profile.aiSuggestionsPrompt?.let { if (it.isNotBlank()) prefs.aiSuggestionsPrompt = it }

            if (profile.lessonIds.isNotEmpty()) {
                val merged = prefs.completedLessons.toMutableSet()
                merged.addAll(profile.lessonIds)
                prefs.completedLessons = merged
            }

            if (profile.lessonScores.isNotEmpty()) {
                val mergedScores = prefs.lessonScores.toMutableMap()
                profile.lessonScores.forEach { (lessonId, cloudScore) ->
                    val localScore = mergedScores[lessonId] ?: 0
                    mergedScores[lessonId] = maxOf(localScore, cloudScore)
                }
                prefs.lessonScores = mergedScores
            }
        }

        response.currency?.let { currency ->
            if (currency.xp > prefs.userXp) {
                prefs.userXp = currency.xp
            }
        }

        response.streak?.let { streak ->
            if (streak.currentStreak > prefs.streakDays) {
                prefs.streakDays = streak.currentStreak
            }
            if (streak.targetLessonsCount > 0) {
                prefs.lessonGoalTarget = streak.targetLessonsCount
            }
        }

        response.skills?.let { s ->
            val cur = prefs.skillMetrics
            prefs.skillMetrics = SkillMetrics(
                productive = maxOf(cur.productive, s.productive),
                pragmatics = maxOf(cur.pragmatics, s.pragmatics),
                receptive = maxOf(cur.receptive, s.receptive),
                structure = maxOf(cur.structure, s.structure),
                lexicon = maxOf(cur.lexicon, s.lexicon),
                overallCefr = s.overallCefr.ifBlank { cur.overallCefr }
            )
        }
    }
}

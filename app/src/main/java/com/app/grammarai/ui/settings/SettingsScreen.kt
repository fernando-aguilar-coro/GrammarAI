package com.app.grammarai.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.app.grammarai.core.util.LocaleHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.app.grammarai.R
import com.app.grammarai.ui.common.components.GrammarButton
import com.app.grammarai.ui.common.components.GrammarButtonStyle
import com.app.grammarai.ui.common.components.GrammarIconButton
import com.app.grammarai.ui.settings.components.SettingsByokCard
import com.app.grammarai.ui.settings.components.SettingsLanguageCard
import com.app.grammarai.ui.settings.components.SettingsProfileCard
import com.app.grammarai.ui.settings.components.SettingsPromptsCard
import com.app.grammarai.ui.settings.components.SettingsReminderCard
import com.app.grammarai.ui.theme.AccentPrimary
import com.app.grammarai.ui.theme.BorderSubtle
import com.app.grammarai.ui.theme.DarkBackground
import com.app.grammarai.ui.theme.DarkSurfaceCard
import com.app.grammarai.ui.theme.SuccessEmerald
import com.app.grammarai.ui.theme.TextPrimary
import com.app.grammarai.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.settings_reminder_perm_denied),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val parseResult = com.app.grammarai.core.auth.GoogleAuthManager.parseSignInResult(result.data)
        parseResult.onSuccess { account ->
            val idToken = account.idToken
            if (!idToken.isNullOrBlank()) {
                viewModel.processIntent(SettingsUiIntent.SignInWithGoogle(idToken))
            } else {
                android.widget.Toast.makeText(context, "No Google ID token received", android.widget.Toast.LENGTH_SHORT).show()
            }
        }.onFailure { err ->
            android.widget.Toast.makeText(context, "Google Sign-In failed: ${err.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsUiEffect.NavigateBack -> onNavigateBack()
                is SettingsUiEffect.LanguageChanged -> {
                    LocaleHelper.applyLanguage(context, effect.newLanguage)
                    (context as? android.app.Activity)?.recreate()
                }
                is SettingsUiEffect.ShowSnackbar -> {
                    android.widget.Toast.makeText(context, effect.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GrammarIconButton(
                        icon = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        onClick = { viewModel.processIntent(SettingsUiIntent.Back) }
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                GrammarButton(
                    text = stringResource(R.string.settings_save),
                    onClick = { viewModel.processIntent(SettingsUiIntent.SaveSettings) },
                    style = GrammarButtonStyle.PRIMARY,
                    leadingIcon = Icons.Outlined.Save,
                    height = 40.dp
                )
            }

            if (state.isSavedSuccess) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SuccessEmerald.copy(alpha = 0.12f))
                        .border(1.dp, SuccessEmerald.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = SuccessEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.settings_saved_success),
                            color = SuccessEmerald,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Profile Card (User Name)
            SettingsProfileCard(
                userName = state.userName,
                onUserNameChange = { viewModel.processIntent(SettingsUiIntent.UpdateUserName(it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Weekly Lesson Goal Card
            com.app.grammarai.ui.settings.components.SettingsGoalCard(
                currentGoal = state.lessonGoalTarget,
                onGoalChange = { viewModel.processIntent(SettingsUiIntent.UpdateLessonGoalTarget(it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Daily Reminders Card
            SettingsReminderCard(
                enabled = state.dailyReminderEnabled,
                currentTime = state.dailyReminderTime,
                onToggle = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    viewModel.processIntent(SettingsUiIntent.ToggleDailyReminder(enabled))
                },
                onTimeSelected = { time ->
                    viewModel.processIntent(SettingsUiIntent.UpdateDailyReminderTime(time))
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // BYOK Card
            SettingsByokCard(
                apiKey = state.apiKey,
                isTestingKey = state.isTestingKey,
                keyValidationStatus = state.keyValidationStatus,
                onApiKeyChange = { viewModel.processIntent(SettingsUiIntent.UpdateApiKey(it)) },
                onTestKey = { viewModel.processIntent(SettingsUiIntent.TestApiKey) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Supabase Cloud Sync Card
            com.app.grammarai.ui.settings.components.SettingsSupabaseSyncCard(
                userId = state.syncUserId,
                userEmail = state.syncUserEmail,
                isAuthenticated = state.isAuthenticated,
                isAuthenticating = state.isAuthenticating,
                lastSyncTimestamp = state.lastSyncTimestamp,
                isSyncing = state.isSyncing,
                isCloudSyncEnabled = state.isCloudSyncEnabled,
                syncStatusMessage = state.syncStatusMessage,
                onSignInWithGoogle = {
                    val client = com.app.grammarai.core.auth.GoogleAuthManager.getClient(context)
                    googleSignInLauncher.launch(client.signInIntent)
                },
                onSignOut = {
                    com.app.grammarai.core.auth.GoogleAuthManager.signOut(context) {
                        viewModel.processIntent(SettingsUiIntent.SignOut)
                    }
                },
                onSyncNow = { viewModel.processIntent(SettingsUiIntent.SyncNow) },
                onRestoreFromCloud = { viewModel.processIntent(SettingsUiIntent.RestoreFromCloud()) },
                onToggleCloudSync = { viewModel.processIntent(SettingsUiIntent.ToggleCloudSync(it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Language Card
            SettingsLanguageCard(
                currentLanguage = state.appLanguage,
                onLanguageSelect = { viewModel.processIntent(SettingsUiIntent.SelectLanguage(it)) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Prompts Card (Conversation Persona)
            SettingsPromptsCard(
                personaPrompt = state.personaPrompt,
                onPersonaChange = { viewModel.processIntent(SettingsUiIntent.UpdatePersonaPrompt(it)) },
                onResetPrompts = { viewModel.processIntent(SettingsUiIntent.ResetPrompts) }
            )

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

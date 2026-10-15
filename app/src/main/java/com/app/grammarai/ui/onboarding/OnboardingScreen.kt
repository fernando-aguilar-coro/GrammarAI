package com.app.grammarai.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.grammarai.ui.onboarding.components.AiChatBubble
import com.app.grammarai.ui.onboarding.components.ByokCard
import com.app.grammarai.ui.onboarding.components.OnboardingInputBar
import com.app.grammarai.ui.onboarding.components.OnboardingLoadingScreen
import com.app.grammarai.ui.onboarding.components.OnboardingTopBar
import com.app.grammarai.ui.onboarding.components.TypingIndicator
import com.app.grammarai.ui.onboarding.components.UserChatBubble
import com.app.grammarai.ui.onboarding.components.UserSuggestedReplies
import com.app.grammarai.ui.theme.DarkBackground

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.app.grammarai.core.auth.GoogleAuthManager
import com.app.grammarai.ui.onboarding.components.OnboardingGoogleSyncCard

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToFeed: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val parseResult = GoogleAuthManager.parseSignInResult(result.data)
        parseResult.onSuccess { account ->
            val idToken = account.idToken
            if (!idToken.isNullOrBlank()) {
                viewModel.processIntent(OnboardingUiIntent.SignInWithGoogle(idToken))
            } else {
                Toast.makeText(context, "No Google ID token received", Toast.LENGTH_SHORT).show()
            }
        }.onFailure { err ->
            Toast.makeText(context, "Google Sign-In failed: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OnboardingUiEffect.NavigateToFeed -> onNavigateToFeed()
                is OnboardingUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Only scroll when a new message is appended (not on keyboard or step changes)
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    AnimatedContent(
        targetState = Pair(state.isLoadingIntro, state.isFinalizing),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "onboarding_screen_state"
    ) { (isLoadingIntro, isFinalizing) ->
        when {
            isLoadingIntro -> {
                OnboardingLoadingScreen(
                    isFinalizing = false,
                    onSkip = { viewModel.processIntent(OnboardingUiIntent.DismissIntroLoading) }
                )
            }

            isFinalizing -> {
                OnboardingLoadingScreen(
                    isFinalizing = true
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(horizontal = 14.dp)
                        .padding(top = 8.dp)
                        .imePadding()
                        .padding(bottom = 8.dp)
                ) {
                    // Top Navigation / Status Header
                    OnboardingTopBar(
                        isPlayingAudio = state.isPlayingAudio,
                        isAiTyping = state.isAiTyping,
                        onSkipClick = { viewModel.processIntent(OnboardingUiIntent.SkipOnboarding) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Messaging Chat Stream (fills remaining available space cleanly)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.messages,
                            key = { "${it.id}_${it.text.hashCode()}" }
                        ) { msg ->
                            if (msg.isFromAi) {
                                val isLastAiMessage = msg.id == state.messages.lastOrNull { it.isFromAi }?.id
                                AiChatBubble(
                                    message = msg,
                                    isAudioLoading = state.isAudioLoading && state.currentAudioMessageId == msg.id,
                                    isPlayingAudio = state.isPlayingAudio && state.currentAudioMessageId == msg.id,
                                    onToggleAudio = {
                                        viewModel.processIntent(OnboardingUiIntent.ToggleAudio(msg.id, msg.text))
                                    },
                                    onToggleTranslation = {
                                        viewModel.processIntent(OnboardingUiIntent.ToggleTranslation(msg.id))
                                    }
                                )

                                // Render selectable options on the user's side (outgoing message style)
                                if (isLastAiMessage && msg.chips.isNotEmpty() && !state.isAiTyping) {
                                    val isSituationsStep = state.currentStep == OnboardingStep.SITUATIONS
                                    val isEn = state.appLanguage.equals("en", ignoreCase = true)
                                    val minReq = 3
                                    val maxLimit = 5
                                    val currentCount = state.selectedSituations.size

                                    val confirmText = when {
                                        currentCount < minReq -> {
                                            if (isEn) "Select at least $minReq topics ($currentCount/$maxLimit)"
                                            else "Elige al menos $minReq categorías ($currentCount/$maxLimit)"
                                        }
                                        else -> {
                                            if (isEn) "Continue with selected scenarios ($currentCount/$maxLimit)"
                                            else "Continuar con mis escenarios ($currentCount/$maxLimit)"
                                        }
                                    }

                                    UserSuggestedReplies(
                                        chips = msg.chips,
                                        isMultiSelect = isSituationsStep,
                                        selectedChips = if (isSituationsStep) state.selectedSituations else emptySet(),
                                        minSelections = if (isSituationsStep) minReq else 1,
                                        maxSelections = if (isSituationsStep) maxLimit else 5,
                                        confirmButtonText = confirmText,
                                        onConfirmMultiSelect = if (isSituationsStep) {
                                            { viewModel.processIntent(OnboardingUiIntent.ConfirmSituationsSelection) }
                                        } else null,
                                        onSelect = { chipText ->
                                            when (state.currentStep) {
                                                OnboardingStep.PRESENTATION -> viewModel.processIntent(OnboardingUiIntent.AdvanceFromPresentation)
                                                OnboardingStep.NATIVE_LANGUAGE -> viewModel.processIntent(OnboardingUiIntent.SelectNativeLanguage(chipText))
                                                OnboardingStep.APP_LANGUAGE -> viewModel.processIntent(OnboardingUiIntent.SelectAppLanguageOption(chipText))
                                                OnboardingStep.GOAL -> viewModel.processIntent(OnboardingUiIntent.SelectGoal(chipText))
                                                OnboardingStep.SITUATIONS -> viewModel.processIntent(OnboardingUiIntent.ToggleSituationSelection(chipText))
                                                OnboardingStep.LEVEL -> viewModel.processIntent(OnboardingUiIntent.SelectLevel(chipText))
                                                OnboardingStep.WEEKLY_GOAL -> viewModel.processIntent(OnboardingUiIntent.SelectWeeklyGoal(chipText))
                                                OnboardingStep.REMINDER -> {
                                                    val wantsReminder = chipText.contains("Sí", ignoreCase = true) || chipText.contains("Yes", ignoreCase = true)
                                                    if (wantsReminder && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                        }
                                                    }
                                                    viewModel.processIntent(OnboardingUiIntent.SelectReminderOption(chipText))
                                                }
                                                OnboardingStep.API_KEY -> viewModel.processIntent(OnboardingUiIntent.SelectApiKeyOption(chipText))
                                                else -> {}
                                            }
                                        }
                                    )
                                }
                            } else {
                                UserChatBubble(message = msg)
                            }
                        }

                        if (state.isAiTyping) {
                            item(key = "typing_indicator") {
                                TypingIndicator()
                            }
                        }
                    }

                    // Direct input area anchored at bottom when on NAME step
                    when (state.currentStep) {
                        OnboardingStep.NAME -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            OnboardingInputBar(
                                value = state.nameInput,
                                onValueChange = { viewModel.processIntent(OnboardingUiIntent.UpdateNameInput(it)) },
                                onSend = { viewModel.processIntent(OnboardingUiIntent.SubmitName) }
                            )
                        }

                        OnboardingStep.API_KEY_INPUT -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            ByokCard(
                                apiKeyInput = state.apiKeyInput,
                                onApiKeyChange = { viewModel.processIntent(OnboardingUiIntent.UpdateApiKeyInput(it)) },
                                onTestKey = { viewModel.processIntent(OnboardingUiIntent.TestApiKey) },
                                onComplete = { viewModel.processIntent(OnboardingUiIntent.SubmitApiKey) },
                                onSkip = { viewModel.processIntent(OnboardingUiIntent.SkipApiKey) },
                                isTestingKey = state.isTestingKey,
                                keyValidationStatus = state.keyValidationStatus
                            )
                        }

                        OnboardingStep.SYNC_ACCOUNT -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            OnboardingGoogleSyncCard(
                                isSigningIn = state.isSigningInWithGoogle,
                                isGoogleLinked = state.isGoogleLinked,
                                linkedEmail = state.linkedGoogleEmail,
                                appLanguage = state.appLanguage,
                                onSignInWithGoogle = {
                                    val client = GoogleAuthManager.getClient(context)
                                    googleSignInLauncher.launch(client.signInIntent)
                                },
                                onContinue = { viewModel.processIntent(OnboardingUiIntent.SkipSyncAccount) },
                                onSkip = { viewModel.processIntent(OnboardingUiIntent.SkipSyncAccount) }
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}

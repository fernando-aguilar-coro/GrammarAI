package com.app.grammarai.ui.practice.drills

import androidx.compose.runtime.Composable
import com.app.grammarai.data.model.ExerciseType
import com.app.grammarai.data.model.GeneratedExercise
import com.app.grammarai.ui.lesson.components.LessonTheoryCard
import com.app.grammarai.ui.practice.ExerciseRunnerViewModel
import com.app.grammarai.ui.practice.ExerciseUiIntent
import com.app.grammarai.ui.practice.ExerciseUiState
import com.app.grammarai.ui.practice.conversation.LiveConversationCard
import com.app.grammarai.ui.practice.drills.FillBlankDrillCard
import com.app.grammarai.ui.practice.drills.FlashcardDrillCard
import com.app.grammarai.ui.practice.drills.ListeningPassageDrillCard
import com.app.grammarai.ui.practice.drills.MultipleChoiceDrillCard
import com.app.grammarai.ui.practice.drills.PronunciationDrillCard
import com.app.grammarai.ui.practice.drills.SelectPairsDrillCard
import com.app.grammarai.ui.practice.drills.SentenceReconstructDrillCard

@Composable
fun ExerciseDynamicContent(
    exercise: GeneratedExercise,
    state: ExerciseUiState,
    viewModel: ExerciseRunnerViewModel,
    onToggleMicWithPermission: () -> Unit
) {
    val onPlayAudio = { viewModel.processIntent(ExerciseUiIntent.PlayExerciseAudio) }
    val onToggleTranslation = { viewModel.processIntent(ExerciseUiIntent.ToggleExerciseTranslation) }

    when (exercise.type) {
        ExerciseType.THEORY -> {
            LessonTheoryCard(
                markdown = exercise.readingPassage.ifBlank { exercise.prompt }
            )
        }

        ExerciseType.FLASHCARDS, ExerciseType.FLASHCARD -> {
            FlashcardDrillCard(
                exercise = exercise,
                onSpeak = { text -> viewModel.processIntent(ExerciseUiIntent.SpeakText(text)) }
            )
        }

        ExerciseType.CONVERSATION_DIALOGUE -> {
            LiveConversationCard(
                conversationTurns = state.conversationTurns,
                conversationTopic = state.currentConversationTopic,
                isAiRespondingLive = state.isAiRespondingLive || state.isAudioLoading,
                isPlayingAudio = state.isPlayingAudio,
                isListening = state.isListening,
                conversationTurnCount = state.conversationTurnCount,
                maxConversationTurns = state.maxConversationTurns,
                liveStreamingAiText = state.liveStreamingAiText,
                liveStreamingUserText = state.liveStreamingUserText,
                isTopicSwitchable = state.lesson?.type != com.app.grammarai.data.model.LessonType.SITUATIONAL_VOCABULARY && exercise.category.canonical != com.app.grammarai.data.model.SkillCategory.LEXICON,
                onChangeTopic = { newTopic -> viewModel.processIntent(ExerciseUiIntent.ChangeConversationTopic(newTopic)) },
                onToggleMic = onToggleMicWithPermission,
                onSkipConversation = { viewModel.processIntent(ExerciseUiIntent.NextExercise) }
            )
        }

        ExerciseType.PRONUNCIATION, ExerciseType.REPEAT_AFTER_ME, ExerciseType.INTONATION_CHALLENGE -> {
            PronunciationDrillCard(
                exercise = exercise,
                isListening = state.isListening,
                spokenTranscript = state.spokenTranscript,
                matchScore = state.pronunciationMatchScore,
                onToggleRecord = onToggleMicWithPermission,
                onSkip = { viewModel.processIntent(ExerciseUiIntent.GiveUpExercise) },
                onPlayAudio = onPlayAudio,
                isTranslationSupported = state.isCurrentExerciseTranslatable,
                isTranslationVisible = state.isTranslationVisible,
                onToggleTranslation = onToggleTranslation
            )
        }

        ExerciseType.SCRAMBLED_SENTENCE, ExerciseType.SENTENCE_RECONSTRUCTION, ExerciseType.SCRAMBLE_LISTENING -> {
            SentenceReconstructDrillCard(
                exercise = exercise,
                reconstructedWords = state.reconstructedWords,
                isAnswerChecked = state.isAnswerChecked,
                isCorrect = state.isCorrect,
                onWordTap = { viewModel.processIntent(ExerciseUiIntent.TapReconstructWord(it)) },
                onRemoveWord = { viewModel.processIntent(ExerciseUiIntent.RemoveReconstructWord(it)) },
                onClear = { viewModel.processIntent(ExerciseUiIntent.ClearReconstruction) },
                onPlayAudio = onPlayAudio,
                isTranslationSupported = state.isCurrentExerciseTranslatable,
                isTranslationVisible = state.isTranslationVisible,
                onToggleTranslation = onToggleTranslation
            )
        }

        ExerciseType.SELECT_PAIRS -> {
            SelectPairsDrillCard(
                exercise = exercise,
                selectedLeftId = state.selectedPairLeftId,
                selectedRightId = state.selectedPairRightId,
                matchedLeftIds = state.matchedLeftIds,
                matchedRightIds = state.matchedRightIds,
                pairError = state.pairError,
                onSelectLeft = { id, text -> viewModel.processIntent(ExerciseUiIntent.SelectPairLeft(id, text)) },
                onSelectRight = { id, text -> viewModel.processIntent(ExerciseUiIntent.SelectPairRight(id, text)) },
                onPlayAudio = onPlayAudio,
                isTranslationSupported = state.isCurrentExerciseTranslatable,
                isTranslationVisible = state.isTranslationVisible,
                onToggleTranslation = onToggleTranslation
            )
        }

        ExerciseType.FILL_BLANK, ExerciseType.FILL_IN_BLANK -> {
            FillBlankDrillCard(
                exercise = exercise,
                selectedOption = state.selectedOption,
                isAnswerChecked = state.isAnswerChecked,
                isCorrect = state.isCorrect,
                onSelectOption = { viewModel.processIntent(ExerciseUiIntent.SelectOption(it)) },
                onSpeakOption = { viewModel.processIntent(ExerciseUiIntent.SpeakText(it)) },
                onInputText = { viewModel.processIntent(ExerciseUiIntent.InputText(it)) },
                onPlayAudio = onPlayAudio,
                isTranslationSupported = state.isCurrentExerciseTranslatable,
                isTranslationVisible = state.isTranslationVisible,
                onToggleTranslation = onToggleTranslation
            )
        }

        ExerciseType.TRANSLATE -> {
            TranslateDrillCard(
                exercise = exercise,
                answerMode = state.translationAnswerMode,
                textAnswer = state.translationTextAnswer,
                orderedWords = state.translationOrderedWords,
                isAnswerChecked = state.isAnswerChecked,
                isCorrect = state.isCorrect,
                onAnswerModeChange = { viewModel.processIntent(ExerciseUiIntent.SetTranslationAnswerMode(it)) },
                onTextAnswerChange = { viewModel.processIntent(ExerciseUiIntent.InputTranslationAnswer(it)) },
                onAddWord = { viewModel.processIntent(ExerciseUiIntent.AddTranslationWord(it)) },
                onRemoveWord = { viewModel.processIntent(ExerciseUiIntent.RemoveTranslationWord(it)) },
                onClearWords = { viewModel.processIntent(ExerciseUiIntent.ClearTranslationWords) },
                onPlayAudio = onPlayAudio,
                isTranslationSupported = state.isCurrentExerciseTranslatable,
                isTranslationVisible = state.isTranslationVisible,
                onToggleTranslation = onToggleTranslation
            )
        }

        ExerciseType.LISTENING_PASSAGE -> {
            ListeningPassageDrillCard(
                exercise = exercise,
                selectedOption = state.selectedOption,
                isAnswerChecked = state.isAnswerChecked,
                onSelectOption = { viewModel.processIntent(ExerciseUiIntent.SelectOption(it)) },
                onSpeakOption = { viewModel.processIntent(ExerciseUiIntent.SpeakText(it)) },
                onPlayAudio = onPlayAudio,
                isTranslationSupported = state.isCurrentExerciseTranslatable,
                isTranslationVisible = state.isTranslationVisible,
                onToggleTranslation = onToggleTranslation
            )
        }

        else -> {
            // Covers MULTIPLE_CHOICE, READING, LONG_READING
            MultipleChoiceDrillCard(
                exercise = exercise,
                selectedOption = state.selectedOption,
                isAnswerChecked = state.isAnswerChecked,
                onSelectOption = { viewModel.processIntent(ExerciseUiIntent.SelectOption(it)) },
                onSpeakOption = { viewModel.processIntent(ExerciseUiIntent.SpeakText(it)) },
                onPlayAudio = onPlayAudio,
                isTranslationSupported = state.isCurrentExerciseTranslatable,
                isTranslationVisible = state.isTranslationVisible,
                onToggleTranslation = onToggleTranslation
            )
        }
    }
}

package com.app.grammarai.core.audio.delivery

import com.app.grammarai.core.mvi.UiEffect
import com.app.grammarai.core.mvi.UiIntent
import com.app.grammarai.core.mvi.UiState

sealed interface PackDeliveryStatus {
    object NotInstalled : PackDeliveryStatus
    data class Downloading(val progress: Float) : PackDeliveryStatus
    object Installed : PackDeliveryStatus
    data class Failed(val errorCode: Int = -1, val message: String = "") : PackDeliveryStatus
}

data class AudioDeliveryUiState(
    val packStatuses: Map<String, PackDeliveryStatus> = emptyMap(),
    val isPrefetching: Boolean = false
) : UiState

sealed interface AudioDeliveryUiIntent : UiIntent {
    data class CheckPrefetch(val highestReachedLessonIndex: Int) : AudioDeliveryUiIntent
    data class RequestPackDownload(val packName: String) : AudioDeliveryUiIntent
    data class CancelPackDownload(val packName: String) : AudioDeliveryUiIntent
}

sealed interface AudioDeliveryUiEffect : UiEffect {
    data class DownloadStarted(val packName: String) : AudioDeliveryUiEffect
    data class DownloadCompleted(val packName: String) : AudioDeliveryUiEffect
    data class DownloadFailed(val packName: String, val message: String) : AudioDeliveryUiEffect
}

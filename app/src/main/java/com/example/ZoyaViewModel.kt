package com.example

import androidx.lifecycle.ViewModel
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.audio.LiveAudioSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class ZoyaViewModel : ViewModel() {
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val _uiState = MutableStateFlow(LiveAudioSession.State.DISCONNECTED)
    val uiState: StateFlow<LiveAudioSession.State> = _uiState.asStateFlow()

    private val _appLaunchEvent = MutableSharedFlow<String>()
    val appLaunchEvent = _appLaunchEvent.asSharedFlow()

    private var liveSession: LiveAudioSession? = null
    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null

    fun toggleConnection() {
        if (_uiState.value == LiveAudioSession.State.DISCONNECTED) {
            connect()
        } else {
            disconnect()
        }
    }

    private fun connect() {
        audioPlayer = AudioPlayer().apply { start() }
        audioRecorder = AudioRecorder { buffer, length ->
            liveSession?.sendAudio(buffer, length)
        }.apply { start() }

        liveSession = LiveAudioSession(
            apiKey = apiKey,
            onStateChange = { state ->
                _uiState.value = state
                if (state == LiveAudioSession.State.DISCONNECTED) {
                    cleanup()
                }
            },
            onAudioReceived = { bytes ->
                audioPlayer?.playChunk(bytes)
            },
            onAppAction = { appName ->
                viewModelScope.launch {
                    _appLaunchEvent.emit(appName)
                }
            }
        ).apply { connect() }
    }

    private fun cleanup() {
        audioRecorder?.stop()
        audioRecorder = null
        audioPlayer?.stop()
        audioPlayer = null
    }

    fun disconnect() {
        liveSession?.disconnect()
        cleanup()
        _uiState.value = LiveAudioSession.State.DISCONNECTED
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}

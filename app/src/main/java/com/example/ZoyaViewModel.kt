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

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private var liveSession: LiveAudioSession? = null
    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null

    fun toggleConnection() {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            viewModelScope.launch {
                _errorEvent.emit("Missing GEMINI_API_KEY! Please add it in the Secrets panel on the left.")
            }
            return
        }
        if (_uiState.value == LiveAudioSession.State.DISCONNECTED || _uiState.value == LiveAudioSession.State.ERROR) {
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
                if (state == LiveAudioSession.State.DISCONNECTED || state == LiveAudioSession.State.ERROR) {
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
            },
            onError = { errorMsg ->
                viewModelScope.launch {
                    _errorEvent.emit(errorMsg)
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

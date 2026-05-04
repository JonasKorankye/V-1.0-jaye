package com.flipverse.chat

import androidx.lifecycle.ViewModel
import com.flipverse.shared.util.TTSState
import com.flipverse.shared.util.getTTSProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class TTSViewModel : ViewModel() {
    // Track highlighted word range while speaking
    private val _currentWordRange = MutableStateFlow(-1..-1)
    val currentWordRange: StateFlow<IntRange> = _currentWordRange

    // Manage TTS state (IDLE, PLAYING, PAUSED)
    private val _ttsState = MutableStateFlow(TTSState.IDLE)
    val ttsState: StateFlow<TTSState> = _ttsState

    // Track initialization status
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    // Get platform-specific TTS provider (Android/iOS)
    private val ttsManager = getTTSProvider()

    init {
        ttsManager.initialize {
            _isInitialized.value = true
        }
    }

    fun speak(text: String) {
        // Reset highlight immediately when starting
        _currentWordRange.update {
            -1..-1
        }

        ttsManager.speak(
            text = text,
            onWordBoundary = { wordStart, wordEnd ->
                _currentWordRange.update {
                    wordStart..wordEnd
                }
            },
            onStart = {
                _ttsState.update {
                    TTSState.PLAYING
                }
            },
            onComplete = {
                _ttsState.update {
                    TTSState.IDLE
                }
                _currentWordRange.update {
                    -1..-1
                }
            }
        )
    }

    fun stop() {
        ttsManager.stop()
        _ttsState.update {
            TTSState.IDLE
        }
        _currentWordRange.update {
            -1..-1
        }
    }

    fun pause() {
        if (_ttsState.value == TTSState.PLAYING) {
            ttsManager.pause()
            _ttsState.update {
                TTSState.PAUSED
            }
        }
    }

    fun resume() {
        if (_ttsState.value == TTSState.PAUSED) {
            ttsManager.resume()
            _ttsState.update {
                TTSState.PLAYING
            }
        }
    }

    fun isPlaying(): Boolean = ttsManager.isPlaying()
    fun isPaused(): Boolean = ttsManager.isPaused()
    fun isIdle(): Boolean = _ttsState.value == TTSState.IDLE

    fun release() {
        ttsManager.release()
    }

}
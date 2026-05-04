package com.flipverse.shared.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

actual fun getTTSProvider(): TTSProvider {
    return IOSTTSProvider()
}

/**
 * iOS TTS provider using AVSpeechSynthesizer.
 * Uses polling-based completion detection to avoid ObjC delegate signature conflicts.
 */
class IOSTTSProvider : TTSProvider {

    private var synthesizer: AVSpeechSynthesizer? = null
    private var currentlyPaused = false
    private val scope = CoroutineScope(Dispatchers.Main)

    @OptIn(ExperimentalForeignApi::class)
    override fun initialize(onInitialized: () -> Unit) {
        // Configure AVAudioSession so TTS audio is audible on all iPhone models,
        // including iPhone 16 / iOS 18 which require an explicit session category.
        // Without this, AVSpeechSynthesizer can fail silently on newer devices.
        // Setting the category to playback is the critical step — without this,
        // AVSpeechSynthesizer fails silently on iOS 17/18 (iPhone 15 Pro / 16 series).
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error = null)

        synthesizer = AVSpeechSynthesizer()
        onInitialized()
    }

    override fun speak(
        text: String,
        onWordBoundary: (Int, Int) -> Unit,
        onStart: () -> Unit,
        onComplete: () -> Unit
    ) {
        val synth = synthesizer ?: return

        // Stop any current speech
        if (synth.isSpeaking()) {
            synth.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }

        currentlyPaused = false

        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
        utterance.rate = 0.5f
        utterance.pitchMultiplier = 1.0f
        utterance.volume = 1.0f

        onStart()
        synth.speakUtterance(utterance)

        // Poll for completion. Add an initial delay so the synthesizer has time to
        // transition isSpeaking() to true before we start checking — avoids a race
        // condition where the poll exits immediately and fires onComplete() prematurely.
        scope.launch {
            delay(300)
            while (synth.isSpeaking()) {
                delay(200)
            }
            if (!currentlyPaused) {
                onComplete()
            }
        }
    }

    override fun stop() {
        synthesizer?.let {
            if (it.isSpeaking()) {
                it.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            }
        }
        currentlyPaused = false
    }

    override fun pause() {
        synthesizer?.let {
            if (it.isSpeaking()) {
                it.pauseSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
                currentlyPaused = true
            }
        }
    }

    override fun resume() {
        if (currentlyPaused) {
            synthesizer?.continueSpeaking()
            currentlyPaused = false
        }
    }

    override fun isPlaying(): Boolean = synthesizer?.isSpeaking() == true && !currentlyPaused

    override fun isPaused(): Boolean = currentlyPaused

    override fun release() {
        stop()
        synthesizer = null
    }
}

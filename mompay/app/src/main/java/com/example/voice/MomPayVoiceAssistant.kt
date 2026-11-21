package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.MomPayInvitation
import com.example.logic.ExpenseCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

data class VoiceAssistantState(
    val isTtsReady: Boolean = false,
    val isInitializing: Boolean = false,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val ttsErrorMessage: String? = null,
    val activeScript: String = ""
)

class MomPayVoiceAssistant(context: Context) {

    private val appContext = context.applicationContext
    private var textToSpeech: TextToSpeech? = null
    private var ringtone: Ringtone? = null
    private var pendingScriptAfterInit: String? = null

    private val _state = MutableStateFlow(VoiceAssistantState())
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    fun buildScript(invitation: MomPayInvitation): String {
        val spokenAmount = ExpenseCalculator.amountToSpokenRupees(invitation.amountPaise)
        val cleanedExpense = invitation.expenseDescription.trim().ifEmpty { "recent group expenses" }
        val expensePhrase = if (cleanedExpense.lowercase().contains("expense")) {
            cleanedExpense
        } else {
            "$cleanedExpense expenses"
        }

        return buildString {
            appendLine("Namaste Aunty!")
            appendLine()
            appendLine("This is MomPay, your family financial reminder assistant.")
            appendLine()
            appendLine(
                "Your son ${invitation.debtorName} has an outstanding payment of " +
                    "$spokenAmount from his recent $expensePhrase."
            )
            appendLine()
            appendLine("His friend ${invitation.senderName} has been waiting for the payment.")
            appendLine()
            appendLine("We kindly request you to remind him to settle his dues.")
            appendLine()
            appendLine("Thank you, Aunty!")
            appendLine()
            append("Have a wonderful day!")
        }
    }

    fun startIncomingAlert() {
        stopIncomingAlert()
        startRingtone()
        startSubtleVibration()
    }

    fun stopIncomingAlert() {
        try {
            ringtone?.let {
                if (it.isPlaying) it.stop()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop incoming ringtone cleanly", e)
        } finally {
            ringtone = null
        }

        try {
            getVibrator()?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel vibration cleanly", e)
        }
    }

    fun prepareAndSpeak(invitation: MomPayInvitation) {
        stopIncomingAlert()
        val script = buildScript(invitation)
        _state.update {
            it.copy(
                activeScript = script,
                isMuted = false,
                ttsErrorMessage = null
            )
        }

        val currentTts = textToSpeech
        if (currentTts != null && _state.value.isTtsReady) {
            speakInternal(script)
            return
        }

        pendingScriptAfterInit = script
        if (_state.value.isInitializing) return

        _state.update { it.copy(isInitializing = true, ttsErrorMessage = null) }
        try {
            textToSpeech = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val tts = textToSpeech
                    if (tts == null) {
                        handleTtsFailure("Text-to-Speech engine could not be initialized.")
                        return@TextToSpeech
                    }
                    configureTtsEngine(tts)
                    _state.update {
                        it.copy(
                            isTtsReady = true,
                            isInitializing = false,
                            ttsErrorMessage = null
                        )
                    }
                    val queued = pendingScriptAfterInit
                    pendingScriptAfterInit = null
                    if (queued != null && !_state.value.isMuted) {
                        speakInternal(queued)
                    }
                } else {
                    handleTtsFailure("Text-to-Speech initialization failed on this device (code $status).")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TextToSpeech construction failed", e)
            handleTtsFailure("Text-to-Speech is unavailable on this device.")
        }
    }

    fun replay() {
        val current = _state.value
        if (current.isMuted) {
            _state.update {
                it.copy(ttsErrorMessage = "Audio is muted. Tap Unmute first to replay.")
            }
            return
        }
        val script = current.activeScript
        if (script.isBlank()) return

        if (current.isTtsReady && textToSpeech != null) {
            _state.update { it.copy(ttsErrorMessage = null) }
            speakInternal(script)
        } else {
            pendingScriptAfterInit = script
            if (!current.isInitializing) {
                handleTtsFailure("Text-to-Speech is not ready yet.")
            }
        }
    }

    fun toggleMute() {
        val nowMuted = !_state.value.isMuted
        if (nowMuted) {
            pendingScriptAfterInit = null
            try {
                textToSpeech?.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop TTS on mute", e)
            }
            _state.update {
                it.copy(
                    isMuted = true,
                    isSpeaking = false,
                    ttsErrorMessage = null
                )
            }
        } else {
            _state.update {
                it.copy(
                    isMuted = false,
                    ttsErrorMessage = null
                )
            }
        }
    }

    fun stopAllPlayback() {
        stopIncomingAlert()
        pendingScriptAfterInit = null
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop TTS playback", e)
        }
        _state.update { it.copy(isSpeaking = false) }
    }

    fun shutdown() {
        stopAllPlayback()
        try {
            textToSpeech?.setOnUtteranceProgressListener(null)
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to shutdown TextToSpeech cleanly", e)
        } finally {
            textToSpeech = null
        }
        _state.value = VoiceAssistantState()
    }

    private fun configureTtsEngine(tts: TextToSpeech) {
        val indianEnglish = Locale("en", "IN")
        val langResult = tts.setLanguage(indianEnglish)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            val fallback = Locale.getDefault()
            val fallbackResult = tts.setLanguage(fallback)
            if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Neither en-IN nor default locale is fully supported by TTS engine")
            }
        }
        tts.setSpeechRate(0.94f)
        tts.setPitch(1.03f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _state.update { it.copy(isSpeaking = true) }
            }

            override fun onDone(utteranceId: String?) {
                _state.update { it.copy(isSpeaking = false) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.update {
                    it.copy(
                        isSpeaking = false,
                        ttsErrorMessage = "Audio playback encountered an interruption."
                    )
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _state.update {
                    it.copy(
                        isSpeaking = false,
                        ttsErrorMessage = "Audio playback error ($errorCode). You can still read the script below."
                    )
                }
            }
        })
    }

    private fun speakInternal(script: String) {
        if (_state.value.isMuted) return
        val tts = textToSpeech ?: return
        val result = try {
            tts.speak(
                script,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "mompay_utterance_${System.currentTimeMillis()}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "TextToSpeech.speak threw exception", e)
            TextToSpeech.ERROR
        }

        if (result == TextToSpeech.ERROR) {
            _state.update {
                it.copy(
                    isSpeaking = false,
                    ttsErrorMessage = "Could not start voice announcement. Read the script below."
                )
            }
        }
    }

    private fun handleTtsFailure(message: String) {
        pendingScriptAfterInit = null
        _state.update {
            it.copy(
                isTtsReady = false,
                isInitializing = false,
                isSpeaking = false,
                ttsErrorMessage = message
            )
        }
    }

    private fun startRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            val r = RingtoneManager.getRingtone(appContext, uri) ?: return
            r.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                r.isLooping = true
            }
            r.play()
            ringtone = r
        } catch (e: Exception) {
            Log.w(TAG, "Unable to play incoming ringtone", e)
        }
    }

    private fun startSubtleVibration() {
        try {
            val vibrator = getVibrator() ?: return
            if (!vibrator.hasVibrator()) return
            val pattern = longArrayOf(0L, 180L, 950L, 180L, 1400L)
            val effect = VibrationEffect.createWaveform(pattern, 0)
            vibrator.vibrate(effect)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to trigger incoming vibration", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    companion object {
        private const val TAG = "MomPayVoiceAssistant"
    }
}

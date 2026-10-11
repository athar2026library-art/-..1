package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Arabic speech fallback for environments where a recorded reciter track is not configured.
 * Keep the user's diacritics: they can improve Arabic pronunciation in TTS engines.
 */
class AudioPlayer(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    @Volatile private var isInitialized = false
    private var speechRate = 0.85f
    private val stateLock = Any()
    private var pendingLoopText: String? = null
    private var loopRemaining = 0
    private var loopPendingStart = false

    private val initialization = CompletableDeferred<Boolean>()
    private val continuations = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        val speechEngine = tts
        if (speechEngine == null || status != TextToSpeech.SUCCESS) {
            Log.e("AudioPlayer", "TTS initialization failed with status=$status")
            initialization.complete(false)
            return
        }

        val preferredLanguage = speechEngine.setLanguage(Locale("ar", "SA"))
        val languageResult =
            if (
                preferredLanguage == TextToSpeech.LANG_MISSING_DATA ||
                preferredLanguage == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                speechEngine.setLanguage(Locale.forLanguageTag("ar"))
            } else {
                preferredLanguage
            }

        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            Log.w("AudioPlayer", "Arabic TTS language is not available on this device")
            initialization.complete(false)
            return
        }

        selectArabicVoice(speechEngine)
        speechEngine.setSpeechRate(speechRate)
        speechEngine.setPitch(1.0f)
        speechEngine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    handleUtteranceDone(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handleUtteranceError(utteranceId)
                }
            }
        )

        val queuedLoopText = synchronized(stateLock) {
            isInitialized = true
            if (loopPendingStart && loopRemaining > 0) {
                loopPendingStart = false
                pendingLoopText
            } else {
                null
            }
        }
        initialization.complete(true)
        queuedLoopText?.let(::speakLoopIteration)
        Log.d("AudioPlayer", "Arabic TTS ready")
    }

    private fun selectArabicVoice(speechEngine: TextToSpeech) {
        val voices = speechEngine.voices.orEmpty()
            .filter { it.locale.language.equals("ar", ignoreCase = true) }
            .sortedWith(
                compareBy<Voice> { !it.locale.language.equals("ar", ignoreCase = true) }
                    .thenBy { if (it.locale.toLanguageTag().equals("ar-SA", ignoreCase = true)) 0 else 1 }
                    .thenBy { it.isNetworkConnectionRequired }
            )

        voices.firstOrNull()?.let { voice ->
            runCatching {
                speechEngine.setVoice(voice)
                Log.d("AudioPlayer", "Selected Arabic voice: ${voice.name} (${voice.locale})")
            }.onFailure { error ->
                Log.w("AudioPlayer", "Unable to select Arabic voice", error)
            }
        }
    }

    private fun cleanForSpeech(text: String): String =
        text
            .replace("ﷺ", "صلى الله عليه وسلم")
            .replace("۝", ". ")
            .replace(Regex("[\\u06D6-\\u06ED]"), "")
            .replace("ـ", "")
            .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    suspend fun playAndWait(text: String): Boolean {
        // A new speech request replaces a loop or another utterance.
        stop()
        val ready = isInitialized || (withTimeoutOrNull(3_000L) { initialization.await() } == true)
        if (!ready) return false

        val cleaned = cleanForSpeech(text)
        if (cleaned.isBlank()) return false

        return suspendCancellableCoroutine { continuation ->
            synchronized(stateLock) {
                pendingLoopText = null
                loopRemaining = 0
            }

            val utteranceId = UUID.randomUUID().toString()
            continuations[utteranceId] = continuation

            val result = tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                ?: TextToSpeech.ERROR
            if (result != TextToSpeech.SUCCESS) {
                continuations.remove(utteranceId)
                if (continuation.isActive) continuation.resume(false) { _, _, _ -> }
                _isPlaying.value = false
            }

            continuation.invokeOnCancellation {
                tts?.stop()
                continuations.remove(utteranceId)
                synchronized(stateLock) {
                    pendingLoopText = null
                    loopRemaining = 0
                }
                _isPlaying.value = false
            }
        }
    }

    /** Repeats the supplied text exactly [times] times, including the current play. */
    fun playLoop(text: String, times: Int) {
        if (times < 1) return
        val cleaned = cleanForSpeech(text)
        if (cleaned.isBlank()) return

        stop()
        val startNow = synchronized(stateLock) {
            pendingLoopText = cleaned
            loopRemaining = times.coerceAtMost(100)
            loopPendingStart = !isInitialized
            isInitialized
        }
        // If TTS is still initializing, onInit consumes this one pending request.
        if (startNow) speakLoopIteration(cleaned)
    }

    private fun speakLoopIteration(text: String) {
        val utteranceId = UUID.randomUUID().toString()
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            ?: TextToSpeech.ERROR
        if (result != TextToSpeech.SUCCESS) {
            synchronized(stateLock) {
                pendingLoopText = null
                loopRemaining = 0
            }
            _isPlaying.value = false
        } else {
            _isPlaying.value = true
        }
    }

    private fun handleUtteranceDone(utteranceId: String?) {
        val repeatText = synchronized(stateLock) {
            val loopText = pendingLoopText
            if (loopText != null && loopRemaining > 1) {
                loopRemaining -= 1
                loopText
            } else {
                pendingLoopText = null
                loopRemaining = 0
                null
            }
        }

        if (repeatText != null) {
            speakLoopIteration(repeatText)
            return
        }

        _isPlaying.value = false
        utteranceId?.let { id ->
            continuations.remove(id)?.let { continuation ->
                if (continuation.isActive) continuation.resume(true) { _, _, _ -> }
            }
        }
    }

    private fun handleUtteranceError(utteranceId: String?) {
        synchronized(stateLock) {
            pendingLoopText = null
            loopRemaining = 0
            loopPendingStart = false
        }
        _isPlaying.value = false
        utteranceId?.let { id ->
            continuations.remove(id)?.let { continuation ->
                if (continuation.isActive) continuation.resume(false) { _, _, _ -> }
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 1.25f)
        if (isInitialized) tts?.setSpeechRate(speechRate)
    }

    fun stop() {
        tts?.stop()
        synchronized(stateLock) {
            pendingLoopText = null
            loopRemaining = 0
        }
        _isPlaying.value = false
        continuations.keys.toList().forEach { id ->
            continuations.remove(id)?.let { continuation ->
                if (continuation.isActive) continuation.resume(false) { _, _, _ -> }
            }
        }
    }

    fun shutdown() {
        stop()
        isInitialized = false
        initialization.complete(false)
        tts?.shutdown()
        tts = null
    }
}

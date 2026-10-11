package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class AudioPlayer(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val continuations = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.forLanguageTag("ar"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        _isPlaying.value = true
                    }
                    override fun onDone(utteranceId: String) {
                        _isPlaying.value = false
                        continuations.remove(utteranceId)?.resume(true) { _, _, _ -> }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String) {
                        _isPlaying.value = false
                        continuations.remove(utteranceId)?.resume(false) { _, _, _ -> }
                    }
                }
            }
        )

        isInitialized = true
        Log.d("AudioPlayer", "TTS ready (ar)")
    }

    private var pendingLoopText: String? = null

    private fun selectArabicVoice(speechEngine: TextToSpeech) {
        val voices =
            speechEngine.voices.orEmpty()
                .filter {
                    it.locale.language.equals("ar", ignoreCase = true)
                }
                .sortedWith(
                    compareBy<Voice> {
                        !it.locale.language.equals("ar", ignoreCase = true)
                    }.thenBy {
                        if (it.locale == Locale("ar", "SA")) 0 else 1
                    }.thenBy {
                        it.isNetworkConnectionRequired
                    }
                )

        voices.firstOrNull()?.let { voice ->
            runCatching {
                speechEngine.setVoice(voice)
                Log.d("AudioPlayer", "Selected voice: ${voice.name} (${voice.locale})")
            }
        }
    }

    suspend fun playAndWait(text: String): Boolean = suspendCancellableCoroutine { cont ->
        if (!isInitialized) {
            cont.resume(false) { _, _, _ -> }
            return@suspendCancellableCoroutine
        }

        val id = UUID.randomUUID().toString()
        continuations[id] = cont

        val status = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (status != TextToSpeech.SUCCESS) {
            continuations.remove(id)
            cont.resume(false) { _, _, _ -> }
        }

        cont.invokeOnCancellation {
            tts?.stop()
            continuations.remove(id)
            _isPlaying.value = false
        }
    }

    /** Repeat the same zekr several times (QUEUE_ADD). */
    fun playLoop(text: String, times: Int) {
        if (!isInitialized || times < 1) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        repeat(times - 1) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
        }
        _isPlaying.value = true
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            _isPlaying.value = false
            continuations.values.forEach { if (it.isActive) it.resume(false) { _, _, _ -> } }
            continuations.clear()
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

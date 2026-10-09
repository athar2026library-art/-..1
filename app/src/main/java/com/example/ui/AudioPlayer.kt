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

    private val continuations =
        ConcurrentHashMap<String, CancellableContinuation<Boolean>>()

    private val loopRemaining = AtomicInteger(0)

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e("AudioPlayer", "TTS init failed with status=$status")
            return
        }

        val speechEngine = tts ?: return
        val preferredLocale = Locale("ar", "SA")

        var languageResult = speechEngine.setLanguage(preferredLocale)

        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            languageResult = speechEngine.setLanguage(Locale.forLanguageTag("ar"))
        }

        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            Log.w("AudioPlayer", "Arabic TTS language not available")
            return
        }

        speechEngine.setSpeechRate(0.64f)
        speechEngine.setPitch(1.0f)
        selectArabicVoice(speechEngine)

        speechEngine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    val remaining = loopRemaining.decrementAndGet()
                    if (remaining > 0 && utteranceId != null) {
                        val text = pendingLoopText
                        if (!text.isNullOrBlank()) {
                            val nextId = UUID.randomUUID().toString()
                            speechEngine.speak(
                                text,
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                nextId
                            )
                            return
                        }
                    }
                    _isPlaying.value = false
                    utteranceId?.let { id ->
                        continuations.remove(id)?.resume(true, null)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                    loopRemaining.set(0)
                    utteranceId?.let { id ->
                        continuations.remove(id)?.resume(false, null)
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

    private fun cleanForSpeech(text: String): String {
        return text
            .replace("ﷺ", "صلى الله عليه وسلم")
            .replace("۝", ". ")
            .replace(Regex("[\\u06D6-\\u06ED]"), "")
            .replace("ـ", "")
            .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E]"), "")
            .replace("…", "... ")
            .replace(Regex("[،؛]"), "$0 ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    suspend fun playAndWait(text: String): Boolean =
        suspendCancellableCoroutine { cont ->
            if (!isInitialized) {
                cont.resume(false, null)
                return@suspendCancellableCoroutine
            }

            val cleaned = cleanForSpeech(text)
            if (cleaned.isBlank()) {
                cont.resume(false, null)
                return@suspendCancellableCoroutine
            }

            loopRemaining.set(0)
            pendingLoopText = null

            val id = UUID.randomUUID().toString()
            continuations[id] = cont

            val status = tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, id)
            if (status != TextToSpeech.SUCCESS) {
                continuations.remove(id)
                cont.resume(false, null)
            }

            cont.invokeOnCancellation {
                tts?.stop()
                continuations.remove(id)
                _isPlaying.value = false
            }
        }

    fun playLoop(text: String, times: Int) {
        if (!isInitialized || times <= 0) return

        val cleaned = cleanForSpeech(text)
        if (cleaned.isBlank()) return

        stop()
        pendingLoopText = cleaned
        loopRemaining.set(times)

        val id = UUID.randomUUID().toString()
        val status = tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, id)
        if (status == TextToSpeech.SUCCESS) {
            _isPlaying.value = true
        } else {
            loopRemaining.set(0)
            pendingLoopText = null
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            _isPlaying.value = false
            loopRemaining.set(0)
            pendingLoopText = null
            continuations.values.forEach { if (it.isActive) it.resume(false, null) }
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

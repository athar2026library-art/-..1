package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ar"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        _isPlaying.value = true
                    }
                    override fun onDone(utteranceId: String) {
                        _isPlaying.value = false
                        continuations.remove(utteranceId)?.resume(true, null)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String) {
                        _isPlaying.value = false
                        continuations.remove(utteranceId)?.resume(false, null)
                    }
                })
            }
        }
    }

    suspend fun playAndWait(text: String): Boolean = suspendCancellableCoroutine { cont ->
        if (!isInitialized) {
            cont.resume(false, null)
            return@suspendCancellableCoroutine
        }
        
        val id = UUID.randomUUID().toString()
        continuations[id] = cont
        
        val status = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
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

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            _isPlaying.value = false
            continuations.values.forEach { if (it.isActive) it.resume(false, null) }
            continuations.clear()
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
    }
}

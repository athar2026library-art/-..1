package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Audio playback for Baqiyat:
 * - prefers a supplied HTTPS recording when one exists,
 * - caches recordings inside app-private storage for offline playback,
 * - keeps Arabic Text-to-Speech as the offline fallback.
 *
 * Recording URLs are content metadata, not bundled API credentials.
 */
class AudioPlayer(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var speechRate = DEFAULT_TTS_RATE

    private var mediaPlayer: MediaPlayer? = null
    private var mediaGeneration = 0
    private var recordingRepeatsRemaining = 0
    private var recordingCompletion: (() -> Unit)? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val continuations = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()
    private val ttsRemaining = AtomicInteger(0)
    private var ttsSequenceCompletion: (() -> Unit)? = null

    private val audioCacheDirectory = File(appContext.filesDir, "reciter-audio")

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TTS initialization failed with status=$status")
            return
        }

        val engine = tts ?: return
        val result = engine.setLanguage(Locale.forLanguageTag("ar"))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Arabic TTS is unavailable on this device")
            return
        }

        engine.setSpeechRate(speechRate)
        engine.setPitch(1.0f)
        selectArabicVoice(engine)
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                }

                override fun onDone(utteranceId: String?) {
                    utteranceId?.let { id ->
                        continuations.remove(id)?.let { continuation ->
                            if (continuation.isActive) continuation.resume(true) { _, _, _ -> }
                        }
                    }
                    finishTtsUtterance()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    utteranceId?.let { id ->
                        continuations.remove(id)?.let { continuation ->
                            if (continuation.isActive) continuation.resume(false) { _, _, _ -> }
                        }
                    }
                    finishTtsUtterance()
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    utteranceId?.let { id ->
                        continuations.remove(id)?.let { continuation ->
                            if (continuation.isActive) continuation.resume(false) { _, _, _ -> }
                        }
                    }
                    // stop() owns queue-state resets. A delayed callback from an old
                    // utterance must not cancel a newer playback sequence.
                }
            }
        )
        isInitialized = true
        Log.d(TAG, "Arabic TTS fallback is ready")
    }

    private fun selectArabicVoice(engine: TextToSpeech) {
        val selected = engine.voices.orEmpty()
            .filter { it.locale.language.equals("ar", ignoreCase = true) }
            .sortedWith(
                compareBy<Voice> { it.isNetworkConnectionRequired }
                    .thenBy { if (it.locale == Locale("ar", "SA")) 0 else 1 }
            )
            .firstOrNull()

        selected?.let { voice ->
            runCatching { engine.voice = voice }
                .onFailure { Log.w(TAG, "Unable to select Arabic TTS voice", it) }
        }
    }

    /** Plays a verified HTTPS recording; downloaded copies are preferred when present. */
    fun playRecording(
        url: String,
        speed: Float = 1.0f,
        repetitions: Int = 1,
        onCompletion: (() -> Unit)? = null
    ) {
        if (!isHttpsUrl(url)) {
            Log.w(TAG, "Refusing a non-HTTPS recording URL")
            return
        }

        stop()
        val generation = mediaGeneration
        recordingRepeatsRemaining = repetitions.coerceIn(1, MAX_REPETITIONS)
        recordingCompletion = onCompletion

        val cachedFile = cacheFileFor(url).takeIf { it.isFile && it.length() > 0L }
        val source = cachedFile?.absolutePath ?: url.trim()
        val player = MediaPlayer()
        mediaPlayer = player

        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setDataSource(source)
            player.setOnPreparedListener { prepared ->
                if (generation != mediaGeneration || mediaPlayer !== prepared) {
                    runCatching { prepared.release() }
                    return@setOnPreparedListener
                }
                runCatching {
                    prepared.setPlaybackParams(
                        prepared.playbackParams.setSpeed(speed.coerceIn(MIN_RECORDING_RATE, MAX_RECORDING_RATE))
                    )
                }.onFailure { Log.w(TAG, "Recording speed control is unavailable", it) }
                _isPlaying.value = true
                prepared.start()
            }
            player.setOnCompletionListener { completed ->
                if (generation != mediaGeneration || mediaPlayer !== completed) return@setOnCompletionListener
                if (recordingRepeatsRemaining > 1) {
                    recordingRepeatsRemaining--
                    runCatching {
                        completed.seekTo(0)
                        completed.start()
                    }.onFailure { finishRecording(generation, completed) }
                } else {
                    finishRecording(generation, completed)
                }
            }
            player.setOnErrorListener { failed, what, extra ->
                Log.w(TAG, "Recording playback failed (what=$what, extra=$extra)")
                finishRecording(generation, failed)
                true
            }
            player.prepareAsync()
        } catch (error: Exception) {
            Log.e(TAG, "Unable to start recording playback", error)
            finishRecording(generation, player)
        }
    }

    private fun finishRecording(generation: Int, player: MediaPlayer) {
        if (generation != mediaGeneration) {
            runCatching { player.release() }
            return
        }
        _isPlaying.value = false
        recordingRepeatsRemaining = 0
        if (mediaPlayer === player) mediaPlayer = null
        runCatching { player.setOnCompletionListener(null) }
        runCatching { player.setOnErrorListener(null) }
        runCatching { player.release() }
        val callback = recordingCompletion
        recordingCompletion = null
        callback?.invoke()
    }

    /**
     * Downloads an HTTPS recording into app-private storage. A completed file is moved
     * into place atomically; partial downloads are never treated as an offline copy.
     */
    suspend fun downloadRecording(url: String): Boolean = withContext(Dispatchers.IO) {
        if (!isHttpsUrl(url)) return@withContext false

        val destination = cacheFileFor(url)
        if (destination.isFile && destination.length() > 0L) return@withContext true
        if (!audioCacheDirectory.exists() && !audioCacheDirectory.mkdirs()) return@withContext false

        val temporary = File(audioCacheDirectory, destination.name + ".part")
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url.trim()).openConnection() as? HttpURLConnection)
                ?: return@withContext false
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Baqiyat/1.0")
            connection.connect()

            if (connection.responseCode !in 200..299 ||
                !connection.url.protocol.equals("https", ignoreCase = true)
            ) return@withContext false

            if (connection.contentLengthLong > MAX_RECORDING_BYTES) return@withContext false

            var downloaded = 0L
            connection.inputStream.buffered().use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloaded += count
                        if (downloaded > MAX_RECORDING_BYTES) {
                            throw IOException("Recording exceeds the configured download limit")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (downloaded == 0L) return@withContext false

            if (destination.exists()) destination.delete()
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            destination.isFile && destination.length() > 0L
        } catch (error: Exception) {
            Log.w(TAG, "Recording download failed", error)
            false
        } finally {
            connection?.disconnect()
            if (temporary.exists()) temporary.delete()
        }
    }

    fun hasCachedRecording(url: String): Boolean {
        val file = cacheFileFor(url)
        return isHttpsUrl(url) && file.isFile && file.length() > 0L
    }

    private fun cacheFileFor(url: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.trim().toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
        return File(audioCacheDirectory, "$digest.mp3")
    }

    private fun isHttpsUrl(url: String): Boolean =
        url.trim().startsWith("https://", ignoreCase = true)

    suspend fun playAndWait(text: String, rate: Float = speechRate): Boolean =
        suspendCancellableCoroutine { continuation ->
            stop()
            if (!isInitialized) {
                continuation.resume(false) { _, _, _ -> }
                return@suspendCancellableCoroutine
            }

            val clean = cleanForSpeech(text)
            if (clean.isBlank()) {
                continuation.resume(false) { _, _, _ -> }
                return@suspendCancellableCoroutine
            }

            setSpeechRate(rate)
            val id = UUID.randomUUID().toString()
            continuations[id] = continuation
            ttsRemaining.set(1)

            val result = tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, id)
            if (result != TextToSpeech.SUCCESS) {
                continuations.remove(id)
                ttsRemaining.set(0)
                continuation.resume(false) { _, _, _ -> }
                _isPlaying.value = false
            }
            continuation.invokeOnCancellation {
                tts?.stop()
                continuations.remove(id)
                ttsRemaining.set(0)
                _isPlaying.value = false
            }
        }

    fun playLoop(
        text: String,
        times: Int,
        rate: Float = speechRate,
        onCompletion: (() -> Unit)? = null
    ) {
        if (!isInitialized || times < 1) return
        val clean = cleanForSpeech(text)
        if (clean.isBlank()) return

        stop()
        setSpeechRate(rate)
        val count = times.coerceIn(1, MAX_REPETITIONS)
        ttsRemaining.set(count)
        ttsSequenceCompletion = onCompletion

        repeat(count) { index ->
            val id = UUID.randomUUID().toString()
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val result = tts?.speak(clean, queueMode, null, id)
            if (result != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TTS rejected an utterance")
                ttsRemaining.set(0)
                ttsSequenceCompletion = null
                _isPlaying.value = false
            }
        }
    }

    private fun finishTtsUtterance() {
        val remaining = ttsRemaining.updateAndGet { current -> if (current > 0) current - 1 else 0 }
        if (remaining == 0) {
            _isPlaying.value = false
            val callback = ttsSequenceCompletion
            ttsSequenceCompletion = null
            callback?.invoke()
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(MIN_TTS_RATE, MAX_TTS_RATE)
        if (isInitialized) tts?.setSpeechRate(speechRate)
    }

    fun stop() {
        mediaGeneration++
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            runCatching { player.setOnCompletionListener(null) }
            runCatching { player.setOnErrorListener(null) }
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        recordingRepeatsRemaining = 0
        recordingCompletion = null

        if (isInitialized) runCatching { tts?.stop() }
        continuations.values.forEach { continuation ->
            if (continuation.isActive) continuation.resume(false) { _, _, _ -> }
        }
        continuations.clear()
        ttsRemaining.set(0)
        ttsSequenceCompletion = null
        _isPlaying.value = false
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
        isInitialized = false
    }

    private fun cleanForSpeech(text: String): String =
        text
            .replace("ﷺ", "صلى الله عليه وسلم")
            .replace("۝", ". ")
            .replace(Regex("[\\u06D6-\\u06ED]"), "")
            .replace("ـ", "")
            .replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E]"), "")
            .replace("…", "... ")
            .replace(Regex("[،؛]"), "$0 ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private companion object {
        const val TAG = "AudioPlayer"
        const val DEFAULT_TTS_RATE = 0.75f
        const val MIN_TTS_RATE = 0.5f
        const val MAX_TTS_RATE = 1.5f
        const val MIN_RECORDING_RATE = 0.75f
        const val MAX_RECORDING_RATE = 1.25f
        const val MAX_REPETITIONS = 100
        const val MAX_RECORDING_BYTES = 50L * 1024L * 1024L
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
    }
}

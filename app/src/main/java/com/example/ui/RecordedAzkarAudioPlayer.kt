package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Recorded full-length morning/evening adhkar audio from IslamHouse.
 * Downloads are stored privately in app filesDir and can be played without a connection.
 */
class RecordedAzkarAudioPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var playbackSpeed = 1.0f

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _downloadedCategories = MutableStateFlow(existingDownloads())
    val downloadedCategories: StateFlow<Set<String>> = _downloadedCategories.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun isSupported(category: String): Boolean = category in AUDIO_URLS

    fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = when {
            speed < 0.875f -> 0.75f
            speed < 1.125f -> 1.0f
            else -> 1.25f
        }
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) {
                    player.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                }
            }.onFailure { Log.w(TAG, "Could not update recorded audio speed", it) }
        }
    }

    fun play(category: String): Boolean {
        val url = AUDIO_URLS[category] ?: return false
        stop()
        _error.value = null

        val localFile = fileFor(category)
        val player = MediaPlayer()
        mediaPlayer = player

        return try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            player.setOnPreparedListener { prepared ->
                if (mediaPlayer !== prepared) {
                    runCatching { prepared.release() }
                    return@setOnPreparedListener
                }
                runCatching {
                    prepared.playbackParams = PlaybackParams().setSpeed(playbackSpeed)
                }.onFailure { Log.w(TAG, "Playback speed could not be applied", it) }
                prepared.start()
                _isPlaying.value = true
            }
            player.setOnCompletionListener { completed ->
                _isPlaying.value = false
                if (mediaPlayer === completed) mediaPlayer = null
                runCatching { completed.release() }
            }
            player.setOnErrorListener { failed, what, extra ->
                Log.e(TAG, "Recorded audio failed: what=$what extra=$extra")
                _error.value = "تعذر تشغيل التسجيل الصوتي. تحقق من الاتصال أو أعد تنزيل الملف."
                _isPlaying.value = false
                if (mediaPlayer === failed) mediaPlayer = null
                runCatching { failed.release() }
                true
            }

            if (localFile.isFile && localFile.length() > MIN_VALID_BYTES) {
                player.setDataSource(localFile.absolutePath)
            } else {
                player.setDataSource(url)
            }
            player.prepareAsync()
            true
        } catch (error: Exception) {
            Log.e(TAG, "Unable to prepare recorded audio", error)
            _error.value = "تعذر بدء التسجيل. جرّب مرة أخرى."
            if (mediaPlayer === player) mediaPlayer = null
            runCatching { player.release() }
            _isPlaying.value = false
            false
        }
    }

    fun stop() {
        val oldPlayer = mediaPlayer
        mediaPlayer = null
        _isPlaying.value = false
        if (oldPlayer != null) {
            runCatching { oldPlayer.stop() }
            runCatching { oldPlayer.reset() }
            runCatching { oldPlayer.release() }
        }
    }

    suspend fun download(category: String): Boolean = withContext(Dispatchers.IO) {
        val url = AUDIO_URLS[category] ?: return@withContext false
        if (_isDownloading.value) return@withContext false

        _isDownloading.value = true
        _downloadProgress.value = 0
        _error.value = null

        val destination = fileFor(category)
        val temporary = File(destination.parentFile, destination.name + ".download")
        var connection: HttpURLConnection? = null

        try {
            temporary.delete()
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "BaqiyatAndroid/1.0")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Audio host returned HTTP $responseCode")
            }

            val expectedLength = connection.contentLengthLong
            if (expectedLength > MAX_DOWNLOAD_BYTES) {
                throw IOException("Audio download exceeds the configured size limit")
            }

            var received = 0L
            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        if (received > MAX_DOWNLOAD_BYTES) {
                            throw IOException("Audio download exceeds the configured size limit")
                        }
                        output.write(buffer, 0, count)
                        if (expectedLength > 0L) {
                            _downloadProgress.value =
                                ((received * 100L) / expectedLength).toInt().coerceIn(0, 99)
                        }
                    }
                }
            }

            if (received < MIN_VALID_BYTES) throw IOException("Audio file is empty or incomplete")
            if (destination.exists() && !destination.delete()) {
                throw IOException("Unable to replace the previous audio file")
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            _downloadProgress.value = 100
            _downloadedCategories.value = existingDownloads()
            true
        } catch (error: Exception) {
            temporary.delete()
            Log.e(TAG, "Recorded audio download failed for category=$category", error)
            _error.value = "تعذر تنزيل الصوت. تحقق من الإنترنت ثم أعد المحاولة."
            throw error
        } finally {
            connection?.disconnect()
            _isDownloading.value = false
        }
    }

    fun release() {
        stop()
    }

    private fun fileFor(category: String): File =
        File(appContext.filesDir, "recorded_azkar_${category}_mishary_1434.mp3")

    private fun existingDownloads(): Set<String> = AUDIO_URLS.keys.filterTo(mutableSetOf()) { category ->
        fileFor(category).let { it.isFile && it.length() > MIN_VALID_BYTES }
    }

    companion object {
        private const val TAG = "RecordedAzkarAudio"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 25_000
        private const val MAX_DOWNLOAD_BYTES = 45L * 1024 * 1024
        private const val MIN_VALID_BYTES = 10_000L

        const val RECITER_NAME = "مشاري بن راشد العفاسي"
        const val SOURCE_NAME = "IslamHouse"

        private val AUDIO_URLS = mapOf(
            "sabah" to "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Mishari_Raashid/Azkar_AlSba7_w_AlMsa/ar_1434_Azkar_AlSba7.mp3",
            "masaa" to "https://d1.islamhouse.com/data/ar/ih_sounds/chain_01/Mishari_Raashid/Azkar_AlSba7_w_AlMsa/ar_1434_Azkar_AlMsa.mp3"
        )

        fun sourcePage(category: String): String = when (category) {
            "sabah" -> "https://islamhouse.com/ar/audios/92368/"
            "masaa" -> "https://islamhouse.com/ar/audios/327603/"
            else -> "https://islamhouse.com/"
        }
    }
}

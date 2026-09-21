package com.example

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.AudioPlayer
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioPlayerInstrumentedTest {
    @Test
    fun audioPlayer_initializesAndShutsDownSafely() {
        val player = AudioPlayer(ApplicationProvider.getApplicationContext())
        try {
            assertFalse(player.isPlaying.value)
        } finally {
            player.shutdown()
        }
    }
}

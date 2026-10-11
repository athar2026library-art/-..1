package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** أنماط اهتزاز موحدة: نقرة خفيفة، علامة كل 33، وإتمام مزدوج. */
object Haptics {
    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun tick(context: Context) = oneShot(context, 12)

    fun mark(context: Context) = oneShot(context, 45)

    fun complete(context: Context) = pattern(context, longArrayOf(0, 60, 70, 120))

    private fun oneShot(context: Context, ms: Long) {
        val v = vibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") v.vibrate(ms)
        }
    }

    private fun pattern(context: Context, timings: LongArray) {
        val v = vibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION") v.vibrate(timings, -1)
        }
    }
}

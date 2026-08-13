package com.duck.twominute

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Tiny wrapper around the two bits of physical feedback the app cares about:
 * a short double buzz and the system notification chime.
 */
class Feedback(context: Context) {

    private val appContext = context.applicationContext

    fun celebrate(vibrate: Boolean, chime: Boolean) {
        if (vibrate) buzz(longArrayOf(0L, 45L, 90L, 70L))
        if (chime) ring()
    }

    fun tap(vibrate: Boolean) {
        if (vibrate) buzz(longArrayOf(0L, 18L))
    }

    private fun vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Vibrator::class.java)
    }

    private fun buzz(pattern: LongArray) {
        val device = vibrator() ?: return
        runCatching { device.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }

    private fun ring() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return@runCatching
            RingtoneManager.getRingtone(appContext, uri)?.play()
        }
    }
}

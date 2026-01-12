package org.cf0x.rustnithm.Data

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View

class Haptic private constructor() {
    private var vibrator: Vibrator? = null

    private val zoneEffect = VibrationEffect.createOneShot(12, 200)
    private val moveEffect = VibrationEffect.createOneShot(5, 100)

    private val hapticAttributes = VibrationAttributes.Builder()
        .setUsage(VibrationAttributes.USAGE_TOUCH)
        .build()

    companion object {
        @Volatile
        private var instance: Haptic? = null
        fun getInstance(): Haptic = instance ?: synchronized(this) {
            instance ?: Haptic().also { instance = it }
        }
    }
    fun attachView(view: View) {
        if (vibrator != null) return
        val context = view.context
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibrator = vibratorManager.defaultVibrator
    }
    fun onZoneActivated() {
        execute(zoneEffect)
    }

    fun onMoveSimulated() {
        execute(moveEffect)
    }

    private fun execute(effect: VibrationEffect) {
        val v = vibrator ?: return
        if (v.hasVibrator()) {
            v.vibrate(effect, hapticAttributes)
        }
    }

    fun stop() {
        vibrator?.cancel()
    }
}
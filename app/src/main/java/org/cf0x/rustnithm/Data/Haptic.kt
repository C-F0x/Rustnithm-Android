package org.cf0x.rustnithm.Data

import android.view.HapticFeedbackConstants
import android.view.View
import java.lang.ref.WeakReference

class Haptic private constructor() {

    private var viewRef: WeakReference<View>? = null

    var isEnabled: Boolean = true

    fun attachView(view: View) {
        if (viewRef?.get() != view) {
            viewRef = WeakReference(view)
        }
    }

    fun onZoneActivated() {
        if (!isEnabled) return

        val view = viewRef?.get()
        if (view != null && view.isHapticFeedbackEnabled) {
            view.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                0
            )
        }
    }

    fun onMoveSimulated() {
        if (!isEnabled) return

        val view = viewRef?.get()
        if (view != null && view.isHapticFeedbackEnabled) {
            view.performHapticFeedback(
                HapticFeedbackConstants.CLOCK_TICK
            )
        }
    }

    fun setHapticStatus(status: Boolean) {
        this.isEnabled = status
    }

    companion object {
        @Volatile
        private var instance: Haptic? = null
        fun getInstance(): Haptic {
            return instance ?: synchronized(this) {
                instance ?: Haptic().also { instance = it }
            }
        }
    }
}
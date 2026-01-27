package org.cf0x.rustnithm.Data

import android.util.Log

object Net {
    init {
        try {
            System.loadLibrary("rustnithm")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("Net", "Failed to load rust_nithm library", e)
        }
    }
    external fun nativeStart(ip: String, port: Int, frequencyHz: Int)

    external fun nativeStop()
    external fun nativeSetFrequency(frequencyHz: Int)
    external fun nativeUpdateState(
        packetType: Int,
        buttonMask: Int,
        airByte: Int,
        sliderMask: Int,
        cardBcd: ByteArray?
    )

    fun start(ip: String, port: Int, frequency: Int) {
        nativeStart(ip, port, frequency)
    }

    fun stop() {
        nativeStop()
    }
    fun sendFullState(
        air: Set<Int>,
        slide: Set<Int>,
        coin: Boolean,
        service: Boolean,
        test: Boolean,
        isCardActive: Boolean,
        accessCode: String
    ) {
        when {
            isCardActive && accessCode.length == 20 -> {
                val bcd = ByteArray(10)
                for (i in 0 until 10) {
                    val high = accessCode[i * 2].digitToInt()
                    val low = accessCode[i * 2 + 1].digitToInt()
                    bcd[i] = ((high shl 4) or low).toByte()
                }
                nativeUpdateState(3, 0, 0, 0, bcd)
            }

            coin || service || test -> {
                var mask = 0
                if (coin) mask = mask or 0x01
                if (service) mask = mask or 0x02
                if (test) mask = mask or 0x04
                nativeUpdateState(1, mask, 0, 0, null)
            }

            else -> {
                var airByte = 0
                for (id in air) {
                    val bitIndex = id - 1
                    if (bitIndex in 0..5) {
                        airByte = airByte or (1 shl bitIndex)
                    }
                }

                var sliderMask = 0
                for (id in slide) {
                    val adjustedId = id - 1
                    if (adjustedId in 0..31) {
                        val bitIdx = 7 - (adjustedId % 8)
                        val group = adjustedId / 8
                        val shift = (3 - group) * 8 + bitIdx

                        sliderMask = sliderMask or (1 shl shift)
                    }
                }
                nativeUpdateState(2, 0, airByte, sliderMask, null)
            }
        }
    }
}
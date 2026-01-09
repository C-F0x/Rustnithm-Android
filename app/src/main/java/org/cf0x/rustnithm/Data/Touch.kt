package org.cf0x.rustnithm.Data

import androidx.compose.ui.geometry.Offset

object TouchLogic {

    fun getActivatedAir(touches: Collection<Offset>, airAreaHeight: Float, multiA: Float): Set<Int> {
        val activated = mutableSetOf<Int>()
        val singleAirHeight = airAreaHeight / 6
        val pairHeight = (singleAirHeight * multiA).coerceIn(0f, singleAirHeight / 2f)

        touches.forEach { pos ->
            if (pos.y in 0f..airAreaHeight) {
                val yFromAirBottom = airAreaHeight - pos.y
                for (i in 0 until 6) {
                    val irStart = i * singleAirHeight
                    val irEnd = (i + 1) * singleAirHeight
                    if (yFromAirBottom in irStart..<irEnd) {
                        if (i > 0 && yFromAirBottom < (irStart + pairHeight)) activated.add(i)
                        if (i < 5 && yFromAirBottom > (irEnd - pairHeight)) activated.add(i + 2)
                        activated.add(i + 1)
                    }
                }
            }
        }
        return activated
    }

    fun getActivatedSlide(touches: Collection<Offset>, totalWidth: Float, airAreaHeight: Float, slideAreaHeight: Float, multiS: Float): Set<Int> {
        val activated = mutableSetOf<Int>()
        val sw = totalWidth / 16
        val sh = slideAreaHeight / 2
        val radiusSq = (sw * multiS).let { it * it }

        touches.forEach { touch ->
            if (touch.y >= airAreaHeight) {
                for (index in 0 until 32) {
                    val row = if (index % 2 == 0) 0 else 1
                    val colFromLeft = 15 - (index / 2)
                    val left = colFromLeft * sw
                    val top = airAreaHeight + row * sh
                    val closestX = touch.x.coerceIn(left, left + sw)
                    val closestY = touch.y.coerceIn(top, top + sh)
                    val dx = touch.x - closestX
                    val dy = touch.y - closestY
                    if ((dx * dx + dy * dy) <= radiusSq) {
                        activated.add(index + 1)
                    }
                }
            }
        }
        return activated
    }
}
package io.github.abc15018045126.sora.util

import kotlin.math.abs

object Floats {
    @JvmStatic
    fun withinDelta(a: Float, b: Float, delta: Float): Boolean {
        return abs(a - b) < abs(delta)
    }
}

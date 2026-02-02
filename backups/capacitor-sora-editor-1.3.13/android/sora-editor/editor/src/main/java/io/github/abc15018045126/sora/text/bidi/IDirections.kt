package io.github.abc15018045126.sora.text.bidi

interface IDirections {
    val runCount: Int
    fun getRunStart(i: Int): Int
    fun getRunEnd(i: Int): Int
    fun getRunLevel(i: Int): Int
    fun isRunRtl(i: Int): Boolean
}

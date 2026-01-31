package io.github.abc15018045126.sora.util

class MutableInt(
    @JvmField var value: Int
) {

    fun decreaseAndGet(): Int {
        return --value
    }

    fun increase() {
        value++
    }
}

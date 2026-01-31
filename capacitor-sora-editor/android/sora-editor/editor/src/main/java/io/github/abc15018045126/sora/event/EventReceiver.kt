package io.github.abc15018045126.sora.event

fun interface EventReceiver<T : Event> {
    fun onReceive(event: T, unsubscribe: Unsubscribe)
}

package io.github.abc15018045126.sora.event

/**
 * Instance for unsubscribing for a receiver.
 * <p>
 * Note that this instance can be reused during an event dispatch, so
 * it is not a valid behavior to save the instance in event receivers.
 * Always use the one given by {@link EventReceiver#onReceive(Event, Unsubscribe)}.
 */
class Unsubscribe {
    var isUnsubscribed: Boolean = false
        private set

    /**
     * Unsubscribe the event. And current receiver will not get event again.
     * References to the receiver are also removed.
     */
    fun unsubscribe() {
        isUnsubscribed = true
    }

    /**
     * Reset the flag
     */
    fun reset() {
        isUnsubscribed = false
    }
}

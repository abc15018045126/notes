package io.github.abc15018045126.sora.event

import java.lang.ref.WeakReference

/**
 * Receipt of {@link EventManager#subscribeEvent(Class, EventReceiver)}. You can unsubscribe the event outside
 * the dispatch process from any thread by calling {@link SubscriptionReceipt#unsubscribe()}
 *
 * @author abc15018045126
 */
class SubscriptionReceipt<R : Event> internal constructor(
    private val manager: EventManager,
    private val clazz: Class<R>,
    receiver: EventReceiver<R>
) {
    private val receiver: WeakReference<EventReceiver<R>> = WeakReference(receiver)

    /**
     * Unsubscribe the event receiver.
     * <p>
     * Does nothing if the listener is already recycled or unsubscribed.
     */
    fun unsubscribe() {
        val receivers = manager.getReceivers(clazz)
        receivers.lock.writeLock().lock()
        try {
            val target = receiver.get()
            if (target != null) {
                receivers.receivers.remove(target)
            }
        } finally {
            receivers.lock.writeLock().unlock()
        }
    }
}

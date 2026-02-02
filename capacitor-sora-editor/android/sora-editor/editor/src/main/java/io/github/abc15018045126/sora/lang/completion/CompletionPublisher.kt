package io.github.abc15018045126.sora.lang.completion

import android.os.Handler
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import io.github.abc15018045126.sora.lang.Language

/**
 * CompletionPublisher manages completion items to be added in one completion analyzing process.
 *
 * You can only add items to the publisher, but no deletion is allowed. As you add more items, the
 * publisher will update the list in UI from time to time, which is related to your threshold
 * settings.([CompletionPublisher.setUpdateThreshold]).
 * There will usually be some items not displayed in screen when the thread is still running. Even
 * when the actual pending item count exceeds the threshold you set, there may still be some items
 * not committed because of lock failures. You can use [CompletionPublisher.updateList]
 * with forced flag to command the UI thread update the completion list, by waiting for the lock from
 * your side to release.
 * If you want to disable this feature, you may want to set it to [Integer.MAX_VALUE]
 *
 * You can set a comparator by [CompletionPublisher.setComparator] to sort your
 * result items, but you should not make it too complex, which will cause laggy in UI thread. It is
 * recommended that you set the comparator before all your actions.
 * Leaving the comparator null results the completion to be unsorted. They will be ordered by the order
 * you add them.
 *
 * After all you additions, you do not need to explicitly invoke [CompletionPublisher.updateList].
 * This will automatically be called by editor framework.
 *
 * Note that your actions may be interrupted because of [Thread.interrupted].
 */
class CompletionPublisher(
    private val handler: Handler,
    private val callback: Runnable,
    private val languageInterruptionLevel: Int
) {

    companion object {
        /**
         * Default value for [CompletionPublisher.setUpdateThreshold]
         */
        const val DEFAULT_UPDATE_THRESHOLD = 5
    }

    private val items: MutableList<CompletionItem> = ArrayList()
    private val candidates: MutableList<CompletionItem> = ArrayList()
    private val lock: Lock = ReentrantLock(true)
    private var comparator: Comparator<CompletionItem>? = null
    private var updateThreshold: Int = DEFAULT_UPDATE_THRESHOLD
    private var invalid = false

    /**
     * Check whether the completion is cancelled
     */
    val isCancelled: Boolean
        get() = invalid

    /**
     * Checks whether there is data
     */
    fun hasData(): Boolean {
        return items.size + candidates.size > 0
    }

    /**
     * Get items currently in display
     */
    @io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
    fun getItems(): List<CompletionItem> {
        return items
    }

    /**
     * Set the max pending items in analyzing thread.
     * See class javadoc for more information.
     */
    fun setUpdateThreshold(updateThreshold: Int) {
        this.updateThreshold = updateThreshold
    }

    /**
     * Set the result's comparator.
     *
     * The comparator is used when publishing the completion to user.
     */
    fun setComparator(comparator: Comparator<CompletionItem>?) {
        checkCancelled()
        if (invalid) {
            return
        }
        this.comparator = comparator
        if (items.isNotEmpty() && comparator != null) {
            handler.post {
                if (invalid) {
                    return@post
                }
                Collections.sort(items, comparator)
                callback.run()
            }
        }
    }

    /**
     * Add items in the completion list.
     *
     * According to your settings and the lock's state, these items may not immediately
     * be displayed to the user.
     *
     * @see CompletionPublisher.setUpdateThreshold
     */
    fun addItems(items: Collection<CompletionItem>) {
        checkCancelled()
        if (invalid) {
            return
        }
        lock.lock()
        try {
            candidates.addAll(items)
        } finally {
            lock.unlock()
        }
        if (candidates.size >= updateThreshold) {
            updateList()
        }
    }

    /**
     * Add a single item in completion list.
     *
     * According to your settings and the lock's state, this item may not immediately
     * be displayed to the user.
     *
     * @see CompletionPublisher.setUpdateThreshold
     */
    fun addItem(item: CompletionItem) {
        checkCancelled()
        if (invalid) {
            return
        }
        lock.lock()
        try {
            candidates.add(item)
        } finally {
            lock.unlock()
        }
        if (candidates.size >= updateThreshold) {
            updateList()
        }
    }

    /**
     * Try to update completion in main thread.
     *
     * If [Lock.tryLock] failed, nothing will happen.
     */
    fun updateList() {
        updateList(false)
    }

    /**
     * Update completion items on main thread
     *
     * @param forced If true, the main thread will wait for the lock. Otherwise, when the lock is
     *               currently available for the thread, the update will be executed.
     */
    fun updateList(forced: Boolean) {
        if (invalid) {
            return
        }
        handler.post {
            // Lock the candidate list accordingly
            if (invalid) {
                callback.run()
                return@post
            }
            var locked = false
            if (forced) {
                lock.lock()
                locked = true
            } else {
                locked = lock.tryLock()
            }

            if (locked) {
                try {
                    if (candidates.isEmpty()) {
                        callback.run()
                        return@post
                    }
                    val comparator = this.comparator
                    if (comparator != null) {
                        while (candidates.isNotEmpty()) {
                            val candidate = candidates.removeAt(0)
                            // Insert the value by binary search
                            var left = 0
                            var right = items.size
                            val size = right
                            while (left <= right) {
                                val mid = (left + right) / 2
                                if (mid < 0 || mid >= size) {
                                    left = mid
                                    break
                                }
                                val cmp = comparator.compare(items[mid], candidate)
                                if (cmp < 0) {
                                    left = mid + 1
                                } else if (cmp > 0) {
                                    right = mid - 1
                                } else {
                                    left = mid
                                    break
                                }
                            }
                            left = kotlin.math.max(0, kotlin.math.min(size, left))
                            items.add(left, candidate)
                        }
                    } else {
                        items.addAll(candidates)
                        candidates.clear()
                    }
                    callback.run()
                } finally {
                    lock.unlock()
                }
            }
        }
    }

    /**
     * Cancel the completion
     */
    fun cancel() {
        invalid = true
    }

    /**
     * Check whether the completion is cancelled. If so, an instance of [CompletionCancelledException]
     * is thrown.
     */
    fun checkCancelled() {
        if (Thread.interrupted() || invalid) {
            invalid = true
            if (languageInterruptionLevel <= Language.INTERRUPTION_LEVEL_SLIGHT) {
                throw CompletionCancelledException()
            }
        }
    }
}

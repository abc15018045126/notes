package io.github.abc15018045126.sora.lang.format

import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.TextRange
import java.lang.ref.WeakReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Base class for formatting code in another thread.
 */
abstract class AsyncFormatter : Formatter {

    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()
    private var receiver: WeakReference<Formatter.FormatResultReceiver>? = null
    
    @Volatile
    private var text: Content? = null
    
    @Volatile
    private var range: TextRange? = null
    
    @Volatile
    private var cursorRange: TextRange? = null

    private var thread: FormattingThread? = null

    override fun setReceiver(receiver: Formatter.FormatResultReceiver?) {
        this.receiver = receiver?.let { WeakReference(it) }
    }

    private fun run() {
        if (thread == null || thread?.isAlive == false) {
            // Create new thread
            Log.v(LOG_TAG, "Starting a new thread for formatting")
            thread = FormattingThread().apply {
                isDaemon = true
                name = "AsyncFormatter-${nextThreadId()}"
                start()
            }
        } else {
            // Wake up thread
            Log.v(LOG_TAG, "Waking up thread for formatting")
            lock.withLock {
                condition.signal()
            }
        }
    }

    override fun format(@NonNull text: Content, @NonNull cursorRange: TextRange) {
        this.text = text
        range = null
        this.cursorRange = cursorRange
        run()
    }

    override fun isRunning(): Boolean {
        return thread != null && thread?.isAlive == true && lock.isLocked
    }

    override fun formatRegion(@NonNull text: Content, @NonNull rangeToFormat: TextRange, @NonNull cursorRange: TextRange) {
        this.text = text
        range = rangeToFormat
        this.cursorRange = cursorRange
        run()
    }

    /**
     * like [Formatter.format], but run in background thread.
     *
     * Implementation of this method can edit text directly to generate formatted code.
     *
     * @return the new cursor range to be applied to the text
     */
    @WorkerThread
    @Nullable
    abstract fun formatAsync(@NonNull text: Content, @NonNull cursorRange: TextRange): TextRange?

    /**
     * like [Formatter.formatRegion], but run in background thread
     *
     * Implementation of this method can edit text directly to generate formatted code.
     *
     * @return the new cursor range to be applied to the text
     */
    @WorkerThread
    @Nullable
    abstract fun formatRegionAsync(@NonNull text: Content, @NonNull rangeToFormat: TextRange, @NonNull cursorRange: TextRange): TextRange?

    private fun sendUpdate(text: Content?, cursorRange: TextRange?) {
        if (!Thread.currentThread().isInterrupted) {
            receiver?.get()?.onFormatSucceed(text.toString(), cursorRange)
        }
    }

    private fun sendFailure(throwable: Throwable) {
        if (!Thread.currentThread().isInterrupted) {
            receiver?.get()?.onFormatFail(throwable)
        }
    }

    override fun cancel() {
        thread?.let {
            if (it.isAlive) {
                it.interrupt()
            }
            thread = null
        }
    }

    override fun destroy() {
        if (thread != null && thread?.isAlive == true) {
            thread?.interrupt()
        }
        thread = null
        receiver = null
        text = null
        range = null
    }

    private inner class FormattingThread : Thread() {

        override fun run() {
            Log.v(LOG_TAG, "AsyncFormatter thread started")
            try {
                while (!isInterrupted) {
                    lock.lock()
                    val currentText = text
                    if (currentText == null) {
                        lock.unlock()
                        continue
                    }
                    val currentCursorRange = cursorRange ?: TextRange(CharPosition(), CharPosition())
                    val newRange: TextRange?
                    val currentRange = range
                    if (currentRange == null) {
                        newRange = formatAsync(currentText, currentCursorRange)
                    } else {
                        newRange = formatRegionAsync(currentText, currentRange, currentCursorRange)
                    }
                    sendUpdate(currentText, newRange)
                    // un-refer immediately
                    text = null
                    range = null
                    // Wait for next time
                    try {
                        condition.await()
                    } finally {
                        lock.unlock()
                    }
                }
            } catch (e: InterruptedException) {
                Log.v(LOG_TAG, "Thread is interrupted.")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Unexpected exception is thrown in the thread.", e)
                sendFailure(e)
            } finally {
                if (lock.isHeldByCurrentThread) {
                    lock.unlock()
                }
            }
        }
    }

    companion object {
        private const val LOG_TAG = "AsyncFormatter"
        private var sThreadId = 0

        @Synchronized
        private fun nextThreadId(): Int {
            sThreadId++
            return sThreadId
        }
    }
}

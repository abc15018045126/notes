package io.github.abc15018045126.sora.widget.layout

import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.widget.CodeEditor
import java.util.Collections
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Base layout implementation of [Layout].
 * It provides some convenient methods to editor instance and text measuring.
 *
 * @author abc15018045126
 */
abstract class AbstractLayout(
    @JvmField protected var editor: CodeEditor?,
    @JvmField protected var text: Content?
) : Layout {

    protected open fun getSpans(line: Int): List<Span> {
        return editor?.getSpansForLine(line)?.filterNotNull() ?: emptyList()
    }

    internal open fun getInlayHints(line: Int): List<InlayHint> {
        val inlayHints = editor?.inlayHints
        return inlayHints?.getForLine(line) ?: Collections.emptyList()
    }

    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
    }

    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
    }

    override fun beforeReplace(content: Content) {
    }
    
    override fun beforeModification(content: Content) {
    }

    override fun destroyLayout() {
        editor = null
        text = null
    }

    protected fun submitTask(task: LayoutTask<*>) {
        executor.submit(task)
    }

    protected open class TaskMonitor(private val taskCount: Int, private val callback: Callback) {
        private val results: Array<Any?> = arrayOfNulls(taskCount)
        private var completedCount = 0
        private var cancelledCount = 0

        @Synchronized
        fun reportCompleted(result: Any?) {
            results[completedCount++] = result
            if (completedCount == taskCount) {
                callback.onCompleted(results, cancelledCount)
            }
        }

        @Synchronized
        fun reportCancelled() {
            cancelledCount++
            reportCompleted(null)
        }

        interface Callback {
            fun onCompleted(results: Array<Any?>, cancelledCount: Int)
        }
    }

    protected abstract inner class LayoutTask<T>(private val monitor: TaskMonitor) : Runnable {
        protected open fun shouldRun(): Boolean {
            return editor != null
        }

        override fun run() {
            if (shouldRun()) {
                val result = compute()
                monitor.reportCompleted(result)
            } else {
                monitor.reportCancelled()
            }
        }

        protected abstract fun compute(): T
    }

    companion object {
        protected const val SUBTASK_COUNT = 8
        protected const val MIN_LINE_COUNT_FOR_SUBTASK = 3000
        private val executor: ThreadPoolExecutor

        init {
            val maximumPoolSize = max(2, Runtime.getRuntime().availableProcessors())
            val corePoolSize = 2
            executor = ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                1,
                TimeUnit.MINUTES,
                LinkedBlockingQueue(128)
            )
        }
    }
}

package io.github.abc15018045126.sora.lang.analysis

import android.os.Message
import android.util.Log
import io.github.abc15018045126.sora.lang.styling.*
import io.github.abc15018045126.sora.lang.util.BaseAnalyzeManager
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

/**
 * Asynchronous base implementation of [IncrementalAnalyzeManager]
 *
 * @author abc15018045126
 */
abstract class AsyncIncrementalAnalyzeManager<S, T> : BaseAnalyzeManager(), IncrementalAnalyzeManager<S, T> {

    private var thread: LooperThread? = null

    @Volatile
    private var runCount: Long = 0

    /**
     * Run the given code block only when the receiver is currently non-null
     */
    protected fun withReceiver(consumer: (StyleReceiver) -> Unit) {
        val r = receiver
        if (r != null) {
            consumer(r)
        }
    }

    override fun insert(start: CharPosition, end: CharPosition, insertedText: CharSequence) {
        thread?.let {
            increaseRunCount()
            it.offerMessage(MSG_MOD, TextModification(IntPair.pack(start.line, start.column), IntPair.pack(end.line, end.column), insertedText))
        }
    }

    override fun delete(start: CharPosition, end: CharPosition, deletedText: CharSequence) {
        thread?.let {
            increaseRunCount()
            it.offerMessage(MSG_MOD, TextModification(IntPair.pack(start.line, start.column), IntPair.pack(end.line, end.column), null))
        }
    }

    override fun rerun() {
        thread?.let {
            if (it.isAlive) {
                it.interrupt()
                it.abort = true
            }
        }
        thread = null
        val ref = contentRef
        if (ref != null) {
            val text = ref.reference.copyText(false)
            text.isUndoEnabled = false
            val newThread = LooperThread()
            newThread.name = "AsyncAnalyzer-" + nextThreadId()
            newThread.offerMessage(MSG_INIT, text)
            increaseRunCount()
            sendNewStyles(null)
            newThread.start()
            thread = newThread
        }
    }

    override fun getState(line: Int): IncrementalAnalyzeManager.LineTokenizeResult<S, T>? {
        val currentThread = thread
        if (currentThread === Thread.currentThread()) {
            val states = currentThread?.states
            if (states != null && line >= 0 && line < states.size) {
                return states[line]
            }
            return null
        }
        throw SecurityException("Can not get state from non-analytical or abandoned thread")
    }

    override fun onAbandonState(state: S) {
    }

    override fun onAddState(state: S) {
    }

    @Synchronized
    private fun increaseRunCount() {
        runCount++
    }

    override fun destroy() {
        thread?.let {
            if (it.isAlive) {
                it.interrupt()
            }
            it.abort = true
        }
        thread = null
        super.destroy()
    }

    private fun sendNewStyles(styles: Styles?) {
        receiver?.setStyles(this, styles)
    }

    private fun sendUpdate(styles: Styles, startLine: Int, endLine: Int) {
        receiver?.updateStyles(this, styles, SequenceUpdateRange(startLine, endLine))
    }

    /**
     * Compute code blocks
     *
     * @param text The text. can be safely accessed.
     */
    abstract fun computeBlocks(text: Content, delegate: CodeBlockAnalyzeDelegate): List<CodeBlock>?

    fun getManagedStyles(): Styles {
        val currentThread = Thread.currentThread()
        if (currentThread !is AsyncIncrementalAnalyzeManager<*, *>.LooperThread) {
            throw IllegalThreadStateException()
        }
        return currentThread.styles!!
    }

    private class LockedSpans : Spans {

        private val lock = ReentrantLock()
        private val lines = ArrayList<Line>(128)

        override fun adjustOnDelete(start: CharPosition, end: CharPosition) {
        }

        override fun adjustOnInsert(start: CharPosition, end: CharPosition) {
        }

        override fun getLineCount(): Int {
            return lines.size
        }

        override fun read(): Spans.Reader {
            return ReaderImpl()
        }

        override fun modify(): Spans.Modifier {
            return ModifierImpl()
        }

        override fun supportsModify(): Boolean = true

        private class Line(var spans: List<Span>) {
            val lock = ReentrantLock()
        }

        private inner class ReaderImpl : Spans.Reader {
            private var currentLine: Line? = null

            override fun moveToLine(line: Int) {
                if (line < 0 || line >= lines.size) {
                    currentLine?.lock?.unlock()
                    currentLine = null
                } else {
                    currentLine?.lock?.unlock()
                    var locked = false
                    try {
                        locked = lock.tryLock(100, TimeUnit.MICROSECONDS)
                    } catch (e: InterruptedException) {
                        Log.w(LOG_TAG, "failed to acquire the lock", e)
                        Thread.currentThread().interrupt()
                    }
                    if (locked) {
                        try {
                            val obj = lines[line]
                            if (obj.lock.tryLock()) {
                                currentLine = obj
                            } else {
                                currentLine = null
                            }
                        } finally {
                            lock.unlock()
                        }
                    } else {
                        currentLine = null
                    }
                }
            }

            override fun getSpanCount(): Int {
                return currentLine?.spans?.size ?: 1
            }

            override fun getSpanAt(index: Int): Span {
                return currentLine?.spans?.get(index) ?: SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
            }

            override fun getSpansOnLine(line: Int): List<Span>? {
                var locked = false
                try {
                    locked = lock.tryLock(1, TimeUnit.MILLISECONDS)
                } catch (e: InterruptedException) {
                    Log.w(LOG_TAG, "failed to acquire the lock", e)
                }
                if (locked) {
                    var obj: Line? = null
                    try {
                        if (line < lines.size) {
                            obj = lines[line]
                        }
                    } finally {
                        lock.unlock()
                    }
                    if (obj != null && obj.lock.tryLock()) {
                        try {
                            return Collections.unmodifiableList(obj.spans)
                        } finally {
                            obj.lock.unlock()
                        }
                    } else {
                        return listOf(getSpanAt(0))
                    }
                } else {
                    return listOf(getSpanAt(0))
                }
            }
        }

        private inner class ModifierImpl : Spans.Modifier {
            override fun setSpansOnLine(line: Int, spans: List<Span>) {
                lock.lock()
                try {
                    while (lines.size <= line) {
                        val list = mutableListOf<Span>()
                        list.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
                        lines.add(Line(list))
                    }
                    val obj = lines[line]
                    obj.lock.lock()
                    try {
                        obj.spans = spans
                    } finally {
                        obj.lock.unlock()
                    }
                } finally {
                    lock.unlock()
                }
            }

            override fun addLineAt(line: Int, spans: List<Span>) {
                lock.lock()
                try {
                    lines.add(line, Line(spans))
                } finally {
                    lock.unlock()
                }
            }

            override fun deleteLineAt(line: Int) {
                lock.lock()
                try {
                    val obj = lines[line]
                    obj.lock.lock()
                    try {
                        lines.removeAt(line)
                    } finally {
                        obj.lock.unlock()
                    }
                } finally {
                    lock.unlock()
                }
            }
        }

        companion object {
            private const val LOG_TAG = "LockedSpans"
        }
    }

    private class TextModification(val start: Long, val end: Long, val changedText: CharSequence?)

    /**
     * Helper class for analyzing code block
     */
    inner class CodeBlockAnalyzeDelegate internal constructor(private val thread: LooperThread) {
        @JvmField
        var suppressSwitch: Int = 0

        fun setSuppressSwitch(suppressSwitch: Int) {
            this.suppressSwitch = suppressSwitch
        }

        internal fun reset() {
            suppressSwitch = Int.MAX_VALUE
        }

        fun isCancelled(): Boolean {
            return thread.myRunCount != runCount || thread.abort || thread.isInterrupted
        }

        fun isNotCancelled(): Boolean {
            return !isCancelled()
        }
    }

    inner class LooperThread : Thread() {
        private val messageQueue = LinkedBlockingQueue<Message>()
        @Volatile
        var abort: Boolean = false
        private var shadowed: Content? = null
        var myRunCount: Long = 0

        val states = ArrayList<IncrementalAnalyzeManager.LineTokenizeResult<S, T>>()
        var styles: Styles? = null
        private var spans: LockedSpans? = null
    val delegate = CodeBlockAnalyzeDelegate(this)

        fun offerMessage(what: Int, obj: Any?) {
            val msg = Message.obtain()
            msg.what = what
            msg.obj = obj
            messageQueue.offer(msg)
        }

        private fun initialize() {
            val sSpans = LockedSpans()
            this.spans = sSpans
            styles = Styles(sSpans)
            var state = initialState
            val mdf = sSpans.modify()
            val currentShadowed = shadowed!!
            for (i in 0 until currentShadowed.lineCount) {
                if (abort || isInterrupted) break
                val line = currentShadowed.getLine(i)
                val result = tokenizeLine(line, state, i)
                state = result.state
                val lineSpans = result.spans ?: generateSpansForLine(result) ?: emptyList()
                states.add(result.clearSpans())
                onAddState(result.state)
                mdf.addLineAt(i, lineSpans)
            }
            styles?.let {
                it.blocks = computeBlocks(currentShadowed, delegate)?.toMutableList()
                it.setSuppressSwitch(delegate.suppressSwitch)
                it.finishBuilding()
                if (!abort) {
                    sendNewStyles(it)
                }
            }
        }

        private fun handleMessage(msg: Message): Boolean {
            try {
                myRunCount = runCount
                delegate.reset()
                when (msg.what) {
                    MSG_INIT -> {
                        shadowed = msg.obj as Content
                        if (!abort && !isInterrupted) {
                            initialize()
                        }
                    }
                    MSG_MOD -> {
                        var updateStart = 0
                        var updateEnd = 0
                        if (!abort && !isInterrupted) {
                            val mod = msg.obj as TextModification
                            val startLine = IntPair.getFirst(mod.start)
                            val endLine = IntPair.getFirst(mod.end)
                            val currentShadowed = shadowed!!

                            updateStart = startLine
                            if (mod.changedText == null) {
                                currentShadowed.delete(
                                    IntPair.getFirst(mod.start), IntPair.getSecond(mod.start),
                                    IntPair.getFirst(mod.end), IntPair.getSecond(mod.end)
                                )
                                var state = if (startLine == 0) initialState else states[startLine - 1].state
                                // Remove states
                                if (endLine >= startLine + 1) {
                                    val subList = states.subList(startLine + 1, endLine + 1)
                                    for (stLineTokenizeResult in subList) {
                                        onAbandonState(stLineTokenizeResult.state)
                                    }
                                    subList.clear()
                                }
                                val mdf = spans?.modify()
                                mdf?.let {
                                    for (i in startLine + 1..endLine) {
                                        it.deleteLineAt(startLine + 1)
                                    }
                                    var line = startLine
                                    while (line < currentShadowed.lineCount) {
                                        val res = tokenizeLine(currentShadowed.getLine(line), state, line)
                                        it.setSpansOnLine(line, res.spans ?: generateSpansForLine(res) ?: emptyList())
                                        val old = states.set(line, res.clearSpans())
                                        if (old != null) {
                                            onAbandonState(old.state)
                                        }
                                        onAddState(res.state)
                                        if (stateEquals(old?.state as S, res.state)) {
                                            break
                                        }
                                        state = res.state
                                        line++
                                    }
                                    updateEnd = line
                                }
                            } else {
                                currentShadowed.insert(IntPair.getFirst(mod.start), IntPair.getSecond(mod.start), mod.changedText)
                                var state = if (startLine == 0) initialState else states[startLine - 1].state
                                var line = startLine
                                val mdf = spans?.modify()
                                mdf?.let {
                                    // Add Lines
                                    while (line <= endLine) {
                                        val res = tokenizeLine(currentShadowed.getLine(line), state, line)
                                        if (line == startLine) {
                                            it.setSpansOnLine(line, res.spans ?: generateSpansForLine(res) ?: emptyList())
                                            val old = states.set(line, res.clearSpans())
                                            if (old != null) {
                                                onAbandonState(old.state)
                                            }
                                        } else {
                                            it.addLineAt(line, res.spans ?: generateSpansForLine(res) ?: emptyList())
                                            states.add(line, res.clearSpans())
                                        }
                                        onAddState(res.state)
                                        state = res.state
                                        line++
                                    }
                                    // line = end.line + 1, check whether the state equals
                                    var flag = true
                                    while (line < currentShadowed.lineCount && flag) {
                                        val res = tokenizeLine(currentShadowed.getLine(line), state, line)
                                        if (stateEquals(res.state, states[line].state)) {
                                            flag = false
                                        }
                                        it.setSpansOnLine(line, res.spans ?: generateSpansForLine(res) ?: emptyList())
                                        val old = states.set(line, res.clearSpans())
                                        if (old != null) {
                                            onAbandonState(old.state)
                                        }
                                        onAddState(res.state)
                                        state = res.state
                                        line++
                                    }
                                    updateEnd = line
                                }
                            }
                        }
                        // Do not update incomplete code blocks
                        val currentShadowed = shadowed!!
                        val blocks = computeBlocks(currentShadowed, delegate)
                        styles?.let {
                            if (delegate.isNotCancelled()) {
                                it.blocks = blocks?.toMutableList()
                                it.finishBuilding()
                                it.setSuppressSwitch(delegate.suppressSwitch)
                            }
                            if (!abort) {
                                sendUpdate(it, updateStart, updateEnd)
                            }
                        }
                    }
                }
                return true
            } catch (e: Exception) {
                Log.w("AsyncAnalysis", "Thread " + Thread.currentThread().name + " failed", e)
            }
            return false
        }

        override fun run() {
            try {
                while (!abort && !isInterrupted) {
                    val msg = messageQueue.take()
                    if (!handleMessage(msg)) {
                        break
                    }
                    msg.recycle()
                }
            } catch (e: InterruptedException) {
                // ignored
            }
        }
    }

    companion object {
        private const val MSG_BASE = 11451400
        private const val MSG_INIT = MSG_BASE + 1
        private const val MSG_MOD = MSG_BASE + 2
        private var sThreadId = 0

        @Synchronized
        private fun nextThreadId(): Int {
            sThreadId++
            return sThreadId
        }
    }
}

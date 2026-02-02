package io.github.abc15018045126.sora.widget

import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.SurroundingText
import android.view.inputmethod.TextAttribute
import android.view.inputmethod.TextSnapshot
import androidx.annotation.RequiresApi
import io.github.abc15018045126.sora.event.ContentChangeEvent
import io.github.abc15018045126.sora.event.ImePrivateCommandEvent
import io.github.abc15018045126.sora.event.SelectionChangeEvent
import io.github.abc15018045126.sora.event.EventReceiver
import io.github.abc15018045126.sora.event.Unsubscribe
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.ComposingText
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.Cursor
import io.github.abc15018045126.sora.util.Logger
import kotlin.math.max
import kotlin.math.min

/**
 * Connection between input method and editor
 *
 * @author abc15018045126
 */
internal class EditorInputConnection(targetView: CodeEditor) : BaseInputConnection(targetView, true) {
    private val editor: CodeEditor = targetView
    @JvmField
    internal var composingText: ComposingText = ComposingText()
    internal var imeConsumingInput: Boolean = false
    private var connectionInvalid: Boolean = false

    init {
        editor.subscribeEvent(ContentChangeEvent::class.java, object : EventReceiver<ContentChangeEvent> {
            override fun onReceive(event: ContentChangeEvent, unsubscribe: Unsubscribe) {
                if (event.action == ContentChangeEvent.ACTION_INSERT) {
                    composingText.shiftOnInsert(event.changeStart.index, event.changeEnd.index)
                } else if (event.action == ContentChangeEvent.ACTION_DELETE) {
                    composingText.shiftOnDelete(event.changeStart.index, event.changeEnd.index)
                }
            }
        })
    }

    internal fun markInvalid() {
        connectionInvalid = true
        composingText.reset()
        resetBatchEdit()
        editor.invalidate()
    }

    /**
     * Reset the state of this connection
     */
    internal fun reset() {
        resetBatchEdit()
        composingText.reset()
        connectionInvalid = false
        imeConsumingInput = false
    }

    private fun resetBatchEdit() {
        val content = editor.text
        while (content.isInBatchEdit) {
            content.endBatchEdit()
        }
    }

    private val cursor: Cursor
        /**
         * Private use.
         * Get the Cursor of Content displaying by Editor
         *
         * @return Cursor
         */
        get() = editor.cursor!!

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (DEBUG) logger.d("commitText text = $text, pos = $newCursorPosition")

        if (!editor.isEditable || connectionInvalid || text == null) {
            return false
        }

        if ("\n" == text.toString()) {
            // #67
            sendKeyClick(KeyEvent.KEYCODE_ENTER)
        } else {
            commitTextInternal(text, true)
        }
        return true
    }

    @Synchronized
    override fun closeConnection() {
        super.closeConnection()
        resetBatchEdit()
        composingText.reset()
        editor.onCloseConnection()
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        return TextUtils.getCapsMode(editor.text, cursor.left, reqModes)
    }

    /**
     * Get content region internally
     */
    private fun getTextRegionInternal(start: Int, end: Int, flags: Int, ignoreIPCLimit: Boolean): CharSequence? {
        var mutableStart = start
        var mutableEnd = end
        val origin = editor.text
        if (mutableStart > mutableEnd) {
            val tmp = mutableStart
            mutableStart = mutableEnd
            mutableEnd = tmp
        }
        if (mutableStart < 0) {
            mutableStart = 0
        }
        if (mutableEnd > origin.length) {
            mutableEnd = origin.length
        }
        if (mutableEnd < mutableStart) {
            mutableEnd = 0
            mutableStart = mutableEnd
        }
        if (!ignoreIPCLimit && mutableEnd - mutableStart > editor.props!!.maxIPCTextLength) {
            mutableEnd = mutableStart + max(0, editor.props!!.maxIPCTextLength)
        }
        val sub = origin.subSequence(mutableStart, mutableEnd).toString()
        if (flags == GET_TEXT_WITH_STYLES) {
            val text = SpannableStringBuilder(sub)
            // Apply composing span
            if (composingText.isComposing()) {
                try {
                    val originalComposingStart = composingText.startIndex
                    val originalComposingEnd = composingText.endIndex
                    var transferredStart = originalComposingStart - mutableStart
                    if (transferredStart >= text.length) {
                        return text
                    }
                    if (transferredStart < 0) {
                        transferredStart = 0
                    }
                    var transferredEnd = originalComposingEnd - mutableStart
                    if (transferredEnd <= 0) {
                        return text
                    }
                    if (transferredEnd >= text.length) {
                        transferredEnd = text.length
                    }
                    text.setSpan(
                        Spanned.SPAN_COMPOSING,
                        transferredStart,
                        transferredEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } catch (e: IndexOutOfBoundsException) {
                    //ignored
                }
            }
            return text
        }
        return sub
    }

    internal fun getTextRegion(start: Int, end: Int, flags: Int): CharSequence? {
        try {
            val res = getTextRegionInternal(start, end, flags, false)
            if (DEBUG) logger.d("getTextRegion result:$res")
            return res
        } catch (e: IndexOutOfBoundsException) {
            logger.w("Failed to get text region for IME", e)
            return ""
        }
    }

    protected fun getTextRegionUnlimited(start: Int, end: Int, flags: Int): CharSequence? {
        try {
            val res = getTextRegionInternal(start, end, flags, true)
            if (DEBUG) logger.d("getTextRegion result:$res")
            return res
        } catch (e: IndexOutOfBoundsException) {
            logger.w("Failed to get text region for IME", e)
            return ""
        }
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        if (editor.props!!.disallowSuggestions) {
            return null
        }
        //This text should be limited because when the user try to select all text
        //it can be quite large text and costs time, which will finally cause ANR
        val left = cursor.left
        val right = cursor.right
        return if (left == right) null else getTextRegion(left, right, flags)
    }

    override fun getTextBeforeCursor(length: Int, flags: Int): CharSequence? {
        if (editor.props!!.disallowSuggestions) {
            return ""
        }
        val end = cursor.left
        val start = max(end - length, end - editor.props!!.maxIPCTextLength)
        return getTextRegion(start, end, flags)
    }

    override fun getTextAfterCursor(length: Int, flags: Int): CharSequence? {
        if (editor.props!!.disallowSuggestions) {
            return ""
        }
        val end = cursor.right
        return getTextRegion(end, end + length, flags)
    }

    private fun sendKeyClick(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        sendKeyEvent(
            KeyEvent(
                eventTime, eventTime,
                KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
        sendKeyEvent(
            KeyEvent(
                SystemClock.uptimeMillis(), eventTime,
                KeyEvent.ACTION_UP, keyCode, 0, 0,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
            )
        )
    }

    internal fun commitTextInternal(text: CharSequence, applyAutoIndent: Boolean) {
        var mutableText = text
        val composingStateBefore = composingText.isComposing()
        // NOTE: Text styles are ignored by editor
        // Remove composing text first if there is
        if (editor.props!!.trackComposingTextOnCommit) {
            if (composingText.isComposing()) {
                val composingStr = editor.text.subSequence(composingText.startIndex, composingText.endIndex).toString()
                val commitText = mutableText.toString()
                if (composingText.endIndex == cursor.left && !cursor.isSelected() && commitText.startsWith(
                        composingStr
                    ) && commitText.length > composingStr.length
                ) {
                    mutableText = commitText.substring(composingStr.length)
                    composingText.reset()
                } else {
                    deleteComposingText()
                }
            }
        } else if (composingText.isComposing()) {
            deleteComposingText()
        }

        editor.commitText(mutableText, applyAutoIndent)

        if (composingStateBefore) {
            endBatchEdit()
        }
    }

    /**
     * Delete composing region
     */
    private fun deleteComposingText() {
        if (!composingText.isComposing()) {
            return
        }
        try {
            editor.text.delete(composingText.startIndex, composingText.endIndex)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }
        composingText.reset()
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        if (DEBUG) logger.d("deleteSurroundingText, before = $beforeLength, after = $afterLength")
        if (!editor.isEditable || connectionInvalid) {
            return false
        }
        if (beforeLength < 0 || afterLength < 0) {
            return false
        }

        // #170 Gboard compatible
        if (beforeLength == 1 && afterLength == 0 && !composingText.isComposing()) {
            editor.deleteText()
            return true
        }

        // Start a batch edit when the operation can not be finished by one call to delete()
        if (beforeLength > 0 && afterLength > 0) {
            beginBatchEdit()
        }

        val composing = composingText.isComposing()
        var composingStart = if (composing) composingText.startIndex else 0
        var composingEnd = if (composing) composingText.endIndex else 0

        var rangeEnd = cursor.left
        var rangeStart = rangeEnd - beforeLength
        if (rangeStart < 0) {
            rangeStart = 0
        }
        editor.text.delete(rangeStart, rangeEnd)

        if (composing) {
            val crossStart = max(rangeStart, composingStart)
            val crossEnd = min(rangeEnd, composingEnd)
            composingEnd -= max(0, crossEnd - crossStart)
            val delta = max(0, crossStart - rangeStart)
            composingEnd -= delta
            composingStart -= delta
        }

        rangeStart = cursor.right
        rangeEnd = rangeStart + afterLength
        if (rangeEnd > editor.text.length) {
            rangeEnd = editor.text.length
        }
        editor.text.delete(rangeStart, rangeEnd)

        if (composing) {
            val crossStart = max(rangeStart, composingStart)
            val crossEnd = min(rangeEnd, composingEnd)
            composingEnd -= max(0, crossEnd - crossStart)
            val delta = max(0, crossStart - rangeStart)
            composingEnd -= delta
            composingStart -= delta
        }

        if (beforeLength > 0 && afterLength > 0) {
            endBatchEdit()
        }

        return true
    }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean {
        // Unsupported operation
        // According to document, we should return false
        return false
    }

    @Synchronized
    override fun beginBatchEdit(): Boolean {
        if (DEBUG) logger.d("beginBatchEdit")
        if (editor.props!!.disallowSuggestions) {
            return editor.text.isInBatchEdit // Do not start new batch edit layer
        }
        return editor.text.beginBatchEdit()
    }

    @Synchronized
    override fun endBatchEdit(): Boolean {
        if (DEBUG) logger.d("endBatchEdit")
        val inBatch = editor.text.endBatchEdit()
        if (!inBatch) {
            editor.updateSelection()
        }
        return inBatch
    }

    private fun deleteSelected() {
        if (cursor.isSelected()) {
            // Delete selected text
            editor.deleteText()
        }
    }

    override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
        if (DEBUG) logger.d("setComposingText, text = $text, pos = $newCursorPosition")
        if (!editor.isEditable || connectionInvalid || !editor.acceptsComposingText()) {
            return false
        }
        if (editor.props!!.disallowSuggestions) {
            composingText.reset()
            commitText(text, 0)
            //editor.restartInput();
            return false
        }
        if (TextUtils.indexOf(text, '\n') != -1) {
            return false
        }
        if (!composingText.isComposing()) {
            // Create composing info
            composingText.preSetComposing = true
            deleteSelected()
            beginBatchEdit()
            editor.commitText(text)
            composingText.preSetComposing = false
            composingText.set(cursor.left - text.length, cursor.left)
            editor.updateCursor()
        } else {
            // Already have composing text
            if (composingText.isComposing()) {
                if (editor.props!!.minimizeComposingTextUpdate) {
                    setComposingTextCompat(text.toString())
                } else {
                    editor.text.replace(composingText.startIndex, composingText.endIndex, text)
                }
                // Reset range
                composingText.adjustLength(text.length)
            }
        }
        if (text.length == 0) {
            finishComposingText()
            return true
        }
        return true
    }

    private fun setComposingTextCompat(text: String) {
        val content = editor.text
        val current = content.substring(composingText.startIndex, composingText.endIndex)
        if (current == text) {
            return
        }
        if (current.length < text.length && text.startsWith(current)) {
            val pos = content.indexer.getCharPosition(composingText.endIndex)
            content.insert(pos.line, pos.column, text.substring(current.length))
        } else if (current.length > text.length && current.startsWith(text)) {
            content.delete(composingText.endIndex - (current.length - text.length), composingText.endIndex)
        } else {
            content.replace(composingText.startIndex, composingText.endIndex, text)
        }
    }

    override fun finishComposingText(): Boolean {
        if (DEBUG) logger.d("finishComposingText")
        if (!editor.isEditable || connectionInvalid) {
            return false
        }
        if (editor.props!!.disallowSuggestions) {
            return false
        }
        composingText.reset()
        endBatchEdit()
        editor.updateCursor()
        editor.invalidate()
        return true
    }

    private fun getWrappedIndex(index: Int): Int {
        if (index < 0) {
            return 0
        }
        if (index > editor.text.length) {
            return editor.text.length
        }
        return index
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        var mutableStart = start
        var mutableEnd = end
        if (DEBUG) logger.d("setSelection, s = $mutableStart, e = $mutableEnd")
        if (!editor.isEditable || connectionInvalid || editor.props!!.disallowSuggestions) {
            return false
        }
        mutableStart = getWrappedIndex(mutableStart)
        mutableEnd = getWrappedIndex(mutableEnd)
        if (mutableStart > mutableEnd) {
            val tmp = mutableStart
            mutableStart = mutableEnd
            mutableEnd = tmp
        }
        if (mutableStart == cursor.left && mutableEnd == cursor.right) {
            return true
        }
        val content = editor.text
        val startPos = content.indexer.getCharPosition(mutableStart)
        val endPos = content.indexer.getCharPosition(mutableEnd)
        editor.setSelectionRegion(
            startPos.line,
            startPos.column,
            endPos.line,
            endPos.column,
            false,
            SelectionChangeEvent.CAUSE_IME
        )
        return true
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        var mutableStart = start
        var mutableEnd = end
        if (DEBUG) logger.d("setComposingRegion, s = $mutableStart, e = $mutableEnd")
        if (!editor.isEditable || connectionInvalid || !editor.acceptsComposingText() || editor.props!!.disallowSuggestions) {
            return false
        }
        if (mutableStart == mutableEnd) {
            finishComposingText()
            return true
        }
        try {
            if (mutableStart > mutableEnd) {
                val tmp = mutableStart
                mutableStart = mutableEnd
                mutableEnd = tmp
            }
            if (mutableStart < 0) {
                mutableStart = 0
            }
            val content = editor.text
            if (mutableEnd > content.length) {
                mutableEnd = content.length
            }
            if (mutableStart >= mutableEnd) {
                return false
            }
            composingText.set(mutableStart, mutableEnd)
            editor.invalidate()
        } catch (e: IndexOutOfBoundsException) {
            logger.w("set composing region for IME failed", e)
            return false
        }
        beginBatchEdit()
        return true
    }

    override fun performContextMenuAction(id: Int): Boolean {
        when (id) {
            android.R.id.selectAll -> {
                editor.selectAll()
                return true
            }

            android.R.id.cut -> {
                editor.copyText()
                if (cursor.isSelected()) {
                    editor.deleteText()
                }
                return true
            }

            android.R.id.paste, android.R.id.pasteAsPlainText -> {
                editor.pasteText()
                return true
            }

            android.R.id.copy -> {
                editor.copyText()
                return true
            }

            android.R.id.undo -> {
                editor.undo()
                return true
            }

            android.R.id.redo -> {
                editor.redo()
                return true
            }
        }
        return false
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        editor.updateCursorAnchor()
        return true
    }

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? {
        if (DEBUG) logger.d("getExtractedText, flags = $flags")
        if (editor.props!!.disallowSuggestions || editor.props!!.disableTextExtracting) {
            return null
        }
        if ((flags and GET_EXTRACTED_TEXT_MONITOR) != 0) {
            editor.setExtracting(request!!)
        } else {
            editor.setExtracting(null)
        }

        return editor.extractText(request!!)
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        editor.getKeyMetaStates().clearMetaStates(states)
        return true
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        return false
    }

    override fun getHandler(): Handler {
        return editor.handler
    }

    @RequiresApi(31)
    override fun getSurroundingText(beforeLength: Int, afterLength: Int, flags: Int): SurroundingText? {
        if (DEBUG) logger.d("getSurroundingText, beforeLen = $beforeLength, afterLen = $afterLength")
        if (editor.props!!.disallowSuggestions) {
            return SurroundingText("", 0, 0, -1)
        }
        require(!((beforeLength or afterLength) < 0)) { "length < 0" }
        var startOffset = max(0, cursor.left - beforeLength)
        val selStart = cursor.left
        startOffset = min(startOffset, selStart)
        val text = getTextRegionUnlimited(
            startOffset,
            min(editor.text.length, cursor.right + afterLength),
            flags
        )
        return SurroundingText(
            text ?: "",
            cursor.left - startOffset,
            cursor.right - startOffset,
            startOffset
        )
    }

    override fun setImeConsumesInput(imeConsumesInput: Boolean): Boolean {
        if (connectionInvalid) {
            return false
        }
        this.imeConsumingInput = imeConsumesInput
        editor.invalidate()
        return true
    }

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean {
        if (connectionInvalid) {
            return false
        }
        editor.dispatchEvent(ImePrivateCommandEvent(editor, action ?: "", data))
        return true
    }

    override fun replaceText(
        start: Int,
        end: Int,
        text: CharSequence,
        newCursorPosition: Int,
        textAttribute: TextAttribute?
    ): Boolean {
        if (DEBUG) {
            logger.d("replaceText, st = $start, ed = $end, text = $text, nCurPos = $newCursorPosition")
        }
        val length = editor.text.length
        if (start < 0 || end < 0 || start > end || start > length || end > length) {
            return false
        }
        beginBatchEdit()
        finishComposingText()
        setSelection(start, end)
        commitText(text, newCursorPosition)
        endBatchEdit()
        return true
    }

    @RequiresApi(33)
    override fun takeSnapshot(): TextSnapshot? {
        var composingStart = -1
        var composingEnd = -1
        if (composingText.isComposing()) {
            composingStart = composingText.startIndex
            composingEnd = composingText.endIndex
        }

        val surroundingText = getSurroundingText(
            MEMORY_EFFICIENT_TEXT_LENGTH / 2,
            MEMORY_EFFICIENT_TEXT_LENGTH / 2,
            GET_TEXT_WITH_STYLES
        )
        if (surroundingText == null) {
            return null
        }

        val cursorCapsMode = getCursorCapsMode(
            (TextUtils.CAP_MODE_CHARACTERS
                    or TextUtils.CAP_MODE_WORDS or TextUtils.CAP_MODE_SENTENCES)
        )

        return TextSnapshot(surroundingText, composingStart, composingEnd, cursorCapsMode)
    }

    companion object {
        private val logger = Logger.instance("EditorInputConnection")

        /**
         * Memory efficient text length from Android [EditorInfo]
         */
        private const val MEMORY_EFFICIENT_TEXT_LENGTH = 2048

        var DEBUG: Boolean = false
    }
}

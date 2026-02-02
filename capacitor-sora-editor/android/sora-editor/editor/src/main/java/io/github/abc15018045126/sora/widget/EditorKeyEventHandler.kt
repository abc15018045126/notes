package io.github.abc15018045126.sora.widget

import android.util.Log
import android.view.KeyCharacterMap
import android.view.KeyEvent
import androidx.annotation.NonNull
import io.github.abc15018045126.sora.event.EditorKeyEvent
import io.github.abc15018045126.sora.event.InterceptTarget
import io.github.abc15018045126.sora.event.KeyBindingEvent
import io.github.abc15018045126.sora.event.SelectionChangeEvent
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.Cursor
import io.github.abc15018045126.sora.text.method.KeyMetaStates
import java.util.Objects

/**
 * Handles [KeyEvent]s in editor.
 *
 * **This is for internal use only.**
 *
 * @author Rose
 * @author Akash Yadav
 */
class EditorKeyEventHandler(private val editor: CodeEditor) {

    private val keyMetaStates: KeyMetaStates = KeyMetaStates(editor)

    init {
        Objects.requireNonNull(editor, "Cannot setup KeyEvent with null editor instance.")
    }

    /**
     * Check if the given [KeyEvent] is a key binding event.
     * [EditorKeyEventHandler.getKeyMetaStates()] must be notified about the key event before this
     * method is called.
     *
     * @param keyCode The keycode.
     * @param event   The key event.
     * @return `true` if the event is a key binding event. `false` otherwise.
     */
    private fun isKeyBindingEvent(keyCode: Int, event: KeyEvent): Boolean {
        // These keys must be pressed for the key event to be a key binding event
        if (!(keyMetaStates.isShiftPressed || keyMetaStates.isAltPressed || event.isCtrlPressed)) {
            return false
        }

        // Any alphabet key
        if (keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
            return true
        }

        // Other key combinations
        return keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_MOVE_HOME ||
                keyCode == KeyEvent.KEYCODE_MOVE_END
    }

    /**
     * Get the [KeyMetaStates] instance.
     *
     * @return The [KeyMetaStates] instance.
     */
    @NonNull
    fun getKeyMetaStates(): KeyMetaStates {
        return keyMetaStates
    }

    /**
     * Called by editor in [CodeEditor.onKeyDown].
     *
     * @param keyCode The key code.
     * @param event   The key event.
     * @return `true` if the event was handled, `false` otherwise.
     */
    fun onKeyDown(keyCode: Int, @NonNull event: KeyEvent): Boolean {
        keyMetaStates.onKeyDown(event)
        val eventManager = editor.eventManager!!

        val editorKeyEvent = EditorKeyEvent(editor, event, EditorKeyEvent.Type.DOWN)
        val keybindingEvent = KeyBindingEvent(
            editor,
            event,
            EditorKeyEvent.Type.DOWN,
            editor.canHandleKeyBinding(
                keyCode,
                event.isCtrlPressed,
                keyMetaStates.isShiftPressed,
                keyMetaStates.isAltPressed
            )
        )
        if ((eventManager.dispatchEvent(editorKeyEvent) and InterceptTarget.TARGET_EDITOR) != 0) {
            return editorKeyEvent.result(false)
        }

        val isShiftPressed = keyMetaStates.isShiftPressed
        val isAltPressed = keyMetaStates.isAltPressed
        val isCtrlPressed = event.isCtrlPressed

        // Currently, KeyBindingEvent is triggered only for (Shift | Ctrl | Alt) + alphabet keys
        // Should we add support for more keys?
        if (isKeyBindingEvent(keyCode, event)) {
            if ((eventManager.dispatchEvent(keybindingEvent) and InterceptTarget.TARGET_EDITOR) != 0) {
                return keybindingEvent.result(false) || editorKeyEvent.result(false)
            }
        }

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_MOVE_HOME,
            KeyEvent.KEYCODE_MOVE_END -> keyMetaStates.adjustAfterKeyPress()
        }

        val result = handleKeyEvent(
            event,
            editorKeyEvent,
            keybindingEvent,
            keyCode,
            isShiftPressed,
            isAltPressed,
            isCtrlPressed
        )
        if (result != null) {
            return editorKeyEvent.result(result)
        }

        return editorKeyEvent.result(editor.onSuperKeyDown(keyCode, event))
    }

    private fun handleKeyEvent(
        event: KeyEvent,
        editorKeyEvent: EditorKeyEvent,
        keybindingEvent: KeyBindingEvent,
        keyCode: Int,
        isShiftPressed: Boolean,
        isAltPressed: Boolean,
        isCtrlPressed: Boolean
    ): Boolean? {
        val connection = editor.inputConnection
        val editorCursor = editor.cursor
        val editorText = editor.text
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (editorCursor!!.isSelected()) {
                    editor.setSelection(editorCursor!!.leftLine, editorCursor!!.leftColumn)
                    return true
                }
                if (editor.isInLongSelect) {
                    editor.endLongSelect()
                    return true
                }
                return false
            }
            KeyEvent.KEYCODE_DEL -> {
                if (editor.isEditable) {
                    if (editor.isTextSelected) {
                        editor.deleteText()
                    } else {
                        if (isCtrlPressed) {
                            editor.extendSelection(SelectionMovement.PREVIOUS_WORD_BOUNDARY)
                            if (editor.isTextSelected) {
                                editor.deleteText()
                            }
                        } else {
                            editor.deleteText()
                        }
                    }
                    editor.notifyIMEExternalCursorChange()
                }
                return true
            }
            KeyEvent.KEYCODE_FORWARD_DEL -> {
                if (editor.isEditable) {
                    if (editor.isTextSelected) {
                        editor.deleteText()
                    } else {
                        if (isCtrlPressed) {
                            editor.extendSelection(SelectionMovement.NEXT_WORD_BOUNDARY)
                            if (editor.isTextSelected) {
                                editor.deleteText()
                            }
                        } else {
                            connection!!.deleteSurroundingText(0, 1)
                        }
                    }
                    editor.notifyIMEExternalCursorChange()
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER -> {
                return handleEnterKeyEvent(
                    editorKeyEvent,
                    keybindingEvent,
                    isShiftPressed,
                    isAltPressed,
                    isCtrlPressed
                )
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isCtrlPressed) {
                    if (isShiftPressed) {
                        val left = editorCursor!!.left()
                        val right = editorCursor!!.right()
                        val lines = editorText.lineCount
                        if (right.line == lines - 1) {
                            // last line, cannot move down
                            return true
                        }

                        val next = editorText.getLine(right.line + 1).toString()
                        editorText.beginBatchEdit()
                        editorText.delete(
                            right.line,
                            editorText.getColumnCount(right.line),
                            right.line + 1,
                            next.length
                        )
                        editorText.insert(left.line, 0, next + editor.lineSeparator!!.content)
                        editorText.endBatchEdit()

                        // Update selection
                        val newLeft = editorText.indexer.getCharPosition(left.line + 1, left.column)
                        val newRight = editorText.indexer.getCharPosition(right.line + 1, right.column)
                        if (left.index != right.index) {
                            val backupAnchor = editor.selectionAnchor
                            editor.setSelectionRegion(
                                newLeft.line,
                                newLeft.column,
                                newRight.line,
                                newRight.column
                            )
                            if (backupAnchor != null) {
                                if (backupAnchor == left) {
                                    editor.selectionAnchor = newLeft
                                } else {
                                    editor.selectionAnchor = newRight
                                }
                            }
                        } else {
                            editor.setSelection(newLeft.line, newLeft.column)
                        }

                        return true
                    }
                    editor.touchHandler!!.scrollBy(0f, editor.rowHeight.toFloat())
                    return true
                }
                editor.moveOrExtendSelection(SelectionMovement.DOWN, isShiftPressed)
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isCtrlPressed) {
                    if (isShiftPressed) {
                        val left = editorCursor!!.left()
                        val right = editorCursor!!.right()
                        if (left.line == 0) {
                            // first line, cannot move up
                            return true
                        }

                        val prev = editorText.getLine(left.line - 1).toString()
                        editorText.beginBatchEdit()
                        editorText.delete(left.line - 1, 0, left.line, 0)
                        editorText.insert(
                            right.line - 1,
                            editorText.getColumnCount(right.line - 1),
                            editor.lineSeparator!!.content + prev
                        )
                        editorText.endBatchEdit()

                        // Update selection
                        val newLeft = editorText.indexer.getCharPosition(left.line - 1, left.column)
                        val newRight = editorText.indexer.getCharPosition(right.line - 1, right.column)
                        if (left.index != right.index) {
                            val backupAnchor = editor.selectionAnchor
                            editor.setSelectionRegion(
                                newLeft.line,
                                newLeft.column,
                                newRight.line,
                                newRight.column
                            )
                            if (backupAnchor != null) {
                                if (backupAnchor == left) {
                                    editor.selectionAnchor = newLeft
                                } else {
                                    editor.selectionAnchor = newRight
                                }
                            }
                        } else {
                            editor.setSelection(newLeft.line, newLeft.column)
                        }

                        return true
                    }
                    editor.touchHandler!!.scrollBy(0f, -editor.rowHeight.toFloat())
                    return true
                }
                editor.moveOrExtendSelection(SelectionMovement.UP, isShiftPressed)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.PREVIOUS_WORD_BOUNDARY, isShiftPressed)
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.LEFT, isShiftPressed)
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.NEXT_WORD_BOUNDARY, isShiftPressed)
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.RIGHT, isShiftPressed)
                }
                return true
            }
            KeyEvent.KEYCODE_MOVE_END -> {
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.TEXT_END, isShiftPressed)
                } else {
                    val movement = if (editor.props!!.rowBasedHomeEnd) SelectionMovement.ROW_END else SelectionMovement.LINE_END
                    editor.moveOrExtendSelection(movement, isShiftPressed)
                }
                return true
            }
            KeyEvent.KEYCODE_MOVE_HOME -> {
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.TEXT_START, isShiftPressed)
                } else {
                    val movement = if (editor.props!!.rowBasedHomeEnd) SelectionMovement.ROW_START else SelectionMovement.LINE_START
                    editor.moveOrExtendSelection(movement, isShiftPressed)
                }
                return true
            }
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.PAGE_BOTTOM, isShiftPressed)
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.PAGE_DOWN, isShiftPressed)
                }
                return true
            }
            KeyEvent.KEYCODE_PAGE_UP -> {
                if (isCtrlPressed) {
                    editor.moveOrExtendSelection(SelectionMovement.PAGE_TOP, isShiftPressed)
                } else {
                    editor.moveOrExtendSelection(SelectionMovement.PAGE_UP, isShiftPressed)
                }
                return true
            }
            KeyEvent.KEYCODE_TAB -> {
                if (editor.isEditable) {
                    if (!isAltPressed && !isCtrlPressed) {
                        if (editor.snippetController?.isInSnippet() == true) {
                            if (isShiftPressed) {
                                editor.snippetController?.shiftToPreviousTabStop()
                            } else {
                                editor.snippetController?.shiftToNextTabStop()
                            }
                        } else {
                            if (isShiftPressed) {
                                // Shift + TAB -> unindent the [selected] lines
                                editor.unindentSelection()
                            } else {
                                editor.indentOrCommitTab()
                            }
                        }
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_PASTE -> {
                if (editor.isEditable) {
                    editor.pasteText()
                }
                return true
            }
            KeyEvent.KEYCODE_COPY -> {
                editor.copyText()
                return true
            }
            KeyEvent.KEYCODE_SPACE -> {
                if (editor.isEditable) {
                    editor.commitText(" ")
                    editor.notifyIMEExternalCursorChange()
                }
                return true
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                if (editorCursor!!.isSelected()) {
                    val newPosition = if (editor.props!!.positionOfCursorWhenExitSelecting) editorCursor!!.right() else editorCursor!!.left()
                    editor.setSelection(newPosition.line, newPosition.column, true)
                }
                return true
            }
            else -> {
                if (event.isCtrlPressed && !event.isAltPressed) {
                    return handleCtrlKeyBinding(editorKeyEvent, keybindingEvent, keyCode, isShiftPressed)
                }
            }
        }
        val printingResult = handlePrintingKey(event, keyCode)
        if (printingResult) {
            keyMetaStates.adjustAfterKeyPress()
        }
        return printingResult
    }

    private fun handlePrintingKey(event: KeyEvent, keyCode: Int): Boolean {
        val editorText = editor.text
        val editorCursor = editor.cursor
        var charCode = event.getUnicodeChar(keyMetaStates.getMetaState(event))
        if (charCode != 0 && editor.isEditable) {
            if (charCode == KeyCharacterMap.HEX_INPUT.toInt() || charCode == KeyCharacterMap.PICKER_DIALOG_INPUT.toInt()) {
                // unsupported: character picker dialog and hex input
                return editor.onSuperKeyDown(keyCode, event)
            }
            // #547 Dead Keys
            var dead = false
            if ((charCode and KeyCharacterMap.COMBINING_ACCENT) != 0) {
                charCode = charCode and KeyCharacterMap.COMBINING_ACCENT_MASK
                dead = true
            }

            if (editorCursor!!.left + 1 == editorCursor!!.right) {
                val base = editorText[editorCursor!!.left]
                val composed = KeyCharacterMap.getDeadChar(base.toInt(), charCode)
                if (composed != base.toInt() && event.repeatCount == 0) {
                    charCode = composed
                    dead = false
                }
            }

            if (dead) {
                val cursor = editor.cursor
                if (!editor.isTextSelected || (editorCursor!!.left + 1 == editorCursor!!.right && editorText[editorCursor!!.left].toInt() == charCode)) {
                    editor.setSelection(
                        editorCursor!!.rightLine,
                        editorCursor!!.rightColumn,
                        SelectionChangeEvent.CAUSE_DEAD_KEYS
                    )
                    editor.commitText(String(Character.toChars(charCode)))
                    val c = cursor!!
                    val charCount = Character.charCount(
                        Character.codePointBefore(
                            editor.text.getLine(c.rightLine),
                            c.rightColumn
                        )
                    )
                    editor.setSelectionRegion(
                        c.rightLine,
                        c.rightColumn - charCount,
                        c.rightLine,
                        c.rightColumn,
                        SelectionChangeEvent.CAUSE_DEAD_KEYS
                    )
                }
                return true
            }

            val text = String(Character.toChars(charCode))

            editor.commitText(text)
            editor.notifyIMEExternalCursorChange()
            return true
        } else {
            return editor.onSuperKeyDown(keyCode, event)
        }
    }

    private fun handleCtrlKeyBinding(
        e: EditorKeyEvent,
        keybindingEvent: KeyBindingEvent,
        keyCode: Int,
        isShiftPressed: Boolean
    ): Boolean? {
        val connection = editor.inputConnection
        val editorText = editor.text
        val editorCursor = editor.cursor
        var editorResult = true
        when (keyCode) {
            KeyEvent.KEYCODE_V -> if (editor.isEditable) {
                editor.pasteText()
            }
            KeyEvent.KEYCODE_C -> editor.copyText()
            KeyEvent.KEYCODE_X -> if (editor.isEditable) {
                editor.cutText()
            } else {
                editor.copyText()
            }
            KeyEvent.KEYCODE_A -> editor.selectAll()
            KeyEvent.KEYCODE_Z -> if (editor.isEditable) {
                editor.undo()
            }
            KeyEvent.KEYCODE_Y -> if (editor.isEditable) {
                editor.redo()
            }
            KeyEvent.KEYCODE_D -> if (editor.isEditable) {
                editor.duplicateLine()
            }
            KeyEvent.KEYCODE_W -> editor.selectCurrentWord()
            KeyEvent.KEYCODE_J -> {
                if (!isShiftPressed || editorCursor!!.isSelected()) {
                    // TODO If the cursor is selected, then the selected lines must be joined.
                    editorResult = false
                } else {
                    val line = editorCursor!!.leftLine
                    editor.setSelection(line, editorText.getColumnCount(line))
                    connection!!.deleteSurroundingText(0, 1)
                    editor.ensureSelectionVisible()
                }
            }
            else -> return null
        }
        return keybindingEvent.result(editorResult) || e.result(editorResult)
    }

    private fun handleEnterKeyEvent(
        editorKeyEvent: EditorKeyEvent,
        keybindingEvent: KeyBindingEvent,
        isShiftPressed: Boolean,
        isAltPressed: Boolean,
        isCtrlPressed: Boolean
    ): Boolean {
        val editorCursor = editor.cursor
        val editorText = editor.text
        if (editor.isEditable) {
            val lineSeparator = editor.lineSeparator!!.content
            val editorLanguage = editor.editorLanguage!!

            if (isShiftPressed && !isAltPressed && !isCtrlPressed) {
                // Shift + Enter
                return startNewLine(editor, editorCursor!!, editorText, editorKeyEvent, keybindingEvent)
            }

            if (isCtrlPressed && !isShiftPressed) {
                if (isAltPressed) {
                    // Ctrl + Alt + Enter
                    var line = editorCursor!!.left().line
                    if (line == 0) {
                        editorText.insert(0, 0, lineSeparator)
                        editor.setSelection(0, 0)
                        editor.ensureSelectionVisible()
                        return keybindingEvent.result(true) || editorKeyEvent.result(true)
                    } else {
                        line--
                        editor.setSelection(line, editorText.getColumnCount(line))
                        return startNewLine(editor, editorCursor!!, editorText, editorKeyEvent, keybindingEvent)
                    }
                }

                // Ctrl + Enter
                val left = editorCursor!!.left().fromThis()
                editor.commitText(lineSeparator)
                editor.setSelection(left.line, left.column)
                editor.ensureSelectionVisible()
                return keybindingEvent.result(true) || editorKeyEvent.result(true)
            }

            val handlers = editorLanguage.newlineHandlers
            if (handlers == null || editorCursor!!.isSelected()) {
                editor.commitText(lineSeparator, true)
            } else {
                var consumed = false
                for (handler in handlers) {
                    if (handler != null) {
                        if (handler.matchesRequirement(editorText, editorCursor!!.left(), editor.styles!!)) {
                            try {
                                val result = handler.handleNewline(
                                    editorText,
                                    editorCursor!!.left(),
                                    editor.styles!!,
                                    editor.tabWidth
                                )
                                editor.commitText(result.text, false)
                                val delta = result.shiftLeft
                                if (delta != 0) {
                                    val newSel = (editorCursor!!.left - delta).coerceAtLeast(0)
                                    val charPosition = editorText.getIndexer().getCharPosition(newSel)
                                    editor.setSelection(charPosition.line, charPosition.column)
                                }
                                consumed = true
                            } catch (ex: Exception) {
                                Log.w(TAG, "Error occurred while calling Language's NewlineHandler", ex)
                            }
                            break
                        }
                    }
                }
                if (!consumed) {
                    editor.commitText(lineSeparator, true)
                }
            }
            editor.notifyIMEExternalCursorChange()
        }
        return editorKeyEvent.result(true)
    }

    private fun startNewLine(
        editor: CodeEditor,
        editorCursor: Cursor,
        editorText: Content,
        e: EditorKeyEvent,
        keybindingEvent: KeyBindingEvent
    ): Boolean {
        val line = editorCursor!!.right().line
        editor.setSelection(line, editorText.getColumnCount(line))
        editor.commitText(editor.lineSeparator!!.content)
        editor.ensureSelectionVisible()
        return keybindingEvent.result(true) || e.result(true)
    }

    /**
     * Called by editor in [CodeEditor.onKeyUp].
     *
     * @param keyCode The key code.
     * @param event   The key event.
     * @return `true` if the event was handled, `false` otherwise.
     */
    fun onKeyUp(keyCode: Int, @NonNull event: KeyEvent): Boolean {
        keyMetaStates.onKeyUp(event)

        val eventManager = editor.eventManager!!

        val e = EditorKeyEvent(editor, event, EditorKeyEvent.Type.UP)
        if ((eventManager.dispatchEvent(e) and InterceptTarget.TARGET_EDITOR) != 0) {
            return e.result(false)
        }

        if (isKeyBindingEvent(keyCode, event)) {
            val keybindingEvent = KeyBindingEvent(
                editor,
                event,
                EditorKeyEvent.Type.UP,
                editor.canHandleKeyBinding(
                    keyCode,
                    event.isCtrlPressed,
                    keyMetaStates.isShiftPressed,
                    keyMetaStates.isAltPressed
                )
            )

            if ((eventManager.dispatchEvent(keybindingEvent) and InterceptTarget.TARGET_EDITOR) != 0) {
                return keybindingEvent.result(false) || e.result(false)
            }
        }

        return e.result(editor.onSuperKeyUp(keyCode, event))
    }

    /**
     * Called by editor in [CodeEditor.onKeyMultiple].
     *
     * @param keyCode     The key code.
     * @param repeatCount The repeat count.
     * @param event       The key event.
     * @return `true` if the event was handled, `false` otherwise.
     */
    fun onKeyMultiple(keyCode: Int, repeatCount: Int, @NonNull event: KeyEvent): Boolean {
        val e = EditorKeyEvent(editor, event, EditorKeyEvent.Type.MULTIPLE)
        val eventManager = editor.eventManager!!
        if ((eventManager.dispatchEvent(e) and InterceptTarget.TARGET_EDITOR) != 0) {
            return e.result(false)
        }

        return e.result(editor.onSuperKeyMultiple(keyCode, repeatCount, event))
    }

    companion object {
        private const val TAG = "EditorKeyEventHandler"
    }
}

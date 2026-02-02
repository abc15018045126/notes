package io.github.abc15018045126.sora.event

import android.view.KeyEvent
import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * Receives key related events in editor.
 * <p>
 * You may set a boolean for editor to return to the Android KeyEvent framework.
 *
 * @author abc15018045126
 * @see ResultedEvent.result
 * <p>
 * This class mirrors methods of {@link KeyEvent}, but some methods are changed:
 * @see #isAltPressed()
 * @see #isShiftPressed()
 */
open class EditorKeyEvent(
    editor: CodeEditor,
    private val src: KeyEvent,
    val eventType: Type
) : ResultedEvent<Boolean>(editor) {

    private val shiftPressed: Boolean = editor.keyMetaStates.isShiftPressed
    private val altPressed: Boolean = editor.keyMetaStates.isAltPressed

    override fun canIntercept(): Boolean {
        return true
    }

    val action: Int
        get() = src.action

    val keyCode: Int
        get() = src.keyCode

    val repeatCount: Int
        get() = src.repeatCount

    val metaState: Int
        get() = src.metaState

    val modifiers: Int
        get() = src.modifiers

    val downTime: Long
        get() = src.downTime

    override val eventTime: Long
        get() = src.eventTime

    /**
     * editor change: track shift/alt by {@link KeyMetaStates}
     */
    fun isShiftPressed(): Boolean {
        return shiftPressed
    }

    /**
     * editor change: track shift/alt by {@link KeyMetaStates}
     */
    fun isAltPressed(): Boolean {
        return altPressed
    }

    fun isCtrlPressed(): Boolean {
        return (src.metaState and KeyEvent.META_CTRL_ON) != 0
    }

    fun markAsConsumed() {
        interceptAndSetResult(true)
    }

    fun result(editorResult: Boolean): Boolean {
        val res = result
        val userResult = res ?: false
        return if (isIntercepted()) {
            userResult
        } else {
            userResult || editorResult
        }
    }

    /**
     * The type of {@link EditorKeyEvent}.
     */
    enum class Type {
        /**
         * Used for {@link CodeEditor#onKeyUp(int, KeyEvent)}.
         */
        UP,

        /**
         * Used for {@link CodeEditor#onKeyDown(int, KeyEvent)}.
         */
        DOWN,

        /**
         * Used for {@link CodeEditor#onKeyMultiple(int, int, KeyEvent)}.
         */
        MULTIPLE
    }
}

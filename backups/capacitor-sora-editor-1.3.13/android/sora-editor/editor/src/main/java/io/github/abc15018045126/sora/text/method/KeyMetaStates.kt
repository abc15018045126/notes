package io.github.abc15018045126.sora.text.method

import android.text.Editable
import android.text.method.MetaKeyKeyListener
import android.view.KeyEvent
import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * Handles key events such as SHIFT
 *
 * @author abc15018045126
 */
class KeyMetaStates(private val editor: CodeEditor) : MetaKeyKeyListener() {

    /**
     * Dummy text used for Android original APIs
     */
    private val dest: Editable = Editable.Factory.getInstance().newEditable("")
    var isCtrlPressed = false
        private set

    fun onKeyDown(event: KeyEvent) {
        super.onKeyDown(editor, dest, event.keyCode, event)
        isCtrlPressed = event.isCtrlPressed
    }

    fun onKeyUp(event: KeyEvent) {
        super.onKeyUp(editor, dest, event.keyCode, event)
        isCtrlPressed = event.isCtrlPressed
    }

    fun getMetaState(event: KeyEvent): Int {
        return getMetaState(dest, event)
    }

    val isShiftPressed: Boolean
        get() = getMetaState(dest, META_SHIFT_ON) == 1

    val isAltPressed: Boolean
        get() = getMetaState(dest, META_ALT_ON) == 1

    val isSymPressed: Boolean
        get() = getMetaState(dest, META_SYM_ON) == 1

    val isSelecting: Boolean
        get() = isShiftPressed && !isAltPressed

    fun adjustAfterKeyPress() {
        adjustMetaAfterKeypress(dest)
    }

    fun clearMetaStates(states: Int) {
        clearMetaKeyState(editor, dest, states)
    }
}

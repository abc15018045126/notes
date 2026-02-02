package io.github.abc15018045126.sora.event

import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * Event with a result
 *
 * @param <T> Result type
 */
abstract class ResultedEvent<T>(editor: CodeEditor) : Event(editor) {

    var result: T? = null

    fun interceptAndSetResult(result: T?) {
        this.result = result
        intercept()
    }

    val isResultSet: Boolean
        get() = result != null
}

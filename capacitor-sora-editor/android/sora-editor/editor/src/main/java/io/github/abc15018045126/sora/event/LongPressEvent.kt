package io.github.abc15018045126.sora.event

import android.view.MotionEvent
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.TextRange
import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * Long press event.
 * <p>
 * This event can be intercepted so that the editor framework will do nothing (such as selecting a word). You can take over the
 * procedure. Note that after intercepting an event, it will not be sent to other listeners, either.
 *
 * @author abc15018045126
 */
class LongPressEvent(
    editor: CodeEditor,
    position: CharPosition,
    event: MotionEvent,
    span: Span?,
    spanRange: TextRange?,
    motionRegion: Int,
    motionBound: Int
) : EditorMotionEvent(editor, position, event, span, spanRange, motionRegion, motionBound)

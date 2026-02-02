package io.github.abc15018045126.sora.event

import android.view.MotionEvent
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.TextRange
import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * Report a single click
 *
 * @author abc15018045126
 */
class ClickEvent(
    editor: CodeEditor,
    position: CharPosition,
    event: MotionEvent,
    span: Span?,
    spanRange: TextRange?,
    motionRegion: Int,
    motionBound: Int
) : EditorMotionEvent(editor, position, event, span, spanRange, motionRegion, motionBound)

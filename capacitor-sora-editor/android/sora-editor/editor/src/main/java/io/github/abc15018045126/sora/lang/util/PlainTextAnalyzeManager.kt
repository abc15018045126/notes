package io.github.abc15018045126.sora.lang.util

import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.text.CharPosition

/**
 * This class generate plain spans for content, in case when
 * using a language without analysis you can still provide context-free completions.
 * By default, editor does not show completions when no span is set
 * on the text. This would be helpful for enabling completion for pure texts.
 *
 * @author abc15018045126
 */
class PlainTextAnalyzeManager : BaseAnalyzeManager() {

    override fun insert(start: CharPosition, end: CharPosition, insertedContent: CharSequence) {}

    override fun delete(start: CharPosition, end: CharPosition, deletedContent: CharSequence) {}

    override fun rerun() {
        val receiver = receiver
        val ref = contentRef
        if (receiver != null && ref != null) {
            val style = Styles()
            style.spans = PlainTextSpans(ref.lineCount)
            receiver.setStyles(this, style)
        } else receiver?.setStyles(this, null)
    }

}

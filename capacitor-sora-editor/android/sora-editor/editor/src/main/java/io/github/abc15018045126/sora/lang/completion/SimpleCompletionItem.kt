package io.github.abc15018045126.sora.lang.completion

import android.graphics.drawable.Drawable
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * SimpleCompletionItem represents a simple replace action for auto-completion.
 * [prefixLength] is the length of prefix (text length you want to replace before the
 * auto-completion position).
 * [commitText] is the text you want to replace the original text.
 *
 * Note that you must make sure the start position of replacement is on the same line as auto-completion's
 * required position.
 *
 * @see CompletionItem
 */
open class SimpleCompletionItem : CompletionItem {

    var commitText: String?

    constructor(prefixLength: Int, commitText: String) : this(commitText, prefixLength, commitText)

    constructor(label: CharSequence, prefixLength: Int, commitText: String) : this(label, null, prefixLength, commitText)

    constructor(label: CharSequence, desc: CharSequence?, prefixLength: Int, commitText: String) : this(label, desc, null, prefixLength, commitText)

    constructor(label: CharSequence, desc: CharSequence?, icon: Drawable?, prefixLength: Int, commitText: String) : super(label, desc, icon) {
        this.commitText = commitText
        this.prefixLength = prefixLength
    }

    fun desc(desc: CharSequence?): SimpleCompletionItem {
        if (desc != null) {
            super.desc(desc)
        }
        return this
    }

    fun icon(icon: Drawable?): SimpleCompletionItem {
        if (icon != null) {
            super.icon(icon)
        }
        return this
    }

    override fun label(label: CharSequence): SimpleCompletionItem {
        super.label(label)
        return this
    }

    override fun kind(kind: CompletionItemKind): SimpleCompletionItem {
        super.kind(kind)
        if (this.icon == null) {
            this.icon = SimpleCompletionIconDrawer.draw(kind)
        }
        return this
    }

    fun commit(prefixLength: Int, commitText: String): SimpleCompletionItem {
        this.prefixLength = prefixLength
        this.commitText = commitText
        return this
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        val commit = commitText
        if (commit == null) {
            return
        }
        if (prefixLength == 0) {
            text.insert(line, column, commit)
            return
        }
        text.replace(line, column - prefixLength, line, column, commit)
    }
}

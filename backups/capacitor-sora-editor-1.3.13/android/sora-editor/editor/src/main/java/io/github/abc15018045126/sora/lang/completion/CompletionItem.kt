package io.github.abc15018045126.sora.lang.completion

import android.graphics.drawable.Drawable
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.widget.CodeEditor

/**
 * The class used to save auto complete result items.
 * For functionality, this class only manages the information to be displayed in list view.
 * You can implement {@link CompletionItem#performCompletion(CodeEditor, Content, int, int)} or
 * {@link CompletionItem#performCompletion(CodeEditor, Content, CharPosition)} to customize
 * your own completion method so that you can develop complex actions.
 * <p>
 * For the simplest usage, see {@link SimpleCompletionItem}
 *
 * @author abc15018045126
 * @see SimpleCompletionItem
 */
abstract class CompletionItem {

    /**
     * Icon for displaying in adapter
     */
    @JvmField
    var icon: Drawable? = null

    /**
     * Text to display as title in adapter
     */
    @JvmField
    var label: CharSequence? = null

    /**
     * Text to display as description in adapter
     */
    @JvmField
    var desc: CharSequence? = null

    /**
     * The kind of this completion item. Based on the kind
     * an icon is chosen by the editor.
     */
    @JvmField
    var kind: CompletionItemKind? = null

    /**
     * Use for default sort
     */
    @JvmField
    var prefixLength: Int = 0

    /**
     * A string that should be used when comparing this item
     * with other items. When null the {@link #label label}
     * is used.
     */
    @JvmField
    var sortText: String? = null

    /**
     * A string that should be used when comparing this item
     * with other items. When null the {@link #sortText sortText}
     * is used.
     */
    @JvmField
    var filterText: String? = null

    @JvmField
    var extra: Any? = null

    constructor(label: CharSequence) : this(label, null)

    constructor(label: CharSequence, desc: CharSequence?) : this(label, desc, null)

    constructor(label: CharSequence, desc: CharSequence?, icon: Drawable?) {
        this.label = label
        this.desc = desc
        this.icon = icon
    }

    open fun label(label: CharSequence): CompletionItem {
        this.label = label
        return this
    }

    open fun desc(desc: CharSequence): CompletionItem {
        this.desc = desc
        return this
    }

    open fun kind(kind: CompletionItemKind): CompletionItem {
        this.kind = kind
        return this
    }

    open fun icon(icon: Drawable): CompletionItem {
        this.icon = icon
        return this
    }

    /**
     * Perform this completion.
     * You can implement custom logic to make your completion better(by updating selection and text
     * from here).
     * To make it considered as a single action, the editor will enter batch edit state before invoking
     * this method. Feel free to update the text by multiple calls to {@code text}.
     *
     * @param editor   The editor. You can set cursor position with that.
     * @param text     The text in editor. You can make modifications to it.
     * @param position The requested completion position (the one passed to completion thread)
     */
    open fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        performCompletion(editor, text, position.line, position.column)
    }

    /**
     * Perform this completion.
     * You can implement custom logic to make your completion better(by updating selection and text
     * from here).
     * To make it considered as a single action, the editor will enter batch edit state before invoking
     * this method. Feel free to update the text by multiple calls to {@code text}.
     *
     * @param editor The editor. You can set cursor position with that.
     * @param text   The text in editor. You can make modifications to it.
     * @param line   The auto-completion line
     * @param column The auto-completion column
     * @see #performCompletion(CodeEditor, Content, CharPosition) Editor calls this method to do completion
     */
    abstract fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int)
}

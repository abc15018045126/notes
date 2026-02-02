package io.github.abc15018045126.sora.widget.component

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import io.github.abc15018045126.sora.lang.completion.CompletionItem
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

/**
 * A class to make custom adapter for auto-completion window
 *
 * @see EditorCompletionAdapter.getItemHeight
 * @see EditorCompletionAdapter.getView
 */
abstract class EditorCompletionAdapter : BaseAdapter() {

    protected var window: EditorAutoCompletion? = null
    var items: List<CompletionItem>? = null

    /**
     * Called by [EditorAutoCompletion] to attach some arguments
     */
    fun attachValues(window: EditorAutoCompletion, items: List<CompletionItem>) {
        this.window = window
        this.items = items
    }

    override fun getItem(position: Int): CompletionItem {
        return items!![position]
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).hashCode().toLong()
    }

    override fun getCount(): Int {
        return items?.size ?: 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent, position == window?.currentPosition)
    }

    /**
     * Get color scheme in editor
     */
    protected fun getColorScheme(): EditorColorScheme {
        return window!!.editor.colorScheme
    }

    /**
     * Get theme color from current color scheme
     *
     * @param type Type of color. Refer to [EditorColorScheme]
     * @see EditorColorScheme.getColor
     */
    protected fun getThemeColor(type: Int): Int {
        return getColorScheme().getColor(type)
    }

    /**
     * Get context from editor
     */
    protected fun getContext(): Context {
        return window!!.editor.context
    }

    /**
     * Implementation of this class should provide exact height of its item
     *
     * The value will be used to calculate the height of completion window
     */
    abstract fun getItemHeight(): Int

    /**
     * @param isCurrentCursorPosition Is the [position] currently selected
     * @see BaseAdapter.getView
     */
    protected abstract fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
        isCurrentCursorPosition: Boolean
    ): View

}

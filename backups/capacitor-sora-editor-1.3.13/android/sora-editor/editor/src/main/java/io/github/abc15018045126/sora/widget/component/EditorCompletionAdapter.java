
package io.github.abc15018045126.sora.widget.component;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;

import java.util.List;

import io.github.abc15018045126.sora.lang.completion.CompletionItem;
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme;

/**
 * A class to make custom adapter for auto-completion window
 *
 * @see EditorCompletionAdapter#getItemHeight()
 * @see EditorCompletionAdapter#getView(int, View, ViewGroup, boolean)
 */
public abstract class EditorCompletionAdapter extends BaseAdapter implements Adapter {

    private EditorAutoCompletion window;
    private List<CompletionItem> items;

    /**
     * Called by {@link EditorAutoCompletion} to attach some arguments
     */
    public void attachValues(@NonNull EditorAutoCompletion window, @NonNull List<CompletionItem> items) {
        this.window = window;
        this.items = items;
    }

    @Override
    public CompletionItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public int getCount() {
        return items == null ? 0 : items.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent, position == window.getCurrentPosition());
    }

    /**
     * Get color scheme in editor
     */
    @NonNull
    protected EditorColorScheme getColorScheme() {
        return window.getEditor().getColorScheme();
    }

    /**
     * Get theme color from current color scheme
     *
     * @param type Type of color. Refer to {@link EditorColorScheme}
     * @see EditorColorScheme#getColor(int)
     */
    protected int getThemeColor(int type) {
        return getColorScheme().getColor(type);
    }

    /**
     * Get context from editor
     */
    @NonNull
    protected Context getContext() {
        return window.getContext();
    }

    /**
     * Implementation of this class should provide exact height of its item
     * <p>
     * The value will be used to calculate the height of completion window
     */
    public abstract int getItemHeight();

    /**
     * @param isCurrentCursorPosition Is the {@param position} currently selected
     * @see BaseAdapter#getView(int, View, ViewGroup)
     */
    protected abstract View getView(int position, View convertView, ViewGroup parent, boolean isCurrentCursorPosition);

}


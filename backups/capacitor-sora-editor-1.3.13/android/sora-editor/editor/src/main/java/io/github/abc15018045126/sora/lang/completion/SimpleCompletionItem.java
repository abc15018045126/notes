
package io.github.abc15018045126.sora.lang.completion;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.text.Content;
import io.github.abc15018045126.sora.widget.CodeEditor;

/**
 * SimpleCompletionItem represents a simple replace action for auto-completion.
 * {@code prefixLength} is the length of prefix (text length you want to replace before the
 * auto-completion position).
 * {@code commitText} is the text you want to replace the original text.
 * <p>
 * Note that you must make sure the start position of replacement is on the same line as auto-completion's
 * required position.
 *
 * @see CompletionItem
 */
public class SimpleCompletionItem extends CompletionItem {

    public String commitText;

    public SimpleCompletionItem(int prefixLength, String commitText) {
        this(commitText, prefixLength, commitText);
    }

    public SimpleCompletionItem(CharSequence label, int prefixLength, String commitText) {
        this(label, null, prefixLength, commitText);
    }

    public SimpleCompletionItem(CharSequence label, CharSequence desc, int prefixLength, String commitText) {
        this(label, desc, null, prefixLength, commitText);
    }

    public SimpleCompletionItem(CharSequence label, CharSequence desc, Drawable icon, int prefixLength, String commitText) {
        super(label, desc, icon);
        this.commitText = commitText;
        this.prefixLength = prefixLength;
    }

    @Override
    public SimpleCompletionItem desc(CharSequence desc) {
        super.desc(desc);
        return this;
    }

    @Override
    public SimpleCompletionItem icon(Drawable icon) {
        super.icon(icon);
        return this;
    }

    @Override
    public SimpleCompletionItem label(CharSequence label) {
        super.label(label);
        return this;
    }

    @Override
    public SimpleCompletionItem kind(CompletionItemKind kind) {
        super.kind(kind);
        if (this.icon == null) {
            icon = SimpleCompletionIconDrawer.draw(kind);
        }
        return this;
    }

    public SimpleCompletionItem commit(int prefixLength, String commitText) {
        this.prefixLength = prefixLength;
        this.commitText = commitText;
        return this;
    }

    @Override
    public void performCompletion(@NonNull CodeEditor editor, @NonNull Content text, int line, int column) {
        if (commitText == null) {
            return;
        }
        if (prefixLength == 0) {
            text.insert(line, column, commitText);
            return;
        }
        text.replace(line, column - prefixLength, line, column, commitText);
    }

}



package io.github.abc15018045126.sora.lang.completion;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.text.CharPosition;
import io.github.abc15018045126.sora.text.Content;
import io.github.abc15018045126.sora.widget.CodeEditor;

public class SimpleSnippetCompletionItem extends CompletionItem {

    private final SnippetDescription snippet;

    public SimpleSnippetCompletionItem(CharSequence label, SnippetDescription snippet) {
        this(label, null, snippet);
    }

    public SimpleSnippetCompletionItem(CharSequence label, CharSequence desc, SnippetDescription snippet) {
        this(label, desc, null, snippet);
    }

    public SimpleSnippetCompletionItem(CharSequence label, CharSequence desc, Drawable icon, SnippetDescription snippet) {
        super(label, desc, icon);
        this.snippet = snippet;
        kind(CompletionItemKind.Snippet);
    }



    @Override
    public void performCompletion(@NonNull CodeEditor editor, @NonNull Content text, @NonNull CharPosition position) {
        int prefixLength = snippet.getSelectedLength();
        var selectedText = text.subSequence(position.index - prefixLength, position.index).toString();
        int actionIndex = position.index;
        if (snippet.getDeleteSelected()) {
            text.delete(position.index - prefixLength, position.index);
            actionIndex -= prefixLength;
        }
        editor.getSnippetController().startSnippet(actionIndex, snippet.getSnippet(), selectedText);
    }

    @Override
    public void performCompletion(@NonNull CodeEditor editor, @NonNull Content text, int line, int column) {
        // do nothing
    }
}


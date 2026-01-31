package io.github.abc15018045126.sora.lang.completion

import android.graphics.drawable.Drawable
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.widget.CodeEditor

class SimpleSnippetCompletionItem : CompletionItem {

    private val snippet: SnippetDescription

    constructor(label: CharSequence, snippet: SnippetDescription) : this(label, null, snippet)

    constructor(label: CharSequence, desc: CharSequence?, snippet: SnippetDescription) : this(label, desc, null, snippet)

    constructor(label: CharSequence, desc: CharSequence?, icon: Drawable?, snippet: SnippetDescription) : super(label, desc, icon) {
        this.snippet = snippet
        kind(CompletionItemKind.Snippet)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        val prefixLength = snippet.selectedLength
        val selectedText = text.subSequence(position.index - prefixLength, position.index).toString()
        var actionIndex = position.index
        if (snippet.deleteSelected) {
            text.delete(position.index - prefixLength, position.index)
            actionIndex -= prefixLength
        }
        editor.snippetController.startSnippet(actionIndex, snippet.snippet, selectedText)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        // do nothing
    }
}

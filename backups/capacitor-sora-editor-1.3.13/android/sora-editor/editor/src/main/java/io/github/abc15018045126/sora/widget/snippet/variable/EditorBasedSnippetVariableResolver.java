
package io.github.abc15018045126.sora.widget.snippet.variable;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.text.ICUUtils;
import io.github.abc15018045126.sora.util.IntPair;
import io.github.abc15018045126.sora.widget.CodeEditor;

public class EditorBasedSnippetVariableResolver implements ISnippetVariableResolver {

    private final CodeEditor editor;

    public EditorBasedSnippetVariableResolver(@NonNull CodeEditor editor) {
        this.editor = editor;
    }

    @NonNull
    @Override
    public String[] getResolvableNames() {
        return new String[]{
                "TM_CURRENT_LINE", "TM_LINE_INDEX", "TM_LINE_NUMBER", "CURSOR_INDEX", "CURSOR_NUMBER",
                "TM_CURRENT_WORD", "SELECTION", "TM_SELECTED_TEXT"
        };
    }

    @NonNull
    @Override
    public String resolve(@NonNull String name) {
        switch (name) {
            case "TM_CURRENT_LINE":
            case "TM_LINE_NUMBER":
                return Integer.toString(editor.getCursor().getLeftLine() + 1);
            case "TM_LINE_INDEX":
                return Integer.toString(editor.getCursor().getLeftLine());
            case "CURSOR_INDEX":
                return Integer.toString(editor.getCursor().getLeft());
            case "CURSOR_NUMBER":
                return Integer.toString(editor.getCursor().getLeft() + 1);
            case "TM_CURRENT_WORD": {
                var text = editor.getText();
                var res = ICUUtils.getWordRange(text.getLine(text.getCursor().getLeftLine()), text.getCursor().getLeftColumn(), true);
                return text.getLine(text.getCursor().getLeftLine()).subSequence(IntPair.getFirst(res), IntPair.getSecond(res)).toString();
            }
            case "SELECTION":
            case "TM_SELECTED_TEXT": {
                var cursor = editor.getCursor();
                return editor.getText().subSequence(cursor.getLeft(), cursor.getRight()).toString();
            }
        }
        throw new IllegalArgumentException("Unsupported variable name:" + name);
    }
}


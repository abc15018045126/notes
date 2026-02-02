
package io.github.abc15018045126.sora.widget;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import io.github.abc15018045126.sora.event.SelectionChangeEvent;
import io.github.abc15018045126.sora.lang.analysis.AnalyzeManager;
import io.github.abc15018045126.sora.lang.analysis.StyleReceiver;
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange;
import io.github.abc15018045126.sora.lang.brackets.BracketsProvider;
import io.github.abc15018045126.sora.lang.brackets.PairedBracket;
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticsContainer;
import io.github.abc15018045126.sora.lang.styling.Styles;

public class EditorStyleDelegate implements StyleReceiver {

    private final WeakReference<CodeEditor> editorRef;
    private PairedBracket foundPair;
    private BracketsProvider bracketsProvider;

    EditorStyleDelegate(@NonNull CodeEditor editor) {
        editorRef = new WeakReference<>(editor);
        editor.subscribeEvent(SelectionChangeEvent.class, (event, __) -> {
            if (!event.isSelected()) {
                postUpdateBracketPair();
            }
        });
    }

    void onTextChange() {
        //  Should we do this?
        //bracketsProvider = null;
        //foundPair = null;
    }

    void postUpdateBracketPair() {
        runOnUiThread(() -> {
            final var provider = bracketsProvider;
            final var editor = editorRef.get();
            if (provider != null && editor != null && !editor.getCursor().isSelected() && editor.isHighlightBracketPair()) {
                foundPair = provider.getPairedBracketAt(editor.getText(), editor.getCursor().getLeft());
                editor.invalidate();
            }
        });
    }

    @Nullable
    public PairedBracket getFoundBracketPair() {
        return foundPair;
    }

    void reset() {
        foundPair = null;
        bracketsProvider = null;
    }

    private void runOnUiThread(Runnable operation) {
        var editor = editorRef.get();
        if (editor == null) {
            return;
        }
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            operation.run();
        } else {
            editor.postInLifecycle(operation);
        }
    }

    @Override
    public void setStyles(@NonNull AnalyzeManager sourceManager, @Nullable Styles styles) {
        setStyles(sourceManager, styles, null);
    }

    @Override
    public void setStyles(@NonNull AnalyzeManager sourceManager, @Nullable Styles styles, @Nullable Runnable action) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            runOnUiThread(() -> {
                if (action != null) {
                    action.run();
                }
                editor.setStyles(styles);
            });
        }
    }

    @Override
    public void setDiagnostics(@NonNull AnalyzeManager sourceManager, @Nullable DiagnosticsContainer diagnostics) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            runOnUiThread(() -> editor.setDiagnostics(diagnostics));
        }
    }

    @Override
    public void updateBracketProvider(@NonNull AnalyzeManager sourceManager, @Nullable BracketsProvider provider) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager() && bracketsProvider != provider) {
            this.bracketsProvider = provider;
            postUpdateBracketPair();
        }
    }

    @Override
    public void updateStyles(@NonNull AnalyzeManager sourceManager, @NonNull Styles styles, @NonNull StyleUpdateRange range) {
        var editor = editorRef.get();
        if (editor != null && sourceManager == editor.getEditorLanguage().getAnalyzeManager()) {
            runOnUiThread(() -> editor.updateStyles(styles, range));
        }
    }

    public void clearFoundBracketPair() {
        this.foundPair = null;
    }
}


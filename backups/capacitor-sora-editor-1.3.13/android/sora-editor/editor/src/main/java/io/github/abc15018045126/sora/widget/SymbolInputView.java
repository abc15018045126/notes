
package io.github.abc15018045126.sora.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.R;

/**
 * A simple symbol input view implementation for editor.
 *
 * <p>
 * First, add your symbols by {@link #addSymbols(String[], String[])}.
 * Then, bind a certain editor by {@link #bindEditor(CodeEditor)} so that it works
 *
 * @author abc15018045126
 */
public class SymbolInputView extends LinearLayout {

    private int textColor;
    private CodeEditor editor;

    public SymbolInputView(Context context) {
        super(context);
        init();
    }

    public SymbolInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SymbolInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SymbolInputView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setBackgroundColor(getContext().getResources().getColor(R.color.defaultSymbolInputBackgroundColor));
        setOrientation(HORIZONTAL);
        setTextColor(getContext().getResources().getColor(R.color.defaultSymbolInputTextColor));
    }

    /**
     * Bind editor for the view
     */
    public void bindEditor(CodeEditor editor) {
        this.editor = editor;
    }

    /**
     * @see #setTextColor(int)
     */
    public int getTextColor() {
        return textColor;
    }

    /**
     * Set text color in the panel
     */
    public void setTextColor(int color) {
        for (int i = 0; i < getChildCount(); i++) {
            ((Button) getChildAt(i)).setTextColor(color);
        }
        textColor = color;
    }

    /**
     * Remove all added symbols
     */
    public void removeSymbols() {
        removeAllViews();
    }

    /**
     * Add symbols to the view.
     *
     * @param display    The texts displayed in button
     * @param insertText The actual text to be inserted to editor when the button is clicked
     */
    public void addSymbols(@NonNull String[] display, @NonNull final String[] insertText) {
        int count = Math.max(display.length, insertText.length);
        for (int i = 0; i < count; i++) {
            var btn = new Button(getContext(), null, android.R.attr.buttonStyleSmall);
            btn.setText(display[i]);
            btn.setBackground(new ColorDrawable(Color.TRANSPARENT));
            btn.setTextColor(textColor);
            addView(btn, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
            int finalI = i;
            btn.setOnClickListener((view) -> {
                if (editor == null || !editor.isEditable()) {
                    return;
                }

                if ("\t".equals(insertText[finalI])) {
                    if (editor.getSnippetController().isInSnippet()) {
                        editor.getSnippetController().shiftToNextTabStop();
                    } else {
                        editor.indentOrCommitTab();
                    }
                } else {
                    editor.insertText(insertText[finalI], 1);
                }
            });
        }
    }

    public void forEachButton(@NonNull ButtonConsumer consumer) {
        for (int i = 0; i < getChildCount(); i++) {
            consumer.accept((Button) getChildAt(i));
        }
    }

    public interface ButtonConsumer {

        void accept(@NonNull Button btn);

    }

}


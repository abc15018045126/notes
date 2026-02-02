
package io.github.abc15018045126.sora.widget;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.event.EventReceiver;
import io.github.abc15018045126.sora.event.SelectionChangeEvent;
import io.github.abc15018045126.sora.event.Unsubscribe;

/**
 * This class is used to control cursor visibility
 *
 * @author Rose
 */
final class CursorBlink implements Runnable, EventReceiver<SelectionChangeEvent> {

    final CodeEditor editor;
    public boolean visibility;
    public boolean valid;
    long lastSelectionModificationTime = 0;
    int period;
    private float[] buffer;

    public CursorBlink(CodeEditor editor, int period) {
        visibility = true;
        this.editor = editor;
        this.period = period;
        editor.subscribeEvent(SelectionChangeEvent.class, this);
    }

    @Override
    public void onReceive(@NonNull SelectionChangeEvent event, @NonNull Unsubscribe unsubscribe) {
        onSelectionChanged();
    }

    public void setPeriod(int period) {
        this.period = period;
        if (period <= 0) {
            visibility = true;
            valid = false;
        } else {
            valid = true;
        }
    }

    public void onSelectionChanged() {
        lastSelectionModificationTime = System.currentTimeMillis();
        visibility = true;
    }

    public boolean isSelectionVisible() {
        return (buffer[0] >= editor.getOffsetY() && buffer[0] - editor.getRowHeight() <= editor.getOffsetY() + editor.getHeight()
                && buffer[1] >= editor.getOffsetX() && buffer[1] - 100f/* larger than a single character */ <= editor.getOffsetX() + editor.getWidth());
    }

    @Override
    public void run() {
        if (valid && period > 0) {
            if (System.currentTimeMillis() - lastSelectionModificationTime >= period * 2L) {
                visibility = !visibility;
                var left = editor.getCursor().left();
                buffer = editor.getLayout().getCharLayoutOffset(left.line, left.column, buffer);
                if (!editor.getCursor().isSelected() && isSelectionVisible()) {
                    editor.postInvalidate();
                }
            } else {
                visibility = true;
            }
            editor.postDelayedInLifecycle(this, period);
        } else {
            visibility = true;
        }
    }

}


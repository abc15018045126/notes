package io.github.abc15018045126.sora.widget.style.builtin;

import android.graphics.Canvas;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle;

/**
 * A handle style that draws nothing.
 */
public class HandleStyleNone implements SelectionHandleStyle {

    @Override
    public void draw(@NonNull Canvas canvas, int handleType, float x, float y, int rowHeight, int color, @NonNull HandleDescriptor descriptor) {
        descriptor.setEmpty();
    }

    @Override
    public void setAlpha(int alpha) {
        // Ignored
    }

    @Override
    public void setScale(float factor) {
        // Ignored
    }
}

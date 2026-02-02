
package io.github.abc15018045126.sora.widget.style;

import android.graphics.Canvas;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme;

/**
 * Class for custom handle style
 *
 * @author abc15018045126
 */
public interface SelectionHandleStyle {

    int HANDLE_TYPE_UNDEFINED = -1;
    int HANDLE_TYPE_INSERT = 0;
    int HANDLE_TYPE_LEFT = 1;
    int HANDLE_TYPE_RIGHT = 2;

    int ALIGN_CENTER = 0;
    int ALIGN_LEFT = 1;
    int ALIGN_RIGHT = 2;

    /**
     * Draw a handle to the given canvas and return descriptor of handle.
     *
     * @param canvas     Canvas to draw
     * @param handleType Type of handle being drawn at this position. Value can be {@link #HANDLE_TYPE_INSERT}, {@link #HANDLE_TYPE_LEFT}, {@link #HANDLE_TYPE_RIGHT} or {@link #HANDLE_TYPE_UNDEFINED}
     * @param x          The x of text position on canvas
     * @param y          The y of row bottom position on canvas
     * @param rowHeight  The height of a single row
     * @param color      The color of handle configured in {@link EditorColorScheme}
     * @param descriptor The descriptor that should be adjusted
     */
    void draw(@NonNull Canvas canvas, int handleType, float x, float y, int rowHeight, int color, @NonNull HandleDescriptor descriptor);

    void setAlpha(int alpha);

    void setScale(float factor);

    /**
     * The descriptor of a drawn handle on canvas
     */
    class HandleDescriptor {

        /**
         * The position of handle
         */
        public final RectF position = new RectF();
        /**
         * The alignment of the handle (of the x coordinate)
         * For example, you can draw handle with align right of the x when you draw the left handle
         *
         * @see #ALIGN_CENTER
         * @see #ALIGN_LEFT
         * @see #ALIGN_RIGHT
         */
        public int alignment = ALIGN_CENTER;

        public void set(float left, float top, float right, float bottom, int alignment) {
            this.alignment = alignment;
            position.set(left, top, right, bottom);
        }

        public void setEmpty() {
            position.setEmpty();
            this.alignment = ALIGN_CENTER;
        }

    }

}


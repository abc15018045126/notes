
package io.github.abc15018045126.sora.widget.layout;

import android.view.View;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.graphics.Paint;
import io.github.abc15018045126.sora.graphics.SingleCharacterWidths;
import io.github.abc15018045126.sora.text.Content;
import io.github.abc15018045126.sora.util.IntPair;
import io.github.abc15018045126.sora.util.MutableInt;

public class ViewMeasureHelper {

    /**
     * Get desired view size for the given arguments
     */
    public static long getDesiredSize(int widthMeasureSpec, int heightMeasureSpec, float gutterSize, float rowHeight, boolean wordwrap, int tabSize, @NonNull Content text, @NonNull Paint paint) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int maxSize = 0X3FFFFFFF;
        int maxWidth;
        if (widthMode == View.MeasureSpec.UNSPECIFIED) {
            maxWidth = maxSize;
        } else {
            maxWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        }
        int maxHeight;
        if (heightMode == View.MeasureSpec.UNSPECIFIED) {
            maxHeight = maxSize;
        } else {
            maxHeight = View.MeasureSpec.getSize(heightMeasureSpec);
        }
        var measurer = new SingleCharacterWidths(tabSize);
        if (wordwrap) {
            if (widthMode != View.MeasureSpec.EXACTLY) {
                var lines = heightMode != View.MeasureSpec.EXACTLY ? new int[text.getLineCount()] : null;
                var lineMaxSize = new MutableInt(0);
                text.runReadActionsOnLines(0, text.getLineCount() - 1, (Content.ContentLineConsumer) (index, line, directions) -> {
                    int measured = (int) Math.ceil(measurer.measureText(line.getBackingCharArray(), 0, line.length(), paint));
                    if (measured > lineMaxSize.value) {
                        lineMaxSize.value = measured;
                    }
                    if (lines != null) {
                        lines[index] = measured;
                    }
                });
                int width = (int) Math.min(maxWidth, lineMaxSize.value + gutterSize);
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
                if (lines != null) {
                    var rowCount = new MutableInt(0);
                    int availableSize = (int) (width - gutterSize);
                    if (availableSize <= 0) {
                        rowCount.value = text.length();
                    } else {
                        for (int i = 0;i < lines.length;i++) {
                            rowCount.value += Math.max(1, Math.ceil(1.0 * lines[i] / availableSize));
                        }
                    }
                    int height = Math.min((int) (rowHeight * rowCount.value), maxHeight);
                    heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
                }
            } else {
                if (heightMode != View.MeasureSpec.EXACTLY) {
                    var rowCount = new MutableInt(0);
                    int availableSize = (int) (maxWidth - gutterSize);
                    if (availableSize <= 0) {
                        rowCount.value = text.length();
                    } else {
                        text.runReadActionsOnLines(0, text.getLineCount() - 1, (Content.ContentLineConsumer) (index, line, directions) -> {
                            int measured = (int) Math.ceil(measurer.measureText(line.getBackingCharArray(), 0, line.length(), paint));
                            rowCount.value += Math.max(1, Math.ceil(1.0 * measured / availableSize));
                        });
                    }
                    int height = Math.min((int) (rowHeight * rowCount.value), maxHeight);
                    heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
                }
            }
        } else {
            if (widthMode != View.MeasureSpec.EXACTLY) {
                var lineMaxSize = new MutableInt(0);
                text.runReadActionsOnLines(0, text.getLineCount() - 1, (Content.ContentLineConsumer) (index, line, directions) -> {
                    int measured = (int) Math.ceil(measurer.measureText(line.getBackingCharArray(), 0, line.length(), paint));
                    if (measured > lineMaxSize.value) {
                        lineMaxSize.value = measured;
                    }
                });
                var width = (int) Math.min(lineMaxSize.value + gutterSize, maxWidth);
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            }
            if (heightMode != View.MeasureSpec.EXACTLY) {
                var height = Math.min(maxHeight, (int) (rowHeight * text.getLineCount()));
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
            }
        }
        return IntPair.pack(widthMeasureSpec, heightMeasureSpec);
    }

}


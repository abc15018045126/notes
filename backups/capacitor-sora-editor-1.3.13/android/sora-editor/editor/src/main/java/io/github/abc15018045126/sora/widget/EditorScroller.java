
package io.github.abc15018045126.sora.widget;

import android.widget.OverScroller;

import androidx.annotation.NonNull;

public class EditorScroller {

    private final CodeEditor editor;

    private final OverScroller scroller;

    public EditorScroller(@NonNull CodeEditor editor) {
        scroller = new OverScroller(editor.getContext());
        this.editor = editor;
    }

    public void setEditorOffsets() {
        editor.setScrollX(scroller.getCurrX());
        editor.setScrollY(scroller.getCurrY());
    }

    public void startScroll(int startX, int startY, int dx, int dy) {
        startScroll(startX, startY, dx, dy, editor.getProps().scrollAnimationDurationMs);
    }

    public void startScroll(int startX, int startY, int dx, int dy, int duration) {
        scroller.startScroll(startX, startY, dx, dy, duration);
        setEditorOffsets();
    }

    public void forceFinished(boolean finished) {
        scroller.forceFinished(finished);
        setEditorOffsets();
    }

    public void abortAnimation() {
        scroller.abortAnimation();
        setEditorOffsets();
    }

    public boolean isFinished() {
        return scroller.isFinished();
    }

    public int getCurrX() {
        return scroller.getCurrX();
    }

    public int getCurrY() {
        return scroller.getCurrY();
    }

    public int getFinalX() {
        return scroller.getFinalX();
    }

    public int getFinalY() {
        return scroller.getFinalY();
    }

    public int getStartX() {
        return scroller.getStartX();
    }

    public int getStartY() {
        return scroller.getStartY();
    }

    public float getCurrVelocity() {
        return scroller.getCurrVelocity();
    }

    public boolean computeScrollOffset() {
        var computed = scroller.computeScrollOffset();
        if (computed) {
            setEditorOffsets();
        }
        return computed;
    }

    public void fling(int startX, int startY, int velocityX, int velocityY,
                      int minX, int maxX, int minY, int maxY, int overX, int overY) {
        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY, overX, overY);
        setEditorOffsets();
    }

    public boolean isOverScrolled() {
        return scroller.isOverScrolled();
    }

    public OverScroller getImplScroller() {
        return scroller;
    }
}


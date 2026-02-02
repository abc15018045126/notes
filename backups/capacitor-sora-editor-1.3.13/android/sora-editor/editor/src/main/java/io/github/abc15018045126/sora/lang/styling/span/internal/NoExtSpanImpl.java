
package io.github.abc15018045126.sora.lang.styling.span.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import io.github.abc15018045126.sora.lang.styling.Span;
import io.github.abc15018045126.sora.lang.styling.SpanPool;
import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor;
import io.github.abc15018045126.sora.lang.styling.span.SpanExt;

/**
 * Span without SpanExt support.
 *
 * @author abc15018045126
 */
public class NoExtSpanImpl implements Span {
    private final static SpanPool<NoExtSpanImpl> pool = new SpanPool<>(NoExtSpanImpl::new);

    private int column;
    private long style;
    private Object extra;

    NoExtSpanImpl() {

    }

    NoExtSpanImpl(int column, long style) {
        this.column = column;
        this.style = style;
    }

    public static NoExtSpanImpl obtain(int column, long style) {
        return pool.obtain(column, style);
    }


    @Override
    public void setColumn(int column) {
        this.column = column;
    }

    @Override
    public int getColumn() {
        return this.column;
    }

    @Override
    public void setStyle(long style) {
        this.style = style;
    }

    @Override
    public long getStyle() {
        return this.style;
    }

    @Override
    public void setUnderlineColor(@Nullable ResolvableColor color) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public ResolvableColor getUnderlineColor() {
        return null;
    }

    @Override
    public void setExtra(Object extraData) {
        this.extra = extraData;
    }

    @Override
    @Nullable
    public Object getExtra() {
        return this.extra;
    }

    @Override
    public void setSpanExt(int extType, @Nullable SpanExt ext) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasSpanExt(int extType) {
        return false;
    }

    @Nullable
    @Override
    public <T> T getSpanExt(int extType) {
        return null;
    }

    @Override
    public void removeAllSpanExt() {

    }

    @Override
    public void reset() {
        setColumn(0);
        setStyle(0L);
        extra = null;
    }

    @NonNull
    @Override
    public Span copy() {
        return new NoExtSpanImpl(this.column, this.style);
    }

    @Override
    public boolean recycle() {
        reset();
        return pool.offer(this);
    }

    @Override
    public String toString() {
        return "NoExtSpanImpl{" +
                "column=" + column +
                ", style=" + style +
                ", extra=" + extra +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NoExtSpanImpl noExtSpan = (NoExtSpanImpl) o;
        return column == noExtSpan.column && style == noExtSpan.style && Objects.equals(extra, noExtSpan.extra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, style, extra);
    }
}


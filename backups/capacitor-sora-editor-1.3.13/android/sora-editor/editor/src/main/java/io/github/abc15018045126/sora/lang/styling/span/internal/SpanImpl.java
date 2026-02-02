
package io.github.abc15018045126.sora.lang.styling.span.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.MutableIntObjectMap;

import java.util.Objects;

import io.github.abc15018045126.sora.lang.styling.Span;
import io.github.abc15018045126.sora.lang.styling.SpanPool;
import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor;
import io.github.abc15018045126.sora.lang.styling.span.SpanExt;
import io.github.abc15018045126.sora.lang.styling.span.SpanExtAttrs;

public class SpanImpl implements Span {
    private final static SpanPool<SpanImpl> pool = new SpanPool<>(SpanImpl::new);

    private int column;
    private long style;
    private Object extra;
    private MutableIntObjectMap<SpanExt> extMap;

    SpanImpl() {

    }

    SpanImpl(int column, long style) {
        setColumn(column);
        setStyle(style);
    }

    public static SpanImpl obtain(int column, long style) {
        return pool.obtain(column, style);
    }

    @Override
    public void setColumn(int column) {
        this.column = column;
    }

    @Override
    public int getColumn() {
        return column;
    }

    @Override
    public void setStyle(long style) {
        this.style = style;
    }

    @Override
    public long getStyle() {
        return style;
    }

    @Override
    public void setUnderlineColor(@Nullable ResolvableColor color) {
        setSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR, color);
    }

    @Nullable
    @Override
    public ResolvableColor getUnderlineColor() {
        return getSpanExt(SpanExtAttrs.EXT_UNDERLINE_COLOR);
    }

    @Override
    public void setExtra(Object extraData) {
        this.extra = extraData;
    }

    @Override
    @Nullable
    public Object getExtra() {
        return extra;
    }

    @Override
    public void setSpanExt(int extType, @Nullable SpanExt ext) {
        if (!SpanExtAttrs.checkType(extType, ext)) {
            throw new IllegalArgumentException("type mismatch: extType " + extType + " and extObj " + ext);
        }
        if (ext == null) {
            if (extMap != null) {
                extMap.remove(extType);
            }
            return;
        }
        if (extMap == null) {
            extMap = new MutableIntObjectMap<>();
        }
        extMap.set(extType, ext);
    }

    @Override
    public boolean hasSpanExt(int extType) {
        return getSpanExt(extType) != null;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSpanExt(int extType) {
        return extMap == null ? null : (T) extMap.get(extType);
    }

    @Override
    public void removeAllSpanExt() {
        if (extMap != null) {
            extMap.clear();
        }
    }

    @Override
    public void reset() {
        setColumn(0);
        setStyle(0L);
        extra = null;
        removeAllSpanExt();
    }

    @NonNull
    @Override
    public Span copy() {
        var span = new SpanImpl();
        span.setColumn(getColumn());
        span.setStyle(getStyle());
        if (extMap != null) {
            span.extMap = new MutableIntObjectMap<>();
            span.extMap.putAll(extMap);
        }
        return span;
    }

    @Override
    public boolean recycle() {
        reset();
        return pool.offer(this);
    }

    @NonNull
    @Override
    public String toString() {
        return "SpanImpl{" +
                "column=" + column +
                ", style=" + style +
                ", extra=" + extra +
                ", extMap=" + extMap +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpanImpl span = (SpanImpl) o;
        return column == span.column && style == span.style && Objects.equals(extMap, span.extMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, style, extMap);
    }
}


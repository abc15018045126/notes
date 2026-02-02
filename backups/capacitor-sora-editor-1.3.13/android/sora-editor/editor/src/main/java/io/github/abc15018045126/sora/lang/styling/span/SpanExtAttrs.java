
package io.github.abc15018045126.sora.lang.styling.span;

import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor;

public class SpanExtAttrs {
    /**
     * @see SpanColorResolver
     */
    public final static int EXT_COLOR_RESOLVER = 0;
    /**
     * @see SpanExternalRenderer
     */
    public final static int EXT_EXTERNAL_RENDERER = 1;
    /**
     * @see SpanInteractionInfo
     */
    public final static int EXT_INTERACTION_INFO = 2;
    /**
     * Set a {@link ResolvableColor} object for underline color resolving
     */
    public final static int EXT_UNDERLINE_COLOR = 3;

    public static boolean checkType(int extType, SpanExt ext) {
        if (ext == null) {
            return true;
        }
        switch (extType) {
            case EXT_COLOR_RESOLVER -> {
                return ext instanceof SpanColorResolver;
            }
            case EXT_EXTERNAL_RENDERER -> {
                return ext instanceof SpanExternalRenderer;
            }
            case EXT_INTERACTION_INFO -> {
                return ext instanceof SpanInteractionInfo;
            }
            case EXT_UNDERLINE_COLOR -> {
                return ext instanceof ResolvableColor;
            }
        }
        return true;
    }
}


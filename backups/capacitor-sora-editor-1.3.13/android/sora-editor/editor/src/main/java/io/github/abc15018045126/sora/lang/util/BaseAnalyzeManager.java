
package io.github.abc15018045126.sora.lang.util;

import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.function.Consumer;

import io.github.abc15018045126.sora.lang.analysis.AnalyzeManager;
import io.github.abc15018045126.sora.lang.analysis.StyleReceiver;
import io.github.abc15018045126.sora.text.ContentReference;

/**
 * Convenience base class for simple {@link AnalyzeManager} implementations
 *
 * @author abc15018045126
 */
public abstract class BaseAnalyzeManager implements AnalyzeManager {

    private StyleReceiver receiver;
    private ContentReference contentRef;
    private Bundle extraArguments;

    @Override
    public void setReceiver(@Nullable StyleReceiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Get current receiver, maybe null
     */
    @Nullable
    public StyleReceiver getReceiver() {
        return receiver;
    }

    /**
     * Get current extra arguments, maybe null
     */
    @Nullable
    public Bundle getExtraArguments() {
        return extraArguments;
    }

    /**
     * Get current content reference, maybe null
     */
    @Nullable
    public ContentReference getContentRef() {
        return contentRef;
    }

    @Override
    @CallSuper
    public void reset(@NonNull ContentReference content, @NonNull Bundle extraArguments) {
        this.extraArguments = extraArguments;
        this.contentRef = content;
        rerun();
    }

    @Override
    @CallSuper
    public void destroy() {
        this.receiver = null;
        this.contentRef = null;
        this.extraArguments = null;
    }

}


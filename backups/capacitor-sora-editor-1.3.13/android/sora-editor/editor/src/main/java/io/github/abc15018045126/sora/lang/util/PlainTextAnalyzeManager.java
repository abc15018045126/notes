
package io.github.abc15018045126.sora.lang.util;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.lang.styling.Styles;
import io.github.abc15018045126.sora.text.CharPosition;

/**
 * This class generate plain spans for content, in case when
 * using a language without analysis you can still provide context-free completions.
 * By default, editor does not show completions when no span is set
 * on the text. This would be helpful for enabling completion for pure texts.
 *
 * @author abc15018045126
 */
public final class PlainTextAnalyzeManager extends BaseAnalyzeManager {

    @Override
    public void insert(@NonNull CharPosition start, @NonNull CharPosition end, @NonNull CharSequence insertedContent) {

    }

    @Override
    public void delete(@NonNull CharPosition start, @NonNull CharPosition end, @NonNull CharSequence deletedContent) {

    }

    @Override
    public void rerun() {
        final var receiver = getReceiver();
        final var ref = getContentRef();
        if (receiver != null && ref != null) {
            var style = new Styles();
            style.spans = new PlainTextSpans(ref.getLineCount());
            receiver.setStyles(this, style);
        } else if (receiver != null) {
            receiver.setStyles(this, null);
        }
    }

}


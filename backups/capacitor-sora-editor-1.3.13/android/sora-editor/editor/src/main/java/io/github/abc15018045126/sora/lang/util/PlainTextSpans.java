
package io.github.abc15018045126.sora.lang.util;

import io.github.abc15018045126.sora.lang.styling.EmptyReader;
import io.github.abc15018045126.sora.lang.styling.Spans;
import io.github.abc15018045126.sora.text.CharPosition;

/**
 * {@link Spans} implementation that always returns {@link EmptyReader} for reading spans.
 * Line count is automatically adjusted as content changes.
 *
 * @author abc15018045126
 */
public class PlainTextSpans implements Spans {

    private int lineCount;

    public PlainTextSpans(int lineCount) {
        this.lineCount = lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
    }

    @Override
    public void adjustOnInsert(CharPosition start, CharPosition end) {
        lineCount += end.line - start.line;
    }

    @Override
    public void adjustOnDelete(CharPosition start, CharPosition end) {
        lineCount -= end.line - start.line;
    }

    @Override
    public Reader read() {
        return EmptyReader.INSTANCE;
    }

    @Override
    public boolean supportsModify() {
        return false;
    }

    @Override
    public Modifier modify() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLineCount() {
        return lineCount;
    }
}


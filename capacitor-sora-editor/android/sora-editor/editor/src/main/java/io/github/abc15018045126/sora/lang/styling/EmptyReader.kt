package io.github.abc15018045126.sora.lang.styling

import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

class EmptyReader : Spans.Reader {

    private val spans: List<Span> = listOf(
        SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong())
    )

    override fun moveToLine(line: Int) {
    }

    override fun getSpanAt(index: Int): Span {
        return spans[index]
    }

    override fun getSpanCount(): Int {
        return 1
    }

    override fun getSpansOnLine(line: Int): List<Span> {
        return ArrayList(spans)
    }

    companion object {
        @JvmField
        val INSTANCE = EmptyReader()
    }
}

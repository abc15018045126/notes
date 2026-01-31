package io.github.abc15018045126.sora.lang.completion.snippet

class PlainTextItem : SnippetItem {

    var text: String

    constructor(text: String, index: Int) : this(text, index, index + text.length)

    constructor(text: String, start: Int, end: Int) {
        setIndex(start, end)
        this.text = text
    }

    override fun clone(): PlainTextItem {
        return PlainTextItem(text, startIndex, endIndex)
    }
}

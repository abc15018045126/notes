package io.github.abc15018045126.sora.lang.completion.snippet

class PlaceholderItem : SnippetItem {

    var definition: PlaceholderDefinition
    private var text: String? = null

    constructor(definition: PlaceholderDefinition, index: Int) : super() {
        setIndex(index, index)
        this.definition = definition
    }

    private constructor(definition: PlaceholderDefinition, text: String?, start: Int, end: Int) : super() {
        setIndex(start, end)
        this.text = text
        this.definition = definition
    }

    public override fun clone(): PlaceholderItem {
        return PlaceholderItem(definition, text, startIndex, endIndex)
    }
}

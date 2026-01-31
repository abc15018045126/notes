package io.github.abc15018045126.sora.lang.completion.snippet

class VariableItem @JvmOverloads constructor(
    index: Int,
    var name: String,
    var defaultValue: String? = null,
    var transform: Transform? = null
) : SnippetItem(index), PlaceHolderElement {

    public override fun clone(): VariableItem {
        val n = VariableItem(startIndex, name, defaultValue)
        n.setIndex(startIndex, endIndex)
        return n
    }
}

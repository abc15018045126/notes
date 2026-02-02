package io.github.abc15018045126.sora.lang.completion.snippet

class InterpolatedShellItem(
    var shellCode: String,
    index: Int
) : SnippetItem(index) {

    override fun clone(): InterpolatedShellItem {
        val n = InterpolatedShellItem(shellCode, startIndex)
        n.setIndex(startIndex, endIndex)
        return n
    }
}

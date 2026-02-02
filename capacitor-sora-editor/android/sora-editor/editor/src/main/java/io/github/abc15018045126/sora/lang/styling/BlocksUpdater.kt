package io.github.abc15018045126.sora.lang.styling

/**
 * Update block line positions on edit
 */
object BlocksUpdater {

    /**
     * Update blocks
     *
     * @param blocks   Block lines to update
     * @param restrict Min line to update
     * @param delta    Delta for line index
     */
    @JvmStatic
    fun update(blocks: MutableList<CodeBlock>, restrict: Int, delta: Int) {
        if (delta == 0) {
            return
        }
        val itr = blocks.iterator()
        while (itr.hasNext()) {
            val block = itr.next()
            if (block.startLine >= restrict) {
                block.startLine += delta
            }
            if (block.endLine >= restrict) {
                block.endLine += delta
            }
            if (block.startLine >= block.endLine) {
                itr.remove()
            }
        }
    }
}

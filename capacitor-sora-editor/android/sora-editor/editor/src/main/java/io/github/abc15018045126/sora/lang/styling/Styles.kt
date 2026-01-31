package io.github.abc15018045126.sora.lang.styling

import io.github.abc15018045126.sora.lang.styling.line.LineAnchorStyle
import io.github.abc15018045126.sora.lang.styling.line.LineStyles
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.util.MutableInt
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * This class stores styles of text and other decorations in editor related to code.
 *
 * Note that this does not save any information related to languages. No extra space is provided
 * for communication between analyzers and auto-completion. You should manage it by yourself.
 *
 * If you are going to extend this class, please read the source code carefully in advance
 */
@Suppress("unused")
open class Styles @JvmOverloads constructor(
    @JvmField
    var spans: Spans? = null,
    initCodeBlocks: Boolean = true
) {

    @JvmField
    var lineStyles: MutableList<LineStyles>? = null
    @JvmField
    var styleTypeCount: MutableMap<Class<*>, MutableInt>? = null

    @JvmField
    var blocks: MutableList<CodeBlock>? = null

    /**
     * Internal, automatically generated
     */
    @JvmField
    var blocksByStart: MutableList<CodeBlock>? = null

    @JvmField
    var suppressSwitch = Int.MAX_VALUE

    @JvmField
    var isIndentCountMode = false

    init {
        if (initCodeBlocks) {
            blocks = ArrayList(128)
        }
    }

    /**
     * Get analyzed spans
     */
    fun getSpans(): Spans? = spans

    /**
     * Returns suppress switch
     *
     * @return suppress switch
     * @see Styles.suppressSwitch
     */
    fun getSuppressSwitch(): Int = suppressSwitch

    /**
     * Set suppress switch for editor
     */
    fun setSuppressSwitch(suppressSwitch: Int) {
        this.suppressSwitch = suppressSwitch
    }

    fun isIndentCountMode(): Boolean = isIndentCountMode

    fun setIndentCountMode(indentCountMode: Boolean) {
        this.isIndentCountMode = indentCountMode
    }

    /**
     * Add a new code block info
     *
     * @param block Info of code block
     */
    fun addCodeBlock(block: CodeBlock) {
        Objects.requireNonNull(block, "CodeBlock must not be null")
        blocks?.add(block)
    }

    /**
     * Adjust styles on insert.
     */
    fun adjustOnInsert(start: CharPosition, end: CharPosition) {
        spans?.adjustOnInsert(start, end)
        val delta = end.line - start.line
        if (delta == 0) {
            return
        }
        val currentBlocks = blocks
        if (currentBlocks != null) {
            BlocksUpdater.update(currentBlocks, start.line, delta)
        }
        val currentLineStyles = lineStyles
        if (currentLineStyles != null) {
            for (styles in currentLineStyles) {
                if (styles.line > start.line) {
                    styles.line = styles.line + delta
                    styles.updateElements()
                }
            }
        }
    }

    /**
     * Adjust styles on delete.
     */
    fun adjustOnDelete(start: CharPosition, end: CharPosition) {
        spans?.adjustOnDelete(start, end)
        val delta = start.line - end.line
        if (delta == 0) {
            return
        }
        val currentBlocks = blocks
        if (currentBlocks != null) {
            BlocksUpdater.update(currentBlocks, start.line, delta)
        }
        val currentLineStyles = lineStyles
        if (currentLineStyles != null) {
            val itr = currentLineStyles.iterator()
            while (itr.hasNext()) {
                val styles = itr.next()
                val line = styles.line
                if (line > end.line) {
                    styles.line = line + delta
                    styles.updateElements()
                } else if (line > start.line /* line <= end.line */) {
                    itr.remove()
                }
            }
        }
    }

    fun addLineStyle(style: LineAnchorStyle) {
        if (lineStyles == null) {
            lineStyles = ArrayList()
            styleTypeCount = ConcurrentHashMap()
        }
        val type = style.javaClass
        val currentLineStyles = lineStyles!!
        for (lineStyle in currentLineStyles) {
            if (lineStyle.line == style.line) {
                styleCountUpdate(type, lineStyle.addStyle(style))
                return
            }
        }
        val lineStyle = LineStyles(style.line)
        currentLineStyles.add(lineStyle)
        styleCountUpdate(type, lineStyle.addStyle(style))
    }

    private fun styleCountUpdate(type: Class<*>, delta: Int) {
        val countMap = styleTypeCount ?: return
        var res = countMap[type]
        if (res == null) {
            res = MutableInt(0)
            countMap[type] = res
        }
        res.value += delta
    }

    /**
     * Remove the style of given kind from line
     */
    fun eraseLineStyle(line: Int, type: Class<out LineAnchorStyle>) {
        val currentLineStyles = lineStyles ?: return
        for (lineStyle in currentLineStyles) {
            if (lineStyle.line == line) {
                styleCountUpdate(type, -lineStyle.eraseStyle(type))
                break
            }
        }
    }

    /**
     * Remove all line styles
     */
    fun eraseAllLineStyles() {
        val currentLineStyles = lineStyles ?: return
        currentLineStyles.clear()
        styleTypeCount?.clear()
    }

    /**
     * Do some extra work before finally sending the result to editor.
     */
    fun finishBuilding() {
        val currentBlocks = blocks
        if (currentBlocks != null) {
            var pre = -1
            var sort = false
            for (i in 0 until currentBlocks.size - 1) {
                val cur = currentBlocks[i + 1].endLine
                if (pre > cur) {
                    sort = true
                    break
                }
                pre = cur
            }
            if (sort) {
                Collections.sort(currentBlocks, CodeBlock.COMPARATOR_END)
            }
            blocksByStart = ArrayList(currentBlocks)
            Collections.sort(blocksByStart, CodeBlock.COMPARATOR_START)
        } else {
            blocksByStart = null
        }
        lineStyles?.let {
            Collections.sort(it)
        }
    }

}

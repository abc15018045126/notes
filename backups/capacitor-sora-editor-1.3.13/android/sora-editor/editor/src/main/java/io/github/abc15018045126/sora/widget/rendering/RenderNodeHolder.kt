
package io.github.abc15018045126.sora.widget.rendering

import android.graphics.Canvas
import android.graphics.RenderNode
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.lang.styling.EmptyReader
import io.github.abc15018045126.sora.widget.CodeEditor
import java.util.Collections
import java.util.Stack

/**
 * Hardware accelerated text render, which manages [RenderNode]
 * to speed up rendering.
 *
 * @author abc15018045126
 */
@RequiresApi(Build.VERSION_CODES.Q)
class RenderNodeHolder(private val editor: CodeEditor) {
    private val cache: ArrayList<TextRenderNode> = ArrayList(64)
    private val pool = Stack<TextRenderNode>()

    fun shouldUpdateCache(): Boolean {
        return !editor.isWordwrap && editor.isHardwareAcceleratedDrawAllowed
    }

    fun invalidateInRegion(range: StyleUpdateRange): Boolean {
        var res = false
        val itr = cache.iterator()
        while (itr.hasNext()) {
            val element = itr.next()
            if (range.isInRange(element.line)) {
                itr.remove()
                element.renderNode.discardDisplayList()
                pool.push(element)
                res = true
            }
        }
        return res
    }

    /**
     * Called by editor when text style changes.
     * Such as text size/typeface.
     * Also called when wordwrap state changes from true to false
     */
    fun invalidate() {
        cache.forEach { it.isDirty = true }
    }

    fun getNode(line: Int): TextRenderNode {
        val size = cache.size
        for (i in 0 until size) {
            val node = cache[i]
            if (node!!.line == line) {
                Collections.swap(cache, 0, i)
                return node
            }
        }
        val node = if (pool.isEmpty()) TextRenderNode(line) else pool.pop()
        node.line = line
        node.isDirty = true
        cache.add(0, node)
        return node
    }

    fun keepCurrentInDisplay(start: Int, end: Int) {
        val itr = cache.iterator()
        while (itr.hasNext()) {
            val node = itr.next()
            if (node.line < start || node.line > end) {
                itr.remove()
                node.renderNode.discardDisplayList()
            }
        }
    }

    fun drawLineHardwareAccelerated(
        canvas: Canvas,
        line: Int,
        offsetX: Float,
        offsetY: Float
    ): Int {
        if (!canvas.isHardwareAccelerated) {
            throw UnsupportedOperationException("Only hardware-accelerated canvas can be used")
        }
        val styles = editor.styles
        // It's safe to use row directly because the mode is non-wordwrap
        val node = getNode(line)
        if (node.needsRecord()) {
            val spans = styles?.spans
            var reader = if (spans == null) EmptyReader.INSTANCE else spans.read()
            try {
                reader.moveToLine(line)
            } catch (e: Exception) {
                reader = EmptyReader.INSTANCE
            }
            editor.renderer.updateLineDisplayList(node.renderNode, line, reader)
            try {
                reader.moveToLine(-1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            node.isDirty = false
        }
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.drawRenderNode(node.renderNode)
        canvas.restore()
        return node.renderNode.width
    }

    fun afterInsert(startLine: Int, endLine: Int) {
        cache.forEach { node ->
            if (node.line == startLine) {
                node.isDirty = true
            } else if (node.line > startLine) {
                node.line += endLine - startLine
            }
        }
    }

    fun afterDelete(startLine: Int, endLine: Int) {
        val garbage: MutableList<TextRenderNode> = ArrayList()
        cache.forEach { node ->
            if (node.line == startLine) {
                node.isDirty = true
            } else if (node.line in (startLine + 1)..endLine) {
                garbage.add(node)
                node.renderNode.discardDisplayList()
            } else if (node.line > endLine) {
                node.line -= endLine - startLine
            }
        }
        cache.removeAll(garbage.toSet())
        pool.addAll(garbage)
    }

    class TextRenderNode(
        /**
         * The target line of this node.
         * -1 for unavailable
         */
        var line: Int
    ) {
        var renderNode: RenderNode = RenderNode("editorRenderNode")
        var isDirty: Boolean = true

        fun needsRecord(): Boolean {
            return isDirty || !renderNode.hasDisplayList()
        }
    }
}


package io.github.abc15018045126.sora.text.bidi

import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.text.ContentListener
import io.github.abc15018045126.sora.util.IntPair
import java.util.Arrays

/**
 * Companion for [Content] to manage line directions
 *
 * @author abc15018045126
 */
class ContentBidi(content: Content) : ContentListener {

    private val entries = arrayOfNulls<DirectionsEntry>(MAX_BIDI_CACHE_ENTRY_COUNT)
    private val text: Content = content
    var isEnabled: Boolean = false
        set(enabled) {
            field = enabled
            if (!enabled) {
                Arrays.fill(entries, null)
            }
        }

    init {
        text.addContentListener(this)
    }

    fun getLineDirections(lineText: ContentLine, line: Int): Directions {
        if (!isEnabled) {
            return Directions(longArrayOf(IntPair.pack(0, 0)), lineText.length)
        }
        synchronized(this) {
            for (i in entries.indices) {
                val entry = entries[i]
                if (entry != null && entry.line == line) {
                    return entry.dir
                }
            }
        }
        val dir = TextBidi.getDirections(lineText)
        synchronized(this) {
            System.arraycopy(entries, 0, entries, 1, entries.size - 1)
            entries[0] = DirectionsEntry(dir, line)
        }
        return dir
    }

    @Synchronized
    override fun afterDelete(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        deletedContent: CharSequence
    ) {
        val delta = endLine - startLine
        for (i in entries.indices) {
            val entry = entries[i] ?: continue
            if (entry.line >= startLine) {
                if (entry.line > endLine) {
                    entry.line -= delta
                } else {
                    entries[i] = null
                }
            }
        }
    }

    @Synchronized
    override fun afterInsert(
        content: Content,
        startLine: Int,
        startColumn: Int,
        endLine: Int,
        endColumn: Int,
        insertedContent: CharSequence
    ) {
        val delta = endLine - startLine
        for (i in entries.indices) {
            val entry = entries[i] ?: continue
            if (entry.line > startLine) {
                entry.line += delta
            } else if (entry.line == startLine) {
                entries[i] = null
            }
        }
    }

    override fun beforeReplace(content: Content) {}

    fun destroy() {
        text.removeContentListener(this)
        Arrays.fill(entries, null)
    }

    private class DirectionsEntry(var dir: Directions, var line: Int)

    companion object {
        const val MAX_BIDI_CACHE_ENTRY_COUNT = 64
    }
}

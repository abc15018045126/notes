package io.github.abc15018045126.sora.widget.layout

import android.util.SparseArray
import androidx.annotation.Size
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.text.ContentListener

/**
 * Layout is a manager class for editor to display text
 * Different layout may display texts in different way
 * Implementations of this interface should manage 'row's in editor.
 *
 * @author Rose
 */
interface Layout : ContentListener {

    /**
     * Called by editor to destroy this layout
     * This means the layout will never be used again
     */
    fun destroyLayout()

    /**
     * Get line index of a row in layout
     *
     * @param row The row index in layout
     * @return Line index in text
     */
    fun getLineNumberForRow(row: Int): Int

    /**
     * Return a [RowIterator] object for editor to draw text rows
     *
     * @param initialRow The first row in result iterator
     * @return Iterator contains rows
     */
    fun obtainRowIterator(initialRow: Int): RowIterator {
        return obtainRowIterator(initialRow, null)
    }

    /**
     * Return a [RowIterator] object for editor to draw text rows
     *
     * @param initialRow The first row in result iterator
     * @param preloadedLines Lines that are already loaded in editor
     * @return Iterator contains rows
     */
    fun obtainRowIterator(initialRow: Int, preloadedLines: SparseArray<ContentLine>?): RowIterator

    /**
     * Get the specific Row
     */
    fun getRowAt(rowIndex: Int): Row

    /**
     * Get the width of this layout
     * Editor will use this to compute scroll range
     *
     * @return Width of layout
     */
    val layoutWidth: Int

    /**
     * Get the height of this layout
     * Editor will use this to compute scroll range
     *
     * @return Height of layout
     */
    val layoutHeight: Int

    /**
     * Get the total row count
     */
    val rowCount: Int

    /**
     * Get character line and column for offsets in layout
     *
     * @param xOffset Horizontal offset on layout
     * @param yOffset Vertical offset on layout
     * @return Packed IntPair, first is line and second is column
     * @see io.github.abc15018045126.sora.util.IntPair
     */
    fun getCharPositionForLayoutOffset(xOffset: Float, yOffset: Float): Long

    /**
     * Get layout offset of a position in text
     *
     * @param line   The line index
     * @param column Column on line
     * @return An array containing layout offset, first element is the bottom of character and second element is the left of character
     */
    @Size(2)
    fun getCharLayoutOffset(line: Int, column: Int): FloatArray {
        return getCharLayoutOffset(line, column, FloatArray(2))
    }

    /**
     * Get layout offset of a position in text
     *
     * @param line   The line index
     * @param column Column on line
     * @param array  If the array is given, it will try to save the two elements in this array. Otherwise, a new array is created
     * @return An array containing layout offset, first element is the bottom of character and second element is the left of character
     */
    fun getCharLayoutOffset(line: Int, column: Int, array: FloatArray?): FloatArray

    /**
     * Get how many rows are in the given line
     */
    fun getRowCountForLine(line: Int): Int

    /**
     * Get position after moving up once
     *
     * @return A packed pair (line, column) describing the result position
     */
    fun getUpPosition(line: Int, column: Int): Long

    /**
     * Get position after moving down once
     *
     * @return A packed pair (line, column) describing the result position
     */
    fun getDownPosition(line: Int, column: Int): Long

    /**
     * Get row index for text index
     */
    fun getRowIndexForPosition(index: Int): Int

    /**
     * Notify the layout that the given lines have external changes and their layout should be re-calculated.
     */
    fun invalidateLines(range: StyleUpdateRange)

    /**
     * Get row top y offset
     */
    fun getRowTop(row: Int): Int

    /**
     * Get row bottom y offset
     */
    fun getRowBottom(row: Int): Int

    /**
     * Get row index for y offset
     */
    fun getRowIndexForY(y: Float): Int
}

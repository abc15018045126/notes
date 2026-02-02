
package io.github.abc15018045126.sora.widget

import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.TextUtils
import io.github.abc15018045126.sora.util.Chars
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.Numbers
import kotlin.math.ceil

private typealias SelectionMovementComputeFunc = ((CodeEditor, CharPosition) -> CharPosition)

/**
 * Defines selection movement types for editor.
 *
 * @author abc15018045126
 */
enum class SelectionMovement(
    private val computeFunc: SelectionMovementComputeFunc,
    val basePosition: MovingBasePosition = MovingBasePosition.SELECTION_ANCHOR
) {
    /** Move Up */
    UP({ editor, pos ->
        val newPos = editor.layout.getUpPosition(pos.line, pos.column)
        editor.text.indexer.getCharPosition(IntPair.getFirst(newPos), IntPair.getSecond(newPos))
    }, MovingBasePosition.LEFT_SELECTION),

    /** Move Down */
    DOWN({ editor, pos ->
        val newPos = editor.layout.getDownPosition(pos.line, pos.column)
        editor.text.indexer.getCharPosition(IntPair.getFirst(newPos), IntPair.getSecond(newPos))
    }, MovingBasePosition.RIGHT_SELECTION),

    /** Move Left */
    LEFT({ editor, pos ->
        val newPos = editor.cursor.getLeftOf(pos.toIntPair())
        editor.text.indexer.getCharPosition(IntPair.getFirst(newPos), IntPair.getSecond(newPos))
    }, MovingBasePosition.LEFT_SELECTION),

    /** Move Right */
    RIGHT({ editor, pos ->
        val newPos = editor.cursor.getRightOf(pos.toIntPair())
        editor.text.indexer.getCharPosition(IntPair.getFirst(newPos), IntPair.getSecond(newPos))
    }, MovingBasePosition.RIGHT_SELECTION),

    /** Move To Previous Word Boundary */
    PREVIOUS_WORD_BOUNDARY({ editor, pos ->
        val newPos = Chars.prevWordStart(pos, editor.text)
        editor.text.indexer.getCharPosition(newPos.line, newPos.column)
    }),

    /** Move To Next Word Boundary */
    NEXT_WORD_BOUNDARY({ editor, pos ->
        val newPos = Chars.nextWordEnd(pos, editor.text)
        editor.text.indexer.getCharPosition(newPos.line, newPos.column)
    }),

    /** Move Page Up */
    PAGE_UP({ editor, pos ->
        val layout = editor.layout
        val rowCount = ceil(editor.height / editor.rowHeight.toFloat()).toInt()
        val currIdx = layout.getRowIndexForPosition(pos.index)
        val afterIdx = Numbers.coerceIn(currIdx - rowCount, 0, layout.rowCount - 1)
        val selOffset = pos.column - layout.getRowAt(currIdx).startColumn
        val row = layout.getRowAt(afterIdx)
        val line = row.lineIndex
        val column =
            row.startColumn + Numbers.coerceIn(selOffset, 0, row.endColumn - row.startColumn)
        editor.text.indexer.getCharPosition(line, column)
    }),

    /** Move Page Down */
    PAGE_DOWN({ editor, pos ->
        val layout = editor.layout
        val rowCount = ceil(editor.height / editor.rowHeight.toFloat()).toInt()
        val currIdx = layout.getRowIndexForPosition(pos.index)
        val afterIdx = Numbers.coerceIn(currIdx + rowCount, 0, layout.rowCount - 1)
        val selOffset = pos.column - layout.getRowAt(currIdx).startColumn
        val row = layout.getRowAt(afterIdx)
        val line = row.lineIndex
        val column =
            row.startColumn + Numbers.coerceIn(selOffset, 0, row.endColumn - row.startColumn)
        editor.text.indexer.getCharPosition(line, column)
    }),

    /** Move To Page Top */
    PAGE_TOP({ editor, pos ->
        val layout = editor.layout
        val currIdx = layout.getRowIndexForPosition(pos.index)
        val selOffset = pos.column - layout.getRowAt(currIdx).startColumn
        val afterIdx = editor.firstVisibleRow
        val row = layout.getRowAt(afterIdx)
        val line = row.lineIndex
        val column =
            row.startColumn + Numbers.coerceIn(selOffset, 0, row.endColumn - row.startColumn)
        editor.text.indexer.getCharPosition(line, column)
    }),

    /** Move To Page Bottom */
    PAGE_BOTTOM({ editor, pos ->
        val layout = editor.layout
        val currIdx = layout.getRowIndexForPosition(pos.index)
        val selOffset = pos.column - layout.getRowAt(currIdx).startColumn
        val afterIdx = editor.lastVisibleRow
        val row = layout.getRowAt(afterIdx)
        val line = row.lineIndex
        val column =
            row.startColumn + Numbers.coerceIn(selOffset, 0, row.endColumn - row.startColumn)
        editor.text.indexer.getCharPosition(line, column)
    }),

    /** Move To Physical Line Start */
    LINE_START({ editor, pos ->
        if (editor.props.enhancedHomeAndEnd) {
            val column = IntPair.getFirst(
                TextUtils.findLeadingAndTrailingWhitespacePos(
                    editor.text.getLine(pos.line)
                )
            )
            if (pos.column == column || column == editor.text.getColumnCount(pos.line)) {
                // Move to start if already at enhanced start / line is space-filled
                editor.text.indexer.getCharPosition(pos.line, 0)
            } else {
                editor.text.indexer.getCharPosition(pos.line, column)
            }
        } else {
            editor.text.indexer.getCharPosition(pos.line, 0)
        }
    }),

    /** Move To Physical Line End */
    LINE_END({ editor, pos ->
        val colNum = editor.text.getColumnCount(pos.line)
        if (editor.props.enhancedHomeAndEnd) {
            val column = IntPair.getSecond(
                TextUtils.findLeadingAndTrailingWhitespacePos(
                    editor.text.getLine(pos.line)
                )
            )
            if (pos.column != column) {
                editor.text.indexer.getCharPosition(pos.line, column)
            } else {
                editor.text.indexer.getCharPosition(pos.line, colNum)
            }
        } else {
            editor.text.indexer.getCharPosition(pos.line, colNum)
        }
    }),

    /** Move To Text Start */
    TEXT_START({ _, _ ->
        CharPosition().toBOF()
    }),

    /** Move To Text End */
    TEXT_END({ editor, _ ->
        editor.text.indexer.getCharPosition(editor.text.length)
    }),

    /** Move To Visual Line Start */
    ROW_START({ editor, pos ->
        val layout = editor.layout
        val rowIndex = layout.getRowIndexForPosition(pos.index)
        val row = layout.getRowAt(rowIndex)
        val maxColumn =
            if (rowIndex + 1 == layout.rowCount || layout.getRowAt(rowIndex + 1).lineIndex != row.lineIndex) {
                row.endColumn
            } else {
                row.endColumn - 1
            }
        if (editor.props.enhancedHomeAndEnd) {
            val column = IntPair.getFirst(
                TextUtils.findLeadingAndTrailingWhitespacePos(
                    editor.text.getLine(pos.line), row.startColumn, maxColumn
                )
            )
            if (pos.column == column || column == maxColumn) {
                // Move to start if already at enhanced start / line is space-filled
                editor.text.indexer.getCharPosition(pos.line, row.startColumn)
            } else {
                editor.text.indexer.getCharPosition(pos.line, column)
            }
        } else {
            editor.text.indexer.getCharPosition(row.lineIndex, row.startColumn)
        }
    }),

    /** Move To Visual Line End */
    ROW_END({ editor, pos ->
        val layout = editor.layout
        val rowIndex = layout.getRowIndexForPosition(pos.index)
        val row = layout.getRowAt(rowIndex)
        val maxColumn =
            if (rowIndex + 1 == layout.rowCount || layout.getRowAt(rowIndex + 1).lineIndex != row.lineIndex) {
                row.endColumn
            } else {
                row.endColumn - 1
            }
        if (editor.props.enhancedHomeAndEnd) {
            val column = IntPair.getSecond(
                TextUtils.findLeadingAndTrailingWhitespacePos(
                    editor.text.getLine(pos.line), row.startColumn, maxColumn
                )
            )
            if (pos.column != column) {
                editor.text.indexer.getCharPosition(pos.line, column)
            } else {
                editor.text.indexer.getCharPosition(pos.line, maxColumn)
            }
        } else {
            editor.text.indexer.getCharPosition(row.lineIndex, maxColumn)
        }
    });

    /**
     * For [CodeEditor.moveSelection]
     */
    enum class MovingBasePosition {
        LEFT_SELECTION,
        RIGHT_SELECTION,
        SELECTION_ANCHOR
    }

    @UnsupportedUserUsage
    fun getPositionAfterMovement(editor: CodeEditor, pos: CharPosition): CharPosition {
        return this.computeFunc(editor, pos)
    }
}


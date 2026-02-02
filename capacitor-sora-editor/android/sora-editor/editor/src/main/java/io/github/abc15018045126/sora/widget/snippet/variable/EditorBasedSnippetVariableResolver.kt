package io.github.abc15018045126.sora.widget.snippet.variable

import io.github.abc15018045126.sora.text.ICUUtils
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.CodeEditor

class EditorBasedSnippetVariableResolver(private val editor: CodeEditor) : ISnippetVariableResolver {

    override fun getResolvableNames(): Array<String> {
        return arrayOf(
            "TM_CURRENT_LINE", "TM_LINE_INDEX", "TM_LINE_NUMBER", "CURSOR_INDEX", "CURSOR_NUMBER",
            "TM_CURRENT_WORD", "SELECTION", "TM_SELECTED_TEXT"
        )
    }

    override fun resolve(name: String): String {
        return when (name) {
            "TM_CURRENT_LINE", "TM_LINE_NUMBER" -> (editor.cursor.leftLine + 1).toString()
            "TM_LINE_INDEX" -> editor.cursor.leftLine.toString()
            "CURSOR_INDEX" -> editor.cursor.left.toString()
            "CURSOR_NUMBER" -> (editor.cursor.left + 1).toString()
            "TM_CURRENT_WORD" -> {
                val text = editor.text
                val lineIndex = text.cursor.leftLine
                val line = text.getLine(lineIndex)
                val res = ICUUtils.getWordRange(line, text.cursor.leftColumn, true)
                line.subSequence(IntPair.getFirst(res), IntPair.getSecond(res)).toString()
            }
            "SELECTION", "TM_SELECTED_TEXT" -> {
                val cursor = editor.cursor
                editor.text.subSequence(cursor.left, cursor.right).toString()
            }
            else -> throw IllegalArgumentException("Unsupported variable name:$name")
        }
    }
}

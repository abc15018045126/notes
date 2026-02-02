package io.github.abc15018045126.sora.widget.schemes

/**
 * ColorScheme for editor
 * picked from Android Studio
 * Thanks to liyujiang-gzu (GitHub @liyujiang-gzu)
 */
class SchemeDarcula : EditorColorScheme(true) {

    override fun applyDefault() {
        super.applyDefault()
        setColor(ANNOTATION, 0xffbbb529.toInt())
        setColor(FUNCTION_NAME, 0xffffffff.toInt())
        setColor(IDENTIFIER_NAME, 0xffffffff.toInt())
        setColor(IDENTIFIER_VAR, 0xff9876aa.toInt())
        setColor(LITERAL, 0xff6a8759.toInt())
        setColor(OPERATOR, 0xffffffff.toInt())
        setColor(COMMENT, 0xff808080.toInt())
        setColor(KEYWORD, 0xffcc7832.toInt())
        setColor(WHOLE_BACKGROUND, 0xff2b2b2b.toInt())
        setColor(COMPLETION_WND_BACKGROUND, 0xff2b2b2b.toInt())
        setColor(COMPLETION_WND_CORNER, 0xff999999.toInt())
        setColor(TEXT_NORMAL, 0xffffffff.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xff313335.toInt())
        setColor(LINE_NUMBER, 0xff606366.toInt())
        setColor(LINE_NUMBER_CURRENT, 0xff606366.toInt())
        setColor(LINE_DIVIDER, 0xff606366.toInt())
        setColor(SCROLL_BAR_THUMB, 0xffa6a6a6.toInt())
        setColor(SCROLL_BAR_THUMB_PRESSED, 0xff565656.toInt())
        setColor(SELECTED_TEXT_BACKGROUND, 0xff3676b8.toInt())
        setColor(MATCHED_TEXT_BACKGROUND, 0xff32593d.toInt())
        setColor(CURRENT_LINE, 0xff323232.toInt())
        setColor(SELECTION_INSERT, 0xffffffff.toInt())
        setColor(SELECTION_HANDLE, 0xffffffff.toInt())
        setColor(BLOCK_LINE, 0xff575757.toInt())
        setColor(BLOCK_LINE_CURRENT, 0xdd575757.toInt())
        setColor(NON_PRINTABLE_CHAR, 0xffdddddd.toInt())
        setColor(TEXT_SELECTED, 0xffffffff.toInt())
    }
}

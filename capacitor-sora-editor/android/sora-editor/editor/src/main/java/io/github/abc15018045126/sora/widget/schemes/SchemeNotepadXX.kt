package io.github.abc15018045126.sora.widget.schemes

/**
 * ColorScheme for editor
 * picked from Notepad++ v7.8.1
 * Thanks to liyujiang-gzu (GitHub @liyujiang-gzu)
 */
class SchemeNotepadXX : EditorColorScheme() {

    override fun applyDefault() {
        super.applyDefault()
        setColor(ANNOTATION, 0xff0000ff.toInt())
        setColor(FUNCTION_NAME, 0xff000000.toInt())
        setColor(IDENTIFIER_NAME, 0xff000000.toInt())
        setColor(IDENTIFIER_VAR, 0xff000000.toInt())
        setColor(LITERAL, 0xff808080.toInt())
        setColor(OPERATOR, 0xff0000ff.toInt())
        setColor(COMMENT, 0xff008000.toInt())
        setColor(KEYWORD, 0xff8000ff.toInt())
        setColor(WHOLE_BACKGROUND, 0xffffffff.toInt())
        setColor(TEXT_NORMAL, 0xff000000.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xffe4e4e4.toInt())
        setColor(LINE_NUMBER, 0xff808080.toInt())
        setColor(LINE_NUMBER_CURRENT, 0xff808080.toInt())
        setColor(SELECTED_TEXT_BACKGROUND, 0xff75d975.toInt())
        setColor(MATCHED_TEXT_BACKGROUND, 0xffc0c0c0.toInt())
        setColor(CURRENT_LINE, 0xffe8e8ff.toInt())
        setColor(SELECTION_INSERT, 0xff8000ff.toInt())
        setColor(SELECTION_HANDLE, 0xff8000ff.toInt())
        setColor(BLOCK_LINE, 0xffc0c0c0.toInt())
        setColor(BLOCK_LINE_CURRENT, 0)
        setColor(TEXT_SELECTED, 0xffffffff.toInt())
    }
}

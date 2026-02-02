package io.github.abc15018045126.sora.widget.schemes

/**
 * ColorScheme for editor
 * picked from Eclipse IDE for Java Developers Version 2019-12 (4.14.0)
 * Thanks to liyujiang-gzu (GitHub @liyujiang-gzu)
 */
class SchemeEclipse : EditorColorScheme() {

    override fun applyDefault() {
        super.applyDefault()
        setColor(ANNOTATION, 0xff646464.toInt())
        setColor(FUNCTION_NAME, 0xff000000.toInt())
        setColor(IDENTIFIER_NAME, 0xff000000.toInt())
        setColor(IDENTIFIER_VAR, 0xffb8633e.toInt())
        setColor(LITERAL, 0xff2a00ff.toInt())
        setColor(OPERATOR, 0xff3a0000.toInt())
        setColor(COMMENT, 0xff3f7f5f.toInt())
        setColor(KEYWORD, 0xff7f0074.toInt())
        setColor(WHOLE_BACKGROUND, 0xffffffff.toInt())
        setColor(TEXT_NORMAL, 0xff000000.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xffffffff.toInt())
        setColor(LINE_NUMBER, 0xff787878.toInt())
        setColor(LINE_NUMBER_CURRENT, 0xff787878.toInt())
        setColor(SELECTED_TEXT_BACKGROUND, 0xff3399ff.toInt())
        setColor(MATCHED_TEXT_BACKGROUND, 0xffd4d4d4.toInt())
        setColor(CURRENT_LINE, 0xffe8f2fe.toInt())
        setColor(SELECTION_INSERT, 0xff03ebeb.toInt())
        setColor(SELECTION_HANDLE, 0xff03ebeb.toInt())
        setColor(BLOCK_LINE, 0xffd8d8d8.toInt())
        setColor(BLOCK_LINE_CURRENT, 0)
        setColor(TEXT_SELECTED, 0xffffffff.toInt())
    }
}

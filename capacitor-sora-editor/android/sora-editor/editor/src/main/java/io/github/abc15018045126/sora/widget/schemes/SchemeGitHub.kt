package io.github.abc15018045126.sora.widget.schemes

/**
 * ColorScheme for editor
 * picked from GitHub site
 * Thanks to liyujiang-gzu (GitHub @liyujiang-gzu)
 */
class SchemeGitHub : EditorColorScheme() {

    override fun applyDefault() {
        super.applyDefault()
        setColor(ANNOTATION, 0xff6f42c1.toInt())
        setColor(FUNCTION_NAME, 0xff24292e.toInt())
        setColor(IDENTIFIER_NAME, 0xff24292e.toInt())
        setColor(IDENTIFIER_VAR, 0xff24292e.toInt())
        setColor(LITERAL, 0xff032f62.toInt())
        setColor(OPERATOR, 0xff005cc5.toInt())
        setColor(COMMENT, 0xff6a737d.toInt())
        setColor(KEYWORD, 0xffde3a49.toInt())
        setColor(WHOLE_BACKGROUND, 0xffffffff.toInt())
        setColor(TEXT_NORMAL, 0xff24292e.toInt())
        setColor(LINE_NUMBER_BACKGROUND, 0xffffffff.toInt())
        setColor(LINE_NUMBER, 0xffbec0c1.toInt())
        setColor(LINE_NUMBER_CURRENT, 0xffbec0c1.toInt())
        setColor(SELECTION_INSERT, 0xffc7edcc.toInt())
        setColor(SELECTION_HANDLE, 0xffc7edcc.toInt())
    }
}

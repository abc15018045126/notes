package io.github.abc15018045126.sora.langs.textmate

import android.graphics.Color
import androidx.annotation.NonNull
import io.github.abc15018045126.sora.langs.textmate.registry.ThemeRegistry
import io.github.abc15018045126.sora.langs.textmate.registry.model.ThemeModel
import io.github.abc15018045126.sora.langs.textmate.utils.ColorUtils
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import org.eclipse.tm4e.core.internal.theme.Theme
import org.eclipse.tm4e.core.internal.theme.raw.IRawTheme
import org.eclipse.tm4e.core.internal.theme.raw.RawTheme
import org.eclipse.tm4e.core.registry.IThemeSource

class TextMateColorScheme(
    private val themeRegistry: ThemeRegistry, 
    themeModel: ThemeModel
) : EditorColorScheme(), ThemeRegistry.ThemeChangeListener {

    private var theme: Theme? = null
    private var rawTheme: IRawTheme? = null

    @Deprecated("Use ThemeModel")
    private var themeSource: IThemeSource? = null
    private var currentTheme: ThemeModel = themeModel

    init {
        setTheme(themeModel)
    }

    fun setTheme(themeModel: ThemeModel) {
        currentTheme = themeModel
        super.colors.clear()
        this.rawTheme = themeModel.rawTheme
        this.theme = themeModel.theme
        @Suppress("DEPRECATION")
        this.themeSource = themeModel.themeSource
        applyDefault()
    }

    override fun onChangeTheme(newTheme: ThemeModel) {
        setTheme(newTheme)
    }

    override fun applyDefault() {
        super.applyDefault()

        if (!themeRegistry.hasListener(this)) {
            themeRegistry.addListener(this)
        }

        val raw = rawTheme ?: return
        val settings = raw.settings

        if (settings == null) {
            val rawSubTheme = (raw as? RawTheme)?.get("colors") as? RawTheme
            if (rawSubTheme != null) {
                applyVSCTheme(rawSubTheme)
            }
        } else {
            val settingsList = settings as? List<*>
            var rawSubTheme = settingsList?.get(0) as? RawTheme
            if (rawSubTheme != null) {
                rawSubTheme = rawSubTheme.getSetting() as? RawTheme
            }
            if (rawSubTheme != null) {
                applyTMTheme(rawSubTheme)
            }
        }
    }

    private fun applyVSCTheme(raw: RawTheme) {
        setColor(LINE_DIVIDER, Color.TRANSPARENT)

        (raw["editorCursor.foreground"] as? String)?.let {
            setColor(SELECTION_INSERT, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editor.selectionBackground"] as? String)?.let {
            setColor(SELECTED_TEXT_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editorWhitespace.foreground"] as? String)?.let {
            setColor(NON_PRINTABLE_CHAR, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editor.lineHighlightBackground"] as? String)?.let {
            setColor(CURRENT_LINE, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editor.background"] as? String)?.let {
            val color = ColorUtils.parseRGBAToARGB(it)
            setColor(WHOLE_BACKGROUND, color)
            setColor(LINE_NUMBER_BACKGROUND, color)
        }

        (raw["editorLineNumber.foreground"] as? String)?.let {
            setColor(LINE_NUMBER, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editorLineNumber.activeForeground"] as? String)?.let {
            setColor(LINE_NUMBER_CURRENT, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editor.foreground"] as? String)?.let {
            setColor(TEXT_NORMAL, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["highlightedDelimitersForeground"] as? String)?.let {
            setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["tooltipBackground"] as? String)?.let {
            setColor(DIAGNOSTIC_TOOLTIP_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["tooltipBriefMessageColor"] as? String)?.let {
            setColor(DIAGNOSTIC_TOOLTIP_BRIEF_MSG, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["tooltipDetailedMessageColor"] as? String)?.let {
            setColor(DIAGNOSTIC_TOOLTIP_DETAILED_MSG, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["tooltipActionColor"] as? String)?.let {
            setColor(DIAGNOSTIC_TOOLTIP_ACTION, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editorSuggestWidget.highlightForeground"] as? String)?.let {
            setColor(COMPLETION_WND_TEXT_MATCHED, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editorSuggestWidget.background"] as? String)?.let {
            setColor(COMPLETION_WND_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editorSuggestWidget.foreground"] as? String)?.let {
            setColor(COMPLETION_WND_TEXT_PRIMARY, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editorSuggestWidget.selectedBackground"] as? String)?.let {
            setColor(COMPLETION_WND_ITEM_CURRENT, ColorUtils.parseRGBAToARGB(it))
        }

        val editorIndentGuideBackground = raw["editorIndentGuide.background"] as? String
        val blockLineColor = ((getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2) and 0x00FFFFFF or -0x78000000 // 0x88000000

        if (editorIndentGuideBackground != null) {
            setColor(BLOCK_LINE, ColorUtils.parseRGBAToARGB(editorIndentGuideBackground))
        } else {
            setColor(BLOCK_LINE, blockLineColor)
        }

        val editorIndentGuideActiveBackground = raw["editorIndentGuide.activeBackground"] as? String
        if (editorIndentGuideActiveBackground != null) {
            setColor(BLOCK_LINE_CURRENT, ColorUtils.parseRGBAToARGB(editorIndentGuideActiveBackground))
        } else {
            setColor(BLOCK_LINE_CURRENT, blockLineColor or -0x1000000)
        }

        (raw["editor.wordHighlightStrongBackground"] as? String)?.let {
            setColor(TEXT_HIGHLIGHT_STRONG_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editor.wordHighlightBackground"] as? String)?.let {
            setColor(TEXT_HIGHLIGHT_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["editor.findMatchBackground"] as? String)?.let {
            setColor(MATCHED_TEXT_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }
    }

    override fun isDark(): Boolean {
        if (super.isDark()) {
            return true
        }
        return currentTheme.isDark
    }

    private fun applyTMTheme(raw: RawTheme) {
        setColor(LINE_DIVIDER, Color.TRANSPARENT)

        (raw["caret"] as? String)?.let {
            setColor(SELECTION_INSERT, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["selection"] as? String)?.let {
            setColor(SELECTED_TEXT_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["invisibles"] as? String)?.let {
            setColor(NON_PRINTABLE_CHAR, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["lineHighlight"] as? String)?.let {
            setColor(CURRENT_LINE, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["background"] as? String)?.let {
            val color = ColorUtils.parseRGBAToARGB(it)
            setColor(WHOLE_BACKGROUND, color)
            setColor(LINE_NUMBER_BACKGROUND, color)
        }

        (raw["foreground"] as? String)?.let {
            setColor(TEXT_NORMAL, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["highlightedDelimitersForeground"] as? String)?.let {
            setColor(HIGHLIGHTED_DELIMITERS_FOREGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["completionWindowBackground"] as? String)?.let {
            setColor(COMPLETION_WND_BACKGROUND, ColorUtils.parseRGBAToARGB(it))
        }

        (raw["completionWindowBackgroundCurrent"] as? String)?.let {
            setColor(COMPLETION_WND_ITEM_CURRENT, ColorUtils.parseRGBAToARGB(it))
        }

        val blockLineColor = ((getColor(WHOLE_BACKGROUND) + getColor(TEXT_NORMAL)) / 2) and 0x00FFFFFF or -0x78000000
        setColor(BLOCK_LINE, blockLineColor)
        setColor(BLOCK_LINE_CURRENT, blockLineColor or -0x1000000)
    }

    override fun getColor(type: Int): Int {
        if (type >= 255) {
            val superColor = super.getColor(type)
            if (superColor == 0) {
                val t = theme
                if (t != null) {
                    val colorString = try {
                        t.getColor(type - 255)
                    } catch (e: IndexOutOfBoundsException) {
                        null
                    }
                    val newColor = if (colorString != null && !colorString.equals("@default", ignoreCase = true)) {
                        ColorUtils.parseRGBAToARGB(colorString)
                    } else {
                        super.getColor(TEXT_NORMAL)
                    }
                    colors.put(type, newColor)
                    return newColor
                }
                return super.getColor(TEXT_NORMAL)
            } else {
                return superColor
            }
        }
        return super.getColor(type)
    }

    override fun detachEditor(editor: CodeEditor) {
        super.detachEditor(editor)
        themeRegistry.removeListener(this)
    }

    override fun attachEditor(editor: CodeEditor) {
        super.attachEditor(editor)
        try {
            themeRegistry.loadTheme(currentTheme)
        } catch (e: Exception) {
            // ignore
        }
        setTheme(currentTheme)
        editor.rerunAnalysis()
    }

    @Deprecated("Use ThemeModel")
    fun getRawTheme(): IRawTheme? = rawTheme

    @Deprecated("Use ThemeModel")
    fun getThemeSource(): IThemeSource? = themeSource

    companion object {
        @JvmStatic
        @Deprecated("Use ThemeModel constructor")
        fun create(themeSource: IThemeSource): TextMateColorScheme {
            return create(ThemeModel(themeSource))
        }

        @JvmStatic
        fun create(themeModel: ThemeModel): TextMateColorScheme {
            return create(ThemeRegistry.getInstance(), themeModel)
        }

        @JvmStatic
        fun create(themeRegistry: ThemeRegistry): TextMateColorScheme {
            return create(themeRegistry, themeRegistry.currentThemeModel)
        }

        @JvmStatic
        fun create(themeRegistry: ThemeRegistry, themeModel: ThemeModel): TextMateColorScheme {
            return TextMateColorScheme(themeRegistry, themeModel)
        }
    }
}

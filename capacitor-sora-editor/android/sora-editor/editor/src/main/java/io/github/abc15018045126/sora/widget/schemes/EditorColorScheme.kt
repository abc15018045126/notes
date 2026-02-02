package io.github.abc15018045126.sora.widget.schemes

import android.graphics.Color
import android.util.SparseIntArray
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.widget.CodeEditor
import java.lang.ref.WeakReference
import java.util.Objects

/**
 * This class manages the colors of editor.
 * You can use color IDs that are not in pre-defined id pool for custom languages. We recommend
 *  adding a base offset for your custom color IDs. For example, first custom color ID is 256. This
 *   leaves enough space for editor's future built-in colors.
 *
 * This is also the default color scheme of editor.
 * Be careful to change this class, because this can cause its
 * subclasses behave differently and some subclasses did not apply
 * their default colors to some color ids. So change to this can cause
 * sub themes to change as well.
 *
 * Typically, you can use this class to set color of editor directly
 * with [setColor] in a thread with looper.
 *
 * However, we also accept you to extend this class to customize
 * your own ColorScheme to use different default colors.
 * Subclasses are expected to override [applyDefault]
 * to define colors, though other methods are not final.
 * After overriding this method, you will have to call super class's
 * applyDefault() and then a series of [setColor] calls
 * to apply your colors.
 *
 * Note that new colors can be added in newer version of editor,
 * it is dangerous not to call super.applyDefault(), which can cause
 * newer editor works wrongly.
 *
 * For more pre-defined color schemes, please turn to package io.github.abc15018045126.editor.widget.schemes
 *
 * Thanks to liyujiang-gzu (GitHub @liyujiang-gzu) for contribution to color schemes
 *
 * @author Rose
 */
open class EditorColorScheme @JvmOverloads constructor(private val dark: Boolean = false) {

    /**
     * Real color saver
     */
    @JvmField
    protected val colors: SparseIntArray = SparseIntArray()

    /**
     * Host editor object
     */
    private val editors: MutableList<WeakReference<CodeEditor>> = mutableListOf()

    /**
     * Create a new ColorScheme for the given editor
     *
     * @param editor Host editor
     */
    constructor(editor: CodeEditor) : this(false) {
        attachEditor(editor)
    }

    init {
        applyDefault()
    }

    /**
     * Subscribe changes
     *
     * Called by editor
     */
    @UnsupportedUserUsage
    open fun attachEditor(@NonNull editor: CodeEditor) {
        for (ref in editors) {
            if (ref.get() == editor) {
                return
            }
        }
        editors.add(WeakReference(editor))
        editor.onColorFullUpdate()
    }

    /**
     * Unsubscribe changes
     */
    @UnsupportedUserUsage
    open fun detachEditor(@NonNull editor: CodeEditor) {
        val itr = editors.iterator()
        while (itr.hasNext()) {
            if (itr.next().get() == editor) {
                itr.remove()
                break
            }
        }
    }

    /**
     * Apply default colors
     */
    open fun applyDefault() {
        for (i in START_COLOR_ID..END_COLOR_ID) {
            applyDefault(i)
        }
    }

    /**
     * Apply default color for the given type
     *
     * @param type The type
     */
    private fun applyDefault(type: Int) {
        var color = colors.get(type)
        when (type) {
            LINE_NUMBER, LINE_NUMBER_CURRENT -> color = 0xFF505050.toInt()
            LINE_NUMBER_BACKGROUND, LINE_DIVIDER -> color = 0xeeeeeeee.toInt()
            STATIC_SPAN_BACKGROUND, WHOLE_BACKGROUND, COMPLETION_WND_BACKGROUND, COMPLETION_WND_CORNER -> {
                color = if (isDark()) BACKGROUND_COLOR_DARK else 0xffffffff.toInt()
            }
            LINE_NUMBER_PANEL_TEXT -> color = 0xffffffff.toInt()
            OPERATOR -> color = 0xFF0066D6.toInt()
            STATIC_SPAN_FOREGROUND, TEXT_NORMAL -> color = 0xFF333333.toInt()
            SELECTION_INSERT -> color = 0xdd536dfe.toInt()
            UNDERLINE -> color = 0xff000000.toInt()
            SELECTION_HANDLE -> color = 0xff536dfe.toInt()
            ANNOTATION, SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER, HOVER_TEXT_HIGHLIGHTED, IDENTIFIER_NAME -> {
                color = 0xFF03A9F4.toInt()
            }
            CURRENT_LINE -> color = 0x10000000
            SELECTED_TEXT_BACKGROUND, FUNCTION_CHAR_BACKGROUND_STROKE -> color = 0x2D3F51B5
            KEYWORD -> color = 0xFF2196F3.toInt()
            COMMENT -> color = 0xffa8a8a8.toInt()
            LITERAL -> color = 0xFF008080.toInt()
            SCROLL_BAR_THUMB -> color = 0xffd8d8d8.toInt()
            SCROLL_BAR_THUMB_PRESSED -> color = 0xFF27292A.toInt()
            BLOCK_LINE -> color = 0xffdddddd.toInt()
            LINE_BLOCK_LABEL, SCROLL_BAR_TRACK, TEXT_SELECTED, STRIKETHROUGH -> color = 0
            LINE_NUMBER_PANEL -> color = 0xdd000000.toInt()
            BLOCK_LINE_CURRENT, SIDE_BLOCK_LINE -> color = 0xff999999.toInt()
            IDENTIFIER_VAR -> color = 0xff546e7a.toInt()
            FUNCTION_NAME -> color = 0xffe040fb.toInt()
            MATCHED_TEXT_BACKGROUND -> color = 0xffffff00.toInt()
            COMPLETION_WND_TEXT_MATCHED -> color = 0xFF4daafc.toInt()
            NON_PRINTABLE_CHAR -> color = 0xeecccccc.toInt()
            PROBLEM_ERROR -> color = 0xaaff0000.toInt()
            PROBLEM_WARNING -> color = 0xaafff100.toInt()
            PROBLEM_TYPO -> color = 0x6600ff11
            HIGHLIGHTED_DELIMITERS_FOREGROUND, HIGHLIGHTED_DELIMITERS_UNDERLINE -> color = 0
            HIGHLIGHTED_DELIMITERS_BACKGROUND -> color = 0x1D000000
            HIGHLIGHTED_DELIMITERS_BORDER -> color = 0xff3f51b5.toInt()
            COMPLETION_WND_TEXT_PRIMARY, COMPLETION_WND_TEXT_SECONDARY, TEXT_INLAY_HINT_FOREGROUND -> {
                color = if (isDark()) 0xffffffff.toInt() else 0xff000000.toInt()
            }
            COMPLETION_WND_ITEM_CURRENT -> color = 0xffeeeeee.toInt()
            SNIPPET_BACKGROUND_EDITING -> color = 0xffcccccc.toInt()
            SNIPPET_BACKGROUND_RELATED -> 0xaadddddd.toInt()
            SNIPPET_BACKGROUND_INACTIVE -> 0x66dddddd
            SIGNATURE_TEXT_NORMAL, HOVER_TEXT_NORMAL -> color = if (isDark()) 0xffeeeeee.toInt() else 0xff000000.toInt()
            STICKY_SCROLL_DIVIDER -> 0x99eeeeee.toInt()
            TEXT_INLAY_HINT_BACKGROUND -> color = if (isDark()) 0x1deeeeee.toInt() else 0x1D000000
            HARD_WRAP_MARKER -> color = if (!isDark()) 0xffeeeeee.toInt() else 0x1D000000
            DIAGNOSTIC_TOOLTIP_BRIEF_MSG -> color = if (isDark()) PRIMARY_TEXT_COLOR_DEFAULT_DARK else PRIMARY_TEXT_COLOR_DEFAULT_LIGHT
            DIAGNOSTIC_TOOLTIP_DETAILED_MSG -> color = if (isDark()) SECONDARY_TEXT_COLOR_DARK else SECONDARY_TEXT_COLOR_LIGHT
            SIGNATURE_BACKGROUND, HOVER_BACKGROUND, DIAGNOSTIC_TOOLTIP_BACKGROUND, TEXT_ACTION_WINDOW_BACKGROUND -> {
                color = if (isDark()) BACKGROUND_COLOR_DARK else BACKGROUND_COLOR_LIGHT
            }
            TEXT_ACTION_WINDOW_ICON_COLOR -> color = if (isDark()) 0xffeeeeee.toInt() else Color.GRAY
            HOVER_BORDER, SIGNATURE_BORDER -> color = 0xff999999.toInt()
            DIAGNOSTIC_TOOLTIP_ACTION -> color = 0xff42A5F5.toInt()
            TEXT_HIGHLIGHT_STRONG_BACKGROUND -> color = if (isDark()) 0xB8004972.toInt() else 0x400e639c
            TEXT_HIGHLIGHT_BACKGROUND -> color = if (isDark()) 0xB8575757.toInt() else 0x40575757
        }
        setColor(type, color)
    }

    /**
     * Apply a new color for the given type
     *
     * @param type  The type
     * @param color New color
     */
    open fun setColor(type: Int, color: Int) {
        //Do not change if the old value is the same as new value
        //due to avoid unnecessary invalidate() calls
        val old = getColor(type)
        if (old == color) {
            return
        }

        colors.put(type, color)

        //Notify the editor
        val itr = editors.iterator()
        while (itr.hasNext()) {
            val editor = itr.next().get()
            if (editor == null) {
                itr.remove()
            } else {
                editor.onColorUpdated(type)
            }
        }
    }

    /**
     * Get color by type
     *
     * @param type The type
     * @return The color for type
     */
    open fun getColor(type: Int): Int {
        return colors.get(type)
    }

    /**
     * Check whether this color scheme is dark
     */
    open fun isDark(): Boolean {
        return dark
    }

    companion object {
        //----------------Issue colors----------------
        const val PROBLEM_TYPO = 37
        const val PROBLEM_WARNING = 36
        const val PROBLEM_ERROR = 35

        //-----------------Highlight colors-----------
        const val ATTRIBUTE_VALUE = 34
        const val ATTRIBUTE_NAME = 33
        const val HTML_TAG = 32
        const val ANNOTATION = 28
        const val FUNCTION_NAME = 27
        const val IDENTIFIER_NAME = 26
        const val IDENTIFIER_VAR = 25
        const val LITERAL = 24
        const val OPERATOR = 23
        const val COMMENT = 22
        const val KEYWORD = 21

        //-------------View colors---------------------
        const val STICKY_SCROLL_DIVIDER = 62

        /**
         * Color for text strikethrough. If value is 0, text color of that region will be used.
         */
        const val STRIKETHROUGH = 57

        /**
         * Alias for [STRIKETHROUGH]
         */
        const val STRIKE_THROUGH = STRIKETHROUGH
        const val DIAGNOSTIC_TOOLTIP_ACTION = 56
        const val DIAGNOSTIC_TOOLTIP_DETAILED_MSG = 55
        const val DIAGNOSTIC_TOOLTIP_BRIEF_MSG = 54
        const val DIAGNOSTIC_TOOLTIP_BACKGROUND = 53
        const val FUNCTION_CHAR_BACKGROUND_STROKE = 52
        const val HARD_WRAP_MARKER = 51
        const val TEXT_INLAY_HINT_FOREGROUND = 50
        const val TEXT_INLAY_HINT_BACKGROUND = 49
        const val SNIPPET_BACKGROUND_EDITING = 48
        const val SNIPPET_BACKGROUND_RELATED = 47
        const val SNIPPET_BACKGROUND_INACTIVE = 46
        const val SIDE_BLOCK_LINE = 38
        const val NON_PRINTABLE_CHAR = 31

        /**
         * Use zero if the text color should not be changed
         */
        const val TEXT_SELECTED = 30
        const val MATCHED_TEXT_BACKGROUND = 29
        const val MATCHED_TEXT_BORDER = 78
        const val COMPLETION_WND_CORNER = 20
        const val COMPLETION_WND_BACKGROUND = 19
        const val COMPLETION_WND_TEXT_MATCHED = 67
        const val COMPLETION_WND_TEXT_PRIMARY = 42
        const val COMPLETION_WND_TEXT_SECONDARY = 43
        const val COMPLETION_WND_ITEM_CURRENT = 44

        /**
         * No longer supported
         */
        const val LINE_BLOCK_LABEL = 18

        const val TEXT_HIGHLIGHT_STRONG_BACKGROUND = 73
        const val TEXT_HIGHLIGHT_STRONG_BORDER = 76
        const val TEXT_HIGHLIGHT_BACKGROUND = 74
        const val TEXT_HIGHLIGHT_BORDER = 77
        const val HIGHLIGHTED_DELIMITERS_BACKGROUND = 41
        const val HIGHLIGHTED_DELIMITERS_UNDERLINE = 40
        const val HIGHLIGHTED_DELIMITERS_FOREGROUND = 39
        const val HIGHLIGHTED_DELIMITERS_BORDER = 75
        const val LINE_NUMBER_PANEL_TEXT = 17
        const val LINE_NUMBER_PANEL = 16
        const val BLOCK_LINE_CURRENT = 15
        const val BLOCK_LINE = 14
        const val SCROLL_BAR_TRACK = 13
        const val SCROLL_BAR_THUMB_PRESSED = 12
        const val SCROLL_BAR_THUMB = 11
        const val UNDERLINE = 10
        const val CURRENT_LINE = 9
        const val CURRENT_ROW_BORDER = 80
        const val SELECTION_HANDLE = 8
        const val SELECTION_INSERT = 7
        const val SELECTED_TEXT_BACKGROUND = 6
        const val SELECTED_TEXT_BORDER = 79
        const val TEXT_NORMAL = 5
        const val WHOLE_BACKGROUND = 4
        const val LINE_NUMBER_BACKGROUND = 3
        const val LINE_NUMBER_CURRENT = 45
        const val LINE_NUMBER = 2
        const val LINE_DIVIDER = 1

        const val SIGNATURE_TEXT_NORMAL = 58
        const val SIGNATURE_TEXT_HIGHLIGHTED_PARAMETER = 59
        const val HOVER_TEXT_NORMAL = 68
        const val HOVER_TEXT_HIGHLIGHTED = 72
        const val HOVER_BACKGROUND = 69
        const val HOVER_BORDER = 70

        const val STATIC_SPAN_BACKGROUND = 63
        const val STATIC_SPAN_FOREGROUND = 64

        const val SIGNATURE_BACKGROUND = 60
        const val SIGNATURE_BORDER = 71

        const val TEXT_ACTION_WINDOW_BACKGROUND = 65
        const val TEXT_ACTION_WINDOW_ICON_COLOR = 66

        /**
         * Min pre-defined color id
         */
        protected const val START_COLOR_ID = 1

        /**
         * Max pre-defined color id
         */
        protected const val END_COLOR_ID = 80

        private const val PRIMARY_TEXT_COLOR_DEFAULT_LIGHT = 0xff424242.toInt()
        private const val PRIMARY_TEXT_COLOR_DEFAULT_DARK = 0xfff5f5f5.toInt()
        private const val BACKGROUND_COLOR_LIGHT = 0xfffefefe.toInt()
        private const val BACKGROUND_COLOR_DARK = 0xff212121.toInt()
        private const val SECONDARY_TEXT_COLOR_LIGHT = 0xff616161.toInt()
        private const val SECONDARY_TEXT_COLOR_DARK = 0xffeeeeee.toInt()

        private var globalDefault = EditorColorScheme()

        /**
         * Get global default color scheme.
         */
        @JvmStatic
        @NonNull
        fun getDefault(): EditorColorScheme {
            return globalDefault
        }

        /**
         * Set global default color scheme. Newly created editor will use the new default color scheme.
         *
         * @param colorScheme new global color scheme, or null for restoring to built-in default
         */
        @JvmStatic
        fun setDefault(@Nullable colorScheme: EditorColorScheme?) {
            setDefault(colorScheme, false)
        }

        /**
         * Set global default color scheme and optionally update existing editors that are using default
         * color scheme.
         *
         * @param colorScheme   new global color scheme, or null for restoring to built-in default
         * @param updateEditors update existing editors that are using default color scheme
         */
        @JvmStatic
        fun setDefault(@Nullable colorScheme: EditorColorScheme?, updateEditors: Boolean) {
            var finalColorScheme = colorScheme
            if (finalColorScheme == null) {
                finalColorScheme = EditorColorScheme()
            }
            if (updateEditors) {
                val editors = globalDefault.editors.toTypedArray()
                for (ref in editors) {
                    val editor = ref.get()
                    editor?.colorScheme = finalColorScheme
                }
            }
            globalDefault = finalColorScheme
        }
    }
}

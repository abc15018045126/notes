package io.github.abc15018045126.sora.widget

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import android.content.ClipData
import android.graphics.Paint.FontMetricsInt
import android.graphics.Paint.FontMetrics
import android.graphics.Paint as AndroidPaint
import android.text.InputType
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle.HandleDescriptor

import android.view.*
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.EdgeEffect
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.Px
import io.github.abc15018045126.sora.I18nConfig
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.event.*
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.graphics.inlayHint.InlayHintRenderer
import io.github.abc15018045126.sora.graphics.inlayHint.InlayHintRendererProvider
import io.github.abc15018045126.sora.lang.EmptyLanguage
import io.github.abc15018045126.sora.lang.Language
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticsContainer
import io.github.abc15018045126.sora.lang.format.Formatter
import io.github.abc15018045126.sora.lang.styling.HighlightTextContainer
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHintsContainer
import io.github.abc15018045126.sora.text.*
import io.github.abc15018045126.sora.text.method.KeyMetaStates
import io.github.abc15018045126.sora.lang.styling.CodeBlock
import io.github.abc15018045126.sora.util.*
import io.github.abc15018045126.sora.widget.component.*
import io.github.abc15018045126.sora.widget.layout.Layout
import io.github.abc15018045126.sora.widget.rendering.RenderContext
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import io.github.abc15018045126.sora.widget.snippet.SnippetController
import io.github.abc15018045126.sora.widget.style.*
import io.github.abc15018045126.sora.widget.style.builtin.*
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import android.graphics.Canvas
import android.graphics.Rect
import android.content.res.Configuration
import android.os.TransactionTooLargeException
import android.widget.SearchView
import android.widget.EditText
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
// Removed duplicate InputType import
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.ExtractedText
import androidx.annotation.UiThread
import androidx.collection.IntSet
import androidx.collection.MutableIntSet
import io.github.abc15018045126.sora.lang.QuickQuoteHandler
import io.github.abc15018045126.sora.lang.QuickQuoteHandler.HandleResult
import io.github.abc15018045126.sora.widget.EditorSearcher.SearchOptions
import io.github.abc15018045126.sora.lang.analysis.StyleUpdateRange
import io.github.abc15018045126.sora.lang.styling.inlayHint.IntSetUpdateRange
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.widget.REGION_OUTBOUND
import io.github.abc15018045126.sora.widget.REGION_LINE_NUMBER
import io.github.abc15018045126.sora.widget.REGION_SIDE_ICON
import io.github.abc15018045126.sora.widget.REGION_DIVIDER_MARGIN
import io.github.abc15018045126.sora.widget.REGION_DIVIDER
import io.github.abc15018045126.sora.widget.REGION_TEXT
import io.github.abc15018045126.sora.widget.IN_BOUND
import io.github.abc15018045126.sora.widget.OUT_BOUND
import io.github.abc15018045126.sora.widget.DirectAccessProps.Companion.MOUSE_MODE_NEVER
import io.github.abc15018045126.sora.lang.styling.SpanFactory
import io.github.abc15018045126.sora.widget.layout.LineBreakLayout
import io.github.abc15018045126.sora.widget.layout.WordwrapLayout
import io.github.abc15018045126.sora.widget.layout.ViewMeasureHelper
import android.graphics.PointF
/**
 * CodeEditor is an editor that can highlight text regions by doing basic syntax analyzing
 * This project in [GitHub](https://github.com/abc15018045126/sora-editor)
 *
 *
 * Note:
 * Row and line are different in this editor
 * When we say 'row', it means a line displayed on screen. It can be a part of a line in the text object.
 * When we say 'line', it means a real line in the original text.
 *
 * @author abc15018045126
 */
@SuppressWarnings("unused")
open class CodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.codeEditorStyle,
    defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr, defStyleRes), ContentListener, Formatter.FormatResultReceiver,
    InlayHintRendererProvider {


    protected val keyEventHandler: EditorKeyEventHandler = EditorKeyEventHandler(this)
    protected var languageSymbolPairs: SymbolPairMatch? = null
    protected var textActionWindow: EditorTextActionWindow? = null
    protected var diagnosticTooltip: EditorDiagnosticTooltipWindow? = null
    protected var contextMenuCreator: EditorContextMenuCreator? = null
    internal var defaultSpans: MutableList<Span?> = ArrayList(2)
    protected var styleDelegate: EditorStyleDelegate? = null
    var startedActionMode: Int = 0
    var selectionAnchor: CharPosition? = null

    internal var inputConnection: EditorInputConnection? = null
    var eventManager: EventManager? = null

    private var _layout: Layout? = null
    var layout: Layout
        get() = _layout!!
        set(value) { _layout = value }

    private var _tabWidth = 0
    var tabWidth: Int
        get() = _tabWidth
        set(width) {
            if (width <= 0) {
                throw IllegalArgumentException("tab width must > 0")
            }
            if (_tabWidth != width) {
                _tabWidth = width
                if (!isWordwrap) {
                    invalidate()
                } else {
                    requestLayoutIfNeeded()
                    createLayout()
                    invalidate()
                }
            }
        }

    private var _blockIndex = -1
    /**
     * Get cursor code block index
     * 
     * @return index of cursor's code block
     */
    var blockIndex: Int
        get() = _blockIndex
        set(value) { _blockIndex = value }

    private var downX = 0
    private var inputType = 0
    private var nonPrintableOptions = 0
    private var completionWndPosMode = 0
    private var availableFloatArrayRegion: Long = 0

    /**
     * Get 1dp = ?px
     * 
     * @return 1dp in pixel
     */
    var dpUnit: Float = 0f
        private set
    private var _dividerWidth = 0f
    var dividerWidth: Float
        get() = _dividerWidth
        set(value) { 
            if (_dividerWidth != value) {
                _dividerWidth = value
                requestLayoutIfNeeded()
                invalidate()
            }
        }


    /**
     * @return Margin left of divider line
     * @see CodeEditor.setDividerMargin
     */
    @get:Px
    internal var dividerMarginLeft: Float = 0f
        set(value) {
            field = value
            requestLayoutIfNeeded()
            createLayout()
            invalidate()
        }

    /**
     * @return Margin right of divider line
     * @see CodeEditor.setDividerMargin
     */
    @get:Px
    internal var dividerMarginRight: Float = 0f
        set(value) {
            field = value
            requestLayoutIfNeeded()
            createLayout()
            invalidate()
        }

    fun getDividerMarginLeft(): Float {
        return dividerMarginLeft
    }

    fun getDividerMarginRight(): Float {
        return dividerMarginRight
    }

    var extraMarginRight: Float = 0f
        set(extraMarginRight) {
            field = extraMarginRight
            requestLayoutIfNeeded()
            createLayout()
            invalidate()
        }

    @get:Px
    var insertSelectionWidth: Float = 0f
        private set

    /**
     * Margin left for line number
     */
    private var blockLineWidth = 0f
    private var textBorderWidth = 0f
    private var verticalScrollFactor = 0f

    /**
     * @see .setLineInfoTextSize
     */
    var lineInfoTextSize: Float = 0f
        /**
         * Set text size for line info panel
         * 
         * @param size Text size for line information, **unit is SP**
         */
        set(size) {
            kotlin.require(!(size <= 0))
            field = size
        }
    private var lineSpacingMultiplier = 1f
    private var lineSpacingAdd = 0f
    private var wrapLineSpacingMultiplier = 1f
    private var wrapLineSpacingAdd = 0f
    private var lineNumberMarginLeft = 0f
    private var verticalExtraSpaceFactor = 0.5f
    private var waitForNextChange = false
    /**
     * @return Whether allow scaling
     * @see CodeEditor.setScalable
     */
    /**
     * Allow scale text size by thumb
     * 
     * @param scale Whether allow
     */
    var isScalable: Boolean = false
    private var _editable = true
    var isEditable: Boolean
        get() = _editable && !layoutBusy && !this.isFormatting
        set(editable) {
            this._editable = editable
            if (!isEditable) {
                hideSoftInput()
                snippetController!!.stopSnippet()
            }
        }

    private var undoEnabled = false
    private var mouseHover = false
    private var mouseButtonPressed = false
    private var lastAnchorIsSelLeft = false

    @Volatile
    internal var layoutBusy = false

    /**
     * Check if the editor is currently calculating layout (e.g. word wrap)
     */
    fun isLayoutBusy(): Boolean = layoutBusy

    /**
     * @return Enabled / disabled
     * @see CodeEditor.setDisplayLnPanel
     */
    var isDisplayLnPanel: Boolean = false
        /**
         * Whether display the line number panel beside vertical scroll bar
         * when the scroll bar is touched by user
         * 
         * @param this.isDisplayLnPanel Enabled / disabled
         */
        set(displayLnPanel) {
            field = displayLnPanel
            invalidate()
        }

    /**
     * @return position
     * @see CodeEditor.setLnPanelPosition
     */
    var lnPanelPosition: Int = 0
        /**
         * Set display position the line number panel beside vertical scroll bar <br></br>
         * Only TOP,CENTER and BOTTOM will be effective when position mode is follow.
         * 
         * @param position default TOP|RIGHT
         * @see io.github.abc15018045126.sora.widget.style.LineInfoPanelPosition
         */
        set(position) {
            field = position
            invalidate()
        }

    /**
     * @return LineInfoPanelPosition.FOLLOW or LineInfoPanelPosition.FIXED
     * @see CodeEditor.setLnPanelPosition
     */
    var lnPanelPositionMode: Int = 0
        /**
         * Set display position mode the line number panel beside vertical scroll bar
         * 
         * @param mode Default LineInfoPanelPosition.FOLLOW
         * @see io.github.abc15018045126.sora.widget.style.LineInfoPanelPositionMode
         */
        set(mode) {
            field = mode
            invalidate()
        }
    private var rejectComposingCount = 0

    /**
     * Check if the editor is released.
     * When an editor is released, you are unexpected to make any changes to it.
     */
    var isReleased: Boolean = false
        private set

    /**
     * Check whether line numbers are shown
     * 
     * @return The state of line number displaying
     */
    var isLineNumberEnabled: Boolean = false
        /**
         * Set whether we should display line numbers
         * 
         * @param this.isLineNumberEnabled The state of line number displaying
         */
        set(lineNumberEnabled) {
            if (lineNumberEnabled != field && isWordwrap) {
                createLayout()
            }
            field = lineNumberEnabled
            invalidate()
        }
    private var blockLineEnabled = false

    /**
     * @see CodeEditor.setInterceptParentHorizontalScrollIfNeeded
     */
    var isInterceptParentHorizontalScrollEnabled: Boolean = false
        private set
    private var highlightCurrentBlock = false

    /**
     * @see CodeEditor.setHighlightCurrentLine
     */
    var isHighlightCurrentLine: Boolean = false
        /**
         * Specify whether the editor should use a different color to draw
         * the background of current line
         */
        set(highlightCurrentLine) {
            field = highlightCurrentLine
            invalidate()
        }
    /**
     * {@inheritDoc}
     */
    /**
     * {@inheritDoc}
     */
    override fun isVerticalScrollBarEnabled(): Boolean = super.isVerticalScrollBarEnabled()
    override fun setVerticalScrollBarEnabled(enabled: Boolean) = super.setVerticalScrollBarEnabled(enabled)
    override fun isHorizontalScrollBarEnabled(): Boolean = super.isHorizontalScrollBarEnabled()
    override fun setHorizontalScrollBarEnabled(enabled: Boolean) = super.setHorizontalScrollBarEnabled(enabled)
    private var cursorAnimation = false
    private var initialPreviewLines = 20
    @JvmField var forceSyncBreakLines = false
    private var isLineNumberRightOfDivider = false

    /**
     * @see CodeEditor.setPinLineNumber
     */
    var isLineNumberPinned: Boolean = false
        private set

    private var _wordwrap = false
    var isWordwrap: Boolean
        get() = _wordwrap
        set(wordwrap) {
            setWordwrap(wordwrap, this.isAntiWordBreaking, this.isWordwrapRtlDisplaySupport)
        }

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if anti word breaking is enabled in wordwrap mode.
     */
    var isAntiWordBreaking: Boolean = false
        set(antiWordBreaking) {
            setWordwrap(this.isWordwrap, antiWordBreaking, this.isWordwrapRtlDisplaySupport)
        }

    /**
     * This only makes sense when wordwrap is enabled.
     * Checks if RTL-based text should display from right of the widget in wordwrap mode.
     */
    var isWordwrapRtlDisplaySupport: Boolean = false
        set(supportRtlRow) {
            setWordwrap(this.isWordwrap, this.isAntiWordBreaking, supportRtlRow)
        }

    /**
     * @see CodeEditor.setFirstLineNumberAlwaysVisible
     */
    var isFirstLineNumberAlwaysVisible: Boolean = false
        /**
         * Show first line number in screen in word wrap mode
         * 
         * @see CodeEditor.isFirstLineNumberAlwaysVisible
         */
        set(enabled) {
            field = enabled
            if (isWordwrap) {
                invalidate()
            }
        }

    /**
     * @see CodeEditor.setLigatureEnabled
     */
    var isLigatureEnabled: Boolean = false
        /**
         * Enable/disable ligature of all types(except 'rlig').
         */
        set(enabled) {
            field = enabled
            setFontFeatureSettings(if (enabled) null else "'liga' 0,'calt' 0,'hlig' 0,'dlig' 0,'clig' 0")
        }
    private var lastCursorState = false
    /**
     * Returns whether the cursor should stick to the text row while selecting the text
     * 
     * @see CodeEditor.setStickyTextSelection
     */
    /**
     * Whether the cursor should stick to the text row while selecting the text.
     * 
     * @param stickySelection value
     */
    var isStickyTextSelection: Boolean = false
    private var highlightBracketPair = false

    /**
     * Checks whether long select mode is started
     */
    var isInLongSelect: Boolean = false
        private set
    private var anyWrapContentSet = false
    private var _renderFunctionCharacters = false
    var isRenderFunctionCharacters: Boolean
        get() = _renderFunctionCharacters
        set(renderFunctionCharacters) {
            if (this._renderFunctionCharacters != renderFunctionCharacters) {
                this._renderFunctionCharacters = renderFunctionCharacters
                renderer.onTextStyleUpdate()
                requestLayoutIfNeeded()
                createLayout()
                invalidate()
            }
        }
    var isSoftKbdEnabled = false
    var isDisableSoftKbdOnHardKbd = false

    var handleDescLeft: SelectionHandleStyle.HandleDescriptor? = null
    var handleDescRight: SelectionHandleStyle.HandleDescriptor? = null
    var handleDescInsert: SelectionHandleStyle.HandleDescriptor? = null

    var clipboardManager: ClipboardManager? = null
    var inputMethodManager: InputMethodManager? = null

    private var _cursor: Cursor? = null
    var cursor: Cursor
        get() = _cursor!!
        set(value) { _cursor = value }
    private var _text: Content? = null
    var text: Content
        get() = _text!!
        set(value) {
            setText(value)
        }

    private var matrix: Matrix? = null
    var colorScheme: EditorColorScheme = EditorColorScheme.getDefault()
        set(colors) {
            field.detachEditor(this)
            field = colors
            // Automatically invoke scheme updating related methods
            colors.attachEditor(this)
            invalidate()
        }
    internal var lineNumberTipTextProvider: LineNumberTipTextProvider? = null
    internal var formatTip: String? = null
    private var _editorLanguage: Language? = null
    var editorLanguage: Language?
        get() = _editorLanguage
        set(lang) {
            var language: Language? = lang
            if (language == null) {
                language = EmptyLanguage()
            }

            // Destroy old one
            val old: Language? = _editorLanguage
            if (old != null) {
                val formatter = old.formatter
                formatter.setReceiver(null)
                formatter.destroy()
                old.analyzeManager.receiver = null
                old.analyzeManager.destroy()
                old.destroy()
            }

            styleDelegate!!.reset()
            this._editorLanguage = language
            this.textStyles = null
            this._diagnostics = null
            this._inlayHints = null
            _searcher.stopSearch()
            if (isAttachedToWindow) {
                language.analyzeManager.receiver = styleDelegate!!
            }
            if (_text != null) {
                _text!!.removeContentListener(this)
                _text!!.addContentListener(this)
            }

            // Symbol pairs
            if (languageSymbolPairs != null) {
                languageSymbolPairs?.parent = null
            }
            languageSymbolPairs = language.symbolPairs
            if (languageSymbolPairs == null) {
                Log.w(
                    LOG_TAG,
                    "Language(" + language.toString() + ") returned null for symbol pairs. It is a mistake."
                )
                languageSymbolPairs = SymbolPairMatch()
            }
            languageSymbolPairs?.parent = props!!.overrideSymbolPairs

            snippetController?.stopSnippet()
            renderContext?.invalidateRenderNodes()
            invalidate()

            // reset inlay hints (partially re-layout!! required)
            if (this.inlayHints != null) {
                inlayHints = null
            }
            if (this.highlightTextContainer != null) {
                highlightTextContainer = null
            }
        }

    private var diagnosticStyle: DiagnosticIndicatorStyle? = DiagnosticIndicatorStyle.WAVY_LINE
    private var lastMakeVisible: Long = 0
    private var completionWindow: EditorAutoCompletion? = null
    var touchHandler: EditorTouchEventHandler? = null

    internal var lineNumberAlign: android.graphics.Paint.Align? = null
    private var basicDetector: GestureDetector? = null
    private var scaleDetector: ScaleGestureDetector? = null
    private var anchorInfoBuilder: CursorAnchorInfo.Builder? = null
    var edgeEffectVertical: EdgeEffect? = null
    var edgeEffectHorizontal: EdgeEffect? = null

    private var extractingTextRequest: ExtractedTextRequest? = null
    protected lateinit var _searcher: EditorSearcher


    private var _cursorAnimator: CursorAnimator? = null

    var handleStyle: SelectionHandleStyle? = null

    internal var cursorBlink: CursorBlink? = null
    var props: DirectAccessProps? = null
    private var extraArguments: Bundle? = null
    private var textStyles: Styles? = null

    private var _diagnostics: DiagnosticsContainer? = null
    var diagnostics: DiagnosticsContainer?
        get() = _diagnostics
        set(diagnostics) {
            this._diagnostics = diagnostics
            invalidate()
        }

    private var _inlayHints: InlayHintsContainer? = null
    var inlayHints: InlayHintsContainer?
        get() = _inlayHints
        set(inlayHints) {
            val affectedLines: MutableIntSet = MutableIntSet()
            val oldInlayHints: InlayHintsContainer? = this._inlayHints
            if (oldInlayHints != null) {
                affectedLines.addAll(oldInlayHints.getLineNumbers())
            }
            if (inlayHints != null) {
                affectedLines.addAll(inlayHints.getLineNumbers())
            }
            this._inlayHints = inlayHints
            val range: IntSetUpdateRange = IntSetUpdateRange(affectedLines)
            if (!layoutBusy) {
                layout?.invalidateLines(range)
            } else {
                createLayout()
            }
            renderContext?.invalidateRenderNodes()
            invalidate()
        }

    private var _highlightTextContainer: HighlightTextContainer? = null
    var highlightTextContainer: HighlightTextContainer?
        get() = _highlightTextContainer
        set(highlightTexts) {
            val affectedLines = MutableIntSet()
            val oldHighlights = this._highlightTextContainer
            if (oldHighlights != null) {
                val lines = oldHighlights.getLineNumbers()
                for (line in lines) {
                    affectedLines.add(line)
                }
            }
            if (highlightTexts != null) {
                val lines = highlightTexts.getLineNumbers()
                for (line in lines) {
                    affectedLines.add(line)
                }
            }
            this._highlightTextContainer = highlightTexts
            val range = IntSetUpdateRange(affectedLines)
            if (!layoutBusy) {
                layout?.invalidateLines(range)
            } else {
                createLayout()
            }
            renderContext?.invalidateRenderNodes()
            invalidate()
        }
    var renderContext: RenderContext? = null

    lateinit var renderer: EditorRenderer
    private var hardwareAccAllowed = false
    private var scrollerFinalX = 0f
    private var scrollerFinalY = 0f
    private var verticalAbsorb = false
    private var horizontalAbsorb = false

    private var _lineSeparator: LineSeparator? = null
    var lineSeparator: LineSeparator?
        get() = _lineSeparator
        set(value) {
            kotlin.require(Objects.requireNonNull(value) !== LineSeparator.NONE)
            _lineSeparator = value
        }


    private var lastInsertion: TextRange? = null
    private var lastSelectedTextRange: TextRange? = null

    private var _snippetController: SnippetController? = null
    var snippetController: SnippetController?
        get() = _snippetController
        set(value) { _snippetController = value }

    private val inlayHintRendererMap: MutableMap<String?, InlayHintRenderer?> = HashMap()


    init {
        initialize(attrs, defStyleAttr, defStyleRes)
        applyAttributeSets(attrs, defStyleAttr, defStyleRes)
    }

    /**
     * Get builtin component so that you can enable/disable them or do some other actions.
     * 
     * @see io.github.abc15018045126.sora.widget.component
     */
    /**
     * Get builtin component so that you can enable/disable them or do some other actions.
     * 
     * @see io.github.abc15018045126.sora.widget.component
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : EditorBuiltinComponent> getComponent(clazz: Class<T>): T {
        if (clazz == EditorAutoCompletion::class.java) {
            return completionWindow as T
        } else if (clazz == Magnifier::class.java) {
            return touchHandler!!.editorMagnifier as T
        } else if (clazz == EditorTextActionWindow::class.java) {
            return textActionWindow as T
        } else if (clazz == EditorDiagnosticTooltipWindow::class.java) {
            return diagnosticTooltip as T
        } else if (clazz == EditorContextMenuCreator::class.java) {
            return contextMenuCreator as T
        } else {
            throw IllegalArgumentException("Unknown component type")
        }
    }

    /**
     * Replace the built-in component to the given one.
     * The new component's enabled state will extend the old one.
     * 
     * @param clazz       Built-in class type. Such as `EditorAutoCompletion.class`
     * @param replacement The new component to apply
     * @param <T>         Type of built-in component
    </T> */
    fun <T : EditorBuiltinComponent> replaceComponent(clazz: Class<T>, replacement: T) {
        val old: EditorBuiltinComponent = getComponent(clazz)
        val isEnabled = old.isEnabled
        old.isEnabled = false
        if (clazz == EditorAutoCompletion::class.java) {
            completionWindow = replacement as EditorAutoCompletion
        } else if (clazz == Magnifier::class.java) {
            touchHandler!!.editorMagnifier = replacement as Magnifier
        } else if (clazz == EditorTextActionWindow::class.java) {
            textActionWindow = replacement as EditorTextActionWindow
        } else if (clazz == EditorDiagnosticTooltipWindow::class.java) {
            diagnosticTooltip = replacement as EditorDiagnosticTooltipWindow
        } else if (clazz == EditorContextMenuCreator::class.java) {
            contextMenuCreator = replacement as EditorContextMenuCreator
        } else {
            throw IllegalArgumentException("Unknown component type")
        }
        replacement.isEnabled = isEnabled
    }

    fun registerInlayHintRenderers(vararg renderers: InlayHintRenderer) {
        var needLayout = false
        for (renderer in renderers) {
            val oldValue = inlayHintRendererMap.put(renderer.typeName, renderer)

            needLayout = needLayout or (oldValue != renderer)
        }
        if (needLayout) {
            createLayout()
        }
    }

    fun registerInlayHintRenderer(@NonNull renderer: InlayHintRenderer) {
        val oldValue = inlayHintRendererMap.put(renderer.typeName, renderer)

        if (oldValue != renderer) {
            createLayout()
        }
    }

    fun removeInlayHintRenderer(@NonNull renderer: InlayHintRenderer) {
        val oldValue = inlayHintRendererMap.get(renderer.typeName)
        if (oldValue == renderer) {
            inlayHintRendererMap.remove(renderer.typeName)

            createLayout()
        }
    }


    val inlayHintRenderers: List<InlayHintRenderer>
        get() = ArrayList(inlayHintRendererMap.values.filterNotNull())


    override fun getInlayHintRendererForType(type: String): InlayHintRenderer? {
        return inlayHintRendererMap.get(type)
    }


    fun getKeyMetaStates(): KeyMetaStates = keyEventHandler.getKeyMetaStates()

    /**
     * Get the touch event handler of the editor
     */
    @NonNull

    /**
     * Cancel the next animation for [CodeEditor.ensurePositionVisible]
     */
    @UnsupportedUserUsage
    open fun cancelAnimation() {
        lastMakeVisible = System.currentTimeMillis()
    }

    /**
     * Get the width of line number and divider line
     * 
     * @return The width
     */
    public fun measureTextRegionOffset(): Float {

        return if (this.isLineNumberEnabled) (measureLineNumber() + dividerMarginLeft + dividerMarginRight + dividerWidth +
                (if (renderer.hasSideHintIcons()) this.rowHeight else 0)) else dividerMarginLeft + dividerMarginRight
    }

    val leftHandleDescriptor: SelectionHandleStyle.HandleDescriptor?
        /**
         * Get the rect of left selection handle painted on view
         * 
         * @return Descriptor of left handle
         */
        get() = handleDescLeft

    val rightHandleDescriptor: SelectionHandleStyle.HandleDescriptor?
        /**
         * Get the rect of right selection handle painted on view
         * 
         * @return Descriptor of right handle
         */
        get() = handleDescRight

    /**
     * Get the character's x offset on view
     * 
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    fun getOffset(line: Int, column: Int): Float {
        return layout!!.getCharLayoutOffset(line, column)[1] + measureTextRegionOffset() - this.offsetX
    }

    /**
     * Get the character's x offset on view
     * 
     * @param line   The line position of character
     * @param column The column position of character
     * @return The x offset on view
     */
    fun getCharOffsetX(line: Int, column: Int): Float {
        return layout!!.getCharLayoutOffset(line, column)[1] + measureTextRegionOffset() - this.offsetX
    }

    /**
     * Get the character's y offset on view
     * 
     * @param line   The line position of character
     * @param column The column position of character
     * @return The y offset on view
     */
    fun getCharOffsetY(line: Int, column: Int): Float {
        return layout!!.getCharLayoutOffset(line, column)[0] - this.offsetY
    }

    /**
     * Prepare editor
     * 
     * 
     * Initialize variants
     */
    protected fun initialize(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        Log.v(
            LOG_TAG,
            COPYRIGHT
        )

        eventManager = EventManager()
        styleDelegate = EditorStyleDelegate(this)
        touchHandler = EditorTouchEventHandler(this)
        _text = Content("")
        _cursor = _text!!.cursor
        renderContext = RenderContext(this)
        renderer = onCreateRenderer()
        isRenderFunctionCharacters = true

        verticalScrollFactor = ViewUtils.getVerticalScrollFactor(getContext())
        lineSeparator = LineSeparator.LF
        lineNumberTipTextProvider = DefaultLineNumberTip
        formatTip = I18nConfig.getString(getContext(), R.string.sora_editor_editor_formatting)
        props = DirectAccessProps()
        dpUnit =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, Resources.getSystem().getDisplayMetrics()) / 10f
        dividerWidth = dpUnit
        insertSelectionWidth = dpUnit * 1.5f
        textBorderWidth = dpUnit
        extraArguments = Bundle()
        dividerMarginRight = dpUnit * 2
        dividerMarginLeft = dividerMarginRight

        matrix = Matrix()
        handleStyle = HandleStyleSideDrop(getContext())
        _searcher = EditorSearcher(this)
        _cursorAnimator = MoveCursorAnimator(this)
        cursorBlink = CursorBlink(this, DEFAULT_CURSOR_BLINK_PERIOD)
        setCursorBlinkPeriod(DEFAULT_CURSOR_BLINK_PERIOD)
        anchorInfoBuilder = CursorAnchorInfo.Builder()


        startedActionMode = ACTION_MODE_NONE
        setTextSize(DEFAULT_TEXT_SIZE.toFloat())
        this.lineInfoTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            DEFAULT_LINE_INFO_TEXT_SIZE.toFloat(),
            Resources.getSystem().getDisplayMetrics()
        )
        colorScheme = EditorColorScheme.getDefault()
        colorScheme.attachEditor(this)
        basicDetector = GestureDetector(getContext(), touchHandler!!)
        basicDetector!!.setOnDoubleTapListener(touchHandler!!)
        scaleDetector = ScaleGestureDetector(getContext(), touchHandler!!)
        handleDescInsert = HandleDescriptor()
        handleDescLeft = HandleDescriptor()
        handleDescRight = HandleDescriptor()
        lineNumberAlign = android.graphics.Paint.Align.RIGHT
        waitForNextChange = false
        blockLineEnabled = true
        blockLineWidth = 1f
        inputMethodManager = getContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        clipboardManager = getContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        setUndoEnabled(true)
        this.blockIndex = -1
        this.isScalable = true
        setFocusable(true)
        setFocusableInTouchMode(true)
        highlightBracketPair = true
        inputConnection = EditorInputConnection(this)
        snippetController = SnippetController(this)
        completionWindow = EditorAutoCompletion(this)
        edgeEffectVertical = EdgeEffect(getContext())
        edgeEffectHorizontal = EdgeEffect(getContext())
        textActionWindow = EditorTextActionWindow(this)
        diagnosticTooltip = EditorDiagnosticTooltipWindow(this)
        contextMenuCreator = EditorContextMenuCreator(this)
        editorLanguage = null
        setText(null)
        tabWidth = 4
        this.isHighlightCurrentLine = true
        this.isVerticalScrollBarEnabled = true
        setHighlightCurrentBlock(true)
        this.isDisplayLnPanel = true
        this.isHorizontalScrollBarEnabled = true
        this.isFirstLineNumberAlwaysVisible = true
        this.isCursorAnimationEnabled = true
        isEditable = true
        this.isLineNumberEnabled = true
        this.isHardwareAcceleratedDrawAllowed = true
        setInterceptParentHorizontalScrollIfNeeded(false)
        setTypefaceText(Typeface.DEFAULT)
        this.isSoftKeyboardEnabled = true
        this.isDisableSoftKbdIfHardKbdAvailable = true

        // Issue #41 View being highlighted when focused on Android 11
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setDefaultFocusHighlightEnabled(false)
        }
        if (getContext() is ContextThemeWrapper) {
            this.edgeEffectColor = ThemeUtils.getColorPrimary(getContext() as ContextThemeWrapper)
        }

        // Config scale detector
        scaleDetector!!.setQuickScaleEnabled(false)
    }

    /**
     * Apply attributes from XML
     */
    protected fun applyAttributeSets(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val array = getContext().obtainStyledAttributes(attrs, R.styleable.CodeEditor, defStyleAttr, defStyleRes)

        setHorizontalScrollbarThumbDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarThumbHorizontal))
        setHorizontalScrollbarTrackDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarTrackHorizontal))
        setVerticalScrollbarThumbDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarThumbVertical))
        setVerticalScrollbarTrackDrawable(array.getDrawable(R.styleable.CodeEditor_scrollbarTrackVertical))

        this.lnPanelPositionMode =
            array.getInt(R.styleable.CodeEditor_lnPanelPositionMode, LineInfoPanelPositionMode.FOLLOW)
        this.lnPanelPosition = array.getInt(R.styleable.CodeEditor_lnPanelPosition, LineInfoPanelPosition.CENTER)

        _dividerWidth = array.getDimension(R.styleable.CodeEditor_dividerWidth, _dividerWidth)
        setDividerMargin(
            array.getDimension(R.styleable.CodeEditor_dividerMargin, this.dividerMarginLeft),
            array.getDimension(R.styleable.CodeEditor_dividerMargin, this.dividerMarginRight)
        )
        setPinLineNumber(array.getBoolean(R.styleable.CodeEditor_pinLineNumber, false))

        setHighlightCurrentBlock(array.getBoolean(R.styleable.CodeEditor_highlightCurrentBlock, true))
        this.isHighlightCurrentLine = array.getBoolean(R.styleable.CodeEditor_highlightCurrentLine, true)
        setHighlightBracketPair(array.getBoolean(R.styleable.CodeEditor_highlightBracketPair, true))

        this.isLigatureEnabled = array.getBoolean(R.styleable.CodeEditor_ligatures, true)
        this.isLineNumberEnabled = array.getBoolean(R.styleable.CodeEditor_lineNumberVisible, this.isLineNumberEnabled)
        getComponent<EditorAutoCompletion>(EditorAutoCompletion::class.java)?.isEnabled =
            array.getBoolean(
                R.styleable.CodeEditor_autoCompleteEnabled,
                true
            )
        props!!.symbolPairAutoCompletion = array.getBoolean(R.styleable.CodeEditor_symbolCompletionEnabled, true)
        isRenderFunctionCharacters = array.getBoolean(
            R.styleable.CodeEditor_renderFunctionChars,
            isRenderFunctionCharacters
        )
        this.isScalable = array.getBoolean(R.styleable.CodeEditor_scalable, this.isScalable)

        this.textSizePx = array.getDimension(R.styleable.CodeEditor_textSize, this.textSizePx)
        setCursorBlinkPeriod(array.getInt(R.styleable.CodeEditor_cursorBlinkPeriod, cursorBlink?.period ?: 500))
        this.tabWidth = array.getInt(R.styleable.CodeEditor_tabWidth, tabWidth)

        val wordwrapMode: Int = array.getInt(R.styleable.CodeEditor_wordwrapMode, 0)
        if (wordwrapMode != 0) {
            setWordwrap(true, wordwrapMode > 1, false)
        }

        setText(array.getString(R.styleable.CodeEditor_text))

        array.recycle()
    }


    /**
     * Get `DirectAccessProps` object of the editor.
     * 
     * 
     * You can update some features in editor with the instance without disturb to call methods.
     */

    /**
     * @see .setFormatTip
     */
    fun getFormatTip(): String? {
        return formatTip
    }

    /**
     * Set the tip text while formatting
     */
    fun setFormatTip(@NonNull formatTip: String?) {
        this.formatTip = Objects.requireNonNull(formatTip)
    }

    /**
     * Set whether line number region will scroll together with code region
     * 
     * @see CodeEditor.isLineNumberPinned
     */
    fun setPinLineNumber(pinLineNumber: Boolean) {
        this.isLineNumberPinned = pinLineNumber
        if (this.isLineNumberEnabled) {
            invalidate()
        }
    }

    /**
     * Set the order of text action menu items
     */
    var textActionMenuOrder: List<String>? = null
        set(value) {
            field = value
            textActionWindow?.updateMenuOrderAndVisibility()
        }

    /**
     * Set the list of hidden text action menu items
     */
    var textActionMenuHidden: List<String>? = null
        set(value) {
            field = value
            textActionWindow?.updateMenuOrderAndVisibility()
        }

    /**
     * Inserts the given text in the editor.
     * 
     * 
     * This method allows you to insert texts externally to the content of editor.
     * The content of {@param text} is not checked to be exactly characters of symbols.
     * 
     * 
     * Note that this still works when the editor is not editable. But you should not
     * call it at that time due to possible problems, especially when [.getEditable] returns
     * true but [.isEditable] returns false
     * 
     * 
     * @param text            Text to insert, usually a text of symbols
     * @param selectionOffset New selection position relative to the start of text to insert.
     * Ranging from 0 to text.length()
     * @throws IllegalArgumentException If the {@param selectionRegion} is invalid
     */
    fun insertText(text: String, selectionOffset: Int) {
        kotlin.require(!(selectionOffset < 0 || selectionOffset > text.length)) { "selectionOffset is invalid" }
        val cur = cursor ?: return
        if (cur.isSelected()) {
            deleteText()
            notifyIMEExternalCursorChange()
        }
        this.text!!.insert(cur.rightLine, cur.rightColumn, text)
        notifyIMEExternalCursorChange()
        if (selectionOffset != text.length) {
            val pos =
                this.text!!.indexer.getCharPosition(cur.right - (text.length - selectionOffset))
            setSelection(pos.line, pos.column)
        }
    }

    /**
     * Set cursor blinking period
     * If zero or negative period is passed, the cursor will always be shown.
     * 
     * @param period The period time of cursor blinking
     */
    fun setCursorBlinkPeriod(period: Int) {
        if (cursorBlink != null) {
            cursorBlink!!.onSelectionChanged()
            val before: Int = cursorBlink!!.period
            cursorBlink!!.setPeriod(period)
            if (before <= 0 && cursorBlink!!.valid && isAttachedToWindow()) {
                postInLifecycle(cursorBlink!!)
            }
        }
    }


    /**
     * Set font feature settings for all paints used by editor
     * 
     * @see Paint.setFontFeatureSettings
     */
    fun setFontFeatureSettings(features: String?) {
        renderer.paint.setFontFeatureSettingsWrapped(features)
        renderer.getPaintOther().setFontFeatureSettings(features)
        renderer.getPaintGraph().setFontFeatureSettings(features)
        renderer.updateTimestamp()
        invalidate()
    }

    /**
     * Set the style of selection handler.
     * 
     * @see SelectionHandleStyle
     * 
     * @see HandleStyleDrop
     * 
     * @see HandleStyleSideDrop
     */
    fun setSelectionHandleStyle(@NonNull style: SelectionHandleStyle?) {
        handleStyle = Objects.requireNonNull(style)
        invalidate()
    }

    @NonNull

    /**
     * Returns whether highlight current code block
     * 
     * @return This module enabled / disabled
     * @see CodeEditor.setHighlightCurrentBlock
     */
    fun isHighlightCurrentBlock(): Boolean {
        return highlightCurrentBlock
    }

    /**
     * Whether the editor should use a different color to draw
     * the current code block line and this code block's start line and end line's
     * background.
     * 
     * @param highlightCurrentBlock Enabled / Disabled this module
     */
    fun setHighlightCurrentBlock(highlightCurrentBlock: Boolean) {
        this.highlightCurrentBlock = highlightCurrentBlock
        if (!this.highlightCurrentBlock) {
            this.blockIndex = -1
        } else {
            this.blockIndex = findCursorBlock()
        }
        invalidate()
    }

    /**
     * Get the editor's language.
     * 
     * @return EditorLanguage
     */

    /**
     * Internal callback to check if the editor is capable of handling the given
     * keybinding [KeyEvent]
     * 
     * @param keyCode      The keycode for the keybinding event.
     * @param ctrlPressed  Is 'Ctrl' key pressed?
     * @param shiftPressed Is 'Shift' key pressed?
     * @param altPressed   Is 'Alt' key pressed?
     * @return `true` if the editor can handle the keybinding, `false` otherwise.
     */
    internal fun canHandleKeyBinding(
        keyCode: Int,
        ctrlPressed: Boolean,
        shiftPressed: Boolean,
        altPressed: Boolean
    ): Boolean {
        val isDpadKey =
            keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
        val isHomeOrEnd = keyCode == KeyEvent.KEYCODE_MOVE_HOME || keyCode == KeyEvent.KEYCODE_MOVE_END

        if (ctrlPressed) {
            if (shiftPressed) {
                // Ctrl+Shift+[xx] keys
                return isDpadKey || isHomeOrEnd || keyCode == KeyEvent.KEYCODE_J
            }

            if (altPressed) {
                // Ctrl+Alt+[xx] keys
                return keyCode == KeyEvent.KEYCODE_ENTER
            }

            // Ctrl+[xx] keys
            return isDpadKey || isHomeOrEnd
                    || keyCode == KeyEvent.KEYCODE_A || keyCode == KeyEvent.KEYCODE_C || keyCode == KeyEvent.KEYCODE_X || keyCode == KeyEvent.KEYCODE_V || keyCode == KeyEvent.KEYCODE_U || keyCode == KeyEvent.KEYCODE_R || keyCode == KeyEvent.KEYCODE_D || keyCode == KeyEvent.KEYCODE_W || keyCode == KeyEvent.KEYCODE_ENTER
        }


        if (shiftPressed) {
            // Shift+[xx] keys
            return isDpadKey || isHomeOrEnd || keyCode == KeyEvent.KEYCODE_ENTER
        }

        return false
    }

    /**
     * Getter
     * 
     * @return The width in dp unit
     * @see CodeEditor.setBlockLineWidth
     */
    fun getBlockLineWidth(): Float {
        return blockLineWidth
    }

    /**
     * Set the width of code block line
     * 
     * @param dp Width in dp unit
     */
    fun setBlockLineWidth(dp: Float) {
        blockLineWidth = dp
        invalidate()
    }



    /**
     * Set whether text in editor should be wrapped to fit its size, with anti-word-breaking enabled
     * by default
     * 
     * @param wordwrap Whether to wrap words
     * @see .setWordwrap
     * @see .isWordwrap
     */

    /**
     * Set whether text in editor should be wrapped to fit its size
     * 
     * @param wordwrap         Whether to wrap words
     * @param antiWordBreaking Prevent English words to be split into two lines
     * @see .isWordwrap
     */


    /**
     * Set whether text in editor should be wrapped to fit its size
     * 
     * @param wordwrap         Whether to wrap words
     * @param antiWordBreaking Prevent English words to be split into two lines
     * @param supportRtlRow    Allow rows with RTL base direction to display from the right side of widget
     * @see .isWordwrap
     */

    private fun setWordwrap(wordwrap: Boolean, antiWordBreaking: Boolean, supportRtlRow: Boolean) {
        if (_wordwrap != wordwrap || this.isAntiWordBreaking != antiWordBreaking || this.isWordwrapRtlDisplaySupport != supportRtlRow) {
            _wordwrap = wordwrap
            this.isAntiWordBreaking = antiWordBreaking
            this.isWordwrapRtlDisplaySupport = supportRtlRow
            requestLayoutIfNeeded()
            createLayout()
            if (!wordwrap) {
                renderContext?.invalidateRenderNodes()
            }
            invalidate()
        }
    }

    var isCursorAnimationEnabled: Boolean
        /**
         * @see .setCursorAnimationEnabled
         */
        get() = cursorAnimation
        /**
         * Set cursor animation enabled
         */
        set(enabled) {
            if (!enabled) {
                _cursorAnimator!!.cancel()
            }
            cursorAnimation = enabled
        }

    /**
     * Get the number of lines to be broken synchronously when editor is initialized or layout is changed.
     */
    fun getInitialPreviewLines(): Int = initialPreviewLines

    /**
     * Set the number of lines to be broken synchronously when editor is initialized or layout is changed.
     */
    fun setInitialPreviewLines(lines: Int) {
        this.initialPreviewLines = lines
    }

    /**
     * Set whether the line number is displayed on the right side of the divider.
     */
    fun setLineNumberRightOfDivider(isRight: Boolean) {
        if (this.isLineNumberRightOfDivider != isRight) {
            this.isLineNumberRightOfDivider = isRight
            invalidate()
        }
    }

    /**
     * Check if the line number is displayed on the right side of the divider.
     */
    fun isLineNumberRightOfDivider(): Boolean {
        return isLineNumberRightOfDivider
    }

    /**
     * @see .setCursorAnimator
     */

    /**
     * Set cursor animation
     * 
     * @see CursorAnimator
     * 
     * @see .getCursorAnimator
     * @see .setCursorAnimationEnabled
     */
    fun setCursorAnimator(@NonNull cursorAnimator: CursorAnimator) {
        _cursorAnimator = cursorAnimator
    }

    /**
     * Whether display vertical scroll bar when scrolling
     * 
     * @param enabled Enabled / disabled
     */
    fun setScrollBarEnabled(enabled: Boolean) {
        this.isHorizontalScrollBarEnabled = enabled
        this.isVerticalScrollBarEnabled = this.isHorizontalScrollBarEnabled
        invalidate()
    }

    override fun getHorizontalScrollbarThumbDrawable(): Drawable? {
        return renderer.getHorizontalScrollbarThumbDrawable()
    }

    override fun setHorizontalScrollbarThumbDrawable(drawable: Drawable?) {
        super.setHorizontalScrollbarThumbDrawable(drawable)
    }

    override fun getHorizontalScrollbarTrackDrawable(): Drawable? {
        return renderer.getHorizontalScrollbarTrackDrawable()
    }

    override fun setHorizontalScrollbarTrackDrawable(drawable: Drawable?) {
        super.setHorizontalScrollbarTrackDrawable(drawable)
    }

    override fun getVerticalScrollbarThumbDrawable(): Drawable? {
        return renderer.getVerticalScrollbarThumbDrawable()
    }

    override fun setVerticalScrollbarThumbDrawable(drawable: Drawable?) {
        super.setVerticalScrollbarThumbDrawable(drawable)
    }

    override fun getVerticalScrollbarTrackDrawable(): Drawable? {
        return renderer.getVerticalScrollbarTrackDrawable()
    }

    override fun setVerticalScrollbarTrackDrawable(drawable: Drawable?) {
        super.setVerticalScrollbarTrackDrawable(drawable)
    }
  /**
     * @see CodeEditor.setLineNumberTipTextProvider
     */
    fun getLineNumberTipTextProvider(): LineNumberTipTextProvider? {
        return lineNumberTipTextProvider
    }

    /**
     * Set the tip text before line number for the line number panel
     */
    fun setLineNumberTipTextProvider(provider: LineNumberTipTextProvider?) {
        Objects.requireNonNull(provider, "Provider can not be null")
        lineNumberTipTextProvider = provider
        invalidate()
    }

    val insertHandleDescriptor: SelectionHandleStyle.HandleDescriptor?
        /**
         * Get the rect of insert cursor handle on view
         * 
         * @return Rect of insert handle
         */
        get() = handleDescInsert

    @get:Px
    var textSizePx: Float
        /**
         * Get text size in pixel unit
         * 
         * @return Text size in pixel unit
         * @see CodeEditor.setTextSize
         * @see CodeEditor.setTextSizePx
         */
        get() = renderer.paint.getTextSize()
        /**
         * Set text size in pixel unit
         * 
         * @param size Text size in pixel unit
         */
        set(size) {
            setTextSizePxDirect(size)
            requestLayoutIfNeeded()
            createLayout()
            invalidate()
        }

    /**
     * Set text size directly without creating layout!! or invalidating view
     * 
     * @param size Text size in pixel unit
     */
    @UnsupportedUserUsage
    open fun setTextSizePxDirect(@Px size: Float) {

        val oldTextSize = this.textSizePx
        renderer.setTextSizePxDirect(size)
        val layout = _layout
        if (layout is io.github.abc15018045126.sora.widget.layout.WordwrapLayout) {
            layout.refreshHeights()
        }
        dispatchEvent(TextSizeChangeEvent(this, oldTextSize, size))
    }

    internal fun requestLayoutIfNeeded() {
        if (anyWrapContentSet) {
            requestLayout()
        }
    }

    protected fun checkForRelayout() {
        if (anyWrapContentSet) {
            val params =
                getLayoutParams()
            if (params != null) {
                if (params.width === ViewGroup.LayoutParams.WRAP_CONTENT) {
                    requestLayout()
                } else if (params.height === ViewGroup.LayoutParams.WRAP_CONTENT) {
                    if (height !== layout!!.layoutHeight) {
                        requestLayout()
                    }
                }
            }
        }
    }



    val lineNumberMetrics: android.graphics.Paint.FontMetricsInt
        get() = renderer.lineNumberMetrics

    /**
     * Whether non-printable is to be drawn
     */
    internal fun shouldInitializeNonPrintable(): Boolean {

        return Numbers.clearBits(
            nonPrintableOptions,
            FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE or FLAG_DRAW_TAB_SAME_AS_SPACE or
                    FLAG_DRAW_LINE_SEPARATOR or FLAG_DRAW_SOFT_WRAP
        ) !== 0
    }

    var isHardwareAcceleratedDrawAllowed: Boolean
        /**
         * @see .setHardwareAcceleratedDrawAllowed
         */
        get() = hardwareAccAllowed
        /**
         * Set whether allow the editor to use RenderNode to draw its text.
         * Enabling this can cause more memory usage, but the editor can display text
         * much quicker.
         * However, only when hardware accelerate is enabled on this view can the switch
         * make a difference.
         */
        set(acceleratedDraw) {
            hardwareAccAllowed = acceleratedDraw
            if (acceleratedDraw && !isWordwrap) {
                renderContext?.invalidateRenderNodes()
            }
        }

    /**
     * As the name is, we find where leading spaces end and trailing spaces start
     * 
     * @param line The line to search
     */
    internal fun findLeadingAndTrailingWhitespacePos(line: ContentLine): Long {

        val buffer =
            line.backingCharArray
        val column: Int = line.length
        var leading = 0
        var trailing = column
        while (leading < column && Character.isWhitespace(buffer[leading])) {
            leading++
        }
        // Only when this action is needed
        if (leading != column && (nonPrintableOptions and (FLAG_DRAW_WHITESPACE_INNER or FLAG_DRAW_WHITESPACE_TRAILING)) != 0) {
            while (trailing > 0 && Character.isWhitespace(buffer[trailing - 1])) {
                trailing--
            }
        }
        return IntPair.pack(leading, trailing)
    }

    /**
     * A quick method to predicate whitespace character
     */
    private fun Character.isWhitespace(ch: Char): Boolean {
        return ch == '\t' || ch == ' '
    }

    /**
     * Get matched text regions on the given line
     * 
     * @param line      Target line
     * @param positions Output start positions
     */
    internal fun computeMatchedPositions(line: Int, positions: LongArrayList) {

        positions.clear()
        if (_searcher.currentPattern == null || _searcher.searchOptions == null) {
            return
        }
        if (!_searcher.isResultValid()) {
            return
        }
        val res =
            _searcher.lastResults
        if (res == null) {
            return
        }
        val lineLeft =
            text.getCharIndex(line, 0)
        val lineRight =
            lineLeft + text.getColumnCount(line)
        for (i in kotlin.math.max(0, res.lowerBoundByFirst(lineLeft) - 1)..<res.size()) {
            val region = res.get(i)
            val start =
                IntPair.getFirst(region)
            val end =
                IntPair.getSecond(region)
            val highlightStart =
                kotlin.math.max(start, lineLeft)
            val highlightEnd =
                kotlin.math.min(end, lineRight)
            if (highlightStart < highlightEnd) {
                positions.add(IntPair.pack(highlightStart - lineLeft, highlightEnd - lineLeft))
            }
            if (start > lineRight) {
                break
            }
        }
    }


    internal fun computeHighlightPositions(line: Int, positions: MutableLongLongMap) {

        positions.clear()
        val container = highlightTextContainer ?: return
        val highlights =
            container.getForLine(line)
        if (highlights.isEmpty()) {
            return
        }
        val lineColumnCount: Int = text.getColumnCount(line)
        val highlightBlankLine = false
        for (highlight in highlights) {
            if (line < highlight.startLine || line > highlight.endLine) {
                continue
            }
            var startColumn = if (line == highlight.startLine) highlight.startColumn else 0
            var endColumn = if (line == highlight.endLine) highlight.endColumn else lineColumnCount
            if (startColumn < 0) {
                startColumn = 0
            } else if (startColumn > lineColumnCount) {
                startColumn = lineColumnCount
            }
            if (endColumn < 0) {
                endColumn = 0
            } else if (endColumn > lineColumnCount) {
                endColumn = lineColumnCount
            }
            if (lineColumnCount == 0) {
                continue
            }
            if (startColumn < endColumn) {
                val backgroundColor: Int = highlight.color.resolve(colorScheme)
                val borderColor: Int = highlight.borderColor.resolve(colorScheme)
                positions.put(IntPair.pack(startColumn, endColumn), IntPair.pack(backgroundColor, borderColor))
            }
        }
    }

    var edgeEffectColor: Int
        /**
         * Get the color of EdgeEffect
         * 
         * @return The color of EdgeEffect.
         */
        get() = edgeEffectVertical!!.color
        /**
         * Set the color of EdgeEffect
         * 
         * @param color The color of EdgeEffect
         */
        set(color) {
            edgeEffectVertical!!.setColor(color)
            edgeEffectHorizontal!!.setColor(color)
        }

    /**
     * Get the layout!! of editor
     */

    val verticalEdgeEffect: EdgeEffect
        /**
         * Get EdgeEffect for vertical direction
         * 
         * @return EdgeEffect
         */
        get() = edgeEffectVertical!!

    val horizontalEdgeEffect: EdgeEffect
        /**
         * Get EdgeEffect for horizontal direction
         * 
         * @return EdgeEffect
         */
        get() = edgeEffectHorizontal!!

    /**
     * Find the smallest code block that cursor is in
     * 
     * @return The smallest code block index.
     * If cursor is not in any code block,just -1.
     */
    private fun findCursorBlock(): Int {
        val blocks: List<CodeBlock>? = if (textStyles == null) null else textStyles!!.blocks
        if (blocks == null || blocks.isEmpty()) {
            return -1
        }
        return findCursorBlock(blocks)
    }

    /**
     * Find the cursor code block internal
     * 
     * @param blocks Current code blocks
     * @return The smallest code block index.
     * If cursor is not in any code block,just -1.
     */
    private fun findCursorBlock(blocks: List<CodeBlock>): Int {
        val line: Int = cursor!!.leftLine
        var min = binarySearchEndBlock(line, blocks)
        if (min == -1) {
            min = 0
        }
        val max: Int = blocks.size - 1
        var minDis: Int = Integer.MAX_VALUE
        var found = -1
        var invalidCount = 0
        var maxCount: Int = Integer.MAX_VALUE
        if (textStyles != null) {
            maxCount = textStyles!!.getSuppressSwitch()
        }
        for (i in min..max) {
            val block: CodeBlock? = blocks.get(i)
            if (block == null) {
                continue
            }
            if (block.endLine >= line && block.startLine <= line) {
                val dis: Int = block.endLine - block.startLine
                if (dis < minDis) {
                    minDis = dis
                    found = i
                }
            } else if (minDis != Integer.MAX_VALUE) {
                invalidCount++
                if (invalidCount >= maxCount) {
                    break
                }
            }
        }
        return found
    }

    /**
     * Find the first code block that maybe seen on screen Because the code blocks is sorted by its
     * end line position we can use binary search to quicken this process in order to decrease the
     * time we use on finding
     * 
     * @param firstVis The first visible line
     * @param blocks   Current code blocks
     * @return The index of the block we found or `-1` if no code block is found.
     */
    fun binarySearchEndBlock(firstVis: Int, blocks: List<CodeBlock>?): Int {
        return CodeBlock.binarySearchEndBlock(firstVis, blocks)
    }


    /**
     * Get spans on the given line
     */
    @NonNull
    fun getSpansForLine(line: Int): List<Span?> {
        val spanMap =
            if (textStyles == null) null else textStyles!!.spans
        if (defaultSpans.isEmpty()) {
            defaultSpans.add(SpanFactory.obtainNoExt(0, EditorColorScheme.TEXT_NORMAL.toLong()))
        }
        try {
            if (spanMap != null) {
                return spanMap.read().getSpansOnLine(line)
            } else {
                return defaultSpans
            }
        } catch (e: Exception) {
            return defaultSpans
        }
    }

    /**
     * Get the width of line number region (include line number margin)
     * 
     * @return width of line number region
     */
    fun measureLineNumber(): Float {
        if (!this.isLineNumberEnabled) {
            return 0f
        }
        var count = 0
        var lineCount = this.lineCount
        while (lineCount > 0) {
            count++
            lineCount /= 10
        }
        val len = NUMBER_DIGITS.length
        val buffer = TemporaryFloatBuffer.obtain(len)
        renderer!!.paintOther.getTextWidths(NUMBER_DIGITS, buffer)
        TemporaryFloatBuffer.recycle(buffer)
        var single = 0f
        var i = 0
        while (i < len) {
            single = kotlin.math.max(single, buffer[i])
            i += 2
        }
        return single * count + lineNumberMarginLeft
    }

    /**
     * Create layout!! for text
     */
    internal fun createLayout(clearWordwrapCache: Boolean = true) {
        val layout = _layout
        val text = _text
        if (text == null) {
            return
        }
        if (layout != null) {
            if (layout is LineBreakLayout && !isWordwrap) {
                (layout as LineBreakLayout).reuse(text)
                return
            }
            if (layout is WordwrapLayout && isWordwrap) {
                val newLayout: WordwrapLayout = WordwrapLayout(
                    this, text,
                    this.isAntiWordBreaking,
                    this.isWordwrapRtlDisplaySupport, layout as WordwrapLayout, clearWordwrapCache
                )
                layout.destroyLayout()
                _layout = newLayout
                return
            }
            layout.destroyLayout()
        }
        if (isWordwrap) {
            renderer.setCachedLineNumberWidth(measureLineNumber().toInt())
            _layout = WordwrapLayout(this, text, this.isAntiWordBreaking, this.isWordwrapRtlDisplaySupport, null, false)
        } else {
            _layout = LineBreakLayout(this, text)
        }
        if (touchHandler != null) {
            touchHandler!!.scrollBy(0f, 0f)
        }
    }

    /**
     * Indents the selected lines. Does nothing if the text is not selected.
     */
    fun indentSelection() {
        indentLines(true)
    }

    /**
     * Indents the lines. Does nothing if the `onlyIfSelected` is `true` and
     * no text is selected.
     * 
     * @param onlyIfSelected Set to `true` if lines must be indented only if the text is
     * selected.
     */
    fun indentLines(onlyIfSelected: Boolean) {
        val cursor: Cursor = cursor
        if (onlyIfSelected && !cursor.isSelected()) {
            return
        }

        val tabString = createTabString()
        val text: Content = text
        val tabWidth = tabWidth

        text.beginBatchEdit()
        for (i in cursor.leftLine..cursor.rightLine) {
            val line = text.getLine(i)
            val result =
                TextUtils.countLeadingSpacesAndTabs(line)
            val spaceCount =
                IntPair.getFirst(result)
            val tabCount =
                IntPair.getSecond(result)
            val spaces: Int = spaceCount + (tabCount * tabWidth)
            val endColumn: Int = spaceCount + tabCount

            val requiredSpaces = tabWidth - (spaces % tabWidth)
            if (spaceCount > 0 && tabCount > 0) {
                // indentation contains spaces as well as tabs
                // replace the leading indentation with appropriate indentation (according to language.useTabs())
                // this should be done while incrementing the indentation
                val finalSpaceCount = ((if (requiredSpaces == 0) tabWidth else requiredSpaces) + spaces) / tabWidth
                text.replace(i, 0, i, endColumn, tabString.repeat(finalSpaceCount))
                continue
            }

            if (requiredSpaces == tabWidth) {
                // line is evenly indented
                // increase the indentation by \t or tabWidthSpaces
                text.insert(i, endColumn, tabString)
            } else {
                // line is oddly indented
                // We know that a line can never be oddly indented when it is indented only with tabs
                // therefore, we insert spaces to align the line
                text.insert(i, endColumn, " ".repeat(requiredSpaces))
            }
        }
        text.endBatchEdit()
    }

    /**
     * Removes indentation from the start of the selected lines. If the text is not selected, or if
     * the start and end selection is on the same line, only the line at the cursor position is
     * unindented.
     */
    fun unindentSelection() {
        val cursor: Cursor = cursor
        val text: Content = text
        val tabWidth = tabWidth
        val tabString = createTabString()

        text.beginBatchEdit()
        for (i in cursor.leftLine..cursor.rightLine) {
            val line =
                text.getLineString(i)
            val result =
                TextUtils.countLeadingSpacesAndTabs(line)
            val spaceCount =
                IntPair.getFirst(result)
            val tabCount =
                IntPair.getSecond(result)
            val spaces: Int = spaceCount + (tabCount * tabWidth)
            if (spaces == 0) {
                // line is not indented
                continue
            }

            val endColumn: Int = spaceCount + tabCount

            val extraSpaces = spaces % tabWidth
            if (spaceCount > 0 && tabCount > 0) {
                // indentation contains spaces as well as tabs
                // replace the leading indentation with appropriate indentation (according to language.useTabs())
                // this should be done while decrementing the indentation
                val finalSpaceCount =
                    Math.abs(spaces - (if (extraSpaces == 0) tabWidth else extraSpaces)) / tabWidth
                text.replace(i, 0, i, endColumn, tabString.repeat(finalSpaceCount))
                continue
            }

            if (extraSpaces == 0) {
                // line is evenly indented
                // remove tabString.length() characters from the start

                // do not use tabString.length()

                text.delete(i, endColumn - (if (tabCount > 0) 1 else tabWidth), i, endColumn)
            } else {
                // line is oddly indented
                // We know that a line can never be oddly indented when it is indented only with tabs
                // therefore, we delete spaces to align the line
                text.delete(i, endColumn - extraSpaces, i, endColumn)
            }
        }
        text.endBatchEdit()
    }

    /**
     * Commit a tab to cursor
     */
    protected fun commitTab() {
        if (inputConnection != null && isEditable) {
            if (inputConnection!!.composingText.isComposing()) {
                // This external change will damage the composing text (#784)
                restartInput()
            }
            inputConnection!!.commitTextInternal(createTabString(), true)
        }
    }

    /**
     * Indents the line if text is not selected and the cursor is at the start of the line. Inserts
     * an indentation string otherwise.
     */
    fun indentOrCommitTab() {
        val cursor: Cursor = cursor
        if (cursor.isSelected()) {
            indentSelection()
            return
        }

        val left = cursor.left()
        val line =
            text.getLine(left.line)

        val count =
            TextUtils.countLeadingSpacesAndTabs(line)
        val spaceCount =
            IntPair.getFirst(count)
        val tabCount =
            IntPair.getSecond(count)

        if (left.column > spaceCount + tabCount) {
            // there is text before the cursor
            commitTab()
            return
        }

        indentLines(false)
    }

    /**
     * Creates the string to insert when `KEYCODE_TAB` key event is received from the IME.
     * 
     * @return The string to insert for tab character.
     */
    protected fun createTabString(): String {
        val language: Language = editorLanguage!!
        return TextUtils.createIndent(tabWidth, tabWidth, language.useTab())
    }

    /**
     * Update the information of cursor
     * Such as the position of cursor on screen(For input method that can go to any position on screen like PC input method)
     * 
     * @return The offset x of right cursor on view
     */
    fun updateCursorAnchor(): Float {
        val l: Int = cursor!!.rightLine
        val column: Int = cursor!!.rightColumn
        var visible = true
        var x = measureTextRegionOffset()
        x = x + layout!!.getCharLayoutOffset(l, column)[1]
        x = x - this.offsetX
        if (x < 0) {
            visible = false
            x = 0f
        }
        val composingText =
            inputConnection!!.composingText
        if (composingText.preSetComposing) {
            return x
        }
        if (props!!.reportCursorAnchor) {
            val builder: CursorAnchorInfo.Builder = anchorInfoBuilder!!
            builder.reset()
            matrix!!.set(getMatrix())
            val b = IntArray(2)
            getLocationOnScreen(b)
            matrix!!.postTranslate(b[0].toFloat(), b[1].toFloat())
            builder.setMatrix(matrix)
            builder.setSelectionRange(cursor!!.left, cursor!!.right)

            if (composingText.isComposing()) {
                builder.setComposingText(
                    composingText.startIndex,
                    text.substring(composingText.startIndex, composingText.endIndex)
                )
            }
            builder.setInsertionMarkerLocation(
                x,
                getRowTop(l) - this.offsetY.toFloat(),
                getRowBaseline(l) - this.offsetY.toFloat(),
                getRowBottom(l) - this.offsetY.toFloat(),
                if (visible) CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION else CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
            )
            inputMethodManager!!.updateCursorAnchorInfo(this, builder.build())
        }
        return x
    }

    /**
     * Delete text before cursor or selected text (if there is)
     */
    fun deleteText() {
        val cur: Cursor = cursor!!
        if (cur.isSelected()) {
            text.delete(cur.leftLine, cur.leftColumn, cur.rightLine, cur.rightColumn)
        } else {
            val col: Int = cur.leftColumn
            val line: Int = cur.leftLine
            if (props!!.deleteEmptyLineFast || (props!!.deleteMultiSpaces !== 1 && col > 0 && text.getLineString(line)[col - 1] === ' ')
            ) {
                // Check whether selection is in leading spaces
                val text =
                    this.text.getLine(cur.leftLine).backingCharArray
                var inLeading = true
                for (i in col - 1 downTo 0) {
                    val ch: Char = text[i]
                    if (ch != ' ' && ch != '\t') {
                        inLeading = false
                        break
                    }
                }

                if (inLeading) {
                    // Check empty line
                    var emptyLine = true
                    val max =
                        this.text.getColumnCount(line)
                    for (i in col..<max) {
                        val ch: Char = text[i]
                        if (ch != ' ' && ch != '\t') {
                            emptyLine = false
                            break
                        }
                    }
                    if (props!!.deleteEmptyLineFast && emptyLine) {
                        if (line == 0) {
                            // Just delete whitespaces before
                            this.text.delete(line, 0, line, col)
                        } else {
                            this.text.delete(line - 1, this.text.getColumnCount(line - 1), line, max)
                        }
                        return
                    }

                    if (props!!.deleteMultiSpaces !== 1 && col > 0 && this.text.getLineString(line)[col - 1] === ' ') {
                        this.text.delete(
                            line,
                            kotlin.math.max(
                                0,
                                col - (if (props!!.deleteMultiSpaces === -1) tabWidth else props!!.deleteMultiSpaces)
                            ),
                            line,
                            col
                        )
                        return
                    }
                }
            }
            // Do not put cursor inside combined characters
            var begin: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                begin = TextUtilsP.getOffsetForBackspaceKey(text.getLine(cur.leftLine), col)
            } else {
                begin = TextLayoutHelper.get().getCurPosLeft(col, text.getLine(cur.leftLine))
            }
            var end: Int = cur.leftColumn
            if (begin > end) {
                val tmp = begin
                begin = end
                end = tmp
            }
            if (begin == end) {
                if (cur.leftLine > 0 && begin == 0) {
                    text.delete(cur.leftLine - 1, text.getColumnCount(cur.leftLine - 1), cur.leftLine, 0)
                }
            } else {
                text.delete(cur.leftLine, begin, cur.leftLine, end)
            }
        }
    }

    /**
     * Commit text to the content from IME
     */
    fun commitText(@NonNull text: CharSequence) {
        commitText(text, true)
    }

    /**
     * Commit text at current state from IME
     * 
     * @param text            Text commit by InputConnection
     * @param applyAutoIndent Apply automatic indentation
     */
    fun commitText(@NonNull text: CharSequence, applyAutoIndent: Boolean) {
        commitText(text, applyAutoIndent, true)
    }

    /**
     * Commit text with given options
     * 
     * @param text Text commit by InputConnection
     * @param applyAutoIndent Apply automatic indentation
     * @param applySymbolCompletion Apply symbol surroundings and completions
     */
    fun commitText(@NonNull text: CharSequence, applyAutoIndent: Boolean, applySymbolCompletion: Boolean) {
        // replace text
        var text = text
        var pair: SymbolPairMatch.SymbolPair? = null
        if (applySymbolCompletion && props!!.symbolPairAutoCompletion && text.length > 0) {
            val endCharFromText: Char =
                text[text.length - 1]

            var inputText: CharArray? = null

            //size > 1
            if (text.length > 1) {
                inputText = text.toString().toCharArray()
            }

            pair = languageSymbolPairs?.matchBestPair(
                this, cursor!!.left(),
                inputText, endCharFromText
            )
        }

        val cur: Cursor = cursor!!
        val editorText: Content = this.text
        val quoteHandler: QuickQuoteHandler? =
            LanguageHelper.getQuickQuoteHandler(editorLanguage!!)

        if (pair != null && pair !== SymbolPairMatch.SymbolPair.EMPTY_SYMBOL_PAIR) {
            // QuickQuoteHandler can easily implement the feature of AutoSurround
            // and is at a higher level (customizable),
            // so if the language implemented QuickQuoteHandler,
            // the AutoSurround feature is not considered needed because it can be implemented through QuickQuoteHandler

            if (pair.shouldDoAutoSurround(editorText) && quoteHandler == null) {
                editorText.beginBatchEdit()
                // insert left
                editorText.insert(cur.leftLine, cur.leftColumn, pair.open)
                // editorText.insert(editorCursor.leftLine,editorCursor.leftColumn,selectText);
                // insert right
                editorText.insert(cur.rightLine, cur.rightColumn, pair.close)
                editorText.endBatchEdit()

                // setSelection
                setSelectionRegion(
                    cur.leftLine, cur.leftColumn,
                    cur.rightLine, cur.rightColumn - pair.close.length
                )

                return
            } else if (cur.isSelected() && quoteHandler != null) {
                if (text.length > 0 && text.length == 1) {
                    val result: QuickQuoteHandler.HandleResult? =
                        quoteHandler.onHandleTyping(
                            text.toString(), this.text,
                            this.cursorRange,
                            this.styles!!
                        )
                    if (result != null && result.isConsumed()) {
                        val range: TextRange? =
                            result.getNewCursorRange()
                        if (range != null) {
                            setSelectionRegion(
                                range.start.line,
                                range.start.column,
                                range.end.line,
                                range.end.column
                            )
                        }
                        return
                    }
                }
            } else {
                editorText.beginBatchEdit()

                val insertPosition: CharPosition =
                    editorText
                        .indexer
                        .getCharPosition(pair.insertOffset)

                editorText.replace(
                    insertPosition.line, insertPosition.column,
                    cur.rightLine, cur.rightColumn, pair.open
                )
                editorText.insert(insertPosition.line, insertPosition.column + pair.open.length, pair.close)
                editorText.endBatchEdit()

                val cursorPosition: CharPosition =
                    editorText
                        .indexer
                        .getCharPosition(pair.cursorOffset)

                setSelection(cursorPosition.line, cursorPosition.column)

                return
            }
        }


        if (cur.isSelected()) {
            editorText.replace(cur.leftLine, cur.leftColumn, cur.rightLine, cur.rightColumn, text)
        } else {
            if (props!!.autoIndent && text.length !== 0 && applyAutoIndent) {
                val first: Char = text[0]
                if (first == '\n' || first == '\r') {
                    val line: String = this.text.getLineString(cur.leftLine)
                    var p = 0
                    var spaceCount = 0
                    var tabCount = 0
                    while (p < cur.leftColumn) {
                        if (Character.isWhitespace(line[p])) {
                            if (line[p] === '\t') {
                                ++tabCount
                            } else {
                                ++spaceCount
                            }
                            p++
                        } else {
                            break
                        }
                    }
                    var count = spaceCount + (tabCount * tabWidth)
                    try {
                        count += LanguageHelper.getIndentAdvance(
                            editorLanguage!!,
                            ContentReference(this.text),
                            cur.leftLine,
                            cur.leftColumn,
                            spaceCount,
                            tabCount
                        )
                    } catch (e: Exception) {
                        Log.w(
                            LOG_TAG,
                            "Language object error",
                            e
                        )
                    }
                    var index = 1
                    if (first == '\r' && text.length >= 2 && text[1] === '\n') {
                        index = 2
                    }
                    val sb: StringBuilder = StringBuilder(text)
                    sb.insert(index, TextUtils.createIndent(count, tabWidth, editorLanguage!!.useTab()))
                    text = sb
                }
            }
            editorText.insert(cur.leftLine, cur.leftColumn, text)
        }
    }

    var nonPrintablePaintingFlags: Int
        /**
         * @see .setNonPrintablePaintingFlags
         * @see .FLAG_DRAW_WHITESPACE_LEADING
         * 
         * @see .FLAG_DRAW_WHITESPACE_INNER
         * 
         * @see .FLAG_DRAW_WHITESPACE_TRAILING
         * 
         * @see .FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
         * 
         * @see .FLAG_DRAW_LINE_SEPARATOR
         * 
         * @see .FLAG_DRAW_WHITESPACE_IN_SELECTION
         */
        get() = nonPrintableOptions
        /**
         * Sets non-printable painting flags.
         * Specify where they should be drawn and some other properties.
         * 
         * 
         * Flags can be mixed.
         * 
         * @param flags Flags
         * @see .FLAG_DRAW_WHITESPACE_LEADING
         * 
         * @see .FLAG_DRAW_WHITESPACE_INNER
         * 
         * @see .FLAG_DRAW_WHITESPACE_TRAILING
         * 
         * @see .FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
         * 
         * @see .FLAG_DRAW_LINE_SEPARATOR
         * 
         * @see .FLAG_DRAW_WHITESPACE_IN_SELECTION
         * 
         * @see .FLAG_DRAW_SOFT_WRAP
         */
        set(flags) {
            val oldFlags = nonPrintableOptions
            this.nonPrintableOptions = flags
            if ((oldFlags and FLAG_DRAW_SOFT_WRAP) != (flags and FLAG_DRAW_SOFT_WRAP)) {
                createLayout()
            }
            invalidate()
        }

    fun hasComposingText(): Boolean {
        return inputConnection!!.composingText.isComposing()
    }

    /**
     * Make the selection visible
     */
    fun ensureSelectionVisible() {
        ensurePositionVisible(cursor.rightLine, cursor.rightColumn)
    }

    /**
     * Make the given character position visible
     * 
     * @param line        Line in text
     * @param column      Column in text
     * @param noAnimation true if no animation should be applied
     */
    /**
     * Make the given character position visible
     * 
     * @param line   Line in text
     * @param column Column in text
     */
    @JvmOverloads
    fun ensurePositionVisible(line: Int, column: Int, noAnimation: Boolean = false) {
        val scroller: EditorScroller = this.scroller
        val layoutOffset: FloatArray = layout!!.getCharLayoutOffset(line, column)
        // x offset is the left of character
        val xOffset = layoutOffset[1] + measureTextRegionOffset()
        // y offset is the bottom of row
        val yOffset = layoutOffset[0]

        val currFinalY: Float = if (scroller.isFinished) this.offsetY.toFloat() else scroller.getFinalY().toFloat()
        val currFinalX: Float = if (scroller.isFinished) this.offsetX.toFloat() else scroller.getFinalX().toFloat()
        var targetY = currFinalY
        var targetX = currFinalX

        val topLines = if (props!!.stickyScroll) props!!.stickyScrollMaxLines else 2
        if (yOffset - this.rowHeight * topLines < currFinalY) {
            // top may be invisible
            targetY = yOffset - this.rowHeight * topLines.toFloat()
        }
        if (yOffset > height + currFinalY) {
            // bottom invisible
            targetY = yOffset - height + this.rowHeight * 1f
        }
        val charWidth: Float = if (column == 0) 0f else this.textPaint.measureText("a")
        if (xOffset < currFinalX + (if (this.isLineNumberPinned) measureTextRegionOffset() else 0f)) {
            val backupX = targetX
            val scrollSlopX =
                width / 2
            targetX = xOffset + (if (this.isLineNumberPinned) -measureTextRegionOffset() else 0f) - charWidth
            if (abs(targetX - backupX) < scrollSlopX) {
                targetX = max(1f, backupX - scrollSlopX)
            }

        }
        if (xOffset + charWidth > currFinalX + width) {
            targetX = xOffset + charWidth * 0.8f - width
        }

        targetX = kotlin.math.max(0f, kotlin.math.min(this.scrollMaxX.toFloat(), targetX))
        targetY = kotlin.math.max(0f, kotlin.math.min(this.scrollMaxY.toFloat(), targetY))

        if (Floats.withinDelta(targetX, this.offsetX.toFloat(), 1f) && Floats.withinDelta(targetY, this.offsetY.toFloat(), 1f)) {
            invalidate()
            return
        }

        val animation = System.currentTimeMillis() - lastMakeVisible >= 100
        lastMakeVisible = System.currentTimeMillis()

        if (animation && !noAnimation) {
            scroller.forceFinished(true)
            scroller.startScroll(
                this.offsetX,
                this.offsetY,
                (targetX - this.offsetX).toInt(),
                (targetY - this.offsetY).toInt()
            )
            if (props!!.awareScrollbarWhenAdjust && Math.abs(this.offsetY - targetY) > dpUnit * 100) {
                touchHandler!!.notifyScrolled()
            }
        } else {
            scroller.startScroll(
                this.offsetX,
                this.offsetY, (targetX - this.offsetX).toInt(), (targetY - this.offsetY).toInt(), 0
            )
            scroller.abortAnimation()
        }

        dispatchEvent(
            ScrollEvent(
                this, this.offsetX,
                this.offsetY, targetX.toInt(), targetY.toInt(), ScrollEvent.CAUSE_MAKE_POSITION_VISIBLE
            )
        )

        invalidate()
    }

    /**
     * Whether there is clip
     * 
     * @return whether clip in clip board
     */
    fun hasClip(): Boolean {
        return clipboardManager!!.hasPrimaryClip()
    }

    val scroller: EditorScroller
        /**
         * Get scroller from EventHandler
         * You would better not use it for your own scrolling
         * 
         * @return The scroller
         */
        get() = touchHandler!!.getScroller()

    /**
     * Checks whether the position is over max Y position
     * 
     * @param posOnScreen Y position on view
     * @return Whether over max Y
     */
    fun isOverMaxY(posOnScreen: Float): Boolean {
        return posOnScreen + this.offsetY > layout!!.layoutHeight
    }

    /**
     * Check if the point on editor view, is inside text region on that row
     */
    fun isScreenPointOnText(x: Float, y: Float): Boolean {
        val pos = getPointPositionOnScreen(x, y)
        val rowIdx: Int =
            layout!!.getRowIndexForPosition(text.getCharIndex(IntPair.getFirst(pos), IntPair.getSecond(pos)))
        val layoutMax: Float =
            renderer.getRowWidth(rowIdx)
        val textRegionX = measureTextRegionOffset()
        val rowRegionRightX: Float = textRegionX + layoutMax

        val offset = this.offsetX + x
        return offset >= textRegionX && offset <= rowRegionRightX
    }

    /**
     * Determine character position using positions in scroll coordinate
     * 
     * @param xOffset Horizontal position in scroll coordinate
     * @param yOffset Vertical position in scroll coordinate
     * @return IntPair. first is line and second is column
     * @see IntPair
     */
    fun getPointPosition(xOffset: Float, yOffset: Float): Long {
        return layout!!.getCharPositionForLayoutOffset(xOffset - measureTextRegionOffset(), yOffset)
    }

    /**
     * Determine character position using positions on view
     * 
     * @param x X on view
     * @param y Y on view
     * @return IntPair. first is line and second is column
     * @see IntPair
     */
    fun getPointPositionOnScreen(x: Float, y: Float): Long {
        var y = y
        y = kotlin.math.max(0f, y)
        val stuckLines: List<CodeBlock?>? =
            renderer.lastStuckLines
        if (stuckLines != null) {
            // position Y maybe negative
            val index = kotlin.math.max(0f, (y / this.rowHeight)).toInt()
            if (y < stuckLines.size * this.rowHeight && index < stuckLines.size) {
                return getPointPosition(
                    x,
                    layout!!.getCharLayoutOffset(stuckLines!![index]!!.startLine, 0)[0] - this.rowHeight / 2f
                )
            }
        }
        return getPointPosition(x + this.offsetX, y + this.offsetY)
    }

    val scrollMaxY: Int
        /**
         * Get max scroll y
         * 
         * @return max scroll y
         */
        get() {
            val params: ViewGroup.LayoutParams? =
                getLayoutParams()
            return kotlin.math.max(
                0,
                layout!!.layoutHeight - (if (params == null || params.height === ViewGroup.LayoutParams.WRAP_CONTENT) height else (height * (1 - verticalExtraSpaceFactor)).toInt())
            )
        }

    val scrollMaxX: Int
        /**
         * Get max scroll x
         * 
         * @return max scroll x
         */
        get() = kotlin.math.max(0, (layout!!.layoutWidth + measureTextRegionOffset() - width / 2f).toInt())

    /**
     * Set the factor of extra space in vertical direction. The factor is multiplied with editor
     * height to compute the extra space of vertical viewport. Specially, when factor is zero, no
     * extra space is added.
     * 
     * @param extraSpaceFactor the factor. 0.5 by default.
     * @throws IllegalArgumentException if the factor is negative or bigger than 1.0f
     * @see .getVerticalExtraSpaceFactor
     */
    fun setVerticalExtraSpaceFactor(extraSpaceFactor: Float) {
        kotlin.require(!(extraSpaceFactor < 0 || extraSpaceFactor > 1.0f)) { "the factor should be in range [0.0, 1.0]" }
        this.verticalExtraSpaceFactor = extraSpaceFactor
        // ensure offset is in scroll range
        touchHandler!!.scrollBy(0f, 0f)
    }

    /**
     * Get the factor used to compute extra space of vertical viewport.
     * 
     * @see .setVerticalExtraSpaceFactor
     */
    fun getVerticalExtraSpaceFactor(): Float {
        return verticalExtraSpaceFactor
    }

    val cursorAnimator: CursorAnimator
        /**
         * Get CursorAnimator of editor
         *
         * @return CursorAnimator
         */
        get() = _cursorAnimator!!

    val searcher: EditorSearcher
        /**
         * Get EditorSearcher
         * 
         * @return EditorSearcher
         */
        get() = _searcher


    /**
     * Set selection around the given position
     * It will try to set selection as near as possible (Exactly the position if that position exists)
     */
    internal fun setSelectionAround(line: Int, column: Int) {

        var column = column
        if (line < this.lineCount) {
            val columnCount: Int = text.getColumnCount(line)
            if (column > columnCount) {
                column = columnCount
            }
            setSelection(line, column)
        } else {
            setSelection(this.lineCount - 1, text.getColumnCount(this.lineCount - 1))
        }
    }

    /**
     * Format text Async
     * 
     * @return Whether the format task is scheduled
     */
    @Synchronized
    fun formatCodeAsync(): Boolean {
        if (this.isFormatting) {
            return false
        }
        val formatter: Formatter? =
            editorLanguage!!.formatter
        formatter?.setReceiver(this)
        val formatContent: Content? =
            text.copyText(false)
        formatContent?.isUndoEnabled = false
        formatter?.format(formatContent!!, this.cursorRange)
        postInvalidate()
        return true
    }

    /**
     * Format text in the given region.
     * 
     * 
     * Note: Make sure the given positions are valid (line, column and index). Typically, you should
     * obtain a position by an object of [io.github.abc15018045126.sora.text.Indexer]
     * 
     * @param start Start position created by Indexer
     * @param end   End position created by Indexer
     * @return Whether the format task is scheduled
     */
    @Synchronized
    fun formatCodeAsync(start: CharPosition, end: CharPosition): Boolean {
        kotlin.require(!(start.index > end.index)) { "start > end" }
        if (this.isFormatting) {
            return false
        }
        val formatter: Formatter? =
            editorLanguage!!.formatter
        formatter?.setReceiver(this)
        val formatContent: Content? =
            text.copyText(false)
        formatContent?.isUndoEnabled = false
        formatter?.formatRegion(formatContent!!, TextRange(start, end), this.cursorRange)
        postInvalidate()
        return true
    }

    val cursorRange: TextRange
        /**
         * Get the cursor range of editor
         */
        get() = cursor!!.getRange()

    val isTextSelected: Boolean
        /**
         * If any text is selected
         */
        get() = cursor!!.isSelected()


    /**
     * Set tab width
     * 
     * @param width tab width compared to space
     */

    /**
     * Set max and min text size that can be used by user zooming.
     * 
     * 
     * Unit is px.
     */
    fun setScaleTextSizes(minSize: Float, maxSize: Float) {
        kotlin.require(!(minSize > maxSize)) { "min size can not be bigger than max size" }
        kotlin.require(!(minSize < 2f)) { "min size must be at least 2px" }
        touchHandler!!.scaleMinSize = minSize
        touchHandler!!.scaleMaxSize = maxSize
    }

    /**
     * When the parent is a scrollable view group,
     * request it not to allow horizontal scrolling to be intercepted.
     * Until the code cannot scroll horizontally
     * 
     * @param forceHorizontalScrollable Whether force horizontal scrolling
     */
    fun setInterceptParentHorizontalScrollIfNeeded(forceHorizontalScrollable: Boolean) {
        this.isInterceptParentHorizontalScrollEnabled = forceHorizontalScrollable
        if (!forceHorizontalScrollable) {
            val parent: ViewParent? = getParent()
            if (parent != null) {
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
    }

    /**
     * @see .setHighlightBracketPair
     */
    fun isHighlightBracketPair(): Boolean {
        return highlightBracketPair
    }

    /**
     * Whether to highlight brackets pairs
     */
    fun setHighlightBracketPair(highlightBracketPair: Boolean) {
        this.highlightBracketPair = highlightBracketPair
        if (!highlightBracketPair) {
            styleDelegate!!.clearFoundBracketPair()
        } else {
            styleDelegate!!.postUpdateBracketPair()
        }
        invalidate()
    }

    /**
     * Set line separator when new lines are created in editor (only texts from IME. texts from clipboard
     * or other strategies are not encountered). Must not be[LineSeparator.NONE]
     * 
     * @param lineSeparator
     * @see .getLineSeparator
     * @see LineSeparator
     */

    /**
     * @see .setLineSeparator
     */

    /**
     * @see CodeEditor.setInputType
     */
    fun getInputType(): Int {
        return inputType
    }

    /**
     * Specify input type for the editor
     * 
     * 
     * Zero for default input type
     * 
     * @see EditorInfo.inputType
     */
    fun setInputType(inputType: Int) {
        this.inputType = inputType
        restartInput()
    }

    /**
     * Undo last action
     */
    fun undo() {
        val range: TextRange? = text.undo()
        if (range != null) {
            try {
                setSelectionRegion(
                    range.start.line,
                    range.start.column,
                    range.end.line,
                    range.end.column,
                    true,
                    SelectionChangeEvent.CAUSE_TEXT_MODIFICATION
                )
            } catch (e: IndexOutOfBoundsException) {
                // Suppressed, typically because an invalid position is memorized.
            }
        }
        notifyIMEExternalCursorChange()
    }

    /**
     * Redo last action
     */
    fun redo() {
        text.redo()
        notifyIMEExternalCursorChange()
    }

    /**
     * Checks whether we can undo
     * 
     * @return true if we can undo
     */
    fun canUndo(): Boolean {
        return text.canUndo()
    }

    /**
     * Checks whether we can redo
     * 
     * @return true if we can redo
     */
    fun canRedo(): Boolean {
        return text.canRedo()
    }

    /**
     * @return Enabled/Disabled
     * @see CodeEditor.setUndoEnabled
     */
    fun isUndoEnabled(): Boolean {
        return undoEnabled
    }

    /**
     * Enable / disabled undo manager
     * 
     * @param enabled Enable/Disable
     */
    fun setUndoEnabled(enabled: Boolean) {
        undoEnabled = enabled
        if (text != null) {
            text.isUndoEnabled = enabled
        }
    }

    val diagnosticIndicatorStyle: DiagnosticIndicatorStyle?
        get() = diagnosticStyle

    fun setDiagnosticIndicatorStyle(@NonNull diagnosticIndicatorStyle: DiagnosticIndicatorStyle?) {
        this.diagnosticStyle = diagnosticIndicatorStyle
        invalidate()
    }

    /**
     * Start search action mode
     */
    fun beginSearchMode() {
        class SearchActionMode : ActionMode.Callback {
            
            override fun onCreateActionMode(p1: ActionMode, p2: Menu): Boolean {
                startedActionMode = ACTION_MODE_SEARCH_TEXT
                p2.add(0, 0, 0, I18nConfig.getResourceId(R.string.sora_editor_next))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                p2.add(0, 1, 0, I18nConfig.getResourceId(R.string.sora_editor_last))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
                p2.add(0, 2, 0, I18nConfig.getResourceId(R.string.sora_editor_replace))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
                p2.add(0, 3, 0, I18nConfig.getResourceId(R.string.sora_editor_replaceAll))
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
                val sv: SearchView = SearchView(getContext())
                sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    
                    override fun onQueryTextSubmit(text: String?): Boolean {
                        _searcher.gotoNext()
                        return false
                    }

                    
                    override fun onQueryTextChange(text: String?): Boolean {
                        if (text == null || text.isEmpty()) {
                            _searcher.stopSearch()
                            return false
                        }
                        _searcher.search(text, SearchOptions(false, false))
                        return false
                    }
                })
                p1.setCustomView(sv)
                sv.performClick()
                sv.setQueryHint(I18nConfig.getString(getContext(), R.string.sora_editor_text_to_search))
                sv.setIconifiedByDefault(false)
                sv.setIconified(false)
                return true
            }

            
            override fun onPrepareActionMode(p1: ActionMode?, p2: Menu?): Boolean {
                return true
            }

            
            override fun onActionItemClicked(am: ActionMode, p2: MenuItem): Boolean {
                if (!_searcher.hasQuery()) {
                    return false
                }
                when (p2.getItemId()) {
                    1 -> _searcher.gotoPrevious()
                    0 -> _searcher.gotoNext()
                    2, 3 -> {
                        val replaceAll = p2.getItemId() === 3
                        val et: EditText = EditText(getContext())
                        et.setHint(I18nConfig.getResourceId(R.string.sora_editor_replacement))
                        android.app.AlertDialog.Builder(context)
                            .setTitle(I18nConfig.getResourceId(if (replaceAll) R.string.sora_editor_replaceAll else R.string.sora_editor_replace))
                            .setView(et)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(
                                I18nConfig.getResourceId(R.string.sora_editor_replace)
                            ) { dialog, which ->
                                if (replaceAll) {
                                    _searcher.replaceAll(et.text.toString(), am::finish)
                                } else {
                                    _searcher.replaceCurrentMatch(et.text.toString())
                                    am.finish()
                                }
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
                return false
            }

            
            override fun onDestroyActionMode(p1: ActionMode?) {
                startedActionMode = ACTION_MODE_NONE
                _searcher.stopSearch()
            }
        }

        val callback: ActionMode.Callback = SearchActionMode()
        startActionMode(callback)
    }

    val eventHandler: EditorTouchEventHandler?
        /**
         * Get [EditorTouchEventHandler] of the editor
         */
        get() = touchHandler!!

    /**
     * Set divider line's left and right margin
     * 
     * @param marginLeft  Margin left for divider line
     * @param marginRight Margin right for divider line
     */
    fun setDividerMargin(@Px marginLeft: Float, @Px marginRight: Float) {
        kotlin.require(!(marginLeft < 0 || marginRight < 0)) { "margin can not be under zero" }
        dividerMarginLeft = marginLeft
        dividerMarginRight = marginRight
        requestLayoutIfNeeded()
        createLayout()
        invalidate()
    }

    /**
     * Set divider line's left and right margin
     * 
     * @param margin Margin left and right for divider line
     */
    fun setDividerMargin(@Px margin: Float) {
        setDividerMargin(margin, margin)
    }

    /**
     * Set line number margin left
     */
    fun setLineNumberMarginLeft(@Px lineNumberMarginLeft: Float) {
        this.lineNumberMarginLeft = lineNumberMarginLeft
        requestLayoutIfNeeded()
        createLayout()
        invalidate()
    }

    /**
     * @see .setLineNumberMarginLeft
     */
    @Px
    fun getLineNumberMarginLeft(): Float {
        return lineNumberMarginLeft
    }

    val typefaceLineNumber: Typeface
        /**
         * @return Typeface of line number
         * @see CodeEditor.setTypefaceLineNumber
         */
        get() = renderer.getPaintOther().typeface

    /**
     * Set line number's typeface
     * 
     * @param typefaceLineNumber New typeface
     */
    fun setTypefaceLineNumber(typefaceLineNumber: Typeface?) {
        renderer.setTypefaceLineNumber(typefaceLineNumber)
        requestLayoutIfNeeded()
    }

    val typefaceText: Typeface

        /**
         * @return Typeface of text
         * @see CodeEditor.setTypefaceText
         */
        get() = renderer.paint.typeface

    /**
     * Set text's typeface
     * 
         * @param typefaceText New typeface
     */
    fun setTypefaceText(typefaceText: Typeface?) {
        renderer.setTypefaceText(typefaceText)
        requestLayoutIfNeeded()
    }

    var textScaleX: Float
        /**
         * @see .setTextScaleX
         */
        get() = renderer.paint.getTextScaleX()
        /**
         * Set text scale x of Paint
         * 
         * @see Paint.setTextScaleX
         * @see .getTextScaleX
         */
        set(textScaleX) {
            renderer.setTextScaleX(textScaleX)
        }

    var textLetterSpacing: Float
        /**
         * @see .setTextLetterSpacing
         */
        get() = renderer.paint.getLetterSpacing()
        /**
         * Set letter spacing of Paint
         * 
         * @see Paint.setLetterSpacing
         * @see .getTextLetterSpacing
         */
        set(textLetterSpacing) {
            renderer.setLetterSpacing(textLetterSpacing)
            requestLayoutIfNeeded()
        }

    /**
     * @return Line number align
     * @see CodeEditor.setLineNumberPaint.Align
     */
    fun getLineNumberAlign(): AndroidPaint.Align? {
        return lineNumberAlign
    }

    /**
     * Set line number align
     * 
     * @param align Paint.Align for line number
     */
    fun setLineNumberPaintAlign(align: AndroidPaint.Align?) {
        var align: AndroidPaint.Align? = align
        if (align == null) {
            align = AndroidPaint.Align.LEFT
        }
        lineNumberAlign = align
        invalidate()
    }

    /**
     * Width for insert cursor
     * 
     * @param width Cursor width
     */
    fun setCursorWidth(@Px width: Float) {
        kotlin.require(!(width < 0)) { "width can not be under zero" }
        insertSelectionWidth = width
        invalidate()
    }

    /**
     * Border width for text border
     */
    fun setTextBorderWidth(@Px width: Float) {
        kotlin.require(!(width < 0)) { "width can not be under zero" }
        textBorderWidth = width
        invalidate()
    }

    /**
     * @see .setTextBorderWidth
     */
    @Px
    fun getTextBorderWidth(): Float {
        return textBorderWidth
    }

    /**
     * Get text cursor.
     * 
     * 
     * Always set selection position by [.setSelection] or [.setSelectionRegion].
     * Do not modify the object returned.
     * 
     * @return Cursor of text
     */

    val lineCount: Int
        /**
         * Get line count
         * 
         * @return line count
         */
        get() = text.lineCount

    val firstVisibleLine: Int
        /**
         * Get first visible line on screen
         * 
         * @return first visible line
         */
        get() {
            try {
                return layout!!.getLineNumberForRow(this.firstVisibleRow)
            } catch (e: IndexOutOfBoundsException) {
                return 0
            }
        }

    val firstVisibleRow: Int
        /**
         * Get first visible row on screen
         * 
         * @return first visible row
         */
        get() {
            if (layout!! == null) return (this.offsetY / this.logicalRowHeight).toInt()
            return layout!!.getRowIndexForY(this.offsetY.toFloat())
        }

    val lastVisibleRow: Int
        /**
         * Get last visible row on screen.
         * 
         * @return last visible row
         */
        get() {
            if (layout!! == null) return ((this.offsetY + height) / this.logicalRowHeight).toInt()
            return kotlin.math.max(0, kotlin.math.min(layout!!.rowCount - 1, layout!!.getRowIndexForY((this.offsetY + height).toFloat())))
        }

    val lastVisibleLine: Int
        /**
         * Get last visible line on screen
         * 
         * @return last visible line
         */
        get() {
            try {
                return layout!!.getLineNumberForRow(this.lastVisibleRow)
            } catch (e: IndexOutOfBoundsException) {
                return this.lineCount - 1
            }
        }

    /**
     * Checks whether this row is visible on screen
     * 
     * @param row Row to check
     * @return Whether visible
     */
    fun isRowVisible(row: Int): Boolean {
        return (this.firstVisibleRow <= row && row <= this.lastVisibleRow)
    }

    /**
     * Sets line spacing for this TextView. Each line other than the last line will have its height
     * multiplied by `mult` and have `add` added to it.
     * 
     * @param add  The value in pixels that should be added to each line other than the last line.
     * This will be applied after the multiplier
     * @param mult The value by which each line height other than the last line will be multiplied
     * by
     */
    fun setLineSpacing(add: Float, mult: Float) {
        lineSpacingAdd = add
        lineSpacingMultiplier = mult
        requestLayout()
        invalidate()
    }

    /**
     * Set wrap line spacing
     */
    fun setWrapLineSpacing(add: Float, mult: Float) {
        wrapLineSpacingAdd = add
        wrapLineSpacingMultiplier = mult
        requestLayout()
        invalidate()
    }

    var lineSpacingExtra: Float
        /**
         * Gets the line spacing extra space
         * 
         * @return the extra space that is added to the height of each lines of this TextView.
         * @see .setLineSpacing
         * @see .getLineSpacingMultiplier
         */
        get() = lineSpacingAdd
        /**
         * @param lineSpacingExtra The value in pixels that should be added to each line other than the last line.
         * *            This will be applied after the multiplier
         */
        set(lineSpacingExtra) {
            lineSpacingAdd = lineSpacingExtra
            invalidate()
        }

    /**
     * @return the value by which each line's height is multiplied to get its actual height.
     * @see .setLineSpacingMultiplier
     */
    fun getLineSpacingMultiplier(): Float {
        return lineSpacingMultiplier
    }

    /**
     * @param lineSpacingMultiplier The value by which each line height other than the last line will be multiplied
     * *             by. Default 1.0f
     */
    fun setLineSpacingMultiplier(lineSpacingMultiplier: Float) {
        this.lineSpacingMultiplier = lineSpacingMultiplier
        invalidate()
    }

    val lineSpacingPixels: Int
        /**
         * Get actual line spacing in pixels.
         */
        get() = getLineSpacingPixels(lineSpacingMultiplier, lineSpacingAdd)

    /**
     * Get actual line spacing in pixels.
     */
    fun getLineSpacingPixels(multiplier: Float, add: Float): Int {
        val metrics: android.graphics.Paint.FontMetricsInt? =
            renderer.metricsText
        return (((metrics!!.descent - metrics.ascent) * (multiplier - 1f) + add).toInt()) / 2 * 2
    }

    /**
     * Get baseline directly
     * 
     * @param row Row
     * @return baseline y offset
     */
    fun getRowBaseline(row: Int): Int {
        val lineSpacing = this.lineSpacingPixels
        val metrics: android.graphics.Paint.FontMetricsInt? =
            renderer.metricsText
        return kotlin.math.max(
            1,
            metrics!!.descent - metrics.ascent + lineSpacing
        ) * (row + 1) - metrics.descent - lineSpacing / 2
    }

    val rowHeight: Int
        /**
         * Get row height
         * 
         * @return height of single row
         */
        get() = this.logicalRowHeight

    val logicalRowHeight: Int
        /**
         * Get logical row height
         */
        get() {
            val metrics: android.graphics.Paint.FontMetricsInt? =
                renderer.metricsText
            return kotlin.math.max(
                1,
                metrics!!.descent - metrics.ascent + getLineSpacingPixels(lineSpacingMultiplier, lineSpacingAdd)
            )
        }

    val wrapRowHeight: Int
        /**
         * Get wrap row height
         */
        get() {
            val metrics: android.graphics.Paint.FontMetricsInt? =
                renderer.metricsText
            return kotlin.math.max(
                1,
                metrics!!.descent - metrics.ascent + getLineSpacingPixels(wrapLineSpacingMultiplier, wrapLineSpacingAdd)
            )
        }

    /**
     * Get row height for specific row
     */
    fun getRowHeight(row: Int): Int {
        if (layout!! == null) return this.rowHeight
        return if (layout!!.getRowAt(row).isTrailingRow) this.logicalRowHeight else this.wrapRowHeight
    }

    /**
     * Get row top y offset
     * 
     * @param row Row
     * @return top y offset
     */
    fun getRowTop(row: Int): Int {
        if (layout!! == null) return this.logicalRowHeight * row
        return layout!!.getRowTop(row)
    }

    /**
     * Get row bottom y offset
     * 
     * @param row Row
     * @return Bottom y offset
     */
    fun getRowBottom(row: Int): Int {
        if (layout!! == null) return this.logicalRowHeight * (row + 1)
        return layout!!.getRowBottom(row)
    }

    /**
     * Get the top of text in target row
     */
    fun getRowTopOfText(row: Int): Int {
        return getRowTop(row) + this.lineSpacingPixels / 2
    }

    /**
     * Get the bottom of text in target row
     */
    fun getRowBottomOfText(row: Int): Int {
        return getRowBottom(row) - this.lineSpacingPixels / 2
    }

    val rowHeightOfText: Int
        /**
         * Get the height of text in row
         */
        get() {
            val metrics = renderer.metricsText!!
            return (metrics.descent - metrics.ascent)
        }

    val offsetX: Int
        /**
         * Get scroll x
         * 
         * @return scroll x
         */
        get() = touchHandler!!.getScroller().getCurrX()

    val offsetY: Int
        /**
         * Get scroll y
         * 
         * @return scroll y
         */
        get() = touchHandler!!.getScroller().getCurrY()

    /**
     * Indicate whether the layout!! is working
     */
    @UnsupportedUserUsage
    public fun setLayoutBusy(busy: Boolean) {

        if (layoutBusy && !busy) {
            if (isWordwrap && touchHandler!!.positionNotApplied) {
                touchHandler!!.positionNotApplied = false
                val line: Int = IntPair.getFirst(touchHandler!!.memoryPosition)
                val column: Int = IntPair.getSecond(touchHandler!!.memoryPosition)
                // Compute new scroll position
                val row: Int =
                    (layout!! as WordwrapLayout).findRow(line, column)
                val afterScrollY: Float =
                    row.toFloat() * this.rowHeight - touchHandler!!.focusY
                val scroller: EditorScroller? =
                    touchHandler!!.getScroller()
                dispatchEvent(
                    ScrollEvent(
                        this, scroller!!.getCurrX().toInt(),
                        scroller.getCurrY().toInt(), 0, afterScrollY.toInt(), ScrollEvent.CAUSE_SCALE_TEXT
                    )
                )
                scroller.startScroll(0, afterScrollY.toInt(), 0, 0, 0)
                scroller.abortAnimation()
            }
            // IMPORTANT restart input after clearing the busy flag
            // otherwise, the connection may fallback to inactive mode
            this.layoutBusy = false
            restartInput()
            postInvalidate()
            dispatchEvent(LayoutStateChangeEvent(this, false))
            return
        }
        if (layoutBusy == busy) {
            return
        }
        this.layoutBusy = busy
        dispatchEvent(LayoutStateChangeEvent(this, busy))
    }

    /**
     * Check whether the editor is actually editable. This is not only related to user
     * property 'editable', but also editor states. When the editor is busy at initializing
     * its layout!! or awaiting the result of format, it is also not editable.
     * 
     * 
     * Do not modify the text externally in editor when this method returns false.
     * 
     * @return Whether the editor is editable, actually.
     * @see CodeEditor.setEditable
     * @see CodeEditor.setLayoutBusy
     * @see .isFormatting
     */


    /**
     * Set whether text can be edited
     * 
     * @param editable Editable
     */

    
    fun isBlockLineEnabled(): Boolean {
        return blockLineEnabled
    }

    fun setBlockLineEnabled(enabled: Boolean) {
        blockLineEnabled = enabled
        invalidate()
    }

    /**
     * Begin a rejection on composing texts
     */
    fun beginComposingTextRejection() {
        rejectComposingCount++
    }

    /**
     * If the editor accepts composing text now, according to composing text rejection count
     */
    fun acceptsComposingText(): Boolean {
        return rejectComposingCount == 0
    }

    /**
     * End a rejection on composing texts
     */
    fun endComposingTextRejection() {
        rejectComposingCount--
        if (rejectComposingCount < 0) {
            rejectComposingCount = 0
        }
    }

    /**
     * Check if there is a mouse inside editor, hovering
     */
    fun hasMouseHovering(): Boolean {
        return mouseHover
    }

    /**
     * Check if there is a mouse inside editor with any button pressed
     */
    fun hasMousePressed(): Boolean {
        return mouseButtonPressed
    }

    val isInMouseMode: Boolean
        /**
         * Check if editor is in mouse mode.
         * 
         * @see DirectAccessprops!!.mouseMode
         */
        get() {
            when (props!!.mouseMode) {
                DirectAccessProps.MOUSE_MODE_ALWAYS -> {
                    return true
                }

                DirectAccessProps.MOUSE_MODE_NEVER -> {
                    return false
                }
            }
            // MOUSE_MODE_AUTO
            return hasMouseHovering() || hasMousePressed()
        }

    internal val selectingTarget: CharPosition
        /**
         * Get the target cursor to move when shift is pressed
         */
        get() {
            if (cursor!!.left().equals(selectionAnchor)) {
                return cursor!!.right()
            } else {
                return cursor!!.left()
            }
        }

    /**
     * Make sure the moving selection is visible
     */
    protected fun ensureSelectingTargetVisible() {
        if (cursor!!.left().equals(selectionAnchor)) {
            // Ensure right selection visible
            ensureSelectionVisible()
        } else {
            ensurePositionVisible(cursor!!.leftLine, cursor!!.leftColumn)
        }
    }

    protected fun ensureSelectionAnchorAvailable() {
        if (selectionAnchor == null || !text.isValidPosition(selectionAnchor!!)) {
            selectionAnchor = cursor!!.right()
        }
    }

    /**
     * Move or extend selection, according to `extend` param.
     * 
     * @param extend True if you want to extend selection.
     */
    fun moveOrExtendSelection(@NonNull movement: SelectionMovement, extend: Boolean) {
        if (extend) {
            extendSelection(movement)
        } else {
            moveSelection(movement)
        }
    }

    /**
     * Extend the selection, based on the selection anchor (select text)
     */
    fun extendSelection(@NonNull movement: SelectionMovement) {
        ensureSelectionAnchorAvailable()
        val sel: CharPosition = movement.getPositionAfterMovement(this, this.selectingTarget)
        setSelectionRegion(
            selectionAnchor!!.line,
            selectionAnchor!!.column,
            sel.line,
            sel.column,
            false,
            SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE
        )
        if (movement === SelectionMovement.PAGE_UP) {
            touchHandler!!.scrollBy(0f, -height.toFloat(), true)
        } else if (movement === SelectionMovement.PAGE_DOWN) {
            touchHandler!!.scrollBy(0f, height.toFloat(), true)
        }
        ensureSelectingTargetVisible()
    }

    /**
     * Move the selection. Selected text will be de-selected.
     */
    fun moveSelection(@NonNull movement: SelectionMovement) {
        if (cursor!!.isSelected()) {
            if (movement === SelectionMovement.LEFT) {
                setSelection(cursor!!.leftLine, cursor!!.leftColumn, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
                return
            }
            if (movement === SelectionMovement.RIGHT) {
                setSelection(
                    cursor!!.rightLine,
                    cursor!!.rightColumn,
                    SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE
                )
                return
            }
        }
        val pos: CharPosition?
        when (movement.basePosition) {
            SelectionMovement.MovingBasePosition.LEFT_SELECTION -> pos = cursor!!.left()
            SelectionMovement.MovingBasePosition.RIGHT_SELECTION -> pos = cursor!!.right()
            else -> {
                selectionAnchor = cursor!!.right()

                pos = selectionAnchor
            }
        }
        val sel: CharPosition = movement.getPositionAfterMovement(this, pos!!)
        if (movement === SelectionMovement.PAGE_UP) {
            touchHandler!!.scrollBy(0f, -height.toFloat(), true)
        } else if (movement === SelectionMovement.PAGE_DOWN) {
            touchHandler!!.scrollBy(0f, height.toFloat(), true)
        }
        setSelection(sel.line, sel.column, SelectionChangeEvent.CAUSE_KEYBOARD_OR_CODE)
    }

    /**
     * Move selection to given position
     * 
     * @param line   The line to move
     * @param column The column to move
     */
    fun setSelection(line: Int, column: Int) {
        setSelection(line, column, SelectionChangeEvent.CAUSE_UNKNOWN)
    }

    /**
     * Move selection to given position
     * 
     * @param line   The line to move
     * @param column The column to move
     */
    fun setSelection(line: Int, column: Int, cause: Int) {
        setSelection(line, column, true, cause)
    }

    /**
     * Move selection to given position
     * 
     * @param line          The line to move
     * @param column        The column to move
     * @param makeItVisible Make the character visible
     */
    fun setSelection(line: Int, column: Int, makeItVisible: Boolean) {
        setSelection(line, column, makeItVisible, SelectionChangeEvent.CAUSE_UNKNOWN)
    }

    /**
     * Move selection to given position
     * 
     * @param line          The line to move
     * @param column        The column to move
     * @param makeItVisible Make the character visible
     */
    fun setSelection(line: Int, column: Int, makeItVisible: Boolean, cause: Int) {
        var column = column
        _cursorAnimator!!.markStartPos()
        if (column > 0 && Character.isHighSurrogate(text.getLineString(line)[column - 1])) {
            column++
            if (column > text.getColumnCount(line)) {
                column--
            }
        }
        cursor!!.set(line, column)
        if (highlightCurrentBlock) {
            this.blockIndex = findCursorBlock()
        }
        updateCursor()
        updateSelection()
        if (isEditable && !touchHandler!!.hasAnyHeldHandle() && acceptsComposingText()) {
            _cursorAnimator!!.markEndPos()
            _cursorAnimator!!.start()
        }

        // Update cursor anchor
        selectionAnchor = cursor!!.right()

        renderContext?.invalidateRenderNodes()
        if (makeItVisible) {
            ensurePositionVisible(line, column)
        } else {
            invalidate()
        }
        onSelectionChanged(cause)
    }

    /**
     * Select all text
     */
    fun selectAll() {
        setSelectionRegion(0, 0, this.lineCount - 1, text.getColumnCount(this.lineCount - 1))
    }

    /**
     * Set selection region with a call to [CodeEditor.ensureSelectionVisible]
     * 
     * @param lineLeft    Line left
     * @param columnLeft  Column Left
     * @param lineRight   Line right
     * @param columnRight Column right
     */
    fun setSelectionRegion(
        lineLeft: Int, columnLeft: Int, lineRight: Int,
        columnRight: Int, cause: Int
    ) {
        setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, true, cause)
    }

    /**
     * Set selection region with a call to [CodeEditor.ensureSelectionVisible]
     * 
     * @param lineLeft    Line left
     * @param columnLeft  Column Left
     * @param lineRight   Line right
     * @param columnRight Column right
     */
    fun setSelectionRegion(
        lineLeft: Int, columnLeft: Int, lineRight: Int,
        columnRight: Int
    ) {
        setSelectionRegion(lineLeft, columnLeft, lineRight, columnRight, true, SelectionChangeEvent.CAUSE_UNKNOWN)
    }

    /**
     * Set selection region
     * 
     * @param lineLeft         Line left
     * @param columnLeft       Column Left
     * @param lineRight        Line right
     * @param columnRight      Column right
     * @param makeRightVisible Whether to make right cursor visible
     */
    fun setSelectionRegion(
        lineLeft: Int, columnLeft: Int, lineRight: Int,
        columnRight: Int, makeRightVisible: Boolean
    ) {
        setSelectionRegion(
            lineLeft,
            columnLeft,
            lineRight,
            columnRight,
            makeRightVisible,
            SelectionChangeEvent.CAUSE_UNKNOWN
        )
    }

    /**
     * Set selection region
     * 
     * @param lineLeft         Line left
     * @param columnLeft       Column Left
     * @param lineRight        Line right
     * @param columnRight      Column right
     * @param makeRightVisible Whether to make right cursor visible
     */
    fun setSelectionRegion(
        lineLeft: Int, columnLeft: Int, lineRight: Int,
        columnRight: Int, makeRightVisible: Boolean, cause: Int
    ) {
        var columnLeft = columnLeft
        var columnRight = columnRight
        requestFocus()
        val start: Int = text.getCharIndex(lineLeft, columnLeft)
        val end: Int = text.getCharIndex(lineRight, columnRight)
        if (start == end) {
            setSelection(lineLeft, columnLeft, makeRightVisible, cause)
            return
        }
        if (start > end) {
            setSelectionRegion(lineRight, columnRight, lineLeft, columnLeft, makeRightVisible, cause)
            Log.w(
                LOG_TAG,
                "setSelectionRegion() error: start > end:start = " + start + " end = " + end + " lineLeft = " + lineLeft + " columnLeft = " + columnLeft + " lineRight = " + lineRight + " columnRight = " + columnRight
            )
            return
        }
        _cursorAnimator!!.cancel()
        val lastState: Boolean = cursor!!.isSelected()
        if (columnLeft > 0) {
            val column = columnLeft - 1
            val ch: Char = text.getLineString(lineLeft)[column]
            if (Character.isHighSurrogate(ch)) {
                columnLeft++
                if (columnLeft > text.getColumnCount(lineLeft)) {
                    columnLeft--
                }
            }
        }
        if (columnRight > 0) {
            val column = columnRight - 1
            val ch: Char = text.getLineString(lineRight)[column]
            if (Character.isHighSurrogate(ch)) {
                columnRight++
                if (columnRight > text.getColumnCount(lineRight)) {
                    columnRight--
                }
            }
        }
        cursor!!.setLeft(lineLeft, columnLeft)
        cursor!!.setRight(lineRight, columnRight)
        updateCursor()
        updateSelection()
        renderContext?.invalidateRenderNodes()

        // Update selection anchor
        if (!cursor!!.left().equals(selectionAnchor) && !cursor!!.right().equals(selectionAnchor)) {
            selectionAnchor = cursor!!.right()
        }

        if (makeRightVisible) {
            if (cause == SelectionChangeEvent.CAUSE_SEARCH) {
                ensurePositionVisible(lineLeft, columnLeft)
                lastMakeVisible = 0
                ensurePositionVisible(lineRight, columnRight)
            } else {
                ensurePositionVisible(lineRight, columnRight)
            }
        } else {
            invalidate()
        }
        onSelectionChanged(cause)
    }

    /**
     * Get system clipboard manager used by editor
     */

    /**
     * Paste text from clip board
     */
    fun pasteText() {
        try {
            var clip: ClipData? = null
            if (!clipboardManager!!.hasPrimaryClip() || (clipboardManager!!.getPrimaryClip().also { clip = it }) == null) {
                return
            }
            pasteText(ClipDataUtils.clipDataToString(clip))
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error pasting text to editor", e)
            Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Paste external text into editor
     */
    fun pasteText(@Nullable text: CharSequence?) {
        if (text != null && inputConnection != null) {
            inputConnection!!.commitText(text, 1)
            if (props!!.formatPastedText) {
                formatCodeAsync(lastInsertion!!.getStart(), lastInsertion!!.getEnd())
            }
            notifyIMEExternalCursorChange()
        }
    }

    /**
     * Copy text to clipboard.
     * 
     * @param shouldCopyLine State whether the editor should select whole line if
     * cursor is not in selection mode.
     */
    /**
     * Copy text to clipboard.
     */
    @JvmOverloads
    fun copyText(shouldCopyLine: Boolean = true) {
        if (cursor!!.isSelected()) {
            copyTextToClipboard(this.text, cursor!!.left, cursor!!.right)
        } else if (shouldCopyLine) {
            copyLine()
        } else {
            val text: CharSequence? =
                lineSeparator?.content
            copyTextToClipboard(text!!, 0, text.length)
        }
    }

    /**
     * Copy the given text region to clipboard, and follow editor's IPC properties.
     */
    protected fun copyTextToClipboard(@NonNull text: CharSequence, start: Int, end: Int) {
        if (end < start) {
            return
        }
        if (end - start > props!!.clipboardTextLengthLimit) {
            Toast.makeText(
                getContext(),
                I18nConfig.getResourceId(R.string.sora_editor_clip_text_length_too_large),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        try {
            val clip: CharSequence? =
                if (text is Content) (text as Content).substring(start, end) else text.subSequence(start, end)
                    .toString()
            clipboardManager!!.setPrimaryClip(ClipData.newPlainText(clip, clip))
        } catch (e: RuntimeException) {
            if (e.cause is TransactionTooLargeException) {
                Toast.makeText(
                    getContext(),
                    I18nConfig.getResourceId(R.string.sora_editor_clip_text_length_too_large),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.w(LOG_TAG, e)
                Toast.makeText(getContext(), e.javaClass.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Copies the current line to clipboard.
     */
    private fun copyLine() {
        val cursor: Cursor = cursor
        if (cursor.isSelected()) {
            copyText()
            return
        }

        val line: Int? = cursor.left().line
        setSelectionRegion(line!!, 0, line, text.getColumnCount(line))
        copyText(false)
    }

    /**
     * Copy text to clipboard and delete them
     */
    fun cutText() {
        if (cursor!!.isSelected()) {
            copyText()
            deleteText()
            notifyIMEExternalCursorChange()
        } else {
            cutLine()
        }
    }

    /**
     * Copy the current line to clipboard and delete it.
     */
    fun cutLine() {
        val cursor: Cursor = cursor
        if (cursor.isSelected()) {
            cutText()
            return
        }

        val left: CharPosition? = cursor.left()
        val line: Int? = left!!.line
        val column: Int? =
            text.getColumnCount(left.line)

        if (line!! + 1 == this.lineCount) {
            val columnCount: Int = text.getColumnCount(line)
            if (columnCount == 0) {
                // copy line separator
                copyText(false)
                return
            }
            setSelectionRegion(line, 0, line, text.getColumnCount(line))
        } else {
            setSelectionRegion(line, 0, line + 1, 0)
        }

        cutText()
        if (props!!.placeSelOnPreviousLineAfterCut) {
            moveSelection(SelectionMovement.LEFT)
        }
    }

    /**
     * Duplicates the current line.
     * Does not selects the duplicated line.
     */
    fun duplicateLine() {
        val cursor: Cursor = cursor
        if (cursor.isSelected()) {
            duplicateSelection()
            return
        }

        val left: CharPosition? = cursor.left()
        setSelectionRegion(left!!.line, 0, left.line, text.getColumnCount(left.line), true)
        duplicateSelection("\n", false)
    }

    /**
     * Copies the current selection and pastes it at the right selection handle.
     * 
     * @param selectDuplicate Whether to select the duplicated content.
     */
    /**
     * Copies the current selection and pastes it at the right selection handle,
     * then selects the duplicated content.
     */
    @JvmOverloads
    fun duplicateSelection(selectDuplicate: Boolean = true) {
        duplicateSelection("", selectDuplicate)
    }

    /**
     * Copies the current selection, add the `prefix` to it
     * and pastes it at the right selection handle.
     * 
     * @param prefix          The prefix for the selected content.
     * @param selectDuplicate Whether to select the duplicated content.
     */
    fun duplicateSelection(prefix: String?, selectDuplicate: Boolean) {
        val cursor: Cursor = cursor
        if (!cursor.isSelected()) {
            return
        }

        val left: CharPosition? = cursor.left()
        val right: CharPosition? =
            cursor.right().fromThis()
        val sub: CharSequence? =
            text.subContent(left!!.line, left.column, right!!.line, right.column)

        setSelection(right.line, right.column)
        commitText(prefix + sub, false)

        if (selectDuplicate) {
            val r: CharPosition? = cursor.right()
            setSelectionRegion(right.line, right.column, r!!.line, r.column)
        }
    }

    /**
     * Selects the word at the left selection handle.
     */
    fun selectCurrentWord() {
        val left: CharPosition? = cursor.left()
        selectWord(left!!.line, left.column)
    }

    /**
     * Selects the word at the given character position.
     * 
     * @param line   The line.
     * @param column The column.
     */
    fun selectWord(line: Int, column: Int) {
        val range: TextRange = getWordRange(line, column)
        val start: CharPosition? = range.getStart()
        val end: CharPosition? = range.getEnd()
        setSelectionRegion(start!!.line, start.column, end!!.line, end.column, SelectionChangeEvent.CAUSE_LONG_PRESS)
    }

    /**
     * @see .getWordRange
     */
    fun getWordRange(line: Int, column: Int): TextRange {
        return getWordRange(line, column, props!!.useICULibToSelectWords)
    }

    /**
     * Get the range of the word at given character position.
     * 
     * @param line   The line.
     * @param column The column.
     * @param useIcu Whether to use the ICU library to get word edges.
     * @return The word range.
     */
    fun getWordRange(line: Int, column: Int, useIcu: Boolean): TextRange {
        return Chars.getWordRange(text, line, column, useIcu)
    }

    /**
     * @return The text displaying.
     * **Changes to this object are expected to be done in main thread
     * due to editor limitations, while the object can be read concurrently.**
     * 
     * @see CodeEditor.setText
     * @see CodeEditor.setText
     */


    /**
     * Set the text to be displayed.
     * With no extra arguments.
     * 
     * @param text the new text you want to display
     */
    fun setText(@Nullable text: CharSequence?) {
        setText(text, true, null)
    }

    /**
     * Get extra argument set by [CodeEditor.setText]
     */
    @NonNull
    fun getExtraArguments(): Bundle? {
        return extraArguments
    }

    /**
     * Sets the text to be displayed.
     * 
     * @param text           the new text you want to display
     * @param extraArguments Extra arguments for the document. This [Bundle] object is passed
     * to all languages and plugins in editor.
     */
    fun setText(@Nullable text: CharSequence?, @Nullable extraArguments: Bundle?) {
        setText(text, true, extraArguments)
    }

    /**
     * Sets the text to be displayed.
     * 
     * @param text               the new text you want to display
     * @param reuseContentObject If the given `text` is an instance of [Content], reuse it.
     * @param extraArguments     Extra arguments for the document. This [Bundle] object is passed
     * to all languages and plugins in editor.
     */
    fun setText(
        @Nullable text: CharSequence?, reuseContentObject: Boolean,
        @Nullable extraArguments: Bundle?
    ) {
        forceSyncBreakLines = true
        var text = text
        if (text == null) {
            text = ""
        }

        if (_text != null) {
            _text!!.removeContentListener(this)
            _text!!.resetBatchEdit()
        }
        this.extraArguments = if (extraArguments == null) Bundle() else extraArguments
        lastInsertion = null
        if (reuseContentObject && text is Content) {
            _text = text as Content
            _text!!.resetBatchEdit()
            renderer!!.updateTimestamp()
        } else {
            _text = Content(text)
        }
        styleDelegate?.reset()
        textStyles = null
        cursor = _text!!.cursor
        selectionAnchor = cursor!!.right()
        touchHandler?.reset()
        _text!!.addContentListener(this)
        _text!!.isUndoEnabled = undoEnabled
        _text!!.setBidiEnabled(true)
        renderContext?.reset(_text!!.lineCount)
        renderer?.onEditorFullTextUpdate()

        if (editorLanguage != null) {
            editorLanguage!!.analyzeManager.reset(ContentReference(_text!!), this.extraArguments!!)
            editorLanguage!!.formatter.cancel()
        }
        inlayHints = null

        dispatchEvent(
            ContentChangeEvent(
                this, ContentChangeEvent.ACTION_SET_NEW_TEXT, CharPosition(), _text!!.indexer.getCharPosition(
                    this.lineCount - 1, _text!!.getColumnCount(this.lineCount - 1)
                ), _text!!, false
            )
        )
        createLayout()
        inputMethodManager?.let { imm ->
            imm.viewClicked(this)
            imm.showSoftInput(this, 0)
        }
        renderContext?.invalidateRenderNodes()
        invalidate()
    }

    /**
     * Set the editor's text size in sp unit. This value must be greater than 0
     * 
     * @param textSize the editor's text size in **Sp** units.
     */
    fun setTextSize(textSize: Float) {
        val context: Context? = getContext()
        val res: Resources

        if (context == null) {
            res = Resources.getSystem()
        } else {
            res = context.resources
        }

        this.textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, res.displayMetrics)
    }

    /**
     * Render ASCII Function characters
     */



    /**
     * Subscribe event of the given type.
     * 
     * @see EventManager.subscribeEvent
     */
    fun <T : Event> subscribeEvent(eventType: Class<T>, receiver: EventReceiver<T>): SubscriptionReceipt<T> {
        return eventManager!!.subscribeEvent(eventType, receiver)
    }

    /**
     * Subscribe event of the given type, without [io.github.abc15018045126.sora.event.Unsubscribe].
     * 
     * @param eventType The type of event to subscribe.
     * @param receiver  The receiver to be called when event is dispatched.
     * @param <T>       Type of event.
     * @return A receipt that can be used to unsubscribe.
     * @see EventManager.subscribeEvent
     */
    fun <T : Event> subscribeAlways(
        eventType: Class<T>,
        receiver: EventManager.NoUnsubscribeReceiver<T>
    ): SubscriptionReceipt<T> {
        return eventManager!!.subscribeAlways(eventType, receiver)
    }

    /**
     * Dispatch the given event
     * 
     * @see EventManager.dispatchEvent
     */
    fun <T : Event> dispatchEvent(event: T): Int {
        return eventManager!!.dispatchEvent(event)
    }

    /**
     * Create a new [EventManager] instance that can be used to subscribe events in editor,
     * as a child instance of editor.
     * 
     * @return Child EventManager instance
     */
    @NonNull
    fun createSubEventManager(): EventManager {
        return EventManager(eventManager)
    }



    val isFormatting: Boolean
        /**
         * Check whether the editor is currently performing a format operation
         * 
         * @return whether the editor is currently formatting
         */
        get() = editorLanguage!!.formatter.isRunning()

    @get:NonNull
    val textPaint: Paint
        /**
         * Get the paint of the editor
         * You should not change text size and other attributes that are related to text measuring by the object
         * 
         * @return The paint which is used by the editor now
         */
        get() = renderer.paint

    val otherPaint: Paint
        get() = renderer.getPaintOther()

    val graphPaint: Paint
        get() = renderer.getPaintGraph()

    /**
     * Move selection to line start with scrolling
     * 
     * @param line Line index to jump
     */
    fun jumpToLine(line: Int) {
        setSelection(line, 0)
    }

    /**
     * Mark current selection position as a point of cursor range.
     * When user taps to select another point in text, the text between the marked point and
     * newly chosen point is selected.
     * 
     * @see .isInLongSelect
     * @see .endLongSelect
     */
    fun beginLongSelect() {
        if (!isEditable) {
            return
        }
        if (cursor!!.isSelected()) {
            setSelection(cursor!!.leftLine, cursor!!.leftColumn)
        }
        isInLongSelect = true
        invalidate()
    }

    /**
     * Marks long select mode is end.
     * This does nothing but set the flag to false.
     */
    fun endLongSelect() {
        isInLongSelect = false
    }


    //-------------------------------------------------------------------------------
    //-------------------------IME Interaction---------------------------------------
    //-------------------------------------------------------------------------------
    /**
     * Rerun analysis forcibly
     */
    fun rerunAnalysis() {
        if (editorLanguage != null) {
            editorLanguage!!.analyzeManager.rerun()
        }
    }

    @get:Nullable
    val styles: Styles?
        /**
         * Get analyze result.
         * **Do not make changes to it or read concurrently**
         */
        get() = textStyles

    @UiThread
    fun setStyles(@Nullable styles: Styles?) {
        textStyles = styles
        if (highlightCurrentBlock) {
            this.blockIndex = findCursorBlock()
        }
        renderContext?.invalidateRenderNodes()
        renderer.updateTimestamp()
        invalidate()
    }

    @UiThread
    fun updateStyles(@NonNull styles: Styles?, @Nullable range: StyleUpdateRange?) {
        if (textStyles !== styles || range == null) {
            setStyles(styles)
            return
        }
        if (highlightCurrentBlock) {
            this.blockIndex = findCursorBlock()
        }
        renderContext?.updateForRange(range)
        renderer.updateTimestamp()
        invalidate()
    }

    @get:Nullable
    val highlightTexts: HighlightTextContainer?
        get() = highlightTextContainer

    /**
     * Hide auto complete window if shown
     */
    fun hideAutoCompleteWindow() {
        if (completionWindow != null) {
            completionWindow!!.hide()
        }
    }

    /**
     * Display soft input method for self
     */
    fun showSoftInput() {
        if (isEditable && isEnabled()) {
            // Note that we always try to get focused at this time.
            if (isInTouchMode() && !isFocused()) {
                requestFocusFromTouch()
            }
            if (!isFocused()) {
                requestFocus()
            }

            if (checkSoftInputEnabled()) inputMethodManager!!.showSoftInput(this, 0)
        }
        invalidate()
    }

    /**
     * Hide soft input
     */
    fun hideSoftInput() {
        inputMethodManager!!.hideSoftInputFromWindow(windowToken, 0)
    }

    /**
     * Check whether the soft keyboard is enabled for this editor. Unlike [.isSoftKeyboardEnabled],
     * this method also checks whether a hardware keyboard is connected.
     * 
     * @return Whether the editor should show soft keyboard.
     * @see .isSoftKeyboardEnabled
     * @see .isDisableSoftKbdIfHardKbdAvailable
     */
    protected fun checkSoftInputEnabled(): Boolean {
        if (this.isDisableSoftKbdIfHardKbdAvailable
            && KeyboardUtils.isHardKeyboardConnected(getContext())
        ) {
            return false
        }
        return this.isSoftKeyboardEnabled
    }

    var isSoftKeyboardEnabled: Boolean
        /**
         * Returns whether the soft keyboard is enabled.
         * 
         * @return Whether the soft keyboard is enabled.
         */
        get() = this.isSoftKbdEnabled
        /**
         * Set whether the soft keyboard is enabled for this editor. Set to `true` by default.
         * 
         * @param isEnabled Whether the soft keyboard is enabled.
         */
        set(isEnabled) {
            if (isSoftKbdEnabled == isEnabled) {
                // no need to do anything
                return
            }

            this.isSoftKbdEnabled = isEnabled
            hideSoftInput()
            restartInput()
        }

    var isDisableSoftKbdIfHardKbdAvailable: Boolean
        /**
         * Returns whether the soft keyboard should be enabled if hardware keyboard is connected.
         * 
         * @return Whether the soft keyboard should be enabled if hardware keyboard is connected.
         */
        get() = isDisableSoftKbdOnHardKbd
        /**
         * Set whether the soft keyboard should be disabled for this editor if a hardware keyboard is
         * connected to the device. Set to `true` by default.
         * 
         * @param isDisabled Whether the soft keyboard should be enabled if hardware keyboard is connected.
         */
        set(isDisabled) {
            if (isDisableSoftKbdOnHardKbd == isDisabled) {
                // no need to do anything
                return
            }

            this.isDisableSoftKbdOnHardKbd = isDisabled
            hideSoftInput()
            restartInput()
        }

    /**
     * Send current selection position to input method
     */
    internal fun updateSelection() {
        if (props!!.disallowSuggestions) {
            val index: Int? =
                Random().nextInt()
            inputMethodManager!!.updateSelection(this, index!!, index, -1, -1)
            return
        }
        if (inputConnection!!.composingText.preSetComposing) {
            return
        }
        var candidatesStart = -1
        var candidatesEnd = -1
        if (inputConnection!!.composingText.isComposing()) {
            try {
                candidatesStart = inputConnection!!.composingText.startIndex
                candidatesEnd = inputConnection!!.composingText.endIndex
            } catch (e: IndexOutOfBoundsException) {
                // Ignored
            }
        }
        inputMethodManager!!.updateSelection(this, cursor!!.left, cursor!!.right, candidatesStart, candidatesEnd)
    }

    /**
     * Update request result for monitoring request
     */
    protected fun updateExtractedText() {
        if (extractingTextRequest != null) {
            val text: ExtractedText? = extractText(extractingTextRequest!!)
            inputMethodManager!!.updateExtractedText(this, extractingTextRequest!!.token, text)
        }
    }

    //-------------------------------------------------------------------------------
    //------------------------Internal Callbacks-------------------------------------
    //-------------------------------------------------------------------------------
    /**
     * Set request needed to update when editor updates selection
     */
    internal fun setExtracting(@Nullable request: ExtractedTextRequest?) {
        if (props!!.disallowSuggestions) {
            extractingTextRequest = null
            return
        }
        extractingTextRequest = request
    }

    /**
     * Extract text in editor for input method
     */
    internal fun extractText(@NonNull request: ExtractedTextRequest): ExtractedText? {
        if (props!!.disallowSuggestions || props!!.disableTextExtracting) {
            return null
        }
        val cur: Cursor = cursor
        val text: ExtractedText = ExtractedText()
        val selBegin: Int = cur.left
        val selEnd: Int = cur.right
        var startOffset = 0
        if (request.hintMaxChars === 0) {
            request.hintMaxChars = props!!.maxIPCTextLength
        }
        if (startOffset + request.hintMaxChars < selBegin) {
            startOffset = selBegin - request.hintMaxChars / 2
            startOffset = kotlin.math.min(startOffset, selBegin) // Ensure not negative
        }
        text.text = inputConnection!!.getTextRegion(startOffset, startOffset + request.hintMaxChars, request.flags)
        text.startOffset = startOffset
        text.selectionStart = selBegin - startOffset
        text.selectionEnd = selEnd - startOffset
        if (this.getKeyMetaStates().isSelecting) {
            text.flags = text.flags or ExtractedText.FLAG_SELECTING
        }
        return text
    }

    /**
     * Notify input method that text has been changed for external reason
     */
    fun notifyIMEExternalCursorChange() {
        updateExtractedText()
        updateSelection()
        updateCursorAnchor()
        // Restart if composing
        if (inputConnection!!.composingText.isComposing()) {
            restartInput()
        }
    }

    /**
     * Restart the input connection.
     * Do not call this method randomly. Please refer to documentation first.
     * 
     * @see InputConnection
     */
    fun restartInput() {
        inputConnection?.reset()
        inputMethodManager?.restartInput(this)
    }

    /**
     * Send cursor position in text and on screen to input method
     */
    fun updateCursor() {
        updateCursorAnchor()
        updateExtractedText()
        if (text.getNestedBatchEdit() <= 1 && !inputConnection!!.composingText.isComposing()) {
            updateSelection()
        }
    }

    /**
     * Release some resources held by editor.
     * This will stop completion threads and destroy using [Language] object.
     * Also it prevents future editor tasks (such as posted Runnable) to be executed.
     * 
     * 
     * You are expected to call this method when the editor instance is no longer used, especially when
     * your activity is to be destroyed. Invoking this method repeatedly will not generated errors.
     */
    fun release() {
        hideEditorWindows()
        if (!this.isReleased) {
            dispatchEvent(EditorReleaseEvent(this))
        } else {
            return
        }
        this.isReleased = true
        editorLanguage?.let { lang ->
            lang.analyzeManager.destroy()
            val formatter: Formatter? = lang.formatter
            formatter?.setReceiver(null)
            formatter?.destroy()
            lang.destroy()
            editorLanguage = EmptyLanguage()
        }

        // avoid access to language related after releasing
        textStyles = null
        diagnostics = null
        styleDelegate!!.reset()

        val text: Content? = this.text
        if (text != null) {
            text.removeContentListener(this)
        }
        colorScheme.detachEditor(this)
    }

    /**
     * Hide all built-in windows of the editor
     */
    fun hideEditorWindows() {
        completionWindow?.cancelCompletion()
        completionWindow?.hide()
        textActionWindow?.dismiss()
        touchHandler!!.editorMagnifier.dismiss()
        diagnosticTooltip?.dismiss()
    }

    /**
     * Called by ColorScheme to notify invalidate
     * 
     * @param type Color type changed
     */
    fun onColorUpdated(type: Int) {
        dispatchEvent(ColorSchemeUpdateEvent(this))
        renderContext?.invalidateRenderNodes()
        invalidate()
    }

    /**
     * Called by color scheme to init colors
     */
    fun onColorFullUpdate() {
        dispatchEvent(ColorSchemeUpdateEvent(this))
        renderContext?.invalidateRenderNodes()
        invalidate()
    }

    /**
     * Get using [InputMethodManager]
     */

    /**
     * Called by [EditorInputConnection]
     */
    internal fun onCloseConnection() {
        setExtracting(null)
        invalidate()
    }

    /**
     * This method is called once when the editor is created.
     */
    @NonNull
    protected fun onCreateRenderer(): EditorRenderer {
        return EditorRenderer(this)
    }

    /**
     * Called when the text is edited or [CodeEditor.setSelection] is called
     */
    protected fun onSelectionChanged(cause: Int) {
        var oldLeft: CharPosition? = null
        var oldRight: CharPosition? = null
        val lastTextRange: TextRange? = this.lastSelectedTextRange
        if (lastTextRange != null) {
            oldLeft = lastTextRange.getStart()
            oldRight = lastTextRange.getEnd()
        }
        dispatchEvent(SelectionChangeEvent(this, oldLeft, oldRight, cause))
        this.lastSelectedTextRange = this.cursorRange
    }

    /**
     * Release active edge effects on thumbs up
     */
    @UnsupportedUserUsage
    open fun releaseEdgeEffects() {

        edgeEffectHorizontal!!.onRelease()
        edgeEffectVertical!!.onRelease()
    }

    //-------------------------------------------------------------------------------
    //-------------------------Override methods--------------------------------------
    //-------------------------------------------------------------------------------
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        renderer.draw(canvas)

        // Update magnifier
        if ((lastCursorState != cursorBlink?.visibility ?: false || !touchHandler!!.getScroller()
                .isFinished) && touchHandler!!.editorMagnifier.isShowing()
        ) {
            lastCursorState = cursorBlink?.visibility ?: false
            postInLifecycle(touchHandler!!.editorMagnifier::updateDisplay)
        }
    }

    
    override fun createAccessibilityNodeInfo(): AccessibilityNodeInfo? {
        val info: AccessibilityNodeInfo? =
            super.createAccessibilityNodeInfo()
        if (isEnabled()) {
            info!!.setEditable(isEditable)
            val cur = cursor
        if (cur != null) {
            info.setTextSelection(cur.left, cur.right)
        }
            info.setInputType(android.text.InputType.TYPE_CLASS_TEXT)
            info.setMultiLine(true)
            info.setText(text)
            info.setLongClickable(true)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_COPY)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CUT)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_PASTE)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)
            val scrollRange = this.scrollMaxY
            if (scrollRange > 0) {
                info.setScrollable(true)
                val scrollY = this.offsetY
                if (scrollY > 0) {
                    info.addAction(
                        AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP)
                    }
                }
                if (scrollY < scrollRange) {
                    info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN)
                    }
                }
            }
        }
        return info
    }

    
    override fun onInitializeAccessibilityEvent(event: AccessibilityEvent) {
        super.onInitializeAccessibilityEvent(event)
        val maxScrollY = this.scrollMaxY
        event.setScrollable(maxScrollY > 0)
        event.setMaxScrollX(this.scrollMaxX)
        event.setMaxScrollY(maxScrollY)
    }

    
    override fun performAccessibilityAction(action: Int, arguments: Bundle?): Boolean {
        when (action) {
            AccessibilityNodeInfo.ACTION_COPY -> {
                copyText()
                return true
            }

            AccessibilityNodeInfo.ACTION_CUT -> {
                cutText()
                return true
            }

            AccessibilityNodeInfo.ACTION_PASTE -> {
                pasteText()
                return true
            }

            AccessibilityNodeInfo.ACTION_SET_TEXT -> {
                setText(arguments?.getCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE))
                return true
            }

            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> {
                moveSelection(SelectionMovement.PAGE_DOWN)
                return true
            }

            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> {
                moveSelection(SelectionMovement.PAGE_UP)
                return true
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (action) {
                android.R.id.accessibilityActionScrollDown -> {
                    moveSelection(SelectionMovement.PAGE_UP)
                    return true
                }

                android.R.id.accessibilityActionScrollUp -> {
                    moveSelection(SelectionMovement.PAGE_DOWN)
                    return true
                }
            }
        }
        return super.performAccessibilityAction(action, arguments)
    }

    override fun getAccessibilityClassName(): CharSequence {
        return io.github.abc15018045126.sora.widget.CodeEditor::class.java.getName()
    }

    
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val x = event.getX().toInt()
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                if (this.isInterceptParentHorizontalScrollEnabled) {
                    getParent().requestDisallowInterceptTouchEvent(true)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - downX
                if (this.isInterceptParentHorizontalScrollEnabled && !touchHandler!!.hasAnyHeldHandle()) {
                    if (deltaX > 0 && this.scroller!!.getCurrX() == 0
                        || deltaX < 0 && this.scroller!!.getCurrX() == this.scrollMaxX
                    ) {
                        getParent().requestDisallowInterceptTouchEvent(false)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    
    override fun onCheckIsTextEditor(): Boolean {
        return isEnabled() && isEditable
    }

    
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        if (!isEditable || !isEnabled()) {
            return null
        }
        if (checkSoftInputEnabled()) {
            outAttrs.inputType =
                if (inputType != 0) inputType else EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        } else {
            outAttrs.inputType = android.text.InputType.TYPE_NULL
        }
        outAttrs.initialSelStart = if (cursor != null) cursor.left else 0
        outAttrs.initialSelEnd = if (cursor != null) cursor.right else 0
        outAttrs.initialCapsMode = inputConnection!!.getCursorCapsMode(0)

        // Prevent fullscreen when the screen height is too small
        // Especially in landscape mode
        if (!props!!.allowFullscreen) {
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        }

        dispatchEvent(BuildEditorInfoEvent(this, outAttrs))
        inputConnection!!.reset()
        text.resetBatchEdit()
        setExtracting(null)
        return inputConnection
    }

    
    override fun onResolvePointerIcon(event: MotionEvent, pointerIndex: Int): PointerIcon {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                if (this.isFormatting || layoutBusy) {
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_WAIT)
                }
                if (touchHandler!!.hasAnyHeldHandle()) {
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRABBING)
                }
                if (this.leftHandleDescriptor?.position?.contains(event.getX(), event.getY()) == true
                    || this.rightHandleDescriptor?.position?.contains(event.getX(), event.getY()) == true
                    || this.insertHandleDescriptor?.position?.contains(event.getX(), event.getY()) == true
                ) {
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRAB)
                }
                val res: Long =
                    resolveTouchRegion(event, pointerIndex)
                val region = IntPair.getFirst(res)
                val inbound = IntPair.getSecond(res) == IN_BOUND
                if (region == REGION_TEXT && inbound) {
                    if (touchHandler!!.mouseCanMoveText && !touchHandler!!.mouseClick) {
                        return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_GRABBING)
                    }
                    if (renderer.lastStuckLines != null) {
                        val stickyLineCount: Int? =
                            renderer.lastStuckLines!!.size
                        if (stickyLineCount!! > 0 && event.getY() < getRowBottom(stickyLineCount - 1)) {
                            return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND)
                        }
                    }
                    return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_TEXT)
                } else if (region == REGION_LINE_NUMBER) {
                    when (props!!.actionWhenLineNumberClicked) {
                        DirectAccessProps.LN_ACTION_SELECT_LINE, DirectAccessProps.LN_ACTION_PLACE_SELECTION_HOME -> {
                            return PointerIcon.getSystemIcon(getContext(), PointerIcon.TYPE_HAND)
                        }
                    }
                }
                return super.onResolvePointerIcon(event, pointerIndex)
            }
        }
        return super.onResolvePointerIcon(event, pointerIndex)
    }

    @SuppressLint("ClickableViewAccessibility")
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled()) {
            return false
        }
        if (event.isFromSource(InputDevice.SOURCE_MOUSE) && props!!.mouseMode !== DirectAccessProps.MOUSE_MODE_NEVER) {
            return touchHandler!!.onMouseEvent(event)
        }
        if (this.isFormatting) {
            touchHandler!!.reset2()
            scaleDetector?.onTouchEvent(event)
            return basicDetector?.onTouchEvent(event) ?: false
        }
        val handlingBefore: Boolean = touchHandler!!.handlingMotions()
        val res: Boolean = touchHandler!!.onTouchEvent(event)
        val handling: Boolean = touchHandler!!.handlingMotions()
        var res2 = false
        val res3: Boolean = scaleDetector?.onTouchEvent(event) ?: false
        if (!handling && !handlingBefore) {
            res2 = basicDetector?.onTouchEvent(event) ?: false
        }
        if (event.getAction() === MotionEvent.ACTION_UP) {
            edgeEffectVertical!!.onRelease()
            edgeEffectHorizontal!!.onRelease()
        }
        return (res3 || res2 || res)
    }

    
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventHandler.onKeyDown(keyCode, event)
    }

    
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return keyEventHandler.onKeyUp(keyCode, event)
    }

    
    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        return keyEventHandler.onKeyMultiple(keyCode, repeatCount, event)
    }

    fun onSuperKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyDown(keyCode, event)
    }

    fun onSuperKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    fun onSuperKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        return super.onKeyMultiple(keyCode, repeatCount, event)
    }

    
    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (MeasureSpec.getMode(widthMeasureSpec) !== MeasureSpec.EXACTLY ||
            MeasureSpec.getMode(heightMeasureSpec) !== MeasureSpec.EXACTLY
        ) {
            Log.w(
                LOG_TAG,
                "use wrap_content in editor may cause layout!! lags"
            )
            val specs: Long = ViewMeasureHelper.getDesiredSize(
                widthMeasureSpec, heightMeasureSpec, measureTextRegionOffset(),
                this.rowHeight.toFloat(), isWordwrap, tabWidth, text, renderer.paintGeneral
            )
            widthMeasureSpec = IntPair.getFirst(specs)
            heightMeasureSpec = IntPair.getSecond(specs)
            anyWrapContentSet = true
        } else {
            anyWrapContentSet = false
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    
    override fun onDragEvent(event: DragEvent): Boolean {
        when (event.getAction()) {
            DragEvent.ACTION_DRAG_STARTED -> {
                return true
            }

            DragEvent.ACTION_DRAG_LOCATION -> {
                val pos = getPointPositionOnScreen(event.getX(), event.getY())
                val line: Int = IntPair.getFirst(pos)
                val column: Int = IntPair.getSecond(pos)
               // if ((touchHandler!!.dispatchEditorMotionEvent(::LongPressEvent, text.indexer.getCharPosition(line, column), event) and InterceptTarget.TARGET_EDITOR) != 0) {
            return true
        // }
        if ((!props!!.reselectOnLongPress && cursor.isSelected())) {
             return true
            }

            }
            DragEvent.ACTION_DRAG_EXITED -> {
                touchHandler!!.draggingSelection = null
                postInvalidate()
            }

            DragEvent.ACTION_DROP -> {
                val targetPos: CharPosition? =
                    touchHandler!!.draggingSelection
                if (targetPos == null) {
                    return false
                }
                touchHandler!!.draggingSelection = null
                setSelection(targetPos.line, targetPos.column)
                pasteText(ClipDataUtils.clipDataToString(event.getClipData()))
                requestFocus()
                postInvalidate()
                // Call super for notifying listeners
                super.onDragEvent(event)
                return true
            }
        }
        return super.onDragEvent(event)
    }

    
    protected override fun onCreateContextMenu(menu: ContextMenu?) {
        super.onCreateContextMenu(menu)
        val pos = touchHandler!!.lastContextClickPosition
        if (pos == null) {
            return
        }
        val charPos = getPointPositionOnScreen(pos.x, pos.y)
        dispatchEvent(
            CreateContextMenuEvent(
                this,
                menu!!,
                text.getIndexer().getCharPosition(IntPair.getFirst(charPos).toInt(), IntPair.getSecond(charPos).toInt())
            )
        )
    }

    
    protected override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        touchHandler!!.resetMouse()
        mouseButtonPressed = false
        mouseHover = mouseButtonPressed
    }

    
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            if (event.getAction() === MotionEvent.ACTION_HOVER_ENTER) {
                mouseHover = true
            } else if (event.getAction() === MotionEvent.ACTION_HOVER_EXIT) {
                mouseHover = false
            }
            if (event.getActionMasked() === MotionEvent.ACTION_BUTTON_PRESS
                || event.getActionMasked() === MotionEvent.ACTION_BUTTON_RELEASE
            ) {
                mouseButtonPressed = event.getButtonState() !== 0
            }
            when (event.getAction()) {
                MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_HOVER_EXIT -> {
                    touchHandler!!.dispatchEditorMotionEvent({ ed, pos, ev, sp, spR, reg, bnd ->
                        HoverEvent(ed, pos, ev, sp, spR, reg, bnd)
                    }, null, event)
                    return true
                }
            }
        }
        if (event.getAction() === MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_CLASS_POINTER) && !keyEventHandler.getKeyMetaStates().isCtrlPressed
        ) {
            val v_scroll: Float = -event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            val h_scroll: Float = -event.getAxisValue(MotionEvent.AXIS_HSCROLL)
            var distanceX: Float = h_scroll * verticalScrollFactor * props!!.mouseWheelScrollFactor
            var distanceY: Float = v_scroll * verticalScrollFactor * props!!.mouseWheelScrollFactor
            if (keyEventHandler.getKeyMetaStates().isAltPressed) {
                val multiplier: Float = props!!.fastScrollSensitivity
                distanceX *= multiplier
                distanceY *= multiplier
            }
            if (keyEventHandler.getKeyMetaStates().isShiftPressed) {
                val tmp = distanceX
                distanceX = distanceY
                distanceY = tmp
            }
            touchHandler!!.onScroll(event, event, distanceX, distanceY)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    
    protected override fun onSizeChanged(w: Int, h: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(w, h, oldWidth, oldHeight)
        renderer.onSizeChanged(w, h)
        this.verticalEdgeEffect.setSize(w, h)
        this.horizontalEdgeEffect.setSize(h, w)
        this.verticalEdgeEffect.finish()
        this.horizontalEdgeEffect.finish()
        if (layout!! == null || (isWordwrap && w != oldWidth)) {
            createLayout()
        } else {
            touchHandler!!.scrollBy(
                if (this.offsetX > this.scrollMaxX) (this.scrollMaxX - this.offsetX).toFloat() else 0f,
                if (this.offsetY > this.scrollMaxY) (this.scrollMaxY - this.offsetY).toFloat() else 0f
            )
        }
        verticalAbsorb = false
        horizontalAbsorb = false
        if (oldHeight > h && props!!.adjustToSelectionOnResize) {
            ensureSelectionVisible()
        }
    }

    
    protected override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dispatchEvent(EditorAttachStateChangeEvent(this, false))
        cursorBlink?.let { removeCallbacks(it) }
    }

    
    protected override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        dispatchEvent(EditorAttachStateChangeEvent(this, true))
    }

    
    protected override fun onFocusChanged(
        gainFocus: Boolean, direction: Int,
        @Nullable previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (gainFocus) {
            if (cursorBlink?.valid == true && (cursorBlink?.period ?: 0) > 0) {
                postInLifecycle(cursorBlink!!)
            }

        } else {
            cursorBlink?.valid = false
            cursorBlink?.visibility = false
            touchHandler!!.hideInsertHandle()
            cursorBlink?.let { removeCallbacks(it) }
        }
        dispatchEvent(EditorFocusChangeEvent(this, gainFocus))
        invalidate()
    }

    override fun computeScroll() {

        val scroller: EditorScroller =
            touchHandler!!.getScroller()
        if (scroller.computeScrollOffset()) {
            if (!scroller.isFinished && (scroller.getStartX() != scroller.getFinalX() || scroller.getStartY() != scroller.getFinalY())) {
                scrollerFinalX = scroller.getFinalX().toFloat()
                scrollerFinalY = scroller.getFinalY().toFloat()
                horizontalAbsorb = abs(scroller.getStartX() - scroller.getFinalX()) > this.dpUnit * 5
                verticalAbsorb = abs(scroller.getStartY() - scroller.getFinalY()) > this.dpUnit * 5
            }

            if (scroller!!.getCurrX() <= 0 && scrollerFinalX <= 0 && edgeEffectHorizontal!!.isFinished() && horizontalAbsorb) {
                edgeEffectHorizontal!!.onAbsorb(scroller!!.getCurrVelocity().toInt())
                touchHandler!!.glowLeftOrRight = false
            } else {
                val max = this.scrollMaxX
                if (scroller!!.getCurrX() >= max && scrollerFinalX >= max && edgeEffectHorizontal!!.isFinished() && horizontalAbsorb) {
                    edgeEffectHorizontal!!.onAbsorb(scroller!!.getCurrVelocity().toInt())
                    touchHandler!!.glowLeftOrRight = true
                }
            }
            if (scroller!!.getCurrY() <= 0 && scrollerFinalY <= 0 && edgeEffectVertical!!.isFinished() && verticalAbsorb) {
                edgeEffectVertical!!.onAbsorb(scroller!!.getCurrVelocity().toInt())
                touchHandler!!.glowTopOrBottom = false
            } else {
                val max = this.scrollMaxY
                if (scroller!!.getCurrY() >= max && scrollerFinalY >= max && edgeEffectVertical!!.isFinished() && verticalAbsorb) {
                    edgeEffectVertical!!.onAbsorb(scroller!!.getCurrVelocity().toInt())
                    touchHandler!!.glowTopOrBottom = true
                }
            }
            postInvalidateOnAnimation()
        }
    }

    override fun computeVerticalScrollRange(): Int {
        return this.scrollMaxY
    }


    override fun computeVerticalScrollOffset(): Int {
        return max(0, min(this.scrollMaxY, this.offsetY))
    }


    override fun computeHorizontalScrollRange(): Int {
        return this.scrollMaxX
    }


    override fun computeHorizontalScrollOffset(): Int {
        return max(0, min(this.scrollMaxX, this.offsetX))
    }


    override fun computeHorizontalScrollExtent(): Int {
        return 0
    }


    override fun computeVerticalScrollExtent(): Int {
        return 0
    }


    override fun removeCallbacks(action: Runnable?): Boolean {
        action?.let { EditorHandler.removeCallbacks(it) }
        return super.removeCallbacks(action)
    }


    /**
     * Post the given action to message queue. Run the action if editor is not released.
     * 
     * @param action The Runnable to be executed.
     * @return Returns true if the Runnable was successfully placed in to the message queue.
     * Returns false on failure, usually because the looper processing the message queue is exiting.
     * @see View.post
     */
    public fun postInLifecycle(action: Runnable): Boolean {

        return EditorHandler.post({
            if (this.isReleased) {
                return@post
            }
            action.run()
        })
    }

    /**
     * Post the given action to message queue. Run the action if editor is not released.
     * 
     * @param action      The Runnable to be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable will be executed.
     * @return Returns true if the Runnable was successfully placed in to the message queue.
     * Returns false on failure, usually because the looper processing the message queue is exiting.
     * @see View.postDelayed
     */
    public fun postDelayedInLifecycle(action: Runnable, delayMillis: Long): Boolean {

        return EditorHandler.postDelayed({
            if (this.isReleased) {
                return@postDelayed
            }
            action.run()
        }, delayMillis)
    }

    
    override fun beforeReplace(@NonNull content: Content) {
        waitForNextChange = true
        layout!!.beforeReplace(content)
    }

    
    override fun afterInsert(
        @NonNull content: Content, startLine: Int, startColumn: Int, endLine: Int,
        endColumn: Int, @NonNull insertedContent: CharSequence
    ) {
        renderContext?.updateForInsertion(startLine, endLine)
        renderer.updateTimestamp()
        styleDelegate!!.onTextChange()
        val start = text.indexer.getCharPosition(startLine, startColumn)
        val end = text.indexer.getCharPosition(endLine, endColumn)

        // Update spans
        val textStyles = this.textStyles
        val diagnostics = this.diagnostics
        val inlayHints = this.inlayHints
        val highlightTextContainer = this.highlightTextContainer
        try {
            textStyles?.adjustOnInsert(start, end)
            diagnostics?.shiftOnInsert(start.index, end.index)
            inlayHints?.updateOnInsertion(startLine, startColumn, endLine, endColumn)
            highlightTextContainer?.updateOnInsertion(startLine, startColumn, endLine, endColumn)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Update failure", e)
        }

        layout!!.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent)
        renderer.buildMeasureCacheForLines(startLine, endLine)
        checkForRelayout()

        editorLanguage!!.analyzeManager.insert(start, end, insertedContent)
        touchHandler!!.hideInsertHandle()
        if (isEditable && cursor != null && !cursor!!.isSelected() && !inputConnection!!.composingText.isComposing() && acceptsComposingText()) {
            _cursorAnimator!!.markEndPos()
            _cursorAnimator!!.start()
        }
        selectionAnchor = if (lastAnchorIsSelLeft) cursor?.left() else cursor?.right()
        dispatchEvent(
            ContentChangeEvent(
                this,
                ContentChangeEvent.ACTION_INSERT,
                start,
                end,
                insertedContent,
                text.isUndoManagerWorking()
            )
        )
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION)
        lastInsertion = TextRange(start.fromThis(), end.fromThis())
        waitForNextChange = false
        ensureSelectionVisible()

        // Notify input method
        updateCursor()
    }

    
    override fun afterDelete(
        @NonNull content: Content, startLine: Int, startColumn: Int, endLine: Int,
        endColumn: Int, @NonNull deletedContent: CharSequence
    ) {
        renderContext?.updateForDeletion(startLine, endLine)
        renderer.updateTimestamp()
        styleDelegate!!.onTextChange()
        val start =
            text.getIndexer().getCharPosition(startLine, startColumn)
        val end = start.fromThis()
        end.column = endColumn
        end.line = endLine
        end.index = start.index + deletedContent.length

        val textStyles = this.textStyles
        val diagnostics = this.diagnostics
        val inlayHints = this.inlayHints
        val highlightTextContainer = this.highlightTextContainer
        try {
            textStyles?.adjustOnDelete(start, end)
            diagnostics?.shiftOnDelete(start.index, end.index)
            inlayHints?.updateOnDeletion(startLine, startColumn, endLine, endColumn)
            highlightTextContainer?.updateOnDeletion(startLine, startColumn, endLine, endColumn)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Update failure", e)
        }

        layout!!.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent)
        renderer!!.buildMeasureCacheForLines(startLine, startLine + 1)
        checkForRelayout()

        if (isEditable && !cursor!!.isSelected() && !waitForNextChange && !inputConnection!!.composingText.isComposing() && acceptsComposingText()) {
            _cursorAnimator!!.markEndPos()
            _cursorAnimator!!.start()
        }
        editorLanguage!!.analyzeManager.delete(start, end, deletedContent)
        selectionAnchor = if (lastAnchorIsSelLeft) cursor!!.left() else cursor!!.right()
        dispatchEvent(
            ContentChangeEvent(
                this,
                ContentChangeEvent.ACTION_DELETE,
                start,
                end,
                deletedContent,
                text.isUndoManagerWorking()
            )
        )
        onSelectionChanged(SelectionChangeEvent.CAUSE_TEXT_MODIFICATION)

        if (!waitForNextChange) {
            updateCursor()
            ensureSelectionVisible()
            touchHandler!!.hideInsertHandle()
        }
    }

    
    override fun beforeModification(@NonNull content: Content) {
        if (props!!.checkModificationThread && isAttachedToWindow) {
            val handler = getHandler()
            if (handler != null) {
                if (handler.looper.thread !== Thread.currentThread()) {
                    throw RuntimeException("text is changed in wrong thread")
                }
            }
        }
        _cursorAnimator!!.markStartPos()
        lastAnchorIsSelLeft = cursor!!.left() == selectionAnchor
    }

    
    override fun onFormatSucceed(@NonNull applyContent: CharSequence, @Nullable cursorRange: TextRange?) {
        postInLifecycle(Runnable {
            val line: Int = cursor!!.leftLine
            val column: Int = cursor!!.leftColumn
            val x = this.offsetX
            val y = this.offsetY
            val string = if (applyContent is Content) (applyContent as Content) else applyContent
            text.beginBatchEdit()
            text.delete(
                0, 0, text.lineCount - 1,
                text.getColumnCount(text.lineCount - 1)
            )
            text.insert(0, 0, string)
            text.endBatchEdit()
            inputConnection!!.markInvalid()
            if (cursorRange == null) {
                setSelectionAround(line, column)
            } else {
                try {
                    val start = cursorRange.start
                    val end = cursorRange.end
                    setSelectionRegion(start.line, start.column, end.line, end.column)
                } catch (e: IndexOutOfBoundsException) {
                    Log.w(LOG_TAG, e)
                }
            }
            this.scroller!!.forceFinished(true)
            this.scroller!!.startScroll(x, y, 0, 0, 0)
            this.scroller!!.abortAnimation()
            // Ensure the scroll offset is valid
            touchHandler!!.scrollBy(0f, 0f)
            inputConnection!!.reset()
            restartInput()
            dispatchEvent(EditorFormatEvent(this, true))
        })
    }

    
    override fun onFormatFail(throwable: Throwable?) {
        postInLifecycle(Runnable {
            Toast.makeText(getContext(), "Format:" + throwable, Toast.LENGTH_SHORT).show()
            dispatchEvent(EditorFormatEvent(this, false))
        })
    }

    companion object {
        /**
         * The default text size when creating the editor object. Unit is sp.
         */
        const val DEFAULT_TEXT_SIZE: Int = 18

        /**
         * The default line info text size when creating the editor object. Unit is sp.
         */
        const val DEFAULT_LINE_INFO_TEXT_SIZE: Int = 21

        /**
         * The default cursor blinking period
         */
        const val DEFAULT_CURSOR_BLINK_PERIOD: Int = 500

        /**
         * Draw whitespace characters before line content start
         * **Whitespace here only means space and tab**
         * 
         * @see .setNonPrintablePaintingFlags
         */
        const val FLAG_DRAW_WHITESPACE_LEADING: Int = 1
        const val FLAG_DRAW_WHITESPACE_INNER: Int = 1 shl 1
        const val FLAG_DRAW_WHITESPACE_TRAILING: Int = 1 shl 2
        const val FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE: Int = 1 shl 3
        const val FLAG_DRAW_LINE_SEPARATOR: Int = 1 shl 4
        const val FLAG_DRAW_TAB_SAME_AS_SPACE: Int = 1 shl 5
        const val FLAG_DRAW_WHITESPACE_IN_SELECTION: Int = 1 shl 6
        const val FLAG_DRAW_SOFT_WRAP: Int = 1 shl 7

        const val ACTION_MODE_NONE: Int = 0
        const val ACTION_MODE_SEARCH_TEXT: Int = 1
        const val ACTION_MODE_SELECT_TEXT: Int = 2
        
        @JvmField
        val logger: Logger? = Logger.instance("CodeEditor")

        /**
         * Digits for line number measuring
         */
        const val NUMBER_DIGITS = "0 1 2 3 4 5 6 7 8 9"
        const val LOG_TAG = "CodeEditor"
        const val COPYRIGHT =
            "sora-editor\nCopyright (C) abc15018045126 roses2020@qq.com\nThis project is distributed under the LGPL v2.1 license"

        /**
         * Checks whether this region has visible region on screen
         * 
         * @param begin The start line of code block
         * @param end   The end line of code block
         * @param first The first visible line on screen
         * @param last  The last visible line on screen
         * @return Whether this block can be seen
         */
        fun hasVisibleRegion(begin: Int, end: Int, first: Int, last: Int): Boolean {
            return (end > first && begin < last)
        }
    }

}


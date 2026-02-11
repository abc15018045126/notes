package io.github.abc15018045126.sora.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Path
import android.graphics.PorterDuff
import java.util.Collections
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.RenderNode
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.util.SparseArray
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.annotations.UnsupportedUserUsage
import io.github.abc15018045126.sora.graphics.BufferedDrawPoints
import io.github.abc15018045126.sora.graphics.GraphicsCompat
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.graphics.TextRow
import io.github.abc15018045126.sora.graphics.TextRowParams
import io.github.abc15018045126.sora.lang.completion.snippet.SnippetItem
import io.github.abc15018045126.sora.lang.diagnostic.DiagnosticRegion
import io.github.abc15018045126.sora.lang.styling.CodeBlock
import io.github.abc15018045126.sora.lang.styling.EmptyReader
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.Spans
import io.github.abc15018045126.sora.lang.styling.Styles
import io.github.abc15018045126.sora.lang.styling.TextStyle
import io.github.abc15018045126.sora.lang.styling.color.ResolvableColor
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.lang.styling.line.LineAnchorStyle
import io.github.abc15018045126.sora.lang.styling.line.LineBackground
import io.github.abc15018045126.sora.lang.styling.line.LineGutterBackground
import io.github.abc15018045126.sora.lang.styling.line.LineSideIcon
import io.github.abc15018045126.sora.lang.styling.line.LineStyles
import io.github.abc15018045126.sora.text.CharPosition
import io.github.abc15018045126.sora.text.Content
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.text.Cursor
import io.github.abc15018045126.sora.text.bidi.Directions
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.LongArrayList
import io.github.abc15018045126.sora.util.MutableInt
import io.github.abc15018045126.sora.util.Numbers
import io.github.abc15018045126.sora.util.Numbers.stringSize
import io.github.abc15018045126.sora.util.MutableIntList
import io.github.abc15018045126.sora.util.MutableLongLongMap
import io.github.abc15018045126.sora.util.TemporaryCharBuffer
import io.github.abc15018045126.sora.widget.layout.Row
import io.github.abc15018045126.sora.widget.layout.RowIterator
import io.github.abc15018045126.sora.widget.rendering.RenderingConstants
import io.github.abc15018045126.sora.widget.rendering.TextAdvancesCache
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import io.github.abc15018045126.sora.widget.style.DiagnosticIndicatorStyle
import io.github.abc15018045126.sora.graphics.BubbleHelper
import java.util.Objects
import io.github.abc15018045126.sora.widget.style.LineInfoPanelPosition
import io.github.abc15018045126.sora.widget.style.LineInfoPanelPositionMode
import io.github.abc15018045126.sora.widget.style.SelectionHandleStyle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ceil
import android.graphics.Paint as AndroidPaint

class EditorRenderer(@NonNull editor: CodeEditor) {
    internal val bufferedDrawPoints: BufferedDrawPoints
    @JvmField
    internal val paintGeneral: Paint
    @JvmField
    internal val paintOther: Paint
    @JvmField
    internal val viewRect: Rect
    private val tmpRect: RectF
    private val tmpPath: Path
    @JvmField
    internal val paintGraph: Paint
    @JvmField
    internal val verticalScrollBarRect: RectF
    @JvmField
    internal val horizontalScrollBarRect: RectF
    private val postDrawLineNumbers: LongArrayList = LongArrayList()
    private val postDrawCurrentLines: MutableIntList = MutableIntList()
    private val matchedPositions: LongArrayList = LongArrayList()
    private val highlightPositions: MutableLongLongMap = MutableLongLongMap()
    private val preloadedLines: SparseArray<ContentLine> = SparseArray()
    private val preloadedDirections: SparseArray<Directions> = SparseArray()
    private val editor: CodeEditor
    private val collectedDiagnostics: MutableList<DiagnosticRegion> = ArrayList()
    
    @JvmField
    var lastStuckLines: List<CodeBlock?>? = null
    
    @JvmField
    var metricsText: AndroidPaint.FontMetricsInt? = null

    private val sharedTextRow = TextRow()

    @Nullable
    private var horizontalScrollbarThumbDrawable: Drawable? = null

    @Nullable
    private var horizontalScrollbarTrackDrawable: Drawable? = null

    @Nullable
    private var verticalScrollbarThumbDrawable: Drawable? = null

    @Nullable
    private var verticalScrollbarTrackDrawable: Drawable? = null
    private val lineBreakGraph: Drawable?
    private val softwrapLeftGraph: Drawable?
    private val softwrapRightGraph: Drawable?

    @Volatile
    var timestamp: Long = 0
        private set
    private var metricsLineNumber: AndroidPaint.FontMetricsInt
    private var metricsGraph: AndroidPaint.FontMetricsInt? = null
    private var cachedGutterWidth = 0
    private var cursor: Cursor? = null
    protected var lineBuf: ContentLine? = null
    protected var content: Content? = null

    @Volatile
    private var renderingFlag = false
    internal var forcedRecreateLayout: Boolean = false

    /**
     * Called when the editor text is changed by [CodeEditor.setText]
     */
    fun onEditorFullTextUpdate() {
        cursor = editor.cursor
        content = editor.text
    }

    fun draw(@NonNull canvas: Canvas) {
        val saveCount: Int = canvas.save()
        canvas.translate(editor.offsetX.toFloat(), editor.offsetY.toFloat())
        renderingFlag = true
        try {
            drawView(canvas)
        } finally {
            renderingFlag = false
        }
        canvas.restoreToCount(saveCount)
    }

    fun onSizeChanged(width: Int, height: Int) {
        viewRect.right = width
        viewRect.bottom = height
    }

    val paint: Paint
        get() = paintGeneral

    fun getPaintOther(): Paint {
        return paintOther
    }

    fun getPaintGraph(): Paint {
        return paintGraph
    }

    fun setCachedLineNumberWidth(width: Int) {
        cachedGutterWidth = width
    }

    fun getVerticalScrollBarRect(): RectF {
        return verticalScrollBarRect
    }

    fun getHorizontalScrollBarRect(): RectF {
        return horizontalScrollBarRect
    }

    fun setHorizontalScrollbarThumbDrawable(@Nullable drawable: Drawable?) {
        horizontalScrollbarThumbDrawable = drawable
    }

    @Nullable
    fun getHorizontalScrollbarThumbDrawable(): Drawable? {
        return horizontalScrollbarThumbDrawable
    }

    fun setHorizontalScrollbarTrackDrawable(@Nullable drawable: Drawable?) {
        horizontalScrollbarTrackDrawable = drawable
    }

    @Nullable
    fun getHorizontalScrollbarTrackDrawable(): Drawable? {
        return horizontalScrollbarTrackDrawable
    }

    fun setVerticalScrollbarThumbDrawable(@Nullable drawable: Drawable?) {
        this.verticalScrollbarThumbDrawable = drawable
    }

    @Nullable
    fun getVerticalScrollbarThumbDrawable(): Drawable? {
        return verticalScrollbarThumbDrawable
    }

    fun setVerticalScrollbarTrackDrawable(@Nullable drawable: Drawable?) {
        verticalScrollbarTrackDrawable = drawable
    }

    @Nullable
    fun getVerticalScrollbarTrackDrawable(): Drawable? {
        return verticalScrollbarTrackDrawable
    }

    fun setTextSizePxDirect(size: Float) {
        paintGeneral.setTextSizeWrapped(size)
        paintOther.setTextSize(size)
        paintGraph.setTextSize(size * editor.props!!.functionCharacterSizeFactor)
        metricsText = paintGeneral.getFontMetricsInt()
        metricsLineNumber = paintOther.getFontMetricsInt()
        metricsGraph = paintGraph.getFontMetricsInt()
        editor.renderContext!!.invalidateRenderNodes()
        updateTimestamp()
    }

    /**
     * Set text's typeface
     * 
     * @param typefaceText New typeface
     */
    fun setTypefaceText(typefaceText: Typeface?) {
        var typefaceText: Typeface? = typefaceText
        if (typefaceText == null) {
            typefaceText = Typeface.DEFAULT
        }
        paintGeneral.setTypefaceWrapped(typefaceText)
        metricsText = paintGeneral.getFontMetricsInt()
        editor.renderContext!!.invalidateRenderNodes()
        updateTimestamp()
        editor.createLayout()
        editor.invalidate()
    }

    fun setTypefaceLineNumber(typefaceLineNumber: Typeface?) {
        var typefaceLineNumber: Typeface? = typefaceLineNumber
        if (typefaceLineNumber == null) {
            typefaceLineNumber = Typeface.MONOSPACE
        }
        paintOther.setTypeface(typefaceLineNumber)
        metricsLineNumber = paintOther.getFontMetricsInt()
        editor.invalidate()
    }

    fun setTextScaleX(textScaleX: Float) {
        paintGeneral.setTextScaleX(textScaleX)
        paintOther.setTextScaleX(textScaleX)
        onTextStyleUpdate()
    }

    fun setLetterSpacing(letterSpacing: Float) {
        paintGeneral.setLetterSpacing(letterSpacing)
        paintOther.setLetterSpacing(letterSpacing)
        onTextStyleUpdate()
    }

    internal fun onTextStyleUpdate() {
        paintGeneral.isRenderFunctionCharacters = editor.isRenderFunctionCharacters
        metricsGraph = paintGraph.fontMetricsInt
        metricsLineNumber = paintOther.getFontMetricsInt()
        metricsText = paintGeneral.getFontMetricsInt()
        editor.renderContext!!.invalidateRenderNodes()
        updateTimestamp()
        editor.createLayout()
        editor.invalidate()
    }

    /**
     * Update timestamp required for measuring cache
     */
    fun updateTimestamp() {
        this.timestamp = SystemClock.elapsedRealtimeNanos()
    }

    protected fun prepareLine(line: Int) {
        lineBuf = getLine(line)
    }

    protected fun getLine(line: Int): ContentLine {
        if (!renderingFlag) {
            return getLineDirect(line)
        }
        var line2 =
            preloadedLines.get(line)
        if (line2 == null) {
            line2 = content!!.getLine(line)
            preloadedLines.put(line, line2)
        }
        return line2!!
    }

    protected fun getLineDirections(line: Int): Directions {
        if (!renderingFlag) {
            return content!!.getLineDirections(line)
        }
        var line2 =
            preloadedDirections.get(line)
        if (line2 == null) {
            line2 = content!!.getLineDirections(line)
            preloadedDirections.put(line, line2)
        }
        return line2!!
    }

    fun getLineDirect(line: Int): ContentLine {
        return content!!.getLine(line)
    }

    fun getColumnCount(line: Int): Int {
        return getLine(line).length
    }

    // draw methods
    @RequiresApi(29)
    fun updateLineDisplayList(renderNode: RenderNode, line: Int, spans: Spans.Reader?) {
        val widthLine = drawSingleTextLine(null, line, 0f, 0f, spans, false)
        renderNode.setPosition(0, 0, (widthLine + 0.5f).toInt(), editor.logicalRowHeight)
        val canvas =
            renderNode.beginRecording()
        try {
            drawSingleTextLine(canvas, line, 0f, 0f, spans, false)
        } finally {
            renderNode.endRecording()
        }
    }

    @UnsupportedUserUsage
    fun createTextRow(rowIndex: Int): TextRow {
        val tr = TextRow()
        updateTextRow(tr, rowIndex)
        return tr
    }

    private fun updateTextRow(tr: TextRow, rowIndex: Int) {
        val styles = editor.styles
        val spanMap =
            if (styles != null) styles.spans else null
        var spanReader =
            if (spanMap != null) spanMap.read() else null
        spanReader = if (spanReader == null) EmptyReader.INSTANCE else spanReader
        val row =
            editor.layout!!.getRowAt(rowIndex)
        val line =
            content!!.getLine(row.lineIndex)
        val cache =
            editor.renderContext!!.cache.queryMeasureCache(row.lineIndex)
        var widths =
            if (cache != null && cache.updateTimestamp >= this.timestamp) cache.widths else null
        widths = if (widths != null && widths.size > line.length) widths else null
        tr.set(
            line,
            row.startColumn,
            row.endColumn,
            spanReader.getSpansOnLine(row.lineIndex),
            row.inlayHints,
            content!!.getLineDirections(row.lineIndex),
            paintGeneral,
            widths,
            createTextRowParams()
        )
        applySelectedTextRange(tr, row.lineIndex)
    }

    private fun applySelectedTextRange(tr: TextRow, lineIndex: Int) {
        val cur = cursor ?: return
        if (cur.isSelected() && lineIndex >= cur.leftLine && lineIndex <= cur.rightLine) {
            var startColInLine = if (lineIndex == cur.leftLine) cur.leftColumn else 0
            var endColInLine: Int =
                if (lineIndex == cur.rightLine) cur.rightColumn else (lineBuf?.length ?: 0)
            startColInLine = Math.max(tr.textStart, startColInLine)
            endColInLine = Math.min(tr.textEnd, endColInLine)
            if (startColInLine < endColInLine) {
                tr.setSelectedRange(startColInLine, endColInLine)
            }
        }
    }

    protected fun drawSingleTextLine(
        canvas: Canvas?,
        line: Int,
        offsetX: Float,
        offsetY: Float,
        spans: Spans.Reader?,
        visibleOnly: Boolean
    ): Float {
        var reader: Spans.Reader? = spans
        prepareLine(line)
        val columnCount = getColumnCount(line)
        if (reader == null || reader.getSpanCount() <= 0) {
            reader = EmptyReader.INSTANCE
        }
        val tr: TextRow = TextRow()
        val inlayHints =
            editor.inlayHints
        val lineInlays: List<InlayHint>? =
            if (inlayHints == null) Collections.emptyList() else inlayHints.getForLine(line) as List<InlayHint>?
        val cache =
            editor.renderContext!!.cache.queryMeasureCache(line)
        var widths =
            if (cache != null && cache.updateTimestamp >= this.timestamp) cache.widths else null
        widths = if (widths != null && widths.size > (lineBuf?.length ?: 0)) widths else null
        tr.set(
            lineBuf!!,
            0,
            columnCount,
            reader.getSpansOnLine(line),
            lineInlays,
            getLineDirections(line),
            paintGeneral,
            widths,
            createTextRowParams()!!
        )
        applySelectedTextRange(tr, line)
        if (canvas != null) {
            canvas.save()
            canvas.translate(offsetX, editor.getRowTop(0) + offsetY)
            if (visibleOnly) {
                val visibleStart: Float = Math.max(0f, -offsetX)
                val visibleEnd: Float = Math.max(visibleStart, -offsetX + editor.width)
                tr.draw(canvas, visibleStart, visibleEnd)
            } else {
                tr.draw(canvas, 0f, Float.MAX_VALUE)
            }
            canvas.restore()
        }
        return if (canvas == null) tr.computeRowWidth() else 0f
    }

    fun hasSideHintIcons(): Boolean {
        val styles: Styles? = editor.styles
        if (styles != null) {
            val styleTypeCount = styles.styleTypeCount
            if (styleTypeCount != null) {
                val count =
                    styleTypeCount.get(LineSideIcon::class.java)
                if (count == null) {
                    return false
                }
                return count.value > 0
            }
        }
        return false
    }

    /**
     * Paint the view on given Canvas
     * 
     * @param canvas Canvas you want to draw
     */
    fun drawView(canvas: Canvas) {
        cursor?.updateCache(editor.firstVisibleLine)

        val color: EditorColorScheme = editor.colorScheme
        drawColor(canvas, color.getColor(EditorColorScheme.WHOLE_BACKGROUND), viewRect)

        val lineNumberWidth: Float = editor.measureLineNumber() // include line number margin
        val sideIconWidth = if (hasSideHintIcons()) editor.logicalRowHeight.toFloat() else 0f
        var offsetX: Float = -editor.offsetX.toFloat() + editor.measureTextRegionOffset()
        val textOffset = offsetX

        val gutterWidth =
            (lineNumberWidth + sideIconWidth + editor.dividerWidth + editor.dividerMarginLeft + editor.dividerMarginRight).toInt()
        if (editor.isWordwrap) {
            if (cachedGutterWidth == 0) {
                cachedGutterWidth = gutterWidth
            } else if (cachedGutterWidth != gutterWidth && !editor.touchHandler!!.isScaling) {
                cachedGutterWidth = gutterWidth
                editor.postInLifecycle(editor::requestLayoutIfNeeded)
                editor.createLayout(false)
            } else if (forcedRecreateLayout) {
                editor.createLayout()
                editor.postInLifecycle(editor::requestLayoutIfNeeded)
            }
        } else {
            cachedGutterWidth = 0
            if (forcedRecreateLayout) {
                editor.createLayout()
            }
        }
        forcedRecreateLayout = false

        prepareLines(editor.firstVisibleLine, editor.lastVisibleLine)
        buildMeasureCacheForLines(editor.firstVisibleLine, editor.lastVisibleLine, this.timestamp, true)
        val stuckLines: List<CodeBlock?>? = this.stuckCodeBlocks
        val cursor = editor.cursor ?: return

        if (cursor.isSelected()) {
            editor.handleDescInsert!!.setEmpty()
        } else {
            editor.handleDescLeft!!.setEmpty()
            editor.handleDescRight!!.setEmpty()
        }

        val lineNumberNotPinned = editor.isLineNumberEnabled && !editor.isLineNumberPinned

        val postDrawLineNumbers: LongArrayList = this.postDrawLineNumbers
        postDrawLineNumbers.clear()
        val postDrawCurrentLines: MutableIntList = this.postDrawCurrentLines
        postDrawCurrentLines.clear()
        val postDrawCursor: MutableList<DrawCursorTask?> = ArrayList(3)
        val firstLn: MutableInt? =
            if (editor.isFirstLineNumberAlwaysVisible && editor.isWordwrap && !editor.isLineNumberPinned) MutableInt(-1) else null

        canvas.save()
        val stuckLineBottom = getStuckLineBottom(stuckLines)
        canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
        drawRows(canvas, textOffset, postDrawLineNumbers, postDrawCursor, postDrawCurrentLines, firstLn)
        patchHighlightedDelimiters(canvas, textOffset)
        drawDiagnosticIndicators(canvas, offsetX)
        canvas.restore()

        offsetX = -editor.offsetX.toFloat()

        val currentLineNumber = if (cursor.isSelected()) -1 else cursor.leftLine

        if (lineNumberNotPinned) {
            drawLineNumberBackground(
                canvas,
                offsetX,
                lineNumberWidth + sideIconWidth + editor.dividerMarginLeft,
                color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND)
            )
            val lineNumberColor: Int = editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER)
            val currentLineBgColor: Int = editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE)
            if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine && editor.isEditable) {
                tmpRect.bottom = (editor.cursorAnimator.animatedLineBottom() - editor.offsetY).toFloat()
                tmpRect.top = tmpRect.bottom - editor.cursorAnimator.animatedLineHeight()
                tmpRect.left = 0f
                tmpRect.right = (textOffset - editor.dividerMarginRight).toFloat()
                drawColor(canvas, currentLineBgColor, tmpRect)
            }

            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            for (i in 0 until postDrawCurrentLines.size) {
                drawRowBackground(
                    canvas,
                    currentLineBgColor,
                    postDrawCurrentLines.get(i),
                    (textOffset - editor.dividerMarginRight).toInt()
                )
            }
            // User defined gutter background
            drawUserGutterBackground(canvas, (textOffset - editor.dividerMarginRight).toInt())
            drawSideIcons(canvas, offsetX + lineNumberWidth)
            canvas.restore()

            val isRightOfDivider = editor.isLineNumberRightOfDivider()
            
            // Draw Divider
            val dividerX = if (isRightOfDivider) 
                offsetX 
            else 
                offsetX + lineNumberWidth + sideIconWidth + editor.dividerMarginLeft
            
            drawDivider(
                canvas,
                dividerX,
                color.getColor(EditorColorScheme.LINE_DIVIDER)
            )

            // Draw Line Numbers
            val numberX = if (isRightOfDivider) 
                offsetX + editor.dividerWidth + editor.dividerMarginRight + sideIconWidth
            else 
                offsetX

            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            if (firstLn != null && firstLn.value != -1) {
                val bottom: Int = editor.getRowBottom(0)
                val y: Float
                if (postDrawLineNumbers.size == 0 || editor.getRowTop(IntPair.getSecond(postDrawLineNumbers.get(0))) - editor.offsetY > bottom) {
                    // Free to draw at first line
                    y =
                        (editor.getRowBottom(0) + editor.getRowTop(0)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent
                } else {
                    val row: Int = IntPair.getSecond(postDrawLineNumbers.get(0))
                    y =
                        (editor.getRowBottom(row - 1) + editor.getRowTop(row - 1)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent - editor.offsetY.toFloat()
                }
                paintOther.textAlign = editor.getLineNumberAlign()
                paintOther.color = if (firstLn.value == currentLineNumber) color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lineNumberColor
                val text =
                    (firstLn.value + 1).toString()
                when (editor.getLineNumberAlign()) {
                    AndroidPaint.Align.LEFT -> canvas.drawText(text, numberX, y, paintOther)
                    AndroidPaint.Align.RIGHT -> canvas.drawText(text, numberX + lineNumberWidth, y, paintOther)
                    AndroidPaint.Align.CENTER -> canvas.drawText(
                        text,
                        numberX + (lineNumberWidth + editor.dividerMarginLeft) / 2f,
                        y,
                        paintOther
                    )
                    else -> {}
                }
            }
            for (i in 0 until postDrawLineNumbers.size) {
                val packed: Long = postDrawLineNumbers.get(i)
                drawLineNumber(
                    canvas,
                    IntPair.getFirst(packed),
                    IntPair.getSecond(packed),
                    numberX,
                    lineNumberWidth,
                    if (IntPair.getFirst(packed) == currentLineNumber) color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lineNumberColor
                )
            }
            canvas.restore()
        }

        if (editor.isBlockLineEnabled()) {
            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            if (editor.isWordwrap) {
                drawSideBlockLine(canvas)
            } else {
                drawBlockLines(canvas, textOffset)
            }
            canvas.restore()
        }

        if (!editor.cursorAnimator.isRunning()) {
            for (action in postDrawCursor) {
                action?.execute(canvas)
            }
        } else {
            drawSelectionOnAnimation(canvas)
        }

        drawStuckLines(canvas, stuckLines, textOffset)

        if (editor.isLineNumberEnabled && !lineNumberNotPinned) {
            drawLineNumberBackground(
                canvas,
                0f,
                lineNumberWidth + sideIconWidth + editor.dividerMarginLeft,
                color.getColor(EditorColorScheme.LINE_NUMBER_BACKGROUND)
            )

            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            val lineNumberColor: Int = editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER)
            val currentLineBgColor: Int = editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE)
            if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine && editor.isEditable) {
                tmpRect.bottom = (editor.cursorAnimator.animatedLineBottom() - editor.offsetY).toFloat()
                tmpRect.top = tmpRect.bottom - editor.cursorAnimator.animatedLineHeight()
                tmpRect.left = 0f
                tmpRect.right = (textOffset - editor.getDividerMarginRight() + editor.offsetX).toFloat()
                drawColor(canvas, currentLineBgColor, tmpRect)
            }
            for (i in 0 until postDrawCurrentLines.size) {
                drawRowBackground(
                    canvas,
                    currentLineBgColor,
                    postDrawCurrentLines.get(i),
                    (textOffset - editor.getDividerMarginRight() + editor.offsetX).toInt()
                )
            }
            drawUserGutterBackground(canvas, (textOffset - editor.getDividerMarginRight() + editor.offsetX).toInt())
            drawSideIcons(canvas, lineNumberWidth)
            canvas.restore()

            drawDivider(
                canvas,
                lineNumberWidth + sideIconWidth + editor.dividerMarginLeft,
                color.getColor(EditorColorScheme.LINE_DIVIDER)
            )

            canvas.save()
            canvas.clipRect(0f, stuckLineBottom, editor.width.toFloat(), editor.height.toFloat())
            for (i in 0 until postDrawLineNumbers.size) {
                val packed: Long = postDrawLineNumbers.get(i)
                drawLineNumber(
                    canvas,
                    IntPair.getFirst(packed),
                    IntPair.getSecond(packed),
                    0f,
                    lineNumberWidth,
                    if (IntPair.getFirst(packed) == currentLineNumber) color.getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lineNumberColor
                )
            }
            canvas.restore()
        }

        drawStuckLineNumbers(
            canvas,
            stuckLines,
            offsetX,
            lineNumberWidth,
            editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER)
        )
        drawScrollBars(canvas)
        drawEdgeEffect(canvas)

        releasePreloadedData()
        lastStuckLines = stuckLines
        drawFormatTip(canvas)
    }

    protected fun drawUserGutterBackground(canvas: Canvas, right: Int) {
        var first = editor.firstVisibleLine
        val last = editor.lastVisibleLine
        for (line in first..last) {
            val bg: ResolvableColor? = getUserGutterBackgroundForLine(line)
            if (bg != null) {
                val bgColor =
                    bg.resolve(editor.colorScheme)
                val top = (editor.layout!!.getCharLayoutOffset(line, 0)[0] / editor.logicalRowHeight).toInt() - 1
                val count =
                    editor.layout!!.getRowCountForLine(line)
                for (i in 0 until count) {
                    drawRowBackground(canvas, bgColor, top + i, right)
                }
            }
        }
    }

    protected fun drawStuckLineNumbers(
        canvas: Canvas,
        candidates: List<CodeBlock?>?,
        offset: Float,
        lineNumberWidth: Float,
        lineNumberColor: Int
    ) {
        if (candidates == null || candidates.isEmpty() || !editor.isLineNumberEnabled) {
            return
        }
        val cur = editor.cursor
        val currentLine = if (cur?.isSelected() == true) -1 else (cur?.leftLine ?: -1)
        canvas.save()
        val offsetY =
            editor.offsetY
        canvas.translate(0f, offsetY.toFloat())
        for (i in 0 until candidates.size) {
            val block =
                candidates[i] ?: continue
            val line = block.startLine
            val bg: ResolvableColor? = getUserGutterBackgroundForLine(line)
            val color = if (bg != null) bg.resolve(editor.colorScheme) else 0
            val bottomOffset =
                editor.getRowBottom(i)
            val endLineTop =
                editor.getRowTop(block.endLine) - editor.offsetY
            val shouldTranslate = endLineTop < bottomOffset && endLineTop >= bottomOffset - editor.logicalRowHeight
            if (shouldTranslate) {
                canvas.save()
                canvas.clipRect(0f, (editor.getRowTop(i) - offsetY).toFloat(), editor.width.toFloat(), editor.height.toFloat())
                canvas.translate(0f, (endLineTop - bottomOffset).toFloat())
            }
            if (currentLine == line || color != 0) {
                tmpRect.top = (editor.getRowTop(i) - offsetY).toFloat()
                tmpRect.bottom = (editor.getRowBottom(i) - offsetY - editor.dpUnit).toFloat()
                tmpRect.left = if (editor.isLineNumberPinned) 0f else offset
                tmpRect.right = tmpRect.left + editor.measureTextRegionOffset()
                if (currentLine == line && editor.isHighlightCurrentLine) drawColor(
                    canvas,
                    editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE),
                    tmpRect
                )
                if (color != 0) drawColor(canvas, color, tmpRect)
            }
            drawLineNumber(
                canvas, line, i,
                if (editor.isLineNumberPinned) 0f else offset, lineNumberWidth,
                if (currentLine == line) editor.colorScheme
                    .getColor(EditorColorScheme.LINE_NUMBER_CURRENT) else lineNumberColor
            )
            if (shouldTranslate) {
                canvas.restore()
            }
        }
        canvas.restore()
    }

    protected fun getStuckLineBottom(candidates: List<CodeBlock?>?): Float {
        if (candidates == null || candidates.isEmpty()) {
            return 0f
        }
        var bottomOffset = 0f
        var offsetLine = 0
        var previousLine = -1
        for (i in 0 until candidates.size) {
            val block =
                candidates.get(i) ?: continue
            if (block.startLine > previousLine) {
                bottomOffset = editor.getRowBottom(offsetLine).toFloat()
                val endLineTop =
                    editor.getRowTop(block.endLine) - editor.offsetY
                val shouldTranslate = endLineTop < bottomOffset && endLineTop >= bottomOffset - editor.logicalRowHeight
                if (shouldTranslate) {
                    bottomOffset += (endLineTop - bottomOffset).toFloat()
                }
                previousLine = block.startLine
                offsetLine++
            }
        }
        return bottomOffset
    }

    protected fun drawStuckLines(canvas: Canvas, candidates: List<CodeBlock?>?, offset: Float) {
        if (candidates == null || candidates.isEmpty()) {
            return
        }
        val styles = editor.styles
        var reader: Spans.Reader? = null
        val spans = styles?.spans
        if (spans != null) {
            reader = spans.read()
        }
        var previousLine = -1
        var offsetLine = 0
        val cur = editor.cursor
        val currentLine = if (cur?.isSelected() == true) -1 else (cur?.leftLine ?: -1)
        var bottomOffset = 0f
        for (i in 0 until (candidates?.size ?: 0)) {
            val block =
                candidates?.get(i) ?: continue
            if (block.startLine > previousLine) {
                tmpRect.top = editor.getRowTop(offsetLine).toFloat()
                tmpRect.bottom = editor.getRowBottom(offsetLine).toFloat()
                bottomOffset = tmpRect.bottom
                tmpRect.left = offset
                tmpRect.right = editor.width.toFloat()
                val endLineTop =
                    editor.getRowTop(block.endLine) - editor.offsetY
                val shouldTranslate = endLineTop < tmpRect.bottom && endLineTop >= tmpRect.top
                if (shouldTranslate) {
                    canvas.save()
                    canvas.clipRect(0f, tmpRect.top, editor.width.toFloat(), editor.height.toFloat())
                    canvas.translate(0f, (endLineTop - tmpRect.bottom).toFloat())
                    bottomOffset += (endLineTop - tmpRect.bottom).toFloat()
                }
                var colorId =
                    EditorColorScheme.WHOLE_BACKGROUND
                if (block.startLine == currentLine && editor.isHighlightCurrentLine) {
                    colorId = EditorColorScheme.CURRENT_LINE
                }
                drawColor(canvas, editor.colorScheme.getColor(colorId), tmpRect)
                if (canvas.isHardwareAccelerated && editor.isHardwareAcceleratedDrawAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && editor.renderContext
                        ?.renderNodeHolder != null && !editor.touchHandler!!.isScaling &&
                    (editor.props!!.cacheRenderNodeForLongLines || getLine(block.startLine).length < 128)
                ) {
                    editor.renderContext?.renderNodeHolder?.drawLineHardwareAccelerated(
                        canvas,
                        block.startLine,
                        offset,
                        (offsetLine * editor.logicalRowHeight).toFloat()
                    )
                } else {
                    try {
                        if (reader != null) {
                            reader.moveToLine(block.startLine)
                        }
                        drawSingleTextLine(
                            canvas,
                            block.startLine,
                            offset,
                            (offsetLine * editor.logicalRowHeight).toFloat(),
                            reader,
                            true
                        )
                    } finally {
                        if (reader != null) {
                            reader.moveToLine(-1)
                        }
                    }
                }
                previousLine = block.startLine
                offsetLine++
                if (shouldTranslate) {
                    canvas.restore()
                }
            }
        }
        if (bottomOffset > 0f) {
            tmpRect.top = bottomOffset - editor.dpUnit
            tmpRect.bottom = bottomOffset
            tmpRect.left = 0f
            tmpRect.right = editor.width.toFloat()
            val shadow =
                (editor.props!!.stickyLineIndicator and DirectAccessProps.STICKY_LINE_INDICATOR_SHADOW) !== 0
            var showLine =
                (editor.props!!.stickyLineIndicator and DirectAccessProps.STICKY_LINE_INDICATOR_LINE) !== 0
            if (!shadow && !showLine) {
                return
            }
            val lineColor =
                editor.colorScheme.getColor(EditorColorScheme.STICKY_SCROLL_DIVIDER)
            showLine = lineColor != 0
            if (shadow) {
                canvas.save()
                canvas.clipRect(0f, if (showLine) tmpRect.top else tmpRect.bottom, editor.width.toFloat(), editor.height.toFloat())
                paintGeneral.setShadowLayer(
                    editor.dpUnit * RenderingConstants.DIVIDER_SHADOW_MAX_RADIUS_DIP,
                    0f,
                    0f,
                    Color.BLACK
                )
            }
            val color = if (!showLine && shadow) Color.BLACK else lineColor
            drawColor(canvas, color, tmpRect)
            if (shadow) {
                paintGeneral.setShadowLayer(0f, 0f, 0f, 0)
                canvas.restore()
            }
        }
    }

    protected fun drawHardwrapMarker(canvas: Canvas?, offset: Float) {
        val column = editor.props!!.hardwrapColumn
        if (!editor.isWordwrap && column > 0) {
            tmpRect.left = offset + paintGeneral.measureText("a") * column
            tmpRect.right = tmpRect.left + editor.dpUnit * 2f
            tmpRect.top = 0f
            tmpRect.bottom = viewRect.bottom.toFloat()
            drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.HARD_WRAP_MARKER), tmpRect)
        }
    }

    protected fun drawSideIcons(canvas: Canvas?, offset: Float) {
        if (!hasSideHintIcons()) {
            return
        }
        var row =
            editor.firstVisibleRow
        val itr =
            editor.layout!!.obtainRowIterator(row)
        val iconSizeFactor =
            editor.props!!.sideIconSizeFactor
        val size = (editor.logicalRowHeight * iconSizeFactor) as Int
        val offsetToLeftTop = (editor.logicalRowHeight * (1 - iconSizeFactor) / 2f) as Int
        while (row <= editor.lastVisibleRow && itr.hasNext()) {
            val rowInf = itr.next()
            if (rowInf.isLeadingRow) {
                val hint: LineSideIcon? = getLineStyle(rowInf.lineIndex, LineSideIcon::class.java)
                if (hint != null) {
                    val drawable = hint.drawable
                    val rect = Rect(0, 0, size, size)
                    rect.offsetTo(
                        offset.toInt() + offsetToLeftTop,
                        (editor.getRowTop(row) - editor.offsetY + offsetToLeftTop).toInt()
                    )
                    drawable.bounds = rect
                    drawable.draw(canvas!!)
                }
            }
            row++
        }
    }

    protected fun drawFormatTip(canvas: Canvas) {
        if (editor.isFormatting) {
            val text = editor.formatTip
            val baseline = editor.getRowBaseline(0).toFloat()
            val rightX = editor.width.toFloat()
            paintGeneral.color = editor.colorScheme.getColor(EditorColorScheme.TEXT_NORMAL)
            paintGeneral.isFakeBoldText = true
            paintGeneral.textAlign = AndroidPaint.Align.RIGHT
            if (text != null) {
                canvas.drawText(text, rightX, baseline, paintGeneral)
            }
            paintGeneral.textAlign = AndroidPaint.Align.LEFT
            paintGeneral.isFakeBoldText = false
        }
    }

    /**
     * Draw rect on screen
     * Will not do anything if color is zero
     * 
     * @param canvas Canvas to draw
     * @param color  Color of rect
     * @param rect   Rect to draw
     */
    protected fun drawColor(canvas: Canvas?, color: Int, rect: RectF?) {
        if (canvas != null && color != 0 && rect != null) {
            paintGeneral.color = color
            canvas.drawRect(rect, paintGeneral)
        }
    }

    /**
     * Draw rect on screen in a round rectangle
     * Will not do anything if color is zero
     * 
     * @param canvas Canvas to draw
     * @param color  Color of rect
     * @param rect   Rect to draw
     */
    protected fun drawColorRound(canvas: Canvas, color: Int, rect: RectF) {
        if (color != 0) {
            paintGeneral.color = color
            canvas.drawRoundRect(
                rect,
                rect.height() * RenderingConstants.ROUND_RECT_FACTOR,
                rect.height() * RenderingConstants.ROUND_RECT_FACTOR,
                paintGeneral
            )
        }
    }

    /**
     * Draw rect on screen
     * Will not do anything if color is zero
     * 
     * @param canvas Canvas to draw
     * @param color  Color of rect
     * @param rect   Rect to draw
     */
    protected fun drawColor(canvas: Canvas?, color: Int, rect: Rect?) {
        if (canvas != null && color != 0 && rect != null) {
            paintGeneral.color = color
            canvas.drawRect(rect, paintGeneral)
        }
    }

    /**
     * Draw background for whole row
     */
    protected fun drawRowBackground(canvas: Canvas, color: Int, row: Int) {
        drawRowBackground(canvas, color, row, viewRect.right)
    }

    protected fun drawRowBackground(canvas: Canvas, color: Int, row: Int, right: Int) {
        tmpRect.top = (editor.getRowTop(row) - editor.offsetY).toFloat()
        tmpRect.bottom = (editor.getRowBottom(row) - editor.offsetY).toFloat()
        tmpRect.left = 0f
        tmpRect.right = right.toFloat()
        drawColor(canvas, color, tmpRect)
    }

    /**
     * Draw single line number
     */
    protected fun drawLineNumber(canvas: Canvas, line: Int, row: Int, offsetX: Float, width: Float, color: Int) {
        var line = line
        if (width + offsetX <= 0) {
            return
        }
        if (paintOther.getTextAlign() !== editor.getLineNumberAlign()) {
            paintOther.setTextAlign(editor.getLineNumberAlign())
        }
        paintOther.setColor(color)
        // Line number center align to text center
        val y: Float =
            (editor.getRowBottom(row) + editor.getRowTop(row)) / 2f - (metricsLineNumber.descent - metricsLineNumber.ascent) / 2f - metricsLineNumber.ascent - editor.offsetY

        val buffer =
            TemporaryCharBuffer.obtain(20)
        line++
        val i: Int = stringSize(line)
        io.github.abc15018045126.sora.util.Numbers.getChars(line, i, buffer)

        when (editor.lineNumberAlign) {
            AndroidPaint.Align.LEFT -> canvas.drawText(buffer, 0, i, offsetX, y, paintOther)
            AndroidPaint.Align.RIGHT -> canvas.drawText(buffer, 0, i, offsetX + width, y, paintOther)
            AndroidPaint.Align.CENTER -> canvas.drawText(
                buffer,
                0,
                i,
                offsetX + (width + editor.dividerMarginLeft) / 2f,
                y,
                paintOther
            )
            else -> {}
        }
        TemporaryCharBuffer.recycle(buffer)
    }

    /**
     * Draw line number background
     * 
     * @param canvas  Canvas to draw
     * @param offsetX Start x of line number region
     * @param width   Width of line number region
     * @param color   Color of line number background
     */
    protected fun drawLineNumberBackground(canvas: Canvas, offsetX: Float, width: Float, color: Int) {
        val right = offsetX + width
        if (right < 0) {
            return
        }
        val left = max(0f, offsetX)
        tmpRect.bottom = editor.height.toFloat()
        tmpRect.top = 0f
        val offY = editor.offsetY
        if (offY < 0) {
            tmpRect.bottom = tmpRect.bottom - offY.toFloat()
            tmpRect.top = tmpRect.top - offY.toFloat()
        }
        tmpRect.left = left
        tmpRect.right = right
        drawColor(canvas, color, tmpRect)
    }

    /**
     * Draw divider line
     * 
     * @param canvas  Canvas to draw
     * @param offsetX End x of line number region
     * @param color   Color to draw divider
     */
    protected fun drawDivider(canvas: Canvas, offsetX: Float, color: Int) {
        val shadow = editor.isLineNumberPinned && !editor.isWordwrap && editor.offsetX > 0
        val right = offsetX + editor.dividerWidth
        if (right < 0) {
            return
        }
        val left = max(0f, offsetX)
        tmpRect.bottom = editor.height.toFloat()
        tmpRect.top = 0f
        val offY = editor.offsetY
        if (offY < 0) {
            tmpRect.bottom = tmpRect.bottom - offY.toFloat()
            tmpRect.top = tmpRect.top - offY.toFloat()
        }
        tmpRect.left = left
        tmpRect.right = right
        if (shadow) {
            canvas.save()
            canvas.clipRect(tmpRect.left, tmpRect.top, editor.width.toFloat(), tmpRect.bottom)
            paintGeneral.setShadowLayer(
                min(
                    (editor.dpUnit * RenderingConstants.DIVIDER_SHADOW_MAX_RADIUS_DIP).toFloat(),
                    editor.offsetX.toFloat()
                ), 0f, 0f, Color.BLACK
            )
        }
        drawColor(canvas, color, tmpRect)
        if (shadow) {
            canvas.restore()
            paintGeneral.setShadowLayer(0f, 0f, 0f, 0)
        }
    }

    private fun prepareLines(start: Int, end: Int) {
        val content = this.content ?: return
        releasePreloadedData()
        content.runReadActionsOnLines(
            Math.max(0, start - 5),
            Math.min(content.lineCount - 1, end + 5),
            { i: Int, line: ContentLine?, dirs: Directions? ->
                preloadedLines.put(i, line)
                preloadedDirections.put(i, dirs)
            })
    }

    private fun releasePreloadedData() {
        preloadedLines.clear()
        preloadedDirections.clear()
    }

    protected val stuckCodeBlocks: List<CodeBlock>?
        get() {
            if (editor.isWordwrap || !editor.props!!.stickyScroll) {
                return null
            }
            val styles = editor.styles ?: return null
            val codeBlocks = styles.blocksByStart ?: return null
            
            var startLine = editor.firstVisibleLine
            var offsetY = editor.offsetY
            val rowHeight = editor.logicalRowHeight
            val size = codeBlocks.size
            if (size == 0) {
                return null
            }
            val candidates = mutableListOf<CodeBlock>()
            val limit = editor.props!!.stickyScrollIterationLimit
            val maxLine = content!!.lineCount
            var i = 0
            while (i < size && i < limit) {
                val block = codeBlocks[i]
                if (block == null || block.startLine > block.endLine || block.startLine > maxLine || block.endLine > maxLine || block.startLine < 0) {
                    i++
                    continue
                }
                if (block.startLine > startLine) {
                    break
                }
                if (block.endLine > startLine && editor.getRowTop(block.startLine) - offsetY < 0) {
                    candidates.add(block)
                    startLine++
                    offsetY += rowHeight
                }
                i++
            }
            
            val maxLines = editor.props!!.stickyScrollMaxLines
            var finalCandidates: List<CodeBlock> = candidates
            if (finalCandidates.size > maxLines) {
                if (maxLines <= 0) {
                    return null
                }
                finalCandidates = if (editor.props!!.stickyScrollPreferInnerScope) {
                    finalCandidates.subList(finalCandidates.size - maxLines, finalCandidates.size)
                } else {
                    finalCandidates.subList(0, maxLines)
                }
            }
            val cur = editor.cursor
            if (cur != null && cur.isSelected() && editor.props!!.stickyScrollAutoCollapse) {
                val limitLine = cur.leftLine
                val firstVis = editor.firstVisibleLine
                val lastSelectionLine = cur.rightLine
                if (lastSelectionLine >= firstVis) {
                    val mutableCandidates = finalCandidates.toMutableList()
                    while (mutableCandidates.isNotEmpty() && firstVis + mutableCandidates.size >= limitLine) {
                        mutableCandidates.removeAt(mutableCandidates.size - 1)
                    }
                    finalCandidates = mutableCandidates
                }
            }
            return if (finalCandidates.isEmpty()) null else finalCandidates
        }

    private val coordinateLine: LineStyles = LineStyles(0)

    init {
        this.editor = editor
        verticalScrollBarRect = RectF()
        horizontalScrollBarRect = RectF()

        bufferedDrawPoints = BufferedDrawPoints()

        paintGeneral = Paint()
        paintGeneral.isFilterBitmap = editor.isRenderFunctionCharacters
        paintGeneral.setAntiAlias(true)
        paintOther = Paint(false)
        paintOther.setStrokeWidth(this.editor.dpUnit * 1.8f)
        paintOther.setStrokeCap(AndroidPaint.Cap.ROUND)
        paintOther.setTypeface(Typeface.MONOSPACE)
        paintOther.setAntiAlias(true)
        paintGraph = Paint(false)
        paintGraph.setAntiAlias(true)

        metricsText = paintGeneral.getFontMetricsInt()
        metricsLineNumber = paintOther.getFontMetricsInt()

        viewRect = Rect()
        tmpRect = RectF()
        tmpPath = Path()

        lineBreakGraph = editor.getContext().getDrawable(R.drawable.line_break)
        softwrapLeftGraph = editor.getContext().getDrawable(R.drawable.softwrap_left)
        softwrapRightGraph = editor.getContext().getDrawable(R.drawable.softwrap_right)

        onEditorFullTextUpdate()
    }

    @Nullable
    protected fun getLineStyles(line: Int): LineStyles? {
        val styles = editor.styles ?: return null
        val lineStylesList = styles.lineStyles ?: return null
        coordinateLine.line = line
        val index =
            Collections.binarySearch(lineStylesList, coordinateLine)
        if (index >= 0 && index < lineStylesList.size) {
            return lineStylesList[index]
        }
        return null
    }

    @Nullable
    internal fun <T : LineAnchorStyle> getLineStyle(line: Int, type: Class<T>): T? {
        val lineStyles: LineStyles? = getLineStyles(line)
        if (lineStyles != null) {
            return lineStyles.findOne(type)
        }
        return null
    }

    @Nullable
    protected fun getUserBackgroundForLine(line: Int): ResolvableColor? {
        val bg: LineBackground? = getLineStyle(line, LineBackground::class.java)
        if (bg != null) {
            return bg.color
        }
        return null
    }

    @Nullable
    protected fun getUserGutterBackgroundForLine(line: Int): ResolvableColor? {
        val bg: LineGutterBackground? = getLineStyle(line, LineGutterBackground::class.java)
        if (bg != null) {
            return bg.color
        }
        return null
    }

    /**
     * Draw current line background during animation
     */
    protected fun drawAnimatedCurrentLineBackground(canvas: Canvas, currentLineBgColor: Int) {
        tmpRect.bottom = (editor.cursorAnimator.animatedLineBottom() - editor.offsetY).toFloat()
        tmpRect.top = tmpRect.bottom - editor.cursorAnimator.animatedLineHeight()
        tmpRect.left = 0f
        tmpRect.right = viewRect.right.toFloat()
        drawColor(canvas, currentLineBgColor, tmpRect)
    }

    fun createTextRowParams(): TextRowParams {
        return TextRowParams(
            editor.tabWidth, this.metricsText!!, editor.getRowTopOfText(0),
            editor.getRowBottomOfText(0), editor.logicalRowHeight, editor.getRowBaseline(0),
            editor.getRowTop(0), editor.getRowBottom(0),
            editor.logicalRowHeight, editor.props!!.roundTextBackgroundFactor,
            editor, editor.colorScheme, paintOther, paintGraph, metricsGraph!!
        )
    }

    /**
     * Draw rows with a [RowIterator]
     * 
     * @param canvas              Canvas to draw
     * @param offset              Offset of text region start
     * @param postDrawLineNumbers Line numbers to be drawn later
     * @param postDrawCursor      Cursors to be drawn later
     */
    protected fun drawRows(
        canvas: Canvas,
        offset: Float,
        postDrawLineNumbers: LongArrayList,
        postDrawCursor: MutableList<DrawCursorTask?>,
        postDrawCurrentLines: MutableIntList,
        requiredFirstLn: MutableInt?
    ) {
        val cursor = this.cursor ?: return
        val content = this.content ?: return
        val firstVis: Int = editor.firstVisibleRow
        val rowIterator: RowIterator = editor.layout!!.obtainRowIterator(firstVis, preloadedLines)
        val spans: Spans? = editor.styles?.spans
        val matchedPositions: LongArrayList = this.matchedPositions
        val highlightPositions: MutableLongLongMap = this.highlightPositions
        matchedPositions.clear()
        highlightPositions.clear()
        val currentLine = if (cursor.isSelected()) -1 else cursor.leftLine
        val currentLineBgColor: Int = editor.colorScheme.getColor(EditorColorScheme.CURRENT_LINE)
        val currentRow = if (cursor.isSelected()) -1 else editor.layout!!.getRowIndexForPosition(cursor.left)
        val currentRowBorder: Int = editor.colorScheme.getColor(EditorColorScheme.CURRENT_ROW_BORDER)
        var lastPreparedLine = -1
        var leadingWhitespaceEnd = 0
        var trailingWhitespaceStart = 0
        var circleRadius = 0f
        val miniGraphWidth =
            if (editor.isWordwrap && (editor.nonPrintablePaintingFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_SOFT_WRAP) !== 0) this.miniGraphWidth else 0f
        val composingPosition =
            if (editor.inputConnection?.composingText?.isComposing() == true && editor.inputConnection!!.composingText.startIndex >= 0 && editor.inputConnection!!.composingText.startIndex < content.length) content.getIndexer()
                .getCharPosition(editor.inputConnection!!.composingText.startIndex) else null
        val composingLength =
            editor.inputConnection!!.composingText.endIndex - editor.inputConnection!!.composingText.startIndex
        val draggingSelection =
            editor.touchHandler?.draggingSelection
        if (editor.shouldInitializeNonPrintable()) {
            val spaceWidth: Float = paintGeneral.spaceWidth
            circleRadius =
                Math.min(editor.logicalRowHeight.toFloat(), spaceWidth) * RenderingConstants.NON_PRINTABLE_CIRCLE_RADIUS_FACTOR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !editor.isWordwrap && canvas.isHardwareAccelerated && editor.isHardwareAcceleratedDrawAllowed) {
            editor.renderContext?.renderNodeHolder?.keepCurrentInDisplay(firstVis, editor.lastVisibleRow)
        }
        val offset2: Float = editor.offsetX - editor.measureTextRegionOffset()

        // Step 1 - Draw background of rows

        val trParams = createTextRowParams()

        // Pre-draw animated current line background
        if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine && editor.isEditable
            && (editor.props!!.cursorLineBgOverlapBehavior === CURSOR_LINE_BG_OVERLAP_CURSOR || editor.props!!.cursorLineBgOverlapBehavior === CURSOR_LINE_BG_OVERLAP_MIXED)
        ) {
            drawAnimatedCurrentLineBackground(canvas, currentLineBgColor)
        }
        // Draw custom line backgrounds & normal current line background
        run {
            var row = firstVis
            while (row <= editor.lastVisibleRow && rowIterator.hasNext()) {
                val rowInf: Row = rowIterator.next()
                val line: Int = rowInf.lineIndex
                if (lastPreparedLine != line) {
                    prepareLine(line)
                    lastPreparedLine = line
                }

                val lineBgOverlapBehavior =
                    editor.props!!.cursorLineBgOverlapBehavior

                var drawCurrentLineBg = line == currentLine && !editor.cursorAnimator.isRunning() &&
                        editor.isHighlightCurrentLine &&
                        editor.isEditable

                val drawCustomLineBg = !drawCurrentLineBg
                        || (editor.props!!.drawCustomLineBgOnCurrentLine && lineBgOverlapBehavior != CURSOR_LINE_BG_OVERLAP_CUSTOM)

                var isOverlapping = false

                if (drawCustomLineBg) {
                    // Draw custom background
                    val customBackground: ResolvableColor? = getUserBackgroundForLine(line)
                    if (customBackground != null) {
                        val color =
                            customBackground.resolve(editor.colorScheme)
                        if (line == currentLine) {
                            isOverlapping = true
                        }

                        drawRowBackground(canvas, color, row)
                    }
                }

                if (isOverlapping) {
                    drawCurrentLineBg = drawCurrentLineBg and (lineBgOverlapBehavior != CURSOR_LINE_BG_OVERLAP_CURSOR)
                }

                if (drawCurrentLineBg) {
                    var commitCurrentLineBg = currentLineBgColor
                    if (isOverlapping && lineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_MIXED) {
                        // alpha = 0.5f = 0.5 * 255 = 128 = 0x80
                        commitCurrentLineBg = (commitCurrentLineBg and 0x00FFFFFF) or -0x80000000
                    }

                    // Draw current line background
                    drawRowBackground(canvas, commitCurrentLineBg, row)
                    postDrawCurrentLines.add(row)
                }
                row++
            }
        }
        // Post-draw animated current line background
        if (editor.cursorAnimator.isRunning() && editor.isHighlightCurrentLine
            && editor.props!!.cursorLineBgOverlapBehavior == CURSOR_LINE_BG_OVERLAP_CUSTOM
        ) {
            drawAnimatedCurrentLineBackground(canvas, currentLineBgColor)
        }
        rowIterator.reset()

        // Other system line background are drawn last
        run {
            var row = firstVis
            while (row <= editor.lastVisibleRow && rowIterator.hasNext()) {
                val rowInf: Row = rowIterator.next()
                canvas.save()
                canvas.translate(rowInf.renderTranslateX, 0f)
                val line: Int = rowInf.lineIndex
                val columnCount = getColumnCount(line)
                if (lastPreparedLine != line) {
                    editor.computeMatchedPositions(line, matchedPositions)
                    editor.computeHighlightPositions(line, highlightPositions)
                    prepareLine(line)
                    lastPreparedLine = line
                }
                var paintingOffset = -offset2
                if (!rowInf.isLeadingRow) paintingOffset += miniGraphWidth

                // Draw matched text background
                if (matchedPositions.size > 0) {
                    updateTextRow(sharedTextRow, row)
                    for (i in 0 until matchedPositions.size) {
                        val position =
                            matchedPositions.get(i)
                        val start =
                            IntPair.getFirst(position)
                        val end =
                            IntPair.getSecond(position)
                        drawRowRegionBackground(
                            canvas,
                            row,
                            sharedTextRow,
                            start,
                            end,
                            rowInf.startColumn,
                            rowInf.endColumn,
                            editor.colorScheme.getColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND),
                            editor.colorScheme.getColor(EditorColorScheme.MATCHED_TEXT_BORDER)
                        )
                    }
                }

                // Draw highlight text background
                if (highlightPositions.size > 0) {
                    val finalRow = row
                    val tr: TextRow = createTextRow(row)
                    highlightPositions.forEach(object : MutableLongLongMap.Consumer {
                        override fun accept(key: Long, value: Long): Any? {
                            val start = IntPair.getFirst(key)
                            val end = IntPair.getSecond(key)
                            updateTextRow(sharedTextRow, finalRow)
                            drawRowRegionBackground(
                                canvas, finalRow, sharedTextRow, start, end, rowInf.startColumn,
                                rowInf.endColumn, IntPair.getFirst(value), IntPair.getSecond(value)
                            )
                            return null
                        }
                    })
                }

                // Draw selected text background
                if (cursor.isSelected() && line >= cursor.leftLine && line <= cursor.rightLine) {
                    var selectionStart = 0
                    var selectionEnd = columnCount
                    if (line == cursor.leftLine) {
                        selectionStart = cursor.leftColumn
                    }
                    if (line == cursor.rightLine) {
                        selectionEnd = cursor.rightColumn
                    }
                    val columnCountLine = getColumnCount(line)
                    if (columnCountLine == 0 && line != cursor.rightLine) {
                        tmpRect.top = (getRowTopForBackground(row) - editor.offsetY).toFloat()
                        tmpRect.bottom = (getRowBottomForBackground(row) - editor.offsetY).toFloat()
                        tmpRect.left = paintingOffset
                        tmpRect.right = tmpRect.left + paintGeneral.spaceWidth * 2
                        drawRowBackgroundRectWithBorder(
                            canvas, tmpRect,
                            editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND),
                            editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BORDER)
                        )
                    } else if (selectionStart < selectionEnd) {
                        updateTextRow(sharedTextRow, row)
                        drawRowRegionBackground(
                            canvas, row, sharedTextRow, selectionStart, selectionEnd, rowInf.startColumn, rowInf.endColumn,
                            editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND),
                            editor.colorScheme.getColor(EditorColorScheme.SELECTED_TEXT_BORDER)
                        )
                    }
                }
                canvas.restore()

                // Draw current row border
                if (row == currentRow && currentRowBorder != 0) {
                    tmpRect.top = (editor.getRowTop(row) - editor.offsetY).toFloat()
                    tmpRect.bottom = (editor.getRowBottom(row) - editor.offsetY).toFloat()
                    tmpRect.left = max(0f, -offset2)
                    tmpRect.right = editor.width.toFloat()
                    paintGeneral.setColor(currentRowBorder)
                    paintGeneral.setStyle(android.graphics.Paint.Style.STROKE)
                    paintGeneral.setStrokeWidth(editor.dpUnit)
                    canvas.drawRect(tmpRect, paintGeneral)
                    paintGeneral.setStyle(android.graphics.Paint.Style.FILL)
                }
                row++
            }
        }
        rowIterator.reset()

        // Background of snippets
        patchSnippetRegions(canvas, offset)

        // Hard wrap marker
        drawHardwrapMarker(canvas, offset)

        // Step 2 - Draw text and text decorations
        var reader: Spans.Reader? = null
        lastPreparedLine = -1
        var lineCache: TextAdvancesCache? = null
        var row = firstVis
        while (row <= editor.lastVisibleRow && rowIterator.hasNext()) {
            val rowInf: Row = rowIterator.next()
            val line: Int = rowInf.lineIndex
            val contentLine: ContentLine = getLine(line)
            val columnCount: Int = contentLine.length
            if (row == firstVis && requiredFirstLn != null) {
                requiredFirstLn.value = line
            } else if (rowInf.isLeadingRow) {
                postDrawLineNumbers.add(IntPair.pack(line, row))
            }

            // Prepare data
            if (lastPreparedLine != line) {
                lastPreparedLine = line
                val cache =
                    editor.renderContext!!.cache.queryMeasureCache(line)
                if (cache != null && cache.updateTimestamp == this.timestamp && cache.widths != null && cache.widths!!
                        .size > columnCount
                ) {
                    lineCache = cache.widths
                } else {
                    lineCache = null
                }
                prepareLine(line)
                // Release old reader
                if (reader != null) {
                    try {
                        reader.moveToLine(-1)
                    } catch (e: Exception) {
                        Log.w(
                            io.github.abc15018045126.sora.widget.EditorRenderer.Companion.LOG_TAG,
                            "Failed to release SpanReader",
                            e
                        )
                    }
                }
                // Get new reader and lock
                // Note that we should hold the reader during the **text line** rendering process
                // Otherwise, the spans?.filterNotNull() of that line can be changed during the inter rendering time
                // between two **rows** because the spans?.filterNotNull() could have been changed concurrently
                // See #290
                reader = if (spans == null) EmptyReader.INSTANCE else spans.read()
                try {
                    reader.moveToLine(line)
                } catch (e: Exception) {
                    Log.w(
                        io.github.abc15018045126.sora.widget.EditorRenderer.Companion.LOG_TAG,
                        "Failed to read span",
                        e
                    )
                    reader = EmptyReader.INSTANCE
                }
                if (reader!!.getSpanCount() == 0) {
                    // Unacceptable span count, use fallback reader
                    reader = EmptyReader.INSTANCE
                }
                if (editor.shouldInitializeNonPrintable()) {
                    val positions: Long = editor.findLeadingAndTrailingWhitespacePos(lineBuf!!)
                    leadingWhitespaceEnd = IntPair.getFirst(positions)
                    trailingWhitespaceStart = IntPair.getSecond(positions)
                }
            }

            // Get visible region on the line
            var paintingOffset = -offset2
            var offsetCopy = offset2

            paintingOffset += rowInf.renderTranslateX
            offsetCopy -= rowInf.renderTranslateX

            if (!rowInf.isLeadingRow) {
                if ((editor.nonPrintablePaintingFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_SOFT_WRAP) !== 0) {
                    drawMiniGraph(canvas, offset, row, softwrapLeftGraph)
                    paintingOffset += miniGraphWidth
                    offsetCopy -= miniGraphWidth
                }
            }

            val backupOffset = paintingOffset
            val nonPrintableFlags: Int = editor.nonPrintablePaintingFlags

            // Draw text here
            if (!editor.isHardwareAcceleratedDrawAllowed || editor.touchHandler!!.isScaling || !canvas.isHardwareAccelerated || editor.isWordwrap || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || (rowInf.endColumn - rowInf.startColumn > 128 && !editor.props!!.cacheRenderNodeForLongLines) /* Save memory */) {
                // Draw without hardware acceleration
                sharedTextRow.set(
                    lineBuf!!,
                    rowInf.startColumn,
                    rowInf.endColumn,
                    reader!!.getSpansOnLine(line),
                    rowInf.inlayHints,
                    getLineDirections(line)!!,
                    paintGeneral,
                    lineCache,
                    trParams
                )
                applySelectedTextRange(sharedTextRow, line)

                canvas.save()
                canvas.translate(-offsetCopy, (editor.getRowTop(row) - editor.offsetY).toFloat())
                // visible editor window: [offsetX, offsetX+editorWidth]
                // current row window region: [textRegionOffsetW+leftMiniGraphWidth, textRegionOffsetX+leftMiniGraphX+rowWidth]
                // shifted start at offsetX-(textRegionOffsetX+leftMiniGraphWidth)
                // visible in-row offset from max{offsetX, textRegionOffsetX+leftMiniGraphWidth} - (textRegionOffsetX+leftMiniGraphWidth)
                // to min{textRegionOffsetX+leftMiniGraphX+rowWidth, offsetX+editorWidth-(textRegionOffsetX+leftMiniGraphWidth)}
                val beginOffset: Float = max(0f, offsetCopy)
                val endOffset: Float = beginOffset + editor.width
                val result =
                    sharedTextRow.draw(canvas, beginOffset, endOffset)
                canvas.restore()

                val exhausted = IntPair.getFirst(result) == 1
                paintingOffset += IntPair.getSecondAsFloat(result)

                // Draw hard wrap & soft wrap
                if (exhausted && rowInf.isTrailingRow && (nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_LINE_SEPARATOR) !== 0) {
                    drawMiniGraph(canvas, paintingOffset, row, lineBreakGraph)
                } else if (!rowInf.isTrailingRow && editor.isWordwrap && (nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_SOFT_WRAP) !== 0) {
                    drawMiniGraph(canvas, paintingOffset, row, softwrapRightGraph)
                }
            } else {
                paintingOffset = editor.renderContext!!.renderNodeHolder
                    ?.drawLineHardwareAccelerated(canvas, line, offset, (editor.getRowTop(row) - editor.offsetY).toFloat())?.toFloat() ?: 0f
                // Draw hard wrap
                if (rowInf.isTrailingRow && (nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_LINE_SEPARATOR) !== 0) {
                    drawMiniGraph(canvas, paintingOffset, row, lineBreakGraph)
                }
            }

            // Recover the offset
            paintingOffset = backupOffset

            // Draw non-printable characters
            if (circleRadius != 0f && (leadingWhitespaceEnd != columnCount || (nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE) !== 0)) {
                sharedTextRow.set(
                    lineBuf!!,
                    rowInf.startColumn,
                    rowInf.endColumn,
                    reader!!.getSpansOnLine(line),
                    rowInf.inlayHints,
                    getLineDirections(line)!!,
                    paintGeneral,
                    lineCache,
                    trParams
                )
                canvas.save()
                val topOfText = (editor.getRowTopOfText(row) - editor.offsetY).toFloat()
                canvas.translate(paintingOffset, topOfText)
                bufferedDrawPoints.setOffsets(paintingOffset, topOfText)
                val beginOffset: Float = max(0f, paintingOffset)
                val endOffset: Float = beginOffset + editor.width
                val wsLeadingEnd = leadingWhitespaceEnd
                val wsTrailingStart = trailingWhitespaceStart

                paintOther.setColor(editor.colorScheme.getColor(EditorColorScheme.NON_PRINTABLE_CHAR))
                sharedTextRow.iterateDrawTextRegions(
                    rowInf.startColumn, rowInf.endColumn, canvas, beginOffset, endOffset, false,
                    object : TextRow.DrawTextConsumer {
                        override fun drawText(
                            _canvas: Canvas?,
                            text: CharArray?,
                            index: Int,
                            count: Int,
                            contextIndex: Int,
                            contextCount: Int,
                            isRtl: Boolean,
                            horizontalOffset: Float,
                            width: Float,
                            params: TextRowParams?,
                            span: Span?
                        ) {
                            if ((nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_WHITESPACE_LEADING) != 0) {
                                drawWhitespaces(
                                    _canvas!!,
                                    sharedTextRow,
                                    text!!,
                                    index,
                                    count,
                                    contextIndex,
                                    contextCount,
                                    isRtl,
                                    horizontalOffset,
                                    width,
                                    0,
                                    wsLeadingEnd
                                )
                            }
                            if ((nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_WHITESPACE_INNER) != 0) {
                                drawWhitespaces(
                                    _canvas!!,
                                    sharedTextRow,
                                    text!!,
                                    index,
                                    count,
                                    contextIndex,
                                    contextCount,
                                    isRtl,
                                    horizontalOffset,
                                    width,
                                    wsLeadingEnd,
                                    wsTrailingStart
                                )
                            }
                            if ((nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_WHITESPACE_TRAILING) != 0) {
                                drawWhitespaces(
                                    _canvas!!,
                                    sharedTextRow,
                                    text!!,
                                    index,
                                    count,
                                    contextIndex,
                                    contextCount,
                                    isRtl,
                                    horizontalOffset,
                                    width,
                                    wsTrailingStart,
                                    columnCount
                                )
                            }
                            if ((nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_WHITESPACE_IN_SELECTION) != 0 && cursor.isSelected() && line >= cursor.leftLine && line <= cursor.rightLine) {
                                var selectionStart = 0
                                var selectionEnd = columnCount
                                if (line == cursor.leftLine) {
                                    selectionStart = cursor.leftColumn
                                }
                                if (line == cursor.rightLine) {
                                    selectionEnd = cursor.rightColumn
                                }
                                if ((nonPrintableFlags and 14) == 0) {
                                    drawWhitespaces(
                                        _canvas!!,
                                        sharedTextRow,
                                        text!!,
                                        index,
                                        count,
                                        contextIndex,
                                        contextCount,
                                        isRtl,
                                        horizontalOffset,
                                        width,
                                        selectionStart,
                                        selectionEnd
                                    )
                                } else {
                                    if ((nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_WHITESPACE_LEADING) == 0) {
                                        drawWhitespaces(
                                            _canvas!!,
                                            sharedTextRow,
                                            text!!,
                                            index,
                                            count,
                                            contextIndex,
                                            contextCount,
                                            isRtl,
                                            horizontalOffset,
                                            width,
                                            selectionStart,
                                            min(wsLeadingEnd, selectionEnd)
                                        )
                                    }
                                    if ((nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_WHITESPACE_INNER) == 0) {
                                        drawWhitespaces(
                                            _canvas!!,
                                            sharedTextRow,
                                            text!!,
                                            index,
                                            count,
                                            contextIndex,
                                            contextCount,
                                            isRtl,
                                            horizontalOffset,
                                            width,
                                            max(wsLeadingEnd, selectionStart),
                                            min(wsTrailingStart, selectionEnd)
                                        )
                                    }
                                    if ((nonPrintableFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_WHITESPACE_TRAILING) == 0) {
                                        drawWhitespaces(
                                            _canvas!!,
                                            sharedTextRow,
                                            text!!,
                                            index,
                                            count,
                                            contextIndex,
                                            contextCount,
                                            isRtl,
                                            horizontalOffset,
                                            width,
                                            max(wsTrailingStart, selectionStart),
                                            selectionEnd
                                        )
                                    }
                                }
                            }
                        }
                    })
                canvas.restore()
                bufferedDrawPoints.setOffsets(0f, 0f)
            }

            // Draw composing text underline
            if (composingPosition != null && line == composingPosition.line) {
                val composingStart: Int = composingPosition.column
                val composingEnd: Int = composingStart + composingLength
                val paintStart: Int = Math.min(Math.max(composingStart, rowInf.startColumn), rowInf.endColumn)
                val paintEnd: Int = Math.min(Math.max(composingEnd, rowInf.startColumn), rowInf.endColumn)

                if (paintStart < paintEnd) {
                    sharedTextRow.set(
                        lineBuf!!,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        reader!!.getSpansOnLine(line),
                        rowInf.inlayHints,
                        content!!.getLineDirections(line),
                        paintGeneral,
                        lineCache,
                        trParams
                    )
                    tmpRect.top = (editor.getRowBottom(row) - editor.offsetY).toFloat()
                    tmpRect.bottom = tmpRect.top + editor.logicalRowHeight.toFloat() * 0.06f
                    val finalOffset = paintingOffset
                    sharedTextRow.iterateBackgroundRegions(paintStart, paintEnd, false, false, object : TextRow.BackgroundRegionConsumer {
                        override fun handleRegion(left: Float, right: Float): Boolean {
                            tmpRect.left = finalOffset + left
                            tmpRect.right = finalOffset + right
                            if (tmpRect.right > 0f && tmpRect.left < editor.width) drawColor(
                                canvas,
                                editor.colorScheme.getColor(EditorColorScheme.UNDERLINE),
                                tmpRect
                            )
                            return tmpRect.right < editor.width
                        }
                    })
                }
            }

            val layout =
                editor.layout!!
            // Draw cursors
            if (cursor.isSelected()) {
                if (cursor.leftLine == line && isInside(
                        cursor.leftColumn,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        rowInf.isTrailingRow
                    )
                ) {
                    val centerX: Float = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                        cursor.leftLine,
                        cursor.leftColumn
                    )[1] - editor.offsetX
                    val type =
                        if (content!!.isRtlAt(
                                cursor.leftLine,
                                cursor.leftColumn
                            )
                        ) SelectionHandleStyle.HANDLE_TYPE_RIGHT else SelectionHandleStyle.HANDLE_TYPE_LEFT
                    val task: DrawCursorTask = DrawCursorTask(
                        centerX,
                        (getRowBottomForBackground(row) - editor.offsetY).toFloat(),
                        type,
                        editor.handleDescLeft!!
                    )
                    postDrawCursor.add(task)
                    applyBidiIndicatorAttrs(task, cursor.leftLine, cursor.leftColumn)
                }
                if (cursor.rightLine == line && isInside(
                        cursor.rightColumn,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        rowInf.isTrailingRow
                    )
                ) {
                    val centerX: Float = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                        cursor.rightLine,
                        cursor.rightColumn
                    )[1] - editor.offsetX
                    val type =
                        if (content!!.isRtlAt(
                                cursor.rightLine,
                                cursor.rightColumn
                            )
                        ) SelectionHandleStyle.HANDLE_TYPE_LEFT else SelectionHandleStyle.HANDLE_TYPE_RIGHT
                    val task: DrawCursorTask = DrawCursorTask(
                        centerX,
                        (getRowBottomForBackground(row) - editor.offsetY).toFloat(),
                        type,
                        editor.handleDescRight!!
                    )
                    postDrawCursor.add(task)
                    applyBidiIndicatorAttrs(task, cursor.rightLine, cursor.rightColumn)
                }
            } else if (cursor.leftLine == line && isInside(
                    cursor.leftColumn,
                    rowInf.startColumn,
                    rowInf.endColumn,
                    rowInf.isTrailingRow
                )
            ) {
                val centerX: Float = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                    cursor.leftLine,
                    cursor.leftColumn
                )[1] - editor.offsetX
                val task: DrawCursorTask = DrawCursorTask(
                    centerX,
                    (getRowBottomForBackground(row) - editor.offsetY).toFloat(),
                    SelectionHandleStyle.HANDLE_TYPE_INSERT,
                    editor.handleDescInsert!!
                )
                postDrawCursor.add(task)
                val c = cursor
                if (c != null) {
                    applyBidiIndicatorAttrs(task, c.leftLine, c.leftColumn)
                }
            }
            // Draw dragging selection or selecting target
            val draggingSelection = editor.touchHandler!!.draggingSelection
            if (draggingSelection != null) {
                if (draggingSelection.line == line && isInside(
                        draggingSelection.column,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        rowInf.isTrailingRow
                    )
                ) {
                    val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
                    val centerX: Float = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                        draggingSelection.line,
                        draggingSelection.column
                    )[1] - editor.offsetX
                    val task: DrawCursorTask = DrawCursorTask(
                        centerX,
                        (getRowBottomForBackground(row) - editor.offsetY).toFloat(),
                        SelectionHandleStyle.HANDLE_TYPE_UNDEFINED,
                        null
                    )
                    postDrawCursor.add(task)
                    val c = cursor
                    if (c != null) {
                        applyBidiIndicatorAttrs(task, draggingSelection.line, draggingSelection.column)
                    }
                }
            } else if (editor.isInMouseMode && editor.isTextSelected) {
                val target =
                    editor.selectingTarget
                if (target != null && target.line == line && isInside(
                        target.column,
                        rowInf.startColumn,
                        rowInf.endColumn,
                        rowInf.isTrailingRow
                    )
                ) {
                    val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
                    val centerX: Float = editor.measureTextRegionOffset() + layout.getCharLayoutOffset(
                        target.line,
                        target.column
                    )[1] - editor.offsetX
                    val task: DrawCursorTask = DrawCursorTask(
                        centerX,
                        (getRowBottomForBackground(row) - editor.offsetY).toFloat(),
                        SelectionHandleStyle.HANDLE_TYPE_UNDEFINED,
                        null
                    )
                    postDrawCursor.add(task)
                    applyBidiIndicatorAttrs(task, target.line, target.column)
                }
            }
            row++
        }

        // Release last used reader object
        if (reader != null) {
            try {
                reader.moveToLine(-1)
            } catch (e: Exception) {
                Log.w(
                    io.github.abc15018045126.sora.widget.EditorRenderer.Companion.LOG_TAG,
                    "Failed to release SpanReader",
                    e
                )
            }
        }

        paintGeneral.isFakeBoldText = false
        paintGeneral.textSkewX = 0f
        paintOther.setStrokeWidth(circleRadius * 2)
        bufferedDrawPoints.commitPoints(canvas, paintOther)
    }

    private fun getBidiIndicatorAttrs(line: Int, column: Int): Long {
        val lineDirections: Directions = getLineDirections(line)
        val count: Int = lineDirections.runCount
        if (count == 1) {
            // Simple LTR/RTL Run
            return IntPair.pack(0, if (lineDirections.isRunRtl(0)) 1 else 0)
        }
        for (i in 0..<count) {
            if (i + 1 == count || lineDirections.getRunStart(i) <= column && column < lineDirections.getRunEnd(i)) {
                return IntPair.pack(
                    if (editor.props!!.showBidiDirectionIndicator) 1 else 0,
                    if (lineDirections.isRunRtl(i)) 1 else 0
                )
            }
        }
        return IntPair.pack(0, 0)
    }

    private fun applyBidiIndicatorAttrs(task: DrawCursorTask, line: Int, column: Int) {
        val bidiAttrs = getBidiIndicatorAttrs(line, column)
        task.isBidiIndicatorRequired = IntPair.getFirst(bidiAttrs) == 1
        task.isRightToLeft = IntPair.getSecond(bidiAttrs) == 1
    }

    private fun drawBidiSelectionIndicator(
        canvas: Canvas,
        x: Float,
        topY: Float,
        selectionHeight: Float,
        isRtl: Boolean
    ) {
        val height = selectionHeight * 0.2f
        val deltaX = height * 0.866f // sqrt(3)/ 2
        tmpPath.reset()
        tmpPath.moveTo(x, topY)
        tmpPath.lineTo(x + (if (isRtl) -deltaX else deltaX), topY + height / 2f)
        tmpPath.lineTo(x, topY + height)
        tmpPath.close()
        canvas.drawPath(tmpPath, paintGeneral)
    }

    protected fun drawDiagnosticIndicator(
        canvas: Canvas,
        style: DiagnosticIndicatorStyle,
        i: Int,
        startX: Float,
        endX: Float
    ) {
        val waveLength: Float = editor.dpUnit * editor.props!!.indicatorWaveLength
        val amplitude: Float = editor.dpUnit * editor.props!!.indicatorWaveAmplitude
        val waveWidth: Float = editor.dpUnit * editor.props!!.indicatorWaveWidth
        // Draw
        val centerY: Float = (editor.getRowBottom(i) - editor.offsetY).toFloat()
        when (style) {
            DiagnosticIndicatorStyle.NONE -> {}
            DiagnosticIndicatorStyle.WAVY_LINE -> {
                var lineWidth = 0f - startX
                var waveCount = ceil((lineWidth / waveLength).toDouble()).toInt()
                val phi = if (lineWidth < 0) 0f else (waveLength * waveCount - lineWidth)
                lineWidth = endX - startX
                canvas.save()
                canvas.clipRect(startX, 0f, endX, canvas.height.toFloat())
                canvas.translate(startX, centerY)
                tmpPath.reset()
                tmpPath.moveTo(0f, 0f)
                waveCount = ceil(((phi + lineWidth) / waveLength).toDouble()).toInt()
                var j = 0
                while (j < waveCount) {
                    tmpPath.quadTo(waveLength * j + waveLength / 4, amplitude, waveLength * j + waveLength / 2, 0f)
                    tmpPath.quadTo(waveLength * j + waveLength * 3 / 4, -amplitude, waveLength * j + waveLength, 0f)
                    j++
                }
                // Draw path
                paintOther.setStrokeWidth(waveWidth)
                paintOther.setStyle(AndroidPaint.Style.STROKE)
                canvas.drawPath(tmpPath, paintOther)
                canvas.restore()
                paintOther.setStyle(AndroidPaint.Style.FILL)
            }

            DiagnosticIndicatorStyle.LINE -> {
                paintOther.setStrokeWidth(waveWidth)
                canvas.drawLine(startX, centerY, endX, centerY, paintOther)
            }

            DiagnosticIndicatorStyle.DOUBLE_LINE -> {
                paintOther.setStrokeWidth(waveWidth / 3f)
                canvas.drawLine(startX, centerY, endX, centerY, paintOther)
                canvas.drawLine(startX, centerY - waveWidth, endX, centerY - waveWidth, paintOther)
            }
            else -> {}
        }
    }

    protected fun drawDiagnosticIndicators(canvas: Canvas, offset: Float) {
        val diagnosticsContainer =
            editor.diagnostics
        val style =
            editor.diagnosticIndicatorStyle
        if (diagnosticsContainer != null && style != DiagnosticIndicatorStyle.NONE && style != null) {
            val text: Content = content!!
            val firstVisRow =
                editor.firstVisibleRow
            val lastVisRow =
                editor.lastVisibleRow
            val firstIndex =
                text.getCharIndex(editor.firstVisibleLine, 0)
            val lastLine =
                kotlin.math.min(text.lineCount - 1, editor.lastVisibleLine + 1)
            val lastIndex =
                text.getCharIndex(lastLine, 0) + text.getColumnCount(lastLine)
            diagnosticsContainer.queryInRegion(collectedDiagnostics, firstIndex, lastIndex)
            if (collectedDiagnostics.isEmpty()) {
                return
            }
            val start: CharPosition = CharPosition()
            val end: CharPosition = CharPosition()
            val localCursor = cursor ?: return
            val indexer =
                localCursor.getIndexer()
            for (region in collectedDiagnostics) {
                val startIndex =
                    max(firstIndex, region.startIndex)
                val endIndex =
                    min(lastIndex, region.endIndex)
                indexer.getCharPosition(startIndex, start)
                indexer.getCharPosition(endIndex, end)
                val startRow =
                    editor.layout!!.getRowIndexForPosition(startIndex)
                val endRow =
                    editor.layout!!.getRowIndexForPosition(endIndex)
                // Setup color
                val severity = region.severity.toInt()
                val colorId =
                    if (severity >= 0 && severity <= 3) sDiagnosticsColorMapping[severity] else 0
                if (colorId == 0) {
                    continue
                }
                paintOther.setColor(editor.colorScheme.getColor(colorId))
                val visStartRow: Int = Math.max(firstVisRow, startRow)
                val visEndRow: Int = Math.min(lastVisRow, endRow)
                for (i in visStartRow..visEndRow) {
                    val row =
                        editor.layout!!.getRowAt(i)
                    updateTextRow(sharedTextRow, i)
                    val startColumn: Int = if (i == startRow) start.column else row.startColumn
                    val endColumn: Int = if (i == endRow) end.column else row.endColumn
                    val finalOffset: Float
                    if (editor.isWordwrap && !row.isLeadingRow && (editor.nonPrintablePaintingFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_SOFT_WRAP) !== 0) {
                        finalOffset = offset + row.renderTranslateX + this.miniGraphWidth
                    } else {
                        finalOffset = offset + row.renderTranslateX
                    }
                    if (startColumn == endColumn) {
                        // Make it always visible
                        val startX =
                            finalOffset + sharedTextRow.getCursorOffsetForIndex(startColumn)
                        val endX =
                            startX + paintGeneral.measureText("a")
                        drawDiagnosticIndicator(canvas, style, i, startX, endX)
                    } else {
                        val rowIndex = i
                        sharedTextRow.iterateBackgroundRegions(startColumn, endColumn, false, false, object : TextRow.BackgroundRegionConsumer {
                            override fun handleRegion(left: Float, right: Float): Boolean {
                                if (right > 0f) drawDiagnosticIndicator(
                                    canvas,
                                    style,
                                    rowIndex,
                                    finalOffset + left,
                                    finalOffset + right
                                )
                                return finalOffset + right < editor.width
                            }
                        })
                    }
                }
            }
        }
        collectedDiagnostics.clear()
    }

    /**
     * Draw non-printable characters
     */
    private fun drawWhitespaces(
        canvas: Canvas,
        tr: TextRow,
        chars: CharArray,
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        isRtl: Boolean,
        horizontalOffset: Float,
        width: Float,
        min: Int,
        max: Int
    ) {
        var paintStart: Int = Math.max(index, Math.min(index + count, min))
        val paintEnd: Int = Math.max(index, Math.min(index + count, max))

        if (paintStart < paintEnd) {
            val spaceWidth: Float = paintGeneral.spaceWidth
            val rowCenter: Float = (editor.logicalRowHeight / 2f + editor.getRowTopOfText(0))
            var offset = if (isRtl) horizontalOffset + width else horizontalOffset
            while (paintStart < paintEnd) {
                val ch = chars[paintStart]
                var paintCount = 0
                var paintLine = false
                if (ch == ' ' || ch == '\t') {
                    val advance: Float = tr.measureAdvanceInRun(
                        paintStart,
                        index,
                        paintStart,
                        contextIndex,
                        contextIndex + contextCount,
                        isRtl
                    )
                    offset = if (isRtl) horizontalOffset + width - advance else horizontalOffset + advance
                }
                if (ch == ' ') {
                    paintCount = 1
                } else if (ch == '\t') {
                    if ((editor.nonPrintablePaintingFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_TAB_SAME_AS_SPACE) !== 0) {
                        paintCount = editor.tabWidth
                    } else {
                        paintLine = true
                    }
                }
                for (i in 0 until paintCount) {
                    val charStartOffset = offset + spaceWidth * i
                    val charEndOffset = charStartOffset + spaceWidth
                    var centerOffset = (charStartOffset + charEndOffset) / 2f
                    if (isRtl) {
                        centerOffset -= spaceWidth
                    }
                    bufferedDrawPoints.drawPoint(centerOffset, rowCenter)
                }
                if (paintLine) {
                    val charWidth =
                        editor.tabWidth * spaceWidth
                    val delta: Float = charWidth * 0.05f
                    val rtlDelta = if (isRtl) -charWidth else 0f
                    canvas.drawLine(
                        offset + delta + rtlDelta,
                        rowCenter,
                        offset + charWidth + rtlDelta - delta,
                        rowCenter,
                        paintOther
                    )
                }

                if (ch == ' ' || ch == '\t') {
                    val charWidth = (if (ch == ' ') spaceWidth else spaceWidth * editor.tabWidth)
                    offset += if (isRtl) -charWidth else charWidth
                }
                paintStart++
            }
        }
    }

    val miniGraphWidth: Float
        get() {
            val height: Float = editor.logicalRowHeight * editor.props!!.miniMarkerSizeFactor
            val graph =
                editor.context.getDrawable(R.drawable.line_break)
            if (graph == null) {
                return 0f
            }
            val w: Int = graph.intrinsicWidth
            val h: Int = graph.intrinsicHeight
            if (w <= 0 || h <= 0 || height <= 0) {
                return 0f
            }
            return height * (w.toFloat() / h)
        }

    /**
     * Draw small characters as graph
     */
    protected fun drawMiniGraph(canvas: Canvas?, offset: Float, row: Int, graph: Drawable?) {
        if (canvas == null) {
            return
        }
        val graphBottom: Float =
            if (row == -1) (editor.getRowBottomOfText(0)).toFloat() else (editor.getRowBottomOfText(row) - editor.offsetY).toFloat()
        val height: Float = editor.logicalRowHeight * editor.props!!.miniMarkerSizeFactor
        if (height <= 0 || graph == null) {
            return
        }
        val w: Int = graph.intrinsicWidth
        val h: Int = graph.intrinsicHeight
        if (w <= 0 || h <= 0) {
            return
        }
        val width = height * (w.toFloat() / h)
        graph.setColorFilter(
            editor.colorScheme.getColor(EditorColorScheme.NON_PRINTABLE_CHAR),
            PorterDuff.Mode.SRC_ATOP
        )
        graph.setBounds(offset.toInt(), (graphBottom - height).toInt(), (offset + width).toInt(), graphBottom.toInt())
        graph.draw(canvas)
    }

    protected fun getRowTopForBackground(row: Int): Int {
        if (!editor.props!!.textBackgroundWrapTextOnly) {
            return editor.getRowTop(row)
        } else {
            return editor.getRowTopOfText(row)
        }
    }

    protected fun getRowBottomForBackground(row: Int): Int {
        if (!editor.props!!.textBackgroundWrapTextOnly) {
            return editor.getRowBottom(row)
        } else {
            return editor.getRowBottomOfText(row)
        }
    }

    /**
     * Draw background of a text region
     * 
     * @param canvas         Canvas to draw
     * @param row            The row index
     * @param highlightStart Region start
     * @param highlightEnd   Region end
     * @param color          Color of background
     */
    protected fun drawRowRegionBackground(
        @NonNull canvas: Canvas,
        row: Int,
        @Nullable tr: TextRow?,
        highlightStart: Int,
        highlightEnd: Int,
        rowStart: Int,
        rowEnd: Int,
        color: Int,
        borderColor: Int
    ) {
        var tr: TextRow? = tr
        var highlightStart = highlightStart
        var highlightEnd = highlightEnd
        highlightStart = Math.max(highlightStart, rowStart)
        highlightEnd = Math.min(highlightEnd, rowEnd)
        if (highlightStart < highlightEnd) {
            tmpRect.top = (getRowTopForBackground(row) - editor.offsetY).toFloat()
            tmpRect.bottom = (getRowBottomForBackground(row) - editor.offsetY).toFloat()
            var offset: Float = editor.measureTextRegionOffset() - editor.offsetX
            if (editor.isWordwrap && !editor.layout!!
                    .getRowAt(row).isLeadingRow && (editor.nonPrintablePaintingFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_SOFT_WRAP) !== 0
            ) {
                offset += this.miniGraphWidth
            }
            val finalOffset = offset
            if (tr == null) {
                tr = createTextRow(row)
            }
            val width =
                editor.width
            tr.iterateBackgroundRegions(highlightStart, highlightEnd, false, false, object : TextRow.BackgroundRegionConsumer {
                override fun handleRegion(left: Float, right: Float): Boolean {
                    tmpRect.left = finalOffset + left
                    tmpRect.right = finalOffset + right
                    if (tmpRect.right < 0 || tmpRect.left > width) {
                        return false
                    }
                    drawRowBackgroundRectWithBorder(canvas, tmpRect, color, borderColor)
                    return true
                }
            })
        }
    }

    protected fun drawRowBackgroundRectWithBorder(
        canvas: Canvas,
        rect: RectF?,
        backgroundColor: Int,
        borderColor: Int
    ) {
        paintGeneral.setColor(backgroundColor)
        drawRowBackgroundRect(canvas, rect)
        if (borderColor == 0) {
            return
        }
        paintGeneral.setColor(borderColor)
        paintGeneral.setStyle(android.graphics.Paint.Style.STROKE)
        paintGeneral.setStrokeWidth(editor.getTextBorderWidth())
        drawRowBackgroundRect(canvas, rect)
        paintGeneral.setStyle(android.graphics.Paint.Style.FILL)
    }

    protected fun drawRowBackgroundRect(canvas: Canvas, rect: RectF?) {
        drawRowBackgroundRect(canvas, rect, paintGeneral)
    }

    protected fun drawRowBackgroundRect(canvas: Canvas, rect: RectF?, p: Paint?) {
        if (rect == null || p == null) {
            return
        }
        if (editor.props!!.enableRoundTextBackground) {
            canvas.drawRoundRect(
                rect,
                editor.logicalRowHeight * editor.props!!.roundTextBackgroundFactor,
                editor.logicalRowHeight * editor.props!!.roundTextBackgroundFactor,
                p
            )
        } else {
            canvas.drawRect(rect, p)
        }
    }

    /**
     * Is inside the region
     * 
     * @param index Index to test
     * @param start Start of region
     * @param end   End of region
     * @return true if cursor should be drawn in this row
     */
    private fun isInside(index: Int, start: Int, end: Int, isLastRow: Boolean): Boolean {
        // Due not to draw duplicate cursors for a single one
        if (index == end && !isLastRow) {
            return false
        }
        return index >= start && index <= end
    }

    val lineNumberMetrics: android.graphics.Paint.FontMetricsInt
        get() = metricsLineNumber

    val textMetrics: android.graphics.Paint.FontMetricsInt?
        get() = metricsText

    /**
     * Draw effect of edges
     * 
     * @param canvas The canvas to draw
     */
    protected fun drawEdgeEffect(canvas: Canvas) {
        var postDraw = false
        val verticalEdgeEffect =
            editor.verticalEdgeEffect!!
        val horizontalEdgeEffect =
            editor.horizontalEdgeEffect!!
        if (!verticalEdgeEffect.isFinished) {
            val bottom: Boolean = editor.touchHandler!!.glowTopOrBottom
            if (bottom) {
                canvas.save()
                canvas.translate(-editor.measuredWidth.toFloat(), editor.measuredHeight.toFloat())
                canvas.rotate(180f, editor.measuredWidth.toFloat(), 0f)
            }
            postDraw = verticalEdgeEffect.draw(canvas)
            if (bottom) {
                canvas.restore()
            }
        }
        if (editor.isWordwrap) {
            horizontalEdgeEffect.finish()
        }
        if (!horizontalEdgeEffect.isFinished) {
            canvas.save()
            val right: Boolean = editor.touchHandler!!.glowLeftOrRight
            if (right) {
                canvas.rotate(90f)
                canvas.translate(0f, -editor.measuredWidth.toFloat())
            } else {
                canvas.translate(0f, editor.measuredHeight.toFloat())
                canvas.rotate(-90f)
            }
            postDraw = horizontalEdgeEffect.draw(canvas) || postDraw
            canvas.restore()
        }
        val scroller =
            editor.scroller
        if (scroller.isOverScrolled()) {
            if (verticalEdgeEffect.isFinished && (scroller.getCurrY() < 0 || scroller.getCurrY() > editor.scrollMaxY)) {
                editor.eventHandler!!.glowTopOrBottom = scroller.getCurrY() >= editor.scrollMaxY
                verticalEdgeEffect.onAbsorb(scroller.getCurrVelocity().toInt())
                postDraw = true
            }
            if (horizontalEdgeEffect.isFinished && (scroller.getCurrX() < 0 || scroller.getCurrX() > editor.scrollMaxX)) {
                editor.eventHandler!!.glowLeftOrRight = scroller.getCurrX() >= editor.scrollMaxX
                horizontalEdgeEffect.onAbsorb(scroller.getCurrVelocity().toInt())
                postDraw = true
            }
        }
        if (postDraw) {
            editor.postInvalidate()
        }
    }

    /**
     * Draw code block lines on screen
     * 
     * @param canvas  The canvas to draw
     * @param offsetX The start x offset for text
     */
    protected fun drawBlockLines(canvas: Canvas?, offsetX: Float) {
        if (canvas == null) {
            return
        }
        val styles = editor.styles
        val blocks: List<CodeBlock?>? = if (styles == null) null else styles.blocks
        val indentMode = styles != null && styles.isIndentCountMode()
        if (blocks == null || blocks.isEmpty()) {
            return
        }
        val firstLine: Int = editor.firstVisibleLine
        val lastLine: Int = editor.lastVisibleLine
        var mark = false
        var invalidCount = 0
        val maxCount: Int = styles!!.getSuppressSwitch()
        var mm: Int = editor.binarySearchEndBlock(firstLine, blocks as List<CodeBlock>)
        if (mm == -1) {
            mm = 0
        }
        val cursorIdx: Int = editor.blockIndex
        for (curr in mm until blocks.size) {
            val block: CodeBlock? = blocks[curr]
            if (block == null) {
                continue
            }
            if (io.github.abc15018045126.sora.widget.CodeEditor.hasVisibleRegion(block.startLine, block.endLine, firstLine, lastLine)) {
                try {
                    var lineContent: ContentLine = getLine(block.endLine)
                    val offsetEnd: Float =
                        if (indentMode) paintGeneral.spaceWidth * block.endColumn else createTextRow(block.endLine).getCursorOffsetForIndex(
                            Math.min(block.endColumn, lineContent.length)
                        )
                    lineContent = getLine(block.startLine)
                    val offsetStart: Float =
                        if (indentMode) paintGeneral.spaceWidth * block.startColumn else createTextRow(block.startLine).getCursorOffsetForIndex(
                            Math.min(block.startColumn, lineContent.length)
                        )
                    val offset: Float = min(offsetEnd, offsetStart)
                    val centerX = offset + offsetX
                    tmpRect.top = max(0f, (editor.getRowBottom(block.startLine) - editor.offsetY).toFloat())
                    tmpRect.bottom = min(
                        editor.height.toFloat(),
                        ((if (block.toBottomOfEndLine) editor.getRowBottom(block.endLine) else editor.getRowTop(block.endLine)) - editor.offsetY).toFloat()
                    )
                    tmpRect.left = centerX - editor.dpUnit * editor.getBlockLineWidth() / 2
                    tmpRect.right = centerX + editor.dpUnit * editor.getBlockLineWidth() / 2
                    drawColor(
                        canvas,
                        editor.colorScheme
                            .getColor(if (curr == cursorIdx) EditorColorScheme.BLOCK_LINE_CURRENT else EditorColorScheme.BLOCK_LINE),
                        tmpRect
                    )
                } catch (e: IndexOutOfBoundsException) {
                    // Ignored
                    // Because the exception usually occurs when the content is changed.
                }
                mark = true
            } else if (mark) {
                if (invalidCount >= maxCount) break
                invalidCount++
            }
        }
    }

    protected fun drawSideBlockLine(canvas: Canvas) {
        if (!editor.props!!.drawSideBlockLine) {
            return
        }
        val styles = editor.styles
        val blocks: List<CodeBlock?>? = styles?.blocks
        if (blocks == null || blocks.isEmpty()) {
            return
        }
        val current =
            editor.blockIndex
        if (current >= 0 && current < blocks.size) {
            val block =
                blocks[current]
            if (block != null) {
                val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
                try {
                    val top: Float = layout.getCharLayoutOffset(
                        block.startLine,
                        block.startColumn
                    )[0] - editor.logicalRowHeight - editor.offsetY
                    val bottom: Float = layout.getCharLayoutOffset(block.endLine, block.endColumn)[0] - editor.offsetY
                    val left: Float = editor.measureLineNumber()
                    val right: Float = left + editor.dividerMarginLeft
                    val center: Float = (left + right) / 2 - editor.offsetX
                    paintGeneral.setColor(editor.colorScheme.getColor(EditorColorScheme.SIDE_BLOCK_LINE))
                    paintGeneral.setStrokeWidth(editor.dpUnit * editor.getBlockLineWidth())
                    canvas.drawLine(center, top, center, bottom, paintGeneral)
                } catch (e: IndexOutOfBoundsException) {
                    //ignored
                }
            }
        }
    }

    /**
     * Draw scroll bars and tracks
     * 
     * @param canvas The canvas to draw
     */
    protected fun drawScrollBars(canvas: Canvas) {
        verticalScrollBarRect.setEmpty()
        horizontalScrollBarRect.setEmpty()
        val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
        if (!handler.shouldDrawScrollBarForTouch() && !(editor.isInMouseMode && editor.props!!.mouseModeAlwaysShowScrollbars)) {
            return
        }
        var percentage =
            handler.getScrollBarFadeOutPercentageForTouch()
        if (editor.isInMouseMode && editor.props!!.mouseModeAlwaysShowScrollbars) {
            percentage = 0f
        }
        val size =
            editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
        if (editor.isHorizontalScrollBarEnabled() && !editor.isWordwrap && editor.scrollMaxX > editor.width * 3 / 4) {
            canvas.save()
            canvas.translate(0f, size * percentage)

            drawScrollBarTrackHorizontal(canvas)
            drawScrollBarHorizontal(canvas)

            canvas.restore()
        }
        if (editor.isVerticalScrollBarEnabled() && editor.scrollMaxY > editor.height / 2) {
            canvas.save()
            canvas.translate(size * percentage, 0f)

            drawScrollBarTrackVertical(canvas)
            drawScrollBarVertical(canvas)

            canvas.restore()
        }
    }

    /**
     * Draw vertical scroll bar track
     * 
     * @param canvas Canvas to draw
     */
    protected fun drawScrollBarTrackVertical(canvas: Canvas?) {
        if (canvas == null) {
            return
        }
        val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
        if (handler.holdVerticalScrollBar()) {
            tmpRect.right = editor.width.toFloat()
            tmpRect.left = editor.width - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
            tmpRect.top = 0f
            tmpRect.bottom = editor.height.toFloat()
            val track = verticalScrollbarTrackDrawable
            if (track != null) {
                track.setBounds(
                    tmpRect.left.toInt(),
                    tmpRect.top.toInt(),
                    tmpRect.right.toInt(),
                    tmpRect.bottom.toInt()
                )
                track.draw(canvas)
            } else {
                drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.SCROLL_BAR_TRACK), tmpRect)
            }
        }
    }

    /**
     * Draw vertical scroll bar
     * 
     * @param canvas Canvas to draw
     */
    protected fun drawScrollBarVertical(canvas: Canvas) {
        val height: Int = editor.height
        val all: Float = (editor.scrollMaxY + height).toFloat()
        val length: Float =
            max(height / all * height, editor.dpUnit * RenderingConstants.SCROLLBAR_LENGTH_MIN_DIP)
        val topY: Float = editor.offsetY * 1.0f / editor.scrollMaxY * (height - length)
        val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
        if (handler.holdVerticalScrollBar()) {
            drawLineInfoPanel(canvas, topY, length)
        }
        tmpRect.right = editor.width.toFloat()
        tmpRect.left = editor.width - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
        tmpRect.top = topY
        tmpRect.bottom = topY + length
        verticalScrollBarRect.set(tmpRect)
        val thumb = verticalScrollbarThumbDrawable
        if (thumb != null) {
            thumb.setState(
                if (handler
                        .holdVerticalScrollBar()
                ) PRESSED_DRAWABLE_STATE else DEFAULT_DRAWABLE_STATE
            )
            thumb.setBounds(
                tmpRect.left.toInt(),
                tmpRect.top.toInt(),
                tmpRect.right.toInt(),
                tmpRect.bottom.toInt()
            )
            thumb.draw(canvas)
        } else {
            drawColor(
                canvas,
                editor.colorScheme.getColor(
                    if (handler
                            .holdVerticalScrollBar()
                    ) EditorColorScheme.SCROLL_BAR_THUMB_PRESSED else EditorColorScheme.SCROLL_BAR_THUMB
                ),
                tmpRect
            )
        }
    }

    /**
     * Draw line number panel
     * 
     * @param canvas Canvas to draw
     * @param topY   The y at the top of the vertical scrollbar
     * @param length The length of vertical scrollbar
     */
    protected fun drawLineInfoPanel(canvas: Canvas, topY: Float, length: Float) {
        if (!editor.isDisplayLnPanel) {
            return
        }
        val mode: Int = editor.lnPanelPositionMode
        val position: Int = editor.lnPanelPosition
        val text: String? = editor.getLineNumberTipTextProvider()!!.getCurrentText(editor)
        val backupSize: Float = paintGeneral.getTextSize()
        paintGeneral.setTextSize(editor.lineInfoTextSize)
        val backupMetrics: AndroidPaint.FontMetricsInt? = metricsText
        metricsText = paintGeneral.getFontMetricsInt()
        val expand: Float = editor.dpUnit * 8
        val textWidth: Float = paintGeneral.measureText(text)
        var baseline: Float
        var textOffset = 0f
        if (mode == LineInfoPanelPositionMode.FIXED) {
            tmpRect.top = editor.height / 2f - editor.logicalRowHeight / 2f - expand
            tmpRect.bottom = editor.height / 2f + editor.logicalRowHeight / 2f + expand
            tmpRect.left = editor.width / 2f - textWidth / 2f - expand
            tmpRect.right = editor.width / 2f + textWidth / 2f + expand
            baseline = editor.height / 2f + 2 * expand
            val offset: Float = 10 * editor.dpUnit
            if (position != LineInfoPanelPosition.CENTER) {
                if ((position or LineInfoPanelPosition.TOP) == position) {
                    tmpRect.top = offset
                    tmpRect.bottom = offset + editor.logicalRowHeight + 2 * expand
                    baseline = offset + editor.getRowBaseline(0) + expand
                }
                if ((position or LineInfoPanelPosition.BOTTOM) == position) {
                    tmpRect.top = editor.height - offset - 2 * expand - editor.logicalRowHeight
                    tmpRect.bottom = editor.height - offset
                    baseline = editor.height - editor.logicalRowHeight + editor.getRowBaseline(0) - offset - expand
                }
                if ((position or LineInfoPanelPosition.LEFT) == position) {
                    tmpRect.left = offset
                    tmpRect.right = offset + 2 * expand + textWidth
                }
                if ((position or LineInfoPanelPosition.RIGHT) == position) {
                    tmpRect.right = editor.width - offset
                    tmpRect.left = editor.width - offset - expand * 2 - textWidth
                }
            }
            drawColorRound(canvas, editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_PANEL), tmpRect)
        } else {
            var radii: FloatArray? = null
            tmpRect.right = editor.width - 30 * editor.dpUnit
            tmpRect.left = editor.width - 30 * editor.dpUnit - expand * 2 - textWidth
            if (position == LineInfoPanelPosition.TOP) {
                tmpRect.top = topY
                tmpRect.bottom = topY + editor.logicalRowHeight + 2 * expand
                baseline = topY + editor.getRowBaseline(0) + expand
                radii = FloatArray(8)
                for (i in 0..7) {
                    if (i != 5) radii[i] = tmpRect.height() * RenderingConstants.ROUND_BUBBLE_FACTOR
                }
            } else if (position == LineInfoPanelPosition.BOTTOM) {
                tmpRect.top = topY + length - editor.logicalRowHeight - 2 * expand
                tmpRect.bottom = topY + length
                baseline = topY + length - editor.getRowBaseline(0) / 2f
                radii = FloatArray(8)
                for (i in 0..7) {
                    if (i != 3) radii[i] = tmpRect.height() * RenderingConstants.ROUND_BUBBLE_FACTOR
                }
            } else {
                val centerY = topY + length / 2f
                tmpRect.top = centerY - editor.logicalRowHeight / 2f - expand
                tmpRect.bottom = centerY + editor.logicalRowHeight / 2f + expand
                baseline = centerY - editor.logicalRowHeight / 2f + editor.getRowBaseline(0)
            }
            if (radii != null) {
                tmpPath.reset()
                tmpPath.addRoundRect(tmpRect, radii, Path.Direction.CW)
            } else {
                tmpRect.offset(-expand, 0f)
                tmpRect.right += expand
                textOffset = -expand / 2f
                BubbleHelper.buildBubblePath(tmpPath, tmpRect)
            }
            paintGeneral.setColor(editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_PANEL))
            canvas.drawPath(tmpPath, paintGeneral)
        }
        val centerX: Float = (tmpRect.left + tmpRect.right) / 2 + textOffset
        paintGeneral.setColor(editor.colorScheme.getColor(EditorColorScheme.LINE_NUMBER_PANEL_TEXT))
        paintGeneral.textAlign = AndroidPaint.Align.CENTER
        if (text != null) {
            canvas.drawText(text, centerX, baseline, paintGeneral)
        }
        paintGeneral.textAlign = AndroidPaint.Align.LEFT
        paintGeneral.setTextSize(backupSize)
        metricsText = backupMetrics
    }

    /**
     * Draw horizontal scroll bar track
     * 
     * @param canvas Canvas to draw
     */
    protected fun drawScrollBarTrackHorizontal(canvas: Canvas?) {
        if (canvas == null) {
            return
        }
        val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
        if (handler.holdHorizontalScrollBar()) {
            tmpRect.set(
                0f,
                editor.height - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP,
                editor.width.toFloat(),
                editor.height.toFloat()
            )
            val track = horizontalScrollbarTrackDrawable
            if (track != null) {
                track.setBounds(
                    tmpRect.left.toInt(),
                    tmpRect.top.toInt(),
                    tmpRect.right.toInt(),
                    tmpRect.bottom.toInt()
                )
                track.draw(canvas)
            } else {
                drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.SCROLL_BAR_TRACK), tmpRect)
            }
        }
    }

    protected fun patchSnippetRegions(canvas: Canvas, textOffset: Float) {
        val controller =
            editor.snippetController!!
        if (controller.isInSnippet()) {
            val editing =
                controller.getEditingTabStop()
            if (editing != null) {
                patchTextRegionWithColor(
                    canvas,
                    textOffset,
                    editing.startIndex,
                    editing.endIndex,
                    0,
                    editor.colorScheme.getColor(EditorColorScheme.SNIPPET_BACKGROUND_EDITING),
                    0
                )
            }
            for (snippetItem in controller.getEditingRelatedTabStops()) {
                patchTextRegionWithColor(
                    canvas,
                    textOffset,
                    snippetItem.startIndex,
                    snippetItem.endIndex,
                    0,
                    editor.colorScheme.getColor(EditorColorScheme.SNIPPET_BACKGROUND_RELATED),
                    0
                )
            }
            for (snippetItem in controller.getInactiveTabStops()) {
                patchTextRegionWithColor(
                    canvas,
                    textOffset,
                    snippetItem.startIndex,
                    snippetItem.endIndex,
                    0,
                    editor.colorScheme.getColor(EditorColorScheme.SNIPPET_BACKGROUND_INACTIVE),
                    0
                )
            }
        }
    }

    protected fun patchHighlightedDelimiters(canvas: Canvas, textOffset: Float) {
        if (true) return
        val paired = object {
            val leftIndex = 0
            val leftLength = 0
            val rightIndex = 0
            val rightLength = 0
        }
        if (paired != null) {
            val color =
                editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND)
            var backgroundColor =
                editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND)
            val underlineColor =
                editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_UNDERLINE)
            val borderColor =
                editor.colorScheme.getColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_BORDER)
            val borderWidth =
                editor.getTextBorderWidth()
            if (isInvalidTextBounds(paired.leftIndex, paired.leftLength) || isInvalidTextBounds(
                    paired.rightIndex,
                    paired.rightLength
                )
            ) {
                // Index out of bounds
                return
            }

            val continuous = paired.leftIndex + paired.leftLength == paired.rightIndex
            if (color != 0 || underlineColor != 0) {
                if (continuous) {
                    patchTextRegionWithColor(
                        canvas,
                        textOffset,
                        paired.leftIndex,
                        paired.rightIndex + paired.rightLength,
                        color,
                        backgroundColor,
                        underlineColor
                    )
                } else {
                    patchTextRegionWithColor(
                        canvas,
                        textOffset,
                        paired.leftIndex,
                        paired.leftIndex + paired.leftLength,
                        color,
                        backgroundColor,
                        underlineColor
                    )
                    patchTextRegionWithColor(
                        canvas,
                        textOffset,
                        paired.rightIndex,
                        paired.rightIndex + paired.rightLength,
                        color,
                        backgroundColor,
                        underlineColor
                    )
                }
                backgroundColor = 0
            }
            if (backgroundColor != 0 || (borderColor != 0 && borderWidth > 0)) {
                if (continuous) {
                    patchTextBackgroundRegions(
                        canvas,
                        textOffset,
                        paired.leftIndex,
                        paired.rightIndex + paired.rightLength,
                        backgroundColor,
                        borderWidth,
                        borderColor
                    )
                } else {
                    patchTextBackgroundRegions(
                        canvas,
                        textOffset,
                        paired.leftIndex,
                        paired.leftIndex + paired.leftLength,
                        backgroundColor,
                        borderWidth,
                        borderColor
                    )
                    patchTextBackgroundRegions(
                        canvas,
                        textOffset,
                        paired.rightIndex,
                        paired.rightIndex + paired.rightLength,
                        backgroundColor,
                        borderWidth,
                        borderColor
                    )
                }
            }
        }
    }

    protected fun isInvalidTextBounds(index: Int, length: Int): Boolean {
        return (index < 0 || length < 0 || index + length > content!!.length)
    }

    protected fun patchTextRegionWithColor(
        canvas: Canvas,
        textOffset: Float,
        start: Int,
        end: Int,
        color: Int,
        backgroundColor: Int,
        underlineColor: Int
    ) {
        paintGeneral.setColor(color)
        paintOther.setStrokeWidth(editor.logicalRowHeight * RenderingConstants.MATCHING_DELIMITERS_UNDERLINE_WIDTH_FACTOR)

        val useBoldStyle =
            editor.props!!.boldMatchingDelimiters
        paintGeneral.setStyle(if (useBoldStyle) AndroidPaint.Style.FILL_AND_STROKE else AndroidPaint.Style.FILL)
        paintGeneral.setFakeBoldText(useBoldStyle)

        patchTextRegions(
            canvas,
            textOffset,
            start,
            end,
            object : TextRow.DrawTextConsumer {
                override fun drawText(canvasLocal: Canvas?, text: CharArray?, index: Int, count: Int, contextIndex: Int, contextCount: Int, isRtl: Boolean, horizontalOffset: Float, width: Float, params: TextRowParams?, span: Span?) {
            if (span == null) {
                return
            }
            if (backgroundColor != 0) {
                tmpRect.top = getRowTopForBackground(0).toFloat()
                tmpRect.bottom = getRowBottomForBackground(0).toFloat()
                tmpRect.left = horizontalOffset
                tmpRect.right = horizontalOffset + width
                paintOther.setColor(backgroundColor)
                drawRowBackgroundRect(canvas, tmpRect, paintOther)
            }
            val style: Long = span!!.style
            if (color != 0) {
                paintGeneral.setTextSkewX(if (TextStyle.isItalics(style)) RenderingConstants.TEXT_SKEW_X else 0f)
                paintGeneral.setStrikeThruText(TextStyle.isStrikeThrough(style))
                GraphicsCompat.drawTextRun(
                    canvas,
                    text!!,
                    index,
                    count,
                    contextIndex,
                    contextCount,
                    horizontalOffset,
                    params!!.textBaseline.toFloat(),
                    isRtl,
                    paintGeneral
                )
            }
            if (underlineColor != 0) {
                paintOther.setColor(underlineColor)
                val bottom =
                    params!!.textBottom - params.textHeight * 0.05f
                canvas.drawLine(horizontalOffset, bottom, horizontalOffset + width, bottom, paintOther)
            }
            }}, null)
        paintGeneral.setStyle(AndroidPaint.Style.FILL)
        paintGeneral.setFakeBoldText(false)
        paintGeneral.setTextSkewX(0f)
        paintGeneral.setStrikeThruText(false)
    }

    protected fun patchTextBackgroundRegions(
        canvas: Canvas,
        textOffset: Float,
        start: Int,
        end: Int,
        backgroundColor: Int,
        borderWidth: Float,
        borderColor: Int
    ) {
        if (backgroundColor == 0 && (borderWidth <= 0 || borderColor == 0)) {
            return
        }
        patchTextRegions(
            canvas,
            textOffset,
            start,
            end,
            null,
            object : TextRow.BackgroundRegionConsumer {
                override fun handleRegion(left: Float, right: Float): Boolean {
                    if (textOffset + left < 0) {
                        return true
                    }
                    tmpRect.top = getRowTopForBackground(0).toFloat()
                    tmpRect.bottom = getRowBottomForBackground(0).toFloat()
                    tmpRect.left = left
                    tmpRect.right = right
                    if (backgroundColor != 0) {
                        paintOther.setColor(backgroundColor)
                        drawRowBackgroundRect(canvas, tmpRect, paintOther)
                    }
                    if (borderWidth > 0 && borderColor != 0) {
                        paintOther.setStyle(AndroidPaint.Style.STROKE)
                        paintOther.setColor(borderColor)
                        paintOther.setStrokeWidth(borderWidth)
                        drawRowBackgroundRect(canvas, tmpRect, paintOther)
                        paintOther.setStyle(AndroidPaint.Style.FILL)
                    }
                    return textOffset + right > editor.width
                }
            })
    }


    protected fun patchTextRegions(
        canvas: Canvas, textOffset: Float, start: Int, end: Int,
        @Nullable patch: TextRow.DrawTextConsumer?,
        @Nullable bgPatch: TextRow.BackgroundRegionConsumer?
    ) {
        if (patch == null && bgPatch == null) {
            return
        }
        val firstVisRow =
            editor.firstVisibleRow
        val lastVisRow =
            editor.lastVisibleRow

        val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
        val startRow =
            layout.getRowIndexForPosition(start)
        val endRow =
            layout.getRowIndexForPosition(end)
        val cursor = this.cursor ?: return
        val posStart =
            cursor.getIndexer().getCharPosition(start)
        val posEnd =
            cursor.getIndexer().getCharPosition(end)
        val itr =
            layout.obtainRowIterator(startRow, preloadedLines as SparseArray<ContentLine>)
        var i: Int = startRow
        while (i <= endRow && itr.hasNext()) {
            val row = itr.next()
            if (!(firstVisRow <= i && i <= lastVisRow)) {
                i++
                continue
            }
            val startOnRow =
                (if (i == startRow) posStart.column else row.startColumn)
            val endOnRow =
                (if (i == endRow) posEnd.column else row.endColumn)
            val tr: TextRow = createTextRow(i)
            var horizontalOffset = textOffset
            if ((editor.nonPrintablePaintingFlags and io.github.abc15018045126.sora.widget.CodeEditor.Companion.FLAG_DRAW_SOFT_WRAP) != 0 && !row.isLeadingRow) {
                horizontalOffset += this.miniGraphWidth
            }
            val minHorizontalOffset: Float = max(0f, -horizontalOffset)
            val maxHorizontalOffset: Float = minHorizontalOffset + editor.width
            canvas.save()
            canvas.translate(horizontalOffset + row.renderTranslateX, (editor.getRowTop(i) - editor.offsetY).toFloat())
            if (bgPatch != null) {
                tr.iterateBackgroundRegions(startOnRow, endOnRow, false, false, bgPatch)
            }
            if (patch != null) {
                tr.iterateDrawTextRegions(
                    startOnRow,
                    endOnRow,
                    canvas,
                    minHorizontalOffset,
                    maxHorizontalOffset,
                    true,
                    patch
                )
            }
            canvas.restore()
            i++
        }
    }

    protected fun drawSelectionOnAnimation(canvas: Canvas) {
        if (!editor.isEditable) {
            return
        }
        tmpRect.bottom = editor.cursorAnimator.animatedY() - editor.offsetY
        tmpRect.top =
            tmpRect.bottom - (if (editor.props!!.textBackgroundWrapTextOnly) editor.logicalRowHeight else editor.logicalRowHeight)
        val centerX: Float = editor.cursorAnimator.animatedX() - editor.offsetX
        tmpRect.left = centerX - editor.insertSelectionWidth / 2
        tmpRect.right = centerX + editor.insertSelectionWidth / 2
        drawColor(canvas, editor.colorScheme.getColor(EditorColorScheme.SELECTION_INSERT), tmpRect)
        val bidiAttrs = getBidiIndicatorAttrs(cursor!!.leftLine, cursor!!.leftColumn)
        if (IntPair.getFirst(bidiAttrs) == 1) {
            drawBidiSelectionIndicator(
                canvas,
                centerX,
                tmpRect.top,
                tmpRect.height(),
                IntPair.getSecond(bidiAttrs) == 1
            )
        }
        val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
        if (handler.shouldDrawInsertHandle() && !editor.isInMouseMode) {
            editor.handleStyle!!.draw(
                canvas,
                SelectionHandleStyle.HANDLE_TYPE_INSERT,
                centerX,
                tmpRect.bottom,
                editor.logicalRowHeight,
                editor.colorScheme.getColor(EditorColorScheme.SELECTION_HANDLE),
                editor.handleDescInsert!!
            )
        }
    }

    /**
     * Draw horizontal scroll bar
     * 
     * @param canvas Canvas to draw
     */
    protected fun drawScrollBarHorizontal(canvas: Canvas?) {
        val page: Int = editor.width
        val all: Float = editor.scrollMaxX.toFloat()
        var length: Float = page / (all + editor.width) * editor.width
        val minLength: Float = RenderingConstants.SCROLLBAR_WIDTH_DIP * editor.dpUnit
        if (length <= minLength) length = minLength
        val leftX: Float = editor.offsetX / all * (editor.width - length)
        tmpRect.top = editor.height - editor.dpUnit * RenderingConstants.SCROLLBAR_WIDTH_DIP
        tmpRect.bottom = editor.height.toFloat()
        tmpRect.right = (leftX + length).toFloat()
        tmpRect.left = leftX
        horizontalScrollBarRect.set(tmpRect)
        val thumb = horizontalScrollbarThumbDrawable
        val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
        if (thumb != null && canvas != null) {
            thumb.setState(
                if (handler
                        .holdHorizontalScrollBar()
                ) PRESSED_DRAWABLE_STATE else DEFAULT_DRAWABLE_STATE
            )
            thumb.setBounds(
                tmpRect.left.toInt(),
                tmpRect.top.toInt(),
                tmpRect.right.toInt(),
                tmpRect.bottom.toInt()
            )
            thumb.draw(canvas)
        } else if (canvas != null) {
            drawColor(
                canvas,
                editor.colorScheme.getColor(
                    if (handler
                            .holdHorizontalScrollBar()
                    ) EditorColorScheme.SCROLL_BAR_THUMB_PRESSED else EditorColorScheme.SCROLL_BAR_THUMB
                ),
                tmpRect
            )
        }
    }

    // BEGIN Measure-------------------------------------
    /**
     * Build measure cache for the given lines, if the timestamp indicates that it is outdated.
     */
    @JvmOverloads
    fun buildMeasureCacheForLines(
        startLine: Int,
        endLine: Int,
        timestamp: Long = this.timestamp,
        useCachedContent: Boolean = false
    ) {
        var startLine = startLine
        val text: Content = content!!
        val context =
            editor.renderContext!!
        while (startLine <= endLine && startLine < text.lineCount) {
            val line: ContentLine = if (useCachedContent) getLine(startLine) else getLineDirect(startLine)
            val cache =
                editor.renderContext!!.cache.getOrCreateMeasureCache(startLine)
            if (cache.updateTimestamp < timestamp) {
                var forced = false
                val w = cache.widths
                if (w == null || w.size < line.length) {
                    cache.widths = TextAdvancesCache(max(line.length + 8, 90))
                    forced = true
                }
                val spans =
                    editor.getSpansForLine(startLine)
                val hash =
                    Objects.hash(
                        spans?.filterNotNull(),
                        line.length,
                        editor.tabWidth,
                        paintGeneral.getFlags(),
                        paintGeneral.getTextSize(),
                        paintGeneral.getTextScaleX(),
                        paintGeneral.letterSpacing,
                        paintGeneral.fontFeatureSettings,
                        paintGeneral.typeface?.hashCode() ?: 0
                    )
                if (context.cache.getStyleHash(startLine) != hash || forced) {
                    context.cache.setStyleHash(startLine, hash)
                    // Build cache here
                    val layout: io.github.abc15018045126.sora.widget.layout.Layout = editor.layout!!
                    val beginRowIndex =
                        layout.getRowIndexForPosition(text.getCharIndex(startLine, 0))
                    val itr =
                        layout.obtainRowIterator(beginRowIndex)
                    val tr: TextRow = TextRow()
                    val lineText =
                        text.getLine(startLine)
                    val directions =
                        text.getLineDirections(startLine)
                    val requiredSize: Int = lineText.length + 10
                    var widths =
                        cache.widths
                    if (widths == null || widths.size < requiredSize) {
                        widths = TextAdvancesCache(requiredSize)
                        cache.widths = widths
                    }
                    while (itr.hasNext()) {
                        val row =
                            itr.next()
                        if (row.lineIndex != startLine) {
                            break
                        }
                        tr.set(
                            lineText,
                            row.startColumn,
                            row.endColumn,
                            spans?.filterNotNull(),
                            row.inlayHints,
                            directions,
                            paintGeneral,
                            null,
                            createTextRowParams()!!
                        )
                        tr.buildMeasureCacheStep(widths)
                    }
                    tr.setRange(0, lineText.length)
                    tr.buildMeasureCacheTailor(widths)
                    cache.updateTimestamp = timestamp
                }
            }
            startLine++
        }
    }

    internal fun getRowWidth(row: Int): Float {
        return createTextRow(row).computeRowWidth()
    }


    // END Measure---------------------------------------
    protected inner class DrawCursorTask(
        protected var x: Float,
        protected var y: Float,
        protected var handleType: Int,
        descriptor: SelectionHandleStyle.HandleDescriptor?
    ) {
        protected var descriptor: SelectionHandleStyle.HandleDescriptor?
        var isBidiIndicatorRequired: Boolean = false
        var isRightToLeft: Boolean = false


        init {
            this.descriptor = descriptor
        }

        private val actualHandleType: Int
            get() {
                if (isRightToLeft && handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT) {
                    return SelectionHandleStyle.HANDLE_TYPE_RIGHT
                }
                if (isRightToLeft && handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT) {
                    return SelectionHandleStyle.HANDLE_TYPE_LEFT
                }
                return handleType
            }

        private fun drawSelForLeftRight(): Boolean {
            return ((handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT || handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT)
                    && editor.props!!.showSelectionWhenSelected && !editor.isInMouseMode)
        }

        private fun drawSelForInsert(): Boolean {
            val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
            return (!(handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT || handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT)
                    && (editor.cursorBlink!!.visibility || handler
                .holdInsertHandle() || editor.isInLongSelect))

        }

        private val isSelForLongSelect: Boolean
            get() = editor.isInLongSelect && !(handleType == SelectionHandleStyle.HANDLE_TYPE_LEFT
                    || handleType == SelectionHandleStyle.HANDLE_TYPE_RIGHT)


        internal fun execute(canvas: Canvas) {
            // Hide cursors (API level 31)
            if (handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED) {
                if (editor.inputConnection!!.imeConsumingInput || !editor.isFocused()) {
                    return
                }
            }
            if (handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT && !editor.isEditable) {

                return
            }
            val descriptor: SelectionHandleStyle.HandleDescriptor =
                this.descriptor ?: TMP_DESC
            // Follow the thumb or stick to text row
            if (!descriptor.position.isEmpty()) {
                if (!editor.isStickyTextSelection) {
                    val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
                    if (handler
                            .getTouchedHandleType() === this.actualHandleType && handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED && handler
                            .isHandleMoving()
                    ) {
                        x =
                            handler.motionX + (if (descriptor.alignment != SelectionHandleStyle.ALIGN_CENTER) descriptor.position.width().toFloat() else 0f) * (if (descriptor.alignment == SelectionHandleStyle.ALIGN_LEFT) 1 else -1)
                        y = handler.motionY - descriptor.position.height().toFloat() * 2 / 3f
                    }
                }

            }

            if (drawSelForLeftRight() || drawSelForInsert() || handleType == SelectionHandleStyle.HANDLE_TYPE_UNDEFINED) {
                val startY: Float =
                    y - (if (editor.props!!.textBackgroundWrapTextOnly) editor.logicalRowHeight else editor.logicalRowHeight)
                val stopY = y
                paintGeneral.setColor(editor.colorScheme.getColor(EditorColorScheme.SELECTION_INSERT))
                paintGeneral.setStrokeWidth(editor.insertSelectionWidth)

                paintGeneral.setStyle(android.graphics.Paint.Style.STROKE)
                if (this.isSelForLongSelect) {
                    paintGeneral.setPathEffect(
                        DashPathEffect(
                            floatArrayOf(
                                (stopY - startY) / 8f,
                                (stopY - startY) / 8f
                            ), (stopY - startY) / 16f
                        )
                    )
                    paintGeneral.setStrokeWidth(editor.insertSelectionWidth.toFloat() * 1.5f)

                }
                canvas.drawLine(x, startY, x, stopY, paintGeneral)
                paintGeneral.setStyle(android.graphics.Paint.Style.FILL)
                paintGeneral.setPathEffect(null)
                if (drawSelForInsert() && isBidiIndicatorRequired) {
                    // Draw a flag for LTR/RTL mixed row
                    val height = (stopY - startY)
                    drawBidiSelectionIndicator(canvas, x, startY, height, isRightToLeft)
                }
            }
            var handleType = this.handleType
            // Hide insert handle conditionally
            val handler: io.github.abc15018045126.sora.widget.EditorTouchEventHandler = editor.touchHandler!!
            if (handleType == SelectionHandleStyle.HANDLE_TYPE_INSERT && (editor.isInLongSelect || !handler
                    .shouldDrawInsertHandle())

            ) {
                handleType = SelectionHandleStyle.HANDLE_TYPE_UNDEFINED
            }
            if (handleType != SelectionHandleStyle.HANDLE_TYPE_UNDEFINED && !editor.isInMouseMode /* hide if mouse inside */) {
                editor.handleStyle!!.draw(
                    canvas,
                    handleType,
                    x,
                    y,
                    editor.logicalRowHeight,
                    editor.colorScheme.getColor(EditorColorScheme.SELECTION_HANDLE),
                    descriptor
                )
                if (descriptor === TMP_DESC) {
                    descriptor.setEmpty()
                }
            } else {
                descriptor.setEmpty()
            }
        }


    }

    companion object {
        internal val TMP_DESC: SelectionHandleStyle.HandleDescriptor = SelectionHandleStyle.HandleDescriptor()
        private val PRESSED_DRAWABLE_STATE = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
        private val DEFAULT_DRAWABLE_STATE = intArrayOf(android.R.attr.state_enabled)

        internal const val LOG_TAG = "EditorRenderer"
        private val sDiagnosticsColorMapping = intArrayOf(
            0,
            EditorColorScheme.PROBLEM_TYPO,
            EditorColorScheme.PROBLEM_WARNING,
            EditorColorScheme.PROBLEM_ERROR
        )
        const val CURSOR_LINE_BG_OVERLAP_CURSOR = 0
        const val CURSOR_LINE_BG_OVERLAP_MIXED = 1
        const val CURSOR_LINE_BG_OVERLAP_CUSTOM = 2
    }
}


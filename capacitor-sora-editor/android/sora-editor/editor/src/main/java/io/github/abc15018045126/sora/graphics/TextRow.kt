package io.github.abc15018045126.sora.graphics

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.Log
import io.github.abc15018045126.sora.lang.styling.Span
import io.github.abc15018045126.sora.lang.styling.SpanFactory
import io.github.abc15018045126.sora.lang.styling.TextStyle
import io.github.abc15018045126.sora.lang.styling.inlayHint.CharacterSide
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.lang.styling.span.SpanExtAttrs
import io.github.abc15018045126.sora.lang.styling.span.SpanExternalRenderer
import io.github.abc15018045126.sora.text.ContentLine
import io.github.abc15018045126.sora.text.FunctionCharacters
import io.github.abc15018045126.sora.text.bidi.Directions
import io.github.abc15018045126.sora.text.bidi.IDirections
import io.github.abc15018045126.sora.text.bidi.VisualDirections
import io.github.abc15018045126.sora.text.breaker.WordBreaker
import io.github.abc15018045126.sora.text.breaker.WordBreakerEmpty
import io.github.abc15018045126.sora.util.IntPair
import io.github.abc15018045126.sora.util.RendererUtils
import io.github.abc15018045126.sora.util.ReversedListView
import io.github.abc15018045126.sora.util.TemporaryFloatBuffer
import io.github.abc15018045126.sora.widget.layout.RowElement
import io.github.abc15018045126.sora.widget.layout.RowElementTypes
import io.github.abc15018045126.sora.widget.rendering.RenderingConstants
import io.github.abc15018045126.sora.widget.rendering.TextAdvancesCache
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * [TextRow] is a helper class for a single text row to shape, measure and draw.
 */
class TextRow {
    private val tmpRect: RectF = RectF()
    private val tmpIndices = IntArray(4)
    private val tmpSpan: Span = SpanFactory.obtainNoExt(0, 0)
    private var text: ContentLine? = null
    private var directions: Directions? = null
    var textStart: Int = 0
        private set
    var textEnd: Int = 0
        private set
    private var spans: List<Span>? = null
    private var inlineElements: List<InlayHint>? = null
    private var params: TextRowParams? = null
    private var inlayHintRenderParams: InlayHintRenderParams? = null
    private var paint: Paint? = null
    private var measureCache: TextAdvancesCache? = null
    private var selectedStart = -1
    private var selectedEnd = -1

    fun set(
        text: ContentLine,
        start: Int, end: Int, spans: List<Span>?, inlineElements: List<InlayHint>?,
        directions: Directions?, paint: Paint,
        measureCache: TextAdvancesCache?, params: TextRowParams
    ) {
        this.text = text
        textStart = start
        textEnd = end
        this.spans = spans
        this.inlineElements = inlineElements
        this.directions = directions
        this.paint = paint
        this.params = params
        this.measureCache = measureCache
        this.inlayHintRenderParams = params.toInlayHintRenderParams()
    }

    fun setRange(start: Int, end: Int) {
        this.textStart = start
        this.textEnd = end
    }

    fun setSelectedRange(start: Int, end: Int) {
        this.selectedStart = start
        this.selectedEnd = end
    }

    private fun getSingleRunAdvancesForBreaking(
        start: Int, end: Int, contextStart: Int, contextEnd: Int,
        isRtl: Boolean, advances: FloatArray?
    ): Float {
        val chars = text!!.backingCharArray
        var lastEnd = start
        val tabWidth = params!!.tabWidth * paint!!.spaceWidth
        var width = 0f
        val p = paint!!
        for (i in start..end) {
            if (i == end || chars[i] == '\t') {
                if (i > lastEnd) width += p.getTextRunAdvances(
                    chars,
                    lastEnd,
                    i - lastEnd,
                    contextStart,
                    contextEnd - contextStart,
                    isRtl,
                    advances,
                    (lastEnd - start)
                )
                if (i < end) {
                    width += tabWidth
                    if (advances != null) advances[i - start] = tabWidth
                }
                lastEnd = i + 1
            }
        }
        return width
    }

    private fun getTextRunAdvancesCacheable(
        index: Int,
        count: Int,
        contextIndex: Int,
        contextCount: Int,
        isRtl: Boolean,
        advances: FloatArray?,
        advancesIndex: Int
    ): Float {
        val cache = measureCache
        if (cache != null) {
            if (advances != null) {
                for (i in 0 until count) {
                    advances[advancesIndex + i] = cache.getAdvanceAt(index + i)
                }
            }
            return cache.getAdvancesSum(index, index + count)
        }
        return paint!!.getTextRunAdvances(
            text!!.backingCharArray,
            index,
            count,
            contextIndex,
            contextCount,
            isRtl,
            advances,
            advancesIndex
        )
    }

    private fun getRunAdvanceCacheable(
        offset: Int, start: Int, end: Int,
        contextStart: Int, contextEnd: Int, isRtl: Boolean
    ): Float {
        val cache = measureCache
        if (cache != null) {
            return cache.getAdvancesSum(start, offset)
        }
        return GraphicsCompat.getRunAdvance(
            paint!!,
            text!!.backingCharArray,
            start,
            end,
            contextStart,
            contextEnd,
            isRtl,
            offset
        )
    }

    private fun findOffsetByAdvanceCacheable(
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        isRtl: Boolean,
        advance: Float
    ): Int {
        val cache = measureCache
        if (cache != null) {
            var left = start
            var right = end
            val base = cache.getAdvancesSum(0, start)
            while (left <= right) {
                val mid = (left + right) / 2
                if (mid < start || mid >= end) {
                    left = mid
                    break
                }
                val value = cache.getAdvancesSum(0, mid) - base
                if (value > advance) {
                    right = mid - 1
                } else if (value < advance) {
                    left = mid + 1
                } else {
                    left = mid
                    break
                }
            }
            if (cache.getAdvancesSum(0, left) - base > advance) {
                left--
            }
            left = max(start, min(end, left))
            return left
        }
        return paint!!.findOffsetByRunAdvance(text!!, start, end, contextStart, contextEnd, isRtl, advance)
    }

    private fun iterateRuns(consumer: RunElementsConsumer, reorderVisually: Boolean) {
        var pointers: ListPointers? = null
        val dirs: IDirections = if (reorderVisually && text!!.mayNeedBidi()) VisualDirections(directions!!) else directions!!
        for (i in 0 until dirs.runCount) {
            val runEnd = dirs.getRunEnd(i)
            val runStart = dirs.getRunStart(i)
            val segmentStart = max(runStart, textStart)
            val segmentEnd = min(runEnd, textEnd)
            if (segmentStart >= segmentEnd) {
                continue
            }
            pointers = seekStartIndices(segmentStart)
            if (!generateAndConsumeSingleRun(segmentStart, segmentEnd, dirs.isRunRtl(i), pointers, consumer)) {
                break
            }
        }
        var currInlineIndex = if (pointers == null) 0 else pointers.inlineElementIndex
        val trailingInlineRun: MutableList<RowElement?> = ArrayList()
        val elements = inlineElements
        if (elements != null) {
            while (currInlineIndex < elements.size && getExpectedInlayHintColumn(elements[currInlineIndex]) == textEnd) {
                val e = RowElement()
                e.type = RowElementTypes.INLAY_HINT
                e.displayColumnPosition = textEnd
                e.inlayHint = elements[currInlineIndex++]
                trailingInlineRun.add(e)
            }
        }
        if (trailingInlineRun.isNotEmpty()) {
            if (pointers == null) {
                pointers = seekStartIndices(textEnd)
            }
            pointers.inlineElementIndex = currInlineIndex
            consumer.accept(trailingInlineRun, false, pointers)
        }
    }

    private fun getExpectedInlayHintColumn(inlayHint: InlayHint): Int {
        var position = inlayHint.column
        if (inlayHint.displaySide == CharacterSide.RIGHT) {
            position++
        }
        position = min(position, textEnd)
        return position
    }

    private fun seekStartIndices(segmentStart: Int): ListPointers {
        tmpSpan.column = segmentStart
        val localSpans = spans!!
        var spanIndex = Collections.binarySearch(
            localSpans,
            tmpSpan,
            SPAN_COMPARATOR
        )
        if (spanIndex < 0) {
            spanIndex = -(spanIndex + 1)
        }
        if (spanIndex == localSpans.size) {
            spanIndex--
        }
        while (spanIndex > 0 && localSpans[spanIndex].column >= segmentStart) {
            spanIndex--
        }
        var inlineIndex = 0
        val localInline = inlineElements
        if (localInline != null) {
            while (inlineIndex < localInline.size && localInline[inlineIndex].column < segmentStart) {
                inlineIndex++
            }
        }
        return ListPointers(spanIndex, inlineIndex)
    }

    private fun generateAndConsumeSingleRun(
        segmentStart: Int,
        segmentEnd: Int,
        isRtl: Boolean,
        pointers: ListPointers,
        consumer: RunElementsConsumer
    ): Boolean {
        val runElements: MutableList<RowElement?> = ArrayList()
        var lastEndIndex = segmentStart
        val localInline = inlineElements
        while (true) {
            if (localInline != null && pointers.inlineElementIndex < localInline.size && localInline[pointers.inlineElementIndex]
                    .column < segmentEnd
            ) {
                val inlay = localInline[pointers.inlineElementIndex]
                val position = getExpectedInlayHintColumn(inlay)
                val element = RowElement()
                if (lastEndIndex == position) {
                    pointers.inlineElementIndex++
                    element.type = RowElementTypes.INLAY_HINT
                    element.inlayHint = inlay
                    element.displayColumnPosition = position
                } else {
                    element.type = RowElementTypes.TEXT
                    element.startColumn = lastEndIndex
                    element.endColumn = position
                    element.isRtlText = isRtl
                    lastEndIndex = position
                }
                runElements.add(element)
            } else if (lastEndIndex < segmentEnd) {
                val element = RowElement()
                element.type = RowElementTypes.TEXT
                element.startColumn = lastEndIndex
                element.endColumn = segmentEnd
                element.isRtlText = isRtl
                lastEndIndex = segmentEnd
                runElements.add(element)
            } else {
                break
            }
        }
        val result = consumer.accept(runElements, isRtl, pointers)
        val localSpans = spans!!
        val spansSize = localSpans.size
        while (pointers.spanIndex + 1 < spansSize && localSpans[pointers.spanIndex + 1].column <= segmentEnd) {
            pointers.spanIndex++
        }
        return result
    }

    fun breakText(width: Int, antiWordBreaking: Boolean): List<WordwrapRow> {
        val rows: MutableList<WordwrapRow> = ArrayList()
        val optimizer: WordBreaker = if (antiWordBreaking) WordBreaker.Factory.newInstance(text!!) else WordBreakerEmpty.INSTANCE

        class TextBreaker : RunElementsConsumer {
            var currentRow: WordwrapRow = WordwrapRow()
            var currentWidth: Float = 0f

            override fun accept(elements: List<RowElement?>?, isRtl: Boolean, pointers: ListPointers?): Boolean {
                if (elements != null) {
                    for (element in elements) {
                        if (element != null) {
                            if (element.type == RowElementTypes.TEXT) {
                                handleText(element)
                            } else if (element.type == RowElementTypes.INLAY_HINT) {
                                handleInlineElement(element)
                            }
                        }
                    }
                }
                return true
            }

            fun commitRow() {
                currentRow.rowWidth = currentWidth
                rows.add(currentRow)
                currentWidth = 0f
                currentRow = WordwrapRow()
            }

            fun handleText(e: RowElement) {
                val advances = TemporaryFloatBuffer.obtain(e.endColumn - e.startColumn) // FloatArray
                val runWidth = getSingleRunRunAdvancesForBreaking(
                    e.startColumn,
                    e.endColumn,
                    e.startColumn,
                    e.endColumn,
                    e.isRtlText,
                    advances
                )

                if (currentWidth + runWidth < width) {
                    if (currentRow.isEmpty) {
                        currentRow.setInitialRange(e.startColumn, e.endColumn)
                    } else {
                        currentRow.setEndColumnChecked(e.endColumn)
                    }
                    currentWidth += runWidth
                    TemporaryFloatBuffer.recycle(advances)
                    return
                }

                val limit = e.endColumn - e.startColumn
                var offset = 0
                while (offset < limit) {
                    var next = GraphemeBoundsBreaker.findGraphemeBreakPoint(
                        advances,
                        limit,
                        (width - currentWidth).toInt(),
                        offset
                    )
                    if (next == offset) {
                        if (currentRow.isEmpty) {
                            next++
                        } else {
                            commitRow()
                            continue
                        }
                    }
                    val beforeOptimization = next
                    next =
                        optimizer.getOptimizedBreakPoint(e.startColumn + offset, e.startColumn + next) - e.startColumn
                    var advance = 0f
                    for (j in offset until next) {
                        advance += advances[j]
                    }
                    if (currentRow.isEmpty) {
                        currentRow.setInitialRange(e.startColumn + offset, e.startColumn + next)
                    } else {
                        currentRow.setEndColumnChecked(e.startColumn + next)
                    }
                    currentWidth += advance
                    if (beforeOptimization != next) {
                        commitRow()
                    }
                    offset = next
                }
                TemporaryFloatBuffer.recycle(advances)
            }

            fun handleInlineElement(e: RowElement) {
                val inlay = e.inlayHint!!
                val renderer = params!!.inlayHintRendererProvider.getInlayHintRendererForType(inlay.type)
                var w = 0f
                if (renderer != null) {
                    w = renderer.measure(inlay, paint!!, inlayHintRenderParams!!)
                    w = max(0f, w)
                }
                if (currentRow.isEmpty || currentWidth + w > width) {
                    if (!currentRow.isEmpty) {
                        commitRow()
                    }
                    currentRow.setInitialRange(e.displayColumnPosition, e.displayColumnPosition)
                    currentRow.addInlayHint(inlay)
                    currentWidth = w
                } else {
                    currentRow.addInlayHint(inlay)
                    currentWidth += w
                }
            }

            private fun getSingleRunRunAdvancesForBreaking(
                start: Int, end: Int, contextStart: Int, contextEnd: Int,
                isRtl: Boolean, advances: FloatArray?
            ): Float {
                return getSingleRunAdvancesForBreaking(start, end, contextStart, contextEnd, isRtl, advances)
            }

            fun appendTailIfNeeded() {
                if (!currentRow.isEmpty) {
                    commitRow()
                }
            }
        }

        val breaker = TextBreaker()
        iterateRuns(breaker, false)
        if (rows.isEmpty() && breaker.currentRow.isEmpty) {
            breaker.currentRow.isEmpty = false
            breaker.currentRow.startColumn = textStart
            breaker.currentRow.endColumn = textEnd
        }
        breaker.appendTailIfNeeded()
        return rows
    }

    private class IteratingContext {
        var lastStyle: Long = -1
        var minOffset: Float = 0f
        var maxOffset: Float = Float.MAX_VALUE
        var targetCharOffset: Int = -1
        var resultOffset: Float = 0f
        var targetHorizontalOffset: Float = -1f
        var resultCharOffset: Int = -1
        var startCharOffset: Int = 0
        var endCharOffset: Int = 0
        var regionBuffer: RegionBuffer? = null
        var autoClip: Boolean = false
        var drawTextConsumer: DrawTextConsumer? = null
        var currentSpan: Span? = null
        var advances: TextAdvancesCache? = null
    }

    private fun checkCursorOffsetInSegment(offset: Int, start: Int, end: Int): Boolean {
        return (offset >= start && (offset < end || (offset == end && end == textEnd)))
    }

    private fun clipRegionForPatchDrawing(textOffset: Float, width: Float, italics: Boolean, canvas: Canvas) {
        if (!italics) {
            canvas.clipRect(textOffset, 0f, textOffset + width, params!!.rowHeight.toFloat())
            return
        }
        val path = Path()
        val y = params!!.textBottom.toFloat()
        path.moveTo(textOffset, y)
        path.lineTo(textOffset - RenderingConstants.TEXT_SKEW_X * y, 0f)
        path.lineTo(textOffset + width - RenderingConstants.TEXT_SKEW_X * y, 0f)
        path.lineTo(textOffset + width, y)
        path.close()
        canvas.clipPath(path)
    }

    protected fun drawFunctionCharacter(canvas: Canvas, offsetX: Float, width: Float, ch: Char) {
        val paintGraph = params!!.graphPaint
        val metricsGraph = params!!.graphMetrics
        paintGraph.textAlign = android.graphics.Paint.Align.CENTER
        val heightScaled = metricsGraph.descent - metricsGraph.ascent
        val centerY = params!!.rowHeight / 2f
        val baseline = centerY - heightScaled / 2f - metricsGraph.ascent
        paintGraph.color = paint!!.color
        canvas.drawText(FunctionCharacters.getNameForFunctionCharacter(ch), offsetX + width / 2f, baseline, paintGraph)
        paintGraph.textAlign = android.graphics.Paint.Align.LEFT

        val actualWidth = paintGraph.measureText(FunctionCharacters.getNameForFunctionCharacter(ch))
        tmpRect.top = centerY - heightScaled / 2f
        tmpRect.bottom = centerY + heightScaled / 2f
        tmpRect.left = offsetX + width / 2f - actualWidth / 2f
        tmpRect.right = offsetX + width / 2f + actualWidth / 2f
        val color = paint!!.color
        paint!!.color = params!!.colorScheme.getColor(EditorColorScheme.FUNCTION_CHAR_BACKGROUND_STROKE)
        paint!!.style = android.graphics.Paint.Style.STROKE
        paint!!.strokeWidth = params!!.rowHeight * 0.05f
        val radius = params!!.rowHeight * params!!.roundTextBackgroundFactor
        canvas.drawRoundRect(
            tmpRect,
            radius,
            radius,
            paint!!
        )
        paint!!.style = android.graphics.Paint.Style.FILL
        paint!!.color = color
    }

    private fun commitTextRunToCanvas(
        paintStart: Int, paintEnd: Int, contextStart: Int, contextEnd: Int, isRtl: Boolean,
        canvas: Canvas, offset: Float, width: Float
    ) {
        val p = paint!!
        if (p.isRenderFunctionCharacters) {
            val chars = text!!.backingCharArray
            var lastEnd = paintStart
            val initOffset = offset + (if (isRtl) width else 0f)
            var drawOffset = initOffset
            for (i in paintStart..paintEnd) {
                var ch = '\u0000'
                if (i == paintEnd || FunctionCharacters.isEditorFunctionChar(chars[i].also { ch = it })) {
                    if (i - lastEnd > 0) {
                        if (isRtl) {
                            p.textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        GraphicsCompat.drawTextRun(
                            canvas,
                            chars,
                            lastEnd,
                            i - lastEnd,
                            contextStart,
                            contextEnd - contextStart,
                            drawOffset,
                            params!!.textBaseline.toFloat(),
                            isRtl,
                            p
                        )
                        if (isRtl) {
                            p.textAlign = android.graphics.Paint.Align.LEFT
                        }
                    }
                    if (i == paintEnd) {
                        break
                    }
                    val chAdvance = p.measureText(FunctionCharacters.getNameForFunctionCharacter(ch))
                    var advance = getRunAdvanceCacheable(i, paintStart, paintEnd, paintStart, paintEnd, isRtl)
                    drawFunctionCharacter(
                        canvas,
                        if (isRtl) initOffset - advance - chAdvance else initOffset + advance,
                        chAdvance,
                        ch
                    )
                    advance += chAdvance
                    drawOffset = initOffset + (if (isRtl) -advance else advance)
                    lastEnd = i
                }
            }
        } else {
            GraphicsCompat.drawTextRun(
                canvas,
                text!!.backingCharArray,
                paintStart,
                paintEnd - paintStart,
                contextStart,
                contextEnd - contextStart,
                offset,
                params!!.textBaseline.toFloat(),
                isRtl,
                p
            )
        }
    }

    private fun commitTextRunToConsumer(
        paintStart: Int, paintEnd: Int, contextStart: Int, contextEnd: Int, isRtl: Boolean,
        canvas: Canvas?, offset: Float, width: Float, ctx: IteratingContext
    ) {
        ctx.drawTextConsumer!!.drawText(
            canvas,
            text!!.backingCharArray,
            paintStart,
            paintEnd - paintStart,
            contextStart,
            contextEnd - contextStart,
            isRtl, // isRtl
            offset,
            width,
            params,
            ctx.currentSpan
        )
    }

    private fun commitTextRunAutoTruncated(
        paintStart: Int, paintEnd: Int, contextStart: Int, contextEnd: Int, isRtl: Boolean,
        canvas: Canvas, offset: Float, width: Float, ctx: IteratingContext
    ) {
        if (paintEnd - paintStart < MIN_AUTO_TRUNCATE_LENGTH || measureCache == null) {
            if (ctx.drawTextConsumer != null) {
                commitTextRunToConsumer(
                    paintStart,
                    paintEnd,
                    contextStart,
                    contextEnd,
                    isRtl,
                    canvas,
                    offset,
                    width,
                    ctx
                )
            } else {
                commitTextRunToCanvas(paintStart, paintEnd, contextStart, contextEnd, isRtl, canvas, offset, width)
            }
        } else {
            val runAdvanceLeft = max(0f, ctx.minOffset - offset) - paint!!.spaceWidth
            val runAdvanceRight = min(width, ctx.maxOffset - offset) + paint!!.spaceWidth
            val boundForLeft =
                findOffsetByAdvanceCacheable(paintStart, paintEnd, contextStart, contextEnd, isRtl, runAdvanceLeft)
            val boundForRight =
                findOffsetByAdvanceCacheable(paintStart, paintEnd, contextStart, contextEnd, isRtl, runAdvanceRight)
            val commitStart = min(boundForLeft, boundForRight)
            val commitEnd = max(boundForLeft, boundForRight)

            if (commitStart < commitEnd) {
                var commitContextStart = commitStart
                var commitContextEnd = commitEnd
                val chars = text!!.backingCharArray
                while (commitContextStart - 1 >= contextStart && chars[commitContextStart - 1] != ' ' && (commitContextEnd - commitContextStart) < MAX_CONTEXT_LENGTH) {
                    commitContextStart--
                }
                while (commitContextEnd + 1 < contextEnd && chars[commitContextEnd] != ' ' && (commitContextEnd - commitContextStart) < MAX_CONTEXT_LENGTH) {
                    commitContextEnd++
                }

                val advanceStart =
                    measureAdvanceInRun(commitStart, paintStart, paintEnd, contextStart, contextEnd, isRtl)
                val advanceEnd = measureAdvanceInRun(commitEnd, paintStart, paintEnd, contextStart, contextEnd, isRtl)
                val newWidth = abs(advanceStart - advanceEnd)
                val commitOffset = if (isRtl) offset + width - advanceEnd else offset + advanceStart
                if (ctx.drawTextConsumer != null) {
                    commitTextRunToConsumer(
                        commitStart,
                        commitEnd,
                        contextStart,
                        contextEnd,
                        isRtl,
                        canvas,
                        commitOffset,
                        newWidth,
                        ctx
                    )
                } else {
                    commitTextRunToCanvas(
                        commitStart,
                        commitEnd,
                        contextStart,
                        contextEnd,
                        isRtl,
                        canvas,
                        commitOffset,
                        newWidth
                    )
                }
            }
        }
    }

    private fun splitRegionsAndCommit(
        paintStart: Int, paintEnd: Int, isRtl: Boolean,
        canvas: Canvas, offset: Float, width: Float, color: Int, ctx: IteratingContext
    ) {
        val selectionStartLocal = max(paintStart, min(paintEnd, selectedStart))
        val selectionEndLocal = max(paintStart, min(paintEnd, selectedEnd))
        tmpIndices[0] = paintStart
        tmpIndices[1] = paintEnd
        tmpIndices[2] = selectionStartLocal
        tmpIndices[3] = selectionEndLocal
        Arrays.sort(tmpIndices)
        var advance = 0f
        var i = 0
        while (i + 1 < tmpIndices.size) {
            val commitStart = tmpIndices[i]
            val commitEnd = tmpIndices[i + 1]
            if (commitStart == commitEnd) {
                i++
                continue
            }
            if (commitStart >= selectionStartLocal && commitEnd <= selectionEndLocal) {
                paint!!.color = params!!.colorScheme.getColor(EditorColorScheme.TEXT_SELECTED)
            } else {
                paint!!.color = color
            }
            val segmentWidth = getRunAdvanceCacheable(commitEnd, commitStart, commitEnd, paintStart, paintEnd, isRtl)
            if (isRtl) {
                commitTextRunAutoTruncated(
                    commitStart,
                    commitEnd,
                    paintStart,
                    paintEnd,
                    true,
                    canvas,
                    offset + width - advance - segmentWidth,
                    segmentWidth,
                    ctx
                )
            } else {
                commitTextRunAutoTruncated(
                    commitStart,
                    commitEnd,
                    paintStart,
                    paintEnd,
                    false,
                    canvas,
                    offset + advance,
                    segmentWidth,
                    ctx
                )
            }
            advance += segmentWidth
            i++
        }
    }

    private fun handleSingleStyledText(
        paintStart: Int, paintEnd: Int, isRtl: Boolean, span: Span,
        canvas: Canvas?, offset: Float, ctx: IteratingContext
    ): Float {
        val paintGeneral = paint!!

        if ((canvas != null && ctx.drawTextConsumer == null) || measureCache == null) {
            val styleBits = span.styleBits
            if (styleBits != ctx.lastStyle) {
                paintGeneral.isFakeBoldText = TextStyle.isBold(styleBits)
                if (TextStyle.isItalics(styleBits)) {
                    paintGeneral.textSkewX = RenderingConstants.TEXT_SKEW_X
                } else {
                    paintGeneral.textSkewX = 0f
                }
                ctx.lastStyle = styleBits
            }
        }

        var advances: FloatArray? = null
        if (ctx.advances != null) {
            advances = TemporaryFloatBuffer.obtain(paintEnd - paintStart)
        }
        val width = getTextRunAdvancesCacheable(
            paintStart, paintEnd - paintStart,
            paintStart, paintEnd - paintStart, isRtl,
            advances, 0
        )
        val ctxAdvances = ctx.advances
        if (ctxAdvances != null && advances != null) {
            for (i in paintStart until paintEnd) {
                ctxAdvances.setAdvanceAt(i, advances[i - paintStart])
            }
            TemporaryFloatBuffer.recycle(advances)
        }
        if (checkCursorOffsetInSegment(ctx.targetCharOffset, paintStart, paintEnd)) {
            ctx.maxOffset = 0f
            val advance =
                getRunAdvanceCacheable(ctx.targetCharOffset, paintStart, paintEnd, paintStart, paintEnd, isRtl)
            if (isRtl) {
                ctx.resultOffset = offset + width - advance
            } else {
                ctx.resultOffset = offset + advance
            }
            return width
        }

        if (ctx.targetHorizontalOffset != -1f) {
            var runOffset = ctx.targetHorizontalOffset - offset
            if (isRtl) {
                runOffset = width - runOffset
            }
            if (runOffset > width) {
                ctx.resultCharOffset = paintEnd
            } else if (runOffset <= 0) {
                ctx.resultCharOffset = paintStart
            } else {
                ctx.resultCharOffset =
                    findOffsetByAdvanceCacheable(paintStart, paintEnd, paintStart, paintEnd, isRtl, runOffset)
            }
        }

        var regionLeft = -1f
        var regionRight = -1f
        val regionBuffer = ctx.regionBuffer
        if (regionBuffer != null || ctx.drawTextConsumer != null) {
            val sharedTextStart = max(paintStart, ctx.startCharOffset)
            val sharedTextEnd = min(paintEnd, ctx.endCharOffset)
            if (sharedTextStart < sharedTextEnd) {
                if (sharedTextStart == paintStart && sharedTextEnd == paintEnd) {
                    regionLeft = offset
                    regionRight = offset + width
                } else {
                    var startAdvance =
                        getRunAdvanceCacheable(sharedTextStart, paintStart, paintEnd, paintStart, paintEnd, isRtl)
                    var endAdvance =
                        getRunAdvanceCacheable(sharedTextEnd, paintStart, paintEnd, paintStart, paintEnd, isRtl)
                    startAdvance = if (isRtl) width - startAdvance else startAdvance
                    endAdvance = if (isRtl) width - endAdvance else endAdvance
                    regionLeft = offset + min(startAdvance, endAdvance)
                    regionRight = offset + max(startAdvance, endAdvance)
                }
                if (regionBuffer != null) {
                    regionBuffer.commitRegion(regionLeft, regionRight)
                }
            }
        }

        val sharedStart = max(offset, ctx.minOffset)
        val sharedEnd = min(offset + width, ctx.maxOffset)
        if (sharedStart >= sharedEnd) {
            return width
        }

        if (canvas == null) {
            return width
        }

        if (ctx.drawTextConsumer != null) {
            val sharedTextStart = max(paintStart, ctx.startCharOffset)
            val sharedTextEnd = min(paintEnd, ctx.endCharOffset)
            if (sharedTextStart >= sharedTextEnd) {
                return width
            }
            if (ctx.autoClip) {
                canvas.save()
                clipRegionForPatchDrawing(
                    regionLeft,
                    regionRight - regionLeft,
                    TextStyle.isItalics(span.styleBits),
                    canvas
                )
            }
            ctx.currentSpan = span
            commitTextRunAutoTruncated(paintStart, paintEnd, paintStart, paintEnd, isRtl, canvas, offset, width, ctx)
            ctx.currentSpan = null
            ctx.lastStyle = -1
            if (ctx.autoClip) {
                canvas.restore()
            }
            return width
        }

        val renderer = span.getSpanExt(SpanExtAttrs.EXT_EXTERNAL_RENDERER) as SpanExternalRenderer?

        if (renderer != null && renderer.requirePreDraw()) {
            val saveCount = canvas.save()
            canvas.translate(offset, 0f)
            canvas.clipRect(0f, params!!.rowTop.toFloat(), width, params!!.rowHeight.toFloat())
            try {
                renderer.draw(canvas, paintGeneral, params!!.colorScheme, true)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error while invoking external renderer", e)
            }
            canvas.restoreToCount(saveCount)
        }

        val backgroundColor = RendererUtils.getBackgroundColor(span, params!!.colorScheme)
        if (backgroundColor != 0 && paintStart != paintEnd) {
            tmpRect.set(offset, params!!.rowTop.toFloat(), offset + width, params!!.rowBottom.toFloat())
            paintGeneral.color = backgroundColor
            val radius = params!!.rowHeight * params!!.roundTextBackgroundFactor
            canvas.drawRoundRect(tmpRect, radius, radius, paintGeneral)
        }

        val foregroundColor = RendererUtils.getForegroundColor(span, params!!.colorScheme)
        if (selectedStart >= selectedEnd || selectedStart >= textEnd || selectedEnd <= textStart || params!!.colorScheme
                .getColor(EditorColorScheme.TEXT_SELECTED) == 0
        ) {
            paintGeneral.color = foregroundColor
            commitTextRunAutoTruncated(paintStart, paintEnd, paintStart, paintEnd, isRtl, canvas, offset, width, ctx)
        } else {
            splitRegionsAndCommit(paintStart, paintEnd, isRtl, canvas, offset, width, foregroundColor, ctx)
        }

        if (TextStyle.isStrikeThrough(span.style)) {
            val strikethroughColor = params!!.colorScheme.getColor(EditorColorScheme.STRIKETHROUGH)
            val paintOther = params!!.miscPaint
            paintOther.color = if (strikethroughColor == 0) paintGeneral.color else strikethroughColor
            val y = params!!.rowTop + params!!.rowHeight / 2f
            canvas.drawLine(
                offset,
                y,
                offset + width,
                y,
                paintOther
            )
        }

        val underlineColor = span.underlineColor
        var underlineColorInt = 0
        if (underlineColor != null && (underlineColor.resolve(params!!.colorScheme)
                .also { underlineColorInt = it }) != 0
        ) {
            tmpRect.bottom = params!!.textBottom.toFloat()
            tmpRect.top = tmpRect.bottom - params!!.textHeight * RenderingConstants.TEXT_UNDERLINE_WIDTH_FACTOR
            tmpRect.left = offset
            tmpRect.right = offset + width
            paintGeneral.color = underlineColorInt
            canvas.drawRect(tmpRect, paintGeneral)
        }

        if (renderer != null && renderer.requirePostDraw()) {
            val saveCount = canvas.save()
            canvas.translate(offset, params!!.rowTop.toFloat())
            canvas.clipRect(0f, 0f, width, params!!.rowHeight.toFloat())
            try {
                renderer.draw(canvas, paintGeneral, params!!.colorScheme, false)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error while invoking external renderer", e)
            }
            canvas.restoreToCount(saveCount)
        }
        return width
    }

    private fun handleMultiStyledText(
        start: Int, end: Int, isRtl: Boolean, pointers: ListPointers,
        canvas: Canvas?, offset: Float, ctx: IteratingContext
    ): Float {
        var spanIndex = pointers.spanIndex
        val targetCharIndex = if (isRtl) end - 1 else start
        val localSpans = spans!!
        val spansSize = localSpans.size
        while (spanIndex + 1 < spansSize && localSpans[spanIndex + 1].column <= targetCharIndex) {
            spanIndex++
        }
        var localOffset = 0f
        if (isRtl) {
            var nextEnd = end
            while (nextEnd > start) {
                var moveSpanIndex = true
                var segmentStart: Int

                val span = localSpans[spanIndex]
                if (spanIndex == 0) {
                    moveSpanIndex = false
                    segmentStart = 0
                } else {
                    segmentStart = span.column
                }
                segmentStart = max(start, segmentStart)
                val segmentEnd = nextEnd

                localOffset += handleSingleStyledText(
                    segmentStart,
                    segmentEnd,
                    isRtl,
                    span,
                    canvas,
                    offset + localOffset,
                    ctx
                )

                if (moveSpanIndex) {
                    spanIndex--
                }
                nextEnd = segmentStart

                if (offset + localOffset > ctx.maxOffset) {
                    break
                }
            }
        } else {
            var lastEnd = start
            while (lastEnd < end) {
                var moveSpanIndex = true
                var segmentEnd: Int
                if (spanIndex + 1 >= spansSize) {
                    moveSpanIndex = false
                    segmentEnd = textEnd
                } else {
                    segmentEnd = localSpans[spanIndex + 1].column
                }
                segmentEnd = min(end, segmentEnd)
                val segmentStart = lastEnd
                val span = localSpans[spanIndex]

                localOffset += handleSingleStyledText(
                    segmentStart,
                    segmentEnd,
                    isRtl,
                    span,
                    canvas,
                    offset + localOffset,
                    ctx
                )

                lastEnd = segmentEnd
                if (moveSpanIndex) {
                    spanIndex++
                }

                if (offset + localOffset > ctx.maxOffset) {
                    break
                }
            }
        }
        return localOffset
    }

    private fun handleSingleTextElement(
        e: RowElement, pointers: ListPointers,
        canvas: Canvas?, offset: Float, ctx: IteratingContext
    ): Float {
        val chars = text!!.backingCharArray
        val isRtl = e.isRtlText
        var localOffset = 0f
        var lastEnd = if (isRtl) e.endColumn else e.startColumn
        val terminalIndex = if (isRtl) (e.startColumn - 1) else e.endColumn
        val tabWidth = params!!.tabWidth * paint!!.spaceWidth
        var index = (if (isRtl) e.endColumn - 1 else e.startColumn)
        while (if (isRtl) (index >= terminalIndex) else (index <= terminalIndex)
        ) {
            if (index == terminalIndex || chars[index] == '\t') {
                val regionStart = if (isRtl) index + 1 else lastEnd
                val regionEnd = if (isRtl) lastEnd else index
                localOffset += handleMultiStyledText(
                    regionStart,
                    regionEnd,
                    isRtl,
                    pointers,
                    canvas,
                    offset + localOffset,
                    ctx
                )
                if (offset + localOffset > ctx.maxOffset) {
                    break
                }
                if (index != terminalIndex) {
                    // tab
                    if (index == ctx.targetCharOffset || (index + 1 == ctx.targetCharOffset && index + 1 == textEnd)) {
                        val advance = if (index == ctx.targetCharOffset) 0f else tabWidth
                        if (isRtl) {
                            ctx.resultOffset = localOffset + tabWidth - advance
                        } else {
                            ctx.resultOffset = localOffset + advance
                        }
                        ctx.maxOffset = 0f
                    }
                    if (ctx.targetHorizontalOffset != -1f) {
                        var runOffset = ctx.targetHorizontalOffset - offset - localOffset
                        if (isRtl) {
                            runOffset = tabWidth - runOffset
                        }
                        if (runOffset > tabWidth / 2f) {
                            ctx.resultCharOffset = index + 1
                        } else {
                            ctx.resultCharOffset = index
                        }
                    }
                    val regionBuffer = ctx.regionBuffer
                    if (regionBuffer != null && index >= ctx.startCharOffset && index < ctx.endCharOffset) {
                        regionBuffer.commitRegion(offset + localOffset, offset + localOffset + tabWidth)
                    }
                    val ctxAdvances = ctx.advances
                    if (ctxAdvances != null) {
                        ctxAdvances.setAdvanceAt(index, tabWidth)
                    }
                    if (ctx.drawTextConsumer != null && index >= ctx.startCharOffset && index < ctx.endCharOffset) {
                        ctx.drawTextConsumer!!.drawText(
                            canvas,
                            chars,
                            index,
                            1,
                            index,
                            1,
                            isRtl,
                            offset + localOffset,
                            tabWidth,
                            params,
                            null
                        )
                    }
                    localOffset += tabWidth
                }
                lastEnd = if (isRtl) index else index + 1
                if (offset + localOffset > ctx.maxOffset) {
                    break
                }
            }
            index += (if (isRtl) -1 else 1)
        }
        return localOffset
    }

    private fun handleSingleInlineElement(
        e: RowElement,
        canvas: Canvas?, offset: Float, ctx: IteratingContext
    ): Float {
        val inlay = e.inlayHint!!
        val renderer = params!!.inlayHintRendererProvider.getInlayHintRendererForType(inlay.type)
        var w = 0f
        if (renderer != null) {
            w = renderer.measure(inlay, paint!!, inlayHintRenderParams!!)
            w = max(0f, w)
        }
        val regionBuffer = ctx.regionBuffer
        if (regionBuffer != null) {
            regionBuffer.commitPossibleInterval(offset, offset + w)
        }
        if (canvas == null || ctx.drawTextConsumer != null) {
            return w
        }
        val regionLeft = offset
        val regionRight = offset + w
        val sharedStart = max(regionLeft, ctx.minOffset)
        val sharedEnd = min(regionRight, ctx.maxOffset)
        if (renderer != null && sharedStart < sharedEnd) {
            val saveCount = canvas.save()
            canvas.translate(offset, params!!.rowTop.toFloat())
            renderer.render(inlay, canvas, paint!!, inlayHintRenderParams!!, params!!.colorScheme, w)
            canvas.restoreToCount(saveCount)
            ctx.lastStyle = -1
        }
        return w
    }

    private fun handleMultiElementRun(
        e: List<RowElement?>?, isRtl: Boolean, pointers: ListPointers,
        canvas: Canvas?, offset: Float, ctx: IteratingContext
    ): Float {
        if (e == null) return 0f
        // ReversedListView for ArrayList
        val visualElements = if (isRtl) ReversedListView(e) else e
        var localOffset = 0f
        for (element in visualElements) {
            if (element != null) {
                if (element.type == RowElementTypes.TEXT) {
                    localOffset += handleSingleTextElement(element, pointers, canvas, offset + localOffset, ctx)
                } else if (element.type == RowElementTypes.INLAY_HINT) {
                    localOffset += handleSingleInlineElement(element, canvas, offset + localOffset, ctx)
                }
            }
            if (offset + localOffset > ctx.maxOffset) {
                break
            }
        }
        return localOffset
    }

    fun draw(canvas: Canvas?, minHorizontalOffset: Float, maxHorizontalOffset: Float): Long {
        val ctx = IteratingContext()
        ctx.minOffset = minHorizontalOffset
        ctx.maxOffset = maxHorizontalOffset
        
        class DrawHandler : RunElementsConsumer {
            var horizontalOffset = 0f
            var isExhausted = true

            override fun accept(e: List<RowElement?>?, isRtl: Boolean, pointers: ListPointers?): Boolean {
                if (pointers != null) {
                    val runWidth = handleMultiElementRun(e, isRtl, pointers, canvas, horizontalOffset, ctx)
                    horizontalOffset += runWidth
                    val exhausted = horizontalOffset < maxHorizontalOffset
                    isExhausted = exhausted
                    return exhausted
                }
                return true
            }
        }

        val handler = DrawHandler()
        iterateRuns(handler, true)
        return IntPair.packIntFloat(if (handler.isExhausted) 1 else 0, handler.horizontalOffset)
    }

    fun getCursorOffsetForIndex(index: Int): Float {
        val ctx = IteratingContext()
        ctx.targetCharOffset = index
        class CursorOffsetHandler : RunElementsConsumer {
            var horizontalOffset = 0f

            override fun accept(e: List<RowElement?>?, isRtl: Boolean, pointers: ListPointers?): Boolean {
                if (pointers != null) {
                    val runWidth = handleMultiElementRun(e, isRtl, pointers, null, horizontalOffset, ctx)
                    horizontalOffset += runWidth
                    return ctx.maxOffset != 0f
                }
                return true
            }
        }

        val handler = CursorOffsetHandler()
        iterateRuns(handler, true)
        return ctx.resultOffset
    }

    fun getIndexForCursorOffset(offset: Float): Int {
        val ctx = IteratingContext()
        ctx.targetHorizontalOffset = offset
        ctx.maxOffset = offset
        iterateRuns(MaxOffsetIterationConsumer(ctx), true)
        return if (ctx.resultCharOffset == -1) textStart else ctx.resultCharOffset
    }

    fun iterateBackgroundRegions(
        start: Int,
        end: Int,
        allowLeadingBackground: Boolean,
        allowTrailingBackground: Boolean,
        handler: BackgroundRegionConsumer
    ) {
        val ctx = IteratingContext()
        ctx.startCharOffset = start
        ctx.endCharOffset = end
        ctx.regionBuffer = RegionBuffer(
            ctx,
            handler,
            allowLeadingBackground,
            allowTrailingBackground
        )
        iterateRuns(MaxOffsetIterationConsumer(ctx), true)
        ctx.regionBuffer?.commitCurrentIfPresent()
    }

    fun iterateDrawTextRegions(
        start: Int, end: Int, canvas: Canvas?,
        minHorizontalOffset: Float, maxHorizontalOffset: Float,
        autoClip: Boolean, consumer: DrawTextConsumer?
    ) {
        val ctx = IteratingContext()
        ctx.startCharOffset = start
        ctx.endCharOffset = end
        ctx.minOffset = minHorizontalOffset
        ctx.maxOffset = maxHorizontalOffset
        ctx.autoClip = autoClip
        ctx.drawTextConsumer = consumer
        iterateRuns(MaxOffsetIterationConsumer(ctx, canvas), true)
    }

    fun computeRowWidth(): Float {
        val ctx = IteratingContext()
        val handler = MaxOffsetIterationConsumer(ctx)
        iterateRuns(handler, true)
        return handler.horizontalOffset
    }

    fun measureAdvanceInRun(
        offset: Int, start: Int, end: Int,
        contextStart: Int, contextEnd: Int, isRtl: Boolean
    ): Float {
        return getRunAdvanceCacheable(offset, start, end, contextStart, contextEnd, isRtl)
    }

    fun buildMeasureCacheStep(cache: TextAdvancesCache?) {
        val ctx = IteratingContext()
        ctx.advances = cache
        iterateRuns(MaxOffsetIterationConsumer(ctx), true)
    }

    fun buildMeasureCacheTailor(cache: TextAdvancesCache) {
        cache.finishBuilding()
    }

    private inner class MaxOffsetIterationConsumer(var ctx: IteratingContext, var canvas: Canvas? = null) : RunElementsConsumer {
        var horizontalOffset = 0f

        override fun accept(e: List<RowElement?>?, isRtl: Boolean, pointers: ListPointers?): Boolean {
            if (pointers != null) {
                horizontalOffset += handleMultiElementRun(e, isRtl, pointers, canvas, horizontalOffset, ctx)
            }
            return horizontalOffset < ctx.maxOffset
        }
    }

    class WordwrapRow {
        var isEmpty = true
        var startColumn = 0
        var endColumn = 0
        var inlayHints: MutableList<InlayHint>? = null
        var rowWidth = 0f

        fun setInitialRange(start: Int, end: Int) {
            isEmpty = false
            startColumn = start
            endColumn = end
        }

        fun setEndColumnChecked(column: Int) {
            check(!isEmpty)
            this.endColumn = column
        }

        fun addInlayHint(inlayHint: InlayHint) {
            check(!isEmpty)
            if (inlayHints == null) {
                inlayHints = ArrayList()
            }
            inlayHints!!.add(inlayHint)
        }
    }

    internal class ListPointers(var spanIndex: Int, var inlineElementIndex: Int) {
        fun copy(): ListPointers {
            return ListPointers(spanIndex, inlineElementIndex)
        }
    }

    private class RegionBuffer(
        var ctx: IteratingContext,
        var consumer: BackgroundRegionConsumer,
        var allowLeadingBackground: Boolean,
        var allowTrailingBackground: Boolean
    ) {
        var isEmpty = true
        var currentLeft = 0f
        var currentRight = 0f
        var hasPossibleInterval = false
        var intervalLeft = 0f
        var intervalRight = 0f

        fun commitRegion(regionLeft: Float, regionRight: Float) {
            if (isEmpty) {
                if (hasPossibleInterval && abs(regionLeft - intervalRight) <= EPS) {
                    currentLeft = intervalLeft
                } else {
                    currentLeft = regionLeft
                }
                currentRight = regionRight
                isEmpty = false
                hasPossibleInterval = false
                return
            }
            if (!hasPossibleInterval && abs(regionLeft - currentRight) <= EPS) {
                currentRight = regionRight
                return
            } else if (hasPossibleInterval && abs(regionLeft - intervalRight) <= EPS) {
                currentRight = regionRight
                hasPossibleInterval = false
                return
            }
            commitCurrentIfPresent()
            isEmpty = false
            currentLeft = regionLeft
            currentRight = regionRight
        }

        fun commitPossibleInterval(regionLeft: Float, regionRight: Float) {
            if (isEmpty && !allowLeadingBackground) {
                return
            }
            if (hasPossibleInterval) {
                if (abs(regionLeft - intervalRight) <= EPS) {
                    intervalRight = regionRight
                } else {
                    hasPossibleInterval = false
                }
            } else if (abs(regionLeft - currentRight) <= EPS) {
                intervalLeft = regionLeft
                intervalRight = regionRight
                hasPossibleInterval = true
            }
        }

        fun commitCurrentIfPresent() {
            if (isEmpty) {
                return
            }
            if (hasPossibleInterval && allowTrailingBackground) {
                currentRight = intervalRight
            }
            if (!consumer.handleRegion(currentLeft, currentRight)) {
                ctx.maxOffset = 0f
            }
            isEmpty = true
            hasPossibleInterval = false
        }

        companion object {
            private const val EPS = 1e-6f
        }
    }

    internal interface RunElementsConsumer {
        fun accept(e: List<RowElement?>?, isRtl: Boolean, pointers: ListPointers?): Boolean
    }

    interface BackgroundRegionConsumer {
        fun handleRegion(left: Float, right: Float): Boolean
    }

    interface DrawTextConsumer {
        fun drawText(
            canvas: Canvas?,
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
        )
    }

    companion object {
        private const val LOG_TAG = "TextRow"
        private val SPAN_COMPARATOR = Comparator<Span> { a, b ->
            Integer.compare(a.column, b.column)
        }

        private const val MIN_AUTO_TRUNCATE_LENGTH = 64
        private const val MAX_CONTEXT_LENGTH = 256
    }
}

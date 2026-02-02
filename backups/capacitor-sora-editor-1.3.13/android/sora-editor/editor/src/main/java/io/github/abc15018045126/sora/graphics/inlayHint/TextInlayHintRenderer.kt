/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/abc15018045126/sora-editor
 *    Copyright (C) 2020-2025  abc15018045126
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact abc15018045126 by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/abc15018045126/sora-editor
 *    Copyright (C) 2020-2025  abc15018045126
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact abc15018045126 by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

package io.github.abc15018045126.sora.graphics.inlayHint

import android.graphics.Canvas
import io.github.abc15018045126.sora.graphics.InlayHintRenderParams
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.lang.styling.inlayHint.TextInlayHint
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

/**
 * A general inlay hint of text. The hint text will be displayed in a round colored rect.
 *
 * @see TextInlayHint
 * @author abc15018045126
 */
open class TextInlayHintRenderer : InlayHintRenderer() {

    companion object {
        val DefaultInstance = TextInlayHintRenderer()
    }

    protected val localPaint = Paint().also { it.isAntiAlias = true }

    override val typeName: String
        get() = "text"

    override fun onMeasure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float {
        localPaint.typeface = paint.typeface
        localPaint.textSize = paint.textSize * 0.75f

        val margin = localPaint.measureText(" ")
        val width = localPaint.measureText((inlayHint as? TextInlayHint)?.text ?: "") + margin * 2f
        return width
    }

    override fun onRender(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) {
        val centerY = (params.textTop + params.textBottom) / 2f
        localPaint.typeface = paint.typeface
        localPaint.textSize = paint.textSize * 0.75f

        val margin = localPaint.measureText(" ")
        val myLineHeight = localPaint.descent() - localPaint.ascent()
        localPaint.color = colorScheme.getColor(EditorColorScheme.TEXT_INLAY_HINT_BACKGROUND)
        canvas.drawRoundRect(
            margin * 0.5f,
            centerY - myLineHeight / 2f,
            measuredWidth - margin * 0.5f,
            centerY + myLineHeight / 2f,
            params.textHeight * 0.15f, params.textHeight * 0.15f,
            localPaint
        )
        localPaint.color = colorScheme.getColor(EditorColorScheme.TEXT_INLAY_HINT_FOREGROUND)
        val myBaseline = centerY + (myLineHeight / 2f - localPaint.descent())
        canvas.drawText((inlayHint as? TextInlayHint)?.text ?: "", margin, myBaseline, localPaint)
    }

}

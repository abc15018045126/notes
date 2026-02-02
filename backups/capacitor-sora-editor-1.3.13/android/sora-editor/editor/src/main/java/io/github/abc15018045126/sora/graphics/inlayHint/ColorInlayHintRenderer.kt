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
import android.graphics.Color
import io.github.abc15018045126.sora.graphics.InlayHintRenderParams
import io.github.abc15018045126.sora.graphics.Paint
import io.github.abc15018045126.sora.lang.styling.inlayHint.ColorInlayHint
import io.github.abc15018045126.sora.lang.styling.inlayHint.InlayHint
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

open class ColorInlayHintRenderer() : InlayHintRenderer() {

    companion object {
        val DefaultInstance = ColorInlayHintRenderer()
    }

    override val typeName: String
        get() = "color"

    protected val localPaint = Paint().also {
        it.isAntiAlias = true
        it.strokeWidth = 2f
    }

    override fun onMeasure(
        inlayHint: InlayHint,
        paint: Paint,
        params: InlayHintRenderParams
    ): Float {
        val margin = paint.spaceWidth
        return margin + params.textHeight * 0.75f
    }

    override fun onRender(
        inlayHint: InlayHint,
        canvas: Canvas,
        paint: Paint,
        params: InlayHintRenderParams,
        colorScheme: EditorColorScheme,
        measuredWidth: Float
    ) {
        val centerX = measuredWidth / 2f
        val centerY = (params.textTop + params.textBottom) / 2f
        val halfSize = params.textHeight * 0.75f / 2f
        localPaint.color =
            (inlayHint as? ColorInlayHint?)?.color?.resolve(colorScheme) ?: Color.WHITE
        localPaint.style = android.graphics.Paint.Style.FILL
        canvas.drawRect(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize,
            localPaint
        )
        localPaint.color = Color.WHITE
        localPaint.style = android.graphics.Paint.Style.STROKE
        canvas.drawRect(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize,
            localPaint
        )
    }


}

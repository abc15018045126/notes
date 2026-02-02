package io.github.abc15018045126.sora.util

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.util.Log
import android.view.ViewConfiguration

object ViewUtils {

    private const val LOG_TAG = "ViewUtils"

    const val DEFAULT_SCROLL_FACTOR: Float = 32f

    const val HOVER_TOOLTIP_SHOW_TIMEOUT: Long = 1000

    const val HOVER_TAP_SLOP: Int = 20

    @JvmStatic
    fun getVerticalScrollFactor(context: Context): Float {
        var verticalScrollFactor: Float
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val configuration = ViewConfiguration.get(context)
            verticalScrollFactor = configuration.scaledVerticalScrollFactor
        } else {
            var a: TypedArray? = null
            try {
                a = context.obtainStyledAttributes(intArrayOf(android.R.attr.listPreferredItemHeight))
                verticalScrollFactor = a.getDimension(0, DEFAULT_SCROLL_FACTOR)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to get vertical scroll factor, using default.", e)
                verticalScrollFactor = DEFAULT_SCROLL_FACTOR
            } finally {
                a?.recycle()
            }
        }
        return verticalScrollFactor
    }
}

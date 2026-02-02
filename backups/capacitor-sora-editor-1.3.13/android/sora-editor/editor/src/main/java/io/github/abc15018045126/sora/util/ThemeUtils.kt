package io.github.abc15018045126.sora.util

import android.util.TypedValue
import android.view.ContextThemeWrapper

object ThemeUtils {

    @JvmStatic
    fun getColorPrimary(context: ContextThemeWrapper): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
        return typedValue.data
    }
}

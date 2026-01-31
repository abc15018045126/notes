package io.github.abc15018045126.sora

import android.content.Context
import android.util.SparseIntArray

/**
 * Map editor built-in string resources to your given string resource. Editor string resource has
 * limited i18n function, as it only contains English and Chinese.
 * <p>
 * Note that you should configure this before creating editor instances
 *
 * @author abc15018045126
 */
object I18nConfig {

    private val mapping = SparseIntArray()

    /**
     * Map the given editor resId to new one
     */
    @JvmStatic
    fun mapTo(originalResId: Int, newResId: Int) {
        mapping.put(originalResId, newResId)
    }

    /**
     * Get mapped resource id or itself
     */
    @JvmStatic
    fun getResourceId(resId: Int): Int {
        val newResource = mapping.get(resId)
        return if (newResource == 0) resId else newResource
    }

    /**
     * Get mapped resource string
     */
    @JvmStatic
    fun getString(context: Context, resId: Int): String {
        return context.getString(getResourceId(resId))
    }
}

package io.github.abc15018045126.sora.util

import android.content.ClipData
import android.content.Intent

object ClipDataUtils {

    @JvmStatic
    fun clipDataToString(clipData: ClipData?): String {
        if (clipData == null) {
            return ""
        }
        val sb = StringBuilder()
        for (i in 0 until clipData.itemCount) {
            if (i > 0) {
                sb.append('\n')
            }
            val item = clipData.getItemAt(i)
            if (item.text != null) {
                sb.append(item.text)
            } else if (item.uri != null) {
                sb.append(item.uri.toString())
            } else if (item.intent != null) {
                sb.append(item.intent.toUri(Intent.URI_INTENT_SCHEME))
            }
        }
        return sb.toString()
    }
}

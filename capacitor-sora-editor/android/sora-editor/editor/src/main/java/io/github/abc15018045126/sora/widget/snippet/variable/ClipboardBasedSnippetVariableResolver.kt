package io.github.abc15018045126.sora.widget.snippet.variable

import android.content.ClipboardManager

class ClipboardBasedSnippetVariableResolver(private val clipboardManager: ClipboardManager?) : ISnippetVariableResolver {

    override fun getResolvableNames(): Array<String> {
        return arrayOf("CLIPBOARD")
    }

    override fun resolve(name: String): String {
        if ("CLIPBOARD" == name) {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                return clip.getItemAt(0).text?.toString() ?: ""
            }
            return ""
        }
        throw IllegalArgumentException("Unsupported variable name:$name")
    }
}

package io.github.abc15018045126.sora.widget.snippet.variable

import io.github.abc15018045126.sora.text.TextUtils.padStart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Resolver for time-related variables
 *
 * @author abc15018045126
 */
class TimeBasedSnippetVariableResolver : ISnippetVariableResolver {

    private fun getDisplayName(field: Int, shortType: Boolean): String {
        val c = Calendar.getInstance()
        var result = c.getDisplayName(field, if (shortType) Calendar.SHORT else Calendar.LONG, Locale.getDefault())
        if (result == null && shortType) {
            result = c.getDisplayName(field, Calendar.LONG, Locale.getDefault())
        }
        if (result == null) {
            result = c.getDisplayName(field, if (shortType) Calendar.SHORT else Calendar.LONG, Locale.US)
        }
        if (result == null) {
            // The very fallback
            result = c.get(field).toString()
        }
        return result
    }

    override fun getResolvableNames(): Array<String> {
        return arrayOf(
            "CURRENT_YEAR", "CURRENT_YEAR_SHORT", "CURRENT_MONTH", "CURRENT_DATE",
            "CURRENT_HOUR", "CURRENT_MINUTE", "CURRENT_SECOND", "CURRENT_DAY_NAME",
            "CURRENT_DAY_NAME_SHORT", "CURRENT_MONTH_NAME", "CURRENT_MONTH_NAME_SHORT",
            "CURRENT_SECONDS_UNIX"
        )
    }

    override fun resolve(name: String): String {
        return when (name) {
            "CURRENT_YEAR" -> Calendar.getInstance().get(Calendar.YEAR).toString()
            "CURRENT_YEAR_SHORT" -> padStart((Calendar.getInstance().get(Calendar.YEAR) % 100).toString(), '0', 2)
            "CURRENT_MONTH" -> padStart(Calendar.getInstance().get(Calendar.MONTH).toString(), '0', 2)
            "CURRENT_DATE" -> SimpleDateFormat.getDateInstance().format(Date())
            "CURRENT_HOUR" -> padStart(Calendar.getInstance().get(Calendar.HOUR_OF_DAY).toString(), '0', 2)
            "CURRENT_MINUTE" -> padStart(Calendar.getInstance().get(Calendar.MINUTE).toString(), '0', 2)
            "CURRENT_SECOND" -> padStart(Calendar.getInstance().get(Calendar.SECOND).toString(), '0', 2)
            "CURRENT_DAY_NAME" -> getDisplayName(Calendar.DAY_OF_WEEK, false)
            "CURRENT_DAY_NAME_SHORT" -> getDisplayName(Calendar.DAY_OF_WEEK, true)
            "CURRENT_MONTH_NAME" -> getDisplayName(Calendar.MONTH, false)
            "CURRENT_MONTH_NAME_SHORT" -> getDisplayName(Calendar.MONTH, true)
            "CURRENT_SECONDS_UNIX" -> (System.currentTimeMillis() / 1000.0).toLong().toString()
            else -> throw IllegalArgumentException("Unsupported variable name:$name")
        }
    }
}

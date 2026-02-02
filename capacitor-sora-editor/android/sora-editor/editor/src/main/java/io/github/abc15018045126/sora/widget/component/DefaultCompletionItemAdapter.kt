package io.github.abc15018045126.sora.widget.component

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

/**
 * Default adapter to display results
 *
 * @author Rose
 */
class DefaultCompletionItemAdapter : EditorCompletionAdapter() {

    override fun getItemHeight(): Int {
        // 45 dp
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            45f,
            getContext().resources.displayMetrics
        ).toInt()
    }

    override fun getView(
        pos: Int,
        view: View?,
        parent: ViewGroup,
        isCurrentCursorPosition: Boolean
    ): View {
        var convertView = view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                .inflate(R.layout.default_completion_result_item, parent, false)
        }
        val item = getItem(pos)

        val tvLabel = convertView!!.findViewById<TextView>(R.id.result_item_label)
        tvLabel.text = item.label
        tvLabel.setTextColor(getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY))

        val tvDesc = convertView.findViewById<TextView>(R.id.result_item_desc)
        tvDesc.text = item.desc
        tvDesc.setTextColor(getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY))

        convertView.tag = pos
        if (isCurrentCursorPosition) {
            convertView.setBackgroundColor(getThemeColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT))
        } else {
            convertView.setBackgroundColor(0)
        }
        val iv = convertView.findViewById<ImageView>(R.id.result_item_image)
        iv.setImageDrawable(item.icon)
        return convertView
    }

}

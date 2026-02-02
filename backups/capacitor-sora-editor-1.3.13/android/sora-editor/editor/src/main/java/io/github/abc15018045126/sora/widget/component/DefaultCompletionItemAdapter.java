
package io.github.abc15018045126.sora.widget.component;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.github.abc15018045126.sora.R;
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme;

/**
 * Default adapter to display results
 *
 * @author Rose
 */
public final class DefaultCompletionItemAdapter extends EditorCompletionAdapter {

    @Override
    public int getItemHeight() {
        // 45 dp
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 45, getContext().getResources().getDisplayMetrics());
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent, boolean isCurrentCursorPosition) {
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.default_completion_result_item, parent, false);
        }
        var item = getItem(pos);

        TextView tv = view.findViewById(R.id.result_item_label);
        tv.setText(item.label);
        tv.setTextColor(getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY));

        tv = view.findViewById(R.id.result_item_desc);
        tv.setText(item.desc);
        tv.setTextColor(getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY));

        view.setTag(pos);
        if (isCurrentCursorPosition) {
            view.setBackgroundColor(getThemeColor(EditorColorScheme.COMPLETION_WND_ITEM_CURRENT));
        } else {
            view.setBackgroundColor(0);
        }
        ImageView iv = view.findViewById(R.id.result_item_image);
        iv.setImageDrawable(item.icon);
        return view;
    }

}


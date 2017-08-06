package cn.intret.app.picgo.utils;

import android.view.View;
import android.widget.TextView;


public class ViewUtils {
    public static void setViewVisibility(View view, int id, int visibility) {
        View v = view.findViewById(id);
        if (v != null)
            v.setVisibility(visibility);
    }

    public static void setTextViewOnClick(View view, int id, View.OnClickListener itemsOnClick) {
        TextView v = (TextView) view.findViewById(id);
        if (v != null)
            v.setOnClickListener(itemsOnClick);
    }

    public static void setText(View view, int id, String value) {
        TextView v = (TextView)view.findViewById(id);
        if (v != null)
            v.setText(value);
    }

    public static void setTextColor(View view, int id, int resId) {
        TextView v = (TextView)view.findViewById(id);
        int color = view.getResources().getColor(resId);
        if (v != null)
            v.setTextColor(color);
    }
}

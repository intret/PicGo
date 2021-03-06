package cn.intret.app.picgo.utils;

import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ViewUtils {

    public static void setViewAndChildrenEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setViewAndChildrenEnabled(child, enabled);
            }
        }
    }

    public static boolean isShow(View view) {
        if (view == null) {
            return false;
        }
        return view.getVisibility() == View.VISIBLE;
    }

    public static boolean isHide(View view) {
        return !isShow(view);
    }

    public static void setViewVisibility(View view, @IdRes int id, int visibility) {
        View v = view.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    public static void setTextViewOnClick(View view, @IdRes int id, View.OnClickListener itemsOnClick) {
        TextView v = view.findViewById(id);
        if (v != null) {
            v.setOnClickListener(itemsOnClick);
        }
    }

    /**
     * Set text value of specified view's child view
     * @param view
     * @param id
     * @param value
     */
    public static void setText(View view, @IdRes int id, String value) {
        TextView v = view.findViewById(id);
        if (v != null) {
            v.setText(value);
        }
    }

    public static void setButtonText(View view, @IdRes int id, String value) {
        Button v = view.findViewById(id);
        if (v != null) {
            v.setText(value);
        }
    }

    public static void setTextColor(View view, @IdRes int id, @ColorRes int resId) {
        TextView v = view.findViewById(id);
        int color = view.getResources().getColor(resId);
        if (v != null) {
            v.setTextColor(color);
        }
    }
}

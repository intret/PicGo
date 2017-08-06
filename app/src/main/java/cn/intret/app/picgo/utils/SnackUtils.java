package cn.intret.app.picgo.utils;


import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

public class SnackUtils {
    public static Snackbar changeSnackBarColor(Snackbar snackbar) {
        View v = snackbar.getView();
        TextView tv = (TextView) v.findViewById(android.support.design.R.id.snackbar_text);
        TextView tvAction = (TextView) v.findViewById(android.support.design.R.id.snackbar_action);
        tv.setTextColor(Color.WHITE);
        tvAction.setTextColor(Color.WHITE);
        return snackbar;
    }

    public static Snackbar makeSnackbarLong(View view, @StringRes int textId) {
        Snackbar snackbar = Snackbar.make(view, textId, Snackbar.LENGTH_LONG);
        return changeSnackBarColor(snackbar);
    }

    public static Snackbar makeSnackbarShort(View view, @StringRes int textId) {
        Snackbar snackbar = Snackbar.make(view, textId, Snackbar.LENGTH_SHORT);
        return changeSnackBarColor(snackbar);
    }
    public static Snackbar makeSnackbarLong(View view, @NonNull CharSequence text) {
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_LONG);
        return changeSnackBarColor(snackbar);
    }

    public static Snackbar makeSnackbarShort(View view, @NonNull CharSequence text) {
        Snackbar snackbar = Snackbar.make(view, text, Snackbar.LENGTH_SHORT);
        return changeSnackBarColor(snackbar);
    }
}

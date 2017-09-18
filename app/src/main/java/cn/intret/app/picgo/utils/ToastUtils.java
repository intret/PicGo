package cn.intret.app.picgo.utils;


import android.content.Context;
import android.icu.text.UnicodeSetSpanner;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import cn.intret.app.picgo.R;

public class ToastUtils {
    public final static String TAG = ToastUtils.class.getSimpleName();
    public static void toastShort(Context context, String message) {
        Toast t = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void toastShort(Context context, @StringRes int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void toastLong(Context context, String message) {
        if (context == null) {
            Log.w(TAG, "toastLong: context is null : " + message);
            return;
        }
        Toast t = Toast.makeText(context, message, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }
    public static void toastLong(Context context, @StringRes int resId, Object ... formatArgs) {
        if (context == null) {
            Log.w(TAG, "toastLong: context is null : " + resId);
            return;
        }

        Toast t = Toast.makeText(context, context.getResources().getString(resId, formatArgs), Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void toastLong(Context context, @StringRes int resId) {
        if (context == null) {
            Log.w(TAG, "toastLong: context is null : " + resId);
            return;
        }
        Toast t = Toast.makeText(context, resId, Toast.LENGTH_LONG);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }
}

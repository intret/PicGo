package cn.intret.app.picgo.utils;


import android.content.Context;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class ToastUtils {
    public final static String TAG = ToastUtils.class.getSimpleName();

    public static void toastShort(Context context, String message) {
        toast(context, message, Toast.LENGTH_SHORT);
    }

    public static void toastShort(Context context, @StringRes int resId) {
        toast(context, resId, Toast.LENGTH_SHORT);
    }

    public static void toastShort(Context context, @StringRes int resId, Object... formatArgs) {
        toast(context, Toast.LENGTH_SHORT, resId, formatArgs);
    }

    public static void toastLong(Context context, String message) {
        toast(context, message, Toast.LENGTH_LONG);
    }

    public static void toastLong(Context context, @StringRes int resId, Object... formatArgs) {
        toast(context, Toast.LENGTH_LONG, resId, formatArgs);
    }

    public static void toastLong(Context context, @StringRes int resId) {
        toast(context, resId, Toast.LENGTH_LONG);
    }

    private static void toast(Context context, String message, int duration) {
        if (context == null) {
            Log.w(TAG, "toastLong: context is null : " + message);
            return;
        }
        Toast t = Toast.makeText(context, message, duration);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void toast(Context context, int duration, @StringRes int resId, Object... formatArgs) {
        if (context == null) {
            Log.w(TAG, "toastLong: context is null : " + resId);
            return;
        }

        Toast t = Toast.makeText(context, context.getResources().getString(resId, formatArgs), duration);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }

    public static void toast(Context context, int duration, @StringRes int resId) {
        if (context == null) {
            Log.w(TAG, "toastLong: context is null : " + resId);
            return;
        }
        Toast t = Toast.makeText(context, resId, duration);
        t.setGravity(Gravity.CENTER, 0, 0);
        t.show();
    }
}

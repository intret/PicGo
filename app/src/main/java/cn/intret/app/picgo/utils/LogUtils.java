package cn.intret.app.picgo.utils;

import android.util.Log;



public class LogUtils {

    public static void logMethodD(String tag) {

    }
    public static void logMethodD(String tag, String message) {
        Log.d(tag, Thread.currentThread().getStackTrace()[3].getMethodName() + " " + message);
    }

    public static void logMethodW(String tag) {
        Log.w(tag, Thread.currentThread().getStackTrace()[3].getMethodName());
    }

    public static void logMethodE(String tag) {
        Log.e(tag, Thread.currentThread().getStackTrace()[3].getMethodName());
    }
}

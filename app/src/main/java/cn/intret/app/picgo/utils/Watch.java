package cn.intret.app.picgo.utils;


import android.support.annotation.NonNull;
import android.util.Log;

/**
 * 计时器
 */
public class Watch {
    long mStart = 0; // in nanosecond
    long mPreviousGlance = 0; // 中间的时间
    long mStop = 0; // in nanosecond

    public Watch() {
        reset();
    }

    public Watch reset() {
        mStart = System.nanoTime();
        mPreviousGlance = mStart;
        mStop = mStart;
        return this;
    }

    public Watch glance() {
        mPreviousGlance = mStop;
        mStop = System.nanoTime();
        return this;
    }

    public long getGlanceMillisecond() {
        return (mStop - mPreviousGlance) / 1000000;
    }

    public long getGlanceSecond() {
        return (mStop - mPreviousGlance) / 1000000000;
    }

    /**
     * 总时间间隔,微秒.
     */
    public long getTotalMicrosecond() {
        return (mStop - mStart) / 1000;
    }

    /**
     * 总时间间隔,毫秒.
     */
    public long getTotalMillisecond() {
        return (mStop - mStart) / 1000000;
    }

    /**
     * 总时间间隔,秒.
     */
    public long getTotalSecond() {
        return (mStop - mStart) / 1000000000;
    }

    /*
     * 日志打印工具方法
     */

    public void logdGlanceMS(@NonNull String tag, String msg) {
        glance();
        Log.d(tag, appendGlanceMS(msg));
    }

    public void logwGlanceMS(@NonNull String tag, String msg) {
        glance();
        Log.w(tag, appendGlanceMS(msg));
    }

    public void logeGlanceMS(@NonNull String tag, String msg) {
        glance();
        Log.e(tag, appendGlanceMS(msg));
    }

    public void logGlanceMS(@NonNull String tag, String msg) {
        glance();
        long ms = getGlanceMillisecond();
        if (ms > 5000) {
            Log.e(tag, appendGlanceMS(msg));
        } else if (ms > 1000) {
            Log.w(tag, appendGlanceMS(msg));
        } else {
            Log.d(tag, appendGlanceMS(msg));
        }
    }

    /**
     * Log 总时间,时间大于5秒,使用Log.e;时间大于1小于5秒,使用Log.w
     */
    public void logTotalMS(@NonNull String tag, String msg) {
        glance();
        long ms = getTotalMillisecond();
        if (ms > 5000) {
            Log.e(tag, appendTotalMS(msg));
        } else if (ms > 1000) {
            Log.w(tag, appendTotalMS(msg));
        } else {
            Log.d(tag, appendTotalMS(msg));
        }
    }

    public void logdTotalMS(@NonNull String tag, String msg) {
        glance();
        Log.d(tag, appendTotalMS(msg));
    }

    public void logwTotalMS(@NonNull String tag, String msg) {
        glance();
        Log.w(tag, appendTotalMS(msg));
    }

    public void logeTotalMS(@NonNull String tag, String msg) {
        glance();
        Log.e(tag, appendTotalMS(msg));
    }

    private String appendGlanceMS(String msg) {
        return msg + " 耗时 " + getGlanceMillisecond() + " ms.";
    }

    private String appendTotalMS(String msg) {
        return msg + " 耗时 " + getTotalMillisecond() + " ms.";
    }

    public static Watch now() {
        return new Watch();
    }
}
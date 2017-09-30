package cn.intret.app.picgo.app;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.LogcatLogStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;

import cn.intret.app.picgo.BuildConfig;
import cn.intret.app.picgo.model.ImageService;
import cn.intret.app.picgo.model.UserDataService;
import cn.intret.app.picgo.utils.Watch;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Application Component
 */

public class AppComponent extends Application {
    static AppComponent instance;

    public final static String TAG = "AppComponent";

    public AppComponent() {
        instance = this;
    }


    public boolean isBackground() {
        Context context = getApplicationContext();

        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                /*
                BACKGROUND=400 EMPTY=500 FOREGROUND=100
                GONE=1000 PERCEPTIBLE=130 SERVICE=300 ISIBLE=200
                 */
                Log.d(context.getPackageName(), "importance ="
                        + appProcess.importance
                        + ",context.getClass().getName()="
                        + context.getClass().getName());
                if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    Log.i(context.getPackageName(), "处于后台"
                            + appProcess.processName);
                    return true;
                } else {
                    Log.i(context.getPackageName(), "处于前台"
                            + appProcess.processName);
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

    }

    // http://stackoverflow.com/questions/30827097/determine-by-which-process-application-oncreate-is-called
    String getProcessName() {
        int myPid = android.os.Process.myPid(); // Get my Process ID
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream("/proc/" + myPid + "/cmdline"));
            StringBuilder processName = new StringBuilder();
            int c;
            while ((c = reader.read()) > 0) {
                processName.append((char) c);
            }

            return processName.toString();
        } catch (Exception e) {
            // ignore
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        return "";
    }

    boolean isMainProcess() {
        String baseAppId = "cn.intret.app.picgo";
        String processName = getProcessName();
        return TextUtils.equals(processName, baseAppId)
                || TextUtils.equals(processName, baseAppId + "." + BuildConfig.FLAVOR);
    }

    boolean isPushProcess() {
        return TextUtils.equals(getProcessName(), "cn.onestone.onestone:pushservice");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //LeakCanary.install(this);

        Watch watch = Watch.now();
        try {
            // 主进程才加载核心业务, 消息推送进程不需要加载业务
            if (isMainProcess()) {
                CoreModule.getInstance().init(getApplicationContext());

                watch.logGlanceMS(TAG, "init core module");
                UserDataService.getInstance();

                watch.logGlanceMS(TAG, "init user module");
                ImageService.getInstance();

                watch.logGlanceMS(TAG, "init image module");

                initLibraries();
                watch.logGlanceMS(TAG, "init libraries");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        watch.logTotalMS(TAG, "onCreate()");
    }

    private void initLibraries() {

        // Pretty Logger
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
                .methodCount(0)         // (Optional) How many method line to show. Default 2
                .methodOffset(7)        // (Optional) Hides internal method calls up to offset. Default 5
                .logStrategy(new LogcatLogStrategy()) // (Optional) Changes the log strategy to print out. Default LogCat
//                .tag("My custom tag")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build();
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy));

        // RxJava 异常处理
        RxJavaPlugins.setErrorHandler(e -> {
            if (e != null) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                } else {
                    Log.w("RxJava", e.getMessage() == null ? "" : e.getMessage());
                }
            }
        });

    }
}

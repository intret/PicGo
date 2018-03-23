package cn.intret.app.picgo.app

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.text.TextUtils
import android.util.Log
import cn.intret.app.picgo.BuildConfig
import cn.intret.app.picgo.di.DaggerAppComponent
import cn.intret.app.picgo.model.CoreModule
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.model.user.UserModule
import cn.intret.app.picgo.ui.main.MainActivity
import cn.intret.app.picgo.utils.Watch
import com.bumptech.glide.Glide
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.LogcatLogStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import io.reactivex.plugins.RxJavaPlugins
import org.greenrobot.eventbus.EventBus
import java.io.FileInputStream
import java.io.InputStreamReader

/**
 * Application Component
 */
class MyApp : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.builder().application(this).build()
    }

    // ------------------------------------------------
    // companion
    // ------------------------------------------------

    companion object {
        private val TAG = "MyApp"
    }

    /*
        BACKGROUND=400 EMPTY=500 FOREGROUND=100
        GONE=1000 PERCEPTIBLE=130 SERVICE=300 ISIBLE=200
    */
    val isBackground: Boolean
        get() {

            val context = applicationContext

            val activityManager = context
                    .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager
                    .runningAppProcesses
            for (appProcess in appProcesses) {
                if (appProcess.processName == context.packageName) {
                    Log.d(context.packageName, "importance ="
                            + appProcess.importance
                            + ",context.getClass().getName()="
                            + context.javaClass.name)
                    if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        Log.i(context.packageName, "处于后台" + appProcess.processName)
                        return true
                    } else {
                        Log.i(context.packageName, "处于前台" + appProcess.processName)
                        return false
                    }
                }
            }
            return false
        }

    // http://stackoverflow.com/questions/30827097/determine-by-which-process-application-oncreate-is-called
    internal// Get my Process ID
    // ignore
    // Ignore
    val processName: String
        get() {
            val myPid = android.os.Process.myPid()
            var reader: InputStreamReader? = null
            try {
                reader = InputStreamReader(FileInputStream("/proc/$myPid/cmdline"))
                val processName = StringBuilder()
                var c: Int
                c = reader.read()
                while ((c) > 0) {
                    processName.append(c.toChar())

                    c = reader.read()
                }

                return processName.toString()
            } catch (e: Exception) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: Exception) {
                    }

                }
            }
            return ""
        }

    internal val isMainProcess: Boolean
        get() {
            val baseAppId = "cn.intret.app.picgo"
            val processName = processName
            return TextUtils.equals(processName, baseAppId) || TextUtils.equals(processName, BuildConfig.APPLICATION_ID)
        }

    internal val isPushProcess: Boolean
        get() = TextUtils.equals(processName, "cn.onestone.onestone:pushservice")

    // ---------------------------------------------------------------------------------------------
    // override
    // ---------------------------------------------------------------------------------------------


    override fun onCreate() {
        super.onCreate()

        //LeakCanary.install(this);

        val watch = Watch.now()
        try {
            // 主进程才加载核心业务, 消息推送进程不需要加载业务
            if (isMainProcess) {

                initLibraries()
                watch.logGlanceMS(TAG, "init libraries")


                CoreModule.init(applicationContext)
                watch.logGlanceMS(TAG, "init core module")


                UserModule.setAppContext(applicationContext)
                watch.logGlanceMS(TAG, "init user module")


                ImageModule.setAppContext(applicationContext)
                watch.logGlanceMS(TAG, "init image module")
            } else {
                Log.w(TAG, "onCreate: not a main process")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        watch.logTotalMS(TAG, "onCreate()")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            Glide.get(this).clearMemory()
        }

        Glide.get(this).trimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()

        Glide.get(this).onLowMemory()
    }

    // ------------------------------------------------
    // init
    // ------------------------------------------------

    private fun initLibraries() {

        // EventBus
        EventBus.builder().skipMethodVerificationFor(MainActivity::class.java)
                .installDefaultEventBus()

        // Pretty Logger
        val formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
                .methodCount(0)         // (Optional) How many method line to show. Default 2
                .methodOffset(7)        // (Optional) Hides internal method calls up to offset. Default 5
                .logStrategy(LogcatLogStrategy()) // (Optional) Changes the log strategy to print out. Default LogCat
                //                .tag("My custom tag")   // (Optional) Global tag for every log. Default PRETTY_LOGGER
                .build()
        Logger.addLogAdapter(AndroidLogAdapter(formatStrategy))

        // RxJava 异常处理
        RxJavaPlugins.setErrorHandler { e ->
            if (e != null) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace()
                } else {
                    Log.w("RxJava", if (e.message == null) "" else e.message)
                }
            }
        }

    }
}

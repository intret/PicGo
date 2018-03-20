package cn.intret.app.picgo.app

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object CoreModule {

    var appContext: Context? = null

    fun init(appContext: Context) {
        this.appContext = appContext
    }
}

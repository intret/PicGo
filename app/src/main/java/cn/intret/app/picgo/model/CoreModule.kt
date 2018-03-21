package cn.intret.app.picgo.model

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object CoreModule {

    var appContext: Context? = null

    fun init(appContext: Context) {
        CoreModule.appContext = appContext
    }
}

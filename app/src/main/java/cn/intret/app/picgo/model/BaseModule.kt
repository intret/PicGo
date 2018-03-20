package cn.intret.app.picgo.model

import android.content.Context

import org.greenrobot.eventbus.EventBus

/**
 * Base service
 */

open class BaseModule {
    protected var mBus: EventBus
    protected lateinit var mContext: Context

    init {
        mBus = EventBus.getDefault()
    }

    open fun setAppContext(applicationContext: Context) {
        mContext = applicationContext
    }
}

package cn.intret.app.picgo.ui.base

import android.arch.lifecycle.LifecycleOwner
import android.content.Context

import com.uber.autodispose.AutoDisposeConverter

import cn.intret.app.picgo.app.AppComponent
import cn.intret.app.picgo.utils.RxUtils

/**
 * Created by intret on 2018/3/13.
 */

abstract class BaseLifecyclePresenter<V : LifecycleOwner> {

    protected var mAppContext: Context

    init {
        mAppContext = AppComponent.getAppContext()
    }

    abstract fun start()

    fun <T> autoDispose(lifeOwner: LifecycleOwner): AutoDisposeConverter<T> {
        return RxUtils.lifecycleDisposable(lifeOwner)
    }
}
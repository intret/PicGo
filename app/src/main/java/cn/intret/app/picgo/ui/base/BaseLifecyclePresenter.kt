package cn.intret.app.picgo.ui.base

import android.arch.lifecycle.LifecycleOwner
import cn.intret.app.picgo.utils.RxUtils
import com.uber.autodispose.AutoDisposeConverter

/**
 * Created by intret on 2018/3/13.
 */

abstract class BaseLifecyclePresenter<V : LifecycleOwner> {

    abstract fun start()

    fun <T> autoDispose(lifeOwner: LifecycleOwner): AutoDisposeConverter<T> {
        return RxUtils.lifecycleDisposable(lifeOwner)
    }
}
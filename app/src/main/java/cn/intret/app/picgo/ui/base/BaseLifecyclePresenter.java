package cn.intret.app.picgo.ui.base;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;

import com.uber.autodispose.AutoDisposeConverter;

import cn.intret.app.picgo.app.AppComponent;
import cn.intret.app.picgo.utils.RxUtils;

/**
 * Created by intret on 2018/3/13.
 */

public abstract class BaseLifecyclePresenter<V extends LifecycleOwner> {

    protected Context mAppContext;


    public BaseLifecyclePresenter() {
        mAppContext = AppComponent.getAppContext();
    }

    abstract public void start();

    public <T> AutoDisposeConverter<T> autoDispose(LifecycleOwner lifeOwner) {
        return RxUtils.lifecycleDisposable(lifeOwner);
    }
}
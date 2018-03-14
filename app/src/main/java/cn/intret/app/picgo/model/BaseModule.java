package cn.intret.app.picgo.model;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;

import cn.intret.app.picgo.app.CoreModule;
import cn.intret.app.picgo.app.RxBus;

/**
 * Base service
 */

public class BaseModule {
    protected EventBus mBus;
    protected Context mContext;

    public BaseModule() {
        mContext = CoreModule.getInstance().getAppContext();
        if (CoreModule.getInstance().getAppContext() == null) {
            throw new IllegalStateException("Please initialize the CoreModule class (CoreModule.getInstance().init(appContext).");
        }
        mBus = EventBus.getDefault();
    }
}

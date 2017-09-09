package cn.intret.app.picgo.model;

import android.content.Context;

import org.greenrobot.eventbus.EventBus;

import cn.intret.app.picgo.app.CoreModule;

/**
 * Base service
 */

class BaseService {
    EventBus mBus;
    Context mContext;

    BaseService() {
        mContext = CoreModule.getInstance().getAppContext();
        if (CoreModule.getInstance().getAppContext() == null) {
            throw new IllegalStateException("Please initialize the CoreModule class (CoreModule.getInstance().init(appContext).");
        }
        mBus = EventBus.getDefault();
    }
}

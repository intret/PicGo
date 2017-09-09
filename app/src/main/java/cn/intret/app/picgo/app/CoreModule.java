package cn.intret.app.picgo.app;

import android.content.Context;

public class CoreModule {
    private static final CoreModule ourInstance = new CoreModule();

    public static CoreModule getInstance() {
        return ourInstance;
    }

    private CoreModule() {
    }

    private Context mAppContext;
    void init(Context appContext) {
        mAppContext = appContext;
    }

    public Context getAppContext() {
        return mAppContext;
    }
}

package cn.intret.app.picgo.utils;

import io.reactivex.ObservableTransformer;
import io.reactivex.android.BuildConfig;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class RxUtils {
    public static  <T> ObservableTransformer<T, T> workAndShow() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static void unhandledThrowable(Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}

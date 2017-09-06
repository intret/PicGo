package cn.intret.app.picgo.utils;

import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by intret on 2017/9/6.
 */

public class RxUtils {
    public static  <T> ObservableTransformer<T, T> workAndShow() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}

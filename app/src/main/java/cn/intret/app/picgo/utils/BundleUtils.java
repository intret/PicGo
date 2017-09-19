package cn.intret.app.picgo.utils;

import android.os.Bundle;

import java.util.function.Consumer;

public class BundleUtils {
    public static <T> void readBundle(Bundle bundle, String bundleKey, Consumer<T> value) {
        if (bundle != null && bundle.containsKey(bundleKey)) {
            if (value != null) {
                value.accept(ObjectUtils.cast(bundle.get(bundleKey)));
            }
        }
    }
}

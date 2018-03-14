package cn.intret.app.picgo.app;

import com.hwangjr.rxbus.Bus;

/**
 * Created by intret on 2018/3/14.
 */

public class RxBusImage {
    private static Bus sBus;

    public static synchronized Bus get() {
        if (sBus == null) {
            sBus = new Bus();
        }
        return sBus;
    }
}

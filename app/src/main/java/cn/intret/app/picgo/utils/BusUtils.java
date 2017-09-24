package cn.intret.app.picgo.utils;


import org.greenrobot.eventbus.EventBus;

public class BusUtils {

    /*
     * EventBus 消息接收优先级.
     */

    public static final int PRIORITY_CONTACT_LIST = 50;
    public static final int PRIORITY_MEETING_LIST = 40;
    public static final int PRIORITY_MESSAGE_LIST = 30;
    public static final int PRIORITY_GROUP_LIST = 20;
    public static final int PRIORITY_SETTING_LIST = 10;

    public static void register(Object object) {
        if (!EventBus.getDefault().isRegistered(object)) {
            EventBus.getDefault().register(object);
        }
    }

    public static void unregister(Object object) {
        if (EventBus.getDefault().isRegistered(object)) {
            EventBus.getDefault().unregister(object);
        }
    }

    public static void postSticky(Object object) {
        EventBus.getDefault().postSticky(object);
    }

    public static void post(Object object) {
        EventBus.getDefault().post(object);
    }
}

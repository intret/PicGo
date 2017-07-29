package cn.intret.app.picgo.utils;

import java.util.List;

public class ListUtils {

    public static <T> T firstOf(List<T> list) {
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public static <T> T lastOf(List<T> list) {
        if (list == null) {
            return null;
        }

        if (list.isEmpty()) {
            return null;
        }

        return list.get(list.size() - 1);
    }
}
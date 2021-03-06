package cn.intret.app.picgo.utils;

import org.apache.commons.collections4.Predicate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ListUtils {

    public static <T> boolean isEmpty(List<T> list) {
        return list == null || list.isEmpty();
    }

    public static <T> int sizeOf(List<T> list) {
        return list == null ? 0 : list.size();
    }

    public static <T> ArrayList<T> objectToArrayList(T object) {
        ArrayList<T> objects = new ArrayList<>();
        objects.add(object);
        return objects;
    }

    public static <T> LinkedList<T> objectToLinkedList(T object) {
        LinkedList<T> objects = new LinkedList<>();
        objects.add(object);
        return objects;
    }

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

    public static <E> boolean removeListElement(List<E> list, Predicate<E> predicate) {
        int i = org.apache.commons.collections4.ListUtils.indexOf(list, predicate);
        if (i != -1) {
            list.remove(i);
            return true;
        }
        return false;
    }
}

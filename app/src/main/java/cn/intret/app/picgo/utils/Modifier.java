package cn.intret.app.picgo.utils;

public interface Modifier<T> {
    T onModify(T data);
}

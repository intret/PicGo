package cn.intret.app.picgo.utils;

/**
 * Reference
 */

public class ReferenceWrapper<T> {
    T mVal;
    ReferenceWrapper(T val) {
        mVal = val;
    }

    public T getVal() {
        return mVal;
    }

    public void setVal(T val) {
        mVal = val;
    }
}

package cn.intret.app.picgo.utils;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ObjectGuarder<T> {

    ReadWriteLock mLock = new ReentrantReadWriteLock();
    T mObject;

    public ObjectGuarder(T object) {
        mObject = object;
    }

    public interface Consumer<T> {
        void accept(T object);
    }

    public void readLock() {
        mLock.readLock().lock();
    }

    public void readUnlock() {
        mLock.readLock().unlock();
    }

    public void writeLock() {
        mLock.writeLock().lock();
    }

    public void writeUnLock() {
        mLock.writeLock().unlock();
    }

    public T getObject() {
        return mObject;
    }

    public ObjectGuarder setObject(T object) {
        mObject = object;
        return this;
    }

    public void readConsume(Consumer<T> consumer) {
        if (consumer == null) {
            throw new NullPointerException("consumer should not be null.");
        }

        try {
            mLock.readLock().lock();
            consumer.accept(mObject);
        } finally {
            mLock.readLock().unlock();
        }
    }

    public void writeConsume(Consumer<T> consumer) {
        if (consumer == null) {
            throw new NullPointerException("consumer should not be null.");
        }

        try {
            mLock.writeLock().lock();
            consumer.accept(mObject);
        } finally {
            mLock.writeLock().unlock();
        }
    }
}

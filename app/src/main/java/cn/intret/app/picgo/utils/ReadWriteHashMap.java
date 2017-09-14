package cn.intret.app.picgo.utils;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

public class ReadWriteHashMap<K,V> extends HashMap<K,V> {

    ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();

    public ReadWriteHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ReadWriteHashMap(int initialCapacity) {
        super(initialCapacity);
    }

    public ReadWriteHashMap() {
    }

    public ReadWriteHashMap(Map m) {
        super(m);
    }

    interface Consumer<K,V> {
        void accept(ReadWriteHashMap<K, V> map);
    }


    public void writeConsumeMap(Consumer<K,V> action) {
        if (action == null) {
            throw new NullPointerException("Action is null");
        }

        mReadWriteLock.readLock().lock();
        action.accept(this);
        mReadWriteLock.readLock().unlock();
    }
    public void readConsumeMap(Consumer<K,V> action) {
        if (action == null) {
            throw new NullPointerException("Action is null");
        }

        mReadWriteLock.writeLock().lock();
        action.accept(this);
        mReadWriteLock.writeLock().unlock();
    }
}

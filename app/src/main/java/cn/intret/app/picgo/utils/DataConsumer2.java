package cn.intret.app.picgo.utils;

public interface DataConsumer2<D1,D2> {
    void accept(D1 data1, D2 data2);
}


package cn.intret.app.picgo.model;

public class LoadMediaFileParam {
    boolean mFromCacheFirst = true;
    boolean mLoadMediaInfo = false;
    SortWay mSortWay = SortWay.DATE;
    SortOrder mSortOrder = SortOrder.DESC;

    public boolean isFromCacheFirst() {
        return mFromCacheFirst;
    }

    public LoadMediaFileParam setFromCacheFirst(boolean fromCacheFirst) {
        mFromCacheFirst = fromCacheFirst;
        return this;
    }

    public boolean isLoadMediaInfo() {
        return mLoadMediaInfo;
    }

    public LoadMediaFileParam setLoadMediaInfo(boolean loadMediaInfo) {
        mLoadMediaInfo = loadMediaInfo;
        return this;
    }

    public SortWay getSortWay() {
        return mSortWay;
    }

    public LoadMediaFileParam setSortWay(SortWay sortWay) {
        mSortWay = sortWay;
        return this;
    }

    public SortOrder getSortOrder() {
        return mSortOrder;
    }

    public LoadMediaFileParam setSortOrder(SortOrder sortOrder) {
        mSortOrder = sortOrder;
        return this;
    }
}

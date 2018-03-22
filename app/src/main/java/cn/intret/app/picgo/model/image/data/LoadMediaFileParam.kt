package cn.intret.app.picgo.model.image.data

import cn.intret.app.picgo.model.user.data.SortOrder
import cn.intret.app.picgo.model.user.data.SortWay

class LoadMediaFileParam {
    internal var mFromCacheFirst = true
    internal var mLoadMediaInfo = false
    internal var mSortWay = SortWay.DATE
    internal var mSortOrder = SortOrder.DESC

    fun isFromCacheFirst(): Boolean {
        return mFromCacheFirst
    }

    fun setFromCacheFirst(fromCacheFirst: Boolean): LoadMediaFileParam {
        mFromCacheFirst = fromCacheFirst
        return this
    }

    fun isLoadMediaInfo(): Boolean {
        return mLoadMediaInfo
    }

    fun setLoadMediaInfo(loadMediaInfo: Boolean): LoadMediaFileParam {
        mLoadMediaInfo = loadMediaInfo
        return this
    }

    fun getSortWay(): SortWay {
        return mSortWay
    }

    fun setSortWay(sortWay: SortWay): LoadMediaFileParam {
        mSortWay = sortWay
        return this
    }

    fun getSortOrder(): SortOrder {
        return mSortOrder
    }

    fun setSortOrder(sortOrder: SortOrder): LoadMediaFileParam {
        mSortOrder = sortOrder
        return this
    }

    override fun toString(): String {
        return "LoadMediaFileParam{" +
                "mFromCacheFirst=" + mFromCacheFirst +
                ", mLoadMediaInfo=" + mLoadMediaInfo +
                ", sortWay=" + mSortWay +
                ", sortOrder=" + mSortOrder +
                '}'.toString()
    }
}

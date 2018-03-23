package cn.intret.app.picgo.ui.main

import cn.intret.app.picgo.model.image.data.GroupMode
import cn.intret.app.picgo.model.user.data.SortOrder
import cn.intret.app.picgo.model.user.data.SortWay
import cn.intret.app.picgo.model.user.data.ViewMode

/**
 * Created by intret on 2017/11/1.
 */

class ListViewState : Cloneable {
    /**
     * 图片分组模式
     */
    internal var mGroupMode = GroupMode.DEFAULT

    internal var mSortOrder = SortOrder.UNKNOWN
    internal var mSortWay = SortWay.UNKNOWN
    internal var mViewMode = ViewMode.UNKNOWN

    fun getGroupMode(): GroupMode {
        return mGroupMode
    }

    fun setGroupMode(groupMode: GroupMode): ListViewState {
        mGroupMode = groupMode
        return this
    }

    fun getSortWay(): SortWay {
        return mSortWay
    }

    fun setSortWay(sortWay: SortWay): ListViewState {
        mSortWay = sortWay
        return this
    }

    fun getViewMode(): ViewMode {
        return mViewMode
    }

    fun setViewMode(viewMode: ViewMode): ListViewState {
        mViewMode = viewMode
        return this
    }

    fun getSortOrder(): SortOrder {
        return mSortOrder
    }

    fun setSortOrder(sortOrder: SortOrder): ListViewState {
        mSortOrder = sortOrder
        return this
    }

    public override fun clone(): Any {
        var clone : Any
        try {
            clone = super.clone()
        } finally {
        }
        return clone
    }

    override fun toString(): String {
        return "ListViewState{" +
                "mGroupMode=" + mGroupMode +
                ", sortOrder=" + mSortOrder +
                ", sortWay=" + mSortWay +
                ", viewMode=" + mViewMode +
                '}'.toString()
    }
}

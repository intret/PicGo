package cn.intret.app.picgo.ui.main;

import cn.intret.app.picgo.model.image.GroupMode;
import cn.intret.app.picgo.model.user.SortOrder;
import cn.intret.app.picgo.model.user.SortWay;
import cn.intret.app.picgo.model.user.ViewMode;

/**
 * Created by intret on 2017/11/1.
 */

public class ListViewState implements Cloneable {
    /**
     * 图片分组模式
     */
    GroupMode mGroupMode = GroupMode.DEFAULT;

    SortOrder mSortOrder = SortOrder.UNKNOWN;
    SortWay mSortWay = SortWay.UNKNOWN;
    ViewMode mViewMode = ViewMode.UNKNOWN;

    public GroupMode getGroupMode() {
        return mGroupMode;
    }

    public ListViewState setGroupMode(GroupMode groupMode) {
        mGroupMode = groupMode;
        return this;
    }

    public SortWay getSortWay() {
        return mSortWay;
    }

    public ListViewState setSortWay(SortWay sortWay) {
        mSortWay = sortWay;
        return this;
    }

    public ViewMode getViewMode() {
        return mViewMode;
    }

    public ListViewState setViewMode(ViewMode viewMode) {
        mViewMode = viewMode;
        return this;
    }

    public SortOrder getSortOrder() {
        return mSortOrder;
    }

    public ListViewState setSortOrder(SortOrder sortOrder) {
        mSortOrder = sortOrder;
        return this;
    }

    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "ListViewState{" +
                "mGroupMode=" + mGroupMode +
                ", mSortOrder=" + mSortOrder +
                ", mSortWay=" + mSortWay +
                ", mViewMode=" + mViewMode +
                '}';
    }
}

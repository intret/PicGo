package cn.intret.app.picgo.model;

import java.util.List;

/**
 * 用户初始偏好设置，包含 UI 决定如何进行首次显示的偏好设置。
 */
public class UserInitialPreferences {
    List<RecentRecord> mRecentRecords;
    ViewMode mViewMode;
    SortWay mSortWay;
    SortOrder mSortOrder;

    public List<RecentRecord> getRecentRecords() {
        return mRecentRecords;
    }

    public UserInitialPreferences setRecentRecords(List<RecentRecord> recentRecords) {
        mRecentRecords = recentRecords;
        return this;
    }

    public ViewMode getViewMode() {
        return mViewMode;
    }

    public UserInitialPreferences setViewMode(ViewMode viewMode) {
        mViewMode = viewMode;
        return this;
    }

    public SortWay getSortWay() {
        return mSortWay;
    }

    public UserInitialPreferences setSortWay(SortWay sortWay) {
        mSortWay = sortWay;
        return this;
    }

    public SortOrder getSortOrder() {
        return mSortOrder;
    }

    public UserInitialPreferences setSortOrder(SortOrder sortOrder) {
        mSortOrder = sortOrder;
        return this;
    }
}

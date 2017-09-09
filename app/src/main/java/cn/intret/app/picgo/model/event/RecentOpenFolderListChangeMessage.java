package cn.intret.app.picgo.model.event;

import java.util.List;

import cn.intret.app.picgo.model.RecentRecord;

public class RecentOpenFolderListChangeMessage {
    private List<RecentRecord> mRecentRecord;

    public RecentOpenFolderListChangeMessage setRecentRecord(List<RecentRecord> recentRecord) {
        mRecentRecord = recentRecord;
        return this;
    }

    public List<RecentRecord> getRecentRecord() {
        return mRecentRecord;
    }

    @Override
    public String toString() {
        return "RecentOpenFolderListChangeMessage{" +
                "mRecentRecord=" + mRecentRecord +
                '}';
    }
}

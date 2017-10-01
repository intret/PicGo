package cn.intret.app.picgo.model.event;

import java.util.List;

import cn.intret.app.picgo.model.user.RecentRecord;

public class RecentOpenFolderListChangeMessage extends BaseMessage {
    private List<RecentRecord> mRecentRecord;

    public RecentOpenFolderListChangeMessage(String desc, List<RecentRecord> recentRecord) {
        super(desc);
        mRecentRecord = recentRecord;
    }

    public RecentOpenFolderListChangeMessage() {
    }

    public RecentOpenFolderListChangeMessage(List<RecentRecord> recentRecord) {
        mRecentRecord = recentRecord;
    }

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
                "} " + super.toString();
    }
}

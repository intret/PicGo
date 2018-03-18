package cn.intret.app.picgo.model.event

import cn.intret.app.picgo.model.user.RecentRecord

class RecentOpenFolderListChangeMessage : BaseMessage {
    private var mRecentRecord: List<RecentRecord>? = null

    constructor(desc: String, recentRecord: List<RecentRecord>) : super(desc) {
        mRecentRecord = recentRecord
    }

    constructor()

    constructor(recentRecord: List<RecentRecord>) {
        mRecentRecord = recentRecord
    }

    fun setRecentRecord(recentRecord: List<RecentRecord>): RecentOpenFolderListChangeMessage {
        mRecentRecord = recentRecord
        return this
    }

    fun getRecentRecord(): List<RecentRecord>? {
        return mRecentRecord
    }

    override fun toString(): String {
        return "RecentOpenFolderListChangeMessage{" +
                "mRecentRecord=" + mRecentRecord +
                "} " + super.toString()
    }
}

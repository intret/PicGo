package cn.intret.app.picgo.model.user.data

/**
 * 用户初始偏好设置，包含 app 使用最后状态的 UI 布局偏好设置。
 */
class UserInitialPreferences {
    lateinit var recentRecords: List<RecentRecord>
    var viewMode: ViewMode = ViewMode.UNKNOWN
    var sortWay: SortWay = SortWay.UNKNOWN
    var sortOrder: SortOrder = SortOrder.UNKNOWN

    override fun toString(): String {
        return "UserInitialPreferences{" +
                "recentRecords=" + recentRecords +
                ", viewMode=" + viewMode +
                ", sortWay=" + sortWay +
                ", sortOrder=" + sortOrder +
                '}'.toString()
    }
}

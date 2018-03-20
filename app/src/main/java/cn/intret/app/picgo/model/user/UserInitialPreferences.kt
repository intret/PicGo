package cn.intret.app.picgo.model.user

/**
 * 用户初始偏好设置，包含 UI 布局偏好设置，包括关闭程序之前。
 */
class UserInitialPreferences {
    lateinit var recentRecords: List<RecentRecord>
    var viewMode: ViewMode = ViewMode.UNKNOWN
    var sortWay: SortWay = SortWay.UNKNOWN
    var sortOrder: SortOrder =  SortOrder.UNKNOWN

    override fun toString(): String {
        return "UserInitialPreferences{" +
                "recentRecords=" + recentRecords +
                ", viewMode=" + viewMode +
                ", sortWay=" + sortWay +
                ", sortOrder=" + sortOrder +
                '}'.toString()
    }
}

package cn.intret.app.picgo.model.user


enum class ViewMode {
    GRID_VIEW,
    LIST_VIEW,
    UNKNOWN;

    override fun toString(): String {
        when (this) {
            LIST_VIEW -> return "LIST_VIEW"
            GRID_VIEW -> return "GRID_VIEW"
        }
        return "UNKNOWN"
    }

    companion object {

        fun fromString(string: String?): ViewMode {
            if (string == null) {
                return UNKNOWN
            }
            when (string) {
                "GRID_VIEW" -> return GRID_VIEW
                "LIST_VIEW" -> return LIST_VIEW
            }
            return UNKNOWN
        }
    }
}

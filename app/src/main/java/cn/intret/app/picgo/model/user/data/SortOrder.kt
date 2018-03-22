package cn.intret.app.picgo.model.user.data

/**
 * Image list sort order
 */
enum class SortOrder {
    DESC,
    ASC,
    UNKNOWN;

    override fun toString(): String {
        when (this) {
            ASC -> return "ASC"
            DESC -> return "DESC"
        }
        return "UNKNOWN"
    }

    companion object {
        fun fromString(string: String?): SortOrder {
            if (string == null) {
                return UNKNOWN
            }
            when (string) {
                "DESC" -> return DESC
                "ASC" -> return ASC
            }
            return UNKNOWN
        }
    }
}

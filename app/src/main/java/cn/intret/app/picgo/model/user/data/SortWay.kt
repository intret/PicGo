package cn.intret.app.picgo.model.user.data

/**
 * Image list sort way
 */
enum class SortWay {
    NAME,
    SIZE,
    DATE,
    UNKNOWN;

    override fun toString(): String {
        when (this) {
            NAME -> return "NAME"
            SIZE -> return "SIZE"
            DATE -> return "DATE"
        }
        return "UNKNOWN"
    }

    companion object {
        fun fromString(string: String?): SortWay {
            if (string == null) {
                return UNKNOWN
            }
            when (string) {
                "NAME" -> return NAME
                "SIZE" -> return SIZE
                "DATE" -> return DATE
            }
            return UNKNOWN
        }
    }
}

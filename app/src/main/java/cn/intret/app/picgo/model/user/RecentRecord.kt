package cn.intret.app.picgo.model.user

import com.google.gson.annotations.SerializedName

class RecentRecord {

    @SerializedName("file_path") internal lateinit var mFilePath: String

    var filePath: String? = null
        get() = mFilePath

    fun setFilePath(filePath: String): RecentRecord {
        mFilePath = filePath
        return this
    }

    override fun toString(): String {
        return "RecentRecord{" +
                "mFilePath='" + mFilePath + '\''.toString() +
                '}'.toString()
    }
}

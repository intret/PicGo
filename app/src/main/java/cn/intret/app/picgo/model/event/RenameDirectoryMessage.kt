package cn.intret.app.picgo.model.event

import java.io.File

/**
 * 重命名目录消息
 */

class RenameDirectoryMessage {
    private var mOldDirectory: File? = null
    private var mNewDirectory: File? = null

    fun setOldDirectory(oldDirectory: File): RenameDirectoryMessage {
        mOldDirectory = oldDirectory
        return this
    }

    fun getOldDirectory(): File? {
        return mOldDirectory
    }

    fun setNewDirectory(newDirectory: File): RenameDirectoryMessage {
        mNewDirectory = newDirectory
        return this
    }

    fun getNewDirectory(): File? {
        return mNewDirectory
    }

    override fun toString(): String {
        return "RenameDirectoryMessage{" +
                "mOldDirectory=" + mOldDirectory +
                ", mNewDirectory=" + mNewDirectory +
                '}'.toString()
    }
}

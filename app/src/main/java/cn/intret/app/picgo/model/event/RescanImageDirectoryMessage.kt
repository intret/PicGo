package cn.intret.app.picgo.model.event

import java.io.File

/**
 * 文件夹已经被重新扫描的消息
 */

class RescanImageDirectoryMessage : BaseMessage {
    private var mDirectory: File? = null

    constructor()

    constructor(directory: File) {
        mDirectory = directory
    }

    constructor(desc: String, directory: File) : super(desc) {
        mDirectory = directory
    }

    fun setDirectory(directory: File): RescanImageDirectoryMessage {
        mDirectory = directory
        return this
    }

    fun getDirectory(): File? {
        return mDirectory
    }

    override fun toString(): String {
        return "RescanImageDirectoryMessage{" +
                "mDirectory=" + mDirectory +
                "} " + super.toString()
    }
}

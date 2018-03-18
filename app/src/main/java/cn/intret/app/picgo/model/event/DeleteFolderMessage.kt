package cn.intret.app.picgo.model.event


import java.io.File

/**
 * A folder has been delete.
 */
class DeleteFolderMessage : BaseMessage {
    internal var mDir: File


    constructor(desc: String, dir: File) : super(desc) {
        mDir = dir
    }

    constructor(dir: File) {
        mDir = dir
    }

    fun getDir(): File {
        return mDir
    }

    fun setDir(dir: File): DeleteFolderMessage {
        mDir = dir
        return this
    }

    override fun toString(): String {
        return "DeleteFolderMessage{" +
                "mDir=" + mDir +
                "} " + super.toString()
    }
}

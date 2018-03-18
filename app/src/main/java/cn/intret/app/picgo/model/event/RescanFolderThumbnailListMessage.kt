package cn.intret.app.picgo.model.event

import java.io.File

/**
 * 目录的缩略图被更新时发送
 */
class RescanFolderThumbnailListMessage {
    internal lateinit var mDirectory: File
    internal lateinit var mThumbnails: List<File>

    fun getDirectory(): File {
        return mDirectory
    }

    fun setDirectory(directory: File): RescanFolderThumbnailListMessage {
        this.mDirectory = directory
        return this
    }

    fun getThumbnails(): List<File> {
        return mThumbnails
    }

    fun setThumbnails(thumbnails: List<File>): RescanFolderThumbnailListMessage {
        mThumbnails = thumbnails
        return this
    }

    override fun toString(): String {
        return "RescanFolderThumbnailListMessage{" +
                "mDirectory=" + mDirectory +
                '}'.toString()
    }
}

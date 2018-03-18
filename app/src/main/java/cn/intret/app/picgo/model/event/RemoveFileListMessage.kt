package cn.intret.app.picgo.model.event

import java.io.File

internal class RemoveFileListMessage {
    lateinit var removedFiles: List<File>
    lateinit var unRemovedFiles: List<File>
    lateinit var mDir: File
    var mTotal: Int = 0

    fun setRemoveSuccessFiles(removedFiles: List<File>): RemoveFileListMessage {
        this.removedFiles = removedFiles
        return this
    }

    fun setRemoveFailedFiles(unRemovedFiles: List<File>): RemoveFileListMessage {
        this.unRemovedFiles = unRemovedFiles
        return this
    }

    fun getDir(): File {
        return mDir
    }

    fun setDir(dir: File): RemoveFileListMessage {
        mDir = dir
        return this
    }

    fun getTotal(): Int {
        return mTotal
    }

    fun setTotal(total: Int): RemoveFileListMessage {
        mTotal = total
        return this
    }
}

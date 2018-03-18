package cn.intret.app.picgo.model.event

import java.io.File

/**
 * Remove file notification message
 */

class RemoveFileMessage {
    private lateinit var mFile: File

    fun setFile(file: File): RemoveFileMessage {
        mFile = file
        return this
    }

    fun getFile(): File {
        return mFile
    }
}

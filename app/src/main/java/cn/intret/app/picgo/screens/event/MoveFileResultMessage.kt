package cn.intret.app.picgo.screens.event

import java.io.File

import cn.intret.app.picgo.model.image.MoveFileResult

class MoveFileResultMessage {
    private var mResult: MoveFileResult? = null
    private var mDestDir: File? = null

    fun setResult(result: MoveFileResult): MoveFileResultMessage {
        mResult = result
        return this
    }

    fun getResult(): MoveFileResult? {
        return mResult
    }

    fun setDestDir(destDir: File): MoveFileResultMessage {
        mDestDir = destDir
        return this
    }

    fun getDestDir(): File? {
        return mDestDir
    }
}

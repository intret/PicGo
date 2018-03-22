package cn.intret.app.picgo.screens.event

import cn.intret.app.picgo.model.image.data.MoveFileResult
import java.io.File

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

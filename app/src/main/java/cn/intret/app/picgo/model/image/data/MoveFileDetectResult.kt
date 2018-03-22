package cn.intret.app.picgo.model.image.data

import android.util.Pair

import java.io.File


class MoveFileDetectResult {

    private var targetDir: File? = null
    internal var canMoveFiles: List<Pair<File, File>>? = null
    var conflictFiles: List<Pair<File, File>>? = null

    fun getTargetDir(): File? {
        return targetDir
    }

    fun setTargetDir(targetDir: File): MoveFileDetectResult {
        this.targetDir = targetDir
        return this
    }

    fun setCanMoveFiles(canMoveFiles: List<Pair<File, File>>): MoveFileDetectResult {
        this.canMoveFiles = canMoveFiles
        return this
    }

    fun getCanMoveFiles(): List<Pair<File, File>>? {
        return canMoveFiles
    }

    fun setConflictFiles(conflictFiles: List<Pair<File, File>>): MoveFileDetectResult {
        this.conflictFiles = conflictFiles
        return this
    }
}

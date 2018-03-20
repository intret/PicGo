package cn.intret.app.picgo.model.image

import android.util.Pair

import java.io.File
import java.util.LinkedList


class MoveFileResult {
    internal var conflictFiles: List<Pair<File, File>> = LinkedList()

    internal var successFiles: List<Pair<File, File>> = LinkedList()

    internal var failedFiles: List<Pair<File, File>> = LinkedList()
}

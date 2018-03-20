package cn.intret.app.picgo.model.image


import java.io.File
import java.util.LinkedHashMap

class DetectFileExistenceResult {
    /**
     * Key: folder
     * Value: existed files
     */
    var existedFiles: LinkedHashMap<File, List<File>>? = LinkedHashMap()


    fun setExistedFiles(existedFiles: LinkedHashMap<File, List<File>>?): DetectFileExistenceResult {
        this.existedFiles = existedFiles
        return this
    }

    override fun toString(): String {
        return "DetectFileExistenceResult{" +
                "existedFiles=" + existedFiles +
                '}'.toString()
    }

    init {
        existedFiles = LinkedHashMap()
    }
}

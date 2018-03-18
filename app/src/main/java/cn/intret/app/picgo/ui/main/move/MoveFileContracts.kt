package cn.intret.app.picgo.ui.main.move

import java.io.File

import cn.intret.app.picgo.model.image.DetectFileExistenceResult
import cn.intret.app.picgo.model.image.FolderModel

/**
 * Contracts of MoveFile's MVP design pattern
 */

class MoveFileContracts {
    interface View {

        fun onLoadFolderModelSuccess(folderModel: FolderModel)

        fun onLoadFolderModelFailed()

        fun onDetectFileExistenceResult(detectFileExistenceResult: DetectFileExistenceResult)

        fun onDetectFileExistenceFailed(sourceFiles: List<File>?)
    }

    interface Presenter {
        fun loadFolderList()

        fun detectFileExistence(sourceFiles: List<File>?)
    }
}

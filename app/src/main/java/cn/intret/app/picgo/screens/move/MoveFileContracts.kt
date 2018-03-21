package cn.intret.app.picgo.screens.move

import cn.intret.app.picgo.model.image.DetectFileExistenceResult
import cn.intret.app.picgo.model.image.FolderModel
import java.io.File

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

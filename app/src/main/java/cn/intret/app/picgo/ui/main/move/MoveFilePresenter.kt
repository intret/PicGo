package cn.intret.app.picgo.ui.main.move

import android.arch.lifecycle.LifecycleOwner
import cn.intret.app.picgo.model.image.DetectFileExistenceResult
import cn.intret.app.picgo.model.image.FolderModel
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.ui.base.BaseLifecyclePresenter
import cn.intret.app.picgo.utils.RxUtils
import java.io.File

/**
 * Presenter role of MoveFile dialog's MPV design pattern
 */

class MoveFilePresenter<V>(var mView: V)
    : BaseLifecyclePresenter<V>(), MoveFileContracts.Presenter
        where V : MoveFileContracts.View, V : LifecycleOwner {

    override fun loadFolderList() {

        var lifecycleDisposable =
        ImageModule
                .loadFolderModel(true)

                .compose(RxUtils.applySchedulers())
                .`as`(RxUtils.lifecycleDisposable(mView))

                .subscribe(
                        { folderModel: FolderModel -> mView.onLoadFolderModelSuccess(folderModel) },
                        { throwable: Throwable -> mView.onLoadFolderModelFailed() })
    }

    override fun detectFileExistence(sourceFiles: List<File>?) {
        sourceFiles?.let {
            ImageModule
                    .detectFileExistence(it)
                    .compose<DetectFileExistenceResult>(RxUtils.applySchedulers<DetectFileExistenceResult>())
                    .`as`(RxUtils.lifecycleDisposable<DetectFileExistenceResult>(mView))
                    .subscribe(
                            { detectFileExistenceResult: DetectFileExistenceResult ->
                                mView.onDetectFileExistenceResult(detectFileExistenceResult)
                            },
                            { throwable: Throwable ->
                                mView.onDetectFileExistenceFailed(sourceFiles)
                            })

        }
    }


    override fun start() {

    }
}

package cn.intret.app.picgo.ui.main

import android.arch.lifecycle.LifecycleOwner
import android.util.Log
import cn.intret.app.picgo.R
import cn.intret.app.picgo.model.NotEmptyException
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.model.image.LoadMediaFileParam
import cn.intret.app.picgo.model.user.SortOrder
import cn.intret.app.picgo.model.user.SortWay
import cn.intret.app.picgo.model.user.UserModule
import cn.intret.app.picgo.ui.base.BaseLifecyclePresenter
import cn.intret.app.picgo.utils.RxUtils
import io.reactivex.Observable
import java.io.File
import javax.inject.Inject

/**
 * Presenter role of Main page's MVP design pattern.
 *
 * @param <V> View type, Fragment or Activity who implemented [LifecycleOwner]
*/
class MainPresenter<V> @Inject constructor (internal var mView: V)
    : BaseLifecyclePresenter<V>(), MainContract.Presenter
        where V : MainContract.View, V : LifecycleOwner {


    /**
     * @see MainContract.View.onLoadedUserInitialPreferences
     */
    override fun loadInitialPreference() {
        UserModule
                .loadInitialPreference(true)
                .compose(RxUtils.applySchedulers())
                .doOnNext { userInitialPreferences -> Log.d(TAG, "Loaded user initial preference : $userInitialPreferences") }
                .`as`(autoDispose(mView))
                .subscribe({ userInitialPreferences -> mView.onLoadedUserInitialPreferences(userInitialPreferences) }, { throwable -> mView.onErrorMessage(R.string.failed_to_load_preference) })
    }

    /**
     *
     * @param dir
     * @param forceDelete
     * @see MainContract.View.onDeletedDirectory
     * @see MainContract.View.onDeleteDirectoryFailed
     * @see MainContract.View.onDeleteNotEmptyDirectoryFailed
     */
    override fun removeDirectory(dir: File, forceDelete: Boolean) {

        ImageModule
                .removeFolder(dir, forceDelete)
                .compose(RxUtils.applySchedulers())
                .`as`(RxUtils.lifecycleDisposable(mView))
                .subscribe({ deleted ->
                    if (deleted) {
                        mView.onDeletedDirectory(dir)
                    } else {
                        mView.onDeleteDirectoryFailed(dir, null!!)
                        Log.e(TAG, "removeDirectory: illegal state")
                    }
                }, { throwable ->

                    if (throwable is NotEmptyException) {
                        Observable.fromCallable { dir.list().size }
                                .compose(RxUtils.applySchedulers())
                                .subscribe { fileCount -> mView.onDeleteNotEmptyDirectoryFailed(dir, fileCount) }
                    } else {
                        mView.onDeleteDirectoryFailed(dir, throwable)
                    }
                })
    }


    /**
     *
     * @param fromCacheFirst
     * @see MainContract.View.onLoadedFolderModel
     */
    override fun loadFolderList(fromCacheFirst: Boolean) {
        ImageModule
                .loadFolderModel(fromCacheFirst)
                .compose(RxUtils.applySchedulers())
                .`as`(RxUtils.lifecycleDisposable(mView))
                .subscribe({ folderModel -> mView.onLoadedFolderModel(folderModel) },
                        { RxUtils.unhandledThrowable(it) })
    }

    override fun loadFolderList(fromCacheFirst: Boolean, diff: Boolean) {
        ImageModule
                .loadFolderModel(fromCacheFirst)
                .compose(RxUtils.applySchedulers())
                .`as`(RxUtils.lifecycleDisposable(mView))
                .subscribe({ folderModel -> mView.onLoadedFolderModel(folderModel, diff) },
                        { throwable -> mView.onLoadFolderModelFailed(throwable) })
    }

    override fun diffLoadMediaFileList(dir: File, fromCacheFirst: Boolean, sortWay: SortWay, sortOrder: SortOrder, hideRefreshControl: Boolean,
                                       purpose: MainContract.LoadImageListPurpose) {
        var loadMediaInfo = false
        if (purpose === MainContract.LoadImageListPurpose.RefreshDetailList) {
            loadMediaInfo = true
        }
        ImageModule
                .loadMediaFileList(dir,
                        LoadMediaFileParam()
                                .setFromCacheFirst(fromCacheFirst)
                                .setLoadMediaInfo(loadMediaInfo)
                                .setSortWay(sortWay)
                                .setSortOrder(sortOrder)
                )
                .compose(RxUtils.applySchedulers())
                .`as`(RxUtils.lifecycleDisposable(mView))
                .subscribe({ mediaFiles -> mView.onDiffDetailMediaFiles(dir, mediaFiles, hideRefreshControl, purpose) }, { throwable -> mView.onDiffLoadDetailMediaFileFailed(dir, hideRefreshControl, purpose) })
    }

    override fun diffLoadDefaultMediaFileList(dir: File, fromCacheFirst: Boolean, sortWay: SortWay, sortOrder: SortOrder, hideRefreshControl: Boolean) {
        ImageModule
                .loadMediaFileList(dir,
                        LoadMediaFileParam()
                                .setFromCacheFirst(fromCacheFirst)
                                .setLoadMediaInfo(true)
                                .setSortWay(sortWay)
                                .setSortOrder(sortOrder)
                )
                .compose(RxUtils.applySchedulers())
                .`as`(RxUtils.lifecycleDisposable(mView))
                .subscribe({ mediaFiles -> mView.onDiffLoadedDefaultMediaFiles(dir, mediaFiles, hideRefreshControl) }, { throwable -> mView.onDiffLoadDefaultMediaFileFailed(dir, hideRefreshControl) })
    }

    override fun updateFolderListItemThumbnailList(directory: File) {
        if (directory == null) {
            Log.e(TAG, "updateFolderListItemThumbnailList: directory is null/empty")
            return
        }
        ImageModule
                .rescanDirectoryThumbnailList(directory)
                .compose(RxUtils.applySchedulers())
                .`as`(RxUtils.lifecycleDisposable(mView))
                .subscribe({ files ->

                }, { RxUtils.unhandledThrowable(it) })
    }


    override fun start() {

    }

    companion object {

        private val TAG = "MainPresenter"
    }
}

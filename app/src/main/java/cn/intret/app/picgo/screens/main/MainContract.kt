package cn.intret.app.picgo.screens.main

import android.support.annotation.StringRes

import java.io.File

import cn.intret.app.picgo.model.image.FolderModel
import cn.intret.app.picgo.model.image.MediaFile
import cn.intret.app.picgo.model.user.SortOrder
import cn.intret.app.picgo.model.user.SortWay
import cn.intret.app.picgo.model.user.UserInitialPreferences

/**
 * Created by intret on 2018/3/13.
 */

class MainContract {

    enum class LoadImageListPurpose {
        RefreshDetailList,
        RefreshDefaultList
    }

    interface View {
        fun onLoadedUserInitialPreferences(userInitialPreferences: UserInitialPreferences)

        fun onErrorMessage(@StringRes msg: Int)

        fun onLoadedFolderModel(folderModel: FolderModel)

        fun onLoadedFolderModel(folderModel: FolderModel, diff: Boolean)

        fun onLoadFolderModelFailed(throwable: Throwable)

        fun onDeletedDirectory(dir: File)

        fun onDeleteDirectoryFailed(dir: File, throwable: Throwable)

        fun onDeleteNotEmptyDirectoryFailed(dir: File, fileCount: Int?)

        fun onDiffDetailMediaFiles(dir: File, mediaFiles: List<MediaFile>, hideRefreshControl: Boolean, purpose: LoadImageListPurpose)

        fun onDiffLoadDetailMediaFileFailed(dir: File, hideRefreshControl: Boolean, purpose: LoadImageListPurpose)

        fun onDiffLoadedDefaultMediaFiles(dir: File, mediaFiles: List<MediaFile>, hideRefreshControl: Boolean)

        fun onDiffLoadDefaultMediaFileFailed(dir: File, hideRefreshControl: Boolean)

    }

    internal interface Presenter {
        fun loadInitialPreference()

        fun removeDirectory(dir: File, forceDelete: Boolean)

        fun loadFolderList(fromCacheFirst: Boolean)

        fun loadFolderList(fromCacheFirst: Boolean, diff: Boolean)

        fun diffLoadMediaFileList(dir: File, fromCacheFirst: Boolean, sortWay: SortWay, sortOrder: SortOrder, hideRefreshControl: Boolean, purpose: LoadImageListPurpose)

        fun diffLoadDefaultMediaFileList(dir: File, fromCacheFirst: Boolean, sortWay: SortWay, sortOrder: SortOrder, hideRefreshControl: Boolean)

        fun updateFolderListItemThumbnailList(directory: File)
    }
}

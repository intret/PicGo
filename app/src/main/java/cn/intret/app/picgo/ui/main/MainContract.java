package cn.intret.app.picgo.ui.main;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import java.io.File;
import java.util.List;

import cn.intret.app.picgo.model.image.FolderModel;
import cn.intret.app.picgo.model.image.MediaFile;
import cn.intret.app.picgo.model.user.SortOrder;
import cn.intret.app.picgo.model.user.SortWay;
import cn.intret.app.picgo.model.user.UserInitialPreferences;

/**
 * Created by intret on 2018/3/13.
 */

public class MainContract {

    enum LoadImageListPurpose {
        RefreshDetailList,
        RefreshDefaultList
    }

    interface View {
        void onLoadedUserInitialPreferences(UserInitialPreferences userInitialPreferences);

        void onErrorMessage(@StringRes int msg);

        void onLoadedFolderModel(@NonNull FolderModel folderModel);

        void onLoadedFolderModel(FolderModel folderModel, boolean diff);

        void onLoadFolderModelFailed(Throwable throwable);

        void onDeletedDirectory(File dir);

        void onDeleteDirectoryFailed(File dir, Throwable throwable);

        void onDeleteNotEmptyDirectoryFailed(File dir, Integer fileCount);

        void onDiffDetailMediaFiles(File dir, List<MediaFile> mediaFiles, boolean hideRefreshControl, LoadImageListPurpose purpose);

        void onDiffLoadDetailMediaFileFailed(File dir, boolean hideRefreshControl, LoadImageListPurpose purpose);

        void onDiffLoadedDefaultMediaFiles(File dir, List<MediaFile> mediaFiles, boolean hideRefreshControl);

        void onDiffLoadDefaultMediaFileFailed(File dir, boolean hideRefreshControl);

    }

    interface Presenter {
        void loadInitialPreference();

        void removeDirectory(File dir, boolean forceDelete);

        void loadFolderList(boolean fromCacheFirst);

        void loadFolderList(boolean fromCacheFirst, boolean diff);

        void diffLoadMediaFileList(File dir, boolean fromCacheFirst, SortWay sortWay, SortOrder sortOrder, boolean hideRefreshControl, LoadImageListPurpose purpose);

        void diffLoadDefaultMediaFileList(File dir, boolean fromCacheFirst, SortWay sortWay, SortOrder sortOrder, boolean hideRefreshControl);

        void updateFolderListItemThumbnailList(File directory);
    }
}

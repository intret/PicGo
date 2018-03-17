package cn.intret.app.picgo.ui.main;

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;

import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.NotEmptyException;
import cn.intret.app.picgo.model.image.FolderModel;
import cn.intret.app.picgo.model.image.ImageModule;
import cn.intret.app.picgo.model.image.LoadMediaFileParam;
import cn.intret.app.picgo.model.user.SortOrder;
import cn.intret.app.picgo.model.user.SortWay;
import cn.intret.app.picgo.model.user.UserInitialPreferences;
import cn.intret.app.picgo.model.user.UserModule;
import cn.intret.app.picgo.ui.base.BaseLifecyclePresenter;
import cn.intret.app.picgo.utils.RxUtils;
import io.reactivex.Observable;

/**
 * Presenter role of Main page's MVP design pattern.
 *
 * @param <V> View type, Fragment or Activity who implemented {@link LifecycleOwner}
 */
public class MainPresenter<V extends MainContract.View & LifecycleOwner>
        extends BaseLifecyclePresenter<V>
        implements MainContract.Presenter {

    private static final String TAG = "MainPresenter";

    V mView;

    public MainPresenter(@NonNull V view) {
        mView = view;
    }

    /**
     * @see MainContract.View#onLoadedUserInitialPreferences(UserInitialPreferences)
     */
    @Override
    public void loadInitialPreference() {
        UserModule.getInstance()
                .loadInitialPreference(true)
                .compose(RxUtils.applySchedulers())
                .doOnNext(userInitialPreferences -> Log.d(TAG, "Loaded user initial preference : " + userInitialPreferences))
                .as(autoDispose(mView))
                .subscribe(userInitialPreferences -> {
                    mView.onLoadedUserInitialPreferences(userInitialPreferences);
                }, throwable -> {
                    mView.onErrorMessage(R.string.failed_to_load_preference);
                });
    }

    /**
     *
     * @param dir
     * @param forceDelete
     * @see MainContract.View#onDeletedDirectory(File)
     * @see MainContract.View#onDeleteDirectoryFailed(File, Throwable)
     * @see MainContract.View#onDeleteNotEmptyDirectoryFailed(File, Integer)
     */
    @Override
    public void removeDirectory(File dir, boolean forceDelete) {

        ImageModule.getInstance()
                .removeFolder(dir, forceDelete)
                .compose(RxUtils.applySchedulers())
                .as(RxUtils.lifecycleDisposable(mView))
                .subscribe(deleted -> {
                    if (deleted) {
                        mView.onDeletedDirectory(dir);
                    } else {
                        mView.onDeleteDirectoryFailed(dir, null);
                        Log.e(TAG, "removeDirectory: illegal state");
                    }
                }, throwable -> {

                    if (throwable instanceof NotEmptyException) {
                        Observable.fromCallable(() -> dir.list().length)
                                .compose(RxUtils.applySchedulers())
                                .subscribe(fileCount -> {
                                    mView.onDeleteNotEmptyDirectoryFailed(dir, fileCount);
                                });
                    } else {
                        mView.onDeleteDirectoryFailed(dir, throwable);
                    }
                });
    }


    /**
     *
     * @param fromCacheFirst
     * @see MainContract.View#onLoadedFolderModel(FolderModel)
     */
    @Override
    public void loadFolderList(boolean fromCacheFirst) {
        ImageModule.getInstance()
                .loadFolderList(fromCacheFirst)
                .compose(RxUtils.applySchedulers())
                .as(RxUtils.lifecycleDisposable(mView))
                .subscribe(folderModel -> mView.onLoadedFolderModel(folderModel),
                        RxUtils::unhandledThrowable);
    }

    @Override
    public void loadFolderList(boolean fromCacheFirst, boolean diff) {
        ImageModule.getInstance()
                .loadFolderList(fromCacheFirst)
                .compose(RxUtils.applySchedulers())
                .as(RxUtils.lifecycleDisposable(mView))
                .subscribe(folderModel -> mView.onLoadedFolderModel(folderModel, diff),
                        throwable -> mView.onLoadFolderModelFailed(throwable));
    }

    @Override
    public void diffLoadMediaFileList(File dir, boolean fromCacheFirst, SortWay sortWay, SortOrder sortOrder, boolean hideRefreshControl, MainContract.LoadImageListPurpose purpose) {
        boolean loadMediaInfo = false;
        if (purpose == MainContract.LoadImageListPurpose.RefreshDetailList) {
            loadMediaInfo = true;
        }
        ImageModule.getInstance()
                .loadMediaFileList(dir,
                        new LoadMediaFileParam()
                                .setFromCacheFirst(fromCacheFirst)
                                .setLoadMediaInfo(loadMediaInfo)
                                .setSortWay(sortWay)
                                .setSortOrder(sortOrder)
                )
                .compose(RxUtils.applySchedulers())
                .as(RxUtils.lifecycleDisposable(mView))
                .subscribe(mediaFiles -> {
                    mView.onDiffDetailMediaFiles(dir, mediaFiles, hideRefreshControl, purpose);
                }, throwable -> {
                    mView.onDiffLoadDetailMediaFileFailed(dir, hideRefreshControl, purpose);
                });
    }

    @Override
    public void diffLoadDefaultMediaFileList(File dir, boolean fromCacheFirst, SortWay sortWay, SortOrder sortOrder, boolean hideRefreshControl) {
        ImageModule.getInstance()
                .loadMediaFileList(dir,
                        new LoadMediaFileParam()
                                .setFromCacheFirst(fromCacheFirst)
                                .setLoadMediaInfo(true)
                                .setSortWay(sortWay)
                                .setSortOrder(sortOrder)
                )
                .compose(RxUtils.applySchedulers())
                .as(RxUtils.lifecycleDisposable(mView))
                .subscribe(mediaFiles -> {
                    mView.onDiffLoadedDefaultMediaFiles(dir, mediaFiles, hideRefreshControl);
                }, throwable -> {
                    mView.onDiffLoadDefaultMediaFileFailed(dir, hideRefreshControl);
                });
    }

    @Override
    public void updateFolderListItemThumbnailList(File directory) {
        if (directory == null) {
            Log.e(TAG, "updateFolderListItemThumbnailList: directory is null/empty");
            return;
        }
        ImageModule.getInstance()
                .rescanDirectoryThumbnailList(directory)
                .compose(RxUtils.applySchedulers())
                .as(RxUtils.lifecycleDisposable(mView))
                .subscribe(files -> {

                }, RxUtils::unhandledThrowable);
    }


    @Override
    public void start() {

    }
}

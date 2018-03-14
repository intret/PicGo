package cn.intret.app.picgo.ui.main;

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;

import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.image.ImageModule;
import cn.intret.app.picgo.model.user.UserModule;
import cn.intret.app.picgo.ui.base.BaseLifecyclePresenter;
import cn.intret.app.picgo.utils.RxUtils;

/**
 * Presenter role of Main page's MVP design pattern.
 *
 * @param <V> View type, Fragment or Activity who implemented {@link LifecycleOwner}
 */
public class MainPresenter<V extends MainContractor.View & LifecycleOwner>
        extends BaseLifecyclePresenter<V>
        implements MainContractor.Presenter {

    private static final String TAG = "MainPresenter";

    V mView;

    public MainPresenter(@NonNull V view) {
        mView = view;
    }

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

    @Override
    public void updateFolderListItemThumbnailList(File directory) {
        if (directory == null) {
            Log.e(TAG, "updateFolderListItemThumbnailList: directory is null/empty");
            return;
        }
        ImageModule.getInstance()
                .rescanDirectoryThumbnailList(directory)
                .compose(RxUtils.applySchedulers())
                .subscribe(files -> {

                }, RxUtils::unhandledThrowable);
    }


    @Override
    public void start() {

    }
}

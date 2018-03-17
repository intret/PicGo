package cn.intret.app.picgo.ui.main.move;

import android.arch.lifecycle.LifecycleOwner;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import cn.intret.app.picgo.model.image.ImageModule;
import cn.intret.app.picgo.ui.base.BaseLifecyclePresenter;
import cn.intret.app.picgo.utils.RxUtils;

/**
 * Created by intret on 2018/3/17.
 */

public class MoveFilePresenter<V extends MoveFileContract.View & LifecycleOwner>
        extends BaseLifecyclePresenter<V> implements MoveFileContract.Presenter {

    V mView;

    public MoveFilePresenter(@NotNull V view) {
        mView = view;
    }

    @Override
    public void loadFolderList() {
        ImageModule.getInstance()
                .loadFolderList(true)
                .compose(RxUtils.applySchedulers())
                .as(RxUtils.lifecycleDisposable(mView))
                .subscribe(folderModel -> mView.onLoadFolderModelSuccess(folderModel),
                        throwable -> {
                            mView.onLoadFolderModelFailed();
                        });
    }

    @Override
    public void detectFileExistence(List<File> sourceFiles) {
        ImageModule.getInstance()
                .detectFileExistence(sourceFiles)
                .compose(RxUtils.applySchedulers())
                .as(RxUtils.lifecycleDisposable(mView))
                .subscribe(detectFileExistenceResult -> {
                    mView.onDetectFileExistenceResult(detectFileExistenceResult);
                }, throwable -> {
                    mView.onDetectFileExistenceFailed(sourceFiles);
                });
    }


    @Override
    public void start() {

    }
}

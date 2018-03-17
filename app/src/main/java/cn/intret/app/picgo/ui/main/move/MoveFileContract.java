package cn.intret.app.picgo.ui.main.move;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import cn.intret.app.picgo.model.image.DetectFileExistenceResult;
import cn.intret.app.picgo.model.image.FolderModel;

/**
 * Created by intret on 2018/3/17.
 */

public class MoveFileContract {
    public interface View {

        void onLoadFolderModelSuccess(FolderModel folderModel);

        void onLoadFolderModelFailed();

        void onDetectFileExistenceResult(@NotNull DetectFileExistenceResult detectFileExistenceResult);

        void onDetectFileExistenceFailed(List<File> sourceFiles);
    }

    public interface Presenter {
        void loadFolderList();

        void detectFileExistence(List<File> sourceFiles);
    }
}

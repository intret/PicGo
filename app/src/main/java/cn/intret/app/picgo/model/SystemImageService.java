package cn.intret.app.picgo.model;


import android.content.Context;
import android.util.Log;

import com.annimon.stream.Stream;
import com.annimon.stream.function.IndexedPredicate;

import java.io.File;
import java.io.FileFilter;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import cn.intret.app.picgo.R;
import cn.intret.app.picgo.app.CoreModule;
import io.reactivex.Observable;


public class SystemImageService {

    private static final SystemImageService ourInstance = new SystemImageService();
    private static final String TAG = SystemImageService.class.getSimpleName();

    public static SystemImageService getInstance() {
        return ourInstance;
    }

    private SystemImageService() {
        if (CoreModule.getInstance().getAppContext() == null) {
            throw new IllegalStateException("Please initialize the CoreModule class (CoreModule.getInstance().init(appContext).");
        }
        mContext = CoreModule.getInstance().getAppContext();
    }

    Context mContext;

    FolderContainerModel mFolderContainerModel = new FolderContainerModel();

    public int getFolderListCount() {
        if (mFolderContainerModel == null) {
            loadGalleryFolderList().subscribe(folderInfos -> {

            });
            return mFolderContainerModel.getFolderContainerInfos().size();
        } else {
            return mFolderContainerModel.getFolderContainerInfos().size();
        }
    }

    public int getSectionForPosition(int position) {
        if (mFolderContainerModel == null) {
            return 0;
        } else {

            int begin = 0;
            int end = 0;
            List<FolderContainerModel.FolderContainerInfo> folderContainerInfos = mFolderContainerModel.getFolderContainerInfos();
            for (int sectionIndex = 0; sectionIndex < folderContainerInfos.size(); sectionIndex++) {
                FolderContainerModel.FolderContainerInfo folderContainerInfo = folderContainerInfos.get(sectionIndex);
                int size = folderContainerInfo.getFolders().size();

                end += size;

                if (position >= begin && position < end) {
                    Log.d(TAG, "getSectionForPosition: position " + position + " to section " + sectionIndex);
                    return sectionIndex;
                }

                begin += size;
            }

            throw new InvalidParameterException(
                    String.format(Locale.getDefault(),
                            "Invalid parameter 'position' value '%d', exceeds total item size %d", position, begin));

        }
    }

    public Observable<FolderContainerModel> loadAvailableFolderListModel() {
        return Observable.<FolderContainerModel>create(emitter -> {
            FolderContainerModel folderContainerModel = new FolderContainerModel();

            // SDCard/DCIM directory images
            List<File> allDcimFolders = GalleryService.getInstance().getAllDCIMFolders();
            appendFolderSectionInfo(folderContainerModel,
                    mContext.getString(R.string.sdcard_DCIM), allDcimFolders);

            // SDCard/Picture directory images
            List<File> allPictureFolders = GalleryService.getInstance().getAllPictureFolders();
            appendFolderSectionInfo(folderContainerModel,
                    mContext.getResources().getString(R.string.sdcard_pictures), allPictureFolders);


            emitter.onNext(folderContainerModel);
            emitter.onComplete();
        })
                .doOnNext(folderContainerModel -> mFolderContainerModel = folderContainerModel);
    }

    private List<File> getThumbnailListOfDir(File file, final int thumbnailCount) {
        // TODO: filter image
        File[] files = file.listFiles(File::isFile);
        List<File> thumbFileList = null;
        if (files != null) {
            if (files.length > 0) {
                // 按照时间排序并取前三个文件
                thumbFileList = Stream.of(files)
                        .sorted((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()))
                        .takeWhileIndexed((index, value) -> index < thumbnailCount)
                        .toList();
            }
        }

        return thumbFileList;
    }

    private void appendFolderSectionInfo(FolderContainerModel model, String name, List<File> allMediaFolders) {
        FolderContainerModel.FolderContainerInfo folderContainerInfo = new FolderContainerModel.FolderContainerInfo();
        LinkedList<ImageFolderModel> imageFolderModels = new LinkedList<>();

        for (int i = 0, allMediaFoldersSize = allMediaFolders.size(); i < allMediaFoldersSize; i++) {
            File file = allMediaFolders.get(i);

            // todo merge with getThumbnailListOfDir
            File[] files = file.listFiles();

            imageFolderModels.add(new ImageFolderModel()
                    .setFile(file)
                    .setName(file.getName())
                    .setCount(files == null ? 0 : files.length)
                    .setThumbList(getThumbnailListOfDir(file, 3)) // Default thumbnail file count
            );
        }

        folderContainerInfo.setName(name);
        folderContainerInfo.setFolders(imageFolderModels);
        model.addFolderSection(folderContainerInfo);
    }

    public Observable<List<ImageFolderModel>> loadGalleryFolderList() {

        return loadAvailableFolderListModel()
                .map(folderContainerModel -> {
                    List<FolderContainerModel.FolderContainerInfo> folderContainerInfos = folderContainerModel.getFolderContainerInfos();

                    List<ImageFolderModel> imageFolderModelList = new LinkedList<ImageFolderModel>();
                    for (int i = 0; i < folderContainerInfos.size(); i++) {
                        FolderContainerModel.FolderContainerInfo folderContainerInfo = folderContainerInfos.get(i);
                        List<ImageFolderModel> folders = folderContainerInfo.getFolders();
                        imageFolderModelList.addAll(folders);
                    }

                    return imageFolderModelList;
                })
                ;
    }

    public String getSectionFileName(int position) {
        int sectionForPosition = getSectionForPosition(position);
        FolderContainerModel.FolderContainerInfo folderContainerInfo = mFolderContainerModel.getFolderContainerInfos().get(sectionForPosition);
        return folderContainerInfo.getName();
    }
}

package cn.intret.app.picgo.model;


import android.content.Context;
import android.util.Log;

import java.io.File;
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

            {
                // Gallery images
                FolderContainerModel.FolderContainerInfo folderContainerInfo = new FolderContainerModel.FolderContainerInfo();
                List<ImageFolderModel> imageFolderModelList = new LinkedList<ImageFolderModel>();


                List<File> allDcimFolders = GalleryService.getInstance().getAllDCIMFolders();
                for (File file : allDcimFolders) {
                    File[] files = file.listFiles();
                    imageFolderModelList.add(new ImageFolderModel()
                            .setFile(file)
                            .setName(file.getName())
                            .setCount(files == null ? 0 : files.length)
                    );
                }

                folderContainerInfo.setName(mContext.getString(R.string.gallery));
                folderContainerInfo.setFolders(imageFolderModelList);

                folderContainerModel.addFolderSection(folderContainerInfo);
            }
            {
                // Picture images
                FolderContainerModel.FolderContainerInfo folderContainerInfo = new FolderContainerModel.FolderContainerInfo();
                LinkedList<ImageFolderModel> imageFolderModels = new LinkedList<>();

                List<File> allPictureFolders = GalleryService.getInstance().getAllPictureFolders();
                for (File file : allPictureFolders) {
                    File[] files = file.listFiles();
                    imageFolderModels.add(new ImageFolderModel()
                            .setFile(file)
                            .setName(file.getName())
                            .setCount(files == null ? 0 : files.length)
                    );
                }

                folderContainerInfo.setName(mContext.getResources().getString(R.string.picture));
                folderContainerInfo.setFolders(imageFolderModels);
                folderContainerModel.addFolderSection(folderContainerInfo);
            }


            emitter.onNext(folderContainerModel);
            emitter.onComplete();
        })
                .doOnNext(folderContainerModel -> mFolderContainerModel = folderContainerModel);
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

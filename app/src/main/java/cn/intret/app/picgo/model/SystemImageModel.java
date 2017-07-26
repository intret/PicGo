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
import cn.intret.app.picgo.service.GalleryService;
import io.reactivex.Observable;


public class SystemImageModel {
    private static final SystemImageModel ourInstance = new SystemImageModel();
    private static final String TAG = SystemImageModel.class.getSimpleName();

    public static SystemImageModel getInstance() {
        return ourInstance;
    }

    private SystemImageModel() {
        if (CoreModule.getInstance().getAppContext() == null) {
            throw new IllegalStateException("Please initialize the CoreModule class (CoreModule.getInstance().init(appContext).");
        }
        mContext = CoreModule.getInstance().getAppContext();
    }

    Context mContext;

    FolderModel mFolderModel = new FolderModel();

    public int getFolderListCount() {
        if (mFolderModel == null) {
            loadGalleryFolderList().subscribe(folderInfos -> {

            });
            return mFolderModel.getFolderContainerInfos().size();
        } else {
            return mFolderModel.getFolderContainerInfos().size();
        }
    }

    public int getSectionForPosition(int position) {
        if (mFolderModel == null) {
            return 0;
        } else {

            int begin = 0;
            int end = 0;
            List<FolderModel.FolderContainerInfo> folderContainerInfos = mFolderModel.getFolderContainerInfos();
            for (int sectionIndex = 0; sectionIndex < folderContainerInfos.size(); sectionIndex++) {
                FolderModel.FolderContainerInfo folderContainerInfo = folderContainerInfos.get(sectionIndex);
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

    public Observable<FolderModel> loadAvailableFolderListModel() {
        return Observable.<FolderModel>create(emitter -> {
            FolderModel folderModel = new FolderModel();

            {
                // Gallery images
                FolderModel.FolderContainerInfo folderContainerInfo = new FolderModel.FolderContainerInfo();
                List<FolderInfo> folderInfoList = new LinkedList<FolderInfo>();


                List<File> allDcimFolders = GalleryService.getInstance().getAllDCIMFolders();
                for (File file : allDcimFolders) {
                    File[] files = file.listFiles();
                    folderInfoList.add(new FolderInfo()
                            .setFile(file)
                            .setName(file.getName())
                            .setCount(files == null ? 0 : files.length)
                    );
                }

                folderContainerInfo.setName(mContext.getString(R.string.gallery));
                folderContainerInfo.setFolders(folderInfoList);

                folderModel.addFolderSection(folderContainerInfo);
            }
            {
                // Picture images
                FolderModel.FolderContainerInfo folderContainerInfo = new FolderModel.FolderContainerInfo();
                LinkedList<FolderInfo> folderInfos = new LinkedList<>();

                List<File> allPictureFolders = GalleryService.getInstance().getAllPictureFolders();
                for (File file : allPictureFolders) {
                    File[] files = file.listFiles();
                    folderInfos.add(new FolderInfo()
                            .setFile(file)
                            .setName(file.getName())
                            .setCount(files == null ? 0 : files.length)
                    );
                }

                folderContainerInfo.setName(mContext.getResources().getString(R.string.picture));
                folderContainerInfo.setFolders(folderInfos);
                folderModel.addFolderSection(folderContainerInfo);
            }


            emitter.onNext(folderModel);
            emitter.onComplete();
        })
                .doOnNext(folderModel -> mFolderModel = folderModel);
    }

    public Observable<List<FolderInfo>> loadGalleryFolderList() {

        return loadAvailableFolderListModel()
                .map(folderModel -> {
                    List<FolderModel.FolderContainerInfo> folderContainerInfos = folderModel.getFolderContainerInfos();

                    List<FolderInfo> folderInfoList = new LinkedList<FolderInfo>();
                    for (int i = 0; i < folderContainerInfos.size(); i++) {
                        FolderModel.FolderContainerInfo folderContainerInfo = folderContainerInfos.get(i);
                        List<FolderInfo> folders = folderContainerInfo.getFolders();
                        folderInfoList.addAll(folders);
                    }

                    return folderInfoList;
                })
                ;
    }

    public String getSectionFileName(int position) {
        int sectionForPosition = getSectionForPosition(position);
        FolderModel.FolderContainerInfo folderContainerInfo = mFolderModel.getFolderContainerInfos().get(sectionForPosition);
        return folderContainerInfo.getName();
    }
}

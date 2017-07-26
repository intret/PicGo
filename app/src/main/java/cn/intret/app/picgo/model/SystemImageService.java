package cn.intret.app.picgo.model;


import android.content.Context;
import android.util.Log;

import com.annimon.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import cn.intret.app.picgo.app.CoreModule;
import cn.intret.app.picgo.utils.SystemUtils;
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

    FolderModel mFolderModel = new FolderModel();

    public int getFolderListCount() {
        if (mFolderModel == null) {
            loadGalleryFolderList().subscribe(folderInfos -> {

            });
            return mFolderModel.getParentFolderInfos().size();
        } else {
            return mFolderModel.getParentFolderInfos().size();
        }
    }

    public int getSectionForPosition(int position) {
        if (mFolderModel == null) {
            return 0;
        } else {

            int begin = 0;
            int end = 0;
            List<FolderModel.ParentFolderInfo> parentFolderInfos = mFolderModel.getParentFolderInfos();
            for (int sectionIndex = 0; sectionIndex < parentFolderInfos.size(); sectionIndex++) {
                FolderModel.ParentFolderInfo parentFolderInfo = parentFolderInfos.get(sectionIndex);
                int size = parentFolderInfo.getFolders().size();

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

            // SDCard/DCIM directory images
            File dcimDir = SystemUtils.getDCIMDir();
            List<File> allDcimFolders = GalleryService.getInstance().getSortedSubDirectories(dcimDir);
            addParentFolderInfo(folderModel, dcimDir, allDcimFolders);

            // SDCard/Picture directory images
            File picturesDir = SystemUtils.getPicturesDir();
            List<File> allPictureFolders = GalleryService.getInstance().getSortedSubDirectories(picturesDir);
            addParentFolderInfo(folderModel, picturesDir, allPictureFolders);

            emitter.onNext(folderModel);
            emitter.onComplete();
        })
                .doOnNext(folderModel -> mFolderModel = folderModel);
    }

    private List<File> getThumbnailListOfDir(File[] files, final int thumbnailCount) {
        // TODO: filter image

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

    private void addParentFolderInfo(FolderModel model, File dir, List<File> allMediaFolders) {
        FolderModel.ParentFolderInfo parentFolderInfo = new FolderModel.ParentFolderInfo();
        List<ImageFolderModel> subFolders = new LinkedList<>();

        subFolders.add(imageFolderOfDir(dir));

        for (int i = 0, s = allMediaFolders.size(); i < s; i++) {
            File folder = allMediaFolders.get(i);
            subFolders.add(imageFolderOfDir(folder));
        }

        parentFolderInfo.setName(dir.getName());
        parentFolderInfo.setFolders(subFolders);

        model.addFolderSection(parentFolderInfo);
    }

    private ImageFolderModel imageFolderOfDir(File folder) {
        // todo merge with getThumbnailListOfDir
        File[] imageFiles = folder.listFiles((dir, name) -> {
            String lname = name.toLowerCase();
            return lname.endsWith(".png") |
                    lname.endsWith(".jpeg") |
                    lname.endsWith(".jpg") |
                    lname.endsWith(".webp") |
                    lname.endsWith(".gif") |
                    lname.endsWith(".mp4") |
                    lname.endsWith(".avi");
        });

        return new ImageFolderModel()
                .setFile(folder)
                .setName(folder.getName())
                .setCount(imageFiles == null ? 0 : imageFiles.length)
                .setThumbList(getThumbnailListOfDir(imageFiles, 3));

    }

    public Observable<List<ImageFolderModel>> loadGalleryFolderList() {

        return loadAvailableFolderListModel()
                .map(folderModel -> {
                    List<FolderModel.ParentFolderInfo> parentFolderInfos = folderModel.getParentFolderInfos();

                    List<ImageFolderModel> imageFolderModelList = new LinkedList<ImageFolderModel>();
                    for (int i = 0; i < parentFolderInfos.size(); i++) {
                        FolderModel.ParentFolderInfo parentFolderInfo = parentFolderInfos.get(i);
                        List<ImageFolderModel> folders = parentFolderInfo.getFolders();
                        imageFolderModelList.addAll(folders);
                    }

                    return imageFolderModelList;
                })
                ;
    }

    public String getSectionFileName(int position) {
        int sectionForPosition = getSectionForPosition(position);
        FolderModel.ParentFolderInfo parentFolderInfo = mFolderModel.getParentFolderInfos().get(sectionForPosition);
        return parentFolderInfo.getName();
    }
}

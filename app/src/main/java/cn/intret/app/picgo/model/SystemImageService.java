package cn.intret.app.picgo.model;


import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.annimon.stream.Collector;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BiConsumer;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Supplier;

import org.apache.commons.collections4.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import cn.intret.app.picgo.app.CoreModule;
import cn.intret.app.picgo.utils.DateTimeUtils;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.SystemUtils;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class SystemImageService {

    private static final SystemImageService ourInstance = new SystemImageService();
    private static final String TAG = SystemImageService.class.getSimpleName();
    public static final FilenameFilter MEDIA_FILENAME_FILTER = (dir, name) -> {
        String lname = name.toLowerCase();
        return lname.endsWith(".png") |
                lname.endsWith(".jpeg") |
                lname.endsWith(".jpg") |
                lname.endsWith(".webp") |
                lname.endsWith(".gif") |
                lname.endsWith(".mp4") |
                lname.endsWith(".avi");
    };

    HashMap<String, List<Image>> mImageListMap = new LinkedHashMap<>();
    HashMap<String, List<ImageGroup>> mDayImageGroupsMap = new LinkedHashMap<>();
    HashMap<String, List<ImageGroup>> mWeekImageGroupsMap = new LinkedHashMap<>();
    HashMap<String, List<ImageGroup>> mMonthImageGroupsMap = new LinkedHashMap<>();
    private EventBus mBus;


    public static SystemImageService getInstance() {
        return ourInstance;
    }

    private SystemImageService() {
        if (CoreModule.getInstance().getAppContext() == null) {
            throw new IllegalStateException("Please initialize the CoreModule class (CoreModule.getInstance().init(appContext).");
        }
        mContext = CoreModule.getInstance().getAppContext();
        mBus = EventBus.getDefault();
    }

    Context mContext;

    FolderModel mFolderModel;

    /*
     * 文件夹列表
     */

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

    public Observable<FolderModel> loadFolderListModel(boolean fromCacheFirst) {
        return Observable.<FolderModel>create(emitter -> {

            if (fromCacheFirst) {
                if (mFolderModel != null) {
                    emitter.onNext(mFolderModel);
                    emitter.onComplete();
                    return;
                }
            }

            FolderModel folderModel = new FolderModel();

            // SDCard/DCIM directory images
            File dcimDir = SystemUtils.getDCIMDir();
            List<File> dcimSubFolders = getSortedSubDirectories(dcimDir);
            addParentFolderInfo(folderModel, dcimDir, dcimSubFolders);

            // SDCard/Picture directory images
            File picturesDir = SystemUtils.getPicturesDir();
            List<File> pictureSubFolders = getSortedSubDirectories(picturesDir);
            addParentFolderInfo(folderModel, picturesDir, pictureSubFolders);

            emitter.onNext(folderModel);
            emitter.onComplete();
        })
                .doOnNext(folderModel -> mFolderModel = folderModel);
    }

    public Observable<File> loadRandomImage() {
        return Observable.create(e -> {

            File cameraDir = SystemUtils.getCameraDir();
            File[] files = cameraDir.listFiles();
            if (files == null) {
                e.onError(new Exception("No files in Camera folder."));
                return;
            }

            File file = files[new Random().nextInt() % files.length];
            e.onNext(file);
            e.onComplete();
        });
    }

    public Observable<File> loadFirstCameraImageFile() {
        return Observable.create(e -> {

            File cameraDir = SystemUtils.getCameraDir();
            File[] files = cameraDir.listFiles();
            if (files == null || files.length == 0) {
                e.onError(new Exception("No files in Camera folder."));
                return;
            }

            e.onNext(files[0]);
            e.onComplete();
        });
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

    private void addParentFolderInfo(FolderModel model, File dir, List<File> mediaFolders) {
        FolderModel.ParentFolderInfo parentFolderInfo = new FolderModel.ParentFolderInfo();
        List<ImageFolder> subFolders = new LinkedList<>();

        for (int i = 0, s = mediaFolders.size(); i < s; i++) {
            File folder = mediaFolders.get(i);
            subFolders.add(imageFolderOfDir(folder));
        }
        subFolders.add(imageFolderOfDir(dir));


        parentFolderInfo.setFile(dir);
        parentFolderInfo.setName(dir.getName());
        parentFolderInfo.setFolders(subFolders);

        model.addFolderSection(parentFolderInfo);
    }

    private ImageFolder imageFolderOfDir(File folder) {
        // todo merge with getThumbnailListOfDir
        File[] imageFiles = folder.listFiles(MEDIA_FILENAME_FILTER);

        return new ImageFolder()
                .setFile(folder)
                .setName(folder.getName())
                .setCount(imageFiles == null ? 0 : imageFiles.length)
                .setThumbList(getThumbnailListOfDir(imageFiles, 3));

    }

    private void cacheImageGroupList(GroupMode mode) {

    }

    /**
     * Load grouped image list
     * @param directory
     * @param mode
     * @param fromCacheFirst
     * @return
     */
    public Observable<List<ImageGroup>> loadImageGroupList(File directory, GroupMode mode, boolean fromCacheFirst) {
        return Observable.<List<ImageGroup>>create(e -> {
            String absolutePath = directory.getAbsolutePath();
            if (fromCacheFirst){
                switch (mode) {
                    case DAY: {
                        List<ImageGroup> imageGroups = mDayImageGroupsMap.get(absolutePath);
                        if (imageGroups != null) {
                            e.onNext(imageGroups);
                            e.onComplete();
                            return;
                        }
                    }
                    break;
                    case WEEK: {
                        List<ImageGroup> imageGroups = mWeekImageGroupsMap.get(absolutePath);
                        if (imageGroups != null) {
                            e.onNext(imageGroups);
                            e.onComplete();
                            return;
                        }
                    }
                    break;
                    case MONTH: {
                        List<ImageGroup> imageGroups = mMonthImageGroupsMap.get(absolutePath);
                        if (imageGroups != null) {
                            e.onNext(imageGroups);
                            e.onComplete();
                            return;
                        }
                    }
                    break;
                }
            }

            List<File> imageFiles = SystemImageService.getInstance().listImageFiles(directory);
            LinkedList<ImageGroup> sections = Stream.of(imageFiles)
                    .groupBy(file -> {
                        long d = file.lastModified();
                        Date date = new Date(d);

                        switch (mode) {
                            case DAY:
                                return DateTimeUtils.daysBeforeToday(date);
                            case WEEK:
                                return DateTimeUtils.weeksBeforeCurrentWeek(date);
                            case MONTH:
                                return DateTimeUtils.monthsBeforeCurrentMonth(date);
                            default:
                                return 0;
                        }
                    })
                    .sorted((o1, o2) -> Integer.compare(o1.getKey(), o2.getKey()))
                    .collect(new Collector<Map.Entry<Integer, List<File>>,
                            LinkedList<ImageGroup>,
                            LinkedList<ImageGroup>>() {
                        @Override
                        public Supplier<LinkedList<ImageGroup>> supplier() {
                            return LinkedList::new;
                        }

                        @Override
                        public BiConsumer<LinkedList<ImageGroup>,
                                Map.Entry<Integer, List<File>>> accumulator() {
                            return (sections, entry) -> {

                                ImageGroup section = new ImageGroup();
                                List<File> files = entry.getValue();
                                List<Image> itemList = Stream.of(files)
                                        .map(file -> new Image()
                                                .setFile(file)
                                                .setDate(new Date(file.lastModified())))
                                        .toList();
                                Image firstItem = ListUtils.firstOf(itemList);
                                Image lastItem = ListUtils.lastOf(itemList);

                                section.setImages(itemList);
                                section.setStartDate(firstItem.getDate());
                                section.setEndDate(lastItem.getDate());

                                sections.add(section);
                            };
                        }

                        @Override
                        public Function<LinkedList<ImageGroup>,
                                LinkedList<ImageGroup>> finisher() {
                            return sections -> sections;
                        }
                    });

            e.onNext(sections);
            e.onComplete();
        }).doOnNext(imageGroups -> cacheSectionedImageGroup(directory.getAbsolutePath(), mode, imageGroups));
    }

    public Observable<Integer> moveFilesToDirectory(File destDir, List<File> files) {
        return Observable.create(e -> {
            int i = moveFilesTo(destDir, files);

            Log.d(TAG, "moveFilesToDirectory: 移动" + files.size() + "个文件到" + destDir);

            // Rescan source dirs
            Stream.of(files).groupBy(File::getParent)
                    .forEach(entry -> rescanImageDirectory(new File(entry.getKey()), false));

            // Rescan target dir
            rescanImageDirectory(destDir, false);

            e.onNext(i);
            e.onComplete();
        });
    }

    /*
     * 扫描目录
     */


    private void rescanImageDirectory(File destDir, boolean onlyCached) {

        boolean loadImageList = false;
        if (onlyCached) {
            if (mImageListMap.containsKey(destDir.getAbsolutePath())) {
                loadImageList = true;
            }
        } else {
            loadImageList = true;
        }

        if (loadImageList) {

            loadImageList(destDir, false)
                    .subscribe(images -> {
                        Log.d(TAG, "rescan directory : " + destDir);

                        mBus.post(new RescanImageDirectoryMessage().setDirectory(destDir));

                    }, throwable -> {
                        Log.e(TAG, "rescan directory with exception : " + throwable );
                    });
        }

        // Todo load grouped image list

        loadImageGroupList(destDir, GroupMode.WEEK, false)
                .subscribe(imageGroups -> {
                    Log.d(TAG, "rescan week grouped directory: " + destDir);
                }, throwable -> {

                });

        loadImageGroupList(destDir, GroupMode.MONTH, false)
                .subscribe(imageGroups -> {
                    Log.d(TAG, "rescan month grouped directory: " + destDir);
                }, throwable -> {

                });
    }

    private void rescanFolderDirectory(File file) {
        loadFolderListModel(false)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(model -> {
                    mBus.post(new RescanFolderListMessage());
                }, throwable -> {
                    Log.d(TAG, "rescanFolderDirectory: " + throwable);
                });
    }

    private int moveFilesTo(File destDir, List<File> files) {
        if (destDir == null) {
            throw new IllegalArgumentException("Argument 'destDir' is null.");
        }

        if (!destDir.isDirectory()) {
            throw new IllegalArgumentException("Argument 'destDir' is not a valid directory.");
        }

        if (ListUtils.isEmpty(files)) {
            return 0;
        }

        int successCount = 0;
        File srcFile;
        try {
            for (int i = 0; i < files.size(); i++) {
                srcFile = files.get(i);
                if (srcFile == null) {
                    continue;
                }
                FileUtils.moveFileToDirectory(srcFile, destDir, true);
                Log.d(TAG, "move file " + srcFile + " to " + destDir);
                successCount++;
            }
        } catch (IOException e) {
            Log.e(TAG, "move files to " + destDir + " failed.");
            e.printStackTrace();
        }

        return successCount;
    }

    /*
     * 加载和缓存图片
     */
    private void cacheSectionedImageGroup(String directory, GroupMode mode, List<ImageGroup> imageGroups) {
        Log.d(TAG, "cacheSectionedImageGroup() called with: directory = [" + directory + "], mode = [" + mode);

        if (directory == null || imageGroups == null) {
            return;
        }

        switch (mode) {
            case DEFAULT:
                break;
            case DAY:
                mDayImageGroupsMap.put(directory, imageGroups);
                break;
            case WEEK:
                mWeekImageGroupsMap.put(directory, imageGroups);
                break;
            case MONTH:
                mMonthImageGroupsMap.put(directory, imageGroups);
                break;
        }
    }

    /**
     * TODO add fromCache param
     * TODO add override method for diff-ing cached list to newly one, trigger changing action
     * @param directory
     * @param fromCacheFirst
     * @return
     */
    public Observable<List<Image>> loadImageList(File directory, boolean fromCacheFirst) {
        return Observable.<List<Image>>create(
                e -> {
                    if (directory == null) {
                        e.onError(new IllegalArgumentException("Invalid argument 'directory' value '" + directory + "'"));
                        return;
                    }
                    if (!directory.exists()) {
                        e.onError(new FileNotFoundException("Directory not found : " + directory));
                        return;
                    }

                    List<Image> images;
                    if (fromCacheFirst){
                        images = mImageListMap.get(directory.getAbsolutePath());
                        if (images != null) {
                            e.onNext(images);
                            e.onComplete();
                            return;
                        }
                    }

                    List<File> imageFiles = SystemImageService.getInstance().listImageFiles(directory);
                    Log.d(TAG, "loadImageList: " + imageFiles.size() + " images in " + directory);
                    List<Image> sortedImages = Stream.of(imageFiles)
                            .map(file -> new Image().setFile(file).setDate(new Date(file.lastModified())))
                            .sorted((o1, o2) -> o2.getDate().compareTo(o1.getDate()))
                            .toList();

                    e.onNext(sortedImages);
                    e.onComplete();
                })
                .doOnNext(images -> cacheImageList(directory.getAbsolutePath(), images));
    }

    private void cacheImageList(String dirPath, List<Image> images) {

        if (dirPath != null && images != null) {
            Log.d(TAG, "cacheImageList() called with: dirPath = [" + dirPath + "], images = [" + images.size() + "]");
            mImageListMap.put(dirPath, images);
        }
    }

    public List<File> listImageFiles(File dir) {
        if (!dir.isDirectory()) {
            throw new InvalidParameterException("参数 'dir' 对应的目录（" + dir.getAbsolutePath() + "）不存在：");
        }

        File[] allFiles = dir.listFiles(MEDIA_FILENAME_FILTER);
        if (allFiles == null) {
            return new LinkedList<>();
        }

        return Stream.of(allFiles)
                .sorted((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()))
                .toList();
    }

    public List<File> getSortedSubDirectories(File directory) throws FileNotFoundException {
        if (directory == null) {
            throw new FileNotFoundException("Cannot found camera directory.");
        }

        File[] allFiles = directory.listFiles((file) -> file.isDirectory() && !file.getName().startsWith("."));
        if (allFiles == null) {
            return new LinkedList<>();
        }

        // Long.compare(file2.lastModified(), file.lastModified())
        return Stream.of(allFiles)
                .sorted((file1, file2) -> StringUtils.compare(file1.getName(), file2.getName())  )
                .toList();
    }

    public Observable<List<ImageFolder>> loadGalleryFolderList() {

        return loadFolderListModel(true)
                .map(folderModel -> {
                    List<FolderModel.ParentFolderInfo> parentFolderInfos = folderModel.getParentFolderInfos();

                    List<ImageFolder> imageFolderList = new LinkedList<ImageFolder>();
                    for (int i = 0; i < parentFolderInfos.size(); i++) {
                        FolderModel.ParentFolderInfo parentFolderInfo = parentFolderInfos.get(i);
                        List<ImageFolder> folders = parentFolderInfo.getFolders();
                        imageFolderList.addAll(folders);
                    }

                    return imageFolderList;
                })
                ;
    }

    public String getSectionFileName(int position) {
        int sectionForPosition = getSectionForPosition(position);
        FolderModel.ParentFolderInfo parentFolderInfo = mFolderModel.getParentFolderInfos().get(sectionForPosition);
        return parentFolderInfo.getName();
    }

    public Observable<Boolean> renameDir(File srcDir, String newDirName) {
        return Observable.create(e -> {
            if (srcDir == null) {
                throw new IllegalArgumentException("File should not be null");
            }

            if (!srcDir.exists()) {
                throw new FileNotFoundException("File not found");
            }

            if (!srcDir.isDirectory()) {
                throw new IllegalStateException("File is not a directory.");
            }

            if (StringUtils.equals(newDirName, srcDir.getName())) {
                throw new IllegalArgumentException("The new directory name '" + newDirName + "' equals to the source directory.");
            }

            File destDir = new File(srcDir.getParentFile(), newDirName);
            FileUtils.moveDirectory(srcDir, destDir);

            mBus.post(new RenameDirectoryMessage()
                    .setOldDirectory(srcDir)
                    .setNewDirectory(destDir));

            e.onNext(true);
            e.onComplete();
        });
    }

    void removeCachedFiles(String dirPath, List<File> files) {
        mImageListMap.containsKey(dirPath);
        rescanImageDirectory(new File(dirPath), false);
    }

    /**
     * 删除文件，并把在一个目录中的文件扫描一次目录，缓存，然后通知目录发送变化
     * @param files
     * @return 已经删除的文件个数，删除失败的文件列表
     */
    public Observable<Pair<Integer, List<File>>> removeFiles(Collection<File> files) {
        return Observable.create(e -> {

            final int[] totalRemoved = {0};
            List<File> removeFailedFiles = new LinkedList<File>();
            // 文件分组来删除：按照目录来分组，每个目录执行一次批量删除，并重新扫描发送目录扫描通知
            Stream.of(files)
                    .groupBy(File::getParent)
                    .forEach(new Consumer<Map.Entry<String, List<File>>>() {
                        @Override
                        public void accept(Map.Entry<String, List<File>> stringListEntry) {
                            String dir = stringListEntry.getKey();
                            List<File> files = stringListEntry.getValue();

                            removeFilesWithNotification(dir, files);
                        }

                        private void removeFilesWithNotification(String dir, List<File> files) {
                            final int[] count = {0};
                            List<File> removeSuccessFiles = new LinkedList<File>();
                            Stream.of(files)
                                    .filter(File::isFile)
                                    .forEach(file -> {
                                        if (file.delete()) {
                                            count[0]++;
                                            removeSuccessFiles.add(file);
                                        } else {
                                            removeFailedFiles.add(file);
                                        }
                                    });
                            totalRemoved[0] += removeSuccessFiles.size();

                            // 扫描目录
                            rescanImageDirectory(new File(dir), false);

                            rescanFolderDirectory(new File(dir));
                        }
                    });


            Pair<Integer, List<File>> result = new Pair<>(totalRemoved[0], removeFailedFiles);
            e.onNext(result);
            e.onComplete();
        });
    }

    /*
     * 目录管理
     */

    public Observable<Boolean> removeFile(File file) {
        return Observable.create(e -> {
            if (file == null) {
                throw new IllegalArgumentException("File should not be null");
            }

            if (!file.exists()) {
                throw new FileNotFoundException("File not found");
            }

            if (file.delete()) {

                String parent = file.getParent();
                List<Image> images = mImageListMap.get(parent);
                if (images != null) {
                    int i = org.apache.commons.collections4.ListUtils.indexOf(images, image -> SystemUtils.isSameFile(image.getFile(), file));
                    if (i != -1) {
                        images.remove(i);
                        Log.d(TAG, "removeFile: " + file + " at " + i);
                    }
                }

                e.onNext(true);
                e.onComplete();

                mBus.post(new RemoveFileMessage().setFile(file));
            }
        });
    }

    public Observable<Boolean> createFolder(File dir) {

        return Observable.create(e -> {
            if (dir == null) {
                throw new IllegalArgumentException("dir must not be null.");
            }

            FileUtils.forceMkdir(dir);

            List<FolderModel.ParentFolderInfo> parentFolderInfos = Stream.of(mFolderModel.getParentFolderInfos())
                    .filter(value -> SystemUtils.isSameFile(value.getFile(), dir.getParentFile()))
                    .limit(1)
                    .toList();

            if (!parentFolderInfos.isEmpty()) {
                FolderModel.ParentFolderInfo parentFolderInfo = parentFolderInfos.get(0);
                parentFolderInfo.getFolders().add(0, imageFolderOfDir(dir));

                mBus.post(new FolderModelChangeMessage());
            }

//            loadFolderListModel(false)
//                    .subscribeOn(Schedulers.io())
//                    .observeOn(Schedulers.io())
//                    .subscribe(model -> {
//                        mFolderModel = model;
//                    }, throwable -> {
//                        Log.d(TAG, "createFolder: reload folder model failed.");
//                    });

            e.onNext(true);
            e.onComplete();
        });
    }

    public Observable<Boolean> removeFolder(File dir) {
        return Observable.create(e -> {
            if (dir == null) {
                throw new IllegalArgumentException("dir must not be null");
            }
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                Log.d(TAG, "removeFolder: 有文件不能移除");
            }

            FileUtils.deleteDirectory(dir);


            Stream.of(mFolderModel.getParentFolderInfos())
                    .forEach(parentFolderInfo -> {
                        List<ImageFolder> folders = parentFolderInfo.getFolders();
                        int i = org.apache.commons.collections4.ListUtils.indexOf(folders, new Predicate<ImageFolder>() {
                            @Override
                            public boolean evaluate(ImageFolder object) {
                                return SystemUtils.isSameFile(object.getFile(), dir);
                            }
                        });

                        if (i != -1) {
                            Log.d(TAG, "removeFolder: remove folder at " + i);
                            parentFolderInfo.getFolders().remove(i);
                        }
                    });

            mBus.post(new FolderModelChangeMessage());

            e.onNext(true);
            e.onComplete();

        });
    }
}

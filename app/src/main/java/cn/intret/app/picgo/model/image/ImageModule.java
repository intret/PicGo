package cn.intret.app.picgo.model.image;


import android.annotation.SuppressLint;
import android.media.ExifInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;
import android.util.Size;

import com.annimon.stream.Collector;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BiConsumer;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Supplier;
import com.f2prateek.rx.preferences2.Preference;
import com.t9search.model.PinyinSearchUnit;
import com.t9search.util.T9Util;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import cn.intret.app.picgo.model.BaseModule;
import cn.intret.app.picgo.model.NotEmptyException;
import cn.intret.app.picgo.model.user.SortOrder;
import cn.intret.app.picgo.model.user.SortWay;
import cn.intret.app.picgo.model.event.FolderModelChangeMessage;
import cn.intret.app.picgo.model.event.RemoveFileMessage;
import cn.intret.app.picgo.model.event.RenameDirectoryMessage;
import cn.intret.app.picgo.model.event.RescanFolderListMessage;
import cn.intret.app.picgo.model.event.RescanFolderThumbnailListMessage;
import cn.intret.app.picgo.model.event.RescanImageDirectoryMessage;
import cn.intret.app.picgo.model.user.UserModule;
import cn.intret.app.picgo.utils.DateTimeUtils;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.MediaUtils;
import cn.intret.app.picgo.utils.ObjectGuarder;
import cn.intret.app.picgo.utils.PathUtils;
import cn.intret.app.picgo.utils.RxUtils;
import cn.intret.app.picgo.utils.SystemUtils;
import cn.intret.app.picgo.utils.Watch;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class ImageModule extends BaseModule {

    @SuppressLint("StaticFieldLeak")
    private static final ImageModule ourInstance = new ImageModule();
    private static final String TAG = ImageModule.class.getSimpleName();

    /**
     * 文件列表项获取的缩略图个数
     */
    private static final int DEFAULT_THUMBNAIL_COUNT = 3;
    public static final Comparator<File> FOLDER_LIST_NAME_ASC_COMPARATOR = (file1, file2) -> StringUtils.compare(file1.getName(), file2.getName());

    /**
     * Key : directory file
     */
    private HashMap<File, MediaFileList> mMediaFileListMap = new LinkedHashMap<>();
    private ObjectGuarder<HashMap<File, MediaFileList>> mMediaFileListMapGuard = new ObjectGuarder<>(mMediaFileListMap);

    private HashMap<String, List<MediaFile>> mImageListMap = new LinkedHashMap<>();
    private HashMap<String, List<ImageGroup>> mDayImageGroupsMap = new LinkedHashMap<>();
    private HashMap<String, List<ImageGroup>> mWeekImageGroupsMap = new LinkedHashMap<>();
    private HashMap<String, List<ImageGroup>> mMonthImageGroupsMap = new LinkedHashMap<>();

    private ObjectGuarder<List<File>> mHiddenFolders = new ObjectGuarder<>(new LinkedList<File>());

    private Preference<Boolean> mShowHiddenFolderPref;
    private boolean mShowHiddenFile = false;
    private SortWay mSortWay = SortWay.UNKNOWN;
    private SortOrder mSortOrder = SortOrder.UNKNOWN;

    FolderModel mFolderModel;
    ReadWriteLock mFolderModelRWLock = new ReentrantReadWriteLock();
    private ObjectGuarder<Preference<LinkedList<File>>> mExlucdeFolderListPref;

    private class MediaFileList {
        List<MediaFile> mMediaFiles;
        boolean mIsLoadedExtraInfo = false;

        List<MediaFile> getMediaFiles() {
            return mMediaFiles;
        }

        MediaFileList setMediaFiles(List<MediaFile> mediaFiles) {
            mMediaFiles = mediaFiles;
            return this;
        }

        boolean isLoadedExtraInfo() {
            return mIsLoadedExtraInfo;
        }

        MediaFileList setLoadedExtraInfo(boolean loadedExtraInfo) {
            mIsLoadedExtraInfo = loadedExtraInfo;
            return this;
        }
    }

    public static ImageModule getInstance() {
        return ourInstance;
    }

    private ImageModule() {
        super();

        loadUserExcludeFileList();
    }


    private void loadUserExcludeFileList() {
        Log.d(TAG, "loadHiddenFileList: before");

        // 是否显示隐藏目录
        Observable.<Preference<Boolean>>create(
                e -> {
                    Watch watch = Watch.now();
                    Preference<Boolean> showHiddenFolder = UserModule.getInstance().getShowHiddenFilePreference();

                    watch.logGlanceMS(TAG, "Load user initial preference");
                    e.onNext(showHiddenFolder);
                    e.onComplete();
                })
                .subscribe(
                        preference -> {
                            mShowHiddenFolderPref = preference;
                            mShowHiddenFile = mShowHiddenFolderPref.get();
                            Log.d(TAG, "loadHiddenFileList: mShowHiddenFile = " + mShowHiddenFile);

                            mShowHiddenFolderPref.asObservable()
                                    .subscribe(showHiddenFile -> {
                                        Log.d(TAG, "loadHiddenFileList: show hidden file preference changed : " + showHiddenFile);
                                        mShowHiddenFile = showHiddenFile;

                                        Log.w(TAG, "loadHiddenFileList: todo reload folder list");
                                    });
                        }
                        , RxUtils::unhandledThrowable);

        // 隐藏目录列表
        UserModule.getInstance()
                .getHiddenFolder()
                .subscribe(files -> {
                    mHiddenFolders.writeConsume(fileList -> {
                        Log.d(TAG, "loadHiddenFileList: save file list " + fileList);

                        fileList.clear();
                        fileList.addAll(files);
                    });

                    //mBus.post(new ExcludeFolderListChangeMessage());

//                    rescanFolderList("loaded hidden folder list ");

                }, RxUtils::unhandledThrowable);
        Log.d(TAG, "loadHiddenFileList: after");

        UserModule.getInstance()
                .getExcludeFolderPreference()
                .subscribeOn(Schedulers.io())
                .subscribe(linkedListPreference -> {
                    mExlucdeFolderListPref = new ObjectGuarder<>(linkedListPreference);
                    mExlucdeFolderListPref.readConsume(pref -> {
                        pref.asObservable().subscribe(newFileList -> {
                            mHiddenFolders.writeConsume(fileList -> {

                                fileList.clear();
                                fileList.addAll(newFileList);

                                rescanFolderList("updated hidden folder list");
                            });
                        });
                    });
                });
    }


    /*
     * 文件夹列表
     */

    public int getFolderListCount() {
        try {
            mFolderModelRWLock.readLock().lock();
            if (mFolderModel == null) {
                loadGalleryFolderList().subscribe(folderInfos -> {

                });
                return mFolderModel.getContainerFolders().size();
            } else {
                return mFolderModel.getContainerFolders().size();
            }
        } finally {
            mFolderModelRWLock.readLock().unlock();
        }
    }

    @Deprecated
    public int getSectionForPosition(int position) {
        try {

            mFolderModelRWLock.readLock().lock();
            if (mFolderModel == null) {
                return 0;
            } else {

                int begin = 0;
                int end = 0;
                List<FolderModel.ContainerFolder> containerFolders = mFolderModel.getContainerFolders();
                for (int sectionIndex = 0; sectionIndex < containerFolders.size(); sectionIndex++) {
                    FolderModel.ContainerFolder containerFolder = containerFolders.get(sectionIndex);
                    int size = containerFolder.getFolders().size();

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
        } finally {
            mFolderModelRWLock.readLock().unlock();
        }
    }

    /**
     * @param fromCacheFirst
     * @param t9NumberInput  为 null 或者空字符串时，获取的文件列表不进行 T9 过滤，并且是正常模式，并非过滤模式。
     * @return
     */
    public Observable<FolderModel> loadFolderList(boolean fromCacheFirst, @Nullable String t9NumberInput) {
        return loadFolderList(fromCacheFirst)
                .map(model -> {
                    if (StringUtils.isBlank(t9NumberInput)) {
                        return model;
                    }

                    // The variable 'model' is a copy of original model, we can modify it.
                    filterModelByT9NumberInput(model, t9NumberInput);

                    model.setT9FilterMode(true);
                    return model;
                });
    }

    private void filterModelByT9NumberInput(FolderModel model, String t9Numbers) {
        for (FolderModel.ContainerFolder folderInfo : model.getContainerFolders()) {

            List<ImageFolder> folders = folderInfo.getFolders();
            {
                List<ImageFolder> filteredFolders = new LinkedList<>();
                for (ImageFolder folder : folders) {
                    PinyinSearchUnit pinyinSearchUnit = folder.getPinyinSearchUnit();

                    // Pinyin match

                    if (T9Util.match(pinyinSearchUnit, t9Numbers)) {

                        folder.setMatchKeywords(pinyinSearchUnit.getMatchKeyword().toString());
                        folder.setMatchStartIndex(folder.getName().indexOf(pinyinSearchUnit.getMatchKeyword().toString()));
                        folder.setMatchLength(folder.getMatchKeywords().length());

                        filteredFolders.add(folder);
                    } else {
//                        Log.d(TAG, "T9: folder [" + folder.getName() + "]  -------------- T9 keyboard input : " + t9Numbers);
                    }
                }

                Log.d(TAG, "----- 最后剩下 " + filteredFolders.size() + "/" + folders.size() + " 项 -----");
                folderInfo.setFolders(filteredFolders);
            }
        }
    }

    public Observable<FolderModel> loadHiddenFileListModel(String t9NumberInput) {
        return loadHiddenFileListModel()
                .map(model -> {
                    if (StringUtils.isBlank(t9NumberInput)) {
                        return model;
                    }

                    // The variable 'model' is a copy of original model, we can modify it.
                    filterModelByT9NumberInput(model, t9NumberInput);

                    model.setT9FilterMode(true);
                    return model;
                });
    }

    public Observable<FolderModel> loadHiddenFileListModel() {

        return Observable.create(e -> {

            FolderModel folderModel = new FolderModel();
            mHiddenFolders.readConsume(object -> Stream.of(object)
                    .groupBy(File::getParentFile)
                    .forEach(fileListEntry -> {
                        File parentDir = fileListEntry.getKey();
                        List<File> subFolderList = fileListEntry.getValue();

                        List<ImageFolder> imageFolders = Stream.of(subFolderList)
                                .map(this::createImageFolder)
                                .toList();

                        folderModel.addFolderSection(new FolderModel.ContainerFolder()
                                .setFile(parentDir)
                                .setName(parentDir.getName())
                                .setFolders(imageFolders)
                        );
                    }));

            e.onNext(folderModel);
            e.onComplete();
        });
    }

    public Observable<FolderModel> loadFolderList(boolean fromCacheFirst) {
        return Observable.create(
                emitter -> {

                    Log.d(TAG, "loadFolderList() called with: fromCacheFirst = [" + fromCacheFirst + "]");

                    if (fromCacheFirst) {
                        try {

                            mFolderModelRWLock.readLock().lock();
                            if (mFolderModel != null) {

                                FolderModel clone = (FolderModel) mFolderModel.clone();
                                Log.d(TAG, "loadFolderList: get clone " + clone + " of " + mFolderModel);
                                emitter.onNext(clone);
                                emitter.onComplete();
                                return;
                            }
                        } finally {
                            mFolderModelRWLock.readLock().unlock();
                        }
                    }

                    Watch watch = Watch.now();
                    FolderModel folderModel = new FolderModel();

                    // SDCard/DCIM directory images
                    File dcimDir = SystemUtils.getDCIMDir();
                    List<File> dcimSubFolders = getSortedSubDirectories(dcimDir);
                    addContainerFolder(folderModel, dcimDir, dcimSubFolders);

                    // SDCard/Picture directory images
                    File picturesDir = SystemUtils.getPicturesDir();
                    List<File> pictureSubFolders = getSortedSubDirectories(picturesDir);
                    addContainerFolder(folderModel, picturesDir, pictureSubFolders);

                    watch.logGlanceMS(TAG, "Load folder list");
                    FolderModel cloneModel;
                    try {
                        mFolderModelRWLock.writeLock().lock();
                        mFolderModel = folderModel;
                        cloneModel = (FolderModel) mFolderModel.clone();
                    } finally {
                        mFolderModelRWLock.writeLock().unlock();
                    }

                    watch.logGlanceMS(TAG, "Cache folder list");

                    emitter.onNext(cloneModel);
                    emitter.onComplete();
                });
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

    private List<File> createThumbnailList(File[] files, final int thumbnailCount) {
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

    private void addContainerFolder(FolderModel model, File dir, List<File> mediaFolders) {
        FolderModel.ContainerFolder containerFolder = new FolderModel.ContainerFolder();
        List<ImageFolder> subFolders = new LinkedList<>();

        for (int i = 0, s = mediaFolders.size(); i < s; i++) {
            File folder = mediaFolders.get(i);

            if (!mShowHiddenFile) {
                mHiddenFolders.readConsume(fileList -> {
                    if (!fileList.contains(folder)) {
                        subFolders.add(createImageFolder(folder));
                    } else {
//                        Log.d(TAG, "addContainerFolder: ignore folder [" + folder + "]");
                    }
                });
            } else {
                subFolders.add(createImageFolder(folder));
            }
        }

        containerFolder.setFile(dir);
        containerFolder.setName(dir.getName());
        containerFolder.setFolders(subFolders);

        model.addFolderSection(containerFolder);
    }

    private ImageFolder createImageFolder(File folder) {
        // todo merge with createThumbnailList
        File[] imageFiles = folder.listFiles(PathUtils.MEDIA_FILENAME_FILTER);

        return new ImageFolder()
                .setFile(folder)
                .setName(folder.getName())
                .setCount(imageFiles == null ? 0 : imageFiles.length)
                .setThumbList(createThumbnailList(imageFiles, DEFAULT_THUMBNAIL_COUNT))
                .setMediaFiles(imageFiles);

    }


    /**
     * TODO: 按照显示模式来对文件列表排序并取前三个文件
     * 扫描目录，产生 {@link RescanFolderThumbnailListMessage} 通知。
     */
    public Observable<List<File>> rescanDirectoryThumbnailList(File dir) {
        return Observable.create(e -> {
            File[] imageFiles = dir.listFiles(PathUtils.MEDIA_FILENAME_FILTER);
            List<File> thumbnailList = createThumbnailList(imageFiles, DEFAULT_THUMBNAIL_COUNT);

            try {
                mFolderModelRWLock.writeLock().lock();
                if (updateFileModelThumbnailList(mFolderModel, dir, thumbnailList)) {
                    Log.d(TAG, "已经更新目录缩略图列表：" + dir);
                    mBus.post(new RescanFolderThumbnailListMessage()
                            .setDirectory(dir)
                            .setThumbnails(thumbnailList)
                    );
                }
            } finally {
                mFolderModelRWLock.writeLock().unlock();
            }
            e.onNext(thumbnailList);
            e.onComplete();
        });
    }

    private boolean updateFileModelThumbnailList(FolderModel folderModel, File dir, List<File> thumbnailList) {
        ImageFolder imageFolder = getFileModelImageFolder(folderModel, dir);
        if (imageFolder != null) {
            if (org.apache.commons.collections4.ListUtils.isEqualList(imageFolder.getThumbList(), thumbnailList)) {
                Log.d(TAG, "updateFileModelThumbnailList: two thumbnail list (cache one and new one) are equal, don't update");
                return false;
            }
            imageFolder.setThumbList(thumbnailList);
            return true;
        }
        return false;
    }

    @Nullable
    private ImageFolder getFileModelImageFolder(FolderModel folderModel, File dir) {
        ImageFolder imageFolder;
        List<FolderModel.ContainerFolder> containerFolders = folderModel.getContainerFolders();
        for (int i = 0, parentFolderInfosSize = containerFolders.size(); i < parentFolderInfosSize; i++) {
            FolderModel.ContainerFolder containerFolder = containerFolders.get(i);
            List<ImageFolder> folders = containerFolder.getFolders();
            for (int i1 = 0, foldersSize = folders.size(); i1 < foldersSize; i1++) {
                imageFolder = folders.get(i1);
                if (imageFolder.getFile().equals(dir)) {
                    return imageFolder;
                }
            }
        }
        return null;
    }

    private void cacheImageGroupList(GroupMode mode) {

    }

    /**
     * Load grouped image list
     *
     * @param directory
     * @param mode
     * @param fromCacheFirst
     * @param order
     * @param way            @return
     */
    public Observable<List<ImageGroup>> loadImageGroupList(File directory, GroupMode mode,
                                                           boolean fromCacheFirst, SortWay way, SortOrder order) {
        return Observable.<List<ImageGroup>>create(e -> {
            String absolutePath = directory.getAbsolutePath();
            if (fromCacheFirst) {
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

            List<File> imageFiles = ImageModule.getInstance().listMediaFiles(directory);
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
                                List<MediaFile> itemList = Stream.of(files)
                                        .map(file -> {
                                            MediaFile mediaFile = new MediaFile();
                                            mediaFile.setFile(file);
                                            mediaFile.setDate(new Date(file.lastModified()));
                                            return mediaFile;
                                        })
                                        .toList();
                                MediaFile firstItem = ListUtils.firstOf(itemList);
                                MediaFile lastItem = ListUtils.lastOf(itemList);

                                section.setMediaFiles(itemList);
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


    /*
     * 扫描目录
     */

    /**
     * 产生 {@link RescanImageDirectoryMessage}
     *
     * @param destDir
     * @param onlyRescanDirInCache
     */
    private void rescanImageDirectory(File destDir, boolean onlyRescanDirInCache) {

        Log.d(TAG, "rescanImageDirectory() called with: destDir = [" + destDir + "], onlyRescanDirInCache = [" + onlyRescanDirInCache + "]");

        boolean loadImageList = false;
        if (onlyRescanDirInCache) {
            try {
                mMediaFileListMapGuard.readLock();
                if (mMediaFileListMap.containsKey(destDir)) {
                    loadImageList = true;
                }
            } finally {
                mMediaFileListMapGuard.readUnlock();
            }
        } else {
            loadImageList = true;
        }

        if (loadImageList) {

            loadMediaFileList(destDir, new LoadMediaFileParam()
                    .setFromCacheFirst(false)
                    .setLoadMediaInfo(false)
                    .setSortOrder(mSortOrder)
                    .setSortWay(mSortWay)
            )
                    .subscribe(images -> {
                        Log.d(TAG, "rescan directory : " + destDir);

                        mBus.post(new RescanImageDirectoryMessage().setDirectory(destDir));

                    }, throwable -> {
                        Log.e(TAG, "rescan directory with exception : " + throwable);
                    });
        } else {
            Log.w(TAG, "rescanImageDirectory: 不扫描目录");
        }

        // Todo load grouped image list
/*
        loadImageGroupList(destDir, GroupMode.WEEK, false, SortWay.DATE, SortOrder.DESC)
                .subscribe(imageGroups -> {
                    Log.d(TAG, "rescan week grouped directory: " + destDir);
                }, throwable -> {

                });

        loadImageGroupList(destDir, GroupMode.MONTH, false, SortWay.DATE, SortOrder.DESC)
                .subscribe(imageGroups -> {
                    Log.d(TAG, "rescan month grouped directory: " + destDir);
                }, throwable -> {

                });*/
    }

    private void rescanFolderList(String actionDesc) {

        loadFolderList(false)
                .subscribeOn(Schedulers.io())
                .subscribe(model -> {
                    mBus.post(new RescanFolderListMessage(actionDesc));
                }, throwable -> {
                    Log.d(TAG, "rescanFolderList: " + throwable);
                });
    }

    public List<File> getSortedSubDirectories(File directory) throws FileNotFoundException {
        if (directory == null) {
            throw new FileNotFoundException("Cannot found camera directory.");
        }

        File[] allFiles = directory.listFiles((file) -> file.isDirectory() && !file.getName().startsWith("."));
        if (allFiles == null) {
            return new LinkedList<>();
        }

        List<File> allFileList = new LinkedList<>(Arrays.asList(allFiles));
        allFileList.add(directory);

        // Long.compare(file2.lastModified(), file.lastModified())
        return Stream.of(allFileList)
                .sorted(FOLDER_LIST_NAME_ASC_COMPARATOR)
                .toList();
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
     * 加载多媒体文件（图片、视频）列表，按照参数 {@code param} 指定的排序方式返回列表。
     *
     * @param directory
     * @param param     指定加载参数：是否有限从缓存加载，是否加载媒体信息，排序
     * @return
     */
    public Observable<List<MediaFile>> loadMediaFileList(File directory, LoadMediaFileParam param) {
        return Observable.create(
                e -> {

                    mSortWay = param.getSortWay();
                    mSortOrder = param.getSortOrder();

                    Log.d(TAG, "loadMediaFileList() called with: directory = [" + directory + "], param = [" + param + "]");
                    if (directory == null) {
                        e.onError(new IllegalArgumentException("Argument 'directory' should not be null"));
                        return;
                    }
                    if (!directory.exists()) {
                        e.onError(new FileNotFoundException("Directory not found : " + directory));
                        return;
                    }

                    if (param.isFromCacheFirst()) {

                        try {
                            Watch watch = Watch.now();
                            mMediaFileListMapGuard.readLock();
                            MediaFileList mediaFileList = mMediaFileListMap.get(directory);
                            if (mediaFileList != null) {

                                // 1 需要加载媒体文件信息并且缓存已经加载文件信息
                                // 2 或者不需要加载媒体文件信息时，
                                // 可以从缓存读取
                                if (!param.isLoadMediaInfo() || mediaFileList.isLoadedExtraInfo()) {
                                    if (mediaFileList.getMediaFiles() != null) {
                                        List<MediaFile> mediaFiles = Stream.of(mediaFileList.getMediaFiles())
                                                .sorted(getMediaFileComparator(param))
                                                .toList();

                                        //Log.d(TAG, "loadMediaFileList: return media file list with comparator : " );
                                        e.onNext(mediaFiles);
                                        e.onComplete();
                                        watch.logGlanceMS(TAG, "load media files from cache and sort it");
                                        return;
                                    }
                                }
                            }
                        } finally {
                            mMediaFileListMapGuard.readUnlock();
                        }
                    }

                    Watch watch = Watch.now();
                    // Get media file(image/video) list and sort
                    List<File> imageFiles = ImageModule.getInstance().listMediaFiles(directory);

                    Comparator<MediaFile> comparator = getMediaFileComparator(param);

                    List<MediaFile> sortedMediaFiles;
                    if (param.isLoadMediaInfo()) {

                        // Load video/image resolution, video duration, file disk usage,
                        sortedMediaFiles = Stream.of(imageFiles)
                                .map(file -> {

                                    // File length
                                    MediaFile mediaFile = new MediaFile();
                                    mediaFile.setFile(file);
                                    mediaFile.setFileSize(file.length());
                                    mediaFile.setDate(new Date(file.lastModified()));

                                    // 填充媒体文件额外信息

                                    // Resolution
                                    if (PathUtils.isVideoFile(file.getAbsolutePath())) {
                                        mediaFile.setVideoDuration(
                                                MediaUtils.getVideoFileDuration(mContext, file));
                                    } else if (PathUtils.isStaticImageFile(file.getAbsolutePath())) {
                                        mediaFile.setMediaResolution(MediaUtils.getImageResolution(file));
                                    }

                                    return mediaFile;
                                })
                                .sorted(comparator)
                                .toList();

                        watch.logGlanceMS(TAG, "Load media files details");
                    } else {

                        sortedMediaFiles = Stream.of(imageFiles)
                                .map(file -> {
                                    MediaFile mediaFile = new MediaFile();
                                    mediaFile.setFile(file);
                                    mediaFile.setFileSize(file.length());
                                    mediaFile.setDate(new Date(file.lastModified()));
                                    return mediaFile;
                                })
                                .sorted(comparator)
                                .toList();

                        watch.logGlanceMS(TAG, "Load media files basic information");
                    }

                    // Cache media file list
                    cacheImageList(directory, sortedMediaFiles, param);

                    e.onNext(sortedMediaFiles);
                    e.onComplete();
                })
                ;
    }

    private Comparator<MediaFile> getMediaFileComparator(LoadMediaFileParam param) {
        Comparator<MediaFile> comparator = null;
        switch (param.getSortWay()) {

            case NAME: {
                switch (param.getSortOrder()) {

                    case DESC:
                        comparator = MediaFile.MEDIA_FILE_NAME_DESC_COMPARATOR;
                        break;
                    case ASC:
                        comparator = MediaFile.MEDIA_FILE_NAME_ASC_COMPARATOR;
                        break;
                    default:
                        comparator = MediaFile.MEDIA_FILE_NAME_DESC_COMPARATOR;
                        break;
                }
            }
            break;
            case SIZE:
                switch (param.getSortOrder()) {

                    case DESC:
                        comparator = MediaFile.MEDIA_FILE_LENGTH_DESC_COMPARATOR;
                        break;
                    case ASC:
                        comparator = MediaFile.MEDIA_FILE_LENGTH_ASC_COMPARATOR;
                        break;
                    default:
                        comparator = MediaFile.MEDIA_FILE_LENGTH_DESC_COMPARATOR;
                        break;
                }
                break;
            case DATE: {
                switch (param.getSortOrder()) {

                    case DESC:
                        comparator = MediaFile.MEDIA_FILE_DATE_DESC_COMPARATOR;
                        break;
                    case ASC:
                        comparator = MediaFile.MEDIA_FILE_DATE_ASC_COMPARATOR;
                        break;
                    default:
                        comparator = MediaFile.MEDIA_FILE_DATE_ASC_COMPARATOR;
                        break;
                }
            }
            break;
            default:
                Log.w(TAG, "getMediaFileComparator: return default comparator for sort way : " + param.getSortWay());
                comparator = MediaFile.MEDIA_FILE_DATE_DESC_COMPARATOR;
                break;
        }
        return comparator;
    }

    private void cacheImageList(@NonNull File dir, @NonNull List<MediaFile> mediaFiles, LoadMediaFileParam param) {
//        Log.d(TAG, "cacheImageList() called with: dir = [" + dir + "], mediaFiles = [" + mediaFiles + "], param = [" + param + "]");

        mMediaFileListMapGuard.writeConsume(object ->
                object.put(dir, new MediaFileList()
                        .setMediaFiles(mediaFiles)
                        .setLoadedExtraInfo(param.isLoadMediaInfo())));
    }

    public List<File> listMediaFiles(File dir) {
        if (!dir.isDirectory()) {
            throw new InvalidParameterException("参数 'dir' 对应的目录（" + dir.getAbsolutePath() + "）不存在：");
        }

        File[] allFiles = dir.listFiles(PathUtils.MEDIA_FILENAME_FILTER);
        if (allFiles == null) {
            return new LinkedList<>();
        }

        return Arrays.asList(allFiles);
//        return Stream.of(allFiles)
//                .sorted((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()))
//                .toList();
    }

    public Observable<List<ImageFolder>> loadGalleryFolderList() {

        return loadFolderList(true)
                .map(folderModel -> {
                    List<FolderModel.ContainerFolder> containerFolders = folderModel.getContainerFolders();

                    List<ImageFolder> imageFolderList = new LinkedList<ImageFolder>();
                    for (int i = 0; i < containerFolders.size(); i++) {
                        FolderModel.ContainerFolder containerFolder = containerFolders.get(i);
                        List<ImageFolder> folders = containerFolder.getFolders();
                        imageFolderList.addAll(folders);
                    }

                    return imageFolderList;
                })
                ;
    }

    public String getSectionFileName(int position) {
        try {
            mFolderModelRWLock.readLock().lock();

            int sectionForPosition = getSectionForPosition(position);
            FolderModel.ContainerFolder containerFolder = mFolderModel.getContainerFolders().get(sectionForPosition);
            return containerFolder.getName();
        } finally {
            mFolderModelRWLock.readLock().unlock();
        }
    }

    @Deprecated
    void removeCachedFiles(String dirPath, List<File> files) {
        mImageListMap.containsKey(dirPath);
        rescanImageDirectory(new File(dirPath), false);
    }

    /**
     * 删除文件，并把在一个目录中的文件扫描一次目录，缓存，然后通知目录发送变化
     *
     * @param files 指定要删除的文件列表
     * @return 返回已经删除的文件个数和删除失败的文件列表
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

                            // 扫描对应的图片目录
                            rescanImageDirectory(new File(dir), false);

                            // 扫描文件夹列表
                            rescanFolderList("remove files");
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

    public Observable<Boolean> renameDirectory(File srcDir, String newDirName) {
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

            try {
                mFolderModelRWLock.writeLock().lock();

                ImageFolder imageFolder;
                List<FolderModel.ContainerFolder> containerFolders = mFolderModel.getContainerFolders();
                boolean found = false;
                for (int i = 0, parentFolderInfosSize = containerFolders.size(); i < parentFolderInfosSize; i++) {
                    FolderModel.ContainerFolder containerFolder = containerFolders.get(i);
                    List<ImageFolder> folders = containerFolder.getFolders();

                    for (int i1 = 0, foldersSize = folders.size(); i1 < foldersSize; i1++) {
                        imageFolder = folders.get(i1);
                        if (imageFolder.getFile().equals(srcDir)) {
                            found = true;
                            imageFolder.setName(newDirName);
                            File newDir = new File(srcDir.getParentFile(), newDirName);
                            imageFolder.setFile(newDir);

                            Log.d(TAG, "renameDirectory: rename folder [" + srcDir.getName() + "] in cache to new name [" + newDirName + "]");

                            // Notify
                            mBus.post(new RenameDirectoryMessage()
                                    .setOldDirectory(srcDir)
                                    .setNewDirectory(destDir));
                            break;
                        }
                    }

                    if (found) {
                        List<ImageFolder> sortedImageFolderList = Stream.of(folders)
                                .sorted((o1, o2) -> StringUtils.compare(o1.getName(), o2.getName()))
                                .toList();
                        containerFolder.setFolders(sortedImageFolderList);

                        mBus.post(new RescanFolderListMessage("renameDirectory"));
                    }
                }

                if (!found) {
                    Log.w(TAG, "renameDirectory: rename director successfully, but update model data failed.");

                    e.onError(new Exception("Cannot found the specified directory : " + srcDir));
                    return;
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                mFolderModelRWLock.writeLock().unlock();
            }


            e.onNext(true);
            e.onComplete();
        });
    }

    /**
     * Remove file.
     * Generate {@link RemoveFileMessage} event.
     *
     * @param file The file to remove.
     * @return
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

                mMediaFileListMapGuard.writeConsume(object -> {
                    MediaFileList mediaFileList = object.get(file.getParentFile());
                    if (mediaFileList != null) {
                        List<MediaFile> mediaFiles = mediaFileList.getMediaFiles();

                        int i = org.apache.commons.collections4.ListUtils.indexOf(mediaFiles, object1 -> object1.getFile().equals(file));
                        if (i != -1) {
                            mediaFiles.remove(i);
                            Log.d(TAG, "removeFile: " + file + " at " + i);
                        }
                    }
                });

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

            if (dir.exists()) {
                throw new FileExistsException("File already exists : " + dir);
            }

            FileUtils.forceMkdir(dir);

            // 更新缓存
            List<FolderModel.ContainerFolder> containerFolders = Stream.of(mFolderModel.getContainerFolders())
                    .filter(value -> SystemUtils.isSameFile(value.getFile(), dir.getParentFile()))
                    .limit(1)
                    .toList();

            if (!containerFolders.isEmpty()) {
                FolderModel.ContainerFolder containerFolder = containerFolders.get(0);

                List<ImageFolder> folders = containerFolder.getFolders();
                folders.add(0, createImageFolder(dir));

                containerFolder.setFolders(
                        Stream.of(folders)
                                .sorted((o1, o2) -> o1.getFile().compareTo(o2.getFile()))
                                .toList());

                mBus.post(new FolderModelChangeMessage());
            }


//            loadFolderList(false)
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

    /**
     * 删除目录，在操作成功后将产生 {@link FolderModelChangeMessage} EventBus 消息。
     *
     * @param dir
     * @param forceDelete
     * @return
     * @see FolderModelChangeMessage
     */
    public Observable<Boolean> removeFolder(File dir, boolean forceDelete) {

        return Observable.create(e -> {
            if (dir == null) {
                throw new IllegalArgumentException("dir must not be null");
            }
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("Not a directory");
            }

            if (!forceDelete) {
                File[] files = dir.listFiles();
                if (files != null && files.length > 0) {
                    Log.d(TAG, "removeFolder: 有文件不能移除");
                    throw new NotEmptyException("Directory is not empty.");
                }
            }

            // 删除目录，包括里面的任何子目录
            FileUtils.deleteDirectory(dir);

            // 通知目录删除
            mBus.post(new cn.intret.app.picgo.model.event.DeleteFolderMessage(dir));

            try {
                mFolderModelRWLock.writeLock().lock();
                Stream.of(mFolderModel.getContainerFolders())
                        .forEach(containerFolder -> {
                            List<ImageFolder> folders = containerFolder.getFolders();
                            int i = org.apache.commons.collections4.ListUtils.indexOf(folders,
                                    object -> SystemUtils.isSameFile(object.getFile(), dir));

                            if (i != -1) {
                                Log.d(TAG, "removeFolder: remove folder at " + i);
                                containerFolder.getFolders().remove(i);
                            }
                        });

                mBus.post(new FolderModelChangeMessage());
            } finally {
                mFolderModelRWLock.writeLock().unlock();
            }

            e.onNext(true);
            e.onComplete();

        });
    }

    /*
     * 移动目录
     */

    public Observable<Integer> moveFilesToDirectory(File destDir, List<File> files) {
        return Observable.create(e -> {
            MoveFileResult result = new MoveFileResult();
            int i = moveFilesTo(destDir, files, false, result, false);

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

    /**
     * 移动文件到指定的目录，在源目录和目标目录上都会产生 {@link RescanImageDirectoryMessage} 通知。
     *
     * @param destDir
     * @param sourceFiles
     * @param detectConflict
     * @param deleteIfTargetExists
     * @return
     */
    public Observable<MoveFileResult> moveFilesToDirectory(File destDir, List<File> sourceFiles,
                                                           boolean detectConflict, boolean deleteIfTargetExists) {
        return Observable.create(e -> {
            MoveFileResult result = new MoveFileResult();
            int i = moveFilesTo(destDir, sourceFiles, detectConflict, result, deleteIfTargetExists);

            Log.d(TAG, "moveFilesToDirectory: 移动" +
                    result.getSuccessFiles().size() +
                    "/" + sourceFiles.size() + "个文件到" + destDir);

            // Rescan source dirs
            Stream.of(sourceFiles)
                    .groupBy(File::getParent)
                    .forEach(entry -> rescanImageDirectory(new File(entry.getKey()), false));

            // Rescan target dir
            rescanImageDirectory(destDir, false);

            e.onNext(result);
            e.onComplete();
        });
    }

    public Observable<MoveFileDetectResult> detectFileConflict(File destDir, List<File> sourceFiles) {
        return Observable.create(e -> {

            if (sourceFiles.isEmpty()) {
                throw new IllegalArgumentException("Source file list is empty.");
            }

            List<Pair<File, File>> canMoveFiles = new LinkedList<Pair<File, File>>();
            List<Pair<File, File>> conflictFiles = detectFileConflict(destDir, sourceFiles, canMoveFiles);

            MoveFileDetectResult moveFileDetectResult = new MoveFileDetectResult()
                    .setCanMoveFiles(canMoveFiles)
                    .setConflictFiles(conflictFiles)
                    .setTargetDir(destDir);

            e.onNext(moveFileDetectResult);
            e.onComplete();
        });
    }

    /**
     * 检测源文件列表在别的目录中是否出现
     * @param sourceFiles
     * @return
     */
    public Observable<DetectFileExistenceResult> detectFileExistence(List<File> sourceFiles) {
        return Observable.create(e -> {
            DetectFileExistenceResult result = new DetectFileExistenceResult();
            try {
                mFolderModelRWLock.readLock().lock();

                List<FolderModel.ContainerFolder> containerFolders = mFolderModel.getContainerFolders();
                for (int i = 0, containerFoldersSize = containerFolders.size(); i < containerFoldersSize; i++) {
                    FolderModel.ContainerFolder containerFolder = containerFolders.get(i);
                    for (ImageFolder imageFolder : containerFolder.getFolders()) {
                        List<Pair<File, File>> conflictFiles = intersectMoveFiles(
                                imageFolder.getFile(),
                                Arrays.asList(imageFolder.getMediaFiles()), sourceFiles, null);
                        if (!conflictFiles.isEmpty()) {
                            result.getExistedFiles()
                                    .put(imageFolder.getFile(),
                                            Stream.of(conflictFiles)
                                                    .map(fileFilePair -> fileFilePair.first)
                                                    .toList()
                                    );
                        }
                    }
                }

                e.onNext(result);
                e.onComplete();

            } catch (Throwable th) {
                e.onError(th);
            } finally {
                mFolderModelRWLock.readLock().unlock();
            }
        });
    }

    /**
     * @param destDir
     * @param destDirFiles All the files should in the same directory
     * @param sourceFiles  The files are not always in the same directory.
     * @return
     */
    @NonNull
    private List<Pair<File, File>> intersectMoveFiles(File destDir,
                                                      final List<File> destDirFiles,
                                                      final List<File> sourceFiles,
                                                      @Nullable List<Pair<File, File>> outCanMoveFiles) {
        if (outCanMoveFiles != null) {
            outCanMoveFiles.clear();
        }

        if (destDirFiles.isEmpty()) {
            // No conflict files
            Stream.of(sourceFiles)
                    .forEach(file -> {
                        if (outCanMoveFiles != null) {
                            outCanMoveFiles.add(new Pair<>(new File(destDir, file.getName()), file));
                        }
                    });
            return new ArrayList<>();
        }

        final List<Pair<File, File>> result = new ArrayList<>();

        File dir = destDirFiles.get(0).getParentFile();

        List<String> destDirFileNames = PathUtils.fileListToNameList(destDirFiles);
        final HashSet<String> destDirFileNameHashSet = new HashSet<String>(destDirFileNames);

        for (final File sourceFile : sourceFiles) {

            String name = sourceFile.getName();
            if (destDirFileNameHashSet.contains(name)) {
                result.add(new Pair<File, File>(new File(dir, name), sourceFile));
                // TODO if the source files are in the same directory, we should uncomment the code below
                // destDirFileNameHashSet.remove(l);
            } else {
                if (outCanMoveFiles != null) {
                    outCanMoveFiles.add(new Pair<>(new File(dir, name), sourceFile));
                }
            }
        }
        return result;
    }

    private int moveFilesTo(File destDir, List<File> sourceFiles, boolean detectConflict, MoveFileResult outResult,
                            boolean deleteIfExists) {
        if (destDir == null) {
            throw new IllegalArgumentException("Argument 'destDir' is null.");
        }

        if (!destDir.isDirectory()) {
            throw new IllegalArgumentException("Argument 'destDir' is not a valid directory.");
        }

        if (ListUtils.isEmpty(sourceFiles)) {
            return 0;
        }

        // Detect file name conflict
        List<Pair<File, File>> canMoveFiles = new LinkedList<>();
        List<Pair<File, File>> conflictFiles;
        if (detectConflict) {
            conflictFiles = detectFileConflict(destDir, sourceFiles, canMoveFiles);
            outResult.setConflictFiles(conflictFiles);
        }

        List<Pair<File, File>> successFiles = new LinkedList<>();
        List<Pair<File, File>> failedFiles = new LinkedList<>();

        if (detectConflict) {

            int successCount = 0;
            try {

                for (Pair<File, File> filePair : canMoveFiles) {
                    if (filePair.second == null) {
                        continue;
                    }
                    FileUtils.moveFileToDirectory(filePair.second, destDir, true);
                    Log.d(TAG, "move file " + filePair.second + " to " + destDir);

                    successFiles.add(filePair);

                    ++successCount;
                }

            } catch (Exception e) {
                Log.e(TAG, "move files to " + destDir + " failed.");
                e.printStackTrace();
            }

            outResult.setSuccessFiles(successFiles);
            outResult.setFailedFiles(failedFiles);

            return successCount;
        } else {

            int successCount = 0;
            File srcFile;
            try {

                for (int i = 0; i < sourceFiles.size(); i++) {
                    srcFile = sourceFiles.get(i);
                    if (srcFile == null) {
                        continue;
                    }
                    File destFile = new File(destDir, srcFile.getName());
                    if (destFile.exists() && deleteIfExists) {
                        Log.d(TAG, "moveFilesTo: delete target file first : " + destFile);
                        destFile.delete();
                    }
                    FileUtils.moveFileToDirectory(srcFile, destDir, true);
                    Log.d(TAG, "move file " + srcFile + " to " + destDir);

                    successFiles.add(new Pair<>(destFile, srcFile));

                    successCount++;
                }
            } catch (IOException e) {
                Log.e(TAG, "move files to " + destDir + " failed.");
                e.printStackTrace();
            }

            outResult.setSuccessFiles(successFiles);
            outResult.setFailedFiles(failedFiles);
            return successCount;
        }
    }

    /*
     * @return Conflict files
     */
    private List<Pair<File, File>> detectFileConflict(File destDir, List<File> sourceFiles,
                                                      List<Pair<File, File>> outCanMoveFiles) {
        return intersectMoveFiles(destDir, Stream.of(destDir.listFiles()).toList(),
                sourceFiles, outCanMoveFiles);

    }

    /**
     * 任何解决后的图片，它所在的目录都会被扫描一次。
     * @param compareItems
     * @return
     * @
     */
    public Observable<CompareItemResolveResult> resolveFileNameConflict(final List<CompareItem> compareItems) {
        return Observable.create(e -> {
            if (ListUtils.isEmpty(compareItems)) {
                throw new IllegalArgumentException("Argument 'compareItems' shouldn't be null/empty.");
            }

            List<File> deletedFiles = new LinkedList<>();

            for (CompareItem compareItem : compareItems) {

                switch (compareItem.getResult()) {

                    case KEEP_SOURCE: {
                        File targetFile = compareItem.getTargetFile();
                        if (targetFile == null) {
                            e.onError(new IllegalStateException("Cannot remove 'null' target file for " + compareItem.getResult()));
                            continue;
                        } else {
                            if (!targetFile.exists()) {
                                e.onError(new FileNotFoundException("File not found : " + targetFile));
                                continue;
                            }
                            if (targetFile.isDirectory()) {
                                e.onError(new IllegalStateException("File shouldn't be a directory : " + targetFile));
                                continue;
                            } else {
                                boolean delete = targetFile.delete();
                                if (delete) {
                                    deletedFiles.add(targetFile);
                                }
                                e.onNext(new CompareItemResolveResult()
                                        .setCompareItem(compareItem)
                                        .setResolved(delete)
                                );
                            }
                        }
                    }
                    break;
                    case KEEP_TARGET: {
                        File sourceFile = compareItem.getSourceFile();
                        if (sourceFile == null) {
                            e.onError(new IllegalStateException("Cannot remove 'null' source file for " + compareItem.getResult()));
                            continue;
                        }

                        if (!sourceFile.exists()) {
                            e.onError(new FileNotFoundException("File not found : " + sourceFile));
                            continue;
                        }

                        if (sourceFile.isDirectory()) {
                            e.onError(new IllegalStateException("Cannot remove a directory for  : " + compareItem.getResult()));
                            continue;
                        }

                        boolean delete = sourceFile.delete();
                        if (delete) {
                            deletedFiles.add(sourceFile);
                        }
                        e.onNext(new CompareItemResolveResult()
                                .setCompareItem(compareItem)
                                .setResolved(delete)
                        );
                    }
                    break;
                    case KEEP_BOTH: {
                        e.onNext(new CompareItemResolveResult()
                                .setCompareItem(compareItem)
                                .setResolved(true));
                    }
                    break;
                    case NONE:
                        e.onNext(new CompareItemResolveResult()
                                .setCompareItem(compareItem)
                                .setResolved(false)
                        );
                        break;
                }
            } // END of for

            Stream.of(deletedFiles)
                    .groupBy(File::getParentFile)
                    .forEach(fileListEntry -> {
                        File dir = fileListEntry.getKey();
                        rescanImageDirectory(dir, false);
                    });
            e.onComplete();
        });
    }

    public Single<List<ImageFileInformation>> loadImageFilesInfo(@NonNull List<File> files) {
        return Single.create(e -> {
            if (files == null) {
                throw new IllegalArgumentException("参数为空");
            }


            List<ImageFileInformation> fileInformationList = Stream.of(files)
                    .map(file -> {
                        try {
                            return getImageFileInformation(file);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        return null;
                    })
                    .filter(cn.intret.app.picgo.utils.Objects::nonNull)
                    .toList();

            e.onSuccess(fileInformationList);
        });
    }

    public Single<Map<File, ImageFileInformation>> loadImageFilesInfoMap(@NonNull List<File> files) {
        return Single.create(e -> {
            if (files == null) {
                throw new IllegalArgumentException("参数为空");
            }

            Map<File, ImageFileInformation> map = new LinkedHashMap<File, ImageFileInformation>();


            Stream.of(files).forEach(file -> {
                try {
                    map.put(file, getImageFileInformation(file));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            });

            e.onSuccess(map);
        });
    }


    /**
     * @implNote https://developer.android.com/reference/android/media/ExifInterface.html
     */
    public Observable<ImageFileInformation> loadImageInfo(File mediaFile) {
        return Observable.create(e -> {
            if (mediaFile == null) {
                throw new IllegalArgumentException("ImageFile should not be null.");
            }
            if (mediaFile.isDirectory()) {
                throw new IllegalArgumentException("File should not be a directory.");
            }

            ImageFileInformation info = getImageFileInformation(mediaFile);

            e.onNext(info);
            e.onComplete();
        });
    }

    @NonNull
    private ImageFileInformation getImageFileInformation(File mediaFile) throws IOException {
        ImageFileInformation info;
        info = new ImageFileInformation();

        String fileAbsPath = mediaFile.getAbsolutePath();
        boolean staticImageFile = PathUtils.isStaticImageFile(fileAbsPath);
        boolean videoFile = PathUtils.isVideoFile(fileAbsPath);
        if (staticImageFile) {
            Size imageResolution = MediaUtils.getImageResolution(mediaFile);
            info.setMediaResolution(imageResolution);

        } else {
            if (videoFile) {
                Size videoResolution = MediaUtils.getVideoResolution(mContext, mediaFile);

                info.setMediaResolution(videoResolution);
                info.setVideoDuration(MediaUtils.getVideoFileDuration(mContext, mediaFile));
            } else {
                Log.w(TAG, "loadImageInfo: don't load media file information : " + mediaFile);
            }
        }

        info.setLastModified(mediaFile.lastModified());
        info.setFileLength(mediaFile.length());


        // Exif
        if (PathUtils.isExifFile(fileAbsPath)) {
            ExifInterface exifInterface = new ExifInterface(fileAbsPath);
            info.setExif(exifInterface);
        }
        return info;
    }

    public Observable<Boolean> hiddenFolder(File selectedDir) {
        return Observable.create(e -> {
            try {
                mFolderModelRWLock.writeLock().lock();

                int index = -1;
                int subIndex = -1;

                List<FolderModel.ContainerFolder> containerFolders = mFolderModel.getContainerFolders();
                for (int i = 0, containerFoldersSize = containerFolders.size(); i < containerFoldersSize; i++) {
                    FolderModel.ContainerFolder containerFolder = containerFolders.get(i);
                    for (int ii = 0; ii < containerFolder.getFolders().size(); ii++) {
                        ImageFolder imageFolder = containerFolder.getFolders().get(ii);
                        if (imageFolder.getFile().equals(selectedDir)) {
                            index = i;
                            subIndex = ii;
                            break;
                        }
                    }
                }

                if (index != -1) {
                    containerFolders.get(index).getFolders().remove(subIndex);

                    e.onNext(true);
                    e.onComplete();
                } else {
                    e.onNext(false);
                    e.onComplete();
                }
            } finally {
                mFolderModelRWLock.writeLock().unlock();
            }
        });
    }
}

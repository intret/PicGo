package cn.intret.app.picgo.model;


import android.content.Context;
import android.util.Log;

import com.annimon.stream.Collector;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BiConsumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Supplier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.intret.app.picgo.app.CoreModule;
import cn.intret.app.picgo.utils.DateTimeUtils;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.SystemUtils;
import io.reactivex.Observable;

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

    public Observable<List<ImageGroup>> loadImageGroupList(File directory, GroupMode mode) {
        return Observable.create(e -> {

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
                                List<ImageGroup.Image> itemList = Stream.of(files)
                                        .map(file -> new ImageGroup.Image()
                                                .setFile(file)
                                                .setDate(new Date(file.lastModified())))
                                        .toList();
                                ImageGroup.Image firstItem = ListUtils.firstOf(itemList);
                                ImageGroup.Image lastItem = ListUtils.lastOf(itemList);

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
        });
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

        return Stream.of(allFiles)
                .sorted((file, file2) -> Long.compare(file2.lastModified(), file.lastModified()))
                .toList();
    }

    public Observable<List<ImageFolder>> loadGalleryFolderList() {

        return loadAvailableFolderListModel()
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
}

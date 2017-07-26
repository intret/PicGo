package cn.intret.app.picgo.model;

import com.annimon.stream.Stream;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;

import cn.intret.app.picgo.utils.SystemUtils;

/**
 * Gallery images service
 */

public class GalleryService {
    private static final GalleryService ourInstance = new GalleryService();

    public static GalleryService getInstance() {
        return ourInstance;
    }

    private GalleryService() {
    }

    public List<File> loadLatestPictures(int count) throws FileNotFoundException {
        File cameraDir = SystemUtils.getCameraDir();
        if (cameraDir == null) {
            throw new FileNotFoundException("Cannot found camera directory.");
        }

        File[] allFiles = cameraDir.listFiles((file) -> file.isFile() && !file.getName().startsWith("."));
        if (allFiles == null) {
            return new LinkedList<>();
        }

        return Stream.of(allFiles)
                .sorted((file, file2) -> Long.compare(file2.lastModified(), file.lastModified()))
                .takeUntilIndexed(0, 1, (index, value) -> {
                    if (index == count) {
                        return true;
                    }
                    return false;
                })
                .toList();

    }

    public List<File> loadAllFolderImages(File dir) {
        if (!dir.isDirectory()) {
            throw new InvalidParameterException("参数 'dir' 对应的目录（" + dir.getAbsolutePath() + "）不存在：");
        }

        File[] allFiles = dir.listFiles((file) -> file.isFile() && !file.getName().startsWith("."));
        if (allFiles == null) {
            return new LinkedList<>();
        }

        return Stream.of(allFiles)
                .sorted((file, file2) -> Long.compare(file2.lastModified(), file.lastModified()))
                .toList();
    }

    public List<File> getAllPictureFolders() throws FileNotFoundException {
        return getSortedSubDirectories(SystemUtils.getDCIMDir());
    }

    public List<File> getAllDCIMFolders() throws FileNotFoundException {
        File dcimDir = SystemUtils.getPicturesDir();
        return getSortedSubDirectories(dcimDir);
    }

    private List<File> getSortedSubDirectories(File directory) throws FileNotFoundException {
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
}

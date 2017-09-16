package cn.intret.app.picgo.utils;


import android.support.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PathUtils {
    static final String[] EXIF_FILE_EXTENSIONS = {
            "JPEG", "DNG", "CR2", "NEF", "NRW", "ARW", "RW2", "ORF", "RAF"};
    static final String[] STATIC_FILE_EXTENSIONS = {
            "PNG", "JPEG", "JPG", "WEBP"
    };

    static final String[] VIDEO_FILE_EXTENSIONS = {
           "MP4","MOV","MPG","MPEG","RMVB"
    };


    public static boolean isExifFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return false;
        }
        String extension = FilenameUtils.getExtension(filePath);
        if (StringUtils.isEmpty(extension)) {
            return false;
        }

        return ArrayUtils.contains(EXIF_FILE_EXTENSIONS, extension.toUpperCase());
    }

    public static boolean isStaticImageFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return false;
        }
        String extension = FilenameUtils.getExtension(filePath);
        if (StringUtils.isEmpty(extension)) {
            return false;
        }

        return ArrayUtils.contains(STATIC_FILE_EXTENSIONS, extension.toUpperCase());
    }

    public static boolean isVideoFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return false;
        }
        String extension = FilenameUtils.getExtension(filePath);
        if (StringUtils.isEmpty(extension)) {
            return false;
        }

        return ArrayUtils.contains(VIDEO_FILE_EXTENSIONS, extension.toUpperCase());
    }

    public static boolean isGifFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return false;
        }
        String extension = FilenameUtils.getExtension(filePath);
        if (StringUtils.isEmpty(extension)) {
            return false;
        }

        return extension.toLowerCase().equals("gif");
    }

    public static boolean isVideoFile(File file) {
        return file != null && isVideoFile(file.getAbsolutePath());
    }

    public static boolean isGifFile(File file) {
        return file != null && isGifFile(file.getAbsolutePath());
    }

    public static List<String> fileListToPathList(List<File> files) {
        if (files == null) {
            return null;
        }
        return com.annimon.stream.Stream.of(files).map(File::getAbsolutePath).toList();
    }

    public static List<String> fileListToNameList(List<File> files) {
        if (files == null) {
            return null;
        }
        return com.annimon.stream.Stream.of(files).map(File::getName).toList();
    }


    @Nullable
    public static ArrayList<String> fileListToPathArrayList(@Nullable List<File> files) {
        if (files == null) {
            return null;
        }
        return new ArrayList<String>(com.annimon.stream.Stream.of(files).map(File::getAbsolutePath).toList());
    }

    public static List<File> stringArrayListToFileList(List<String> strings) {
        return com.annimon.stream.Stream.of(strings).map(File::new).toList();
    }
}

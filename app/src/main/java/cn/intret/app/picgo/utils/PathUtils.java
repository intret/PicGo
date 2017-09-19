package cn.intret.app.picgo.utils;


import android.support.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class PathUtils {


    private static final String[] EXIF_FILE_EXTENSIONS = {
            "jpeg", "dng", "cr2", "nef", "nrw", "arw", "rw2", "orf", "raf"};

    private static final String[] STATIC_IMAGE_FILE_EXTENSIONS = {
            "png", "jpeg", "jpg", "webp"
    };

    private static final String[] ANIMATION_IMAGE_FILE_EXTENSIONS = {
            "gif"
    };

    private static final String[] VIDEO_FILE_EXTENSIONS = {
            "mp4", "mov", "mpg", "mpeg", "rmvb"
    };

    private static final String[] MEDIA_FILE_EXTENSIONS = {
            "png","jpeg","jpg","webp",
            "gif",
            "mp4","mov","mpg","mpeg","rmvb","avi"
    };

    public static final FilenameFilter MEDIA_FILENAME_FILTER = (dir, name) -> {
        return !name.startsWith(".") && FilenameUtils.isExtension(name.toLowerCase(), MEDIA_FILE_EXTENSIONS);
    };

    public static boolean isExifFile(String filePath) {
        return FilenameUtils.isExtension(StringUtils.lowerCase(filePath), EXIF_FILE_EXTENSIONS);
    }

    public static boolean isStaticImageFile(String filePath) {
        return FilenameUtils.isExtension(StringUtils.lowerCase(filePath), STATIC_IMAGE_FILE_EXTENSIONS);
    }

    public static boolean isVideoFile(String filePath) {
        return FilenameUtils.isExtension(StringUtils.lowerCase(filePath), VIDEO_FILE_EXTENSIONS);
    }

    public static boolean isGifFile(String filePath) {
        return FilenameUtils.isExtension(StringUtils.lowerCase(filePath), ANIMATION_IMAGE_FILE_EXTENSIONS);
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

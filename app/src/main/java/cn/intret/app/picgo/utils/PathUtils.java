package cn.intret.app.picgo.utils;


import android.support.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class PathUtils {

    public static boolean isStaticImageFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return false;
        }
        String extension = FilenameUtils.getExtension(filePath);
        if (StringUtils.isEmpty(extension)) {
            return false;
        }
        switch (extension.toLowerCase()) {
            case "png":
            case "jpeg":
            case "jpg":
            case "webp": {
                return true;
            }
        }
        return false;

    }

    public static boolean isVideoFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return false;
        }
        String extension = FilenameUtils.getExtension(filePath);
        if (StringUtils.isEmpty(extension)) {
            return false;
        }
        switch (extension.toLowerCase()) {
            case "mp4":
            case "avi":
            case "mov":
            case "mpg":
            case "mpeg":
            case "rmvb": {
                return true;
            }
        }
        return false;
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

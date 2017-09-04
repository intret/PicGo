package cn.intret.app.picgo.utils;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class PathUtils {

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
}

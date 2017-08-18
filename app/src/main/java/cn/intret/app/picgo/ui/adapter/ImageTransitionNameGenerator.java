package cn.intret.app.picgo.ui.adapter;

import org.apache.commons.io.FilenameUtils;

public class ImageTransitionNameGenerator {
    public static String generateTransitionName(String filePath) {
        return "image:item:" + FilenameUtils.getBaseName(filePath).toLowerCase();
    }
}

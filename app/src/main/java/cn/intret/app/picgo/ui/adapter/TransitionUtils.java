package cn.intret.app.picgo.ui.adapter;

import org.apache.commons.io.FilenameUtils;

public class TransitionUtils {
    public static final String TRANSITION_PREFIX_FILETYPE = "filetype";

    public static String generateTransitionName(String filePath) {
        return "image:item:" + FilenameUtils.getBaseName(filePath).toLowerCase();
    }

    public static String generateTransitionName(String itemName, String filePath) {
        return "image:" + itemName + ":item:" + FilenameUtils.getBaseName(filePath).toLowerCase();
    }
}

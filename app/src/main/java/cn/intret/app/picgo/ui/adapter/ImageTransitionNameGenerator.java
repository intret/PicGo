package cn.intret.app.picgo.ui.adapter;

import java.io.File;

public class ImageTransitionNameGenerator {
    public static String generateTransitionName(String filePath) {
        return "image:item:" + filePath.toLowerCase().hashCode();
    }
}

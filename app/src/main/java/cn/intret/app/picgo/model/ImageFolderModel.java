package cn.intret.app.picgo.model;


import java.io.File;


/**
 * 图片文件夹模型，也就是这个文件中至少有一张图片。
 */
public class ImageFolderModel {
    File mFile;
    int mCount;
    private String mName;

    public File getFile() {
        return mFile;
    }

    public ImageFolderModel setFile(File file) {
        mFile = file;
        return this;
    }

    public int getCount() {
        return mCount;
    }

    public ImageFolderModel setCount(int count) {
        mCount = count;
        return this;
    }

    public ImageFolderModel setName(String name) {
        mName = name;
        return this;
    }

    public String getName() {
        return mName;
    }
}

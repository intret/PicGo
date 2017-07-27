package cn.intret.app.picgo.model;


import java.io.File;
import java.util.List;


/**
 * 图片文件夹模型，也就是这个文件中至少有一张图片。
 */
public class ImageFolder {
    File mFile;
    int mCount;
    private String mName;
    List<File> mThumbList;

    public List<File> getThumbList() {
        return mThumbList;
    }

    public ImageFolder setThumbList(List<File> thumbList) {
        this.mThumbList = thumbList;
        return this;
    }

    public File getFile() {
        return mFile;
    }

    public ImageFolder setFile(File file) {
        mFile = file;
        return this;
    }

    public int getCount() {
        return mCount;
    }

    public ImageFolder setCount(int count) {
        mCount = count;
        return this;
    }

    public ImageFolder setName(String name) {
        mName = name;
        return this;
    }

    public String getName() {
        return mName;
    }
}

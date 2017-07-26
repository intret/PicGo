package cn.intret.app.picgo.model;


import java.io.File;

public class FolderInfo {
    File mFile;
    int mCount;
    private String mName;

    public File getFile() {
        return mFile;
    }

    public FolderInfo setFile(File file) {
        mFile = file;
        return this;
    }

    public int getCount() {
        return mCount;
    }

    public FolderInfo setCount(int count) {
        mCount = count;
        return this;
    }

    public FolderInfo setName(String name) {
        mName = name;
        return this;
    }

    public String getName() {
        return mName;
    }
}

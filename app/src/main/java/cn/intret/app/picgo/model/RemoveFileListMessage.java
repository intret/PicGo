package cn.intret.app.picgo.model;

import java.io.File;
import java.util.List;

class RemoveFileListMessage {
    List<File> mRemovedFiles;
    List<File> mUnRemovedFiles;
    File mDir;
    int mTotal;

    public List<File> getRemovedFiles() {
        return mRemovedFiles;
    }

    public RemoveFileListMessage setRemoveSuccessFiles(List<File> removedFiles) {
        mRemovedFiles = removedFiles;
        return this;
    }

    public List<File> getUnRemovedFiles() {
        return mUnRemovedFiles;
    }

    public RemoveFileListMessage setRemoveFailedFiles(List<File> unRemovedFiles) {
        mUnRemovedFiles = unRemovedFiles;
        return this;
    }

    public File getDir() {
        return mDir;
    }

    public RemoveFileListMessage setDir(File dir) {
        mDir = dir;
        return this;
    }

    public int getTotal() {
        return mTotal;
    }

    public RemoveFileListMessage setTotal(int total) {
        mTotal = total;
        return this;
    }
}

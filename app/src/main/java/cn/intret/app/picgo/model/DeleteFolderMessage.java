package cn.intret.app.picgo.model;


import java.io.File;

/**
 * A folder has been delete.
 */
public class DeleteFolderMessage {
    File mDir;

    public DeleteFolderMessage(File dir) {
        mDir = dir;
    }

    public File getDir() {
        return mDir;
    }

    public DeleteFolderMessage setDir(File dir) {
        mDir = dir;
        return this;
    }
}

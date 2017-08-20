package cn.intret.app.picgo.model;

import java.io.File;

/**
 * Remove file notification message
 */

public class RemoveFileMessage {
    private File mFile;

    public RemoveFileMessage setFile(File file) {
        mFile = file;
        return this;
    }

    public File getFile() {
        return mFile;
    }
}

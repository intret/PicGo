package cn.intret.app.picgo.model.event;

import java.io.File;

/**
 * 重命名目录消息
 */

public class RenameDirectoryMessage {
    private File mOldDirectory;
    private File mNewDirectory;

    public RenameDirectoryMessage setOldDirectory(File oldDirectory) {
        mOldDirectory = oldDirectory;
        return this;
    }

    public File getOldDirectory() {
        return mOldDirectory;
    }

    public RenameDirectoryMessage setNewDirectory(File newDirectory) {
        mNewDirectory = newDirectory;
        return this;
    }

    public File getNewDirectory() {
        return mNewDirectory;
    }
}

package cn.intret.app.picgo.model;

import java.io.File;

/**
 * 文件夹已经被重新扫描的消息
 */

public class DirectoryRescanMessage {
    private File mDirectory;

    public DirectoryRescanMessage setDirectory(File directory) {
        mDirectory = directory;
        return this;
    }

    public File getDirectory() {
        return mDirectory;
    }
}

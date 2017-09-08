package cn.intret.app.picgo.model.event;

import java.io.File;

/**
 * 文件夹已经被重新扫描的消息
 */

public class RescanImageDirectoryMessage {
    private File mDirectory;

    public RescanImageDirectoryMessage setDirectory(File directory) {
        mDirectory = directory;
        return this;
    }

    public File getDirectory() {
        return mDirectory;
    }
}

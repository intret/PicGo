package cn.intret.app.picgo.model.event;

import java.io.File;

/**
 * 文件夹已经被重新扫描的消息
 */

public class RescanImageDirectoryMessage extends BaseMessage {
    private File mDirectory;

    public RescanImageDirectoryMessage() {
    }

    public RescanImageDirectoryMessage(File directory) {
        mDirectory = directory;
    }

    public RescanImageDirectoryMessage(String desc, File directory) {
        super(desc);
        mDirectory = directory;
    }

    public RescanImageDirectoryMessage setDirectory(File directory) {
        mDirectory = directory;
        return this;
    }

    public File getDirectory() {
        return mDirectory;
    }

    @Override
    public String toString() {
        return "RescanImageDirectoryMessage{" +
                "mDirectory=" + mDirectory +
                "} " + super.toString();
    }
}

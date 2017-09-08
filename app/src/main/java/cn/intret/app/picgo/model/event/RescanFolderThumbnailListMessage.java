package cn.intret.app.picgo.model.event;

import java.io.File;
import java.util.List;

/**
 * 目录的缩略图被更新时发送
 */
public class RescanFolderThumbnailListMessage {
    File mDirectory;
    List<File> mThumbnails;

    public File getDirectory() {
        return mDirectory;
    }

    public RescanFolderThumbnailListMessage setDirectory(File directory) {
        this.mDirectory = directory;
        return this;
    }

    public List<File> getThumbnails() {
        return mThumbnails;
    }

    public RescanFolderThumbnailListMessage setThumbnails(List<File> thumbnails) {
        mThumbnails = thumbnails;
        return this;
    }

    @Override
    public String toString() {
        return "RescanFolderThumbnailListMessage{" +
                "mDirectory=" + mDirectory +
                '}';
    }
}

package cn.intret.app.picgo.model;

import android.util.Size;

import java.io.File;
import java.util.Date;

/**
 * Media file (image/video)
 */
public class MediaFile {
    File mFile;
    Date mDate;

    /**
     * Image size or video resolution
     */
    Size mMediaResolution;

    /**
     * File size ( disk usage of file), in bytes
     */
    long mFileSize;


    long mVideoDuration;

    /**
     * Get duration, in ms
     * @return
     */
    public long getVideoDuration() {
        return mVideoDuration;
    }

    public MediaFile setVideoDuration(long videoDuration) {
        mVideoDuration = videoDuration;
        return this;
    }

    public long getFileSize() {
        return mFileSize;
    }

    public MediaFile setFileSize(long fileSize) {
        mFileSize = fileSize;
        return this;
    }

    public Size getMediaResolution() {
        return mMediaResolution;
    }

    public MediaFile setMediaResolution(Size mediaResolution) {
        mMediaResolution = mediaResolution;
        return this;
    }

    public File getFile() {
        return mFile;
    }

    public void setFile(File file) {
        mFile = file;
    }

    public Date getDate() {
        return mDate;
    }

    public MediaFile setDate(Date date) {
        mDate = date;
        return this;
    }

    @Override
    public String toString() {
        return "MediaFile{" +
                "mFile=" + mFile +
                ", mDate=" + mDate +
                ", mMediaResolution=" + mMediaResolution +
                ", mFileSize=" + mFileSize +
                ", mVideoDuration=" + mVideoDuration +
                '}';
    }
}

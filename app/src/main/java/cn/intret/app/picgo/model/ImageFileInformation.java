package cn.intret.app.picgo.model;


import android.media.ExifInterface;
import android.util.Size;

import java.io.File;

public class ImageFileInformation {
    private File mFile;
    private ExifInterface mExif;
    private long mLastModified;
    private long mFileLength;
    private int mImageWidth;
    private int mImageHeight;
    /**
     * 图片分辨率、视频分辨率
     */
    private Size mMediaResolution;
    /**
     * 视频播放时长
     */
    private int mVideoDuration;

    public ImageFileInformation setExif(ExifInterface exif) {
        mExif = exif;
        return this;
    }

    public ExifInterface getExif() {
        return mExif;
    }

    public ImageFileInformation setLastModified(long lastModified) {
        mLastModified = lastModified;
        return this;
    }

    public long getLastModified() {
        return mLastModified;
    }

    public ImageFileInformation setFileLength(long fileLength) {
        mFileLength = fileLength;
        return this;
    }

    public File getFile() {
        return mFile;
    }

    public ImageFileInformation setFile(File file) {
        mFile = file;
        return this;
    }

    /**
     * Get file size, in bytes.
     * @return
     */
    public long getFileLength() {
        return mFileLength;
    }

    public void setImageWidth(int imageWidth) {
        mImageWidth = imageWidth;
    }

    public int getImageWidth() {
        return mImageWidth;
    }

    public void setImageHeight(int imageHeight) {
        mImageHeight = imageHeight;
    }

    public int getImageHeight() {
        return mImageHeight;
    }

    public void setMediaResolution(Size size) {
        mMediaResolution = size;
    }

    public Size getMediaResolution() {
        return mMediaResolution;
    }

    public void setVideoDuration(int videoDuration) {
        mVideoDuration = videoDuration;
    }

    public int getVideoDuration() {
        return mVideoDuration;
    }
}

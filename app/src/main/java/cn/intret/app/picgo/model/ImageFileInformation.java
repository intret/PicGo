package cn.intret.app.picgo.model;


import android.media.ExifInterface;
import android.util.Size;

import java.io.File;

public class ImageFileInformation {
    private ExifInterface mExif;
    private long mLastModifed;
    private long mFileSize;
    private int mImageWidth;
    private int mImageHeight;
    private Size mImageSize;

    public ImageFileInformation setExif(ExifInterface exif) {
        mExif = exif;
        return this;
    }

    public ExifInterface getExif() {
        return mExif;
    }

    public ImageFileInformation setLastModifed(long lastModifed) {
        mLastModifed = lastModifed;
        return this;
    }

    public long getLastModified() {
        return mLastModifed;
    }

    public ImageFileInformation setFileSize(long fileSize) {
        mFileSize = fileSize;
        return this;
    }

    /**
     * Get file size, in bytes.
     * @return
     */
    public long getFileSize() {
        return mFileSize;
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

    public void setImageSize(Size size) {
        mImageSize = size;
    }

    public Size getImageSize() {
        return mImageSize;
    }
}

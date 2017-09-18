package cn.intret.app.picgo.model;

import android.util.Size;

import java.io.File;
import java.util.Comparator;
import java.util.Date;

/**
 * Media file (image/video)
 */
public class MediaFile {

    // TODO: add sort by resolution
    public final static Comparator<MediaFile> MEDIA_FILE_DATE_DESC_COMPARATOR =
            (MediaFile o1, MediaFile o2) -> o2.getDate().compareTo(o1.getDate());
    public final static Comparator<MediaFile> MEDIA_FILE_DATE_ASC_COMPARATOR =
            (MediaFile o1, MediaFile o2) -> o1.getDate().compareTo(o2.getDate());

    public final static Comparator<MediaFile> MEDIA_FILE_NAME_ASC_COMPARATOR =
            (MediaFile o1, MediaFile o2) -> {
                return o1 == null
                        ? (o2 == null ? 0 : 1)
                        : o1.getFile().compareTo(o2.getFile());
            };

    public final static Comparator<MediaFile> MEDIA_FILE_NAME_DESC_COMPARATOR =
            (MediaFile o1, MediaFile o2) -> o2.getFile().compareTo(o1.getFile());


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
     *
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MediaFile)) return false;

        MediaFile mediaFile = (MediaFile) o;

        return mFile != null ? mFile.equals(mediaFile.mFile) : mediaFile.mFile == null;

    }

    @Override
    public int hashCode() {
        return mFile != null ? mFile.hashCode() : 0;
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

package cn.intret.app.picgo.model;

import java.util.Date;
import java.util.List;

/**
 * Image Group
 */

public class ImageGroup {

    Date mStartDate;
    Date mEndDate;
    List<MediaFile> mMediaFiles;

    public Date getStartDate() {
        return mStartDate;
    }

    public ImageGroup setStartDate(Date startDate) {
        mStartDate = startDate;
        return this;
    }

    public Date getEndDate() {
        return mEndDate;
    }

    public ImageGroup setEndDate(Date endDate) {
        mEndDate = endDate;
        return this;
    }

    public List<MediaFile> getMediaFiles() {
        return mMediaFiles;
    }

    public ImageGroup setMediaFiles(List<MediaFile> mediaFiles) {
        mMediaFiles = mediaFiles;
        return this;
    }

    @Override
    public String toString() {
        return "ImageGroup{" +
                "mStartDate=" + mStartDate +
                ", mEndDate=" + mEndDate +
                '}';
    }
}

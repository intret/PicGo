package cn.intret.app.picgo.model;

import java.util.Date;
import java.util.List;

/**
 * Image Group
 */

public class ImageGroup {

    Date mStartDate;
    Date mEndDate;
    List<Image> mImages;

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

    public List<Image> getImages() {
        return mImages;
    }

    public ImageGroup setImages(List<Image> images) {
        mImages = images;
        return this;
    }
}

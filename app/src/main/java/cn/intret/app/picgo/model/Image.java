package cn.intret.app.picgo.model;

import java.io.File;
import java.util.Date;

/**
 * Image
 */
public class Image {
    File mFile;
    Date mDate;

    public File getFile() {
        return mFile;
    }

    public Image setFile(File file) {
        mFile = file;
        return this;
    }

    public Date getDate() {
        return mDate;
    }

    public Image setDate(Date date) {
        mDate = date;
        return this;
    }
}

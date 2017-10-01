package cn.intret.app.picgo.model.user;

import com.google.gson.annotations.SerializedName;

public class RecentRecord {

    @SerializedName("file_path")
    String mFilePath;

    public String getFilePath() {
        return mFilePath;
    }

    public RecentRecord setFilePath(String filePath) {
        mFilePath = filePath;
        return this;
    }

    @Override
    public String toString() {
        return "RecentRecord{" +
                "mFilePath='" + mFilePath + '\'' +
                '}';
    }
}

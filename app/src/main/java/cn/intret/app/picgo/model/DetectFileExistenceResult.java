package cn.intret.app.picgo.model;


import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DetectFileExistenceResult {
    /**
     * Key: folder
     * Value: existed files
     */
    Map<File, List<File>> mExistedFiles = new LinkedHashMap<>();

    public Map<File, List<File>> getExistedFiles() {
        return mExistedFiles;
    }

    public DetectFileExistenceResult setExistedFiles(Map<File, List<File>> existedFiles) {
        mExistedFiles = existedFiles;
        return this;
    }

    @Override
    public String toString() {
        return "DetectFileExistenceResult{" +
                "mExistedFiles=" + mExistedFiles +
                '}';
    }
}

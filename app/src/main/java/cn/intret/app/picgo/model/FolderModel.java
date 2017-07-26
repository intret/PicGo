package cn.intret.app.picgo.model;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 图片文件夹数据模型
 */
public class FolderModel {

    public static class FolderContainerInfo {
        File mFile;
        String mName;

        List<FolderInfo> mFolders;

        public List<FolderInfo> getFolders() {
            return mFolders;
        }

        public FolderContainerInfo setFolders(List<FolderInfo> folders) {
            mFolders = folders;
            return this;
        }

        public File getFile() {
            return mFile;
        }

        public FolderContainerInfo setFile(File file) {
            mFile = file;
            return this;
        }

        public String getName() {
            return mName;
        }

        public FolderContainerInfo setName(String name) {
            mName = name;
            return this;
        }
    }

    List<FolderContainerInfo> mFolderContainerInfos = new LinkedList<>();

    public List<FolderContainerInfo> getFolderContainerInfos() {
        return mFolderContainerInfos;
    }

    public void addFolderSection(FolderContainerInfo section) {
        mFolderContainerInfos.add(section);
    }

}

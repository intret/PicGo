package cn.intret.app.picgo.model;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 图片文件夹数据模型，包含多个文件夹。
 */
public class FolderModel {


    /**
     * TODO merge with class {@link ImageFolder}
     */
    public static class ParentFolderInfo {
        File mFile;
        String mName;

        List<ImageFolder> mFolders;

        public List<ImageFolder> getFolders() {
            return mFolders;
        }

        public ParentFolderInfo setFolders(List<ImageFolder> folders) {
            mFolders = folders;
            return this;
        }

        public File getFile() {
            return mFile;
        }

        public ParentFolderInfo setFile(File file) {
            mFile = file;
            return this;
        }

        public String getName() {
            return mName;
        }

        public ParentFolderInfo setName(String name) {
            mName = name;
            return this;
        }
    }

    List<ParentFolderInfo> mParentFolderInfos = new LinkedList<>();

    public List<ParentFolderInfo> getParentFolderInfos() {
        return mParentFolderInfos;
    }

    public void addFolderSection(ParentFolderInfo section) {
        mParentFolderInfos.add(section);
    }

}

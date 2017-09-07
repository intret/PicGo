package cn.intret.app.picgo.model;

import org.apache.commons.collections4.ListUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import cn.intret.app.picgo.ui.main.MoveFileDialogFragment;

/**
 * 图片文件夹数据模型，包含多个文件夹。
 */
public class FolderModel implements Cloneable {


    /**
     * TODO merge with class {@link ImageFolder}
     */
    public static class ParentFolderInfo implements Cloneable {
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

        @Override
        public Object clone() throws CloneNotSupportedException {
            ParentFolderInfo clone = (ParentFolderInfo) super.clone();
            clone.setFile(new File(mFile.getAbsolutePath()));
            if (mFolders != null) {
                List<ImageFolder> newFolders = new LinkedList<>();
                for (int i = 0, mFoldersSize = mFolders.size(); i < mFoldersSize; i++) {
                    ImageFolder folder = mFolders.get(i);
                    newFolders.add((ImageFolder) folder.clone());
                }
                clone.setFolders(newFolders);
            }

            return clone;
        }
    }

    List<ParentFolderInfo> mParentFolderInfos = new LinkedList<>();

    public List<ParentFolderInfo> getParentFolderInfos() {
        return mParentFolderInfos;
    }

    public void addFolderSection(ParentFolderInfo section) {
        mParentFolderInfos.add(section);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        FolderModel clone = (FolderModel) super.clone();
        if (mParentFolderInfos != null) {
            List<ParentFolderInfo> parentFolderInfos = new LinkedList<>();
            for (int i = 0, mParentFolderInfosSize = mParentFolderInfos.size(); i < mParentFolderInfosSize; i++) {
                ParentFolderInfo parentFolderInfo = mParentFolderInfos.get(i);
                parentFolderInfos.add((ParentFolderInfo) parentFolderInfo.clone());
            }
            clone.mParentFolderInfos = parentFolderInfos;
        }
        return clone;
    }
}

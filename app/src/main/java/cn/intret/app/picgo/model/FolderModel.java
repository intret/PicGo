package cn.intret.app.picgo.model;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 图片文件夹数据模型，包含多个文件夹。
 */
public class FolderModel implements Cloneable {

    /**
     * 数据模型是否是使用 T9 过滤得到的结果
     */
    boolean mIsT9FilterMode = false;

    public boolean isT9FilterMode() {
        return mIsT9FilterMode;
    }

    public FolderModel setT9FilterMode(boolean t9FilterMode) {
        mIsT9FilterMode = t9FilterMode;
        return this;
    }

    /**
     * TODO merge with class {@link ImageFolder}
     */
    public static class ContainerFolder implements Cloneable {
        File mFile;
        String mName;

        List<ImageFolder> mFolders;

        public List<ImageFolder> getFolders() {
            return mFolders;
        }

        public ContainerFolder setFolders(List<ImageFolder> folders) {
            mFolders = folders;
            return this;
        }

        public File getFile() {
            return mFile;
        }

        public ContainerFolder setFile(File file) {
            mFile = file;
            return this;
        }

        public String getName() {
            return mName;
        }

        public ContainerFolder setName(String name) {
            mName = name;
            return this;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            ContainerFolder clone = (ContainerFolder) super.clone();
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

    List<ContainerFolder> mContainerFolders = new LinkedList<>();

    public List<ContainerFolder> getContainerFolders() {
        return mContainerFolders;
    }

    public void addFolderSection(ContainerFolder section) {
        mContainerFolders.add(section);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        FolderModel clone = (FolderModel) super.clone();
        if (mContainerFolders != null) {
            List<ContainerFolder> containerFolders = new LinkedList<>();
            for (int i = 0, mParentFolderInfosSize = mContainerFolders.size(); i < mParentFolderInfosSize; i++) {
                ContainerFolder containerFolder = mContainerFolders.get(i);
                containerFolders.add((ContainerFolder) containerFolder.clone());
            }
            clone.mContainerFolders = containerFolders;
        }
        return clone;
    }
}

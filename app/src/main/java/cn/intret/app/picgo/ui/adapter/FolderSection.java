package cn.intret.app.picgo.ui.adapter;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.chad.library.adapter.base.entity.AbstractExpandableItem;
import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.io.File;


public class FolderSection extends AbstractExpandableItem<FolderItem> implements MultiItemEntity, Cloneable {
    public String title;
    public File mFile;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        FolderSection sec = (FolderSection) super.clone();
        if (mFile != null) {
            sec.mFile = new File(mFile.getAbsolutePath());
        }

        return sec;
    }

    public FolderSection() {

    }
    public String getTitle() {
        return title;
    }

    public FolderSection setTitle(String title) {
        this.title = title;
        return this;
    }

    public File getFile() {
        return mFile;
    }

    public FolderSection setFile(File file) {
        this.mFile = file;
        return this;
    }

    public FolderSection(String title, File mFile) {
        this.mFile = mFile;
        this.title = title;
    }

    @Override
    public int getItemType() {
        return ExpandableFolderAdapter.TYPE_LEVEL_0;
    }

    @Override
    public int getLevel() {
        return 0;
    }
}

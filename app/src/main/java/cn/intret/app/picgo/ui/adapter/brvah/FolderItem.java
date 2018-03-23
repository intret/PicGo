package cn.intret.app.picgo.ui.adapter.brvah;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import cn.intret.app.picgo.model.image.data.ImageFolder;
import cn.intret.app.picgo.ui.adapter.ThumbnailListAdapter;


public class FolderItem extends ImageFolder implements MultiItemEntity, Cloneable {

    boolean mIsSelected;
    public String title;
    List<File> mConflictFiles;
    boolean mIsSelectionSourceDir = false;
    int mSelectedCount = -1;
    ThumbnailListAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    public Object clone() throws CloneNotSupportedException {
        FolderItem item = (FolderItem) super.clone();
        if (mConflictFiles != null) {
            item.mConflictFiles = new LinkedList<>();

            for (File conflictFile : mConflictFiles) {
                item.mConflictFiles.add(new File(conflictFile.getAbsolutePath()));
            }
        }

        return item;
    }

    public RecyclerView.LayoutManager getLayout(Context context) {
        if (layoutManager == null) {
            layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, true);
        }
        return layoutManager;
    }

    public int getSelectedCount() {
        return mSelectedCount;
    }

    public FolderItem setSelectedCount(int selectedCount) {
        mSelectedCount = selectedCount;
        return this;
    }

    public boolean isSelectionSourceDir() {
        return mIsSelectionSourceDir;
    }

    public FolderItem setSelectionSourceDir(boolean selectionSourceDir) {
        mIsSelectionSourceDir = selectionSourceDir;
        return this;
    }

    public List<File> getConflictFiles() {
        return mConflictFiles;
    }

    public FolderItem setConflictFiles(List<File> conflictFiles) {
        mConflictFiles = conflictFiles;
        return this;
    }

    public FolderItem() {
        super();
    }

    public String getTitle() {
        return title;
    }

    public FolderItem setTitle(String title) {
        this.title = title;
        return this;
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public FolderItem setSelected(boolean selected) {
        mIsSelected = selected;
        return this;
    }

    @Override
    public int getItemType() {
        return ExpandableFolderAdapter.TYPE_LEVEL_1;
    }


}
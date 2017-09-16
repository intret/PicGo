package cn.intret.app.picgo.ui.adapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseViewHolder;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public abstract class BaseImageAdapter<T extends BaseFileItem,VH extends BaseViewHolder>
        extends BaseSelectableAdapter<T,VH> {

    protected File mDirectory;
    protected int mFirstVisibleItem;

    /*
     * Getter an setter
     */

    public File getDirectory() {
        return mDirectory;
    }

    public BaseImageAdapter setDirectory(File directory) {
        mDirectory = directory;
        return this;
    }

    public int getFirstVisibleItem() {
        return mFirstVisibleItem;
    }

    public BaseImageAdapter setFirstVisibleItem(int firstVisibleItem) {
        mFirstVisibleItem = firstVisibleItem;
        return this;
    }

    @Override
    public void onViewRecycled(VH holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public BaseSelectableAdapter setOnInteractionListener(OnInteractionListener<BaseSelectableAdapter, T> onInteractionListener) {
        return super.setOnInteractionListener(onInteractionListener);
    }

    @NonNull
    public List<File> getSelectedFiles() {

        List<File> files = new LinkedList<>();
        for (int i = 0; i < mData.size(); i++) {
            T t = mData.get(i);
            if (t.isSelected()) {
                files.add(t.getFile());
            }
        }
        return files;
    }

    /*
     * Constructor
     */

    public BaseImageAdapter(@LayoutRes int layoutResId, @Nullable List data) {
        super(layoutResId, data);
    }

    public BaseImageAdapter(@Nullable List data) {
        super(data);
    }

    public BaseImageAdapter(@LayoutRes int layoutResId) {
        super(layoutResId);
    }
}

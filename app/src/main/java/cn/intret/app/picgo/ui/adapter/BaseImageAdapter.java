package cn.intret.app.picgo.ui.adapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;

import com.chad.library.adapter.base.BaseViewHolder;

import java.io.File;
import java.util.List;

public abstract class BaseImageAdapter<T extends ItemSelectable,VH extends BaseViewHolder>
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

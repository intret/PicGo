package cn.intret.app.picgo.ui.adapter.brvah;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Stream;
import com.chad.library.adapter.base.BaseViewHolder;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import cn.intret.app.picgo.ui.adapter.BaseFileItem;
import cn.intret.app.picgo.ui.adapter.ItemSelectable;

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

    public void selectAll() {
        for (int i = 0, mDataSize = mData.size(); i < mDataSize; i++) {
            T item = mData.get(i);
            item.setSelected(true);
        }

        if (!mIsSelectionMode) {
            mIsSelectionMode = true;
            if (mOnInteractionListener != null) {
                mOnInteractionListener.onSelectionModeChange(this, true);
            }
        }

        notifyDataSetChanged();

        if (mOnInteractionListener != null) {
            mOnInteractionListener.onSelectedCountChange(this, mData.size());
        }
    }

    public void deselectAll() {
        for (int i = 0, mDataSize = mData.size(); i < mDataSize; i++) {
            T item = mData.get(i);
            item.setSelected(false);
        }

        notifyDataSetChanged();

        if (mOnInteractionListener != null) {
            mOnInteractionListener.onSelectedCountChange(this, 0);
        }
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

    /*
     * Update items
     */

    public void diffUpdate(@NonNull List<T> newData) {

        updateDataSet(newData);

        int oldCount = getSelectedItemCount();
        long newCount = Stream.of(newData).filter(ItemSelectable::isSelected).count();

        if (mIsSelectionMode && newCount == 0) {
            mIsSelectionMode = false;
            if (mOnInteractionListener != null) {
                mOnInteractionListener.onSelectionModeChange(this, mIsSelectionMode);
            }
        }

        if (oldCount != newCount) {
            if (mOnInteractionListener != null) {
                mOnInteractionListener.onSelectedCountChange(this, (int) newCount);
            }
        }
    }
}

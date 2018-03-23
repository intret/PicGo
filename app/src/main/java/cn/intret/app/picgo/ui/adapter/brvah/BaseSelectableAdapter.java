package cn.intret.app.picgo.ui.adapter.brvah;


import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.afollestad.dragselectrecyclerview.DragSelectRecyclerView;
import com.afollestad.dragselectrecyclerview.IDragSelectAdapter;
import com.annimon.stream.Stream;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.LinkedList;
import java.util.List;

import cn.intret.app.picgo.ui.adapter.ItemSelectable;

public abstract class BaseSelectableAdapter< T extends ItemSelectable, VH extends BaseViewHolder>
        extends BaseQuickAdapter<T, VH> implements IDragSelectAdapter, BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemLongClickListener {

    protected boolean mIsSelectionMode = false;

    public BaseSelectableAdapter(@LayoutRes int layoutResId, @Nullable List data) {
        super(layoutResId, data);

        registerClickListener();
    }

    private void registerClickListener() {
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
    }

    public BaseSelectableAdapter(@Nullable List data) {
        super(data);
        registerClickListener();

    }

    public BaseSelectableAdapter(@LayoutRes int layoutResId) {
        super(layoutResId);
        registerClickListener();
    }

    protected abstract void convert(VH vh, T t);

    void selectItem(int position) {
        T t = mData.get(position);
        t.setSelected(true);

        setItemViewSelected(position, t);
    }

    private void setItemViewSelected(int position, T item) {
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView != null) {
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(position);
            if (vh != null && vh instanceof BaseViewHolder) {
                onBindViewHolderSelected((BaseViewHolder) vh, item.isSelected(), mIsSelectionMode);
            }
        }
    }

    @Override
    public void onItemClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
        T item = getItem(i);
        if (item != null) {
            if (mIsSelectionMode) {
                item.setSelected(!item.isSelected());
                if (mOnInteractionListener != null) {
                    mOnInteractionListener.onItemCheckedChanged(item);
                }

                int selectedCount = getSelectedItemCount();
                if (selectedCount == 0) {
                    mIsSelectionMode = false;
                    if (mOnInteractionListener != null) {
                        mOnInteractionListener.onSelectionModeChange(this, false);
                    }
                }
                if (mOnInteractionListener != null) {
                    mOnInteractionListener.onSelectedCountChange(this, selectedCount);
                }
            } else {
                if (mOnInteractionListener != null) {
                    mOnInteractionListener.onItemClicked(item, view, i);
                }
            }

            try {
                setItemViewSelected(i, item);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onItemLongClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
        T item = mData.get(i);
        if (item != null) {

            if (mIsSelectionMode) {

                boolean isEntered = enterDragSelect(i);
                if (!isEntered) {
                    if (mOnInteractionListener != null) {
                        mOnInteractionListener.onSelectedItemLongClick(view, i, item);
                    }
                }

                return true;
            } else {

                // Enter drag-select mode

                // Grid Layout will enter drag-select mode
                boolean isEnteredDragSelect = enterDragSelect(i);

                // Other layout (Linear) will enter normal click-select mode
                if (!isEnteredDragSelect) {
                    setSelected(i, true);
                }

                if (mOnInteractionListener != null) {
                    mOnInteractionListener.onItemLongClick(item, i);
                }
                return true;
            }
        }
        return true;
    }

    private boolean enterDragSelect(int i) {
        boolean enterDragSelect = false;
        RecyclerView rv = getRecyclerView();
        if (rv != null && rv instanceof DragSelectRecyclerView) {
            RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                ((DragSelectRecyclerView) rv).setDragSelectActive(true, i);
                enterDragSelect = true;
            } else if (layoutManager instanceof LinearLayoutManager) {
                ((DragSelectRecyclerView) rv).setDragSelectActive(true, i);
                enterDragSelect = true;
            }
        }
        return enterDragSelect;
    }

    public List<T> getSelectedItems() {
        if (mData == null) {
            return new LinkedList<T>();
        }
        return Stream.of(mData).filter(ItemSelectable::isSelected).toList();
    }

    public List<T> getSelectedItemUntil(final int count) {
        return Stream.of(mData)
                .filter(T::isSelected)
                .limit(count)
                .toList()
                ;
    }

    public void leaveSelectionMode() {

        // Update selected item's status to "unselected"
        for (int i = 0, mItemsSize = mData.size(); i < mItemsSize; i++) {
            T item = mData.get(i);
            if (item.isSelected()) {

                item.setSelected(false);

                if (mOnInteractionListener != null) {
                    mOnInteractionListener.onItemCheckedChanged(item);
                }
            }
        }

        notifyDataSetChanged();


        // Notify leaving selection mode
        mIsSelectionMode = false;
        if (mOnInteractionListener != null) {
            mOnInteractionListener.onSelectionModeChange(this, false);
            mOnInteractionListener.onSelectedCountChange(this, 0);
        }

    }

    public interface OnInteractionListener<AdapterT extends BaseSelectableAdapter, ItemT> {

        void onItemLongClick(ItemT item, int position);

        void onItemCheckedChanged(ItemT item);

        void onItemClicked(ItemT item, View view, int position);

        void onSelectionModeChange(AdapterT adapter, boolean isSelectionMode);

        void onSelectedItemLongClick(View view, int position, ItemT item);

        void onSelectedCountChange(AdapterT adapter, int selectedCount);
    }

    protected OnInteractionListener<BaseSelectableAdapter, T> mOnInteractionListener;

    public OnInteractionListener<BaseSelectableAdapter, T> getOnInteractionListener() {
        return mOnInteractionListener;
    }

    public BaseSelectableAdapter setOnInteractionListener(
            OnInteractionListener<BaseSelectableAdapter, T> onInteractionListener) {
        mOnInteractionListener = onInteractionListener;
        return this;
    }

    abstract void onBindViewHolderSelected(BaseViewHolder vh, boolean selected, boolean isSelectionMode);

    public boolean isSelectionMode() {
        return mIsSelectionMode;
    }

    public BaseSelectableAdapter setSelectionMode(boolean selectionMode) {
        mIsSelectionMode = selectionMode;
        return this;
    }

    @Override
    public void setSelected(int index, boolean selected) {

        T item = mData.get(index);
        item.setSelected(selected);

        int selectedCount = getSelectedItemCount();
        if (selectedCount == 1) {

            mIsSelectionMode = true;
            notifySelectionModeChange(true);

            notifyDataSetChanged();
        } else if (selectedCount == 0) {

            mIsSelectionMode = false;
            notifySelectionModeChange(false);
        }

        if (mOnInteractionListener != null) {
            mOnInteractionListener.onSelectedCountChange(this, selectedCount);
        }

        setItemViewSelected(index, item);
    }

    public int getSelectedItemCount() {
        int selectedCount = 0;
        for (T data : mData) {
            if (data.isSelected()) {
                selectedCount++;
            }
        }
        return selectedCount;
    }

    private void notifySelectionModeChange(boolean isSelectionMode) {
        if (mOnInteractionListener != null) {
            mOnInteractionListener.onSelectionModeChange(this, isSelectionMode);
        }
    }

    @Override
    public boolean isIndexSelectable(int index) {
        // Return false if you don't want this position to be selectable.
        // Useful for items like section headers.
        return true;
    }
}

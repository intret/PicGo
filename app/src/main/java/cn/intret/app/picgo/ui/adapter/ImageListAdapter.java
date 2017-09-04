package cn.intret.app.picgo.ui.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.utils.PathUtils;
import cn.intret.app.picgo.utils.SystemUtils;

/**
 * Waterfall Image List Adapter class for {@link RecyclerView}
 */
public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder> implements View.OnLongClickListener, View.OnClickListener {

    @Override
    public String toString() {
        return "ImageListAdapter{" +
                "mDirectory=" + mDirectory +
                ", mSpanCount=" + mSpanCount +
                ", mIsSelectionMode=" + mIsSelectionMode +
                '}';
    }

    private File mDirectory;

    public void setDirectory(File dir) {
        mDirectory = dir;
    }

    public File getDirectory() {
        return mDirectory;
    }

    @MainThread
    public void diffUpdateWithItems(List<Item> items) {

        Log.d(TAG, "diffUpdateWithItems: 计算差异：old " + mItems.size() + " new " + items.size());

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mItems.size();
            }

            @Override
            public int getNewListSize() {
                return items.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Item oldItem = mItems.get(oldItemPosition);
                Item newItem = items.get(newItemPosition);

                return oldItem.getFile().getAbsolutePath().equalsIgnoreCase(newItem.getFile().getAbsolutePath());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Item oldItem = mItems.get(oldItemPosition);
                Item newItem = items.get(newItemPosition);
                boolean isSameFile = SystemUtils.isSameFile(oldItem.getFile(), newItem.getFile());
                boolean b = (oldItem.isSelected() == newItem.isSelected()) && isSameFile;
                if (!b) {
                    Log.d(TAG, String.format("areContentsTheSame false: old item at %d : %s, new item at %d : %s",
                            oldItemPosition, oldItem.getFile(), newItemPosition, newItem.getFile()));
                }
                return b;
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                Log.d(TAG, "getChangePayload() called with: oldItemPosition = [" + oldItemPosition + "], newItemPosition = [" + newItemPosition + "]");

                Item oldItem = mItems.get(oldItemPosition);
                Item newItem = items.get(newItemPosition);
                newItem.setSelected(oldItem.isSelected());
                newItem.setViewHolder(oldItem.getViewHolder());

                Bundle diffBundle = new Bundle();
                diffBundle.putBoolean("selected", oldItem.isSelected());
                return diffBundle;
            }
        }, true);

        mItems = items;
        diffResult.dispatchUpdatesTo(this);

        if (getSelectedCount() == 0 && mIsSelectionMode && mOnItemInteractionListener != null) {
            mIsSelectionMode = false;
            mOnItemInteractionListener.onSelectionModeChange(mIsSelectionMode);
        }
    }

    public List<Item> getSelectedItemUntil(final int count) {
        return Stream.of(mItems)
                .filter(Item::isSelected)
                .limit(count)
                .toList()
                ;
    }

    public void removeFile(File file) {
        int i = org.apache.commons.collections4.ListUtils.indexOf(mItems, item -> SystemUtils.isSameFile(item.getFile(), file));
        if (i != -1) {
            mItems.remove(i);
            notifyItemRemoved(i);
        }
    }

    public void leaveSelectionMode() {

        // Update selected item's status to "unselected"
        for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
            Item item = mItems.get(i);
            if (item.isSelected()) {

                item.setSelected(false);
                if (mRecyclerView != null) {
                    RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(i);
                    if (viewHolder != null) {
                        ViewHolder vh = (ViewHolder) viewHolder;
                        vh.setChecked(false);
                    }
                }

                if (mOnItemInteractionListener != null) {
                    mOnItemInteractionListener.onItemCheckedChanged(item);
                }
            }
        }

        // Notify leaving selection mode
        mIsSelectionMode = false;
        if (mOnItemInteractionListener != null) {
            mOnItemInteractionListener.onSelectionModeChange(false);
        }
    }

    public interface OnItemInteractionListener {

        void onItemLongClick(Item item);

        void onItemCheckedChanged(Item item);

        void onItemClicked(Item item, View view, int position);

        void onSelectionModeChange(boolean isSelectionMode);

        void onDragBegin(View view, int position, Item item);
    }

    public static final String TAG = ImageListAdapter.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private int mSpanCount = 2;
    boolean mIsSelectionMode = false;
    OnItemInteractionListener mOnItemInteractionListener;

    public ImageListAdapter setOnItemInteractionListener(OnItemInteractionListener onItemInteractionListener) {
        mOnItemInteractionListener = onItemInteractionListener;
        return this;
    }


    public Item getItem(int position) {
        if (position < 0 || position >= mItems.size()) {
            throw new IllegalArgumentException("Invalid argument 'position' value '" + position + "'.");
        }
        return mItems.get(position);
    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag(R.id.item);
        if (tag == null) {
            return;
        }

        Item item = (Item) tag;
    }

    @Override
    public boolean onLongClick(View v) {

        Object tag = v.getTag(R.id.item);
        if (tag != null) {
            Item item = (Item) tag;
        }
        return true;
    }

    public void handleItemLongClickEvent(View view, int position) {

        ImageListAdapter.Item item = getItem(position);

        Log.d(TAG, String.format("handleItemLongClickEvent() called with: view = [%s], position = [%d] item = [%s]", view, position, item));

        if (mIsSelectionMode) {
            Log.d(TAG, "handleItemLongClickEvent: ignore long click on item " + position);

            if (item.isSelected() && mOnItemInteractionListener != null) {
                mOnItemInteractionListener.onDragBegin(view, position, item);
            }
        } else {
            Log.d(TAG, "handleItemLongClickEvent: entered selection mode from item (" + position + ") click");

            // update item data
            item.setSelected(true);

            // Update item ui
            View viewById = view.findViewById(R.id.checkbox);
            if (viewById != null) {
                viewById.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
            } else {
                Log.e(TAG, String.format("handleItemLongClickEvent: 索引 %d 的项 %s 应该标记为选中，但是找不到 ViewHolder ",
                        position, item));
            }

            mIsSelectionMode = true;

            // Notify entering selecting mode
            if (mOnItemInteractionListener != null) {
                mOnItemInteractionListener.onSelectionModeChange(true);
            }
        }
    }

    public void handleItemClickEvent(View view, int position) {
        Log.d(TAG, "handleItemClickEvent() called with: view = [" + view + "], position = [" + position + "]");

        if (mIsSelectionMode) {
            int selectedCount = getSelectedCount();
            Item item = mItems.get(position);

            // The last item has been clicked
            if (item.isSelected() && selectedCount == 1) {

                // Notify leaving selection mode
                if (mOnItemInteractionListener != null) {
                    mIsSelectionMode = false;
                    mOnItemInteractionListener.onSelectionModeChange(false);
                }
                item.setSelected(false);

            } else {
                item.setSelected(!item.isSelected());
            }

            // update item ui
            View viewById = view.findViewById(R.id.checkbox);
            if (viewById != null) {
                viewById.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
            } else {
                Log.d(TAG, "handleItemClickEvent: ");
            }

        } else {

            if (mOnItemInteractionListener != null) {
                Item item = mItems.get(position);
                mOnItemInteractionListener.onItemClicked(item, view, position);
            }
        }
    }

    public static class Item {
        @Override
        public String toString() {
            return "Item{" + this.hashCode() +
                    "mFile=" + mFile +
                    ", mViewHolder=" + mViewHolder +
                    ", mSelected=" + mSelected +
                    '}';
        }

        File mFile;
        ViewHolder mViewHolder;
        boolean mSelected = false;
        private int mHeight = -1;

        public String getTransitionName() {
            return ImageTransitionNameGenerator.generateTransitionName(mFile.getAbsolutePath());
        }

        Item setViewHolder(ViewHolder viewHolder) {
            mViewHolder = viewHolder;
            return this;
        }

        public ViewHolder getViewHolder() {
            return mViewHolder;
        }

        public File getFile() {
            return mFile;
        }

        public Item setFile(File file) {
            mFile = file;
            return this;
        }

        public boolean isSelected() {
            return mSelected;
        }

        public void setSelected(boolean selected) {
            mSelected = selected;
        }

        public int getHeight() {
            return mHeight;
        }

        public void setHeight(int height) {
            mHeight = height;
        }
    }

    private List<Item> mItems = new LinkedList<>();

    public ImageListAdapter(List<Item> items) {
        if (items != null) {
            mItems = items;
        }
    }

    public ImageListAdapter setItems(List<Item> items) {
        if (items != null) {
            mItems = items;
        }
        return this;
    }

    public ImageListAdapter setSpanCount(int spanCount) {
        mSpanCount = spanCount;
        return this;
    }

    public boolean isSelectionMode() {
        return mIsSelectionMode;
    }

    public int getSelectedCount() {
        if (mItems == null || !mIsSelectionMode) {
            return 0;
        }
        return (int) Stream.of(mItems).filter(Item::isSelected).count();
    }

    public List<Item> getSelectedItems() {
        return Stream.of(mItems).filter(Item::isSelected).toList();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mRecyclerView = null;
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        if (holder != null) {
            holder.checkBox.setVisibility(View.GONE);
            holder.fileType.setVisibility(View.GONE);

            if (holder.image != null) {
                holder.image.setTag(R.id.item, -1);
                holder.image.setTransitionName(null);
            }
        }

        super.onViewRecycled(holder);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // todo check parent.getContext() ?
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_list_item, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {

        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            Bundle o = ((Bundle) payloads.get(0));
            boolean selected = o.getBoolean("selected");
            holder.checkBox.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Item item = mItems.get(position);
        item.setViewHolder(holder);

        // Bind data to image view
        holder.image.setTag(R.id.item, item);
        holder.image.setTransitionName(item.getTransitionName());

        // Layout
        if (item.getFile() != null) {

            if (mRecyclerView != null) {
                Context context = mRecyclerView.getContext();

//                holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(context)
                        .asDrawable()
                        //.asBitmap() // bitmap 不会播放 GIF
                        .load(item.getFile())
                        .apply(RequestOptions.centerCropTransform())
                        //.transition(BitmapTransitionOptions.withCrossFade())
                        .into(holder.image);

            } else {
                Log.e(TAG, "onBindViewHolder: mRecyclerView is null.");
            }
        } else {
            // todo load default image
            Log.e(TAG, "onBindViewHolder: No file url for image.");
            if (mRecyclerView != null) {
                holder.image.setBackgroundColor(mRecyclerView.getResources().getColor(R.color.colorPrimaryDark));
            }
        }

        // Checked status
        holder.setChecked(item.isSelected());

        // File type
        // Show file type
        String fileTypeTransitionName = ImageTransitionNameGenerator.generateTransitionName("filetype", item.getFile().getAbsolutePath());

        if (PathUtils.isVideoFile(item.getFile())) {
            holder.fileType.setVisibility(View.VISIBLE);
            holder.fileType.setImageResource(R.drawable.ic_play_circle_outline_black_48px);

            holder.fileType.setTransitionName(fileTypeTransitionName);
        } else if (PathUtils.isGifFile(item.getFile())) {
            holder.fileType.setImageResource(R.drawable.ic_gif_black_48px);
            holder.fileType.setVisibility(View.VISIBLE);

            holder.fileType.setTransitionName(fileTypeTransitionName);
        } else {
            holder.fileType.setVisibility(View.GONE);
        }

        // Image clicking
//        holder.image.setOnLongClickListener(this);
//        holder.image.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.image) ImageView image;
        @BindView(R.id.file_type) ImageView fileType;
        @BindView(R.id.checkbox) ImageView checkBox;

        public ImageView getImage() {
            return image;
        }

        public ImageView getFileType() {
            return fileType;
        }

        ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnItemInteractionListener != null) {

                    }
                    return true;
                }
            });
        }

        public void setChecked(boolean selected) {
            checkBox.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }
}

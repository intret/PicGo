package cn.intret.app.picgo.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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
import com.annimon.stream.function.IndexedPredicate;
import com.annimon.stream.function.Predicate;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

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
                return oldItem.isSelected() == newItem.isSelected();
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                Item oldItem = mItems.get(oldItemPosition);
                Item newItem = items.get(newItemPosition);
                newItem.setSelected(oldItem.isSelected());

                return oldItem.isSelected();
            }
        }, false);

        mItems = items;
        diffResult.dispatchUpdatesTo(this);
    }

    public List<Item> getSelectedItemUntil(final int count) {
        return Stream.of(mItems)
                .filter(Item::isSelected)
                .limit(count)
                .toList()
                ;
    }

    public interface OnItemInteractionListener {

        void onItemLongClick(Item item);

        void onItemCheckedChanged(Item item);
        void onItemClicked(Item item, View view, int position);

        void onSelectionModeChange(boolean isSelectionMode);

        void onDragStared();
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
        handleItemSelectAction(v, item, false);
    }

    @Override
    public boolean onLongClick(View v) {

        Object tag = v.getTag(R.id.item);
        if (tag != null) {
            Item item = (Item) tag;
            return handleItemSelectAction(v, item, true);
        }
        return true;
    }

    public void handleItemLongClickEvent(View view, int position) {
        Log.d(TAG, "handleItemLongClickEvent() called with: view = [" + view + "], position = [" + position + "]");

        ImageListAdapter.Item item = getItem(position);
        if (mIsSelectionMode) {
            Log.d(TAG, "handleItemLongClickEvent: ignore long click on item " + position);

            if (mOnItemInteractionListener != null) {
                mOnItemInteractionListener.onDragStared();
            }
        } else {
            Log.d(TAG, "handleItemLongClickEvent: entered selection mode from item (" + position + ") click");

            // Notify entering selecting mode
            if (mOnItemInteractionListener != null) {
                mOnItemInteractionListener.onSelectionModeChange(true);
            }

            // Update item ui
            ViewHolder holder = item.getViewHolder();
            if (holder != null) {
                holder.setChecked(true);
            }

            // update item data
            item.setSelected(true);

            mIsSelectionMode = true;
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
            ViewHolder viewHolder = item.getViewHolder();
            if (viewHolder != null) {
                viewHolder.setChecked(item.isSelected());
            }

        } else {

            if (mOnItemInteractionListener != null) {
                Item item = mItems.get(position);
                mOnItemInteractionListener.onItemClicked(item, view, position);
            }
        }
    }

    private void onItemClick(int position) {

    }

    private void onItemLongClick(int position) {

    }

    /**
     * A click or long click perform on an item
     *
     *
     * @param view
     * @param item
     * @param isLongClick
     * @return true, if action has been handled.
     */
    private boolean handleItemSelectAction(View view, Item item, boolean isLongClick) {
        int selectedCount = getSelectedCount();

        // Update item selected status
        if (mIsSelectionMode) {

            if (isLongClick) {
                // Entering drap-and-drop mode
                Log.d(TAG, "handleItemSelectAction: Drag started");
                if (mOnItemInteractionListener != null) {
                    mOnItemInteractionListener.onDragStared();
                }
                return true;
            } else {

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
            }

        } else {

            if (isLongClick) {
                mIsSelectionMode = true;

                // Notify entering selection mode
                if (mOnItemInteractionListener != null) {
                    mOnItemInteractionListener.onSelectionModeChange(true);
                }
                item.setSelected(true);
            } else {

                // 单击图片
                if (mOnItemInteractionListener != null) {
                    ViewHolder viewHolder = item.getViewHolder();
                    int position = viewHolder.getAdapterPosition();
                    mOnItemInteractionListener.onItemClicked(item,view, position);
                }
            }
        }


        ViewHolder viewHolder = item.getViewHolder();
        if (viewHolder != null) {
            viewHolder.setChecked(item.isSelected());
        }


        if (mOnItemInteractionListener != null) {
            mOnItemInteractionListener.onItemLongClick(item);
        }

        return true;
    }

    public static class Item {
        @Override
        public String toString() {
            return "Item{" +
                    "mFile=" + mFile +
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

                holder.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(context)
                        .asDrawable()
                        .load(item.getFile())
                        .transition(DrawableTransitionOptions.withCrossFade())
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
        String extension = FilenameUtils.getExtension(item.getFile().getAbsolutePath());
        if (extension != null) {
            switch (extension.toLowerCase()) {
                case "mp4":
                case "avi":
                case "mov":
                case "mpg":
                case "mpeg":
                case "rmvb": {
                    holder.fileType.setVisibility(View.VISIBLE);
                    holder.fileType.setImageResource(R.drawable.ic_videocam);
                }
                break;
                case "gif": {
                    holder.fileType.setImageResource(R.drawable.ic_gif);
                    holder.fileType.setVisibility(View.VISIBLE);
                }
                break;
                default:
                    holder.fileType.setVisibility(View.GONE);
                    break;
            }
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

        @BindView(R.id.img) ImageView image;
        @BindView(R.id.file_type) ImageView fileType;
        @BindView(R.id.checkbox) ImageView checkBox;

        public ImageView getImage() {
            return image;
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

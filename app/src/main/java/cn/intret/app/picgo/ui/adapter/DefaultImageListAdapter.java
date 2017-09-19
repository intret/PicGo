package cn.intret.app.picgo.ui.adapter;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseViewHolder;

import java.io.File;
import java.util.List;

import cn.intret.app.picgo.R;
import cn.intret.app.picgo.utils.PathUtils;

/**
 * Waterfall Image List Adapter class for {@link RecyclerView}
 */
public class DefaultImageListAdapter extends BaseImageAdapter<DefaultImageListAdapter.Item, DefaultImageListAdapter.ViewHolder> {

    public static final String TAG = DefaultImageListAdapter.class.getSimpleName();
    public static final String PAYLOAD_SELECTED = "PAYLOAD_SELECTED";

    /*
     * Internal class
     */

    public static class Item implements BaseFileItem {
        @Override
        public String toString() {
            return "Item{" + this.hashCode() +
                    "mFile=" + mFile +
                    ", mSelected=" + mSelected +
                    '}';
        }

        File mFile;
        boolean mSelected = false;
        private int mHeight = -1;

        @Override
        public File getFile() {
            return mFile;
        }

        @Override
        public void setFile(File file) {
            mFile = file;
        }

        @Override
        public boolean isSelected() {
            return mSelected;
        }

        @Override
        public void setSelected(boolean selected) {
            mSelected = selected;
        }

        public int getHeight() {
            return mHeight;
        }

        public void setHeight(int height) {
            mHeight = height;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Item item = (Item) o;

            return mFile != null ? mFile.equals(item.mFile) : item.mFile == null;

        }

        @Override
        public int hashCode() {
            return mFile != null ? mFile.hashCode() : 0;
        }
    }

    /*
     * View holder
     */

    public class ViewHolder extends BaseViewHolder {

        public ViewHolder(View view) {
            super(view);
        }
    }

    /*
     * Constructor
     */
    public DefaultImageListAdapter(@LayoutRes int layoutResId, @Nullable List data) {
        super(layoutResId, data);
    }

    public DefaultImageListAdapter(@Nullable List data) {
        super(data);
    }

    public DefaultImageListAdapter(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

//    /*
//     * Diff and update
//     */
//    @MainThread
//    public void diffUpdate(List<Item> items) {
//
//        Log.d(TAG, "diffUpdate: 计算差异：old " + mData.size() + " new " + items.size());
//
////        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
////            @Override
////            public int getOldListSize() {
////                return mData.size();
////            }
////
////            @Override
////            public int getNewListSize() {
////                return items.size();
////            }
////
////            @Override
////            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
////                Item oldItem = mData.get(oldItemPosition);
////                Item newItem = items.get(newItemPosition);
////
////                return oldItem.getFile().equals(newItem.getFile());
////            }
////
////            @Override
////            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
////                Item oldItem = mData.get(oldItemPosition);
////                Item newItem = items.get(newItemPosition);
////                boolean isSameFile = SystemUtils.isSameFile(oldItem.getFile(), newItem.getFile());
////                boolean isSameSelected = (oldItem.isSelected() == newItem.isSelected());
////                boolean areSameContent = isSameFile && isSameSelected;
////                if (!areSameContent) {
////
////                    Log.d(TAG, String.format("areContentsTheSame false: old item at %d : %s, new item at %d : %s",
////                            oldItemPosition, oldItem.getFile(), newItemPosition, newItem.getFile()));
////                }
////                return areSameContent;
////            }
////
////            @Nullable
////            @Override
////            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
////                Log.d(TAG, "getChangePayload() called with: oldItemPosition = [" + oldItemPosition + "], newItemPosition = [" + newItemPosition + "]");
////
////                Item oldItem = mData.get(oldItemPosition);
////                Item newItem = items.get(newItemPosition);
////
////                Bundle diffBundle = new Bundle();
////                diffBundle.putBoolean(PAYLOAD_SELECTED, oldItem.isSelected());
////                return diffBundle;
////            }
////        });
//
//
////        mData = items;
////        diffResult.dispatchUpdatesTo(this);
//
//        updateDataSet(items);
//
//        int oldSelectedItemCount = getSelectedItemCount();
//
//        // 新数据集合为空，应该退出选择模式
//        int newSelectedItemCount = (int)com.annimon.stream.Stream.of(items).filter(Item::isSelected).count();
//        if ( newSelectedItemCount == 0 && mIsSelectionMode && mOnInteractionListener != null) {
//            mIsSelectionMode = false;
//            mOnInteractionListener.onSelectionModeChange(this, mIsSelectionMode);
//        }
//
//        // 新数据集合选中 Item 个数变化
//
//        if (oldSelectedItemCount != newSelectedItemCount) {
//            if (mOnInteractionListener != null) {
//                mOnInteractionListener.onSelectedCountChange(this, newSelectedItemCount);
//            }
//        }
//    }

    /*
     * Select all
     */

    public void selectAll() {
        for (int i = 0, mDataSize = mData.size(); i < mDataSize; i++) {
            Item item = mData.get(i);
            item.setSelected(true);
        }

        notifyDataSetChanged();

        if (mOnInteractionListener != null) {
            mOnInteractionListener.onSelectedCountChange(this, mData.size());
        }
    }

    public void unselectAll() {
        for (int i = 0, mDataSize = mData.size(); i < mDataSize; i++) {
            Item item = mData.get(i);
            item.setSelected(false);
        }

        notifyDataSetChanged();

        if (mOnInteractionListener != null) {
            mOnInteractionListener.onSelectedCountChange(this, 0);
        }
    }

    @Override
    void onBindViewHolderSelected(BaseViewHolder vh, boolean selected, boolean isSelectionMode) {
        vh.setVisible(R.id.checkbox, isSelectionMode && selected);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.setChecked(R.id.checkbox, false);
        holder.setVisible(R.id.checkbox, false);
        holder.setVisible(R.id.file_type, false);
        holder.getView(R.id.image).setTransitionName(null);
    }

    @Override
    protected void convert(ViewHolder helper, Item item, List payloads) {
        if (payloads.isEmpty()) {
            super.convert(helper, item, payloads);
        } else {
            Bundle payload = (Bundle) payloads.get(0);

            boolean selected = payload.getBoolean(PAYLOAD_SELECTED, false);
            helper.setVisible(R.id.checkbox, selected);
            Log.d(TAG, "convert: diff update selected " + selected + " : " + item.getFile());
        }
    }

    @Override
    protected void convert(ViewHolder viewHolder, Item item) {

        // Image
        String absolutePath = item.getFile().getAbsolutePath();
        ImageView imageView = viewHolder.getView(R.id.image);
        imageView.setTransitionName(ImageTransitionNameGenerator
                .generateTransitionName(absolutePath));

        Glide.with(viewHolder.itemView.getContext())
                //.asDrawable()
                .asBitmap() // bitmap 不会播放 GIF
                .load(item.getFile())
                .apply(RequestOptions.fitCenterTransform())
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(imageView);

        // File type
        View view = viewHolder.getView(R.id.file_type);
        view.setTransitionName(ImageTransitionNameGenerator.generateTransitionName(ImageTransitionNameGenerator.TRANSITION_PREFIX_FILETYPE, absolutePath));

        boolean videoFile = PathUtils.isVideoFile(absolutePath);
        boolean gifFile = PathUtils.isGifFile(absolutePath);
        boolean staticImageFile = PathUtils.isStaticImageFile(absolutePath);

        if (videoFile || gifFile) {

            if (videoFile) {
                viewHolder.setImageResource(R.id.file_type, R.drawable.ic_play_circle_filled_white_48px);
                viewHolder.setVisible(R.id.file_type, true);
            }
            if (gifFile) {
                viewHolder.setImageResource(R.id.file_type, R.drawable.ic_gif_black_24px);
                viewHolder.setVisible(R.id.file_type, true);
            }
        } else {
            viewHolder.setVisible(R.id.file_type, false);
        }

        // Item Checked
        viewHolder.setVisible(R.id.checkbox, item.isSelected());
    }
}

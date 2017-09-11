package cn.intret.app.picgo.ui.adapter;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseViewHolder;

import java.io.File;
import java.util.List;

import cn.intret.app.picgo.R;
import cn.intret.app.picgo.utils.PathUtils;
import cn.intret.app.picgo.utils.SystemUtils;

/**
 * Waterfall Image List Adapter class for {@link RecyclerView}
 */
public class ImageListAdapter extends BaseImageAdapter<ImageListAdapter.Item,ImageListAdapter.ViewHolder> {

    public static final String TAG = ImageListAdapter.class.getSimpleName();

    private int mSpanCount = 2;

    public ImageListAdapter(@LayoutRes int layoutResId, @Nullable List data) {
        super(layoutResId, data);
    }

    public ImageListAdapter(@Nullable List data) {
        super(data);
    }

    public ImageListAdapter(@LayoutRes int layoutResId) {
        super(layoutResId);
    }


    @MainThread
    public void diffUpdate(List<Item> items) {

        Log.d(TAG, "diffUpdate: 计算差异：old " + mData.size() + " new " + items.size());

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mData.size();
            }

            @Override
            public int getNewListSize() {
                return items.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Item oldItem = mData.get(oldItemPosition);
                Item newItem = items.get(newItemPosition);

                return oldItem.getFile().getAbsolutePath().equalsIgnoreCase(newItem.getFile().getAbsolutePath());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Item oldItem = mData.get(oldItemPosition);
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

                Item oldItem = mData.get(oldItemPosition);
                Item newItem = items.get(newItemPosition);
                newItem.setSelected(oldItem.isSelected());

                Bundle diffBundle = new Bundle();
                diffBundle.putBoolean("selected", oldItem.isSelected());
                return diffBundle;
            }
        }, true);

        mData = items;
        diffResult.dispatchUpdatesTo(this);

        // TODO x
        if (getSelectedCount() == 0 && mIsSelectionMode && mOnInteractionListener != null) {
            mIsSelectionMode = false;
            mOnInteractionListener.onSelectionModeChange(this, mIsSelectionMode);
        }
    }

    /*
     * Internal class
     */

    public static class Item implements ItemSelectable {
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

    public ImageListAdapter setSpanCount(int spanCount) {
        mSpanCount = spanCount;
        return this;
    }

    @Override
    void onBindViewHolderSelected(BaseViewHolder vh, Item item) {
        vh.setVisible(R.id.checkbox, item.isSelected());
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.setChecked(R.id.checkbox, false);
        holder.setVisible(R.id.checkbox, false);
        holder.setVisible(R.id.file_type, false);
        holder.getView(R.id.image).setTransitionName(null);
    }

    @Override
    protected void convert(ViewHolder viewHolder, Item item) {

        // Image
        String absolutePath = item.getFile().getAbsolutePath();
        ImageView imageView = viewHolder.getView(R.id.image);
        imageView.setTransitionName(ImageTransitionNameGenerator
                        .generateTransitionName(absolutePath));

        Glide.with(viewHolder.itemView.getContext())
                .asDrawable()
                //.asBitmap() // bitmap 不会播放 GIF
                .load(item.getFile())
                .apply(RequestOptions.fitCenterTransform())
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);

        // Image type
        View view = viewHolder.getView(R.id.file_type);
        view.setTransitionName(ImageTransitionNameGenerator.generateTransitionName("filetype", absolutePath));
        boolean videoFile = PathUtils.isVideoFile(absolutePath);
        boolean gifFile = PathUtils.isGifFile(absolutePath);
        boolean staticImageFile = PathUtils.isStaticImageFile(absolutePath);

        if (videoFile || gifFile) {

            if (videoFile) {
                viewHolder.setImageResource(R.id.file_type, R.drawable.ic_videocam);
                viewHolder.setVisible(R.id.file_type, true);
            }
            if (gifFile) {
                viewHolder.setImageResource(R.id.file_type, R.drawable.ic_gif);
                viewHolder.setVisible(R.id.file_type, true);
            }
        } else {
            viewHolder.setVisible(R.id.file_type, false);
        }

        // Item Checked
        viewHolder.setVisible(R.id.checkbox, item.isSelected());
    }

    /*
     * View holder
     */

    public class ViewHolder extends BaseViewHolder {

        public ViewHolder(View view) {
            super(view);
        }
    }
}

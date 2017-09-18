package cn.intret.app.picgo.ui.adapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.siyamed.shapeimageview.RoundedImageView;

import org.apache.commons.io.FileUtils;
import org.joda.time.Duration;

import java.io.File;
import java.util.Date;
import java.util.List;

import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.MediaFile;
import cn.intret.app.picgo.utils.DateTimeUtils;
import cn.intret.app.picgo.utils.PathUtils;


public class DetailImageAdapter
        extends BaseImageAdapter<DetailImageAdapter.Item, DetailImageAdapter.ItemViewHolder> {

    public static class Item implements BaseFileItem {
        File mFile;
        Date mDate;

        /**
         * Image size or video resolution
         */
        Size mMediaResolution;

        /**
         * File size ( disk usage of file), in bytes
         */
        long mFileSize;


        long mVideoDuration;

        /**
         * Get duration, in ms
         * @return
         */
        public long getVideoDuration() {
            return mVideoDuration;
        }

        public void setVideoDuration(long videoDuration) {
            mVideoDuration = videoDuration;
        }

        public long getFileSize() {
            return mFileSize;
        }

        public void setFileSize(long fileSize) {
            mFileSize = fileSize;
        }

        public Size getMediaResolution() {
            return mMediaResolution;
        }

        public void setMediaResolution(Size mediaResolution) {
            mMediaResolution = mediaResolution;
        }

        public File getFile() {
            return mFile;
        }

        public void setFile(File file) {
            mFile = file;
        }

        public Date getDate() {
            return mDate;
        }

        public void setDate(Date date) {
            mDate = date;
        }


        @Override
        public int hashCode() {
            return mFile != null ? mFile.hashCode() : 0;
        }
        public Item() {
        }

        boolean mSelected;

        @Override
        public boolean isSelected() {
            return mSelected;
        }

        @Override
        public void setSelected(boolean selected) {
            mSelected = selected;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Item)) return false;

            Item item = (Item) o;

            return mFile != null ? mFile.equals(item.mFile) : item.mFile == null;

        }
    }

    public class ItemViewHolder extends BaseViewHolder {

        public ItemViewHolder(View view) {
            super(view);
        }
    }

    /*
     * Constructor
     */

    public DetailImageAdapter(@LayoutRes int layoutResId, @Nullable List<Item> data) {
        super(layoutResId, data);
    }

    public DetailImageAdapter(@Nullable List<Item> data) {
        super(data);
    }

    public DetailImageAdapter(@LayoutRes int layoutResId) {
        super(layoutResId);
    }

    /*
     * Bind item
     */

    @Override
    protected void convert(ItemViewHolder viewHolder, Item item) {

        // File name
        viewHolder.setText(R.id.filename, item.getFile().getName());

        // Image/Video Resolution
        if (item.getMediaResolution() != null) {

            viewHolder.setText(R.id.resolution,
                    viewHolder.itemView
                            .getContext()
                            .getResources()
                            .getString(R.string.image_size_d_d,
                                    item.getMediaResolution().getWidth(),
                                    item.getMediaResolution().getHeight()))

            ;
        }
        String absolutePath = item.getFile().getAbsolutePath();
        boolean videoFile = PathUtils.isVideoFile(absolutePath);
        boolean gifFile = PathUtils.isGifFile(absolutePath);
        boolean staticImageFile = PathUtils.isStaticImageFile(absolutePath);

        // Desc
        if (videoFile) {
            viewHolder.setText(R.id.desc, DateTimeUtils.formatDuration(new Duration(item.getVideoDuration())));
        } else {
            viewHolder.setText(R.id.desc, FileUtils.byteCountToDisplaySize(item.getFileSize()));
        }

        // File type
        if (videoFile || gifFile) {

            if (videoFile) {
                viewHolder.setImageResource(R.id.file_type, R.drawable.ic_play_circle_filled_white_48px);
                viewHolder.setVisible(R.id.file_type, true);
            }
            if (gifFile) {
                viewHolder.setImageResource(R.id.file_type, R.drawable.ic_gif);
                viewHolder.setVisible(R.id.file_type, true);
            }
        } else {
            viewHolder.setVisible(R.id.file_type, false);
        }

        // Transition name
        RoundedImageView imageView = viewHolder.getView(R.id.image);
        imageView.setTransitionName(ImageTransitionNameGenerator.generateTransitionName(item.getFile().getAbsolutePath()));

        // Check
        onBindViewHolderSelected(viewHolder, item);

        // Image
        Glide.with(viewHolder.itemView.getContext())
                .asBitmap()
                .load(absolutePath)
                .apply(RequestOptions.centerCropTransform())
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(imageView);
    }

    @Override
    void onBindViewHolderSelected(BaseViewHolder vh, Item item) {
        vh.setVisible(R.id.checkBox, mIsSelectionMode);
        vh.setChecked(R.id.checkBox, item.isSelected());
    }

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
}

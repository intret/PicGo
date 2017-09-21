package cn.intret.app.picgo.ui.adapter.brvah;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.siyamed.shapeimageview.RoundedImageView;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.joda.time.Duration;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import cn.intret.app.picgo.R;
import cn.intret.app.picgo.ui.adapter.BaseFileItem;
import cn.intret.app.picgo.ui.adapter.ContentEqual;
import cn.intret.app.picgo.ui.adapter.ImageTransitionNameGenerator;
import cn.intret.app.picgo.utils.DateTimeUtils;
import cn.intret.app.picgo.utils.PathUtils;


public class DetailImageAdapter
        extends BaseImageAdapter<DetailImageAdapter.Item, DetailImageAdapter.ItemViewHolder> {

    public static class Item implements BaseFileItem, ContentEqual {
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

        boolean mSelected;

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

        /**
         * https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/builder/EqualsBuilder.html
         * @param obj
         * @return
         */
        @Override
        public boolean contentEquals(Object obj) {
            if (obj == null) { return false; }
            if (obj == this) { return true; }
            if (obj.getClass() != getClass()) {
                return false;
            }
            Item rhs = (Item) obj;
            return new EqualsBuilder()
                    .appendSuper(super.equals(obj))

                    .append(mDate, rhs.mDate)
                    .append(mFile, rhs.mFile)
                    .append(mFileSize, rhs.mFileSize)
                    .append(mMediaResolution, rhs.mMediaResolution)
                    .append(mSelected, rhs.mSelected)

                    .append(mVideoDuration, rhs.mVideoDuration)
                    .isEquals();
        }

        public static final String FIELD_MEDIA_RESOLUTION_WIDTH = "FIELD_MEDIA_RESOLUTION_WIDTH";
        public static final String FIELD_MEDIA_RESOLUTION_HEIGHT = "FIELD_MEDIA_RESOLUTION_HEIGHT";
        public static final String FIELD_FILE = "FIELD_FILE";
        public static final String FIELD_DATE = "FIELD_DATE";
        public static final String FIELD_FILE_SIZE = "FIELD_FILE_SIZE";
        public static final String FIELD_SELECTED = "FIELD_SELECTED";
        public static final String FIELD_VIDEO_DURATION = "FIELD_VIDEO_DURATION";


        public void fillPayloadIntent(Bundle bundle, Object obj) {
            if (bundle == null || obj == null || obj == this) {
                throw new IllegalArgumentException("'intent' or 'obj' is null, or 'obj == this'." );
            }
            if (obj.getClass() != getClass()) {
                throw new ClassCastException("'obj' is not the right class : " + getClass());
            }

            Item rhs = (Item) obj;

            if (!Objects.equals(mMediaResolution, rhs.mMediaResolution)) {
                bundle.putInt(FIELD_MEDIA_RESOLUTION_WIDTH, mMediaResolution.getWidth());
                bundle.putInt(FIELD_MEDIA_RESOLUTION_HEIGHT, mMediaResolution.getHeight());
            }

            if (!Objects.equals(mFile, rhs.mFile)) {
                bundle.putSerializable(FIELD_FILE, rhs.mFile);
            }

            if (!Objects.equals(mDate, rhs.mDate)) {
                bundle.putSerializable(FIELD_DATE, rhs.mDate);
            }

            if ( mFileSize != rhs.mFileSize ) {
                bundle.putLong(FIELD_FILE_SIZE, rhs.mFileSize);
            }

            if (mSelected != rhs.mSelected) {
                bundle.putBoolean(FIELD_SELECTED, rhs.mSelected);
            }

            if (mVideoDuration != rhs.mVideoDuration) {
                bundle.putLong(FIELD_VIDEO_DURATION, rhs.mVideoDuration);
            }
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
        init();
    }

    public DetailImageAdapter(@Nullable List<Item> data) {
        super(data);
        init();
    }

    public DetailImageAdapter(@LayoutRes int layoutResId) {
        super(layoutResId);
        init();
    }

    private void init() {

         setDiffUtilCallback(new BaseQuickAdapter.DiffUtilCallback<DetailImageAdapter.Item>() {

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                // 使用 Item 的 equals 方法比较
                return super.areItemsTheSame(oldItemPosition, newItemPosition);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                DetailImageAdapter.Item oldItem = getOldItem(oldItemPosition);
                DetailImageAdapter.Item newItem = getNewItem(newItemPosition);

                return oldItem.contentEquals(newItem);
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                DetailImageAdapter.Item oldItem = getOldItem(oldItemPosition);
                DetailImageAdapter.Item newItem = getNewItem(newItemPosition);

                Bundle bundle = new Bundle();
                oldItem.fillPayloadIntent(bundle, newItem);
                return bundle;
            }
        });
    }

    /*
     * Bind item
     */

    @Override
    protected void convert(ItemViewHolder helper, Item item, List payloads) {
        try {

            if (payloads.isEmpty()) {
                super.convert(helper, item, payloads);
            } else {
                Bundle payload = (Bundle) payloads.get(0);
                if (payload.containsKey(Item.FIELD_DATE)) {
                    Log.w(TAG, "convert: do nothing FIELD_DATE");
                }

                if (payload.containsKey(Item.FIELD_FILE)) {
                    Log.w(TAG, "convert: do nothing FIELD_FILE");
                }

                if (payload.containsKey(Item.FIELD_FILE_SIZE)) {
                    Log.w(TAG, "convert: do nothing FIELD_FILE_SIZE");
                }

                if (payload.containsKey(Item.FIELD_MEDIA_RESOLUTION_HEIGHT)
                        && payload.containsKey(Item.FIELD_MEDIA_RESOLUTION_WIDTH)) {
                    int width = payload.getInt(Item.FIELD_MEDIA_RESOLUTION_WIDTH);
                    int height = payload.getInt(Item.FIELD_MEDIA_RESOLUTION_HEIGHT);

                    bindResolution(helper, new Size(width, height));
                }

                if (payload.containsKey(Item.FIELD_SELECTED)) {
                    boolean selected = payload.getBoolean(Item.FIELD_SELECTED);
                    onBindViewHolderSelected(helper, selected, mIsSelectionMode);
                }

                if (payload.containsKey(Item.FIELD_VIDEO_DURATION)) {
                    int duration = payload.getInt(Item.FIELD_VIDEO_DURATION);
                    bindDuration(helper, duration);
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    protected void convert(ItemViewHolder viewHolder, Item item) {

        // File name
        viewHolder.setText(R.id.filename, item.getFile().getName());

        // Image/Video Resolution
        Size mediaResolution = item.getMediaResolution();
        bindResolution(viewHolder, mediaResolution);

        String absolutePath = item.getFile().getAbsolutePath();
        boolean videoFile = PathUtils.isVideoFile(absolutePath);
        boolean gifFile = PathUtils.isGifFile(absolutePath);
        boolean staticImageFile = PathUtils.isStaticImageFile(absolutePath);

        // Desc
        if (videoFile) {
            bindDuration(viewHolder, item.getVideoDuration());
        } else {
            bindFileSize(viewHolder, item.getFileSize());
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
        onBindViewHolderSelected(viewHolder, item.isSelected(), mIsSelectionMode);

        // Image
        Glide.with(viewHolder.itemView.getContext())
                .asBitmap()
                .load(absolutePath)
                .apply(RequestOptions.centerCropTransform())
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(imageView);
    }

    private void bindResolution(ItemViewHolder viewHolder, Size mediaResolution) {
        if (mediaResolution != null) {

            viewHolder.setText(R.id.resolution,
                    viewHolder.itemView
                            .getContext()
                            .getResources()
                            .getString(R.string.image_size_d_d,
                                    mediaResolution.getWidth(),
                                    mediaResolution.getHeight()))

            ;
        }
    }

    private void bindFileSize(ItemViewHolder viewHolder, long fileSize) {
        viewHolder.setText(R.id.desc, FileUtils.byteCountToDisplaySize(fileSize));
    }

    private void bindDuration(ItemViewHolder viewHolder, long videoDuration) {
        viewHolder.setText(R.id.desc, DateTimeUtils.formatDuration(new Duration(videoDuration)));
    }

    void onBindViewHolderSelected(BaseViewHolder vh, boolean selected, boolean isSelectionMode) {
        vh.setVisible(R.id.checkBox, isSelectionMode);
        vh.setChecked(R.id.checkBox, selected);
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

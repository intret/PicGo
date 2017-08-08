package cn.intret.app.picgo.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.siyamed.shapeimageview.RoundedImageView;

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

    public interface OnItemInteractionListener {
        void onItemLongClick(Item item);

        void onItemCheckedChanged(Item item);

        void onItemClicked(Item item);
        void onSelectionModeChange(boolean isSelectionMode);

        void onDragStared();
    }

    public static final String TAG = "WaterfallListAdapter";
    private Context mContext;
    private RecyclerView mRecyclerView;
    private int mSpanCount = 2;
    private int mGutterWidth = 6; // in dps

    boolean mIsSelectionMode = false;

    OnItemInteractionListener mOnItemInteractionListener;

    public ImageListAdapter setOnItemInteractionListener(OnItemInteractionListener onItemInteractionListener) {
        mOnItemInteractionListener = onItemInteractionListener;
        return this;
    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag(R.id.item);
        if (tag == null) {
            return;
        }

        Item item = (Item) tag;
        handleItemSelectAction(item, false);
    }

    @Override
    public boolean onLongClick(View v) {

        Object tag = v.getTag(R.id.item);
        if (tag != null) {
            Item item = (Item) tag;
            return handleItemSelectAction(item, true);
        }
        return true;
    }

    /**
     * A click or long click perform on an item
     *
     * @param item
     * @param isLongClick
     * @return true, if action has been handled.
     */
    private boolean handleItemSelectAction(Item item, boolean isLongClick) {
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
                    mOnItemInteractionListener.onItemClicked(item);
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
        Drawable mDrawable;
        File mFile;
        ViewHolder mViewHolder;
        boolean mSelected = false;
        private int mHeight = -1;

        Item setViewHolder(ViewHolder viewHolder) {
            mViewHolder = viewHolder;
            return this;
        }

        public ViewHolder getViewHolder() {
            return mViewHolder;
        }

        public Drawable getDrawable() {
            return mDrawable;
        }

        public Item setDrawable(Drawable drawable) {
            mDrawable = drawable;
            return this;
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


    public int getSelectedCount() {
        if (mItems == null) {
            return 0;
        }
        return (int) Stream.of(mItems).filter(Item::isSelected).count();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mContext = recyclerView.getContext();
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mContext = null;
        mRecyclerView = null;
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        if (holder != null) {
            holder.radio.setVisibility(View.GONE);
            holder.fileType.setVisibility(View.GONE);

            if (holder.image != null) {
                holder.image.setTag(R.id.item, -1);
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

        // Layout
        if (item.getFile() != null) {

            if (mRecyclerView != null) {
                Context context = mRecyclerView.getContext();

                float vhMargin = mContext.getResources().getDimension(R.dimen.margin_list_item_image_view);
                int parentWidth = mRecyclerView.getWidth();
                int width = (int) (parentWidth - (vhMargin * 2 * (mSpanCount + 1)) / mSpanCount);

                // Setting image size
                ViewGroup.LayoutParams layoutParams = holder.image.getLayoutParams();
                if (layoutParams != null) {
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    holder.image.setLayoutParams(layoutParams);
                }
//                    if (item.getHeight() == -1) {
//                        initialLoadImage(item, holder, width, position);
//                    } else {
                Glide.with(context)
                        .asBitmap()
                        .load(item.getFile())
                        .apply(RequestOptions.fitCenterTransform())
                        .transition(BitmapTransitionOptions.withCrossFade())
                        .into(holder.image);
//                    }

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
        holder.image.setOnLongClickListener(this);
        holder.image.setOnClickListener(this);
    }

    private void initialLoadImage(Item item, ViewHolder holder, int imageWidth, int position) {
        //构造方法中参数view,就是回调方法中的this.view
        ViewTarget<View, Bitmap> target = new ViewTarget<View, Bitmap>(holder.image) {
            @Override
            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                //加载图片成功后调用
                float scaleType = ((float) resource.getHeight()) / resource.getWidth();
                int imageHeight = (int) (imageWidth * scaleType);
                //获取图片高度，保存在Map中

                item.setHeight(imageHeight);

                //设置图片布局的长宽，Glide会根据布局的自动加载适应大小的图片
                ViewGroup.LayoutParams lp = this.view.getLayoutParams();
                lp.width = imageWidth;
                lp.height = imageHeight;
                this.view.setLayoutParams(lp);
                //resource就是加载成功后的图片资源
                ((ImageView) view).setImageBitmap(resource);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                super.onLoadFailed(errorDrawable);
                int imageHeight = imageWidth;
                item.setHeight(imageHeight);
                ViewGroup.LayoutParams lp = this.view.getLayoutParams();
                lp.width = imageWidth;
                lp.height = imageHeight;
                this.view.setLayoutParams(lp);
                ((ImageView) view).setImageResource(R.mipmap.ic_launcher);
            }
        };

        ViewTarget<RoundedImageView, Drawable> viewTarget = new ViewTarget<RoundedImageView, Drawable>(holder.image) {
            @Override
            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                float radio = (float) resource.getIntrinsicHeight() / (float) resource.getIntrinsicWidth();
                int imageHeight = (int) (imageWidth * radio);

                item.setHeight(imageHeight);

                Log.d(TAG, String.format("onResourceReady: w %d h %d %d %d",
                        resource.getIntrinsicWidth(), resource.getIntrinsicHeight(),
                        imageWidth, imageHeight)
                );
                //设置图片布局的长宽，Glide会根据布局的自动加载适应大小的图片
                ViewGroup.LayoutParams lp = this.view.getLayoutParams();
                lp.width = imageWidth;
                lp.height = imageHeight;
                this.view.setLayoutParams(lp);
                //resource就是加载成功后的图片资源
                view.setImageDrawable(resource);
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                super.onLoadFailed(errorDrawable);

                int imageHeight = imageWidth;
                item.setHeight(imageHeight);
                ViewGroup.LayoutParams lp = this.view.getLayoutParams();
                lp.width = imageWidth;
                lp.height = imageHeight;
                this.view.setLayoutParams(lp);
                view.setBackgroundColor(mRecyclerView.getResources().getColor(R.color.colorPrimaryDark));
            }
        };

        Glide.with(this.mContext)
                .load(item.getFile())
                .into(viewTarget);
    }


    @Override
    public int getItemCount() {
        return mItems.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.img) RoundedImageView image;
        @BindView(R.id.radio) AppCompatCheckBox radio;
        @BindView(R.id.file_type) ImageView fileType;
        @BindView(R.id.checkbox) ImageView checkBox;

        ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        public void setChecked(boolean selected) {
            checkBox.setVisibility(selected ? View.VISIBLE : View.GONE);
        }
    }
}

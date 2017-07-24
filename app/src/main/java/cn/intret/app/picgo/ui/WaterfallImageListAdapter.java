package cn.intret.app.picgo.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRadioButton;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.siyamed.shapeimageview.RoundedImageView;

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
class WaterfallImageListAdapter extends RecyclerView.Adapter<WaterfallImageListAdapter.ViewHolder> implements View.OnLongClickListener, View.OnClickListener {

    public interface OnItemEventListener {
        void onItemLongClick(Item item);

        void onItemCheckedChanged(Item item);

        void onSelectionModeChange(boolean isSelectionMode);

        void onDragStared();
    }

    public static final String TAG = "WaterfallListAdapter";
    private Context mContext;
    private RecyclerView mRecyclerView;
    private int mSpanCount = 2;
    private int mGutterWidth = 6; // in dps

    boolean mIsSelectionMode = false;

    OnItemEventListener mOnItemEventListener;

    public WaterfallImageListAdapter setOnItemEventListener(OnItemEventListener onItemEventListener) {
        mOnItemEventListener = onItemEventListener;
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
                Log.d(TAG, "handleItemSelectAction: Drap started");
                if (mOnItemEventListener != null) {
                    mOnItemEventListener.onDragStared();
                }
                return true;
            } else {

                if (item.isSelected() && selectedCount == 1) {

                    // Notify leaving selection mode
                    if (mOnItemEventListener != null) {
                        mIsSelectionMode = false;
                        mOnItemEventListener.onSelectionModeChange(false);
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
                if (mOnItemEventListener != null) {
                    mOnItemEventListener.onSelectionModeChange(true);
                }
                item.setSelected(true);
            }
        }


        ViewHolder viewHolder = item.getViewHolder();
        if (viewHolder != null) {

            viewHolder.radio.setChecked(true); // Always checked
            viewHolder.radio.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
        }


        if (mOnItemEventListener != null) {
            mOnItemEventListener.onItemLongClick(item);
        }

        return true;
    }

    public static class Item {
        Drawable mDrawable;
        File mFile;
        ViewHolder mViewHolder;
        boolean mSelected = false;

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
    }

    List<Item> mItems = new LinkedList<>();

    WaterfallImageListAdapter(List<Item> items) {
        if (items != null) {
            mItems = items;
        }
    }

    public WaterfallImageListAdapter setItems(List<Item> items) {
        if (items != null) {
            mItems = items;
        }
        return this;
    }

    public WaterfallImageListAdapter setSpanCount(int spanCount) {
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
            if (holder.image != null) {
                holder.radio.setVisibility(View.GONE);
                holder.image.setTag(R.id.item, -1);
            }
        }
        super.onViewRecycled(holder);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // todo check parent.getContext() ?
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.waterfall_image_list_item, null);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Item item = mItems.get(position);
        item.setViewHolder(holder);

        // Bind data to image view
        holder.image.setTag(R.id.item, item);

        // Layout

        if (item.getDrawable() != null) {
            holder.image.setImageDrawable(item.getDrawable());
        } else {

            if (item.getFile() != null) {

                if (mRecyclerView != null) {

                    Context context = mRecyclerView.getContext();
                    float vhMargin = mContext.getResources().getDimension(R.dimen.margin_list_item_image_view);
                    int parentWidth = mRecyclerView.getWidth();

                    // Setting image size
                    ViewGroup.LayoutParams layoutParams = holder.image.getLayoutParams();

                    //layoutParams.height = (int) (parentWidth - (vhMargin * 2 * (mSpanCount + 1)) / mSpanCount);
                    //holder.image.setLayoutParams(layoutParams);

                    Glide.with(context)
                            .load(item.getFile())
                            .apply(RequestOptions.centerCropTransform())
                            .transition(withCrossFade())
                            .into(holder.image);
                }
            } else {
                // todo load default image
                if (mRecyclerView != null) {
                    holder.image.setBackgroundColor(mRecyclerView.getResources().getColor(R.color.colorPrimaryDark));
                }
            }
        }

        // Checked status
        holder.radio.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
        holder.radio.setChecked(true);

        // Action
        holder.image.setOnLongClickListener(this);
        holder.image.setOnClickListener(this);
    }


    @Override
    public int getItemCount() {
        return mItems.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.img) RoundedImageView image;
        @BindView(R.id.radio) AppCompatCheckBox radio;

        ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

    }
}

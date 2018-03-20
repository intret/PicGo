package cn.intret.app.picgo.screens.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.siyamed.shapeimageview.RoundedImageView;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;

/**
 * 水平图片列表
 */
public class HorizontalImageListAdapter extends RecyclerView.Adapter<HorizontalImageListAdapter.ViewHolder> {

    List<Item> mItems = new LinkedList<Item>();

    interface OnItemClickListener {
        void onItemClick(View v, Item item, int position);

        void onItemLongClick(View v, Item item, int position);
    }

    OnItemClickListener mClickListener;

    public HorizontalImageListAdapter setOnClickListener(OnItemClickListener listener) {
        mClickListener = listener;
        return this;
    }

    public static class Item {
        File mFile;

        public File getFile() {
            return mFile;
        }

        public Item setFile(File file) {
            mFile = file;
            return this;
        }
    }

    public HorizontalImageListAdapter(List<Item> items) {
        super();
        if (items != null) {
            mItems = items;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.horizontal_image_list_item, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Item item = mItems.get(mItems.size() - 1 - position);

//        holder.itemView.setOnClickListener(v -> {
//            if (mClickListener != null) {
//                mClickListener.onItemClick(v, item, position);
//            }
//        });

        holder.itemView.setOnLongClickListener(v -> {
            if (mClickListener != null) {
                mClickListener.onItemLongClick(v, item, position);
            }
            return true;
        });

        if (item.getFile() != null) {
            Glide.with(holder.itemView.getContext())
                    .asBitmap()
                    .load(item.getFile())
                    .apply(RequestOptions.centerCropTransform())
                    .into(holder.image);
        } else {
            // TODO: set placeholder
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.image) RoundedImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
//            itemView.setOnTouchListener((v, event) -> {
//                // http://stackoverflow.com/questions/8121491/is-it-possible-to-add-a-scrollable-textview-to-a-listview
//                v.getParent().requestDisallowInterceptTouchEvent(false); // needed for complex gestures
//                // simple tap works without the above line as well
//                return image.dispatchTouchEvent(event); // onTouchEvent won't work
//            });
        }
    }
}

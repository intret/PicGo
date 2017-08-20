package cn.intret.app.picgo.ui.adapter;


import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.annimon.stream.Stream;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;

public class FlatFolderListAdapter extends RecyclerView.Adapter<FlatFolderListAdapter.ViewHolder> {

    List<Item> mItems = new LinkedList<>();

    public List<Item> getSelectedItem() {
        return Stream.of(mItems).filter(Item::isSelected).toList();
    }

    public static class Item {
        boolean mIsSelected = true;
        File mDirectory;
        int mCount;
        HorizontalImageListAdapter mAdapter;

        public boolean isSelected() {
            return mIsSelected;
        }

        public Item setSelected(boolean selected) {
            mIsSelected = selected;
            return this;
        }

        public HorizontalImageListAdapter getAdapter() {
            return mAdapter;
        }

        public Item setAdapter(HorizontalImageListAdapter adapter) {
            mAdapter = adapter;
            return this;
        }

        public int getCount() {
            return mCount;
        }

        public Item setCount(int count) {
            mCount = count;
            return this;
        }

        List<File> mThumbList;

        public List<File> getThumbList() {
            return mThumbList;
        }

        public Item setThumbList(List<File> thumbList) {
            mThumbList = thumbList;
            return this;
        }

        List<ThumbnailImage> mThumbnailImages = null;

        public File getDirectory() {
            return mDirectory;
        }

        public Item setDirectory(File directory) {
            mDirectory = directory;
            return this;
        }

        public List<ThumbnailImage> getThumbnailImages() {
            return mThumbnailImages;
        }

        public Item setThumbnailImages(List<ThumbnailImage> thumbnailImages) {
            mThumbnailImages = thumbnailImages;
            return this;
        }
    }

    public static class ThumbnailImage {
        File mFile;

        public File getFile() {
            return mFile;
        }

        public ThumbnailImage setFile(File file) {
            mFile = file;
            return this;
        }
    }

    public FlatFolderListAdapter(List<Item> items) {
        if (items != null) {
            mItems = items;
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.flat_folder_list_item, parent, false);
        return new ViewHolder(v);
    }

    private List<HorizontalImageListAdapter.Item> filesToItems(List<File> thumbList) {
        if (thumbList == null) {
            return null;
        }
        return Stream.of(thumbList).map(file -> new HorizontalImageListAdapter.Item().setFile(file)).toList();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Item item = mItems.get(position);

        holder.name.setText(item.getDirectory().getName());
        holder.count.setText(String.valueOf(item.getCount()));

        holder.checkBox.setChecked(item.isSelected());
        holder.checkBox.setTag(R.id.item, item);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Object tag = buttonView.getTag(R.id.item);
            if (tag instanceof Item) {
                ((Item) tag).setSelected(isChecked);
            }
        });

        // 缩略图列表
        if (item.getAdapter() == null) {
            HorizontalImageListAdapter adapter = new HorizontalImageListAdapter(filesToItems(item.getThumbList()));

            item.setAdapter(adapter);
            holder.thumbList.setClickable(false);

            holder.thumbList.setLayoutManager(holder.getLayout());
            holder.thumbList.setAdapter(item.getAdapter());
        } else {
            holder.thumbList.setClickable(false);
            holder.thumbList.setLayoutManager(holder.getLayout());
            holder.thumbList.swapAdapter(item.getAdapter(), false);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.name) TextView name;
        @BindView(R.id.count) TextView count;
        @BindView(R.id.thumb_list) RecyclerView thumbList;
        @BindView(R.id.check_box) AppCompatCheckBox checkBox;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        private LinearLayoutManager mLayoutManager;

        public RecyclerView.LayoutManager getLayout() {
            if (mLayoutManager == null) {
                mLayoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, true);
            }
            return mLayoutManager;
        }
    }

}

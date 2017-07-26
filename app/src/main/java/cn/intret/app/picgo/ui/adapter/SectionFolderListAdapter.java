package cn.intret.app.picgo.ui.adapter;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.truizlop.sectionedrecyclerview.SectionedRecyclerViewAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.intret.app.picgo.R;

/**
 * TODO add add/remove method
 */
public class SectionFolderListAdapter
        extends SectionedRecyclerViewAdapter<
        SectionFolderListAdapter.FolderHeaderViewHolder,
        SectionFolderListAdapter.ViewHolder,
        SectionFolderListAdapter.FolderFooterViewHolder
        > {

    List<SectionItem> mSectionItems = new ArrayList<>();

    public static class SectionItem {
        String name;
        File mFile;
        List<Item> mItems;

        public String getName() {
            return name;
        }

        public SectionItem setName(String name) {
            this.name = name;
            return this;
        }

        public File getFile() {
            return mFile;
        }

        public SectionItem setFile(File file) {
            mFile = file;
            return this;
        }

        public List<Item> getItems() {
            return mItems;
        }

        public SectionItem setItems(List<Item> items) {
            mItems = items;
            return this;
        }
    }


    public static class Item {
        String mName;
        int mCount;
        File mFile;
        List<File> mThumbList;
        private HorizontalImageListAdapter mAdapter;

        public List<File> getThumbList() {
            return mThumbList;
        }

        public Item setThumbList(List<File> thumbList) {
            mThumbList = thumbList;
            return this;
        }

        public String getName() {
            return mName;
        }

        public Item setName(String name) {
            mName = name;
            return this;
        }

        public int getCount() {
            return mCount;
        }

        public Item setCount(int count) {
            mCount = count;
            return this;
        }

        public File getFile() {
            return mFile;
        }

        public Item setFile(File file) {
            mFile = file;
            return this;
        }

        public void setAdapter(HorizontalImageListAdapter adapter) {
            mAdapter = adapter;
        }

        public HorizontalImageListAdapter getAdapter() {
            return mAdapter;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(SectionItem sectionItem, Item item);
    }
    OnItemClickListener mOnItemClickListener;

    public SectionFolderListAdapter setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
        return this;
    }

    public SectionFolderListAdapter(List<SectionItem> items) {
        if (items != null) {
            mSectionItems = items;
        }
    }

    @Override
    protected int getSectionCount() {
        return mSectionItems.size();
    }

    @Override
    protected int getItemCountForSection(int section) {
        int size = mSectionItems.get(section).getItems().size();
        return size;
    }

    @Override
    protected boolean hasFooterInSection(int section) {
        return false;
    }

    @Override
    protected FolderHeaderViewHolder onCreateSectionHeaderViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folder_list_section_header, parent, false);
        return new FolderHeaderViewHolder(v);
    }

    @Override
    protected FolderFooterViewHolder onCreateSectionFooterViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folder_list_section_header, parent, false);
        return new FolderFooterViewHolder(v);
    }

    @Override
    protected ViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folder_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    protected void onBindSectionHeaderViewHolder(FolderHeaderViewHolder holder, int section) {
        SectionItem sectionItem = mSectionItems.get(section);
        holder.name.setText(sectionItem.getName());
    }

    @Override
    protected void onBindSectionFooterViewHolder(FolderFooterViewHolder holder, int section) {

    }

    private void setViewItemTag(View view, Object tag) {
        view.setTag(R.id.item, tag);
    }

    private Item getViewItemTag(View view) {
        Object tag = view.getTag(R.id.item);
        if (tag != null) {
            return ((SectionFolderListAdapter.Item) tag);
        }
        return null;
    }

    @Override
    protected void onBindItemViewHolder(ViewHolder holder, int section, int position) {
        SectionItem sectionItem = mSectionItems.get(section);
        Item item = sectionItem.getItems().get(position);

        setViewItemTag(holder.itemView, item);

        holder.itemView.setOnClickListener(v -> {
            Item i = getViewItemTag(v);
            if (i != null) {
                if (mOnItemClickListener != null) {
                    // todo change the first parameter to section-info class
                    mOnItemClickListener.onItemClick(mSectionItems.get(section), i);
                }
            }
        });

        holder.thumbList.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(),
                LinearLayoutManager.HORIZONTAL, true));
//        holder.thumbList.setOnTouchListener((v, event) -> {
//            // http://stackoverflow.com/questions/8121491/is-it-possible-to-add-a-scrollable-textview-to-a-listview
//            v.getParent().requestDisallowInterceptTouchEvent(true); // needed for complex gestures
//            // simple tap works without the above line as well
//            return holder.itemView.dispatchTouchEvent(event); // onTouchEvent won't work
//        });

        if (item.getAdapter() == null) {
            HorizontalImageListAdapter adapter = new HorizontalImageListAdapter(filesToItems(item.getThumbList()));
            adapter.setOnClickListener();
            item.setAdapter(adapter);
            holder.thumbList.setAdapter(item.getAdapter());
        } else {
            holder.thumbList.swapAdapter(item.getAdapter(), false);
        }

        holder.name.setText(item.getName());
        holder.count.setText(String.valueOf(item.getCount()));
    }

    private List<HorizontalImageListAdapter.Item> filesToItems(List<File> thumbList) {
        if (thumbList == null) {
            return null;
        }
        return Stream.of(thumbList).map(file -> new HorizontalImageListAdapter.Item().setFile(file)).toList();
    }

    class FolderHeaderViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.name)
        TextView name;

        FolderHeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.name)
        TextView name;

        @BindView(R.id.count)
        TextView count;

        @BindView(R.id.thumb_list)
        RecyclerView thumbList;

        ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

    }

    class FolderFooterViewHolder extends RecyclerView.ViewHolder {

        public FolderFooterViewHolder(View itemView) {
            super(itemView);
        }


    }
}

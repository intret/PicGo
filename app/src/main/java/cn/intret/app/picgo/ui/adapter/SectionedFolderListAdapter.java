package cn.intret.app.picgo.ui.adapter;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;

/**
 * 分段文件夹列表
 */
public class SectionedFolderListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    List<SectionItem> mSectionItems = new LinkedList<>();

    public Item getItem(ItemCoord relativePosition) {
        int section = relativePosition.section();
        if (section >= 0 && section< mSectionItems.size()) {
            SectionItem sectionItem = mSectionItems.get(section);
            List<Item> items = sectionItem.getItems();

            int i = relativePosition.relativePos();
            if (i >= 0 && i < items.size()) {
                return items.get(i);
            }
        }
        return null;
    }

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

        public List<File> getThumbList() {
            return mThumbList;
        }

        public Item setThumbList(List<File> thumbList) {
            mThumbList = thumbList;
            return this;
        }

        HorizontalImageListAdapter mAdapter;

        public HorizontalImageListAdapter getAdapter() {
            return mAdapter;
        }

        public Item setAdapter(HorizontalImageListAdapter adapter) {
            mAdapter = adapter;
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
    }

    interface OnItemClickListener {
        void onItemClick(SectionItem sectionItem, int section, Item item, int relativePos);
    }

    OnItemClickListener mOnItemClickListener;

    SectionedFolderListAdapter() {

    }

    public SectionedFolderListAdapter(List<SectionItem> sectionItems) {
        if (sectionItems != null) {
            mSectionItems = sectionItems;
            notifyDataSetChanged();
        }
    }

    SectionedFolderListAdapter setSectionItems(List<SectionItem> sectionItems) {
        if (sectionItems != null) {
            this.mSectionItems = sectionItems;
        }
        return this;
    }

    @Override
    public int getSectionCount() {
        return mSectionItems.size();
    }

    @Override
    public int getItemCount(int section) {
        SectionItem items = mSectionItems.get(section);
        return items.getItems().size();
    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder holder, int section, boolean expanded) {
        HeaderViewHolder vh = (HeaderViewHolder) holder;
        SectionItem sectionItem = mSectionItems.get(section);

        vh.name.setText(sectionItem.getName());
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {

    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition) {
        ItemViewHolder vh = (ItemViewHolder) holder;
        SectionItem sectionItem = mSectionItems.get(section);
        Item item = sectionItem.getItems().get(relativePosition);

        vh.name.setText(item.getName());
        vh.count.setText(String.valueOf(item.getCount()));
        if (item.getAdapter() == null) {
            HorizontalImageListAdapter adapter = new HorizontalImageListAdapter(filesToItems(item.getThumbList()));
//            adapter.setOnClickListener();
            item.setAdapter(adapter);
            vh.thumbList.setClickable(false);
            vh.thumbList.setLayoutManager(new LinearLayoutManager(vh.itemView.getContext(), LinearLayoutManager.HORIZONTAL, true));
            vh.thumbList.setAdapter(item.getAdapter());
        } else {
            vh.thumbList.setClickable(false);
            vh.thumbList.swapAdapter(item.getAdapter(), false);
        }
    }

    private List<HorizontalImageListAdapter.Item> filesToItems(List<File> thumbList) {
        if (thumbList == null) {
            return null;
        }
        return com.annimon.stream.Stream.of(thumbList).map(file -> new HorizontalImageListAdapter.Item().setFile(file)).toList();
    }

    @Override
    public SectionedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Change inflated layout based on type
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.folder_list_section_header, parent, false);
                return new HeaderViewHolder(v);
            }
            case VIEW_TYPE_FOOTER:
                // if footers are enabled
                return null;
            default: {

                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.folder_list_item, parent, false);
                return new ItemViewHolder(v);
            }
        }
    }

    class HeaderViewHolder extends SectionedViewHolder implements View.OnClickListener {

        @BindView(R.id.name)
        TextView name;

        HeaderViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

        }
    }

    class ItemViewHolder extends SectionedViewHolder implements View.OnClickListener {

        private static final String TAG = "SectionViewHolder";

        @BindView(R.id.name)
        TextView name;

        @BindView(R.id.count)
        TextView count;

        @BindView(R.id.thumb_list)
        RecyclerView thumbList;

        ItemViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            // Setup view holder. You'd want some views to be optional, e.g. the
            // header/footer will have views that normal item views do or do not have.
            //itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // SectionedViewHolder exposes methods such as:
            boolean isHeader = isHeader();
            boolean isFooter = isFooter();
            ItemCoord position = getRelativePosition();
            int section = position.section();
            int relativePos = position.relativePos();

            Log.d(TAG, "onClick: section " + section + " relativePos " + relativePos);

            SectionItem sectionItem = mSectionItems.get(section);
            Item item = sectionItem.getItems().get(relativePos);

            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(sectionItem, section, item, relativePos);
            }
        }
    }
}

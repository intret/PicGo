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
 * 分段标签列表
 */
public class SectionedLabelListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    List<Section> mSections = new LinkedList<>();

    public Item getItem(ItemCoord relativePosition) {
        int section = relativePosition.section();
        if (section >= 0 && section < mSections.size()) {
            Section sectionItem = mSections.get(section);
            List<Item> items = sectionItem.getItems();

            int i = relativePosition.relativePos();
            if (i >= 0 && i < items.size()) {
                return items.get(i);
            }
        }
        return null;
    }

    public static class Section {
        String name;
        File mFile;
        List<Item> mItems;

        public String getName() {
            return name;
        }

        public Section setName(String name) {
            this.name = name;
            return this;
        }

        public File getFile() {
            return mFile;
        }

        public Section setFile(File file) {
            mFile = file;
            return this;
        }

        public List<Item> getItems() {
            return mItems;
        }

        public Section setItems(List<Item> items) {
            mItems = items;
            return this;
        }
    }


    public static class Item {
        String mName;
        File mFile;

        public String getName() {
            return mName;
        }

        public Item setName(String name) {
            mName = name;
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
        void onItemClick(Section sectionItem, int section, Item item, int relativePos);
    }

    OnItemClickListener mOnItemClickListener;

    SectionedLabelListAdapter() {

    }

    public SectionedLabelListAdapter(List<Section> sections) {
        if (sections != null) {
            mSections = sections;
            notifyDataSetChanged();
        }
    }

    SectionedLabelListAdapter setSections(List<Section> sections) {
        if (sections != null) {
            this.mSections = sections;
        }
        return this;
    }

    @Override
    public int getSectionCount() {
        return mSections.size();
    }

    @Override
    public int getItemCount(int section) {
        Section items = mSections.get(section);
        return items.getItems().size();
    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder holder, int section, boolean expanded) {
        HeaderViewHolder vh = (HeaderViewHolder) holder;
        Section sectionItem = mSections.get(section);

        vh.name.setText(sectionItem.getName());
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {

    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition) {
        ItemViewHolder vh = (ItemViewHolder) holder;
        Section sectionItem = mSections.get(section);
        Item item = sectionItem.getItems().get(relativePosition);

        vh.name.setText(item.getName());
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

        private static final String TAG = "ItemViewHolder";

        @BindView(R.id.name) TextView name;
        @BindView(R.id.count) TextView count;
        @BindView(R.id.thumb_list) RecyclerView thumbList;
        private LinearLayoutManager mLinearLayoutManager;

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

            Section sectionItem = mSections.get(section);
            Item item = sectionItem.getItems().get(relativePos);

            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(sectionItem, section, item, relativePos);
            }
        }

        public RecyclerView.LayoutManager getLayout() {
            if (mLinearLayoutManager == null) {
                mLinearLayoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, true);
            }
            return mLinearLayoutManager;
        }
    }
}

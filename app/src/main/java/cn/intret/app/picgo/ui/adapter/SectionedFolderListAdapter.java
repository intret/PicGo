package cn.intret.app.picgo.ui.adapter;

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

import butterknife.BindInt;
import butterknife.BindView;
import butterknife.Optional;
import cn.intret.app.picgo.R;

/**
 * 分段文件夹列表
 */

public class SectionedFolderListAdapter extends SectionedRecyclerViewAdapter<SectionedFolderListAdapter.ViewHolder> {

    List<SectionItem> mSectionItems = new LinkedList<>();

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

    SectionedFolderListAdapter(List<SectionItem> sectionItems) {
        if (sectionItems != null) {
            mSectionItems = sectionItems;
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
    public void onBindHeaderViewHolder(ViewHolder holder, int section, boolean expanded) {

    }

    @Override
    public void onBindFooterViewHolder(ViewHolder holder, int section) {

    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int section, int relativePosition, int absolutePosition) {

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Change inflated layout based on type
        int layoutRes;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                layoutRes = R.layout.folder_list_section_header;
                break;
            case VIEW_TYPE_FOOTER:
                // if footers are enabled
                layoutRes = R.layout.folder_list_section_header;
                break;
            default:
                layoutRes = R.layout.folder_list_item;
                break;
        }
        View v = LayoutInflater.from(parent.getContext())
                .inflate(layoutRes, parent, false);
        return new ViewHolder(v);
    }

    class ViewHolder extends SectionedViewHolder implements View.OnClickListener {

        private static final String TAG = "SectionViewHolder";

        @BindView(R.id.name)
        TextView mName;

        public ViewHolder(View itemView) {
            super(itemView);
            // Setup view holder. You'd want some views to be optional, e.g. the
            // header/footer will have views that normal item views do or do not have.
            itemView.setOnClickListener(this);
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

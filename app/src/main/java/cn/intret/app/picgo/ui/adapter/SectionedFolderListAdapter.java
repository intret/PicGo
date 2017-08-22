package cn.intret.app.picgo.ui.adapter;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.utils.SystemUtils;

/**
 * 分段文件夹列表
 */
public class SectionedFolderListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    private static final String TAG = SectionedFolderListAdapter.class.getSimpleName();
    private static final String PAYLOAD_KEY_NAME = "name";
    private static final String PAYLOAD_KEY_FILE = "file";

    private boolean mEnableItemClick = true;
    /*
     * UI options
     */
    boolean mShowHeaderOptionButton = false;
    boolean mIsSelectable = false;
    boolean mIsCollapsable = true;
    boolean mIsMultiSelect = false;

    /*
     * Data
     */
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

    public void renameDirectory(File oldDirectory, File newDirectory) {
        if (oldDirectory == null || newDirectory == null) {
            return;
        }

        int sectionIndex = -1;
        int itemIndex = -1;
        for (int sec = 0, mSectionsSize = mSections.size(); sec < mSectionsSize; sec++) {
            Section section = mSections.get(sec);
            int ii = ListUtils.indexOf(section.getItems(), object -> SystemUtils.isSameFile(object.getFile(), oldDirectory));
            if (ii != -1) {
                sectionIndex = sec;
                itemIndex = ii;
                break;
            }
        }
        if (sectionIndex != -1) {
            mSections.get(sectionIndex).getItems().get(itemIndex).setFile(newDirectory).setName(newDirectory.getName());
            notifyItemChanged(getAbsolutePosition(sectionIndex, itemIndex));
        }
    }

    enum ItemType {
        HEADER,
        FOOTER,
        ITEM
    }

    private ItemType getItemType(SectionedFolderListAdapter adapter, int position) {
        boolean header = adapter.isHeader(position);
        boolean footer = adapter.isFooter(position);
        if (header) {
            return ItemType.HEADER;
        } else if (footer) {
            return ItemType.FOOTER;
        } else {
            return ItemType.ITEM;
        }
    }

    public void diffUpdateItems(SectionedFolderListAdapter adapter) {
        List<Section> sections = adapter.getSections();
        int oldItemCount = getItemCount();

        int newItemCount = adapter.getItemCount();
        Log.d(TAG, "diffUpdateItems: 计算差异 old " + oldItemCount + " new " + newItemCount);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {

            @Override
            public int getOldListSize() {
                return oldItemCount;
            }

            @Override
            public int getNewListSize() {
                return newItemCount;
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                ItemCoord oldCoord = getRelativePosition(oldItemPosition);
                ItemCoord newCoord = adapter.getRelativePosition(newItemPosition);

                ItemType oldItemType = getItemType(SectionedFolderListAdapter.this, oldItemPosition);
                ItemType newItemType = getItemType(adapter, newItemPosition);

                if (oldItemType == newItemType) {
                    // 类型一样 Section 索引
                    if (oldItemType == ItemType.HEADER) {
                        return oldCoord.section() == newCoord.section();
                    }

                    if (oldItemType == ItemType.FOOTER) {
                        return oldCoord.section() == newCoord.section();
                    }

                    if (oldItemType == ItemType.ITEM) {
                        return oldCoord.section() == newCoord.section() && oldCoord.relativePos() == newCoord.relativePos();
                    }
                }
                return false;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ItemCoord oldCoord = getRelativePosition(oldItemPosition);
                ItemCoord newCoord = adapter.getRelativePosition(newItemPosition);

                ItemType oldItemType = getItemType(SectionedFolderListAdapter.this, oldItemPosition);
                ItemType newItemType = getItemType(adapter, newItemPosition);

                if (oldItemType == newItemType) {
                    // 类型一样比较值
                    switch (oldItemType) {

                        case HEADER:
                        case FOOTER: {
                            Section oldSec = mSections.get(oldCoord.section());
                            Section newSec = adapter.getSections().get(newCoord.section());

                            File oldFile = oldSec.getFile();
                            File newFile = newSec.getFile();

                            return SystemUtils.isSameFile(oldFile, newFile) && StringUtils.equals(oldSec.getName(), newSec.getName());
                        }
                        case ITEM: {
                            Item oldItem = SectionedFolderListAdapter.this.getItem(oldCoord);
                            Item newItem = adapter.getItem(newCoord);


                            if (StringUtils.equals(oldItem.getName(), newItem.getName()) &&
                                    SystemUtils.isSameFile(oldItem.getFile(), newItem.getFile())) {
                                // 目录一样，比较缩略图列表
                                List<File> oldThumbList = oldItem.getThumbList();
                                List<File> newThumbList = newItem.getThumbList();

                                return ListUtils.isEqualList(oldThumbList, newThumbList);
                            } else {

                                // 名称和目录有一项不一样就重新绑定 ViewHolder
                                return false;
                            }
                        }
                    }
                }

                return false;
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {

                Log.d(TAG, "getChangePayload() called with: oldItemPosition = [" + oldItemPosition + "], newItemPosition = [" + newItemPosition + "]");

                ItemCoord oldCoord = getRelativePosition(oldItemPosition);
                ItemCoord newCoord = adapter.getRelativePosition(newItemPosition);

                ItemType oldItemType = getItemType(SectionedFolderListAdapter.this, oldItemPosition);

                switch (oldItemType) {

                    case FOOTER:
                    case HEADER: {
                        Section oldSec = mSections.get(oldCoord.section());
                        Section newSec = adapter.getSections().get(newCoord.section());

                        Bundle res = new Bundle();
                        if (!StringUtils.equals(oldSec.getName(), newSec.getName())) {
                            res.putString(PAYLOAD_KEY_NAME, newSec.getName());
                        }

                        // todo 处理 name 一样但是 file 不一样的情况

                        return res;
                    }
                    case ITEM: {
                        Item oldItem = SectionedFolderListAdapter.this.getItem(oldCoord);
                        Item newItem = adapter.getItem(newCoord);

                        // 哪一项不一样就只存哪一项
                        Bundle res = new Bundle();
                        boolean isSameName = StringUtils.equals(oldItem.getName(), newItem.getName());

                        // File 不一样会导致缩略图不一样
                        boolean isSameFile = SystemUtils.isSameFile(oldItem.getFile(), newItem.getFile());
                        if (isSameName && isSameFile) {
                            // 重新绑定 ViewHolder
                            // TODO 更细致的局部更新，因为缩略图也是使用 RecyclerView 所以可以使用 DiffUtil
                            return super.getChangePayload(oldItemPosition, newItemPosition);

                        } else {
                            if (!isSameFile) {
                                res.putString(PAYLOAD_KEY_FILE, newItem.getFile().getAbsolutePath());
                            }
                            if (!isSameName) {
                                res.putString(PAYLOAD_KEY_NAME, newItem.getName());
                            }
                        }
                        return res;
                    }
                }
                return super.getChangePayload(oldItemPosition, newItemPosition);
            }
        });

        mSections = adapter.getSections();
        diffResult.dispatchUpdatesTo(this);
    }

    public void updateThumbList(String dir) {
        if (dir == null) {
            return;
        }
        for (Section section : mSections) {
            List<Item> items = section.getItems();
            for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
                Item item = items.get(i);

            }
        }
    }

    /*
     * Interfaces and Classes
     */
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

    public interface OnItemClickListener {
        void onSectionHeaderClick(Section section, int sectionIndex, int adapterPosition);

        void onSectionHeaderOptionButtonClick(View v, Section section, int sectionIndex);

        void onItemClick(Section sectionItem, int section, Item item, int relativePos);

        void onItemLongClick(Section sectionItem, int section, Item item, int relativePos);
    }

    /*
     * Event handler
     */
    OnItemClickListener mOnItemClickListener;

     /*
     * Getter and setter
     */

    public List<Section> getSections() {
        return mSections;
    }

    public boolean isShowHeaderOptionButton() {
        return mShowHeaderOptionButton;
    }

    public SectionedFolderListAdapter setShowHeaderOptionButton(boolean showHeaderOptionButton) {
        mShowHeaderOptionButton = showHeaderOptionButton;
        return this;
    }

    public boolean isSelectable() {
        return mIsSelectable;
    }

    public SectionedFolderListAdapter setSelectable(boolean selectable) {
        mIsSelectable = selectable;
        return this;
    }

    public boolean isCollapsable() {
        return mIsCollapsable;
    }

    public SectionedFolderListAdapter setCollapsable(boolean collapsable) {
        mIsCollapsable = collapsable;
        return this;
    }

    public boolean isMultiSelect() {
        return mIsMultiSelect;
    }

    public SectionedFolderListAdapter setMultiSelect(boolean multiSelect) {
        mIsMultiSelect = multiSelect;
        return this;
    }

    public OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public SectionedFolderListAdapter setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
        return this;
    }

    /*
     * Ctor an Dtor
     */

    SectionedFolderListAdapter() {

    }

    public SectionedFolderListAdapter(List<Section> sections) {
        if (sections != null) {
            mSections = sections;
            notifyDataSetChanged();
        }
    }

    SectionedFolderListAdapter setSections(List<Section> sections) {
        if (sections != null) {
            this.mSections = sections;
        }
        return this;
    }

    /*
     * RecyclerView
     */

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

        View.OnClickListener clickListener = v -> {
            Object tag = v.getTag(R.id.item);
            if (tag != null && tag instanceof Section) {

                int adapterPosition = vh.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    ItemCoord coord = getRelativePosition(adapterPosition);
                    Section section1 = mSections.get(coord.section());

                    clickSectionHeader(section1, coord.section(), adapterPosition);
                }
            }
        };
        vh.itemView.setTag(R.id.item, sectionItem);
        vh.itemView.setOnClickListener(clickListener);


        vh.name.setText(sectionItem.getName());
        vh.name.setTag(R.id.item, sectionItem);
        vh.name.setClickable(false);


        if (mShowHeaderOptionButton) {
            if (mEnableItemClick) {

                // 选项文字按钮
                vh.option.setTag(R.id.item, sectionItem);
                vh.option.setOnClickListener(v -> {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        ItemCoord coord = getRelativePosition(adapterPosition);
                        Section sec = mSections.get(coord.section());
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onSectionHeaderOptionButtonClick(v, sec, coord.section());
                        }
                    }
                });
            }
        } else {
            vh.option.setVisibility(View.GONE);
        }
    }

    private void clickSectionHeader(Section section, int sectionIndex, int adapterPosition) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onSectionHeaderClick(section, sectionIndex, adapterPosition);
        }
    }

    public <R> R extractTagValue(View view, int id) {
        Object tag = view.getTag(id);
        if (tag != null) {
            return ((R) tag);
        }
        return null;
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {

    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition) {
        ItemViewHolder vh = (ItemViewHolder) holder;
        Section sectionItem = mSections.get(section);
        Item item = sectionItem.getItems().get(relativePosition);

        /*
         * Item click
         */
        if (mEnableItemClick) {

            // Save data to view tag
            vh.itemView.setTag(R.id.section, sectionItem);
            vh.itemView.setTag(R.id.item, item);

            // Click
            vh.itemView.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                ItemCoord coor = getRelativePosition(adapterPosition);

                if (mOnItemClickListener != null) {
                    Section sectionItem1 = mSections.get(coor.section());
                    mOnItemClickListener.onItemClick(
                            sectionItem1, coor.section(),
                            sectionItem1.getItems().get(coor.relativePos()),
                            coor.relativePos());
                }
            });

            // Long click
            vh.itemView.setOnLongClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                ItemCoord coor = getRelativePosition(adapterPosition);

                if (mOnItemClickListener != null) {
                    Section sectionItem1 = mSections.get(coor.section());
                    mOnItemClickListener.onItemLongClick(
                            sectionItem1, coor.section(),
                            sectionItem1.getItems().get(coor.relativePos()),
                            coor.relativePos());
                    return true;
                } else {
                    return false;
                }
            });

            vh.thumbList.setClickable(false);
        }

        vh.name.setText(item.getName());
        vh.count.setText(String.valueOf(item.getCount()));
        if (item.getAdapter() == null) {
            HorizontalImageListAdapter adapter = new HorizontalImageListAdapter(filesToItems(item.getThumbList()));

            item.setAdapter(adapter);
            vh.thumbList.setClickable(false);

            vh.thumbList.setLayoutManager(vh.getLayout());
            vh.thumbList.setAdapter(item.getAdapter());
        } else {
            vh.thumbList.setClickable(false);
            vh.thumbList.setLayoutManager(vh.getLayout());
            vh.thumbList.swapAdapter(item.getAdapter(), false);
        }
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition, List<Object> payload) {
        if (payload.isEmpty()) {
            super.onBindViewHolder(holder, section, relativePosition, absolutePosition, payload);
        } else {
            Object o = payload.get(0);
            if (o instanceof Bundle) {
                ItemViewHolder vh = (ItemViewHolder) holder;

                Bundle bundle = (Bundle) o;
                String name = bundle.getString(PAYLOAD_KEY_NAME);
                if (name != null) {

                    vh.name.setText(name);
                }

                String filePath = bundle.getString(PAYLOAD_KEY_FILE);
                if (filePath != null) {
                    Log.d(TAG, "onBindViewHolder: do nothing for partial update with file path : " + filePath);
                }
            }
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

        @BindView(R.id.name) TextView name;
        @BindView(R.id.option) TextView option;

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

package cn.intret.app.picgo.ui.adapter;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.annimon.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.utils.DataConsumer2;
import cn.intret.app.picgo.utils.PathUtils;
import cn.intret.app.picgo.utils.SystemUtils;
import q.rorbin.badgeview.Badge;
import q.rorbin.badgeview.QBadgeView;

/**
 * 分段文件夹列表
 */
public class SectionedFolderListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    private static final String TAG = SectionedFolderListAdapter.class.getSimpleName();

    private static final String PAYLOAD_KEY_SECTION_NAME = "name";
    private static final String PAYLOAD_KEY_FILE = "file";
    private static final String PAYLOAD_KEY_ITEM_NAME = "item_name";
    private static final String PAYLOAD_KEY_THUMB_LIST = "item_thum_list";
    private static final String PAYLOAD_KEY_CONFLICT_FILES = "item_conflict_files";
    private static final String PAYLOAD_KEY_CONFLICT_FILES_COUNT = "item_conflict_files_count";
    private static final String PAYLOAD_KEY_SELECTION = "item_selection";
    /*
     * UI options
     */
    private boolean mShowHeaderOptionButton = false;

    private boolean mShowInFilterMode = false;
    private boolean mEnableItemClick = true;
    private boolean mIsSelectable = false;
    private boolean mIsCollapsable = true;
    private boolean mIsMultiSelect = false;

    /*
     * Data
     */
    private List<Section> mSections = new LinkedList<>();
    private RecyclerView mRecyclerView;
    private Map<File, List<File>> mConflictFiles;
    private boolean mShowConflictBadge;
    private File mMoveFileSourceDir;
    private boolean mShowSourceDirBadgeWhenEmpty = true;

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

    public boolean selectItem(File dir) {
        if (dir == null) {
            return false;
        }

        // Find single selected item and mark it as 'unselected'
        for (int si = 0, mSectionsSize = mSections.size(); si < mSectionsSize; si++) {

            Section currSec = mSections.get(si);
            List<Item> items = currSec.getItems();

            // find current selected item index
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                Item currItem = items.get(ii);

                // Update 'selected' item to 'unselected'
                if (currItem.isSelected()) {
                    if (!currItem.getFile().equals(dir)) {
                        currItem.setSelected(false);
                        // mark as 'unselected'
                    } else {
                        currItem.setSelected(true);
                    }

                    if (mRecyclerView != null) {
                        int absPos = getAbsolutePosition(si, ii);
                        updateItemViewHolderCheckStatus(absPos, currItem.isSelected());
                    }
                } else {
                    if (mRecyclerView != null) {
                        int absPos = getAbsolutePosition(si, ii);
                        if (currItem.getFile().equals(dir)) {
                            currItem.setSelected(true);
                            updateItemViewHolderCheckStatus(absPos, currItem.isSelected());
                        }
                    }
                }
            }
        }
        return false;
    }

    private enum ItemType {
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

    public void setSourceDirectory(File file) {
        mMoveFileSourceDir = file;
    }

    public void updateConflictFiles(@NonNull Map<File, List<File>> folderConflictFiles) {

        mShowConflictBadge = true;
        mConflictFiles = folderConflictFiles;

        Log.d(TAG, "updateConflictFiles() called with: folderConflictFiles = [" + folderConflictFiles + "]");

        boolean clearConflictCount = folderConflictFiles.isEmpty();
        for (int si = 0, ss = mSections.size(); si < ss; si++) {
            Section section = mSections.get(si);
            List<Item> items = section.getItems();
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                Item item = items.get(ii);
                if (item.getFile().equals(mMoveFileSourceDir)) {
                    continue;
                }

                if (clearConflictCount) {

                    if (item.clearConflictFiles()) {

                        // TODO replaced with notifyItem
                        if (mRecyclerView != null) {
                            int absolutePosition = getAbsolutePosition(si, ii);

                            Bundle playload = new Bundle();
                            playload.putInt(PAYLOAD_KEY_CONFLICT_FILES, item.getConflictFiles().size());
                            notifyItemChanged(absolutePosition, playload);

//                        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(absolutePosition);
//                        if (vh != null && vh instanceof ItemViewHolder) {
//                            Log.d(TAG, "updateConflictFiles: update conflict file count");
//
//                            bindViewHolderBadge((ItemViewHolder) vh, item);
//                        }
                        }
                    }

                } else {

                    if (folderConflictFiles.containsKey(item.getFile())) {
                        List<File> conflictFiles = folderConflictFiles.get(item.getFile());
                        item.setConflictFiles(conflictFiles);

                        // TODO replaced with notifyDataSetChanged
                        if (mRecyclerView != null) {
                            RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(getAbsolutePosition(si, ii));
                            if (vh != null && vh instanceof ItemViewHolder) {
                                Log.d(TAG, "updateConflictFiles: update conflict file count");

                                bindViewHolderBadge((ItemViewHolder) vh, item);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 局部刷新更新
     *
     * @param newAdapter
     */
    public void diffUpdate(SectionedFolderListAdapter newAdapter) {
        int oldItemCount = getItemCount();
        int newItemCount = newAdapter.getItemCount();

        if (mShowConflictBadge) {
            newAdapter.updateConflictFiles(getConflictFiles());
        }

        Log.d(TAG, "diffUpdate: 计算差异 old " + oldItemCount + " new " + newItemCount);

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
                ItemCoord newCoord = newAdapter.getRelativePosition(newItemPosition);

                ItemType oldItemType = getItemType(SectionedFolderListAdapter.this, oldItemPosition);
                ItemType newItemType = getItemType(newAdapter, newItemPosition);

                if (oldItemType == newItemType) {
                    // 都是 Header 比较 Section 对应的文件
                    if (oldItemType == ItemType.HEADER) {
                        Section oldSec = mSections.get(oldCoord.section());
                        Section newSec = newAdapter.getSections().get(newCoord.section());

                        return oldSec.getFile().equals(newSec.getFile());
                    }

                    // 都是 Footer 比较 Section 对应的文件
                    if (oldItemType == ItemType.FOOTER) {
                        Section oldSec = mSections.get(oldCoord.section());
                        Section newSec = newAdapter.getSections().get(newCoord.section());

                        return oldSec.getFile().equals(newSec.getFile());
                    }

                    if (oldItemType == ItemType.ITEM) {
                        Item oldItem = SectionedFolderListAdapter.this.getItem(oldCoord);
                        Item newItem = newAdapter.getItem(newCoord);

                        boolean equals = oldItem.getFile().equals(newItem.getFile());
//                        if (!equals) {
//                            Log.d(TAG, String.format("areItemsTheSame() called with: oldItemPosition = [%d], newItemPosition = [%d] equal = %s, diff : %s, %s",
//                                    oldItemPosition, newItemPosition, equals, oldItem.getFile(), newItem.getFile()));
//                        }

                        return equals;
//                        return oldCoord.section() == newCoord.section() && oldCoord.relativePos() == newCoord.relativePos();
                    }
                }

                // 类型不一样这两项自然是不一样
                return false;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                ItemCoord oldCoord = getRelativePosition(oldItemPosition);
                ItemCoord newCoord = newAdapter.getRelativePosition(newItemPosition);

                ItemType oldItemType = getItemType(SectionedFolderListAdapter.this, oldItemPosition);
                ItemType newItemType = getItemType(newAdapter, newItemPosition);

                if (oldItemType == newItemType) {
                    // Item 类型一样比较 Item 的值
                    switch (oldItemType) {

                        case HEADER:
                        case FOOTER: {
                            Section oldSec = mSections.get(oldCoord.section());
                            Section newSec = newAdapter.getSections().get(newCoord.section());

                            // Section Header 的显示名称是否一样
                            return StringUtils.equals(oldSec.getName(), newSec.getName());
                        }
                        case ITEM: {
                            Item oldItem = SectionedFolderListAdapter.this.getItem(oldCoord);
                            Item newItem = newAdapter.getItem(newCoord);

                            List<File> oldConflictFiles = oldItem.getConflictFiles();
                            List<File> newConflictFiles = newItem.getConflictFiles();

                            // TODO: 不用如此严格检测冲突文件列表顺序的吧？
                            boolean isSameConflictFileList = ListUtils.isEqualList(oldConflictFiles, newConflictFiles);
                            boolean isSameName = StringUtils.equals(oldItem.getName(), newItem.getName());
                            boolean isSameThumbList = ListUtils.isEqualList(oldItem.getThumbList(), newItem.getThumbList());

                            boolean isSameSelection = oldItem.isSelected() == newItem.isSelected();

                            return isSameName && isSameThumbList && isSameConflictFileList && isSameSelection;
                        }
                    }
                }

                return false;
            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {

                ItemCoord oldCoord = getRelativePosition(oldItemPosition);
                ItemCoord newCoord = newAdapter.getRelativePosition(newItemPosition);

                ItemType oldItemType = getItemType(SectionedFolderListAdapter.this, oldItemPosition);

                switch (oldItemType) {

                    case FOOTER:
                    case HEADER: {
                        // 执行到这里说明 Section 名称不一样，保存新的 Section 名称即可
                        Section oldSec = mSections.get(oldCoord.section());
                        Section newSec = newAdapter.getSections().get(newCoord.section());

                        Bundle res = new Bundle();
                        if (!StringUtils.equals(oldSec.getName(), newSec.getName())) {
                            res.putString(PAYLOAD_KEY_SECTION_NAME, newSec.getName());
                        }
                        return res;
                    }
                    case ITEM: {
                        Item oldItem = SectionedFolderListAdapter.this.getItem(oldCoord);
                        Item newItem = newAdapter.getItem(newCoord);

                        List<File> oldConflictFiles = oldItem.getConflictFiles();
                        List<File> newConflictFiles = newItem.getConflictFiles();

                        // TODO: 不用如此严格检测冲突文件列表顺序的吧？
                        boolean isSameConflictFileList = ListUtils.isEqualList(oldConflictFiles, newConflictFiles);
                        boolean isSameName = StringUtils.equals(oldItem.getName(), newItem.getName());
                        boolean isSameThumbList = ListUtils.isEqualList(oldItem.getThumbList(), newItem.getThumbList());
                        boolean isSameSelection = oldItem.isSelected() == newItem.isSelected();

                        // 哪一项不一样就只存哪一项
                        Bundle payloadBundle = new Bundle();
                        if (!isSameName) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_ITEM_NAME " + newItem.getName());
                            payloadBundle.putString(PAYLOAD_KEY_ITEM_NAME, newItem.getName());
                        }

                        if (!isSameThumbList) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_THUMB_LIST " + newItem.getThumbList());
                            payloadBundle.putStringArrayList(PAYLOAD_KEY_THUMB_LIST,
                                    PathUtils.fileListToPathArrayList(newItem.getThumbList()));
                        }

                        if (!isSameConflictFileList) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_CONFLICT_FILES" + newItem.getConflictFiles());
                            payloadBundle.putStringArrayList(PAYLOAD_KEY_CONFLICT_FILES,
                                    PathUtils.fileListToPathArrayList(newItem.getConflictFiles()));
                        }

                        if (!isSameSelection) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_SELECTION(old) " + oldItem.isSelected());
                            payloadBundle.putBoolean(PAYLOAD_KEY_SELECTION, newItem.isSelected());
                        }
                        return payloadBundle;
                    }
                }
                return super.getChangePayload(oldItemPosition, newItemPosition);
            }
        });

        setShowInFilterMode(newAdapter.isShowInFilterMode());
        mSections = newAdapter.getSections();
        diffResult.dispatchUpdatesTo(this);
    }

    public void updateThumbList(File directory, List<File> thumbnails) {
        if (directory == null || thumbnails == null) {
            return;
        }

        for (int secIndex = 0, mSectionsSize = mSections.size(); secIndex < mSectionsSize; secIndex++) {
            Section section = mSections.get(secIndex);
            List<Item> items = section.getItems();
            for (int itemIndex = 0, itemsSize = items.size(); itemIndex < itemsSize; itemIndex++) {
                Item item = items.get(itemIndex);
                if (item.getFile().equals(directory)) {
                    item.setThumbList(thumbnails);
                    if (mRecyclerView != null) {
                        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(getAbsolutePosition(secIndex, itemIndex));
                        if (vh != null && vh instanceof SectionedFolderListAdapter.ItemViewHolder) {
                            updateThumbList((ItemViewHolder) vh, item, true);
                        }
                    }
                    return;
                }
            }
        }
    }

    public void updateItemCount(File dir, int count) {
        if (dir == null || count < 0) {
            return;
        }

        for (int si = 0, mSectionsSize = mSections.size(); si < mSectionsSize; si++) {
            Section section = mSections.get(si);
            List<Item> items = section.getItems();
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                Item item = items.get(ii);
                if (item.getFile().equals(dir)) {

                    item.setCount(count);
                    ItemViewHolder vh = findItemViewHolder(si, ii);
                    if (vh != null) {
                        vh.count.setText(String.valueOf(count));
                    }
                    return;
                }
            }
        }
    }

    public void updateSelectedCount(Map<File, Integer> fileSelectedCountMap) {
        if (fileSelectedCountMap == null) {
            return;
        }

        for (int i = 0, mSectionsSize = mSections.size(); i < mSectionsSize; i++) {
            Section section = mSections.get(i);
            List<Item> items = section.getItems();
            for (int i1 = 0, itemsSize = items.size(); i1 < itemsSize; i1++) {
                Item item = items.get(i1);
                Integer count = fileSelectedCountMap.get(item.getFile());
                if (count != null) {
                    item.setSelectedCount(count);


                }
            }
        }
    }

    public void scrollToItem(File dir) {
        for (int si = 0, mSectionsSize = mSections.size(); si < mSectionsSize; si++) {
            Section section = mSections.get(si);
            List<Item> items = section.getItems();
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                Item item = items.get(ii);
                if (item.getFile().equals(dir)) {

                    if (mRecyclerView != null) {
                        mRecyclerView.scrollToPosition(getAbsolutePosition(si, ii));
                    }
                    return;
                }
            }
        }
    }

    public void updateSelectedCount(File dir, int selectedCount) {
        if (dir == null || selectedCount < 0) {
            return;
        }

        for (int si = 0, mSectionsSize = mSections.size(); si < mSectionsSize; si++) {
            Section section = mSections.get(si);
            List<Item> items = section.getItems();
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                Item item = items.get(ii);
                if (item.getFile().equals(dir)) {

                    item.setSelectedCount(selectedCount);
                    ItemViewHolder itemViewHolder = findItemViewHolder(si, ii);
                    if (itemViewHolder != null) {
                        bindViewHolderBadge(itemViewHolder, item);
                    }
                    return;
                }
            }
        }
    }

    private ItemViewHolder findItemViewHolder(int sectionIndex, int relativePosition) {

        int absolutePosition = getAbsolutePosition(sectionIndex, relativePosition);
        if (mRecyclerView != null) {
            RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(absolutePosition);
            if (vh != null && vh instanceof ItemViewHolder) {
                return ((ItemViewHolder) vh);
            }
        }
        return null;
    }

    public void updateSelectedCount(ItemCoord relativePosition) {
        if (mRecyclerView != null) {
            int absolutePosition = getAbsolutePosition(relativePosition);
            new SectionedListItemClickDispatcher(this)
                    .dispatch(absolutePosition, new SectionedListItemDispatchListener() {
                        @Override
                        public void onHeader(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {

                        }

                        @Override
                        public void onFooter(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {

                        }

                        @Override
                        public void onItem(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {
                            mRecyclerView.findViewHolderForAdapterPosition(absolutePosition);
                        }
                    });

        }
    }

    /*
     * Interfaces and Classes
     */
    public static class Section implements Cloneable {
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

        @Override
        protected Object clone() throws CloneNotSupportedException {
            Section clone = (Section) super.clone();
            clone.setFile(new File(mFile.getAbsolutePath()));

            if (mItems != null) {
                clone.mItems = new LinkedList<>();
                for (Item item : mItems) {
                    clone.mItems.add(item);
                }
            }
            return clone;
        }
    }

    public static final int COUNT_NONE = -1;

    public static class Item implements Cloneable {
        String mName;
        int mSelectedCount = COUNT_NONE;
        int mCount;
        File mFile;
        List<File> mThumbList;
        List<File> mConflictFiles;
        boolean mIsSelected;
        boolean mIsSelectionSourceDir = false;

        private int mKeywordStartIndex;
        private int mKeywordLength;

        public boolean isSelectionSourceDir() {
            return mIsSelectionSourceDir;
        }

        public Item setSelectionSourceDir(boolean selectionSourceDir) {
            mIsSelectionSourceDir = selectionSourceDir;
            return this;
        }

        public List<File> getConflictFiles() {
            return mConflictFiles;
        }

        public Item setConflictFiles(List<File> conflictFiles) {
            Log.d(TAG, "setConflictFiles: set " + mName + " conflict files :" + conflictFiles);
            mConflictFiles = conflictFiles;
            return this;
        }

        public boolean clearConflictFiles() {
            if (mConflictFiles != null && !mConflictFiles.isEmpty()) {
                mConflictFiles.clear();
                return true;
            } else {
                return false;
            }
        }

        public int getSelectedCount() {
            return mSelectedCount;
        }

        public Item setSelectedCount(int selectedCount) {
            mSelectedCount = selectedCount;
            return this;
        }

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

        public Item setSelected(boolean selected) {
            mIsSelected = selected;
            return this;
        }

        public boolean isSelected() {
            return mIsSelected;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "mName='" + mName + '\'' +
                    ", mSelectedCount=" + mSelectedCount +
                    ", mCount=" + mCount +
                    ", mConflictFiles=" + mConflictFiles +
                    ", mIsSelected=" + mIsSelected +
                    '}';
        }

        public Item setKeywordStartIndex(int keywordStartIndex) {
            mKeywordStartIndex = keywordStartIndex;
            return this;
        }

        public int getKeywordStartIndex() {
            return mKeywordStartIndex;
        }

        public Item setKeywordLength(int keywordLength) {
            mKeywordLength = keywordLength;
            return this;
        }

        public int getKeywordLength() {
            return mKeywordLength;
        }

        @Override
        protected Object clone() {
            try {
                Item item = (Item) super.clone();
                item.setFile(this.getFile() == null ? null : new File(this.getFile().getAbsolutePath()));
                if (this.mThumbList != null) {
                    item.mThumbList = new LinkedList<>();
                    for (File file : this.mThumbList) {
                        item.mThumbList.add(file);
                    }
                }
                if (this.mConflictFiles != null) {
                    item.mConflictFiles = new LinkedList<>();
                    for (File conflictFile : this.mConflictFiles) {
                        item.mConflictFiles.add(conflictFile);
                    }
                }

                return item;
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return null;
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

    public boolean isShowSourceDirBadgeWhenEmpty() {
        return mShowSourceDirBadgeWhenEmpty;
    }

    public SectionedFolderListAdapter setShowSourceDirBadgeWhenEmpty(boolean showSourceDirBadgeWhenEmpty) {
        mShowSourceDirBadgeWhenEmpty = showSourceDirBadgeWhenEmpty;
        return this;
    }

    public File getMoveFileSourceDir() {
        return mMoveFileSourceDir;
    }

    public SectionedFolderListAdapter setMoveFileSourceDir(File moveFileSourceDir) {

        boolean clearOldDir = moveFileSourceDir != null;

        for (int si = 0, mSectionsSize = mSections.size(); si < mSectionsSize; si++) {
            Section section = mSections.get(si);
            List<Item> items = section.getItems();
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                Item item = items.get(ii);
                if (moveFileSourceDir == null) {
                    // 清除标记为‘源目录’的项
                    if (item.isSelectionSourceDir()) {
                        item.setSelectionSourceDir(false);

                        updateViewHolder(si, ii, item, (vh, it) -> {
                           bindViewHolderBadge(vh, item);
//                            vh.badge.hide(false);
                        });
                    }
                } else {

                    // Clear old item's Mark label 'source file'
                    if (clearOldDir && item.getFile().equals(mMoveFileSourceDir)) {
                        item.setSelectionSourceDir(false);

                        if (mShowConflictBadge) {
                            int conflictCount = item.getConflictFiles() == null ? 0 : item.getConflictFiles().size();
                            if (item.getConflictFiles() != null) {
                                updateViewHolder(si, ii, item, (vh, it) -> {
                                    bindViewHolderBadge(vh, item);
//                                    vh.setConflictCount(conflictCount);
                                });
                            } else {
                                updateViewHolder(si, ii, item, (vh, it) -> {
                                    bindViewHolderBadge(vh, item);
//                                    vh.badge.hide(false);
                                });
                            }
                        } else {
                            updateViewHolder(si, ii, item, (vh, it) -> {
                                bindViewHolderBadge(vh, item);
//                                vh.setSelectedCount(item.getSelectedCount());
                            });
                        }
                    }

                    // 当前 item 是要设置为 ‘源目录’
                    if (item.getFile().equals(moveFileSourceDir)) {
                        item.setSelectionSourceDir(true);

                        updateViewHolder(si, ii, item, (vh, it) -> {
                            bindViewHolderBadge(vh, item);
                            //vh.showSourceDirBadge(item.getSelectedCount());
                        });
                    }
                }
            }
        }


        mMoveFileSourceDir = moveFileSourceDir;
        return this;
    }

    public Map<File, List<File>> getConflictFiles() {
        return mConflictFiles;
    }

    public SectionedFolderListAdapter setConflictFiles(Map<File, List<File>> conflictFiles) {
        mConflictFiles = conflictFiles;
        return this;
    }

    public boolean isShowConflictBadge() {
        return mShowConflictBadge;
    }

    public SectionedFolderListAdapter setShowConflictBadge(boolean showConflictBadge) {
        mShowConflictBadge = showConflictBadge;
        return this;
    }

    public boolean isShowInFilterMode() {
        return mShowInFilterMode;
    }

    public SectionedFolderListAdapter setShowInFilterMode(boolean showInFilterMode) {
        mShowInFilterMode = showInFilterMode;
        return this;
    }

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

    private <R> R extractTagValue(View view, int id) {
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
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mRecyclerView = null;
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int sectionIndex, int relativePosition, int absolutePosition) {
        ItemViewHolder vh = (ItemViewHolder) holder;
        Section section = mSections.get(sectionIndex);
        Item item = section.getItems().get(relativePosition);

        /*
         * Item click
         */
        if (mEnableItemClick) {

            // Save data to view tag
            vh.itemView.setTag(R.id.section, section);
            vh.itemView.setTag(R.id.item, item);

            // Click
            vh.itemView.setOnClickListener(v -> {
                int adapterPosition = holder.getAdapterPosition();
                ItemCoord coor = getRelativePosition(adapterPosition);

                Item clickItem = mSections.get(coor.section()).getItems().get(coor.relativePos());
                // Mark as 'selected'
                {
                    if (!clickItem.isSelected()) {

                        // Section loop : Find single selected item and mark it as 'unselected'
                        for (int si = 0, mSectionsSize = mSections.size(); si < mSectionsSize; si++) {

                            Section currSec = mSections.get(si);
                            List<Item> items = currSec.getItems();

                            // Item loop : find current selected item index
                            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                                Item currItem = items.get(ii);
                                if (currItem.isSelected()) {
                                    // mark as 'unselected'
                                    int absPos = getAbsolutePosition(si, ii);
                                    currItem.setSelected(false);
                                    updateItemViewHolderCheckStatus(absPos, currItem.isSelected());
                                }
                            }

                        }

                        // Mark the current clicked item as 'selected'
                        clickItem.setSelected(true);
                        updateItemViewHolderCheckStatus(adapterPosition, clickItem.isSelected());
                    } else {
                        Log.w(TAG, "item view clicked: click a selected item at coor " + coor);
                    }
                }

                // Notify item clicking
                if (mOnItemClickListener != null) {
                    Section sect = mSections.get(coor.section());
                    mOnItemClickListener.onItemClick(
                            sect, coor.section(),
                            sect.getItems().get(coor.relativePos()),
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

        vh.check.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);

        // Folder name
        String name = item.getName();
        if (mShowInFilterMode) {
            int keywordStartIndex = item.getKeywordStartIndex();
            if (keywordStartIndex >= 0 && item.getKeywordLength() > 0 && keywordStartIndex < name.length()) {

                SpannableString nameSpan = new SpannableString(name);
                nameSpan.setSpan(new ForegroundColorSpan(vh.name.getContext().getResources().getColor(R.color.colorAccent)),
                        keywordStartIndex, keywordStartIndex + item.getKeywordLength(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                vh.name.setText(nameSpan);
            } else {
                vh.name.setText(name);
            }
        } else {
            vh.name.setText(name);
        }

        // Badge
        bindViewHolderBadge(vh, item);

        // File total file count
        vh.count.setText(String.valueOf(item.getCount()));

//        vh.setSelectedCountText(item.getSelectedCount(), item.getCount());

        // Thumbnail image list
        updateThumbList(vh, item, false);
    }

    private void bindViewHolderBadge(ItemViewHolder vh, Item item) {
        if (item.isSelectionSourceDir()) {

            if (mShowSourceDirBadgeWhenEmpty) {
                vh.showSourceDirBadge(item.getSelectedCount());
            } else {
                if (item.getSelectedCount() > 0) {
                    vh.showSourceDirBadge(item.getSelectedCount());
                } else {
                    vh.badge.hide(false);
                }
            }

        } else {

            if (mShowConflictBadge) {
                if (cn.intret.app.picgo.utils.ListUtils.isEmpty(item.getConflictFiles())) {
                    if (item.getSelectedCount() > 0) {
                        vh.setSelectedCount(item.getSelectedCount());
                    } else {
                        vh.badge.hide(false);
                    }
                } else {
                    vh.setConflictCount(item.getConflictFiles().size());
                }
            } else {
                vh.setSelectedCount(item.getSelectedCount());
            }
        }
    }

    private void updateThumbList(ItemViewHolder vh, Item item, boolean forceUpdate) {
        if (forceUpdate) {
            vh.mAdapter = null;
        }

        if (vh.mAdapter == null) {

            vh.mAdapter = new HorizontalImageListAdapter(filesToItems(item.getThumbList()));
            vh.thumbList.setClickable(false);

            vh.thumbList.setLayoutManager(vh.getLayout());
            vh.thumbList.setAdapter(vh.mAdapter);
        } else {
            vh.thumbList.setClickable(false);
            vh.thumbList.setLayoutManager(vh.getLayout());
            vh.thumbList.swapAdapter(vh.mAdapter, false);
        }
    }

    private void updateItemViewHolderCheckStatus(int absolutePosition, boolean selected) {
        Log.d(TAG, "updateItemViewHolderCheckStatus() called with: absolutePosition = [" + absolutePosition + "], selected = [" + selected + "]");

        if (mRecyclerView != null) {
            RecyclerView.ViewHolder selectedVH = mRecyclerView.findViewHolderForAdapterPosition(absolutePosition);
            if (selectedVH != null && selectedVH instanceof ItemViewHolder) {
                ((ItemViewHolder) selectedVH).check.setVisibility(selected ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void updateViewHolder(int sectionIndex, int relativeIndex, Item item, DataConsumer2<ItemViewHolder, Item> consumer) {
        if (mRecyclerView != null) {
            int absolutePosition = getAbsolutePosition(sectionIndex, relativeIndex);
            RecyclerView.ViewHolder selectedVH = mRecyclerView.findViewHolderForAdapterPosition(absolutePosition);
            if (selectedVH != null && selectedVH instanceof ItemViewHolder && consumer != null) {
                consumer.accept(((ItemViewHolder) selectedVH), item);
            }
        }
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition, List<Object> payload) {
        if (payload.isEmpty()) {
            Log.w(TAG, "onBindViewHolder: payload is empty");
            onBindViewHolder(holder, section, relativePosition, absolutePosition);
        } else {
            Object o = payload.get(0);
            if (o instanceof Bundle) {

                Bundle bundle = (Bundle) o;
                if (isHeader(absolutePosition)) {
                    SectionedImageListAdapter.SectionHeaderViewHolder viewHolder =
                            (SectionedImageListAdapter.SectionHeaderViewHolder) holder;
                    String name = bundle.getString(PAYLOAD_KEY_SECTION_NAME);
                    ((SectionedImageListAdapter.SectionHeaderViewHolder) holder).title.setText(name);
                } else if (isFooter(absolutePosition)) {
                    Log.w(TAG, "onBindViewHolder: update footer do nothing");
                } else {
                    ItemViewHolder vh = (ItemViewHolder) holder;

                    String name = bundle.getString(PAYLOAD_KEY_ITEM_NAME);
                    if (name != null) {
                        Log.d(TAG, "onBindViewHolder: 部分更新 Item 名称 : " + name);
                        vh.name.setText(name);
                    }

                    ArrayList<String> thumbList = bundle.getStringArrayList(PAYLOAD_KEY_THUMB_LIST);
                    if (thumbList != null) {
                        Log.d(TAG, "onBindViewHolder: partial update thumbList: " + thumbList);

                        vh.mAdapter = new HorizontalImageListAdapter(
                                filesToItems(PathUtils.stringArrayListToFileList(thumbList)));
                        vh.thumbList.setClickable(false);

                        vh.thumbList.setLayoutManager(vh.getLayout());
                        vh.thumbList.setAdapter(vh.mAdapter);
                    } else {
                        vh.thumbList.setAdapter(null);
                    }

                    ArrayList<String> conflictFileList = bundle.getStringArrayList(PAYLOAD_KEY_CONFLICT_FILES);
                    if (conflictFileList != null) {
                        Log.w(TAG, "onBindViewHolder: do nothing for conflict file partial update : " + conflictFileList);
                    }

                    Boolean selected = bundle.getBoolean(PAYLOAD_KEY_SELECTION, false);
                    Log.d(TAG, "onBindViewHolder: partial update selected status, " + section + ":" + relativePosition + " selected : " + selected);
                    vh.check.setVisibility(selected ? View.VISIBLE : View.GONE);

                    int conflictFileCount = bundle.getInt(PAYLOAD_KEY_CONFLICT_FILES_COUNT, -1);
                    if (conflictFileCount != -1) {
                        Log.d(TAG, String.format("onBindViewHolder: 目录更新文件冲突个数为 %d", conflictFileCount));
                        vh.setConflictCount(conflictFileCount);
                    }
                }
            } else {
                Log.w(TAG, "onBindViewHolder: no bundle in list : " + o);
            }
        }
    }

    private List<HorizontalImageListAdapter.Item> filesToItems(List<File> thumbList) {
        if (thumbList == null) {
            return null;
        }
        return Stream.of(thumbList).map(file -> new HorizontalImageListAdapter.Item().setFile(file)).toList();
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

    @Override
    public void onViewRecycled(SectionedViewHolder holder) {

        if (holder instanceof ItemViewHolder) {
            Object tag = ((ItemViewHolder) holder).itemView.getTag(R.id.item);

            ((ItemViewHolder) holder).mAdapter = null;
            ((ItemViewHolder) holder).mLinearLayoutManager = null;
        }

        super.onViewRecycled(holder);
    }

    /*
     * Item selection status
     */

    /*
     * Inner class
     */

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


        @BindView(R.id.check) ImageView check;
        @BindView(R.id.name) TextView name;
        @BindView(R.id.count) TextView count;
        @BindView(R.id.thumb_list) RecyclerView thumbList;
        Badge badge;

        HorizontalImageListAdapter mAdapter;
        private LinearLayoutManager mLinearLayoutManager;

        ItemViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            // Setup view holder. You'd want some views to be optional, e.g. the
            // header/footer will have views that normal item views do or do not have.
            //itemView.setOnClickListener(this);

            this.badge = new QBadgeView(itemView.getContext())
                    .bindTarget(thumbList);
            Resources resources = itemView.getContext().getResources();
            badge.setBadgeGravity(Gravity.START | Gravity.TOP)
                    .setExactMode(true)
                    .setBadgeBackgroundColor(resources.getColor(R.color.colorAccent))
                    .setBadgeTextColor(resources.getColor(android.R.color.white))
            ;
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

            // Single selection status


            // Notification
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

        public void setSelectedCount(int selectedCount) {
            if (selectedCount > 0) {
                badge.setBadgeBackgroundColor(itemView.getContext().getResources().getColor(R.color.colorAccent));
                badge.setBadgeNumber(selectedCount);
            } else {
                badge.hide(false);
            }
        }

        void setConflictCount(int conflictCount) {
            badge.setBadgeBackgroundColor(itemView.getContext().getResources().getColor(R.color.warnning));
//            badge.setBadgeNumber(conflictCount);
            badge.setBadgeText(itemView.getContext().getResources().getString(R.string.conflict_d, conflictCount));
        }

        void showSourceDirBadge() {
            Resources resources = itemView.getContext().getResources();
            badge.setBadgeText(resources.getString(R.string.source_folder));
            badge.setBadgeBackgroundColor(resources.getColor(R.color.colorAccent));
        }

        void showSourceDirBadge(int selectCount) {
            Resources resources = itemView.getContext().getResources();
            if (selectCount > 0) {
                badge.setBadgeText(resources.getString(R.string.source_folder_d, selectCount));
            } else {
                badge.setBadgeText(resources.getString(R.string.source_folder));
            }
            badge.setBadgeBackgroundColor(resources.getColor(R.color.colorAccent));
        }

        @Deprecated
        public void setSelectedCountText(int selectedCount, int count) {
            if (selectedCount == COUNT_NONE || selectedCount == 0) {
                this.count.setText(String.valueOf(count));
                this.count.setTextColor(this.count.getContext().getResources().getColor(R.color.list_item_text_light));
                this.count.setBackground(null);
            } else {
                this.count.setText(this.count.getContext().getResources().getString(R.string.percent_d_d, selectedCount, count));
                this.count.setTextColor(this.count.getContext().getResources().getColor(R.color.white));
                this.count.setBackgroundResource(R.drawable.badge);
            }
        }
    }
}

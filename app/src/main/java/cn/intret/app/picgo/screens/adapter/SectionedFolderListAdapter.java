package cn.intret.app.picgo.screens.adapter;

import android.content.Context;
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
import com.annimon.stream.function.Predicate;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.utils.BundleUtils;
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
    private static final String PAYLOAD_KEY_COUNT = "item_count";
    private static final String PAYLOAD_KEY_KEYWORD_LENGTH = "item_keyword_lenght";
    private static final String PAYLOAD_KEY_KEYWORD_START_INDEX = "item_keyword_start_index";
    private static final String PAYLOAD_KEY_SUB_TYPE = "item_selection_source_dir";
    /*
     * UI options
     */
    private boolean mShowHeaderOptionButton = false;
    private boolean mShowCloseButton = false;


    // 过滤模式会高亮显示匹配关键字
    private boolean mHighlightItemName = false;
    private boolean mEnableItemClick = true;
    private boolean mIsSelectable = false;
    private boolean mIsCollapsable = true;
    private boolean mIsMultiSelect = false;
    // 是否显示冲突微标
    private boolean mShowConflictBadge;
    private boolean mShowSourceDirBadgeWhenEmpty = true;
    private boolean mFiltering = false;

    /*
     * Data
     */
    private List<Section> mSections = new LinkedList<>();
    private RecyclerView mRecyclerView;
    private Map<File, List<File>> mConflictFiles;
    private File mMoveFileSourceDir;
    private List<Section> mSectionsBeforeFilter;


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
            Log.w(TAG, "selectItem: select null directory");
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
                        currItem.setSelected(false); // mark as 'unselected'
                    }


                    if (mRecyclerView != null) {
                        int absPos = getAbsolutePosition(si, ii);
                        updateItemViewHolderCheckStatus(absPos, currItem.isSelected());
                    }
                } else {
                    if (currItem.getFile().equals(dir)) {
                        currItem.setSelected(true);

                        if (mRecyclerView != null) {
                            RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
                            if (lm != null) {
                                int absPos = getAbsolutePosition(si, ii);
                                updateItemViewHolderCheckStatus(absPos, currItem.isSelected());
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean removeFolderItem(File selectedDir) {

        int sec = -1;
        int relative = -1;

        for (int si = 0, mSectionsSize = mSections.size(); si < mSectionsSize; si++) {
            Section section = mSections.get(si);
            List<Item> items = section.getItems();
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                Item item = items.get(ii);
                if (item.getFile().equals(selectedDir)) {
                    sec = si;
                    relative = ii;
                }
            }
        }


        if (sec == -1 || relative == -1) {
            return false;
        } else {
            mSections.get(sec).getItems().remove(relative);

            int absolutePosition = getAbsolutePosition(sec, relative);
            notifyItemRemoved(absolutePosition);

            return true;
        }
    }

    public void removeFolderItem(int sectionIndex, int relativePosition) {
        if (sectionIndex >= 0 && sectionIndex < mSections.size()) {
            Section section = mSections.get(sectionIndex);
            List<Item> items = section.getItems();

            if (relativePosition >= 0 && relativePosition < items.size()) {
                int absolutePosition = getAbsolutePosition(sectionIndex, relativePosition);
                items.remove(relativePosition);
                notifyItemRemoved(absolutePosition);
            }
        } else {
            Log.w(TAG, "removeFolderItem: invalid argument");
        }
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
        if (mMoveFileSourceDir != null && folderConflictFiles.size() == 1 && folderConflictFiles.containsKey(mMoveFileSourceDir)) {
            clearConflictCount = true;
        }

        for (int si = 0, ss = mSections.size(); si < ss; si++) {
            Section section = mSections.get(si);
            List<Item> items = section.getItems();
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                Item item = items.get(ii);
                if (item.getFile() == null) {
                    continue;
                }
                if (Objects.equals(item.getFile(), mMoveFileSourceDir)) {
                    continue;
                }

                if (clearConflictCount) {

                    if (item.clearConflictFiles()) {

                        // TODO replaced with notifyItem

                        if (mRecyclerView != null) {
                            int absolutePosition = getAbsolutePosition(si, ii);

                            Bundle payload = new Bundle();
                            payload.putStringArrayList(PAYLOAD_KEY_CONFLICT_FILES, PathUtils.fileListToPathArrayList(item.getConflictFiles()));
                            notifyItemChanged(absolutePosition, payload);

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
                        if (mShowConflictBadge) {
                            item.setItemSubType(ItemSubType.CONFLICT_COUNT);
                        } else {
                            Log.w(TAG, "updateConflictFiles: 设置了conflict 文件列表但是配置为不显示 conflict badge");
                            item.setItemSubType(ItemSubType.NORMAL);
                        }

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

        // 应用新的过滤模式
        setHighlightItemName(newAdapter.isHighlightItemName());

        // 更新新 item 中的选中状态
        File selectedItem = getSelectedItem();
        if (selectedItem != null) {
            newAdapter.selectItem(selectedItem);
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

                        boolean equals = Objects.equals(oldItem.getFile(), newItem.getFile());
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

                            return oldItem.contentEquals(newItem);
                        }
                        default:
                            Log.e(TAG, "areContentsTheSame: unhandled type : " + oldItemType);
                            return false;
                    }
                } else {
                    Log.w(TAG, "areContentsTheSame: not same type old:" + oldItemType + " new:" + newItemType);
                    return false;
                }

            }

            @Nullable
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {

                ItemCoord oldCoord = getRelativePosition(oldItemPosition);
                ItemCoord newCoord = newAdapter.getRelativePosition(newItemPosition);

                ItemType oldItemType = getItemType(SectionedFolderListAdapter.this, oldItemPosition);
                ItemType newItemType = getItemType(newAdapter, newItemPosition);

                if (oldItemType != newItemType) {
                    Log.w(TAG, "getChangePayload: not the same item type : old=" + oldItemCount + " new:" + newItemType);
                    return null;
                }
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
                        Item oldItem = getItem(oldCoord);
                        Item newItem = newAdapter.getItem(newCoord);

                        if (oldItem == null || newItem == null) {

                            Log.w(TAG, "getChangePayload: empty item " + oldItem + " new " + newItem);
                            return null;
                        }
                        List<File> oldConflictFiles = oldItem.getConflictFiles();
                        List<File> newConflictFiles = newItem.getConflictFiles();

                        // TODO: 不用如此严格检测冲突文件列表顺序的吧？
                        boolean isSameConflictFileList = ListUtils.isEqualList(oldConflictFiles, newConflictFiles);
                        boolean isSameName = StringUtils.equals(oldItem.getName(), newItem.getName());
                        boolean isSameThumbList = ListUtils.isEqualList(oldItem.getThumbList(), newItem.getThumbList());
                        boolean isSameSelection = oldItem.isSelected() == newItem.isSelected();
                        boolean isSameSubType = oldItem.getItemSubType() == newItem.getItemSubType();

                        boolean isSameCount = oldItem.getCount() == newItem.getCount();
                        boolean isSameKeywordLen = oldItem.getKeywordLength() == newItem.getKeywordLength();
                        boolean isSameKeywordStartIndex = oldItem.getKeywordStartIndex() == newItem.getKeywordStartIndex();


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

                        if (!isSameCount) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_COUNT(old) " + oldItem.isSelected());
                            payloadBundle.putInt(PAYLOAD_KEY_COUNT, newItem.getCount());
                        }

                        if (!isSameKeywordLen) {
                            payloadBundle.putInt(PAYLOAD_KEY_KEYWORD_LENGTH, newItem.getKeywordLength());
                        }

                        if (!isSameKeywordStartIndex) {
                            payloadBundle.putInt(PAYLOAD_KEY_KEYWORD_START_INDEX, newItem.getKeywordStartIndex());
                        }

                        if (!isSameSubType) {
                            payloadBundle.putSerializable(PAYLOAD_KEY_SUB_TYPE, newItem.getItemSubType());
                        }

                        return payloadBundle;
                    }
                }
                return super.getChangePayload(oldItemPosition, newItemPosition);
            }
        });

        mSections.clear();
        for (int i = 0; i < newAdapter.getSections().size(); i++) {
            Section section = newAdapter.getSections().get(i);
            mSections.add((Section) section.clone());
        }

        diffResult.dispatchUpdatesTo(this);
    }

    public File getSelectedItem() {
        for (int i = 0; i < mSections.size(); i++) {
            Section section = mSections.get(i);
            List<Item> items = section.getItems();
            for (int i1 = 0, itemsSize = items.size(); i1 < itemsSize; i1++) {
                Item item = items.get(i1);
                if (item.isSelected()) {
                    return item.getFile();
                }
            }
        }
        return null;
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
                            updateThumbList((ItemViewHolder) vh, true, new HorizontalImageListAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(View v, HorizontalImageListAdapter.Item ii, int position) {
                                    if (mOnItemClickListener != null) {
                                        mOnItemClickListener.onItemClick(null, -1, item, -1);
                                    }
                                }

                                @Override
                                public void onItemLongClick(View v, HorizontalImageListAdapter.Item ii, int position) {
                                    if (mOnItemClickListener != null) {
                                        mOnItemClickListener.onItemLongClick(v, null, -1, item, -1);
                                    }
                                }
                            }, item.getThumbList());
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

    public void scrollToItem(@Nullable File dir) {

        if (dir == null) {
            Log.w(TAG, "scrollToItem: dir is null.");
            return;
        }

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

    /**
     * Filter
     *
     * @param filter 过滤
     */
    public void filter(Predicate<? super Item> filter) {

        List<Section> filteredSections = new LinkedList<>();

        // 保存现在的数据
        mSectionsBeforeFilter = new LinkedList<>();
        for (int i = 0, mSectionsSize = mSections.size(); i < mSectionsSize; i++) {
            Section section = mSections.get(i);
            mSectionsBeforeFilter.add((Section) section.clone());
        }

        // 根据现在的数据进行过滤
        for (int i = 0, mSectionsSize = mSections.size(); i < mSectionsSize; i++) {
            Section section = (Section) mSections.get(i).clone();
            filteredSections.add(section);

            List<Item> items = Stream.of(section.getItems())
                    .filter(filter)
                    .toList();
            section.setItems(items);
        }

        // 显示过滤后的数据
        mFiltering = true;
        filter(filteredSections);
    }

    public void filter(List<Section> sections) {
        if (sections == null) {
            Log.w(TAG, "filter: 参数为空" );
            return;
        }

        SectionedFolderListAdapter adapter = new SectionedFolderListAdapter(sections);
        adapter.setHighlightItemName(mHighlightItemName);

        diffUpdate(adapter);
    }

    public void leaveFilterMode() {
        if (!mFiltering) {
            Log.w(TAG, "leaveFilterMode: 没有在过滤模式" );
            return;
        }

        mFiltering = false;
        filter(mSectionsBeforeFilter);
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
        public Object clone() {
            try {
                Section clone = (Section) super.clone();
                clone.setFile(new File(mFile.getAbsolutePath()));

                if (mItems != null) {
                    clone.mItems = new LinkedList<>();
                    for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
                        Item item = mItems.get(i);
                        clone.mItems.add(item);
                    }
                }
                return clone;
            } catch (Throwable throwable) {
                return null;
            }
        }
    }

    public static final int COUNT_NONE = -1;

    public enum ItemSubType {
        NORMAL,
        ADD_ITEM,
        CONFLICT_COUNT,
        SELECTED_COUNT,
        SOURCE_DIR,
        NONE
    }

    public static class Item implements Cloneable, ContentEqual {

        boolean mIsSelected;
        File mFile;
        int mCount;
        int mSelectedCount = COUNT_NONE;
        List<File> mConflictFiles;

        List<File> mThumbList;
        private int mKeywordLength;
        private int mKeywordStartIndex;
        String mName;

        ItemSubType mItemSubType = ItemSubType.NORMAL;

        public ItemSubType getItemSubType() {
            return mItemSubType;
        }

        public Item setItemSubType(ItemSubType itemSubType) {
            mItemSubType = itemSubType;
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Item item = (Item) o;

            if (mIsSelected != item.mIsSelected) return false;
            if (mCount != item.mCount) return false;
            if (mSelectedCount != item.mSelectedCount) return false;
            if (mKeywordLength != item.mKeywordLength) return false;
            if (mKeywordStartIndex != item.mKeywordStartIndex) return false;
            if (mFile != null ? !mFile.equals(item.mFile) : item.mFile != null) return false;
            if (mConflictFiles != null ? !mConflictFiles.equals(item.mConflictFiles) : item.mConflictFiles != null)
                return false;
            if (mThumbList != null ? !mThumbList.equals(item.mThumbList) : item.mThumbList != null)
                return false;
            if (mName != null ? !mName.equals(item.mName) : item.mName != null) return false;
            return mItemSubType == item.mItemSubType;

        }

        @Override
        public int hashCode() {
            return mFile != null ? mFile.hashCode() : 0;
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

        @Override
        public String toString() {
            return "Item{" +
                    "mIsSelected=" + mIsSelected +
                    ", file=" + mFile +
                    ", count=" + mCount +
                    ", mSelectedCount=" + mSelectedCount +
                    ", conflictFiles=" + mConflictFiles +
                    ", mThumbList=" + mThumbList +
                    ", mKeywordLength=" + mKeywordLength +
                    ", mKeywordStartIndex=" + mKeywordStartIndex +
                    ", name='" + mName + '\'' +
                    ", mItemSubType=" + mItemSubType +
                    '}';
        }

        @Override
        public boolean contentEquals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            Item rhs = (Item) obj;
            return new EqualsBuilder()
                    .appendSuper(super.equals(obj))

                    .append(mConflictFiles, rhs.mConflictFiles)
                    .append(mCount, rhs.mCount)
                    .append(mFile, rhs.mFile)
                    .append(mIsSelected, rhs.mIsSelected)
                    .append(mKeywordLength, rhs.mKeywordLength)
                    .append(mKeywordStartIndex, rhs.mKeywordStartIndex)
                    .append(mSelectedCount, rhs.mSelectedCount)
                    .append(mItemSubType, rhs.mItemSubType)
                    .append(mThumbList, rhs.mThumbList)

                    .isEquals();
        }

        public boolean isSourceDirType() {
            return mItemSubType == ItemSubType.SOURCE_DIR;
        }
    }

    public interface OnItemClickListener {
        void onSectionHeaderClick(Section section, int sectionIndex, int adapterPosition);

        void onSectionHeaderOptionButtonClick(View v, Section section, int sectionIndex);

        void onItemClick(Section sectionItem, int section, Item item, int relativePos);

        void onItemLongClick(View v, Section sectionItem, int section, Item item, int relativePos);

        void onItemCloseClick(View v, Section section, Item item, int sectionIndex, int relativePosition);
    }

    /*
     * Event handler
     */
    private OnItemClickListener mOnItemClickListener;

     /*
     * Getter and setter
     */

    public boolean isFiltering() {
        return mFiltering;
    }

    public SectionedFolderListAdapter setFiltering(boolean filtering) {
        mFiltering = filtering;
        return this;
    }

    public boolean isShowCloseButton() {
        return mShowCloseButton;
    }

    public SectionedFolderListAdapter setShowCloseButton(boolean showCloseButton) {
        mShowCloseButton = showCloseButton;
        return this;
    }

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

    /**
     * @param moveFileSourceDir 指定文件则标记对应的项为'源目录'，指定 null 则清除任何标记为 '源目录' 的项；
     * @return
     */
    public SectionedFolderListAdapter setMoveFileSourceDir(File moveFileSourceDir) {

        boolean clearMark = moveFileSourceDir != null;

        if (clearMark) {
            // 清除标志

            for (int si = 0, mSectionsSize = mSections.size(); si < mSectionsSize; si++) {
                Section section = mSections.get(si);
                List<Item> items = section.getItems();
                for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                    Item item = items.get(ii);
                    if (item.getItemSubType() == ItemSubType.SOURCE_DIR) {
                        // 恢复为某一种模式
                        if (mShowConflictBadge && mConflictFiles.containsKey(item.getFile())) {
                            item.setItemSubType(ItemSubType.CONFLICT_COUNT);
                        } else {
                            // TODO
                            item.setItemSubType(ItemSubType.NORMAL);
                        }

                        // 更新界面
                        updateViewHolder(si, ii, item, (vh, it) -> {
                            bindViewHolderBadge(vh, item);
                        });
                    }
                }
            }
        } else {

            // 设置标志


        }

        for (int si = 0, mSectionsSize = mSections.size(); si < mSectionsSize; si++) {
            Section section = mSections.get(si);
            List<Item> items = section.getItems();
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                Item item = items.get(ii);

                // Clear old item's Badge label 'source file'
                if (item.getItemSubType() == ItemSubType.SOURCE_DIR
                        && !Objects.equals(item.getFile(), moveFileSourceDir)) {

                    // 恢复为某种 sub type
                    if (mShowConflictBadge) {
                        item.setItemSubType(ItemSubType.CONFLICT_COUNT);

                        if (item.getConflictFiles() != null) {
                            updateViewHolder(si, ii, item, (vh, it) -> {
                                bindViewHolderBadge(vh, item);
                            });
                        } else {
                            updateViewHolder(si, ii, item, (vh, it) -> {
                                bindViewHolderBadge(vh, item);
                            });
                        }
                    } else {
                        if (item.getFile() == null) {
                            item.setItemSubType(ItemSubType.ADD_ITEM);
                        } else {
                            // TODO
                            item.setItemSubType(ItemSubType.NORMAL);
                        }

                        updateViewHolder(si, ii, item, (vh, it) -> {
                            bindViewHolderBadge(vh, item);
                        });
                    }
                }

                // 当前 item 是要设置为 ‘源目录’
                if (Objects.equals(item.getFile(), moveFileSourceDir)) {
                    item.setItemSubType(ItemSubType.SOURCE_DIR);

                    updateViewHolder(si, ii, item, (vh, it) -> {
                        bindViewHolderBadge(vh, item);
                        //vh.showSourceDirBadge(item.getSelectedItemCount());
                    });
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

    public boolean isHighlightItemName() {
        return mHighlightItemName;
    }

    public SectionedFolderListAdapter setHighlightItemName(boolean highlightItemName) {
        mHighlightItemName = highlightItemName;
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
     * Constructor
     */

    SectionedFolderListAdapter() {

    }

    public SectionedFolderListAdapter(List<Section> sections) {
        if (sections != null) {
            mSections = sections;
        }
    }

    SectionedFolderListAdapter setSections(List<Section> sections) {
        if (sections != null) {
            this.mSections = sections;
//            notifyDataSetChanged();
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
        Log.d(TAG, "onAttachedToRecyclerView() called with: recyclerView = [" + recyclerView + "]");
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

        Context context = vh.itemView.getContext();
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
                    if (clickItem.getItemSubType() == ItemSubType.ADD_ITEM) {
                        Log.d(TAG, "onBindViewHolder: 点击添加项");
                    } else {

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
                    mOnItemClickListener.onItemLongClick(v,
                            sectionItem1, coor.section(),
                            sectionItem1.getItems().get(coor.relativePos()),
                            coor.relativePos());
                    return true;
                } else {
                    return false;
                }
            });

/*
            vh.thumbList.addOnItemTouchListener(
                    new RecyclerItemTouchListener(
                            vh.itemView.getContext(),
                            mRecyclerView, (view, position) -> {

                            }, (view, position) -> {
                                if (mOnItemClickListener != null) {
                                    ItemCoord coor = getRelativePosition(vh.getAdapterPosition());

                                    Section section1 = mSections.get(coor.section());
                                    Item item1 = section1.getItems().get(coor.relativePos());
                                    mOnItemClickListener.onItemLongClick(vh.thumbList, section1, coor.section(), item1,  coor.relativePos() );
                                }
                            }));
*/
        }

        // Check
        vh.check.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);

        // Title : Folder name
        String name = item.getName();
        if (mHighlightItemName) {
            // 高亮关键字
            int keywordStartIndex = item.getKeywordStartIndex();
            if (isValidKeyword(name, keywordStartIndex, item.getKeywordLength())) {
                vh.setHighlightName(keywordStartIndex, item.getKeywordLength(), name);
            } else {
                vh.name.setText(name);
            }
        } else {
            vh.name.setText(name);
        }

        // Title : Text Color
        vh.name.setTextColor(context.getResources()
                .getColor(item.getItemSubType() == ItemSubType.ADD_ITEM
                        ? R.color.list_item_title_text_highlight : R.color.list_item_title_text_normal));

        // Badge
        bindViewHolderBadge(vh, item);

        // Count
        if (item.getItemSubType() == ItemSubType.ADD_ITEM) {
            vh.count.setVisibility(View.GONE);
        } else {
            if (item.getCount() >= 0) {
                vh.count.setVisibility(View.VISIBLE);
                vh.count.setText(String.valueOf(item.getCount()));
            } else {
                vh.count.setVisibility(View.GONE);
            }
        }

//        vh.setSelectedCountText(item.getSelectedItemCount(), item.getCount());

        // Thumbnail image list
        updateThumbList(vh, false, new HorizontalImageListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View v, HorizontalImageListAdapter.Item ii, int position) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(null, -1, item, -1);
                }
            }

            @Override
            public void onItemLongClick(View v, HorizontalImageListAdapter.Item ii, int position) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemLongClick(v, null, -1, item, -1);
                }
            }
        }, item.getThumbList());

        // Close button
        vh.close.setVisibility(mShowCloseButton ? View.VISIBLE : View.GONE);
        vh.close.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemCloseClick(v, section, item, sectionIndex, relativePosition);
            }
        });
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition, List<Object> payloads) {
        if (payloads.isEmpty()) {
            Log.w(TAG, "onBindViewHolder: payload is empty");
            onBindViewHolder(holder, section, relativePosition, absolutePosition);
        } else {
            Object o = payloads.get(0);
            if (o instanceof Bundle) {


                Bundle payload = (Bundle) o;
                if (isHeader(absolutePosition)) {

                    SectionedImageListAdapter.SectionHeaderViewHolder viewHolder = (SectionedImageListAdapter.SectionHeaderViewHolder) holder;
                    String name = payload.getString(PAYLOAD_KEY_SECTION_NAME);
                    ((SectionedImageListAdapter.SectionHeaderViewHolder) holder).title.setText(name);

                } else if (isFooter(absolutePosition)) {

                    Log.w(TAG, "onBindViewHolder: update footer do nothing");

                } else {

                    Item item = mSections.get(section).getItems().get(relativePosition);
                    ItemViewHolder vh = (ItemViewHolder) holder;

                    // name
                    BundleUtils.<String>readBundle(payload, PAYLOAD_KEY_ITEM_NAME, vh.name::setText);

                    // thumbnail list
                    BundleUtils.<ArrayList<String>>readBundle(payload, PAYLOAD_KEY_THUMB_LIST, thumbFilePathList ->
                            updateThumbList(vh, true, new HorizontalImageListAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(View v, HorizontalImageListAdapter.Item ii, int position) {
                                    if (mOnItemClickListener != null) {
                                        mOnItemClickListener.onItemClick(null, -1, item, -1);
                                    }
                                }

                                @Override
                                public void onItemLongClick(View v, HorizontalImageListAdapter.Item ii, int position) {
                                    if (mOnItemClickListener != null) {
                                        mOnItemClickListener.onItemLongClick(v, null, -1, item, -1);
                                    }
                                }
                            }, Stream.of(thumbFilePathList).map(File::new).toList()));

                    // Item sub type
                    BundleUtils.<ItemSubType>readBundle(payload, PAYLOAD_KEY_SUB_TYPE, itemSubType -> {
                        bindViewHolderBadge(vh, item);
                    });

                    // conflict files
                    BundleUtils.<ArrayList<String>>readBundle(payload, PAYLOAD_KEY_CONFLICT_FILES, conflictFileList -> {
                        Log.w(TAG, "onBindViewHolder: do nothing for conflict file partial update : " + conflictFileList);
                    });

                    // selection
                    BundleUtils.<Boolean>readBundle(payload, PAYLOAD_KEY_SELECTION, selected -> {

                        Log.d(TAG, "onBindViewHolder: partial update selected status, " + section + ":" + relativePosition + " selected : " + selected);
                        vh.check.setVisibility(selected ? View.VISIBLE : View.GONE);
                    });

                    // Conflict file count
                    BundleUtils.<Integer>readBundle(payload, PAYLOAD_KEY_CONFLICT_FILES_COUNT, conflictFileCount -> {
                        if (conflictFileCount != -1) {
                            Log.d(TAG, String.format("onBindViewHolder: 目录更新文件冲突个数为 %d", conflictFileCount));
                            vh.setBadgeConflictCount(conflictFileCount);
                        }
                    });

                    // Count
                    BundleUtils.<Integer>readBundle(payload, PAYLOAD_KEY_COUNT, count -> {
                        if (count != -1) {
                            vh.count.setText(String.valueOf(count));
                        }
                    });

                    // Name Keyword
                    int keywordStartIndex = payload.getInt(PAYLOAD_KEY_KEYWORD_START_INDEX, -1);
                    int keywordLength = payload.getInt(PAYLOAD_KEY_KEYWORD_LENGTH, -1);
                    if (isValidKeyword(item.getName(), keywordStartIndex, keywordLength)) {
                        vh.setHighlightName(keywordStartIndex, keywordLength, item.getName());
                    }
                }
            } else {
                Log.w(TAG, "onBindViewHolder: no bundle in list : " + o);
            }
        }
    }

    private boolean isValidKeyword(String name, int keywordStartIndex, int keywordLength) {
        return keywordStartIndex >= 0 && keywordLength > 0 && keywordStartIndex < name.length();
    }

    private void bindViewHolderBadge(ItemViewHolder vh, Item item) {
        switch (item.getItemSubType()) {

            case ADD_ITEM: {
                vh.badge.hide(false);
            }
            break;
            case CONFLICT_COUNT:
                vh.setBadgeConflictCount(cn.intret.app.picgo.utils.ListUtils.sizeOf(item.getConflictFiles()));
                break;
            case NORMAL:
            case SELECTED_COUNT:
                vh.setBadgeSelectedCount(item.getSelectedCount());
                break;
            case SOURCE_DIR:
                if (mShowSourceDirBadgeWhenEmpty) {
                    vh.showSourceDirBadge(item.getSelectedCount());
                } else {
                    if (item.getSelectedCount() > 0) {
                        vh.showSourceDirBadge(item.getSelectedCount());
                    } else {
                        vh.badge.hide(false);
                    }
                }
                break;
            case NONE:
                vh.badge.hide(false);
                break;
        }

        if (item.getItemSubType() != ItemSubType.ADD_ITEM) {
            vh.count.setVisibility(View.VISIBLE);
        }
    }

    private void updateThumbList(ItemViewHolder vh, boolean forceUpdate, HorizontalImageListAdapter.OnItemClickListener onItemClickListener, List<File> thumbList) {
        if (forceUpdate) {
            vh.mAdapter = null;
        }

        if (vh.mAdapter == null) {

            vh.mAdapter = new HorizontalImageListAdapter(filesToItems(thumbList));
            vh.mAdapter.setOnClickListener(onItemClickListener);
            vh.thumbList.setLayoutManager(vh.getLayout());
            vh.thumbList.setAdapter(vh.mAdapter);
        } else {
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
                        .inflate(R.layout.item_folder, parent, false);
                return new ItemViewHolder(v);
            }
        }
    }

    @Override
    public void onViewRecycled(SectionedViewHolder holder) {

        if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).itemView.setTag(R.id.item, null);

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
        @BindView(R.id.title) TextView name;
        @BindView(R.id.count) TextView count;
        @BindView(R.id.thumb_list) RecyclerView thumbList;
        @BindView(R.id.btn_close) ImageView close;

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

        public void setBadgeSelectedCount(int selectedCount) {
            if (badge != null) {
                if (selectedCount > 0) {
                    badge.setBadgeBackgroundColor(itemView.getContext().getResources().getColor(R.color.colorAccent));
                    badge.setBadgeNumber(selectedCount);
                } else {
                    badge.hide(false);
                }

            }
        }

        void setBadgeConflictCount(int conflictCount) {
            if (badge != null) {
                if (conflictCount > 0) {
                    badge.setBadgeBackgroundColor(itemView.getContext().getResources().getColor(R.color.warning));
                    badge.setBadgeText(itemView.getContext().getResources().getString(R.string.conflict_d, conflictCount));
                } else {
                    badge.hide(false);
                }
            }
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

        public void setHighlightName(int keywordStartIndex, int keywordLength, String name) {
            SpannableString nameSpan = new SpannableString(name);
            nameSpan.setSpan(new ForegroundColorSpan(this.name.getContext().getResources().getColor(R.color.colorAccent)),
                    keywordStartIndex, keywordStartIndex + keywordLength,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.name.setText(nameSpan);
        }
    }
}

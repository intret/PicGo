package cn.intret.app.picgo.ui.adapter.brvah;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.io.File;
import java.util.List;
import java.util.Map;

import cn.intret.app.picgo.R;
import cn.intret.app.picgo.ui.adapter.HorizontalImageAdapterUils;
import cn.intret.app.picgo.ui.adapter.HorizontalImageListAdapter;
import q.rorbin.badgeview.Badge;
import q.rorbin.badgeview.QBadgeView;


public class ExpandableFolderAdapter
        extends BaseMultiItemQuickAdapter<MultiItemEntity,BaseViewHolder>
        implements BaseQuickAdapter.OnItemClickListener, BaseQuickAdapter.OnItemLongClickListener {
    private static final String TAG = ExpandableFolderAdapter.class.getSimpleName();

    public static class ItemViewHolder extends BaseViewHolder {

        HorizontalImageListAdapter adapter;
        private RecyclerView.LayoutManager layoutManager;

        RecyclerView thumbList;
        Badge badge;

        public ItemViewHolder(View view) {
            super(view);
//            ButterKnife.bind(this, view);

            // Setup view holder. You'd want some views to be optional, e.g. the
            // header/footer will have views that normal item views do or do not have.
            //itemView.setOnClickListener(this);


            this.badge = new QBadgeView(itemView.getContext()).bindTarget(thumbList);
            Resources resources = itemView.getContext().getResources();
            badge.setBadgeGravity(Gravity.START | Gravity.TOP)
                    .setExactMode(true)
                    .setBadgeBackgroundColor(resources.getColor(R.color.colorAccent))
                    .setBadgeTextColor(resources.getColor(android.R.color.white))
            ;
        }

        public RecyclerView.LayoutManager getLayout() {
            if (layoutManager == null) {
                layoutManager = new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, true);
            }
            return layoutManager;
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
            badge.setBadgeBackgroundColor(itemView.getContext().getResources().getColor(R.color.warning));
//            badge.setBadgeNumber(conflictCount);
            badge.setBadgeText(itemView.getContext().getResources().getString(R.string.conflict_d, conflictCount));
        }

        void showSourceDirBadge() {
            Resources resources = itemView.getContext().getResources();
            badge.setBadgeText(resources.getString(R.string.source_folder));
            badge.setBadgeBackgroundColor(resources.getColor(R.color.colorAccent));
        }
    }

    public interface OnInteractionListener {
        void onItemClick(FolderItem item);
    }


    public static final int TYPE_LEVEL_0 = 0;
    public static final int TYPE_LEVEL_1 = 1;

    private boolean mShowInFilterMode = false;
    private Map<File, List<File>> mConflictFiles;
    private boolean mShowConflict;
    private File mMoveFileSourceDir;

    OnInteractionListener mOnInteractionListener;

    public OnInteractionListener getOnInteractionListener() {
        return mOnInteractionListener;
    }

    public ExpandableFolderAdapter setOnInteractionListener(OnInteractionListener onInteractionListener) {
        mOnInteractionListener = onInteractionListener;
        return this;
    }

    /**
     * Same as QuickAdapter#QuickAdapter(Context,int) but with
     * some initialization data.
     *
     * @param data A new list is created out of this one to avoid mutable list
     */
    public ExpandableFolderAdapter(List<MultiItemEntity> data) {
        super(data);
        addItemType(TYPE_LEVEL_0, R.layout.item_folder_section_header);
        addItemType(TYPE_LEVEL_1, R.layout.item_folder);
        registerClickListener();
    }

    private void registerClickListener() {
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
    }

    @Override
    public void onItemClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
        Log.d(TAG, "onItemClick() called with: baseQuickAdapter = [" + baseQuickAdapter + "], view = [" + view + "], i = [" + i + "]");
    }

    @Override
    public boolean onItemLongClick(BaseQuickAdapter baseQuickAdapter, View view, int i) {
        Log.d(TAG, "onItemLongClick() called with: baseQuickAdapter = [" + baseQuickAdapter + "], view = [" + view + "], i = [" + i + "]");
        return true;
    }

    private void showSourceDirBadge(BaseViewHolder viewHolder) {
        View itemView = viewHolder.itemView;
        Badge badge = (Badge) viewHolder.itemView.getTag(R.id.badge);

        Resources resources = itemView.getContext().getResources();
        badge.setBadgeText(resources.getString(R.string.source_folder));
        badge.setBadgeBackgroundColor(resources.getColor(R.color.colorAccent));
    }

    public void setSelectedCount(BaseViewHolder vh, int selectedCount) {
        View itemView = vh.itemView;
        Badge badge = (Badge) vh.itemView.getTag(R.id.badge);


        if (selectedCount > 0) {
            badge.setBadgeBackgroundColor(itemView.getContext().getResources().getColor(R.color.colorAccent));
            badge.setBadgeNumber(selectedCount);
        } else {
            badge.hide(false);
        }
    }

    private void setConflictCount(BaseViewHolder viewHolder, int conflictCount) {
        View itemView = viewHolder.itemView;
        Badge badge = (Badge) itemView.getTag(R.id.badge);

        badge.setBadgeBackgroundColor(itemView.getContext().getResources().getColor(R.color.warning));
//            badge.setBadgeNumber(conflictCount);
        badge.setBadgeText(itemView.getContext().getResources().getString(R.string.conflict_d, conflictCount));
    }

    private void bindViewHolderBadge(BaseViewHolder vh, FolderItem item) {
        if (item.isSelectionSourceDir()) {
            showSourceDirBadge(vh);
        } else {

            if (item.getConflictFiles() != null && !item.getConflictFiles().isEmpty()) {

                setConflictCount(vh, item.getConflictFiles().size());
            } else {
                setSelectedCount(vh, item.getSelectedCount());
            }
        }
    }

    public void updateConflictFiles(@NonNull Map<File, List<File>> existedFiles) {

        mShowConflict = true;
        mConflictFiles = existedFiles;

        Log.d(TAG, "updateConflictFiles() called with: existedFiles = [" + existedFiles + "]");

        for (int i = 0, mDataSize = mData.size(); i < mDataSize; i++) {
            FolderSection entity = (FolderSection) mData.get(i);
            List<FolderItem> subItems = entity.getSubItems();
            for (int i1 = 0, subItemsSize = subItems.size(); i1 < subItemsSize; i1++) {
                FolderItem item = subItems.get(i1);

                if (existedFiles.containsKey(item.getFile())) {
                    List<File> conflictFiles = existedFiles.get(item.getFile());
                    item.setConflictFiles(conflictFiles);

                    // TODO update item badge

//                    if (getRecyclerView() != null) {
//                        RecyclerView.ViewHolder vh = getRecyclerView()
//                                .findViewHolderForAdapterPosition(getAbsolutePosition(si, ii));
//                        if (vh != null && vh instanceof ItemViewHolder) {
//                            Log.d(TAG, "updateConflictFiles: update conflict file count");
//
//                            bindViewHolderBadge((ItemViewHolder) vh, item);
//                        }
//                    }
                }
            }
        }
    }

    private void updateThumbList(BaseViewHolder vh, FolderItem item, boolean forceUpdate) {
        RecyclerView rv = vh.getView(R.id.thumb_list);
        if (forceUpdate) {
            item.adapter = null;
        }

        if (item.adapter == null) {

            Log.d(TAG, "updateThumbList: create thumbnail adapte for " + item.getName());
            item.adapter = new HorizontalImageListAdapter(HorizontalImageAdapterUils.filesToItems(item.getThumbList()));

            rv.setLayoutManager(item.getLayout(vh.itemView.getContext()));
            rv.setAdapter(item.adapter);
        } else {
            rv.setClickable(false);
            RecyclerView.LayoutManager layout = item.getLayout(vh.itemView.getContext());
            if (!layout.isAttachedToWindow()) {
                rv.setLayoutManager(layout);
                rv.swapAdapter(item.adapter, true);
            } else {
                Log.e(TAG, "updateThumbList: layout manager is already attached to window ：" + item.getName() );
            }
        }
    }

    public void setMoveFileSourceDir(File moveFileSourceDir) {

        for (int si = 0, mSectionsSize = mData.size(); si < mSectionsSize; si++) {
            FolderSection section = (FolderSection) mData.get(si);
            List<FolderItem> items = section.getSubItems();
            for (int ii = 0, itemsSize = items.size(); ii < itemsSize; ii++) {
                FolderItem item = items.get(ii);
                if (moveFileSourceDir == null) {
                    if (item.isSelectionSourceDir()) {
                        item.setSelectionSourceDir(false);

//                        updateViewHolder(si, ii, item, (vh, it) -> {
//                            vh.badge.hide(true);
//                        });
                    }
                } else {
                    if (item.getFile().equals(moveFileSourceDir)) {
                        item.setSelectionSourceDir(true);

//                        updateViewHolder(si, ii, item, (vh, it) -> {
//                            vh.showSourceDirBadge();
//                        });
                    }
                }
            }
        }


        mMoveFileSourceDir = moveFileSourceDir;
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        View view = holder.getView(R.id.thumb_list);
        if (view != null) {
            holder.itemView.setTag(R.id.item, null);
            holder.itemView.setTag(R.id.badge, null);

            ((RecyclerView) view).setLayoutManager(null);
            ((RecyclerView) view).setAdapter(null);
        }
        super.onViewRecycled(holder);
    }

    @Override
    protected void convert(final BaseViewHolder holder, final MultiItemEntity item) {
        switch (holder.getItemViewType()) {
            case TYPE_LEVEL_0: {
                final FolderSection folderSection = (FolderSection) item;
                holder.setText(R.id.title, folderSection.title);
                holder.setImageResource(R.id.arrow, folderSection.isExpanded()
                        ? R.drawable.ic_keyboard_arrow_down_black_24px
                        : R.drawable.ic_keyboard_arrow_right_black_24px);

                holder.itemView.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    Log.d(TAG, "Level 0 item pos: " + pos);
                    if (folderSection.isExpanded()) {
                        collapse(pos);
                    } else {
                        expand(pos);
                    }
                });
            }
            break;
            case TYPE_LEVEL_1: {

                final FolderItem folderItem = (FolderItem) item;
                holder.setVisible(R.id.check, folderItem.isSelected());

                holder.itemView.setOnClickListener(v -> {
                    if (mOnInteractionListener != null) {
                        mOnInteractionListener.onItemClick(folderItem);
                    }
                });

                // Folder name
                holder.setText(R.id.title, folderItem.title);

                // Count
                holder.setText(R.id.count, String.valueOf(folderItem.getCount()));

                // ThumbList
                updateThumbList(holder, folderItem, false);

                RecyclerView rv = holder.getView(R.id.thumb_list);
                Object tag = holder.itemView.getTag(R.id.badge);
                if (tag == null) {
                    Badge badge = new QBadgeView(holder.itemView.getContext()).bindTarget(rv);
                    Resources resources = holder.itemView.getContext().getResources();
                    badge.setBadgeGravity(Gravity.START | Gravity.TOP)
                            .setExactMode(true)
                            .setBadgeBackgroundColor(resources.getColor(R.color.colorAccent))
                            .setBadgeTextColor(resources.getColor(android.R.color.white))
                    ;
                    holder.itemView.setTag(R.id.badge, badge);
                }

                // Badge
                bindViewHolderBadge(holder,folderItem);
            }

            break;
        }
    }
}

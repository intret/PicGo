package cn.intret.app.picgo.ui.adapter.fast;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v13.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.mikepenz.fastadapter.IClickable;
import com.mikepenz.fastadapter.IDraggable;
import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.ISubItem;
import com.mikepenz.fastadapter.commons.items.AbstractExpandableItem;
import com.mikepenz.fastadapter.commons.utils.FastAdapterUIUtils;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.ui.adapter.HorizontalImageListAdapter;
import q.rorbin.badgeview.Badge;
import q.rorbin.badgeview.QBadgeView;

public class FolderItem<Parent extends IItem & IExpandable & ISubItem & IClickable>
        extends AbstractExpandableItem<Parent, FolderItem.ViewHolder, FolderItem<Parent>>
        implements IDraggable<FolderItem, IItem> {

    boolean mIsSelected;
    boolean mIsSelectionSourceDir = false;
    File mFile;
    int mCount;
    int mSelectedCount = -1;
    List<File> mConflictFiles;

    List<File> mThumbList;
    private int mKeywordLength;
    private int mKeywordStartIndex;
    String mName;

    private boolean mIsDraggable = true;


    @Override
    public boolean isSelected() {
        return mIsSelected;
    }

    public FolderItem setSelected(boolean selected) {
        mIsSelected = selected;
        return this;
    }

    public boolean isSelectionSourceDir() {
        return mIsSelectionSourceDir;
    }

    public FolderItem setSelectionSourceDir(boolean selectionSourceDir) {
        mIsSelectionSourceDir = selectionSourceDir;
        return this;
    }

    public File getFile() {
        return mFile;
    }

    public FolderItem setFile(File file) {
        mFile = file;
        return this;
    }

    public int getCount() {
        return mCount;
    }

    public FolderItem setCount(int count) {
        mCount = count;
        return this;
    }

    public int getSelectedCount() {
        return mSelectedCount;
    }

    public FolderItem setSelectedCount(int selectedCount) {
        mSelectedCount = selectedCount;
        return this;
    }

    public List<File> getConflictFiles() {
        return mConflictFiles;
    }

    public FolderItem setConflictFiles(List<File> conflictFiles) {
        mConflictFiles = conflictFiles;
        return this;
    }

    public List<File> getThumbList() {
        return mThumbList;
    }

    public FolderItem setThumbList(List<File> thumbList) {
        mThumbList = thumbList;
        return this;
    }

    public int getKeywordLength() {
        return mKeywordLength;
    }

    public FolderItem setKeywordLength(int keywordLength) {
        mKeywordLength = keywordLength;
        return this;
    }

    public int getKeywordStartIndex() {
        return mKeywordStartIndex;
    }

    public FolderItem setKeywordStartIndex(int keywordStartIndex) {
        mKeywordStartIndex = keywordStartIndex;
        return this;
    }

    public String getName() {
        return mName;
    }

    public FolderItem setName(String name) {
        mName = name;
        return this;
    }

    public FolderItem setDraggable(boolean draggable) {
        mIsDraggable = draggable;
        return this;
    }

    @Override
    public boolean isDraggable() {
        return mIsDraggable;
    }

    @Override
    public FolderItem withIsDraggable(boolean draggable) {
        this.mIsDraggable = draggable;
        return this;
    }

    /**
     * defines the type defining this item. must be unique. preferably an id
     *
     * @return the type
     */
    @Override
    public int getType() {
        return R.id.item;
    }

    /**
     * defines the layout which will be used for this item in the list
     *
     * @return the layout for this item
     */
    @Override
    public int getLayoutRes() {
        return R.layout.item_folder;
    }

    /**
     * binds the data of this item onto the viewHolder
     *
     * @param viewHolder the viewHolder of this item
     */
    @Override
    public void bindView(ViewHolder viewHolder, List<Object> payloads) {
        super.bindView(viewHolder, payloads);

        //get the context
        Context ctx = viewHolder.itemView.getContext();
        viewHolder.name.setText(this.getName());
        viewHolder.count.setText(String.valueOf(getCount()));

        int normalColor = ctx.getResources().getColor(R.color.white);
        int pressedColor = ctx.getResources().getColor(R.color.grey);

        StateListDrawable selectableBackground = FastAdapterUIUtils.getSelectableBackground(ctx, ctx.getResources().getColor(R.color.gray_95), true);

//        Drawable rippleDrawable = FastAdapterUIUtils.getRippleDrawable(normalColor, pressedColor, 0);

        ViewCompat.setBackground(viewHolder.itemView, selectableBackground);

        viewHolder.itemView.requestApplyInsets();

        viewHolder.updateThumbList(getThumbList());
    }

    @Override
    public void unbindView(ViewHolder holder) {
        super.unbindView(holder);
        holder.name.setText(null);
        holder.count.setText(null);
    }

    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    /**
     * our ViewHolder
     */
    protected static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.check) ImageView check;
        @BindView(R.id.title) TextView name;
        @BindView(R.id.count) TextView count;
        @BindView(R.id.thumb_list) RecyclerView thumbList;
        Badge badge;

        HorizontalImageListAdapter mAdapter;
        private LinearLayoutManager mLinearLayoutManager;

        ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
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

        public void updateThumbList(List<File> thumbFiles) {
            if (thumbFiles== null) {
                return;
            }

            if (mAdapter != null) {
                thumbList.setLayoutManager(getLayout());
                thumbList.swapAdapter(mAdapter, false);
            } else {
                mAdapter = new HorizontalImageListAdapter(
                        Stream.of(thumbFiles)
                                .map(f -> new HorizontalImageListAdapter.Item().setFile(f))
                                .toList());

                thumbList.setLayoutManager(getLayout());
                thumbList.setAdapter(mAdapter);
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
            badge.setBadgeBackgroundColor(itemView.getContext().getResources().getColor(R.color.warning));
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
            if (selectedCount == -1 || selectedCount == 0) {
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


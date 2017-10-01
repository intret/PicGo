package cn.intret.app.picgo.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.github.siyamed.shapeimageview.RoundedImageView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.image.GroupMode;

/**
 * 分段图片列表
 */
public class SectionedImageListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    private static final String TAG = SectionedImageListAdapter.class.getSimpleName();
    private List<Section> mSectionList = new LinkedList<>();

    private File directory;
    private GroupMode mGroupMode;

    public File getDirectory() {
        return directory;
    }

    public SectionedImageListAdapter setDirectory(File directory) {
        this.directory = directory;
        return this;
    }

    public void onClickItem(ItemCoord relativePosition) {
        Section section = mSectionList.get(relativePosition.section());

        Item item = section.getItems().get(relativePosition.relativePos());
        SectionItemViewHolder viewHolder = item.getViewHolder();
        if (viewHolder != null) {
            item.setSelected(!item.isSelected());
            viewHolder.checkbox.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);
        }
    }

    public void setGroupMode(GroupMode groupMode) {
        mGroupMode = groupMode;
    }

    public GroupMode getGroupMode() {
        return mGroupMode;
    }

    public static class Section {
        Date mStartDate;
        Date mEndDate;
        String description;
        List<Item> mItems;

        public Date getEndDate() {
            return mEndDate;
        }

        public Section setEndDate(Date endDate) {
            mEndDate = endDate;
            return this;
        }

        public Date getStartDate() {
            return mStartDate;
        }

        public Section setStartDate(Date startDate) {
            mStartDate = startDate;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Section setDescription(String description) {
            this.description = description;
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
        File mFile;
        Date mDate;
        boolean mIsSelected;
        SectionItemViewHolder mViewHolder;

        public SectionItemViewHolder getViewHolder() {
            return mViewHolder;
        }

        public Item setViewHolder(SectionItemViewHolder viewHolder) {
            mViewHolder = viewHolder;
            return this;
        }

        public boolean isSelected() {
            return mIsSelected;
        }

        public Item setSelected(boolean selected) {
            mIsSelected = selected;
            return this;
        }

        public Date getDate() {
            return mDate;
        }

        public Item setDate(Date date) {
            mDate = date;
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

    public SectionedImageListAdapter(List<Section> sectionList) {
        if (sectionList != null) {
            mSectionList = sectionList;
        } else {
            mSectionList = new LinkedList<>();
        }
    }

    @Override
    public int getSectionCount() {
        if (mSectionList == null || mSectionList.isEmpty()) {
            return -1;
        }
        return mSectionList.size();
    }

    @Override
    public int getItemCount(int section) {
        if (mSectionList.isEmpty()) {
            return 0;
        }
        Section sec = mSectionList.get(section);
        int size = sec.getItems().size();

        return size;
    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder holder, int section, boolean expanded) {
        SectionHeaderViewHolder vh = (SectionHeaderViewHolder) holder;

        Section s = mSectionList.get(section);
        vh.title.setText(s.getDescription());
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {

    }

    @Override
    public void onViewRecycled(SectionedViewHolder holder) {
        if (holder instanceof SectionHeaderViewHolder) {
            Object tag = ((SectionHeaderViewHolder) holder).itemView.getTag(R.id.item);
            if (tag instanceof Item) {
                ((Item) tag).setViewHolder(null);
            }
        }
        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition) {

        SectionItemViewHolder vh = (SectionItemViewHolder) holder;
        Section s = mSectionList.get(section);
        Item item = s.getItems().get(relativePosition);
        item.setViewHolder(vh);

        vh.itemView.setTag(R.id.item, item);

        // Checkbox selectable status
        vh.checkbox.setVisibility(item.isSelected() ? View.VISIBLE : View.GONE);

        // File type
        // Show file type
        String extension = FilenameUtils.getExtension(item.getFile().getAbsolutePath());
        if (extension != null) {
            switch (extension.toLowerCase()) {
                case "mp4":
                case "avi":
                case "mov":
                case "mpg":
                case "mpeg":
                case "rmvb": {
                    vh.fileType.setVisibility(View.VISIBLE);
                    vh.fileType.setImageResource(R.drawable.ic_videocam);
                }
                break;
                case "gif": {
                    vh.fileType.setImageResource(R.drawable.ic_gif);
                    vh.fileType.setVisibility(View.VISIBLE);
                }
                break;
                default:
                    vh.fileType.setVisibility(View.GONE);
                    break;
            }
        } else {
            vh.fileType.setVisibility(View.GONE);
        }

        // Load Image
        Context context = vh.itemView.getContext();
        Glide.with(context)
                .asBitmap()
                .load(item.getFile())
                .apply(RequestOptions.fitCenterTransform())
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(vh.image);
    }

    @Override
    public int getItemViewType(int section, int relativePosition, int absolutePosition) {
        return super.getItemViewType(section, relativePosition, absolutePosition);
    }

    @Override
    public SectionedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                View view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.image_list_section_header, parent, false);
                return new SectionHeaderViewHolder(view);
            case VIEW_TYPE_FOOTER:
                return null;
            default: {
                View itemView = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.image_list_item, parent, false
                );
                return new SectionItemViewHolder(itemView);
            }
        }
    }

    class SectionHeaderViewHolder extends SectionedViewHolder {

        @BindView(R.id.title) TextView title;

        SectionHeaderViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }

    class SectionItemViewHolder extends SectionedViewHolder {

        @BindView(R.id.image) RoundedImageView image;
        @BindView(R.id.checkbox) ImageView checkbox;
        @BindView(R.id.file_type) ImageView fileType;

        SectionItemViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}

package cn.intret.app.picgo.ui.adapter.brvah;

import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.entity.MultiItemEntity;

import org.apache.commons.collections4.ListUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.ui.adapter.SectionedFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.ThumbnailListAdapter;

/**
 * 抽屉菜单列表
 */
public class FolderListAdapter
        extends BaseMultiItemQuickAdapter<FolderListAdapter.Item, FolderListAdapter.ViewHolder> {

    public static class Item implements MultiItemEntity {
        String name;
        long count = 0;
        boolean isSelected = false;

        ViewHolder viewHolder;
        private File mDirectory;

        public ViewHolder getViewHolder() {
            return viewHolder;
        }

        public Item setSelected(boolean selected) {
            isSelected = selected;
            return this;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public Item setViewHolder(ViewHolder viewHolder) {
            this.viewHolder = viewHolder;
            return this;
        }

        public long getCount() {
            return count;
        }

        public Item setCount(long count) {
            this.count = count;
            return this;
        }

        public String getName() {
            return name;
        }

        public Item setName(String name) {
            this.name = name;
            return this;
        }

        public Item setDirectory(File directory) {
            mDirectory = directory;
            return this;
        }

        public File getDirectory() {
            return mDirectory;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "name='" + name + '\'' +
                    ", count=" + count +
                    ", mDirectory=" + mDirectory +
                    '}';
        }

        List<File> mThumbList;

        public List<File> getThumbList() {
            return mThumbList;
        }

        public Item setThumbList(List<File> thumbList) {
            mThumbList = thumbList;
            return this;
        }

        @Override
        public int getItemType() {
            return 0;
        }
    }

    List<Item> mItems = new LinkedList<>();

    public List<Item> getItems() {
        return mItems;
    }


    public void diffUpdateItems(SectionedFolderListAdapter adapter) {
        List<SectionedFolderListAdapter.Section> sections = adapter.getSections();

        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mItems.size();
            }

            @Override
            public int getNewListSize() {
                return sections.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Item item = mItems.get(oldItemPosition);
                return false;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return false;
            }
        });
    }

    public interface OnItemEventListener {
        void onItemClick(Item item);
    }

    OnItemEventListener mOnItemEventListener;

    public FolderListAdapter setOnItemEventListener(OnItemEventListener onItemEventListener) {
        mOnItemEventListener = onItemEventListener;
        return this;
    }


    public FolderListAdapter(List<Item> items) {
        super(items);
        if (items != null) {
            mItems = items;
        } else {
            mItems = new ArrayList<>();
        }
    }

    public void setItemSelected(int position) {
        if (position < 0 || position >= mItems.size()) {
            return;
        }

        // Current selected
        int i = ListUtils.indexOf(mItems, Item::isSelected);
        if (i == position) {

        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        Object tag = holder.itemView.getTag(R.id.item);
        if (tag != null) {

            Item item = (Item) tag;
            ViewHolder viewHolder = item.getViewHolder();
            if (viewHolder != null) {
                viewHolder.itemView.setTag(R.id.item, null);
            }


            holder.name.setText(null);
            holder.count.setText(null);
        }


        super.onViewRecycled(holder);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.folder_list_item, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Item item = mItems.get(position);
        holder.itemView.setTag(R.id.item, item);

        SparseArray<ThumbnailListAdapter.Item> items = new SparseArray<>();
        item.getThumbList();

        holder.name.setText(item.getName());
        holder.count.setText(String.valueOf(item.getCount()));
    }

    @Override
    protected void convert(ViewHolder holder, Item item) {

        holder.setText(R.id.name, item.getName());
        holder.setText(R.id.count, String.valueOf(item.getCount()));
    }

    class ViewHolder extends BaseViewHolder {

        @BindView(R.id.check) ImageView check;
        @BindView(R.id.name) TextView name;
        @BindView(R.id.count) TextView count;
        @BindView(R.id.thumb_list) RecyclerView thumbList;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }
    }
}

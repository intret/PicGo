package cn.intret.app.picgo.ui.adapter.fast;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.ISubItem;
import com.mikepenz.fastadapter.commons.items.AbstractExpandableItem;

import java.io.File;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;

public class SectionItem<Parent extends IItem & IExpandable, SubItem extends IItem & ISubItem>
        extends AbstractExpandableItem<SectionItem<Parent, SubItem>, SectionItem.ViewHolder, SubItem> {

    public String header;
    private File mFile;

    public SectionItem<Parent, SubItem> setHeader(String header) {
        this.header = header;
        return this;
    }

    private FastAdapter.OnClickListener<SectionItem> mOnClickListener;

    public SectionItem<Parent, SubItem> withHeader(String header) {
        this.header = header;
        return this;
    }

    public FastAdapter.OnClickListener<SectionItem> getOnClickListener() {
        return mOnClickListener;
    }

    public SectionItem<Parent, SubItem> withOnClickListener(FastAdapter.OnClickListener<SectionItem> mOnClickListener) {
        this.mOnClickListener = mOnClickListener;
        return this;
    }

    //we define a clickListener in here so we can directly animate
    final private FastAdapter.OnClickListener<SectionItem<Parent, SubItem>> onClickListener = new FastAdapter.OnClickListener<SectionItem<Parent, SubItem>>() {
        @Override
        public boolean onClick(View v, IAdapter adapter, SectionItem item, int position) {
            if (item.getSubItems() != null) {
                if (!item.isExpanded()) {
                    ViewCompat.animate(v.findViewById(R.id.arrow)).rotation(180).start();
                } else {
                    ViewCompat.animate(v.findViewById(R.id.arrow)).rotation(0).start();
                }
                return mOnClickListener == null || mOnClickListener.onClick(v, adapter, item, position);
            }
            return mOnClickListener != null && mOnClickListener.onClick(v, adapter, item, position);
        }
    };

    /**
     * we overwrite the item specific click listener so we can automatically animate within the item
     *
     * @return
     */
    @Override
    public FastAdapter.OnClickListener<SectionItem<Parent, SubItem>> getOnItemClickListener() {
        return onClickListener;
    }

    @Override
    public boolean isSelectable() {
        //this might not be true for your application
        return getSubItems() == null;
    }



    /**
     * defines the type defining this item. must be unique. preferably an id
     *
     * @return the type
     */
    @Override
    public int getType() {
        return R.id.parent_folder;
    }

    /**
     * defines the layout which will be used for this item in the list
     *
     * @return the layout for this item
     */
    @Override
    public int getLayoutRes() {
        return R.layout.item_folder_section_header;
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

        //set the background for the item
         viewHolder.name.setText(this.header);
        if (isExpanded()) {
            ViewCompat.setRotation(viewHolder.arrow, 0);
        } else {
            ViewCompat.setRotation(viewHolder.arrow, 180);
        }
    }

    @Override
    public void unbindView(ViewHolder holder) {
        super.unbindView(holder);
        holder.name.setText(null);

        //make sure all animations are stopped
        holder.arrow.clearAnimation();
    }

    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    public void setFile(File file) {
        mFile = file;
    }

    public File getFile() {
        return mFile;
    }

    /**
     * our ViewHolder
     */
    protected static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.title) public TextView name;
        @BindView(R.id.arrow) public ImageView arrow;


        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

        }
    }
}


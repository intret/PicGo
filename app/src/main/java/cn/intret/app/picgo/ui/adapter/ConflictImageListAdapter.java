package cn.intret.app.picgo.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;


public class ConflictImageListAdapter extends RecyclerView.Adapter<ConflictImageListAdapter.ViewHolder> {

    private RecyclerView mRecyclerView;



    public enum ResolveResult {
        RESOLVE_RESULT_SOURCE,
        RESOLVE_RESULT_TARGET,
        RESOLVE_RESULT_BOTH,
        RESOLVE_RESULT_NONE
    }

    public static class Item {
        File mSourceFile;
        File mTargetFile;
        ResolveResult mResolveResult = ResolveResult.RESOLVE_RESULT_NONE;

        public File getSourceFile() {
            return mSourceFile;
        }

        public Item setSourceFile(File sourceFile) {
            mSourceFile = sourceFile;
            return this;
        }

        public File getTargetFile() {
            return mTargetFile;
        }

        public Item setTargetFile(File targetFile) {
            mTargetFile = targetFile;
            return this;
        }

        public ResolveResult getResolveResult() {
            return mResolveResult;
        }

        public Item setResolveResult(ResolveResult resolveResult) {
            mResolveResult = resolveResult;
            return this;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "mSourceFile=" + mSourceFile +
                    ", mTargetFile=" + mTargetFile +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Item item = (Item) o;

            if (mSourceFile != null ? !mSourceFile.equals(item.mSourceFile) : item.mSourceFile != null)
                return false;
            return mTargetFile != null ? mTargetFile.equals(item.mTargetFile) : item.mTargetFile == null;

        }

        @Override
        public int hashCode() {
            int result = mSourceFile != null ? mSourceFile.hashCode() : 0;
            result = 31 * result + (mTargetFile != null ? mTargetFile.hashCode() : 0);
            return result;
        }
    }

    List<Item> mItems = new LinkedList<>();

    /*
     * Constructor
     */
    public ConflictImageListAdapter(List<Item> items) {
        if (items != null) {
            mItems = items;
        }
    }

    /*
     * Selection
     */

    public List<Item> getItems() {
        return mItems;
    }

    public List<Item> getKeepSourceItems() {
        return Stream.of(mItems).filter(item -> item.getResolveResult() == ResolveResult.RESOLVE_RESULT_SOURCE).toList();
    }

    public List<Item> getKeepBothItems() {
        return Stream.of(mItems).filter(item -> item.getResolveResult() == ResolveResult.RESOLVE_RESULT_BOTH).toList();
    }

    public List<Item> getKeepTargetItems() {
        return Stream.of(mItems).filter(item -> item.getResolveResult() == ResolveResult.RESOLVE_RESULT_TARGET).toList();
    }

    public boolean hasUnsolvedItem() {
        return getUnsolvedItems().isEmpty();
    }

    public List<Item> getUnsolvedItems() {
        return Stream.of(mItems).filter(item -> item.getResolveResult() == ResolveResult.RESOLVE_RESULT_NONE).toList();
    }

    public void selectAllSourceItems() {

        for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
            Item item = mItems.get(i);
            if (item.getResolveResult() == ResolveResult.RESOLVE_RESULT_SOURCE) {
                continue;
            }

            // Update item checked data
            item.setResolveResult(ResolveResult.RESOLVE_RESULT_SOURCE);

            // Update item checked status
            if (mRecyclerView != null) {
                RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(i);
                if (vh != null && vh instanceof ViewHolder) {
                    ((ViewHolder) vh).selectSource();
                }
            }
        }
    }

    public void selectAllTargetItems() {
        for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
            Item item = mItems.get(i);
            if (item.getResolveResult() == ResolveResult.RESOLVE_RESULT_TARGET) {
                continue;
            }

            // Update item checked data
            item.setResolveResult(ResolveResult.RESOLVE_RESULT_TARGET);

            // Update item checked status
            if (mRecyclerView != null) {
                RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(i);
                if (vh != null && vh instanceof ViewHolder) {
                    ((ViewHolder) vh).selectTarget();
                }
            }
        }
    }

    public void selectBothItems() {
        for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
            Item item = mItems.get(i);
            if (item.getResolveResult() == ResolveResult.RESOLVE_RESULT_TARGET) {
                continue;
            }

            // Update item checked data
            item.setResolveResult(ResolveResult.RESOLVE_RESULT_BOTH);

            // Update item checked status
            if (mRecyclerView != null) {
                RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(i);
                if (vh != null && vh instanceof ViewHolder) {
                    ((ViewHolder) vh).selectBoth();
                }
            }
        }
    }

    /*
     * RecyclerView
     */

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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.conflict_image_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Item item = mItems.get(position);

        // Update resolve result
        ResolveResult resolveResult = item.getResolveResult();
        updateViewHolderResultResult(holder, resolveResult);

        // Load images
        Glide.with(holder.itemView.getContext())
                .load(item.getSourceFile())
                .into(holder.sourceImage);

        Glide.with(holder.itemView.getContext())
                .load(item.getTargetFile())
                .into(holder.targetImage);


        // Image click event
        holder.sourceImage.setTag(R.id.item, item);
        holder.sourceImage.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Item clickItem = mItems.get(adapterPosition);
                clickItem.setResolveResult(ResolveResult.RESOLVE_RESULT_SOURCE);
                updateViewHolderResultResult(holder, clickItem.getResolveResult());
            }
        });

        holder.targetImage.setTag(R.id.item, item);
        holder.targetImage.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Item clickedItem = mItems.get(adapterPosition);

                clickedItem.setResolveResult(ResolveResult.RESOLVE_RESULT_TARGET);
                updateViewHolderResultResult(holder, clickedItem.getResolveResult());
            }
        });
    }

    private void updatePositionViewHolderResolveResult(int position, ResolveResult resolveResult) {
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(position);
        if (vh != null && vh instanceof ViewHolder) {
            updateViewHolderResultResult(((ViewHolder) vh), resolveResult);
        }
    }

    private void updateViewHolderResultResult(ViewHolder holder, ResolveResult resolveResult) {
        switch (resolveResult) {

            case RESOLVE_RESULT_SOURCE:
                holder.selectSource();
                break;
            case RESOLVE_RESULT_TARGET:
                holder.selectTarget();
                break;
            case RESOLVE_RESULT_BOTH:
                holder.selectBoth();
                break;
            case RESOLVE_RESULT_NONE:
                holder.selectNone();
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    /*
     * Inner class
     */
    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.target_image) ImageView targetImage;
        @BindView(R.id.target_check) CheckBox targetCheckBox;

        @BindView(R.id.source_image) ImageView sourceImage;
        @BindView(R.id.source_check) CheckBox sourceCheckBox;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        void selectSource() {
            sourceCheckBox.setChecked(true);
            sourceCheckBox.setVisibility(View.VISIBLE);

            targetCheckBox.setVisibility(View.GONE);
        }

        void selectTarget() {
            targetCheckBox.setChecked(true);
            targetCheckBox.setVisibility(View.VISIBLE);

            sourceCheckBox.setVisibility(View.GONE);
        }

        void selectNone() {
            targetCheckBox.setVisibility(View.GONE);
            sourceCheckBox.setVisibility(View.GONE);
        }

        public void selectBoth() {
            targetCheckBox.setVisibility(View.VISIBLE);
            sourceCheckBox.setVisibility(View.VISIBLE);
        }
    }
}

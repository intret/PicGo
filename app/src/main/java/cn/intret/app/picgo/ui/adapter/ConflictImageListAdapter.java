package cn.intret.app.picgo.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.image.ResolveResult;
import cn.intret.app.picgo.utils.FileSizeUtils;
import cn.intret.app.picgo.utils.MediaUtils;
import cn.intret.app.picgo.utils.Param;


public class ConflictImageListAdapter extends RecyclerView.Adapter<ConflictImageListAdapter.ViewHolder> {


    public interface OnInteractionListener {
        void onResolveCountChange(int totalCount, int keepTargetCount, int keepSourceCount, int deleteTargetCount, int deleteSourceCount);
    }

    public static class Item {
        File mSourceFile;
        Size mTargetImageResolution;
        long mTargetFileSize;

        File mTargetFile;
        Size mSourceImageResolution;
        long mSourceFileSize;

        ResolveResult mResolveResult = ResolveResult.NONE;


        public Size getTargetImageResolution() {
            return mTargetImageResolution;
        }

        public Item setTargetImageResolution(Size targetImageResolution) {
            mTargetImageResolution = targetImageResolution;
            return this;
        }

        public long getTargetFileSize() {
            return mTargetFileSize;
        }

        public Item setTargetFileSize(long targetFileSize) {
            mTargetFileSize = targetFileSize;
            return this;
        }

        public Size getSourceImageResolution() {
            return mSourceImageResolution;
        }

        public Item setSourceImageResolution(Size sourceImageResolution) {
            mSourceImageResolution = sourceImageResolution;
            return this;
        }

        public long getSourceFileSize() {
            return mSourceFileSize;
        }

        public Item setSourceFileSize(long sourceFileSize) {
            mSourceFileSize = sourceFileSize;
            return this;
        }

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

    private RecyclerView mRecyclerView;
    OnInteractionListener mOnInteractionListener;

    /*
     * Getter an setter
     */

    public ConflictImageListAdapter setOnInteractionListener(OnInteractionListener onInteractionListener) {
        mOnInteractionListener = onInteractionListener;
        return this;
    }

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
        return Stream.of(mItems).filter(item -> item.getResolveResult() == ResolveResult.KEEP_SOURCE).toList();
    }

    public List<Item> getKeepBothItems() {
        return Stream.of(mItems).filter(item -> item.getResolveResult() == ResolveResult.KEEP_BOTH).toList();
    }

    public List<Item> getKeepTargetItems() {
        return Stream.of(mItems).filter(item -> item.getResolveResult() == ResolveResult.KEEP_TARGET).toList();
    }

    public boolean hasUnsolvedItem() {
        return getUnsolvedItems().isEmpty();
    }

    public List<Item> getUnsolvedItems() {
        return Stream.of(mItems).filter(item -> item.getResolveResult() == ResolveResult.NONE).toList();
    }

    public void unselectAllSourceItem() {
        for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
            Item item = mItems.get(i);
            switch (item.getResolveResult()) {
                case KEEP_SOURCE:
                    item.setResolveResult(ResolveResult.NONE);
                    continue;
                case KEEP_TARGET:
                    break;
                case KEEP_BOTH:
                    item.setResolveResult(ResolveResult.KEEP_TARGET);
                    continue;
                case NONE:
                    break;
            }
        }

        notifyResolveCountChange();

        notifyDataSetChanged();
    }

    public void unselectAllTargetItem() {
        for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
            Item item = mItems.get(i);
            switch (item.getResolveResult()) {
                case KEEP_SOURCE:
                    continue;
                case KEEP_TARGET:
                    item.setResolveResult(ResolveResult.NONE);
                    break;
                case KEEP_BOTH:
                    item.setResolveResult(ResolveResult.KEEP_SOURCE);
                    continue;
                case NONE:
                    break;
            }
        }

        notifyResolveCountChange();

        notifyDataSetChanged();
    }

    public void selectAllSourceItems() {

        for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
            Item item = mItems.get(i);
            if (item.getResolveResult() == ResolveResult.KEEP_SOURCE) {
                continue;
            }

            // Update item checked data
            item.setResolveResult(ResolveResult.KEEP_SOURCE);
        }

        notifyResolveCountChange();

        notifyDataSetChanged();
    }

    public void selectAllTargetItems() {
        for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
            Item item = mItems.get(i);
            if (item.getResolveResult() == ResolveResult.KEEP_TARGET) {
                continue;
            }

            // Update item checked data
            item.setResolveResult(ResolveResult.KEEP_TARGET);
        }

        notifyResolveCountChange();

        notifyDataSetChanged();
    }

    public void selectBothItems() {
        for (int i = 0, mItemsSize = mItems.size(); i < mItemsSize; i++) {
            Item item = mItems.get(i);
            if (item.getResolveResult() == ResolveResult.KEEP_TARGET) {
                continue;
            }

            // Update item checked data
            item.setResolveResult(ResolveResult.KEEP_BOTH);
        }

        notifyDataSetChanged();
    }

    /*
     * Remove item
     */

    public void removeItemSource(File sourceFile) {
        int i = org.apache.commons.collections4.ListUtils.indexOf(mItems, item -> item.getSourceFile().equals(sourceFile));
        if (i != -1) {
            mItems.remove(i);
            notifyItemRemoved(i);
        }

        notifyResolveCountChange();
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
                .inflate(R.layout.list_item_conflict, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Item item = mItems.get(position);

        Context context = holder.itemView.getContext();

        // Update resolve result
        ResolveResult resolveResult = item.getResolveResult();
        updateViewHolderResultResult(holder, resolveResult);

        // Load images
        Glide.with(context)
                .load(item.getSourceFile())
                .apply(RequestOptions.fitCenterTransform())
                .into(holder.sourceImage);

        Glide.with(context)
                .load(item.getTargetFile())
                .apply(RequestOptions.fitCenterTransform())
                .into(holder.targetImage);

        // Target image information
        holder.targetImageResolution.setText(MediaUtils.getResolutionString(context, item.getTargetImageResolution()));
        holder.targetFileSize.setText(FileSizeUtils.formatFileSize(item.getTargetFileSize(), false));

        holder.sourceImageResolution.setText(MediaUtils.getResolutionString(context, item.getSourceImageResolution()));
        holder.sourceFileSize.setText(FileSizeUtils.formatFileSize(item.getSourceFileSize(), false));

        // Image click event
        holder.sourceImage.setTag(R.id.item, item);
        holder.sourceImage.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Item clickItem = mItems.get(adapterPosition);
                clickItem.setResolveResult(ResolveResult.KEEP_SOURCE);

                updateViewHolderResultResult(holder, clickItem.getResolveResult());

                notifyResolveCountChange();
            }
        });

        holder.targetImage.setTag(R.id.item, item);
        holder.targetImage.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                Item clickedItem = mItems.get(adapterPosition);
                clickedItem.setResolveResult(ResolveResult.KEEP_TARGET);

                updateViewHolderResultResult(holder, clickedItem.getResolveResult());

                notifyResolveCountChange();
            }
        });
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        holder.resolveResult.setImageDrawable(null);
        holder.itemView.setSelected(false);
        holder.sourceImage.setSelected(false);
        holder.targetImage.setSelected(false);
        holder.sourceCheckBox.setSelected(false);
        holder.targetCheckBox.setSelected(false);
        holder.targetCheckBox.setVisibility(View.GONE);

        super.onViewRecycled(holder);
    }

    public Param.Five<Integer, Integer, Integer, Integer, Integer> getResolveResultCount() {

        int keepTargetCount = 0;
        int keepSourceCount = 0;
        int deleteTargetCount = 0;
        int deleteSourceCount = 0;
        for (Item item1 : mItems) {
            switch (item1.getResolveResult()) {
                case KEEP_SOURCE:
                    ++keepSourceCount;
                    ++deleteTargetCount;
                    break;
                case KEEP_TARGET:
                    ++keepTargetCount;
                    ++deleteSourceCount;
                    break;
                case KEEP_BOTH:
                    ++keepSourceCount;
                    ++keepTargetCount;
                    break;
                case NONE:
                    break;
            }
        }

        return new Param.Five<>(keepTargetCount, keepSourceCount, deleteTargetCount, deleteSourceCount, mItems.size());
    }

    private void notifyResolveCountChange() {
        if (mOnInteractionListener != null) {

            int keepTargetCount = 0;
            int keepSourceCount = 0;
            int deleteTargetCount = 0;
            int deleteSourceCount = 0;
            for (Item item1 : mItems) {
                switch (item1.getResolveResult()) {
                    case KEEP_SOURCE:
                        ++keepSourceCount;
                        ++deleteTargetCount;
                        break;
                    case KEEP_TARGET:
                        ++keepTargetCount;
                        ++deleteSourceCount;
                        break;
                    case KEEP_BOTH:
                        ++keepSourceCount;
                        ++keepTargetCount;
                        break;
                    case NONE:
                        break;
                }
            }

            mOnInteractionListener.onResolveCountChange(mItems.size(), keepTargetCount, keepSourceCount, deleteTargetCount, deleteSourceCount);
        }
    }

    private void updatePositionViewHolderResolveResult(int position, ResolveResult resolveResult) {
        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(position);
        if (vh != null && vh instanceof ViewHolder) {
            updateViewHolderResultResult(((ViewHolder) vh), resolveResult);
        }
    }

    private void updateViewHolderResultResult(ViewHolder holder, ResolveResult resolveResult) {
        switch (resolveResult) {

            case KEEP_SOURCE:
                holder.selectSource();
                break;
            case KEEP_TARGET:
                holder.selectTarget();
                break;
            case KEEP_BOTH:
                holder.selectBoth();
                break;
            case NONE:
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
        @BindView(R.id.target_check) TextView targetCheckBox;
        @BindView(R.id.target_image_resolution) TextView targetImageResolution;
        @BindView(R.id.target_file_size) TextView targetFileSize;


        @BindView(R.id.source_image) ImageView sourceImage;
        @BindView(R.id.source_check) TextView sourceCheckBox;
        @BindView(R.id.source_image_resolution) TextView sourceImageResolution;
        @BindView(R.id.source_file_size) TextView sourceFileSize;
        @BindView(R.id.resolve_result_indicator) ImageView resolveResult;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        void selectSource() {
            sourceCheckBox.setVisibility(View.VISIBLE);
            sourceCheckBox.setSelected(true);
            sourceImage.setSelected(true);

            targetCheckBox.setVisibility(View.GONE);
            targetCheckBox.setSelected(false);
            targetImage.setSelected(false);

            resolveResult.setImageResource(R.drawable.ic_compare_arrow_right_black_18px);
        }

        void selectTarget() {

            targetCheckBox.setSelected(true);
            targetCheckBox.setVisibility(View.VISIBLE);

            targetImage.setSelected(true);


            sourceCheckBox.setVisibility(View.GONE);
            sourceCheckBox.setSelected(false);

            sourceImage.setSelected(false);

            resolveResult.setImageResource(R.drawable.ic_compare_arrow_left_black_18px);
        }

        void selectNone() {
            targetCheckBox.setVisibility(View.GONE);
            targetCheckBox.setSelected(false);
            targetImage.setSelected(false);

            sourceCheckBox.setVisibility(View.GONE);
            sourceCheckBox.setSelected(false);
            sourceImage.setSelected(false);

            resolveResult.setImageDrawable(null);
        }

        public void selectBoth() {
            targetCheckBox.setVisibility(View.VISIBLE);
            targetCheckBox.setSelected(true);

            targetImage.setSelected(true);

            sourceCheckBox.setVisibility(View.VISIBLE);
            sourceCheckBox.setSelected(true);
            sourceImage.setSelected(true);

            resolveResult.setImageDrawable(null);
        }
    }
}

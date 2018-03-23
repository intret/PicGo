package cn.intret.app.picgo.ui.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import cn.intret.app.picgo.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.siyamed.shapeimageview.RoundedImageView
import java.io.File
import java.util.*

/**
 * 水平图片列表
 */
class ThumbnailListAdapter(items: List<Item>?) : RecyclerView.Adapter<ThumbnailListAdapter.ViewHolder>() {

    internal var mItems: List<Item> = LinkedList()

    internal var mClickListener: OnItemClickListener? = null

    internal interface OnItemClickListener {
        fun onItemClick(v: View, item: Item, position: Int)

        fun onItemLongClick(v: View, item: Item, position: Int)
    }

    internal fun setOnClickListener(listener: OnItemClickListener): ThumbnailListAdapter {
        mClickListener = listener
        return this
    }

    class Item {
        internal var mFile: File? = null

        fun setFile(file: File): Item {
            mFile = file
            return this
        }
    }

    init {
        if (items != null) {
            mItems = items
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.horizontal_image_list_item, null)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mItems[mItems.size - 1 - position]

        //        holder.itemView.setOnClickListener(v -> {
        //            if (mClickListener != null) {
        //                mClickListener.onItemClick(v, item, position);
        //            }
        //        });

        holder.itemView.setOnLongClickListener { v ->
            if (mClickListener != null) {
                mClickListener!!.onItemLongClick(v, item, position)
            }
            true
        }

        if (item.mFile != null) {
            Glide.with(holder.itemView.context)
                    .asBitmap()
                    .load(item.mFile)
                    .apply(RequestOptions.centerCropTransform())
                    .into(holder.image)
        } else {
            // TODO: set placeholder
        }
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.image)
        lateinit var image: RoundedImageView

        init {
            ButterKnife.bind(this, itemView)
            //            itemView.setOnTouchListener((v, event) -> {
            //                // http://stackoverflow.com/questions/8121491/is-it-possible-to-add-a-scrollable-textview-to-a-listview
            //                v.getParent().requestDisallowInterceptTouchEvent(false); // needed for complex gestures
            //                // simple tap works without the above line as well
            //                return image.dispatchTouchEvent(event); // onTouchEvent won't work
            //            });
        }
    }
}

package cn.intret.app.picgo.screens.adapter


import android.support.v7.widget.AppCompatCheckBox
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import cn.intret.app.picgo.R
import com.annimon.stream.Stream
import java.io.File
import java.util.*

class FlatFolderListAdapter(items: List<Item>?) : RecyclerView.Adapter<FlatFolderListAdapter.ViewHolder>() {

    internal var mItems: List<Item> = LinkedList()

    val selectedItem: List<Item>
        get() = Stream.of(mItems).filter({ it.isSelected() }).toList()

    class Item {
        var mIsSelected = true
        var mDirectory: File? = null
        var mCount: Int = 0
        var mAdapter: ThumbnailListAdapter? = null
        var mThumbList: List<File>? = null
        var mThumbnailImages: List<ThumbnailImage>? = null

        fun isSelected(): Boolean {
            return mIsSelected
        }

        fun setSelected(selected: Boolean): Item {
            mIsSelected = selected
            return this
        }

        fun getAdapter(): ThumbnailListAdapter? {
            return mAdapter
        }

        fun setAdapter(adapter: ThumbnailListAdapter): Item {
            mAdapter = adapter
            return this
        }

        fun getCount(): Int {
            return mCount
        }

        fun setCount(count: Int): Item {
            mCount = count
            return this
        }

        fun getThumbList(): List<File>? {
            return mThumbList
        }

        fun setThumbList(thumbList: List<File>): Item {
            mThumbList = thumbList
            return this
        }

        fun setDirectory(directory: File): Item {
            mDirectory = directory
            return this
        }

        fun getThumbnailImages(): List<ThumbnailImage>? {
            return mThumbnailImages
        }

        fun setThumbnailImages(thumbnailImages: List<ThumbnailImage>): Item {
            mThumbnailImages = thumbnailImages
            return this
        }
    }

    class ThumbnailImage {
        internal var mFile: File? = null

        fun setFile(file: File?): ThumbnailImage {
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
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.flat_folder_list_item, parent, false)
        return ViewHolder(v)
    }

    private fun filesToItems(thumbList: List<File>?): List<ThumbnailListAdapter.Item>? {
        return if (thumbList == null) {
            null
        } else Stream.of(thumbList).map { file -> ThumbnailListAdapter.Item().setFile(file) }.toList()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = mItems[position]

        holder.name
        holder.name.text = item.mDirectory?.name
        holder.count.text = item.getCount().toString()

        holder.checkBox.isChecked = item.isSelected()
        holder.checkBox.setTag(R.id.item, item)
        holder.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            val tag = buttonView.getTag(R.id.item)
            if (tag is Item) {
                tag.setSelected(isChecked)
            }
        }

        // 缩略图列表
        if (item.getAdapter() == null) {
            val adapter = ThumbnailListAdapter(filesToItems(item.getThumbList()))

            item.setAdapter(adapter)
            holder.thumbList.isClickable = false

            holder.thumbList.layoutManager = holder.layout
            holder.thumbList.adapter = item.getAdapter()
        } else {
            holder.thumbList.isClickable = false
            holder.thumbList.layoutManager = holder.layout
            holder.thumbList.swapAdapter(item.getAdapter(), false)
        }
    }

    override fun getItemCount(): Int {
        return mItems.size
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @BindView(R.id.name)
        lateinit var name: TextView

        @BindView(R.id.count)
        lateinit var count: TextView

        @BindView(R.id.thumb_list)
        lateinit var thumbList: RecyclerView

        @BindView(R.id.check_box)
        lateinit var checkBox: AppCompatCheckBox

        private var mLayoutManager: LinearLayoutManager? = null

        val layout: RecyclerView.LayoutManager?
            get() {
                if (mLayoutManager == null) {
                    mLayoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, true)
                }
                return mLayoutManager
            }

        init {

            ButterKnife.bind(this, itemView)
        }
    }

}

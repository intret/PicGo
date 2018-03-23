package cn.intret.app.picgo.ui.adapter

import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import cn.intret.app.picgo.R
import cn.intret.app.picgo.utils.BundleUtils
import cn.intret.app.picgo.utils.DataConsumer2
import cn.intret.app.picgo.utils.PathUtils
import cn.intret.app.picgo.utils.SystemUtils
import com.afollestad.sectionedrecyclerview.ItemCoord
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.annimon.stream.Stream
import com.annimon.stream.function.Predicate
import org.apache.commons.collections4.ListUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.EqualsBuilder
import q.rorbin.badgeview.Badge
import q.rorbin.badgeview.QBadgeView
import java.io.File
import java.util.*

/**
 * 分段文件夹列表
 */
class SectionedFolderListAdapter : SectionedRecyclerViewAdapter<SectionedViewHolder> {

    // ---------------------------------------------------------------------------------------------
    // UI options
    // ---------------------------------------------------------------------------------------------

    var mShowHeaderOptionButton = false
    var mShowCloseButton = false


    // 过滤模式会高亮显示匹配关键字
    private var mHighlightItemName = false
    val mEnableItemClick = true
    var mIsSelectable = false
    var mIsCollapsable = true
    var mIsMultiSelect = false

    // 是否显示冲突微标
    private var mShowConflictBadge: Boolean = false
    private var mShowSourceDirBadgeWhenEmpty = true
    private var mFiltering = false


    // ---------------------------------------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------------------------------------

    private var mSections: MutableList<Section> = LinkedList()
    private var mRecyclerView: RecyclerView? = null
    private var mConflictFiles: Map<File, List<File>>? = null
    var mMoveFileSourceDir: File? = null
    private var mSectionsBeforeFilter: MutableList<Section>? = null

    val selectedItem: File?
        get() {
            for (i in mSections.indices) {
                val section = mSections[i]
                val items = section.items
                var i1 = 0
                val itemsSize = items!!.size
                while (i1 < itemsSize) {
                    val item = items[i1]
                    if (item.isSelected()) {
                        return item.getFile()
                    }
                    i1++
                }
            }
            return null
        }

    // ---------------------------------------------------------------------------------------------
    // Event handler
    // ---------------------------------------------------------------------------------------------

    var mOnItemClickListener: OnItemClickListener? = null

    val sections: List<Section>
        get() = mSections


    fun getItem(relativePosition: ItemCoord): Item? {
        val section = relativePosition.section()
        if (section >= 0 && section < mSections.size) {
            val sectionItem = mSections[section]
            val items = sectionItem.items

            val i = relativePosition.relativePos()
            if (i >= 0 && i < items!!.size) {
                return items[i]
            }
        }
        return null
    }

    fun renameDirectory(oldDirectory: File?, newDirectory: File?) {
        if (oldDirectory == null || newDirectory == null) {
            return
        }

        var sectionIndex = -1
        var itemIndex = -1
        var sec = 0
        val mSectionsSize = mSections.size
        while (sec < mSectionsSize) {
            val section = mSections[sec]
            val ii = ListUtils.indexOf(section.items) { `object` -> SystemUtils.isSameFile(`object`.getFile(), oldDirectory) }
            if (ii != -1) {
                sectionIndex = sec
                itemIndex = ii
                break
            }
            sec++
        }
        if (sectionIndex != -1) {
            mSections[sectionIndex].items!![itemIndex].setFile(newDirectory).setName(newDirectory.name)
            notifyItemChanged(getAbsolutePosition(sectionIndex, itemIndex))
        }
    }

    fun selectItem(dir: File?): Boolean {
        if (dir == null) {
            Log.w(TAG, "selectItem: select null directory")
            return false
        }

        // Find single selected item and mark it as 'unselected'
        var si = 0
        val mSectionsSize = mSections.size
        while (si < mSectionsSize) {

            val currSec = mSections[si]
            val items = currSec.items

            // find current selected item index
            var ii = 0
            val itemsSize = items!!.size
            while (ii < itemsSize) {
                val currItem = items[ii]

                // Update 'selected' item to 'unselected'
                if (currItem.isSelected()) {
                    if (currItem.getFile() != dir) {
                        currItem.setSelected(false) // mark as 'unselected'
                    }


                    if (mRecyclerView != null) {
                        val absPos = getAbsolutePosition(si, ii)
                        updateItemViewHolderCheckStatus(absPos, currItem.isSelected())
                    }
                } else {
                    if (currItem.getFile() == dir) {
                        currItem.setSelected(true)

                        if (mRecyclerView != null) {
                            val lm = mRecyclerView!!.layoutManager
                            if (lm != null) {
                                val absPos = getAbsolutePosition(si, ii)
                                updateItemViewHolderCheckStatus(absPos, currItem.isSelected())
                            }
                        }
                    }
                }
                ii++
            }
            si++
        }
        return false
    }

    fun removeFolderItem(selectedDir: File): Boolean {

        var sec = -1
        var relative = -1

        var si = 0
        val mSectionsSize = mSections.size
        while (si < mSectionsSize) {
            val section = mSections[si]
            val items = section.items
            var ii = 0
            val itemsSize = items!!.size
            while (ii < itemsSize) {
                val item = items[ii]
                if (item.getFile() == selectedDir) {
                    sec = si
                    relative = ii
                }
                ii++
            }
            si++
        }


        if (sec == -1 || relative == -1) {
            return false
        } else {
            mSections[sec].items!!.removeAt(relative)

            val absolutePosition = getAbsolutePosition(sec, relative)
            notifyItemRemoved(absolutePosition)

            return true
        }
    }

    fun removeFolderItem(sectionIndex: Int, relativePosition: Int) {
        if (sectionIndex >= 0 && sectionIndex < mSections.size) {
            val section = mSections[sectionIndex]
            val items = section.items

            if (relativePosition >= 0 && relativePosition < items!!.size) {
                val absolutePosition = getAbsolutePosition(sectionIndex, relativePosition)
                items.removeAt(relativePosition)
                notifyItemRemoved(absolutePosition)
            }
        } else {
            Log.w(TAG, "removeFolderItem: invalid argument")
        }
    }

    private enum class ItemType {
        HEADER,
        FOOTER,
        ITEM
    }

    private fun getItemType(adapter: SectionedFolderListAdapter, position: Int): ItemType {
        val header = adapter.isHeader(position)
        val footer = adapter.isFooter(position)
        return if (header) {
            ItemType.HEADER
        } else if (footer) {
            ItemType.FOOTER
        } else {
            ItemType.ITEM
        }
    }

    fun setSourceDirectory(file: File) {
        mMoveFileSourceDir = file
    }

    fun updateConflictFiles(folderConflictFiles: Map<File, List<File>>) {


        mShowConflictBadge = true
        mConflictFiles = folderConflictFiles

        Log.d(TAG, "updateConflictFiles() called with: folderConflictFiles = [$folderConflictFiles]")

        var clearConflictCount = folderConflictFiles.isEmpty()
        mMoveFileSourceDir
                ?.takeIf {
                    folderConflictFiles.size == 1 && folderConflictFiles.containsKey(it)
                }?.let {
                    clearConflictCount = true
                }

        var si = 0
        val ss = mSections.size
        while (si < ss) {
            val section = mSections[si]
            val items = section.items
            var ii = 0
            val itemsSize = items!!.size
            while (ii < itemsSize) {
                val item = items[ii]
                if (item.getFile() == null) {
                    ii++
                    continue
                }
                if (item.getFile() == mMoveFileSourceDir) {
                    ii++
                    continue
                }

                if (clearConflictCount) {

                    if (item.clearConflictFiles()) {

                        // TODO replaced with notifyItem

                        if (mRecyclerView != null) {
                            val absolutePosition = getAbsolutePosition(si, ii)

                            val payload = Bundle()
                            payload.putStringArrayList(PAYLOAD_KEY_CONFLICT_FILES, PathUtils.fileListToPathArrayList(item.conflictFiles))
                            notifyItemChanged(absolutePosition, payload)

                            //                        RecyclerView.ViewHolder vh = mRecyclerView.findViewHolderForAdapterPosition(absolutePosition);
                            //                        if (vh != null && vh instanceof ItemViewHolder) {
                            //                            Log.d(TAG, "updateConflictFiles: update conflict file count");
                            //
                            //                            bindViewHolderBadge((ItemViewHolder) vh, item);
                            //                        }
                        }
                    }

                } else {

                    item.getFile()?.let {

                        if (folderConflictFiles.containsKey(it)) {
                            val conflictFiles = folderConflictFiles[it]
                            item.setConflictFiles(conflictFiles?.toMutableList())

                            if (mShowConflictBadge) {
                                item.setItemSubType(ItemSubType.CONFLICT_COUNT)
                            } else {
                                Log.w(TAG, "updateConflictFiles: 设置了conflict 文件列表但是配置为不显示 conflict badge")
                                item.setItemSubType(ItemSubType.NORMAL)
                            }

                            // TODO replaced with notifyDataSetChanged
                            if (mRecyclerView != null) {
                                val vh = mRecyclerView!!.findViewHolderForAdapterPosition(getAbsolutePosition(si, ii))
                                if (vh != null && vh is ItemViewHolder) {
                                    Log.d(TAG, "updateConflictFiles: update conflict file count")

                                    bindViewHolderBadge(vh, item)
                                }
                            }
                        }
                    }
                }
                ii++
            }
            si++
        }
    }

    /**
     * 局部刷新更新
     *
     * @param newAdapter
     */
    fun diffUpdate(newAdapter: SectionedFolderListAdapter) {
        val oldItemCount = itemCount
        val newItemCount = newAdapter.itemCount

        // 应用新的过滤模式
        setHighlightItemName(newAdapter.isHighlightItemName())

        // 更新新 item 中的选中状态
        val selectedItem = selectedItem
        if (selectedItem != null) {
            newAdapter.selectItem(selectedItem)
        }

        Log.d(TAG, "diffUpdate: 计算差异 old $oldItemCount new $newItemCount")

        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun getOldListSize(): Int {
                return oldItemCount
            }

            override fun getNewListSize(): Int {
                return newItemCount
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldCoord = getRelativePosition(oldItemPosition)
                val newCoord = newAdapter.getRelativePosition(newItemPosition)

                val oldItemType = getItemType(this@SectionedFolderListAdapter, oldItemPosition)
                val newItemType = getItemType(newAdapter, newItemPosition)

                if (oldItemType == newItemType) {
                    // 都是 Header 比较 Section 对应的文件
                    if (oldItemType == ItemType.HEADER) {
                        val oldSec = mSections[oldCoord.section()]
                        val newSec = newAdapter.sections[newCoord.section()]

                        return oldSec.file == newSec.file
                    }

                    // 都是 Footer 比较 Section 对应的文件
                    if (oldItemType == ItemType.FOOTER) {
                        val oldSec = mSections[oldCoord.section()]
                        val newSec = newAdapter.sections[newCoord.section()]

                        return oldSec.file == newSec.file
                    }

                    if (oldItemType == ItemType.ITEM) {
                        val oldItem = this@SectionedFolderListAdapter.getItem(oldCoord)
                        val newItem = newAdapter.getItem(newCoord)

//                        if (!equals) {
                        //                            Log.d(TAG, String.format("areItemsTheSame() called with: oldItemPosition = [%d], newItemPosition = [%d] equal = %s, diff : %s, %s",
                        //                                    oldItemPosition, newItemPosition, equals, oldItem.getFile(), newItem.getFile()));
                        //                        }

                        return oldItem!!.getFile() == newItem!!.getFile()
                        //                        return oldCoord.section() == newCoord.section() && oldCoord.relativePos() == newCoord.relativePos();
                    }
                }

                // 类型不一样这两项自然是不一样
                return false
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldCoord = getRelativePosition(oldItemPosition)
                val newCoord = newAdapter.getRelativePosition(newItemPosition)

                val oldItemType = getItemType(this@SectionedFolderListAdapter, oldItemPosition)
                val newItemType = getItemType(newAdapter, newItemPosition)

                if (oldItemType == newItemType) {
                    // Item 类型一样比较 Item 的值
                    when (oldItemType) {

                        SectionedFolderListAdapter.ItemType.HEADER, SectionedFolderListAdapter.ItemType.FOOTER -> {
                            val oldSec = mSections[oldCoord.section()]
                            val newSec = newAdapter.sections[newCoord.section()]

                            // Section Header 的显示名称是否一样
                            return StringUtils.equals(oldSec.name, newSec.name)
                        }
                        SectionedFolderListAdapter.ItemType.ITEM -> {
                            val oldItem = this@SectionedFolderListAdapter.getItem(oldCoord)
                            val newItem = newAdapter.getItem(newCoord)

                            return oldItem!!.contentEquals(newItem)
                        }
                        else -> {
                            Log.e(TAG, "areContentsTheSame: unhandled type : $oldItemType")
                            return false
                        }
                    }
                } else {
                    Log.w(TAG, "areContentsTheSame: not same type old:$oldItemType new:$newItemType")
                    return false
                }

            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {

                val oldCoord = getRelativePosition(oldItemPosition)
                val newCoord = newAdapter.getRelativePosition(newItemPosition)

                val oldItemType = getItemType(this@SectionedFolderListAdapter, oldItemPosition)
                val newItemType = getItemType(newAdapter, newItemPosition)

                if (oldItemType != newItemType) {
                    Log.w(TAG, "getChangePayload: not the same item type : old=$oldItemCount new:$newItemType")
                    return null
                }
                when (oldItemType) {

                    SectionedFolderListAdapter.ItemType.FOOTER, SectionedFolderListAdapter.ItemType.HEADER -> {
                        // 执行到这里说明 Section 名称不一样，保存新的 Section 名称即可
                        val oldSec = mSections[oldCoord.section()]
                        val newSec = newAdapter.sections[newCoord.section()]

                        val res = Bundle()
                        if (!StringUtils.equals(oldSec.name, newSec.name)) {
                            res.putString(PAYLOAD_KEY_SECTION_NAME, newSec.name)
                        }
                        return res
                    }
                    SectionedFolderListAdapter.ItemType.ITEM -> {
                        val oldItem = getItem(oldCoord)
                        val newItem = newAdapter.getItem(newCoord)

                        if (oldItem == null || newItem == null) {

                            Log.w(TAG, "getChangePayload: empty item $oldItem new $newItem")
                            return null
                        }
                        val oldConflictFiles = oldItem.conflictFiles
                        val newConflictFiles = newItem.conflictFiles

                        // TODO: 不用如此严格检测冲突文件列表顺序的吧？
                        val isSameConflictFileList = ListUtils.isEqualList(oldConflictFiles, newConflictFiles)
                        val isSameName = StringUtils.equals(oldItem.getName(), newItem.getName())
                        val isSameThumbList = ListUtils.isEqualList(oldItem.thumbList, newItem.thumbList)
                        val isSameSelection = oldItem.isSelected() == newItem.isSelected()
                        val isSameSubType = oldItem.getItemSubType() == newItem.getItemSubType()

                        val isSameCount = oldItem.getCount() == newItem.getCount()
                        val isSameKeywordLen = oldItem.getKeywordLength() == newItem.getKeywordLength()
                        val isSameKeywordStartIndex = oldItem.getKeywordStartIndex() == newItem.getKeywordStartIndex()


                        // 哪一项不一样就只存哪一项

                        val payloadBundle = Bundle()

                        if (!isSameName) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_ITEM_NAME " + newItem.getName()!!)
                            payloadBundle.putString(PAYLOAD_KEY_ITEM_NAME, newItem.getName())
                        }

                        if (!isSameThumbList) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_THUMB_LIST " + newItem.thumbList!!)
                            payloadBundle.putStringArrayList(PAYLOAD_KEY_THUMB_LIST,
                                    PathUtils.fileListToPathArrayList(newItem.thumbList))
                        }

                        if (!isSameConflictFileList) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_CONFLICT_FILES" + newItem.conflictFiles!!)
                            payloadBundle.putStringArrayList(PAYLOAD_KEY_CONFLICT_FILES,
                                    PathUtils.fileListToPathArrayList(newItem.conflictFiles))
                        }

                        if (!isSameSelection) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_SELECTION(old) " + oldItem.isSelected())
                            payloadBundle.putBoolean(PAYLOAD_KEY_SELECTION, newItem.isSelected())
                        }

                        if (!isSameCount) {
                            Log.w(TAG, "getChangePayload: PAYLOAD_KEY_COUNT(old) " + oldItem.isSelected())
                            payloadBundle.putInt(PAYLOAD_KEY_COUNT, newItem.getCount())
                        }

                        if (!isSameKeywordLen) {
                            payloadBundle.putInt(PAYLOAD_KEY_KEYWORD_LENGTH, newItem.getKeywordLength())
                        }

                        if (!isSameKeywordStartIndex) {
                            payloadBundle.putInt(PAYLOAD_KEY_KEYWORD_START_INDEX, newItem.getKeywordStartIndex())
                        }

                        if (!isSameSubType) {
                            payloadBundle.putSerializable(PAYLOAD_KEY_SUB_TYPE, newItem.getItemSubType())
                        }

                        return payloadBundle
                    }
                }
                return super.getChangePayload(oldItemPosition, newItemPosition)
            }
        })

        mSections.clear()
        for (i in 0 until newAdapter.sections.size) {
            val section = newAdapter.sections[i]
            (section.clone() as Section).let {
                mSections.add(it)
            }
        }

        diffResult.dispatchUpdatesTo(this)
    }

    fun updateThumbList(directory: File?, thumbnails: MutableList<File>?) {
        if (directory == null || thumbnails == null) {
            return
        }

        var secIndex = 0
        val mSectionsSize = mSections.size
        while (secIndex < mSectionsSize) {
            val section = mSections[secIndex]
            val items = section.items
            var itemIndex = 0
            val itemsSize = items!!.size
            while (itemIndex < itemsSize) {
                val item = items[itemIndex]
                if (item.getFile() == directory) {
                    item.setThumbList(thumbnails)

                    if (mRecyclerView != null) {
                        val vh = mRecyclerView!!.findViewHolderForAdapterPosition(getAbsolutePosition(secIndex, itemIndex))
                        if (vh != null && vh is SectionedFolderListAdapter.ItemViewHolder) {
                            updateThumbList(vh, true, object : ThumbnailListAdapter.OnItemClickListener {
                                override fun onItemClick(v: View, ii: ThumbnailListAdapter.Item, position: Int) {
                                    if (mOnItemClickListener != null) {
                                        mOnItemClickListener!!.onItemClick(null, -1, item, -1)
                                    }
                                }

                                override fun onItemLongClick(v: View, ii: ThumbnailListAdapter.Item, position: Int) {
                                    if (mOnItemClickListener != null) {
                                        mOnItemClickListener!!.onItemLongClick(v, null, -1, item, -1)
                                    }
                                }
                            }, item.thumbList)
                        }
                    }
                    return
                }
                itemIndex++
            }
            secIndex++
        }
    }

    fun updateItemCount(dir: File?, count: Int) {
        if (dir == null || count < 0) {
            return
        }

        var si = 0
        val mSectionsSize = mSections.size
        while (si < mSectionsSize) {
            val section = mSections[si]
            val items = section.items
            var ii = 0
            val itemsSize = items!!.size
            while (ii < itemsSize) {
                val item = items[ii]
                if (item.getFile() == dir) {

                    item.setCount(count)
                    val vh = findItemViewHolder(si, ii)
                    if (vh != null) {
                        vh.count.text = count.toString()
                    }
                    return
                }
                ii++
            }
            si++
        }
    }

    fun updateSelectedCount(fileSelectedCountMap: Map<File, Int>?) {
        if (fileSelectedCountMap == null) {
            return
        }

        var i = 0
        val mSectionsSize = mSections.size
        while (i < mSectionsSize) {
            val section = mSections[i]
            val items = section.items
            var i1 = 0
            val itemsSize = items!!.size
            while (i1 < itemsSize) {
                val item = items[i1]
                val count = fileSelectedCountMap[item.getFile()]
                if (count != null) {
                    item.setSelectedCount(count)


                }
                i1++
            }
            i++
        }
    }

    fun scrollToItem(dir: File?) {

        if (dir == null) {
            Log.w(TAG, "scrollToItem: dir is null.")
            return
        }

        var si = 0
        val mSectionsSize = mSections.size
        while (si < mSectionsSize) {
            val section = mSections[si]
            val items = section.items
            var ii = 0
            val itemsSize = items!!.size
            while (ii < itemsSize) {
                val item = items[ii]
                if (item.getFile() == dir) {

                    if (mRecyclerView != null) {
                        mRecyclerView!!.scrollToPosition(getAbsolutePosition(si, ii))
                    }
                    return
                }
                ii++
            }
            si++
        }
    }

    fun updateSelectedCount(dir: File?, selectedCount: Int) {
        if (dir == null || selectedCount < 0) {
            return
        }

        var si = 0
        val mSectionsSize = mSections.size
        while (si < mSectionsSize) {
            val section = mSections[si]
            val items = section.items
            var ii = 0
            val itemsSize = items!!.size
            while (ii < itemsSize) {
                val item = items[ii]
                if (item.getFile() == dir) {

                    item.setSelectedCount(selectedCount)
                    val itemViewHolder = findItemViewHolder(si, ii)
                    if (itemViewHolder != null) {
                        bindViewHolderBadge(itemViewHolder, item)
                    }
                    return
                }
                ii++
            }
            si++
        }
    }

    private fun findItemViewHolder(sectionIndex: Int, relativePosition: Int): ItemViewHolder? {

        val absolutePosition = getAbsolutePosition(sectionIndex, relativePosition)
        if (mRecyclerView != null) {
            val vh = mRecyclerView!!.findViewHolderForAdapterPosition(absolutePosition)
            if (vh != null && vh is ItemViewHolder) {
                return vh
            }
        }
        return null
    }

    fun updateSelectedCount(relativePosition: ItemCoord) {
        if (mRecyclerView != null) {
            val absolutePosition = getAbsolutePosition(relativePosition)
            SectionedListItemClickDispatcher(this)
                    .dispatch(absolutePosition, object : SectionedListItemDispatchListener<SectionedRecyclerViewAdapter<*>> {
                        override fun onHeader(adapter: SectionedRecyclerViewAdapter<*>, coord: ItemCoord) {

                        }

                        override fun onFooter(adapter: SectionedRecyclerViewAdapter<*>, coord: ItemCoord) {

                        }

                        override fun onItem(adapter: SectionedRecyclerViewAdapter<*>, coord: ItemCoord) {
                            mRecyclerView!!.findViewHolderForAdapterPosition(absolutePosition)
                        }
                    })

        }
    }

    // ------------------------------------------------
    // Item filter
    // ------------------------------------------------


    /**
     * Filter
     *
     * @param filter 过滤
     */
    fun filter(filter: Predicate<in Item>) {

        val filteredSections = LinkedList<Section>()

        // 保存复制现有数据并保存
        mSectionsBeforeFilter = LinkedList()
        mSections.forEach { section ->
            mSectionsBeforeFilter?.add(section.clone() as Section)
        }

        // 根据现在的数据进行过滤

        for (s in mSections) {
            val cloneSection = s.clone() as Section
            filteredSections.add(cloneSection)

            val items = Stream.of(cloneSection.items)
                    .filter(filter)
                    .toList()
            cloneSection.items = items
        }

        // 显示过滤后的数据
        mFiltering = true
        showFiltereSections(filteredSections)
    }

    fun showFiltereSections(sections: MutableList<Section>?) {
        if (sections == null) {
            Log.w(TAG, "showFiltereSections: 参数为空")
            return
        }

        val adapter = SectionedFolderListAdapter(sections)
        adapter.setHighlightItemName(mHighlightItemName)

        diffUpdate(adapter)
    }

    fun leaveFilterMode() {
        if (!mFiltering) {
            Log.w(TAG, "leaveFilterMode: 没有在过滤模式")
            return
        }

        mFiltering = false
        showFiltereSections(mSectionsBeforeFilter)
    }


    // ---------------------------------------------------------------------------------------------
    // Interfaces and Classes
    // ---------------------------------------------------------------------------------------------


    class Section : Cloneable {
        var name: String? = null
        var file: File? = null
        var items: MutableList<Item>? = null

        public override fun clone(): Any {
            val clone = super.clone() as Section
            clone.file = this.file?.let { File(it.absolutePath) }
            clone.items = this.items?.map { it }?.toMutableList()
            return clone
        }
    }

    enum class ItemSubType {
        NORMAL,
        ADD_ITEM,
        CONFLICT_COUNT,
        SELECTED_COUNT,
        SOURCE_DIR,
        NONE
    }

    class Item : Cloneable, ContentEqual {

        var mIsSelected: Boolean = false
        var mFile: File? = null
        var mCount: Int = 0
        var mSelectedCount = COUNT_NONE
        var mConflictFiles: MutableList<File>? = null

        var mThumbList: MutableList<File>? = null
        var mKeywordLength: Int = 0
        var mKeywordStartIndex: Int = 0
        var mName: String? = null

        var mItemSubType = ItemSubType.NORMAL

        val conflictFiles: List<File>?
            get() = mConflictFiles

        val thumbList: List<File>?
            get() = mThumbList

        val isSourceDirType: Boolean
            get() = mItemSubType == ItemSubType.SOURCE_DIR

        fun getItemSubType(): ItemSubType {
            return mItemSubType
        }

        fun setItemSubType(itemSubType: ItemSubType): Item {
            mItemSubType = itemSubType
            return this
        }

        fun setConflictFiles(conflictFiles: MutableList<File>?): Item {
            Log.d(TAG, "setConflictFiles: set $mName conflict files :$conflictFiles")
            mConflictFiles = conflictFiles
            return this
        }

        fun clearConflictFiles(): Boolean {
            if (mConflictFiles != null && !mConflictFiles!!.isEmpty()) {
                mConflictFiles!!.clear()
                return true
            } else {
                return false
            }
        }

        fun getSelectedCount(): Int {
            return mSelectedCount
        }

        fun setSelectedCount(selectedCount: Int): Item {
            mSelectedCount = selectedCount
            return this
        }

        fun setThumbList(thumbList: MutableList<File>?): Item {
            mThumbList = thumbList
            return this
        }

        fun getName(): String? {
            return mName
        }

        fun setName(name: String): Item {
            mName = name
            return this
        }

        fun getCount(): Int {
            return mCount
        }

        fun setCount(count: Int): Item {
            mCount = count
            return this
        }

        fun getFile(): File? {
            return mFile
        }

        fun setFile(file: File?): Item {
            mFile = file
            return this
        }

        fun setSelected(selected: Boolean): Item {
            mIsSelected = selected
            return this
        }

        fun isSelected(): Boolean {
            return mIsSelected
        }

        fun setKeywordStartIndex(keywordStartIndex: Int): Item {
            mKeywordStartIndex = keywordStartIndex
            return this
        }

        fun getKeywordStartIndex(): Int {
            return mKeywordStartIndex
        }

        fun setKeywordLength(keywordLength: Int): Item {
            mKeywordLength = keywordLength
            return this
        }

        fun getKeywordLength(): Int {
            return mKeywordLength
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false

            val item = o as Item?

            if (mIsSelected != item!!.mIsSelected) return false
            if (mCount != item.mCount) return false
            if (mSelectedCount != item.mSelectedCount) return false
            if (mKeywordLength != item.mKeywordLength) return false
            if (mKeywordStartIndex != item.mKeywordStartIndex) return false
            if (if (mFile != null) mFile != item.mFile else item.mFile != null) return false
            if (if (mConflictFiles != null) mConflictFiles != item.mConflictFiles else item.mConflictFiles != null)
                return false
            if (if (mThumbList != null) mThumbList != item.mThumbList else item.mThumbList != null)
                return false
            return if (if (mName != null) mName != item.mName else item.mName != null) false else mItemSubType == item.mItemSubType

        }

        override fun hashCode(): Int {
            return if (mFile != null) mFile!!.hashCode() else 0
        }


        override fun clone(): Any {

            val item = super.clone() as Item

            item.setFile(if (this.getFile() == null) null else File(this.getFile()!!.absolutePath))
            if (this.mThumbList != null) {
                item.mThumbList = LinkedList()
                for (file in this.mThumbList!!) {
                    item.mThumbList!!.add(file)
                }
            }
            if (this.mConflictFiles != null) {
                item.mConflictFiles = LinkedList()
                for (conflictFile in this.mConflictFiles!!) {
                    item.mConflictFiles!!.add(conflictFile)
                }
            }

            return item
        }

        override fun toString(): String {
            return "Item{" +
                    "mIsSelected=" + mIsSelected +
                    ", file=" + mFile +
                    ", count=" + mCount +
                    ", mSelectedCount=" + mSelectedCount +
                    ", conflictFiles=" + mConflictFiles +
                    ", mThumbList=" + mThumbList +
                    ", mKeywordLength=" + mKeywordLength +
                    ", mKeywordStartIndex=" + mKeywordStartIndex +
                    ", name='" + mName + '\''.toString() +
                    ", mItemSubType=" + mItemSubType +
                    '}'.toString()
        }

        override fun contentEquals(obj: Any?): Boolean {
            if (obj == null) {
                return false
            }
            if (obj === this) {
                return true
            }
            if (obj.javaClass != javaClass) {
                return false
            }
            val rhs = obj as Item?
            return EqualsBuilder()
                    .appendSuper(super.equals(obj))

                    .append(mConflictFiles, rhs!!.mConflictFiles)
                    .append(mCount, rhs.mCount)
                    .append(mFile, rhs.mFile)
                    .append(mIsSelected, rhs.mIsSelected)
                    .append(mKeywordLength, rhs.mKeywordLength)
                    .append(mKeywordStartIndex, rhs.mKeywordStartIndex)
                    .append(mSelectedCount, rhs.mSelectedCount)
                    .append(mItemSubType, rhs.mItemSubType)
                    .append(mThumbList, rhs.mThumbList)

                    .isEquals
        }
    }

    interface OnItemClickListener {
        fun onSectionHeaderClick(section: Section, sectionIndex: Int, adapterPosition: Int)

        fun onSectionHeaderOptionButtonClick(v: View, section: Section, sectionIndex: Int)

        fun onItemClick(sectionItem: Section?, section: Int, item: Item, relativePos: Int)

        fun onItemLongClick(v: View, sectionItem: Section?, section: Int, item: Item, relativePos: Int)

        fun onItemCloseClick(v: View, section: Section, item: Item, sectionIndex: Int, relativePosition: Int)
    }

    // ---------------------------------------------------------------------------------------------
    // Getter and setter
    // ---------------------------------------------------------------------------------------------


    fun isFiltering(): Boolean {
        return mFiltering
    }

    fun setFiltering(filtering: Boolean): SectionedFolderListAdapter {
        mFiltering = filtering
        return this
    }

    fun isShowCloseButton(): Boolean {
        return mShowCloseButton
    }

    fun setShowCloseButton(showCloseButton: Boolean): SectionedFolderListAdapter {
        mShowCloseButton = showCloseButton
        return this
    }

    fun isShowSourceDirBadgeWhenEmpty(): Boolean {
        return mShowSourceDirBadgeWhenEmpty
    }

    fun setShowSourceDirBadgeWhenEmpty(showSourceDirBadgeWhenEmpty: Boolean): SectionedFolderListAdapter {
        mShowSourceDirBadgeWhenEmpty = showSourceDirBadgeWhenEmpty
        return this
    }

    fun getMoveFileSourceDir(): File? {
        return mMoveFileSourceDir
    }

    /**
     * @param moveFileSourceDir 指定文件则标记对应的项为'源目录'，指定 null 则清除任何标记为 '源目录' 的项；
     * @return
     */
    fun setMoveFileSourceDir(moveFileSourceDir: File?): SectionedFolderListAdapter {

        val clearMark = moveFileSourceDir != null

        if (clearMark) {
            // 清除标志

            var si = 0
            val mSectionsSize = mSections.size
            while (si < mSectionsSize) {
                val section = mSections[si]
                val items = section.items
                var ii = 0
                val itemsSize = items!!.size
                while (ii < itemsSize) {
                    val item = items[ii]
                    if (item.getItemSubType() == ItemSubType.SOURCE_DIR) {
                        // 恢复为某一种模式
                        if (mShowConflictBadge && mConflictFiles!!.containsKey(item.getFile())) {
                            item.setItemSubType(ItemSubType.CONFLICT_COUNT)
                        } else {
                            // TODO
                            item.setItemSubType(ItemSubType.NORMAL)
                        }

                        // 更新界面
                        updateViewHolder(si, ii, item, DataConsumer2<ItemViewHolder, Item> { vh, it -> bindViewHolderBadge(vh, item) })
                    }
                    ii++
                }
                si++
            }
        } else {

            // 设置标志


        }

        var si = 0
        val mSectionsSize = mSections.size
        while (si < mSectionsSize) {
            val section = mSections[si]
            val items = section.items
            var ii = 0
            val itemsSize = items!!.size
            while (ii < itemsSize) {
                val item = items[ii]

                // Clear old item's Badge label 'source file'
                if (item.getItemSubType() == ItemSubType.SOURCE_DIR && item.getFile() != moveFileSourceDir) {

                    // 恢复为某种 sub type
                    if (mShowConflictBadge) {
                        item.setItemSubType(ItemSubType.CONFLICT_COUNT)

                        if (item.conflictFiles != null) {
                            updateViewHolder(si, ii, item, DataConsumer2<ItemViewHolder, Item> { vh, it -> bindViewHolderBadge(vh, item) })
                        } else {
                            updateViewHolder(si, ii, item, DataConsumer2<ItemViewHolder, Item> { vh, it -> bindViewHolderBadge(vh, item) })
                        }
                    } else {
                        if (item.getFile() == null) {
                            item.setItemSubType(ItemSubType.ADD_ITEM)
                        } else {
                            // TODO
                            item.setItemSubType(ItemSubType.NORMAL)
                        }

                        updateViewHolder(si, ii, item, DataConsumer2<ItemViewHolder, Item> { vh, it -> bindViewHolderBadge(vh, item) })
                    }
                }

                // 当前 item 是要设置为 ‘源目录’
                if (item.getFile() == moveFileSourceDir) {
                    item.setItemSubType(ItemSubType.SOURCE_DIR)

                    updateViewHolder(si, ii, item, DataConsumer2<ItemViewHolder, Item> { vh, it ->
                        bindViewHolderBadge(vh, item)
                        //vh.showSourceDirBadge(item.getSelectedItemCount());
                    })
                }
                ii++
            }
            si++
        }


        mMoveFileSourceDir = moveFileSourceDir
        return this
    }

    fun getConflictFiles(): Map<File, List<File>>? {
        return mConflictFiles
    }

    fun setConflictFiles(conflictFiles: Map<File, List<File>>): SectionedFolderListAdapter {
        mConflictFiles = conflictFiles
        return this
    }

    fun isShowConflictBadge(): Boolean {
        return mShowConflictBadge
    }

    fun setShowConflictBadge(showConflictBadge: Boolean): SectionedFolderListAdapter {
        mShowConflictBadge = showConflictBadge
        return this
    }

    fun isHighlightItemName(): Boolean {
        return mHighlightItemName
    }

    fun setHighlightItemName(highlightItemName: Boolean): SectionedFolderListAdapter {
        mHighlightItemName = highlightItemName
        return this
    }

    fun isShowHeaderOptionButton(): Boolean {
        return mShowHeaderOptionButton
    }

    fun setShowHeaderOptionButton(showHeaderOptionButton: Boolean): SectionedFolderListAdapter {
        mShowHeaderOptionButton = showHeaderOptionButton
        return this
    }

    fun isSelectable(): Boolean {
        return mIsSelectable
    }

    fun setSelectable(selectable: Boolean): SectionedFolderListAdapter {
        mIsSelectable = selectable
        return this
    }

    fun isCollapsable(): Boolean {
        return mIsCollapsable
    }

    fun setCollapsable(collapsable: Boolean): SectionedFolderListAdapter {
        mIsCollapsable = collapsable
        return this
    }

    fun isMultiSelect(): Boolean {
        return mIsMultiSelect
    }

    fun setMultiSelect(multiSelect: Boolean): SectionedFolderListAdapter {
        mIsMultiSelect = multiSelect
        return this
    }

    fun getOnItemClickListener(): OnItemClickListener? {
        return mOnItemClickListener
    }

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener): SectionedFolderListAdapter {
        mOnItemClickListener = onItemClickListener
        return this
    }


    // ---------------------------------------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------------------------------------

    internal constructor()

    constructor(sections: MutableList<Section>?) {
        if (sections != null) {
            mSections = sections
        }
    }

    internal fun setSections(sections: MutableList<Section>?): SectionedFolderListAdapter {
        if (sections != null) {
            this.mSections = sections
            //            notifyDataSetChanged();
        }
        return this
    }


    // ---------------------------------------------------------------------------------------------
    // RecyclerView
    // ---------------------------------------------------------------------------------------------

    override fun getSectionCount(): Int {
        return mSections.size
    }

    override fun getItemCount(section: Int): Int {
        val items = mSections[section]
        return items.items!!.size
    }

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder, section: Int, expanded: Boolean) {
        val vh = holder as HeaderViewHolder
        val sectionItem = mSections[section]

        val clickListener = { v: View ->
            val tag = v.getTag(R.id.item)
            if (tag != null && tag is Section) {

                val adapterPosition = vh.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val coord = getRelativePosition(adapterPosition)
                    val section1 = mSections[coord.section()]

                    clickSectionHeader(section1, coord.section(), adapterPosition)
                }
            }
        }
        vh.itemView.setTag(R.id.item, sectionItem)
        vh.itemView.setOnClickListener(clickListener)


        vh.name.text = sectionItem.name
        vh.name.setTag(R.id.item, sectionItem)
        vh.name.isClickable = false


        if (mShowHeaderOptionButton) {
            if (mEnableItemClick) {

                // 选项文字按钮
                vh.option.setTag(R.id.item, sectionItem)
                vh.option.setOnClickListener { v ->
                    val adapterPosition = holder.getAdapterPosition()
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        val coord = getRelativePosition(adapterPosition)
                        val sec = mSections[coord.section()]
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener!!.onSectionHeaderOptionButtonClick(v, sec, coord.section())
                        }
                    }
                }
            }
        } else {
            vh.option.visibility = View.GONE
        }
    }

    private fun clickSectionHeader(section: Section, sectionIndex: Int, adapterPosition: Int) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener!!.onSectionHeaderClick(section, sectionIndex, adapterPosition)
        }
    }

    private fun <R> extractTagValue(view: View, id: Int): R? {
        val tag = view.getTag(id)
        return if (tag != null) {
            tag as R
        } else null
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder, section: Int) {

    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
        Log.d(TAG, "onAttachedToRecyclerView() called with: recyclerView = [$recyclerView]")
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mRecyclerView = null
    }

    override fun onBindViewHolder(holder: SectionedViewHolder, sectionIndex: Int, relativePosition: Int, absolutePosition: Int) {
        val vh = holder as ItemViewHolder
        val section = mSections[sectionIndex]
        val item = section.items!![relativePosition]

        val context = vh.itemView.context
        /*
         * Item click
         */
        if (mEnableItemClick) {

            // Save data to view tag
            vh.itemView.setTag(R.id.section, section)
            vh.itemView.setTag(R.id.item, item)

            // Click
            vh.itemView.setOnClickListener { v ->
                val adapterPosition = holder.getAdapterPosition()
                val coor = getRelativePosition(adapterPosition)

                val clickItem = mSections[coor.section()].items!![coor.relativePos()]
                // Mark as 'selected'
                run {
                    if (clickItem.getItemSubType() == ItemSubType.ADD_ITEM) {
                        Log.d(TAG, "onBindViewHolder: 点击添加项")
                    } else {

                        if (!clickItem.isSelected()) {

                            // Section loop : Find single selected item and mark it as 'unselected'
                            var si = 0
                            val mSectionsSize = mSections.size
                            while (si < mSectionsSize) {

                                val currSec = mSections[si]
                                val items = currSec.items

                                // Item loop : find current selected item index
                                var ii = 0
                                val itemsSize = items!!.size
                                while (ii < itemsSize) {
                                    val currItem = items[ii]
                                    if (currItem.isSelected()) {
                                        // mark as 'unselected'
                                        val absPos = getAbsolutePosition(si, ii)
                                        currItem.setSelected(false)
                                        updateItemViewHolderCheckStatus(absPos, currItem.isSelected())
                                    }
                                    ii++
                                }
                                si++
                            }

                            // Mark the current clicked item as 'selected'
                            clickItem.setSelected(true)
                            updateItemViewHolderCheckStatus(adapterPosition, clickItem.isSelected())
                        } else {
                            Log.w(TAG, "item view clicked: click a selected item at coor $coor")
                        }
                    }
                }

                // Notify item clicking
                if (mOnItemClickListener != null) {
                    val sect = mSections[coor.section()]
                    mOnItemClickListener!!.onItemClick(
                            sect, coor.section(),
                            sect.items!![coor.relativePos()],
                            coor.relativePos())
                }
            }

            // Long click
            vh.itemView.setOnLongClickListener { v ->
                val adapterPosition = holder.getAdapterPosition()
                val coor = getRelativePosition(adapterPosition)

                if (mOnItemClickListener != null) {
                    val sectionItem1 = mSections[coor.section()]
                    mOnItemClickListener!!.onItemLongClick(v,
                            sectionItem1, coor.section(),
                            sectionItem1.items!![coor.relativePos()],
                            coor.relativePos())
                    return@setOnLongClickListener true
                } else {
                    return@setOnLongClickListener false
                }
            }

            /*
            vh.thumbnailList.addOnItemTouchListener(
                    new RecyclerItemTouchListener(
                            vh.itemView.getContext(),
                            mRecyclerView, (view, position) -> {

                            }, (view, position) -> {
                                if (mOnItemClickListener != null) {
                                    ItemCoord coor = getRelativePosition(vh.getAdapterPosition());

                                    Section section1 = mSections.get(coor.section());
                                    Item item1 = section1.getItems().get(coor.relativePos());
                                    mOnItemClickListener.onItemLongClick(vh.thumbnailList, section1, coor.section(), item1,  coor.relativePos() );
                                }
                            }));
*/
        }

        // Check
        vh.check.visibility = if (item.isSelected()) View.VISIBLE else View.GONE

        // Title : Folder name
        val name = item.getName()
        if (mHighlightItemName) {
            // 高亮关键字
            val keywordStartIndex = item.getKeywordStartIndex()
            if (isValidKeyword(name, keywordStartIndex, item.getKeywordLength())) {
                vh.setHighlightName(keywordStartIndex, item.getKeywordLength(), name)
            } else {
                vh.name.text = name
            }
        } else {
            vh.name.text = name
        }

        // Title : Text Color
        vh.name.setTextColor(context.resources
                .getColor(if (item.getItemSubType() == ItemSubType.ADD_ITEM)
                    R.color.list_item_title_text_highlight
                else
                    R.color.list_item_title_text_normal))

        // Badge
        bindViewHolderBadge(vh, item)

        // Count
        if (item.getItemSubType() == ItemSubType.ADD_ITEM) {
            vh.count.visibility = View.GONE
        } else {
            if (item.getCount() >= 0) {
                vh.count.visibility = View.VISIBLE
                vh.count.text = item.getCount().toString()
            } else {
                vh.count.visibility = View.GONE
            }
        }

        //        vh.setSelectedCountText(item.getSelectedItemCount(), item.getCount());

        // Thumbnail image list
        updateThumbList(vh, false, object : ThumbnailListAdapter.OnItemClickListener {
            override fun onItemClick(v: View, ii: ThumbnailListAdapter.Item, position: Int) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener!!.onItemClick(null, -1, item, -1)
                }
            }

            override fun onItemLongClick(v: View, ii: ThumbnailListAdapter.Item, position: Int) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener!!.onItemLongClick(v, null, -1, item, -1)
                }
            }
        }, item.thumbList)

        // Close button
        vh.close.visibility = if (mShowCloseButton) View.VISIBLE else View.GONE
        vh.close.setOnClickListener { v ->
            if (mOnItemClickListener != null) {
                mOnItemClickListener!!.onItemCloseClick(v, section, item, sectionIndex, relativePosition)
            }
        }
    }

    override fun onBindViewHolder(holder: SectionedViewHolder, section: Int, relativePosition: Int, absolutePosition: Int, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            Log.w(TAG, "onBindViewHolder: payload is empty")
            onBindViewHolder(holder, section, relativePosition, absolutePosition)
        } else {
            val o = payloads[0]
            if (o is Bundle) {


                if (isHeader(absolutePosition)) {

                    val viewHolder = holder as SectionedImageListAdapter.SectionHeaderViewHolder
                    val name = o.getString(PAYLOAD_KEY_SECTION_NAME)
                    holder.title.text = name

                } else if (isFooter(absolutePosition)) {

                    Log.w(TAG, "onBindViewHolder: update footer do nothing")

                } else {

                    val item = mSections[section].items!![relativePosition]
                    val vh = holder as ItemViewHolder

                    // name
                    BundleUtils.readBundle<String>(o, PAYLOAD_KEY_ITEM_NAME, { vh.name.text = it })

                    // thumbnail list
                    BundleUtils.readBundle<ArrayList<String>>(o, PAYLOAD_KEY_THUMB_LIST) { thumbFilePathList ->
                        updateThumbList(vh, true, object : ThumbnailListAdapter.OnItemClickListener {
                            override fun onItemClick(v: View, ii: ThumbnailListAdapter.Item, position: Int) {
                                if (mOnItemClickListener != null) {
                                    mOnItemClickListener!!.onItemClick(null, -1, item, -1)
                                }
                            }

                            override fun onItemLongClick(v: View, ii: ThumbnailListAdapter.Item, position: Int) {
                                if (mOnItemClickListener != null) {
                                    mOnItemClickListener!!.onItemLongClick(v, null, -1, item, -1)
                                }
                            }
                        }, Stream.of(thumbFilePathList).map<File>({ File(it) }).toList())
                    }

                    // Item sub type
                    BundleUtils.readBundle<ItemSubType>(o, PAYLOAD_KEY_SUB_TYPE) { itemSubType -> bindViewHolderBadge(vh, item) }

                    // conflict files
                    BundleUtils.readBundle<ArrayList<String>>(o, PAYLOAD_KEY_CONFLICT_FILES) { conflictFileList -> Log.w(TAG, "onBindViewHolder: do nothing for conflict file partial update : $conflictFileList") }

                    // selection
                    BundleUtils.readBundle<Boolean>(o, PAYLOAD_KEY_SELECTION) { selected ->

                        Log.d(TAG, "onBindViewHolder: partial update selected status, $section:$relativePosition selected : $selected")
                        vh.check.visibility = if (selected) View.VISIBLE else View.GONE
                    }

                    // Conflict file count
                    BundleUtils.readBundle<Int>(o, PAYLOAD_KEY_CONFLICT_FILES_COUNT) { conflictFileCount ->
                        if (conflictFileCount != -1) {
                            Log.d(TAG, String.format("onBindViewHolder: 目录更新文件冲突个数为 %d", conflictFileCount))
                            vh.setBadgeConflictCount(conflictFileCount!!)
                        }
                    }

                    // Count
                    BundleUtils.readBundle<Int>(o, PAYLOAD_KEY_COUNT) { count ->
                        if (count != -1) {
                            vh.count.text = count.toString()
                        }
                    }

                    // Name Keyword
                    val keywordStartIndex = o.getInt(PAYLOAD_KEY_KEYWORD_START_INDEX, -1)
                    val keywordLength = o.getInt(PAYLOAD_KEY_KEYWORD_LENGTH, -1)
                    if (isValidKeyword(item.getName(), keywordStartIndex, keywordLength)) {
                        vh.setHighlightName(keywordStartIndex, keywordLength, item.getName())
                    }
                }
            } else {
                Log.w(TAG, "onBindViewHolder: no bundle in list : $o")
            }
        }
    }


    // ---------------------------------------------------------------------------------------------
    // Item
    // ---------------------------------------------------------------------------------------------

    private fun isValidKeyword(name: String?, keywordStartIndex: Int, keywordLength: Int): Boolean {
        return keywordStartIndex >= 0 && keywordLength > 0 && keywordStartIndex < name!!.length
    }

    private fun bindViewHolderBadge(vh: ItemViewHolder, item: Item) {
        when (item.getItemSubType()) {

            SectionedFolderListAdapter.ItemSubType.ADD_ITEM -> {
                vh.badge!!.hide(false)
            }
            SectionedFolderListAdapter.ItemSubType.CONFLICT_COUNT -> vh.setBadgeConflictCount(cn.intret.app.picgo.utils.ListUtils.sizeOf(item.conflictFiles))
            SectionedFolderListAdapter.ItemSubType.NORMAL, SectionedFolderListAdapter.ItemSubType.SELECTED_COUNT -> vh.setBadgeSelectedCount(item.getSelectedCount())
            SectionedFolderListAdapter.ItemSubType.SOURCE_DIR -> if (mShowSourceDirBadgeWhenEmpty) {
                vh.showSourceDirBadge(item.getSelectedCount())
            } else {
                if (item.getSelectedCount() > 0) {
                    vh.showSourceDirBadge(item.getSelectedCount())
                } else {
                    vh.badge!!.hide(false)
                }
            }
            SectionedFolderListAdapter.ItemSubType.NONE -> vh.badge!!.hide(false)
        }

        if (item.getItemSubType() != ItemSubType.ADD_ITEM) {
            vh.count.visibility = View.VISIBLE
        }
    }

    private fun updateThumbList(vh: ItemViewHolder, forceUpdate: Boolean, onItemClickListener: ThumbnailListAdapter.OnItemClickListener, thumbList: List<File>?) {
        if (forceUpdate) {
            vh.mAdapter = null
        }

        if (vh.mAdapter == null) {

            vh.mAdapter = ThumbnailListAdapter(filesToItems(thumbList))
            vh.mAdapter!!.setOnClickListener(onItemClickListener)
            vh.thumbList.layoutManager = vh.layout
            vh.thumbList.adapter = vh.mAdapter
        } else {
            vh.thumbList.layoutManager = vh.layout
            vh.thumbList.swapAdapter(vh.mAdapter, false)
        }
    }

    private fun updateItemViewHolderCheckStatus(absolutePosition: Int, selected: Boolean) {
        Log.d(TAG, "updateItemViewHolderCheckStatus() called with: absolutePosition = [$absolutePosition], selected = [$selected]")

        if (mRecyclerView != null) {
            val selectedVH = mRecyclerView!!.findViewHolderForAdapterPosition(absolutePosition)
            if (selectedVH != null && selectedVH is ItemViewHolder) {
                selectedVH.check.visibility = if (selected) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateViewHolder(sectionIndex: Int, relativeIndex: Int, item: Item, consumer: DataConsumer2<ItemViewHolder, Item>?) {
        if (mRecyclerView != null) {
            val absolutePosition = getAbsolutePosition(sectionIndex, relativeIndex)
            val selectedVH = mRecyclerView!!.findViewHolderForAdapterPosition(absolutePosition)
            if (selectedVH != null && selectedVH is ItemViewHolder && consumer != null) {
                consumer.accept(selectedVH, item)
            }
        }
    }

    private fun filesToItems(thumbList: List<File>?): List<ThumbnailListAdapter.Item>? {
        return if (thumbList == null) {
            null
        } else Stream.of(thumbList).map { file -> ThumbnailListAdapter.Item().setFile(file) }.toList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        // Change inflated layout based on type
        when (viewType) {
            SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER -> {
                val v = LayoutInflater.from(parent.context)
                        .inflate(R.layout.folder_list_section_header, parent, false)
                return HeaderViewHolder(v)
            }
            else -> {

                val v = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_folder, parent, false)
                return ItemViewHolder(v)
            }
        }
    }

    override fun onViewRecycled(holder: SectionedViewHolder) {

        if (holder is ItemViewHolder) {
            holder.itemView.setTag(R.id.item, null)

            holder.mAdapter = null
            holder.mLinearLayoutManager = null
        }

        super.onViewRecycled(holder)
    }


    // ---------------------------------------------------------------------------------------------
    // Inner class
    // ---------------------------------------------------------------------------------------------


    open inner class HeaderViewHolder(itemView: View) : SectionedViewHolder(itemView), View.OnClickListener {

        @BindView(R.id.name)
        lateinit var name: TextView

        @BindView(R.id.option)
        lateinit var option: TextView

        init {

            ButterKnife.bind(this, itemView)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {

        }
    }

    open inner class ItemViewHolder(itemView: View) : SectionedViewHolder(itemView), View.OnClickListener {


        @BindView(R.id.check)
        lateinit var check: ImageView

        @BindView(R.id.title)
        lateinit var name: TextView

        @BindView(R.id.count)
        lateinit var count: TextView

        @BindView(R.id.thumb_list)
        lateinit var thumbList: RecyclerView

        @BindView(R.id.btn_close)
        lateinit var close: ImageView

        var badge: Badge? = null

        var mAdapter: ThumbnailListAdapter? = null
        var mLinearLayoutManager: LinearLayoutManager? = null

        val layout: RecyclerView.LayoutManager
            get() {
                if (mLinearLayoutManager == null) {
                    mLinearLayoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, true)
                }
                return mLinearLayoutManager as LinearLayoutManager
            }

        init {

            ButterKnife.bind(this, itemView)
            // Setup view holder. You'd want some views to be optional, e.g. the
            // header/footer will have views that normal item views do or do not have.
            //itemView.setOnClickListener(this);

            this.badge = QBadgeView(itemView.context)
                    .bindTarget(thumbList)
            val resources = itemView.context.resources
            badge!!.setBadgeGravity(Gravity.START or Gravity.TOP)
                    .setExactMode(true)
                    .setBadgeBackgroundColor(resources.getColor(R.color.colorAccent)).badgeTextColor = resources.getColor(android.R.color.white)
        }

        override fun onClick(v: View) {
            // SectionedViewHolder exposes methods such as:
            val isHeader = isHeader
            val isFooter = isFooter
            val position = relativePosition
            val section = position.section()
            val relativePos = position.relativePos()

            Log.d(TAG, "onClick: section $section relativePos $relativePos")

            val sectionItem = mSections[section]
            val item = sectionItem.items!![relativePos]

            // Single selection status


            // Notification
            if (mOnItemClickListener != null) {
                mOnItemClickListener!!.onItemClick(sectionItem, section, item, relativePos)
            }
        }

        fun setBadgeSelectedCount(selectedCount: Int) {
            if (badge != null) {
                if (selectedCount > 0) {
                    badge!!.badgeBackgroundColor = itemView.context.resources.getColor(R.color.colorAccent)
                    badge!!.badgeNumber = selectedCount
                } else {
                    badge!!.hide(false)
                }

            }
        }

        fun setBadgeConflictCount(conflictCount: Int) {
            if (badge != null) {
                if (conflictCount > 0) {
                    badge!!.badgeBackgroundColor = itemView.context.resources.getColor(R.color.warning)
                    badge!!.badgeText = itemView.context.resources.getString(R.string.conflict_d, conflictCount)
                } else {
                    badge!!.hide(false)
                }
            }
        }

        fun showSourceDirBadge() {
            val resources = itemView.context.resources
            badge!!.badgeText = resources.getString(R.string.source_folder)
            badge!!.badgeBackgroundColor = resources.getColor(R.color.colorAccent)
        }

        fun showSourceDirBadge(selectCount: Int) {
            val resources = itemView.context.resources
            if (selectCount > 0) {
                badge!!.badgeText = resources.getString(R.string.source_folder_d, selectCount)
            } else {
                badge!!.badgeText = resources.getString(R.string.source_folder)
            }
            badge!!.badgeBackgroundColor = resources.getColor(R.color.colorAccent)
        }

        @Deprecated("")
        fun setSelectedCountText(selectedCount: Int, count: Int) {
            if (selectedCount == COUNT_NONE || selectedCount == 0) {
                this.count.text = count.toString()
                this.count.setTextColor(this.count.context.resources.getColor(R.color.list_item_text_light))
                this.count.background = null
            } else {
                this.count.text = this.count.context.resources.getString(R.string.percent_d_d, selectedCount, count)
                this.count.setTextColor(this.count.context.resources.getColor(R.color.white))
                this.count.setBackgroundResource(R.drawable.badge)
            }
        }

        fun setHighlightName(keywordStartIndex: Int, keywordLength: Int, name: String?) {
            val nameSpan = SpannableString(name)
            nameSpan.setSpan(ForegroundColorSpan(this.name.context.resources.getColor(R.color.colorAccent)),
                    keywordStartIndex, keywordStartIndex + keywordLength,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            this.name.text = nameSpan
        }

        private val TAG = "SectionViewHolder"
    }

    companion object {

        private val TAG = SectionedFolderListAdapter::class.java.simpleName

        private val PAYLOAD_KEY_SECTION_NAME = "name"
        private val PAYLOAD_KEY_FILE = "file"
        private val PAYLOAD_KEY_ITEM_NAME = "item_name"
        private val PAYLOAD_KEY_THUMB_LIST = "item_thum_list"
        private val PAYLOAD_KEY_CONFLICT_FILES = "item_conflict_files"
        private val PAYLOAD_KEY_CONFLICT_FILES_COUNT = "item_conflict_files_count"
        private val PAYLOAD_KEY_SELECTION = "item_selection"
        private val PAYLOAD_KEY_COUNT = "item_count"
        private val PAYLOAD_KEY_KEYWORD_LENGTH = "item_keyword_lenght"
        private val PAYLOAD_KEY_KEYWORD_START_INDEX = "item_keyword_start_index"
        private val PAYLOAD_KEY_SUB_TYPE = "item_selection_source_dir"

        val COUNT_NONE = -1
    }
}

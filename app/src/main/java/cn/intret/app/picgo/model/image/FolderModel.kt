package cn.intret.app.picgo.model.image

import java.io.File
import java.util.LinkedList
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.toList

/**
 * 表示图片目录的数据模型，一个父文件夹包含多个‘含有图片的文件夹’
 */
class FolderModel : Cloneable {

    /**
     * 数据模型是否是使用 T9 过滤得到的结果
     */
    internal var mIsT9FilterMode = false

    internal var mContainerFolders: MutableList<ContainerFolder>? = LinkedList()

    val containerFolders: List<ContainerFolder>?
        get() = mContainerFolders

    fun isT9FilterMode(): Boolean {
        return mIsT9FilterMode
    }

    fun setT9FilterMode(t9FilterMode: Boolean): FolderModel {
        mIsT9FilterMode = t9FilterMode
        return this
    }

    /**
     * TODO merge with class [ImageFolder]
     */
    class ContainerFolder : Cloneable {
        internal lateinit var mFile: File
        internal lateinit var mName: String

        internal var mFolders: MutableList<ImageFolder>? = null

        val folders: List<ImageFolder>
            get() = mFolders?.toList() ?: LinkedList()

        fun setFolders(folders: List<ImageFolder>): ContainerFolder {
            if (mFolders == null) {
                mFolders = ArrayList<ImageFolder>()
            }

            mFolders?.clear()
            mFolders?.addAll(folders)

            return this
        }

        fun getFile(): File {
            return mFile
        }

        fun setFile(file: File): ContainerFolder {
            mFile = file
            return this
        }

        fun getName(): String {
            return mName
        }

        fun setName(name: String): ContainerFolder {
            mName = name
            return this
        }

        @Throws(CloneNotSupportedException::class)
        public override fun clone(): Any {
            val clone = super.clone() as ContainerFolder
            clone.setFile(File(mFile.absolutePath))
            if (mFolders != null) {
                val newFolders = LinkedList<ImageFolder>()
                var i = 0
                val mFoldersSize = mFolders!!.size
                while (i < mFoldersSize) {
                    val folder = mFolders!![i]
                    newFolders.add(folder.clone() as ImageFolder)
                    i++
                }
                clone.setFolders(newFolders)
            }

            return clone
        }
    }

    fun addFolderSection(section: ContainerFolder) {
        mContainerFolders!!.add(section)
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        val clone = super.clone() as FolderModel
        if (mContainerFolders != null) {
            val containerFolders = LinkedList<ContainerFolder>()
            var i = 0
            val mParentFolderInfosSize = mContainerFolders!!.size
            while (i < mParentFolderInfosSize) {
                val containerFolder = mContainerFolders!![i]
                containerFolders.add(containerFolder.clone() as ContainerFolder)
                i++
            }
            clone.mContainerFolders = containerFolders
        }
        return clone
    }
}

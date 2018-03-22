package cn.intret.app.picgo.model.image.data

import java.io.File
import java.util.LinkedList
import kotlin.collections.ArrayList

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
        internal lateinit var file: File
        internal lateinit var name: String

        internal var subFolders: MutableList<ImageFolder>? = null

        fun setFolders(folders: List<ImageFolder>): ContainerFolder {
            if (this.subFolders == null) {
                this.subFolders = ArrayList<ImageFolder>()
            }

            this.subFolders?.clear()
            this.subFolders?.addAll(folders)

            return this
        }

        fun setFile(file: File): ContainerFolder {
            this.file = file
            return this
        }

        fun setName(name: String): ContainerFolder {
            this.name = name
            return this
        }

        @Throws(CloneNotSupportedException::class)
        public override fun clone(): Any {
            val clone = super.clone() as ContainerFolder
            clone.file = File(file.absolutePath)
            clone.subFolders = this.subFolders?.let {
                it.map { it.clone() as ImageFolder }.toMutableList()
            }
            return clone
        }

        override fun toString(): String {
            return "ContainerFolder(file=$file, name='$name', subFolders=${this.subFolders})"
        }

    }

    fun addFolderSection(section: ContainerFolder) {
        mContainerFolders!!.add(section)
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        val clone = super.clone() as FolderModel
        clone.mContainerFolders = this.mContainerFolders?.let {
            it.map { it.clone() as ContainerFolder }.toMutableList()
        }
        clone.mIsT9FilterMode = this.mIsT9FilterMode
        return clone
    }

    override fun toString(): String {
        return "FolderModel(mIsT9FilterMode=$mIsT9FilterMode, mContainerFolders=$mContainerFolders)"
    }


}

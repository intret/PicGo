package cn.intret.app.picgo.model.image


import com.t9search.model.PinyinSearchUnit
import com.t9search.util.PinyinUtil
import java.io.File


/**
 * 图片文件夹数据模型。
 */
open class ImageFolder : Cloneable {
    /**
     * Folder file
     */
    lateinit var file: File

    /*
     * File count in folder
     */
    var count: Int = 0

    /**
     * Folder name
     */
    var name: String? = null

    /**
     * Thumbnail image file list
     */
    var thumbnailList: MutableList<File>? = null

    /**
     * sub folder list which each item contains image files
     */
    var subFolders: MutableList<ImageFolder>? = null

    /**
     * Media file list, contains video/image files
     */
    var mediaFiles: Array<File>? = null

    // ------------------------------------------------
    // T9 键盘过滤信息
    // ------------------------------------------------


    var pinyinSearchUnit: PinyinSearchUnit? = null

    var matchKeywords: StringBuffer? = StringBuffer()
        private set        //Used to save the type of Match Keywords.(name or phoneNumber)


    /**
     * the match start  position of mMatchKeywords in original string(name or phoneNumber).
     */
    var matchStartIndex: Int = 0

    /**
     * the match length of mMatchKeywords in original string(name or phoneNumber).
     */
    var matchLength: Int = 0

    fun setNameAndCreateSearchUnit(name: String?): ImageFolder {

        this.name = name

        // Generate Pinyin search data
        pinyinSearchUnit = PinyinSearchUnit(name)
        PinyinUtil.parse(pinyinSearchUnit)

        matchKeywords = StringBuffer()
        matchStartIndex = -1
        matchLength = 0

        return this
    }


    fun setMatchKeywords(matchKeywords: String?) {
        this.matchKeywords?.let {
            it.delete(0, it.length)
            it.append(matchKeywords)
        }
    }

    fun clearMatchKeywords() {
        matchKeywords?.let { it.delete(0, it.length) }
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        val clone = super.clone() as ImageFolder
        clone.matchKeywords = StringBuffer(this.matchKeywords.toString())
        clone.pinyinSearchUnit = this.pinyinSearchUnit?.clone() as PinyinSearchUnit? ?: PinyinSearchUnit()
        clone.file = File(this.file.absolutePath)
        clone.setNameAndCreateSearchUnit(this.name)

        clone.thumbnailList = this.thumbnailList?.let {
            it.map { File(it.absolutePath) }.toMutableList()
        }

        return clone
    }

    override fun toString(): String {
        return "ImageFolder(count=$count, name=$name, mediaFiles=${mediaFiles?.size
                ?: 0}, pinyinSearchUnit=$pinyinSearchUnit, matchKeywords=$matchKeywords, matchStartIndex=$matchStartIndex, matchLength=$matchLength)"
    }

}

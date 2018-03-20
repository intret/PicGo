package cn.intret.app.picgo.model.image


import com.t9search.model.PinyinSearchUnit
import com.t9search.util.PinyinUtil
import java.io.File
import java.util.*


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

    internal var mThumbList: MutableList<File>? = null
    internal var mMediaFiles: Array<File>? = null

    /*
     * T9 键盘过滤信息
     */
    internal var mPinyinSearchUnit: PinyinSearchUnit? = null

    var matchKeywords: StringBuffer? = StringBuffer()
        private set        //Used to save the type of Match Keywords.(name or phoneNumber)
    private var mMatchStartIndex: Int = 0                //the match start  position of mMatchKeywords in original string(name or phoneNumber).
    private var mMatchLength: Int = 0                    //the match length of mMatchKeywords in original string(name or phoneNumber).

    var thumbList: MutableList<File>?
        get() = mThumbList
        set(value) {mThumbList = value}



    fun getPinyinSearchUnit(): PinyinSearchUnit? {
        return mPinyinSearchUnit
    }

    fun setPinyinSearchUnit(pinyinSearchUnit: PinyinSearchUnit?): ImageFolder {
        mPinyinSearchUnit = pinyinSearchUnit
        return this
    }

    fun getMediaFiles(): Array<File>? {
        return mMediaFiles
    }

    fun setMediaFiles(mediaFiles: Array<File>?): ImageFolder {
        mMediaFiles = mediaFiles
        return this
    }

    fun setThumbList(thumbList: MutableList<File>?): ImageFolder {
        this.mThumbList = thumbList
        return this
    }


    fun setName(name: String?): ImageFolder {
        this.name = name

        // Generate Pinyin search data
        setPinyinSearchUnit(PinyinSearchUnit(name))
        PinyinUtil.parse(mPinyinSearchUnit)

        matchKeywords = StringBuffer()
        matchKeywords!!.delete(0, matchKeywords!!.length)
        setMatchStartIndex(-1)
        setMatchLength(0)

        return this
    }


    fun setMatchKeywords(matchKeywords: String?) {
        this.matchKeywords?.let {
            it.delete(0, it.length)
            it.append(matchKeywords)
        }
    }

    fun clearMatchKeywords() {
        matchKeywords?.let { it.delete(0, it.length)}
    }

    fun getMatchStartIndex(): Int {
        return mMatchStartIndex
    }

    fun setMatchStartIndex(matchStartIndex: Int): ImageFolder {
        mMatchStartIndex = matchStartIndex
        return this
    }

    fun getMatchLength(): Int {
        return mMatchLength
    }

    fun setMatchLength(matchLength: Int): ImageFolder {
        mMatchLength = matchLength
        return this
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        val clone = super.clone() as ImageFolder
        clone.matchKeywords = StringBuffer(this.matchKeywords!!.toString())
        clone.setPinyinSearchUnit(this.mPinyinSearchUnit?.clone() as PinyinSearchUnit? ?:PinyinSearchUnit())
        clone.file = File(this.file.absolutePath)
        clone.name = this.name


        if (mThumbList != null) {
            clone.mThumbList = LinkedList()
            var i = 0
            val mThumbListSize = mThumbList!!.size
            while (i < mThumbListSize) {
                val file = mThumbList!![i]
                clone.mThumbList!!.add(File(file.absolutePath))
                i++
            }
        }
        return clone
    }
}

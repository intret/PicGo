package cn.intret.app.picgo.model.image.data

import android.util.Size
import java.io.File
import java.util.*

/**
 * Media file (image/video)
 */
class MediaFile {


    var file: File? = null
    internal lateinit var date: Date

    /**
     * Image size or video resolution
     */
    internal lateinit var mediaResolution: Size

    /**
     * File size ( disk usage of file), in bytes
     */
    internal var fileSize: Long = 0


    internal var videoDuration: Long = 0

    /**
     * Get duration, in ms
     *
     * @return
     */
    fun getVideoDuration(): Long {
        return videoDuration
    }

    fun setVideoDuration(videoDuration: Long): MediaFile {
        this.videoDuration = videoDuration
        return this
    }

    fun getFileSize(): Long {
        return fileSize
    }

    fun setFileSize(fileSize: Long): MediaFile {
        this.fileSize = fileSize
        return this
    }

    fun getMediaResolution(): Size {
        return mediaResolution
    }

    fun setMediaResolution(mediaResolution: Size): MediaFile {
        this.mediaResolution = mediaResolution
        return this
    }

    fun getDate(): Date {
        return date
    }

    fun setDate(date: Date): MediaFile {
        this.date = date
        return this
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is MediaFile) return false

        val mediaFile = o as MediaFile?

        return if (file != null) file == mediaFile!!.file else mediaFile!!.file == null

    }

    override fun hashCode(): Int {
        return if (file != null) file!!.hashCode() else 0
    }

    override fun toString(): String {
        return "MediaFile{" +
                "file=" + file +
                ", date=" + date +
                ", mediaResolution=" + mediaResolution +
                ", fileSize=" + fileSize +
                ", videoDuration=" + videoDuration +
                '}'.toString()
    }

    companion object {

        // TODO: add sort by resolution
        val MEDIA_FILE_DATE_DESC_COMPARATOR = Comparator<MediaFile>{ o1: MediaFile, o2: MediaFile -> o2.getDate().compareTo(o1.getDate()) }
        val MEDIA_FILE_DATE_ASC_COMPARATOR = Comparator<MediaFile>{ o1: MediaFile, o2: MediaFile -> o1.getDate().compareTo(o2.getDate()) }

        val MEDIA_FILE_NAME_ASC_COMPARATOR = Comparator<MediaFile>{ o1: MediaFile, o2: MediaFile -> o1.file!!.compareTo(o2.file) }

        val MEDIA_FILE_NAME_DESC_COMPARATOR = Comparator<MediaFile>{ o1: MediaFile, o2: MediaFile -> o2.file!!.compareTo(o1.file) }

        val MEDIA_FILE_LENGTH_DESC_COMPARATOR = Comparator<MediaFile>{ o1: MediaFile, o2: MediaFile -> java.lang.Long.compare(o2.getFileSize(), o1.getFileSize()) }


        val MEDIA_FILE_LENGTH_ASC_COMPARATOR = Comparator<MediaFile>{ o1: MediaFile, o2: MediaFile -> java.lang.Long.compare(o1.getFileSize(), o2.getFileSize()) }
    }
}

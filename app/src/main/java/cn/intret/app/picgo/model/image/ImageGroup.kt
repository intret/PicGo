package cn.intret.app.picgo.model.image

import java.util.Date

/**
 * Image Group
 */

class ImageGroup {

    internal lateinit var startDate: Date
    internal lateinit var endDate: Date
    internal lateinit var mediaFiles: List<MediaFile>

    fun getStartDate(): Date {
        return startDate
    }

    fun setStartDate(startDate: Date): ImageGroup {
        this.startDate = startDate
        return this
    }

    fun getEndDate(): Date {
        return endDate
    }

    fun setEndDate(endDate: Date): ImageGroup {
        this.endDate = endDate
        return this
    }

    fun getMediaFiles(): List<MediaFile> {
        return mediaFiles
    }

    fun setMediaFiles(mediaFiles: List<MediaFile>): ImageGroup {
        this.mediaFiles = mediaFiles
        return this
    }

    override fun toString(): String {
        return "ImageGroup{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                '}'.toString()
    }
}

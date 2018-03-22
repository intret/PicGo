package cn.intret.app.picgo.model.image.data


import android.media.ExifInterface
import android.util.Size

import java.io.File

class ImageFileInformation {
    var file: File? = null
    var exif: ExifInterface? = null
    var lastModified: Long = 0
    var fileLength: Long = 0
    var imageWidth: Int = 0
    var imageHeight: Int = 0
    /**
     * 图片分辨率、视频分辨率
     */
    var mediaResolution: Size? = null
    /**
     * 视频播放时长
     */
    var videoDuration: Int = 0

    fun setExif(exif: ExifInterface): ImageFileInformation {
        this.exif = exif
        return this
    }

    fun setLastModified(lastModified: Long): ImageFileInformation {
        this.lastModified = lastModified
        return this
    }

    fun setFileLength(fileLength: Long): ImageFileInformation {
        this.fileLength = fileLength
        return this
    }



}

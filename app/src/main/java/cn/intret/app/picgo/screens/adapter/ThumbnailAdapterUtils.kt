package cn.intret.app.picgo.screens.adapter

import java.io.File


object ThumbnailAdapterUtils {
    fun filesToItems(thumbList: List<File>?): List<ThumbnailListAdapter.Item>? {
        return thumbList?.map { ThumbnailListAdapter.Item().apply { setFile(it) } }?.toList()
    }
}

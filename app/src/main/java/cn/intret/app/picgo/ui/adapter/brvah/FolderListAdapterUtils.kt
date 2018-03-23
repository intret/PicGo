package cn.intret.app.picgo.ui.adapter.brvah

import cn.intret.app.picgo.model.image.data.FolderModel
import cn.intret.app.picgo.model.image.data.ImageFolder
import cn.intret.app.picgo.ui.adapter.SectionedFolderListAdapter
import cn.intret.app.picgo.utils.whenNotNullNorEmpty
import com.annimon.stream.Stream

object FolderListAdapterUtils {

    fun folderModelToSectionedFolderListAdapter(model: FolderModel): SectionedFolderListAdapter {
        val sections = model.containerFolders?.map(this::parentFolderToItem)
        val mutableList = sections?.toMutableList()
                ?: mutableListOf<SectionedFolderListAdapter.Section>()

        return SectionedFolderListAdapter(mutableList).setHighlightItemName(model.mIsT9FilterMode)
    }

    private fun parentFolderToItem(containerFolder: FolderModel.ContainerFolder): SectionedFolderListAdapter.Section {
        return SectionedFolderListAdapter.Section().apply {
            name = containerFolder.name
            file = containerFolder.file
            items = containerFolder.subFolders
                    ?.map { item ->
                        SectionedFolderListAdapter.Item().apply {
                            this.mFile = item.file
                            this.mName = item.name
                            this.mKeywordStartIndex = item.matchStartIndex
                            this.mKeywordLength = item.matchLength
                            this.mCount = item.count
                            setThumbList(item.thumbnailList)
                        }
                    }
                    ?.toMutableList()
        }
    }

    fun imageFolderToFolderListAdapterItem(imageFolder: ImageFolder): FolderListAdapter.Item {
        return FolderListAdapter.Item()
                .apply {
                    name = imageFolder.name
                    count = imageFolder.count.toLong()
                    directory = imageFolder.file
                    thumbList = imageFolder.thumbnailList
                }
    }

    fun imageFolderToExpandableFolderItem(folder: ImageFolder): FolderItem {
        return FolderItem()
                .apply {
                    file = folder.file
                    count = folder.count
                    setNameAndCreateSearchUnit(folder.name)
                    title = folder.name
                    thumbnailList = folder.thumbnailList
                    setMatchKeywords(folder.matchKeywords?.toString())
                    matchLength = folder.matchLength
                    matchStartIndex = folder.matchStartIndex
                    mediaFiles = folder.mediaFiles
                    pinyinSearchUnit = folder.pinyinSearchUnit
                }
    }

    fun containerFolderToFolderSection(containerFolder: FolderModel.ContainerFolder): FolderSection {
        return FolderSection().apply {
            setTitle(containerFolder.name)
            file = containerFolder.file

            val folders = containerFolder.subFolders
            val folderItems = Stream.of(folders)
                    .map { imageFolderToExpandableFolderItem(it) }
                    .toList()

            subItems = folderItems
        }
    }

    fun folderModelToExpandableFolderAdapter(model: FolderModel): ExpandableFolderAdapter {
        val items = model.containerFolders.whenNotNullNorEmpty { it.map { containerFolderToFolderSection(it) } }
        return ExpandableFolderAdapter(items)
    }
}

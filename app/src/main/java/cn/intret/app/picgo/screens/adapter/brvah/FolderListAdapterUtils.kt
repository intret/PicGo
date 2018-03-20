package cn.intret.app.picgo.screens.adapter.brvah

import com.annimon.stream.Stream
import com.chad.library.adapter.base.entity.MultiItemEntity

import java.util.LinkedList

import cn.intret.app.picgo.model.image.FolderModel
import cn.intret.app.picgo.model.image.ImageFolder
import cn.intret.app.picgo.screens.adapter.SectionedFolderListAdapter

object FolderListAdapterUtils {

    fun folderModelToSectionedFolderListAdapter(model: FolderModel): SectionedFolderListAdapter {
        val sections = LinkedList<SectionedFolderListAdapter.Section>()

        val containerFolders = model.containerFolders
        var i = 0
        val s = containerFolders!!.size
        while (i < s) {
            sections.add(parentFolderToItem(containerFolders[i]))
            i++
        }

        return SectionedFolderListAdapter(sections).setHighlightItemName(model.isT9FilterMode())
    }

    private fun parentFolderToItem(containerFolder: FolderModel.ContainerFolder): SectionedFolderListAdapter.Section {
        val section = SectionedFolderListAdapter.Section()

        return section.setName(containerFolder.getName())
                .setFile(containerFolder.getFile())
                .setItems(
                        Stream.of(containerFolder.folders)
                                .map { item ->
                                    SectionedFolderListAdapter.Item()
                                            .setFile(item.file)
                                            .setName(item.name)
                                            .setKeywordStartIndex(item.getMatchStartIndex())
                                            .setKeywordLength(item.getMatchLength())
                                            .setCount(item.count)
                                            .setThumbList(item.thumbList)
                                }
                                .toList()
                )
    }

    fun imageFolderToFolderListAdapterItem(imageFolder: ImageFolder): FolderListAdapter.Item {
        return FolderListAdapter.Item()
                .setName(imageFolder.name)
                .setCount(imageFolder.count.toLong())
                .setDirectory(imageFolder.file)
                .setThumbList(imageFolder.thumbList)
    }

    fun imageFolderToExpandableFolderItem(folder: ImageFolder): FolderItem {
        val folderItem: FolderItem
        folderItem = FolderItem()
        folderItem.file = folder.file
        folderItem.count = folder.count
        folderItem.setName(folder.name)
        folderItem.setTitle(folder.name)
        folderItem.setThumbList(folder.thumbList)
        folderItem.setMatchKeywords(folder.matchKeywords?.toString())
        folderItem.setMatchLength(folder.getMatchLength())
        folderItem.setMatchStartIndex(folder.getMatchStartIndex())
        folderItem.setMediaFiles(folder.getMediaFiles())
        folderItem.setPinyinSearchUnit(folder.getPinyinSearchUnit())
        return folderItem
    }

    fun containerFolderToFolderSection(containerFolder: FolderModel.ContainerFolder): FolderSection {
        val folderSection: FolderSection
        folderSection = FolderSection()
                .setTitle(containerFolder.getName())
                .setFile(containerFolder.getFile())

        val folders = containerFolder.folders

        val folderItems = Stream.of(folders)
                .map { imageFolderToExpandableFolderItem(it) }
                .toList()

        folderSection.subItems = folderItems
        return folderSection
    }

    fun folderModelToExpandableFolderAdapter(model: FolderModel): ExpandableFolderAdapter {
        val data = LinkedList<MultiItemEntity>()
        val containerFolders = model.containerFolders
        var i = 0
        val containerFoldersSize = containerFolders!!.size
        while (i < containerFoldersSize) {
            val containerFolder = containerFolders[i]

            val folderSection = containerFolderToFolderSection(containerFolder)
            data.add(folderSection)
            i++
        }

        return ExpandableFolderAdapter(data)
    }
}

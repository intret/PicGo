package cn.intret.app.picgo.screens.adapter.brvah

import cn.intret.app.picgo.model.image.FolderModel
import cn.intret.app.picgo.model.image.ImageFolder
import cn.intret.app.picgo.screens.adapter.SectionedFolderListAdapter
import cn.intret.app.picgo.utils.whenNotNullNorEmpty
import com.annimon.stream.Stream
import java.util.*

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

        return section.setName(containerFolder.mName)
                .setFile(containerFolder.file)
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
        return FolderItem()
                .apply {
                    file = folder.file
                    count = folder.count
                    setNameAndCreateSearchUnit(folder.name)
                    setTitle(folder.name)
                    setThumbList(folder.thumbList)
                    setMatchKeywords(folder.matchKeywords?.toString())
                    setMatchLength(folder.getMatchLength())
                    setMatchStartIndex(folder.getMatchStartIndex())
                    setMediaFiles(folder.getMediaFiles())
                    setPinyinSearchUnit(folder.getPinyinSearchUnit())
                }
    }

    fun containerFolderToFolderSection(containerFolder: FolderModel.ContainerFolder): FolderSection {
        return FolderSection().apply {
            setTitle(containerFolder.mName)
            file = containerFolder.file

            val folders = containerFolder.folders
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

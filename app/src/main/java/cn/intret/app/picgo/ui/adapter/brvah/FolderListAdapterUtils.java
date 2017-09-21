package cn.intret.app.picgo.ui.adapter.brvah;

import android.support.annotation.NonNull;

import com.annimon.stream.Stream;
import com.chad.library.adapter.base.entity.MultiItemEntity;

import java.util.LinkedList;
import java.util.List;

import cn.intret.app.picgo.model.FolderModel;
import cn.intret.app.picgo.model.ImageFolder;
import cn.intret.app.picgo.ui.adapter.SectionedFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.brvah.FolderListAdapter;

public class FolderListAdapterUtils {

    @NonNull
    public static SectionedFolderListAdapter folderModelToSectionedFolderListAdapter(FolderModel model) {
        List<SectionedFolderListAdapter.Section> sections = new LinkedList<>();

        List<FolderModel.ContainerFolder> containerFolders = model.getContainerFolders();
        for (int i = 0, s = containerFolders.size(); i < s; i++) {
            sections.add(parentFolderToItem(containerFolders.get(i)));
        }

        return new SectionedFolderListAdapter(sections).setShowInFilterMode(model.isT9FilterMode());
    }

    private static SectionedFolderListAdapter.Section parentFolderToItem(FolderModel.ContainerFolder containerFolder) {
        SectionedFolderListAdapter.Section section = new SectionedFolderListAdapter.Section();

        return section.setName(containerFolder.getName())
                .setFile(containerFolder.getFile())
                .setItems(
                        Stream.of(containerFolder.getFolders())
                                .map(item -> new SectionedFolderListAdapter.Item()
                                        .setFile(item.getFile())
                                        .setName(item.getName())
                                        .setKeywordStartIndex(item.getMatchStartIndex())
                                        .setKeywordLength(item.getMatchLength())
                                        .setCount(item.getCount())
                                        .setThumbList(item.getThumbList())
                                )
                                .toList()
                );
    }

    public static FolderListAdapter.Item imageFolderToFolderListAdapterItem(ImageFolder imageFolder) {
        return new FolderListAdapter.Item()
                .setName(imageFolder.getName())
                .setCount(imageFolder.getCount())
                .setDirectory(imageFolder.getFile())
                .setThumbList(imageFolder.getThumbList())
                ;
    }

    @NonNull
    public static FolderItem imageFolderToExpandableFolderItem(ImageFolder folder) {
        FolderItem folderItem;
        folderItem = new FolderItem();
        folderItem.setFile(folder.getFile());
        folderItem.setCount(folder.getCount());
        folderItem.setName(folder.getName());
        folderItem.setTitle(folder.getName());
        folderItem.setThumbList(folder.getThumbList());
        folderItem.setMatchKeywords(folder.getMatchKeywords() == null ? null : folder.getMatchKeywords().toString());
        folderItem.setMatchLength(folder.getMatchLength());
        folderItem.setMatchStartIndex(folder.getMatchStartIndex());
        folderItem.setMediaFiles(folder.getMediaFiles());
        folderItem.setPinyinSearchUnit(folder.getPinyinSearchUnit());
        return folderItem;
    }

    @NonNull
    public static FolderSection containerFolderToFolderSection(FolderModel.ContainerFolder containerFolder) {
        FolderSection folderSection;
        folderSection = new FolderSection()
                .setTitle(containerFolder.getName())
                .setFile(containerFolder.getFile());

        List<ImageFolder> folders = containerFolder.getFolders();

        List<FolderItem> folderItems = Stream.of(folders)
                .map(FolderListAdapterUtils::imageFolderToExpandableFolderItem)
                .toList();

        folderSection.setSubItems(folderItems);
        return folderSection;
    }

    public static ExpandableFolderAdapter folderModelToExpandableFolderAdapter(FolderModel model) {
        List<MultiItemEntity> data = new LinkedList<>();
        List<FolderModel.ContainerFolder> containerFolders = model.getContainerFolders();
        for (int i = 0, containerFoldersSize = containerFolders.size(); i < containerFoldersSize; i++) {
            FolderModel.ContainerFolder containerFolder = containerFolders.get(i);

            FolderSection folderSection = containerFolderToFolderSection(containerFolder);
            data.add(folderSection);
        }

        return new ExpandableFolderAdapter(data);
    }
}

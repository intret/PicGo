package cn.intret.app.picgo.ui.adapter;

import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import java.util.LinkedList;
import java.util.List;

import cn.intret.app.picgo.model.FolderModel;

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
}

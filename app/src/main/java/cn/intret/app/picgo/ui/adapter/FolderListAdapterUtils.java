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

        List<FolderModel.ParentFolderInfo> parentFolderInfos = model.getParentFolderInfos();
        for (int i = 0, s = parentFolderInfos.size(); i < s; i++) {
            sections.add(parentFolderToItem(parentFolderInfos.get(i)));
        }

        return new SectionedFolderListAdapter(sections).setShowInFilterMode(model.isT9FilterMode());
    }

    private static SectionedFolderListAdapter.Section parentFolderToItem(FolderModel.ParentFolderInfo parentFolderInfo) {
        SectionedFolderListAdapter.Section section = new SectionedFolderListAdapter.Section();

        return section.setName(parentFolderInfo.getName())
                .setFile(parentFolderInfo.getFile())
                .setItems(
                        Stream.of(parentFolderInfo.getFolders())
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

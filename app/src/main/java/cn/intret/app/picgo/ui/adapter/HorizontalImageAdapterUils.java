package cn.intret.app.picgo.ui.adapter;

import com.annimon.stream.Stream;

import java.io.File;
import java.util.List;


public class HorizontalImageAdapterUils {
    public static List<HorizontalImageListAdapter.Item> filesToItems(List<File> thumbList) {
        if (thumbList == null) {
            return null;
        }
        return Stream.of(thumbList).map(file -> new HorizontalImageListAdapter.Item().setFile(file)).toList();
    }
}

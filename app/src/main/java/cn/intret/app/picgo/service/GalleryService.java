package cn.intret.app.picgo.service;

import android.support.v7.util.SortedList;

import com.annimon.stream.function.IndexedPredicate;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import cn.intret.app.picgo.utils.SystemUtils;
import io.reactivex.Observable;
import io.reactivex.functions.BiConsumer;

/**
 * Gallery images service
 */

public class GalleryService {
    private static final GalleryService ourInstance = new GalleryService();

    public static GalleryService getInstance() {
        return ourInstance;
    }

    private GalleryService() {
    }

    public List<File> loadLatestPictures(int count) throws FileNotFoundException {
        File cameraDir = SystemUtils.getCameraDir();
        if (cameraDir == null) {
            throw new FileNotFoundException("Cannot found camera directory.");
        }

        File[] allFiles = cameraDir.listFiles((file) -> file.isFile() && !file.getName().startsWith("."));
        if (allFiles == null) {
            return new LinkedList<File>();
        }

        final Object[] selectedFiles = {new LinkedList<>()};

        List<File> files1 = com.annimon.stream.Stream.of(allFiles)
                .sorted((file, file2) -> Long.compare(file2.lastModified(), file.lastModified()))
                .takeUntilIndexed(0, 1, (index, value) -> {
                    if (index == count) {
                        return true;
                    }
                    return false;
                })
                .toList();
        return files1;
//        Observable.fromArray(allFiles)
//                .sorted( )
//                .take(count)
//                .toList()
//                .subscribe((files, throwable) -> {
//                    selectedFiles[0] = files;
//                });

//        return (List<File>) selectedFiles[0];
    }
}

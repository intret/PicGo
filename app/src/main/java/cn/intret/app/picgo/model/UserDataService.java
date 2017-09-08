package cn.intret.app.picgo.model;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import cn.intret.app.picgo.model.event.RecentOpenFolderListChangeMessage;
import io.reactivex.Observable;

public class UserDataService {
    EventBus mEventBus = EventBus.getDefault();

    int mMoveFileDialogFirstVisibleItemPosition = 0;

    public int getMoveFileDialogFirstVisibleItemPosition() {
        return mMoveFileDialogFirstVisibleItemPosition;
    }

    public UserDataService setMoveFileDialogFirstVisibleItemPosition(int moveFileDialogFirstVisibleItemPosition) {
        mMoveFileDialogFirstVisibleItemPosition = moveFileDialogFirstVisibleItemPosition;
        return this;
    }

    public static final String TAG = "UserDataService";

    private static final UserDataService ourInstance = new UserDataService();

    public static UserDataService getInstance() {
        return ourInstance;
    }

    private UserDataService() {
    }

    List<File> mRecentOpenFolders = new LinkedList<>();

    public Observable<Boolean> addRecentOpenFolder(File dir) {
        return Observable.create(e -> {
            if (dir == null) {
                throw new IllegalArgumentException("Argument 'dir' is null.");
            }

            boolean remove = mRecentOpenFolders.remove(dir);
            if (remove) {
                Log.d(TAG, "addRecentOpenFolder: removed dir " + dir);
            }

            // Add to list's top position
            mRecentOpenFolders.add(0, dir);

            mEventBus.post(new RecentOpenFolderListChangeMessage());

            e.onNext(true);
            e.onComplete();
        });
    }

    public Observable<List<File>> getRecentOpenFolders() {
        return Observable.just(mRecentOpenFolders);
    }
}

package cn.intret.app.picgo.model;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.annimon.stream.Stream;
import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;

import cn.intret.app.picgo.model.event.RecentOpenFolderListChangeMessage;
import cn.intret.app.picgo.model.event.RenameDirectoryMessage;
import cn.intret.app.picgo.utils.Action1;
import cn.intret.app.picgo.utils.ListUtils;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class UserDataService extends BaseService {

    public static final String PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY = "folder_access_recent_history";
    private final int mMaxRecentHistorySize = 10;
    RxSharedPreferences mPreferences;

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
        super();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPreferences = RxSharedPreferences.create(preferences);

        mEventBus.register(this);

        validateRecentFolderList();
    }

    private void validateRecentFolderList() {
        loadRecentOpenFolders(false)
                .subscribeOn(Schedulers.io())
                .subscribe(recentRecords -> {
                    List<RecentRecord> invalidRecordList = Stream.of(recentRecords)
                            .filter(record -> !new File(record.getFilePath()).exists())
                            .toList();

                    if (!invalidRecordList.isEmpty()) {

                        Preference<String> prefRecent = getJsonStringPreference(PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY);
                        Log.w(TAG, "validateRecentFolderList: some folder exist : " + invalidRecordList);

                        prefRecent.set(new Gson().toJson(invalidRecordList));
                    }
                });
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(RenameDirectoryMessage message) {

        Log.d(TAG, "onEvent() called with: message = [" + message + "]");

        renameOpenFolderRecentRecord(message.getOldDirectory(), message.getNewDirectory())
        .subscribe(ok -> {
            Log.d(TAG, "onEvent: rename recent record success : " + message.getOldDirectory());
        });

    }
    private List<RecentRecord> jsonToHistoryList(String json) {
        // https://stackoverflow.com/questions/5554217/google-gson-deserialize-listclass-object-generic-type
        Type listType = new TypeToken<List<RecentRecord>>() {
        }.getType();
        List<RecentRecord> recentRecords = new Gson().fromJson(json, listType);
        return recentRecords;
    }

    private String recentHistoryListToJson(List<RecentRecord> recentRecords) {
        return new Gson().toJson(recentRecords);
    }

    public Observable<Boolean> addOpenFolderRecentRecord(File dir) {

        return Observable.create(e -> {
            if (dir == null) {
                throw new IllegalArgumentException("Argument 'dir' is null.");
            }

            List<RecentRecord> saveRecentList = updateOpenFolderRecentPreference(dir, recentRecords -> {

                boolean remove = cn.intret.app.picgo.utils.ListUtils.removeListElement(
                        recentRecords,
                        record -> record.getFilePath() != null && record.getFilePath().equals(dir.getAbsolutePath()));
                if (remove) {
                    Log.d(TAG, "addOpenFolderRecentRecord: remove element in list : " + dir.getAbsolutePath());
                }
                recentRecords.add(0, new RecentRecord().setFilePath(dir.getAbsolutePath()));

                return Stream.of(recentRecords).limit(mMaxRecentHistorySize).toList();
            });

            mEventBus.post(new RecentOpenFolderListChangeMessage().setRecentRecord(saveRecentList));

            e.onNext(true);
            e.onComplete();
        });
    }

    public Observable<Boolean> renameOpenFolderRecentRecord(File dir, File newDir) {

        return Observable.create(e -> {
            if (dir == null) {
                throw new IllegalArgumentException("Argument 'dir' is null.");
            }

            List<RecentRecord> saveRecentList = updateOpenFolderRecentPreference(dir, recentRecords -> {

                for (RecentRecord recentRecord : recentRecords) {
                    if (recentRecord.getFilePath() != null) {
                        if (recentRecord.getFilePath().equals(dir.getAbsolutePath())) {
                            Log.d(TAG, "renameOpenFolderRecentRecord: update [" + recentRecord.getFilePath() + "] to [" + dir.getAbsolutePath() + "]");
                            recentRecord.setFilePath(newDir.getAbsolutePath());
                        }
                    }
                }
               return recentRecords;
            });

            mEventBus.post(new RecentOpenFolderListChangeMessage().setRecentRecord(saveRecentList));

            e.onNext(true);
            e.onComplete();
        });
    }

    private List<RecentRecord> updateOpenFolderRecentPreference(File dir, Action1<List<RecentRecord>> updateObjectAction) {
        Preference<String> prefRecent = getJsonStringPreference(PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY);

        String json = prefRecent.get();
        List<RecentRecord> recentRecords = jsonToHistoryList(json);

        List<RecentRecord> newList = updateObjectAction.onAction(recentRecords);

        String newJson = recentHistoryListToJson(newList);
        Log.d(TAG, "addOpenFolderRecentRecord: update [" + PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY + "] with value : " + newJson);
        prefRecent.set(newJson);
        return newList;
    }

    public Observable<List<RecentRecord>> loadRecentOpenFolders(boolean detectFileExistence) {
        return Observable.create(e -> {
            Preference<String> prefRecent = getJsonStringPreference(PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY);
            List<RecentRecord> recentRecords = jsonToHistoryList(prefRecent.get());
            List<RecentRecord> result = recentRecords;

            if (detectFileExistence) {
                if (!ListUtils.isEmpty(recentRecords)) {
                    List<RecentRecord> checkedList = Stream.of(recentRecords).filter(record -> new File(record.getFilePath()).exists()).toList();
                    if (checkedList.size() < recentRecords.size()) {
                        Log.d(TAG, "loadRecentOpenFolders: found invalid folder path, so update with new folder list");

                        result = checkedList;
                        prefRecent.set(new Gson().toJson(checkedList));
                    }
                }
            }

            e.onNext(result);
            e.onComplete();
        });
    }

    @NonNull
    private Preference<String> getJsonStringPreference(String prefKey) {
        return mPreferences.getString(prefKey, "[]");
    }
}

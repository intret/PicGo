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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cn.intret.app.picgo.model.event.RecentOpenFolderListChangeMessage;
import cn.intret.app.picgo.model.event.RenameDirectoryMessage;
import cn.intret.app.picgo.utils.Modifier;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.MapAction1;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class UserDataService extends BaseService {

    public static final String PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY = "folder_recent_history";
    public static final String PREF_KEY_IMAGE_VIEW_MODE = "image_view_mode";
    private static final String PREF_KEY_IMAGE_SORT_ORDER = "image_sort_order";
    private static final String PREF_KEY_IMAGE_SORT_WAY = "image_sort_way";

    public static final String VIEW_MODE_LIST_VIEW = "list_view";
    public static final String VIEW_MODE_GRID_VIEW = "grid_view";
    public static final String PREF_KEY_SHOW_HIDDEN_FOLDER = "show_hidden_folder";
    public static final String PREF_KEY_HIDDEN_FOLDER_LIST_JSON = "hidden_folder_list_json";
    public static final String TAG = "UserDataService";

    private final int mMaxRecentHistorySize = 10;

    RxSharedPreferences mPreferences;

    EventBus mEventBus = EventBus.getDefault();

    public RxSharedPreferences getPreferences() {
        return mPreferences;
    }

    int mMoveFileDialogFirstVisibleItemPosition = 0;

    /*
     * Inner class
     */

    public int getMoveFileDialogFirstVisibleItemPosition() {
        return mMoveFileDialogFirstVisibleItemPosition;
    }

    /*
     * UI preferences
     */
    public UserDataService setMoveFileDialogFirstVisibleItemPosition(int moveFileDialogFirstVisibleItemPosition) {
        mMoveFileDialogFirstVisibleItemPosition = moveFileDialogFirstVisibleItemPosition;
        return this;
    }

    /*
     * Singleton
     */
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

    /*
     * Helper
     */

    public <R> R getStringPreference(String key, MapAction1<R, String> mapAction) {
        String val = mPreferences.getString(key).get();
        if (mapAction != null) {
            return mapAction.onMap(val);
        }
        return null;
    }

    @NonNull
    private Preference<String> getJsonStringPreference(String prefKey) {
        return mPreferences.getString(prefKey, "[]");
    }

    private void updatePreference(String preferenceKey, Modifier modifyAction) {
        Preference<String> prefRecent = getJsonStringPreference(preferenceKey);

    }


    /*
     * EventBus message
     */

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(RenameDirectoryMessage message) {

        Log.d(TAG, "onEvent() called with: message = [" + message + "]");

        renameOpenFolderRecentRecord(message.getOldDirectory(), message.getNewDirectory())
                .subscribe(ok -> {
                    Log.d(TAG, "onEvent: rename recent record success : " + message.getOldDirectory());
                });
    }

    /*
     * Recent folder list
     */
    private void validateRecentFolderList() {

        loadRecentOpenFolders(false)
                .subscribeOn(Schedulers.io())
                .subscribe(recentRecords -> {

                }, throwable -> {
                    Log.e(TAG, "validateRecentFolderList: failed to load recent history" );
                });

        getHiddenFolder().subscribeOn(Schedulers.io())
                .subscribe(files -> {

                }, throwable -> {
                    Log.e(TAG, "validateRecentFolderList: failed to load hidden file list");
                });
    }

    private List<RecentRecord> jsonToHistoryList(String json) {
        // https://stackoverflow.com/questions/5554217/google-gson-deserialize-listclass-object-generic-type
        Type listType = new TypeToken<List<RecentRecord>>() {
        }.getType();
        return new Gson().fromJson(json, listType);
    }

    private String recentHistoryListToJson(List<RecentRecord> recentRecords) {
        return new Gson().toJson(recentRecords);
    }

    public Observable<Boolean> addOpenFolderRecentRecord(File dir) {

        return Observable.create(e -> {
            if (dir == null) {
                throw new IllegalArgumentException("Argument 'dir' is null.");
            }

            List<RecentRecord> saveRecentList = updateOpenFolderRecentPreference(recentRecords -> {

                boolean remove = cn.intret.app.picgo.utils.ListUtils.removeListElement(
                        recentRecords,
                        record -> record.getFilePath() != null && record.getFilePath().equals(dir.getAbsolutePath()));
                recentRecords.add(0, new RecentRecord().setFilePath(dir.getAbsolutePath()));

                return Stream.of(recentRecords).limit(mMaxRecentHistorySize).toList();
            });

            mEventBus.post(new RecentOpenFolderListChangeMessage().setRecentRecord(saveRecentList));

            e.onNext(true);
            e.onComplete();
        });
    }

    private List<RecentRecord> updateOpenFolderRecentPreference(Modifier<List<RecentRecord>> updateObjectAction) {
        Preference<String> prefRecent = getJsonStringPreference(PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY);

        String json = prefRecent.get();
        List<RecentRecord> recentRecords = jsonToHistoryList(json);

        List<RecentRecord> newList = updateObjectAction.onModify(recentRecords);

        String newJson = recentHistoryListToJson(newList);
//        Log.d(TAG, "updateOpenFolderRecentPreference: update [" + PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY + "] with value : " + newJson);
        prefRecent.set(newJson);
        return newList;
    }

    public Observable<Boolean> renameOpenFolderRecentRecord(File dir, File newDir) {

        return Observable.create(e -> {
            if (dir == null) {
                throw new IllegalArgumentException("Argument 'dir' is null.");
            }

            List<RecentRecord> saveRecentList = updateOpenFolderRecentPreference(recentRecords -> {

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

    public Observable<UserInitialPreferences> loadInitialPreference(boolean detectFileExistence) {
        return Observable.create(e -> {

            UserInitialPreferences pref = new UserInitialPreferences();

            // 最近访问文件列表
            Preference<LinkedList<RecentRecord>> recentHistoryPreference = getRecentHistoryPreference();
            LinkedList<RecentRecord> records = recentHistoryPreference.get();
            pref.setRecentRecords(records);

            LinkedList<RecentRecord> result = records;

            if (detectFileExistence && !ListUtils.isEmpty(records)) {

                // 移除掉不存在的目录
                List<RecentRecord> checkedList = Stream.of(records).filter(record -> new File(record.getFilePath()).exists()).toList();
                if (checkedList.size() < records.size()) {
                    Log.d(TAG, "loadRecentOpenFolders: found invalid folder path, so update with new folder list");

                    result = new LinkedList<RecentRecord>(checkedList);
                    recentHistoryPreference.set(result);
                }
            }

            // 图片显示模式偏好
            Preference<ViewMode> viewMode = getViewMode();
            if (viewMode.get() == ViewMode.UNKNOWN) {
                viewMode.set(ViewMode.GRID_VIEW);
            }
            pref.setViewMode(viewMode.get());

            // 排序偏好设置
            Preference<SortWay> sortWay = getSortWay();
            SortWay way = sortWay.get();
            if (way == SortWay.UNKNOWN) {
                sortWay.set(SortWay.DATE);
            }
            pref.setSortWay(sortWay.get());

            Preference<SortOrder> sortOrder = getSortOrder();
            SortOrder order = sortOrder.get();
            if (order == SortOrder.UNKNOWN) {
                if (way == SortWay.DATE || way == SortWay.SIZE) {
                    sortOrder.set(SortOrder.DESC);
                } else {
                    sortOrder.set(SortOrder.ASC);
                }
            }
            pref.setSortOrder(sortOrder.get());

            e.onNext(pref);
            e.onComplete();
        });
    }

    public Preference<SortWay> getSortWay() {
        return getPreferences().getObject(PREF_KEY_IMAGE_SORT_WAY,
                SortWay.UNKNOWN, new Preference.Converter<SortWay>() {
                    @NonNull
                    @Override
                    public SortWay deserialize(@NonNull String serialized) {
                        return SortWay.fromString(serialized);
                    }

                    @NonNull
                    @Override
                    public String serialize(@NonNull SortWay value) {
                        return value.toString();
                    }
                });
    }

    public Preference<SortOrder> getSortOrder() {
        return getPreferences().getObject(PREF_KEY_IMAGE_SORT_ORDER,
                SortOrder.UNKNOWN, new Preference.Converter<SortOrder>() {
            @NonNull
            @Override
            public SortOrder deserialize(@NonNull String serialized) {
                return SortOrder.fromString(serialized);
            }

            @NonNull
            @Override
            public String serialize(@NonNull SortOrder value) {
                return value.toString();
            }
        });
    }

    public Preference<Boolean> getShowHiddenFilePreference() {
        return getPreferences().getBoolean(PREF_KEY_SHOW_HIDDEN_FOLDER, false);
    }

    public Preference<ViewMode> getViewMode() {

        return getPreferences().getObject(PREF_KEY_IMAGE_VIEW_MODE, ViewMode.UNKNOWN, new Preference.Converter<ViewMode>() {
            @NonNull
            @Override
            public ViewMode deserialize(@NonNull String serialized) {
                return ViewMode.fromString(serialized);
            }

            @NonNull
            @Override
            public String serialize(@NonNull ViewMode value) {
                return value.toString();
            }
        });
    }

    public Observable<List<RecentRecord>> loadRecentOpenFolders(boolean detectFileExistence) {
        return Observable.create(e -> {

            Preference<LinkedList<RecentRecord>> pref = getRecentHistoryPreference();

            List<RecentRecord> recentRecords = pref.get();
            List<RecentRecord> result = recentRecords;

            if (detectFileExistence) {
                if (!ListUtils.isEmpty(recentRecords)) {
                    List<RecentRecord> checkedList = Stream.of(recentRecords)
                            .filter(record -> new File(record.getFilePath()).exists())
                            .toList();
                    if (checkedList.size() < recentRecords.size()) {
                        Log.d(TAG, "loadRecentOpenFolders: found invalid folder path, will be removed.");

                        result = checkedList;
                        pref.set(new LinkedList<>(checkedList));
                    }
                }
            }

            e.onNext(result);
            e.onComplete();
        });
    }

    /*
     * Hidden folders
     */
    public Observable<Boolean> addHiddenFolder(File dir) {

        return Observable.create(e -> {

            Preference<LinkedList<File>> hiddenFolderPref = getHiddenFolderPreference();
            LinkedList<File> files = hiddenFolderPref.get();
            if (!files.contains(dir)) {
                files.add(dir);
            }

            Log.d(TAG, "addHiddenFolder: " + files);
            hiddenFolderPref.set(files);

            e.onNext(true);
            e.onComplete();
        });
    }

    @NonNull
    private Preference<LinkedList<RecentRecord>> getRecentHistoryPreference() {
        Preference<LinkedList<RecentRecord>> object;
        object = getPreferences().getObject(PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY,
                new LinkedList<RecentRecord>(), new Preference.Converter<LinkedList<RecentRecord>>() {
                    @NonNull
                    @Override
                    public LinkedList<RecentRecord> deserialize(@NonNull String serialized) {
                        //serialized = "[]";
                        Log.d(TAG, "deserialize() called with: serialized = [" + serialized + "]");

                        Type listType = new TypeToken<ArrayList<RecentRecord>>() {
                        }.getType();
                        ArrayList<RecentRecord> files = new Gson().fromJson(serialized, listType);
                        return new LinkedList<RecentRecord>(files);
                    }

                    @NonNull
                    @Override
                    public String serialize(@NonNull LinkedList<RecentRecord> value) {

                        Type listType = new TypeToken<ArrayList<RecentRecord>>() {
                        }.getType();

                        String json = new Gson().toJson(value, listType);
                        Log.d(TAG, "serialize() called with: value = [" + value + "]" + " result = [" + json + "]");
                        return json;
                    }
                });
        return object;
    }

    @NonNull
    private Preference<LinkedList<File>> getHiddenFolderPreference() {
        Preference<LinkedList<File>> object;
        object = getPreferences().getObject(PREF_KEY_HIDDEN_FOLDER_LIST_JSON,
                new LinkedList<File>(), new Preference.Converter<LinkedList<File>>() {
                    @NonNull
                    @Override
                    public LinkedList<File> deserialize(@NonNull String serialized) {
                        //serialized = "[]";
                        Log.d(TAG, "deserialize() called with: serialized = [" + serialized + "]");

                        Type listType = new TypeToken<ArrayList<String>>() {
                        }.getType();
                        ArrayList<String> files = new Gson().fromJson(serialized, listType);
                        List<File> files1 = Stream.of(files).map(File::new).toList();
                        return new LinkedList<File>(files1);
                    }

                    @NonNull
                    @Override
                    public String serialize(@NonNull LinkedList<File> value) {

                        Type listType = new TypeToken<ArrayList<String>>() {
                        }.getType();

                        List<String> filePathList = Stream.of(value).map(File::getAbsolutePath).toList();
                        String json = new Gson().toJson(filePathList, listType);
                        Log.d(TAG, "serialize() called with: value = [" + value + "]" + " result = [" + json + "]");
                        return json;
                    }
                });
        return object;
    }

    public Observable<List<File>> getHiddenFolder() {
        return Observable.create(e -> {
            Preference<LinkedList<File>> hiddenFolderPreference = getHiddenFolderPreference();
            e.onNext(hiddenFolderPreference.get());
            e.onComplete();
        });
    }
}

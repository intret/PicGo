package cn.intret.app.picgo.model.user

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import cn.intret.app.picgo.model.BaseModule
import cn.intret.app.picgo.model.event.RecentOpenFolderListChangeMessage
import cn.intret.app.picgo.model.event.RenameDirectoryMessage
import cn.intret.app.picgo.model.user.data.*
import cn.intret.app.picgo.utils.ListUtils
import cn.intret.app.picgo.utils.MapAction1
import cn.intret.app.picgo.utils.Modifier
import cn.intret.app.picgo.utils.Watch
import com.annimon.stream.Stream
import com.f2prateek.rx.preferences2.Preference
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.orhanobut.logger.Logger
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.*
import javax.inject.Singleton

/**
 * Singleton Module of user preferences: image list view mode, image list sort order, recently opened image folder list.
 *
 * The module needs an application context, given by calling {@link #setAppContext}
 */
@Singleton
@SuppressLint("StaticFieldLeak")
object UserModule : BaseModule() {
    // ------------------------------------------------
    // constants
    // ------------------------------------------------

    val PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY = "folder_recent_history"
    val PREF_KEY_IMAGE_VIEW_MODE = "image_view_mode"
    private val PREF_KEY_IMAGE_SORT_ORDER = "image_sort_order"
    private val PREF_KEY_IMAGE_SORT_WAY = "image_sort_way"

    val VIEW_MODE_LIST_VIEW = "list_view"
    val VIEW_MODE_GRID_VIEW = "grid_view"
    val PREF_KEY_SHOW_HIDDEN_FOLDER = "show_hidden_folder"
    val PREF_KEY_HIDDEN_FOLDER_LIST_JSON = "hidden_folder_list_json"

    val TAG = "UserDataService"

    // ------------------------------------------------
    //
    // ------------------------------------------------

    private val mMaxRecentHistorySize = 10

    lateinit var preferences: RxSharedPreferences
        internal set

    // a scoped event bus for current module
    internal var mEventBus = EventBus.getDefault()

    // ------------------------------------------------
    // ui preferences
    // ------------------------------------------------

    internal var moveFileDialogFirstVisibleItemPosition = 0

    val imageClickToFullscreen: Boolean
        get() = true

    // ------------------------------------------------
    // image list preferences
    // ------------------------------------------------

    val sortWay: Preference<SortWay>
        get() = preferences.getObject(PREF_KEY_IMAGE_SORT_WAY,
                SortWay.UNKNOWN, object : Preference.Converter<SortWay> {
            override fun deserialize(serialized: String): SortWay {
                return SortWay.fromString(serialized)
            }

            override fun serialize(value: SortWay): String {
                return value.toString()
            }
        })

    val sortOrder: Preference<SortOrder>
        get() = preferences.getObject(PREF_KEY_IMAGE_SORT_ORDER,
                SortOrder.UNKNOWN, object : Preference.Converter<SortOrder> {
            override fun deserialize(serialized: String): SortOrder {
                return SortOrder.fromString(serialized)
            }

            override fun serialize(value: SortOrder): String {
                return value.toString()
            }
        })

    val showHiddenFilePreference: Preference<Boolean>
        get() = preferences.getBoolean(PREF_KEY_SHOW_HIDDEN_FOLDER, false)

    val viewMode: Preference<ViewMode>
        get() = preferences.getObject(PREF_KEY_IMAGE_VIEW_MODE, ViewMode.UNKNOWN, object : Preference.Converter<ViewMode> {
            override fun deserialize(serialized: String): ViewMode {
                return ViewMode.fromString(serialized)
            }

            override fun serialize(value: ViewMode): String {
                return value.toString()
            }
        })

    private//serialized = "[]";
    //                        Log.d(TAG, "deserialize() called with: serialized = [" + serialized + "]");
    //                        Log.d(TAG, "serialize() called with: value = [" + value + "]" + " result = [" + json + "]");
    val recentHistoryPreference: Preference<LinkedList<RecentRecord>>
        get() {
            val `object`: Preference<LinkedList<RecentRecord>>
            `object` = preferences.getObject(PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY,
                    LinkedList(), object : Preference.Converter<LinkedList<RecentRecord>> {
                override fun deserialize(serialized: String): LinkedList<RecentRecord> {

                    val listType = object : TypeToken<ArrayList<RecentRecord>>() {

                    }.type
                    val files = Gson().fromJson<ArrayList<RecentRecord>>(serialized, listType)
                    return LinkedList(files)
                }

                override fun serialize(value: LinkedList<RecentRecord>): String {

                    val listType = object : TypeToken<ArrayList<RecentRecord>>() {

                    }.type

                    val json = Gson().toJson(value, listType)
                    return json
                }
            })
            return `object`
        }

    private//serialized = "[]";
    //                        Log.d(TAG, "deserialize() called with: serialized = [" + serialized + "]");
    //                        Log.d(TAG, "serialize() called with: value = [" + value + "]" + " result = [" + json + "]");
    val hiddenFolderPreference: Preference<LinkedList<File>>
        get() {
            val `object`: Preference<LinkedList<File>>
            `object` = preferences.getObject(PREF_KEY_HIDDEN_FOLDER_LIST_JSON,
                    LinkedList(), object : Preference.Converter<LinkedList<File>> {
                override fun deserialize(serialized: String): LinkedList<File> {

                    val listType = object : TypeToken<ArrayList<String>>() {

                    }.type
                    val files = Gson().fromJson<ArrayList<String>>(serialized, listType)
                    val files1 = Stream.of(files).map<File>({ File(it) }).toList()
                    return LinkedList(files1)
                }

                override fun serialize(value: LinkedList<File>): String {

                    val listType = object : TypeToken<ArrayList<String>>() {

                    }.type

                    val filePathList = Stream.of(value).map<String>({ it.absolutePath }).toList()
                    val json = Gson().toJson(filePathList, listType)
                    return json
                }
            })
            return `object`
        }

    val excludeFolderPreference: Observable<Preference<LinkedList<File>>>
        get() = Observable.create { e ->
            val hiddenFolderPreference = hiddenFolderPreference
            e.onNext(hiddenFolderPreference)
            e.onComplete()
        }

    val hiddenFolder: Observable<List<File>>
        get() = Observable.create { e ->
            val hiddenFolderPreference = hiddenFolderPreference
            e.onNext(hiddenFolderPreference.get())
            e.onComplete()
        }


    // ---------------------------------------------------------------------------------------------
    // module initialization
    // ---------------------------------------------------------------------------------------------

    init {
        mEventBus.register(this)
    }

    override fun setAppContext(applicationContext: Context) {
        super.setAppContext(applicationContext)

        initWithContext(applicationContext)
    }

    private fun initWithContext(applicationContext: Context) {
        this.preferences = RxSharedPreferences.create(PreferenceManager.getDefaultSharedPreferences(applicationContext))

        validateRecentFolderList()
    }

    fun addOpenFolderRecentRecord(dir: File?): Observable<Boolean> {

        return Observable.create { e ->
            if (dir == null) {
                throw IllegalArgumentException("Argument 'dir' is null.")
            }

            val saveRecentList = updateOpenFolderRecentPreference(Modifier<MutableList<RecentRecord>> { recentRecords ->

                val remove = cn.intret.app.picgo.utils.ListUtils.removeListElement<RecentRecord>(recentRecords)
                { record ->
                    record.filePath != null && record.filePath == dir.absolutePath
                }

                recentRecords.toMutableList().add(0, RecentRecord().setFilePath(dir.absolutePath))

                Stream.of<RecentRecord>(recentRecords).limit(mMaxRecentHistorySize.toLong()).toList()
            })

            mEventBus.post(RecentOpenFolderListChangeMessage().setRecentRecord(saveRecentList))

            e.onNext(true)
            e.onComplete()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // UI preferences
    // ---------------------------------------------------------------------------------------------

    fun setMoveFileDialogFirstVisibleItemPosition(moveFileDialogFirstVisibleItemPosition: Int): UserModule {
        this.moveFileDialogFirstVisibleItemPosition = moveFileDialogFirstVisibleItemPosition
        return this
    }

    fun getMoveFileDialogFirstVisibleItemPosition(): Int {
        return moveFileDialogFirstVisibleItemPosition
    }

    // ---------------------------------------------------------------------------------------------
    // preference helper functions
    // ---------------------------------------------------------------------------------------------

    fun <R> getStringPreference(key: String, mapAction: MapAction1<R, String>?): R? {
        val `val` = preferences.getString(key).get()
        return mapAction?.onMap(`val`)
    }

    private fun getJsonStringPreference(prefKey: String): Preference<String> {
        return preferences.getString(prefKey, "[]")
    }

    private fun updatePreference(preferenceKey: String, modifyAction: Modifier<*>) {
        val prefRecent = getJsonStringPreference(preferenceKey)

    }


    // ---------------------------------------------------------------------------------------------
    // EventBus message
    // ---------------------------------------------------------------------------------------------


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onEvent(message: RenameDirectoryMessage) {

        Log.d(TAG, "onEvent() called with: message = [$message]")

        renameOpenFolderRecentRecord(message.getOldDirectory(), message.getNewDirectory())
                .subscribe { ok -> Log.d(TAG, "onEvent: rename recent record success : " + message.getOldDirectory()!!) }
    }


    // ------------------------------------------------
    // Recent folder list
    // ------------------------------------------------

    private fun validateRecentFolderList() {

        loadRecentOpenFolders(false)
                .subscribeOn(Schedulers.io())
                .subscribe({ recentRecords ->

                }) { throwable -> Log.e(TAG, "validateRecentFolderList: failed to load recent history") }

        hiddenFolder.subscribeOn(Schedulers.io())
                .subscribe({ files ->

                }) { throwable -> Log.e(TAG, "validateRecentFolderList: failed to load hidden file list") }
    }

    private fun jsonToHistoryList(json: String): List<RecentRecord>? {
        // https://stackoverflow.com/questions/5554217/google-gson-deserialize-listclass-object-generic-type
        val listType = object : TypeToken<List<RecentRecord>>() {

        }.type
        return Gson().fromJson<List<RecentRecord>>(json, listType)
    }

    private fun recentHistoryListToJson(recentRecords: List<RecentRecord>): String {
        return Gson().toJson(recentRecords)
    }

    private fun updateOpenFolderRecentPreference(updateObjectAction: Modifier<MutableList<RecentRecord>>): List<RecentRecord> {
        val prefRecent = getJsonStringPreference(PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY)

        val json = prefRecent.get()
        val recentRecords = jsonToHistoryList(json)

        val newList = updateObjectAction.onModify(recentRecords?.toMutableList())

        val newJson = recentHistoryListToJson(newList)
        //        Log.d(TAG, "updateOpenFolderRecentPreference: update [" + PREF_KEY_FOLDER_ACCESS_RECENT_HISTORY + "] with value : " + newJson);
        prefRecent.set(newJson)
        return newList
    }

    fun renameOpenFolderRecentRecord(dir: File?, newDir: File?): Observable<Boolean> {

        return Observable.create { e ->
            if (dir == null) {
                throw IllegalArgumentException("Argument 'dir' is null.")
            }

            val saveRecentList = updateOpenFolderRecentPreference(Modifier<MutableList<RecentRecord>> { recentRecords ->

                for (recentRecord in recentRecords) {
                    if (recentRecord.filePath != null) {
                        if (recentRecord.filePath == dir.absolutePath) {
                            Log.d(TAG, "renameOpenFolderRecentRecord: update [" + recentRecord.filePath + "] to [" + dir.absolutePath + "]")
                            recentRecord.setFilePath(newDir!!.absolutePath)
                        }
                    }
                }
                recentRecords
            })

            mEventBus.post(RecentOpenFolderListChangeMessage().setRecentRecord(saveRecentList))

            e.onNext(true)
            e.onComplete()
        }
    }

    fun loadInitialPreference(detectFileExistence: Boolean): Observable<UserInitialPreferences> {
        return Observable.create { e ->

            val watch = Watch.now()
            val pref = UserInitialPreferences()

            // 最近访问文件列表
            val recentHistoryPreference = recentHistoryPreference
            val records = recentHistoryPreference.get()
            pref.recentRecords = records

            var result = records

            if (detectFileExistence && !ListUtils.isEmpty(records)) {

                // 移除掉不存在的目录
                val checkedList = Stream.of(records).filter { record -> File(record.filePath).exists() }.toList()
                if (checkedList.size < records.size) {
                    Log.d(TAG, "loadRecentOpenFolders: found invalid folder path, so update with new folder list")

                    result = LinkedList(checkedList)
                    recentHistoryPreference.set(result)
                }
            }

            // 图片显示模式偏好
            val viewMode = viewMode
            if (viewMode.get() == ViewMode.UNKNOWN) {
                viewMode.set(ViewMode.GRID_VIEW)
            }
            pref.viewMode = viewMode.get()

            // 排序偏好设置
            val sortWay = sortWay
            val way = sortWay.get()
            if (way == SortWay.UNKNOWN) {
                sortWay.set(SortWay.DATE)
            }
            pref.sortWay = sortWay.get()

            val sortOrder = sortOrder
            val order = sortOrder.get()
            if (order == SortOrder.UNKNOWN) {
                if (way == SortWay.DATE || way == SortWay.SIZE) {
                    sortOrder.set(SortOrder.DESC)
                } else {
                    sortOrder.set(SortOrder.ASC)
                }
            }
            pref.sortOrder = sortOrder.get()

            watch.logTotalMS(TAG, "Load user initial preferences")

            e.onNext(pref)
            e.onComplete()
        }
    }

    fun loadRecentOpenFolders(detectFileExistence: Boolean): Observable<List<RecentRecord>> {
        return Observable.create { e ->

            val pref = recentHistoryPreference

            val recentRecords = pref.get()
            var result = recentRecords

            if (detectFileExistence) {
                if (!ListUtils.isEmpty(recentRecords)) {
                    val checkedList = Stream.of(recentRecords)
                            .filter { record -> File(record.filePath).exists() }
                            .toList()
                    if (checkedList.size < recentRecords.size) {
                        Log.d(TAG, "loadRecentOpenFolders: found invalid folder path, will be removed.")

                        result = LinkedList(checkedList)
                        pref.set(LinkedList(checkedList))
                    }
                }
            }

            e.onNext(result)
            e.onComplete()
        }
    }


    // ------------------------------------------------
    // Hidden subFolders
    // ------------------------------------------------

    fun addExcludeFolder(dir: File): Observable<Boolean> {

        return Observable.create { e ->

            val hiddenFolderPref = hiddenFolderPreference
            val files = hiddenFolderPref.get()
            if (!files.contains(dir)) {
                files.add(dir)
            }

            Logger.d(files)
            hiddenFolderPref.set(files)

            e.onNext(true)
            e.onComplete()
        }
    }
}

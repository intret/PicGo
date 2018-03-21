package cn.intret.app.picgo.model.image


import android.annotation.SuppressLint
import android.media.ExifInterface
import android.util.Log
import android.util.Pair
import cn.intret.app.picgo.model.BaseModule
import cn.intret.app.picgo.model.NotEmptyException
import cn.intret.app.picgo.model.event.*
import cn.intret.app.picgo.model.user.SortOrder
import cn.intret.app.picgo.model.user.SortWay
import cn.intret.app.picgo.model.user.UserModule
import cn.intret.app.picgo.utils.*
import com.annimon.stream.Collector
import com.annimon.stream.Stream
import com.annimon.stream.function.*
import com.annimon.stream.function.Function
import com.f2prateek.rx.preferences2.Preference
import com.t9search.util.T9Util
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.apache.commons.io.FileExistsException
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.InvalidParameterException
import java.util.*
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.inject.Singleton

@Singleton
@SuppressLint("StaticFieldLeak")
object ImageModule : BaseModule() {

    /**
     * Key : directory file
     */
    private val mMediaFileListMap = LinkedHashMap<File, MediaFileList>()
    private val mMediaFileListMapGuard = ObjectGuarder<HashMap<File, MediaFileList>>(mMediaFileListMap)

    private val mImageListMap = LinkedHashMap<String, List<MediaFile>>()
    private val mDayImageGroupsMap = LinkedHashMap<String, List<ImageGroup>>()
    private val mWeekImageGroupsMap = LinkedHashMap<String, List<ImageGroup>>()
    private val mMonthImageGroupsMap = LinkedHashMap<String, List<ImageGroup>>()

    private val mHiddenFolders = ObjectGuarder<LinkedList<File>>(LinkedList())

    private var mShowHiddenFolderPref: Preference<Boolean>? = null
    private var mShowHiddenFile = false
    private var mSortWay = SortWay.UNKNOWN
    private var mSortOrder = SortOrder.UNKNOWN

    internal var mFolderModel: FolderModel? = null
    internal var mFolderModelRWLock: ReadWriteLock = ReentrantReadWriteLock()
    private var mExcludeFolderListPref: ObjectGuarder<Preference<LinkedList<File>>>? = null

    /*
     * 文件夹列表
     */

    val folderListCount: Int
        get() {
            try {
                mFolderModelRWLock.readLock().lock()
                if (mFolderModel == null) {

                    loadGalleryFolderList().subscribe { folderInfos ->

                    }
                    return mFolderModel!!.containerFolders?.size ?: 0
                } else {
                    return mFolderModel!!.containerFolders?.size ?: 0
                }
            } finally {
                mFolderModelRWLock.readLock().unlock()
            }
        }

    private class MediaFileList {
        internal lateinit var mMediaFiles: MutableList<MediaFile>
        internal var mIsLoadedExtraInfo = false

        internal val mediaFiles: MutableList<MediaFile>?
            get() = mMediaFiles

        internal fun setMediaFiles(mediaFiles: List<MediaFile>): MediaFileList {
            mMediaFiles = mediaFiles.toMutableList()
            return this
        }

        internal fun isLoadedExtraInfo(): Boolean {
            return mIsLoadedExtraInfo
        }

        internal fun setLoadedExtraInfo(loadedExtraInfo: Boolean): MediaFileList {
            mIsLoadedExtraInfo = loadedExtraInfo
            return this
        }
    }

    init {

        loadUserExcludeFileList()
    }

    private fun loadUserExcludeFileList() {
        Log.d(TAG, "loadHiddenFileList: before")

        // 是否显示隐藏目录
        Observable
                .create<Preference<Boolean>> { e ->
                    val watch = Watch.now()
                    val showHiddenFolder = UserModule.showHiddenFilePreference

                    watch.logGlanceMS(TAG, "Load user initial preference")
                    e.onNext(showHiddenFolder)
                    e.onComplete()
                }
                .subscribe(
                        {
                            mShowHiddenFolderPref = it
                            mShowHiddenFile = mShowHiddenFolderPref!!.get()
                            Log.d(TAG, "loadHiddenFileList: mShowHiddenFile = $mShowHiddenFile")

                            mShowHiddenFolderPref!!.asObservable()
                                    .subscribe { showHiddenFile ->
                                        Log.d(TAG, "loadHiddenFileList: show hidden file preference changed : " + showHiddenFile!!)
                                        mShowHiddenFile = showHiddenFile

                                        Log.w(TAG, "loadHiddenFileList: todo reload folder list")
                                    }
                        },
                        { RxUtils.unhandledThrowable(it) })

        // 隐藏目录列表
        UserModule
                .hiddenFolder
                .subscribe({ files ->

                    mHiddenFolders.apply {

                    }
                    mHiddenFolders.writeConsume { fileList ->
                        Log.d(TAG, "loadHiddenFileList: save file list $fileList")

                        fileList.clear()
                        fileList.addAll(files)
                    }

                    //mBus.post(new ExcludeFolderListChangeMessage());

                    //                    rescanFolderList("loaded hidden folder list ");

                }, ({ RxUtils.unhandledThrowable(it) }))
        Log.d(TAG, "loadHiddenFileList: after")

        UserModule.excludeFolderPreference
                .subscribeOn(Schedulers.io())
                .subscribe({ linkedListPreference ->

                    mExcludeFolderListPref = ObjectGuarder<Preference<LinkedList<File>>>(linkedListPreference)

                    mExcludeFolderListPref!!.readConsume { pref ->
                        pref.asObservable().subscribe { newFileList ->
                            mHiddenFolders.writeConsume { fileList ->

                                fileList.clear()
                                fileList.addAll(newFileList)

                                rescanFolderList("updated hidden folder list")
                            }
                        }
                    }
                })
    }

    @Deprecated("")
    fun getSectionForPosition(position: Int): Int {
        try {

            mFolderModelRWLock.readLock().lock()
            if (mFolderModel == null) {
                return 0
            } else {

                var begin = 0
                var end = 0
                mFolderModel?.containerFolders?.let {
                    for (sectionIndex in it.indices) {
                        val containerFolder = it[sectionIndex]
                        val size = containerFolder.folders.size

                        end += size

                        if (position >= begin && position < end) {
                            Log.d(TAG, "getSectionForPosition: position $position to section $sectionIndex")
                            return sectionIndex
                        }

                        begin += size
                    }
                }


                throw InvalidParameterException(
                        String.format(Locale.getDefault(),
                                "Invalid parameter 'position' value '%d', exceeds total item size %d", position, begin))

            }
        } finally {
            mFolderModelRWLock.readLock().unlock()
        }
    }

    /**
     * @param fromCacheFirst
     * @param t9NumberInput  为 null 或者空字符串时，获取的文件列表不进行 T9 过滤，并且是正常模式，并非过滤模式。
     * @return
     */
    fun loadFolderModel(fromCacheFirst: Boolean, t9NumberInput: String?): Observable<FolderModel> {
        return loadFolderModel(fromCacheFirst)
                .map { model ->
                    if (StringUtils.isBlank(t9NumberInput)) {
                        return@map model
                    }

                    // The variable 'model' is a copy of original model, we can modify it.
                    filterModelByT9NumberInput(model, t9NumberInput)

                    model.mIsT9FilterMode = true
                    model
                }
    }

    private fun filterModelByT9NumberInput(model: FolderModel, t9Numbers: String?) {
        model.containerFolders?.forEachIndexed { index, containerFolder ->
            val folders = containerFolder.folders
            run {
                val filteredFolders = LinkedList<ImageFolder>()
                for (folder in folders) {
                    val pinyinSearchUnit = folder.getPinyinSearchUnit()

                    // Pinyin match

                    if (T9Util.match(pinyinSearchUnit, t9Numbers)) {

                        folder.setMatchKeywords(pinyinSearchUnit?.matchKeyword.toString())
                        folder.setMatchStartIndex(folder.name?.indexOf(pinyinSearchUnit?.matchKeyword.toString())
                                ?: 0)
                        folder.setMatchLength(folder.matchKeywords?.length ?: 0)

                        filteredFolders.add(folder)
                    } else {
                        //                        Log.d(TAG, "T9: folder [" + folder.getName() + "]  -------------- T9 keyboard input : " + t9Numbers);
                    }
                }

                Log.d(TAG, "----- 最后剩下 " + filteredFolders.size + "/" + folders.size + " 项 -----")
                containerFolder.setFolders(filteredFolders)
            }
        }
    }

    fun loadHiddenFileListModel(t9NumberInput: String): Observable<FolderModel> {
        return loadHiddenFileListModel()
                .map { model ->
                    if (StringUtils.isBlank(t9NumberInput)) {
                        return@map model
                    }

                    // The variable 'model' is a copy of original model, we can modify it.
                    filterModelByT9NumberInput(model, t9NumberInput)

                    model.mIsT9FilterMode = true
                    model
                }
    }

    fun loadHiddenFileListModel(): Observable<FolderModel> {

        return Observable.create { e ->

            val folderModel = FolderModel()
            mHiddenFolders.readConsume { `object` ->
                Stream.of(`object`)
                        .groupBy { it.parentFile }
                        .forEach { fileListEntry ->
                            val parentDir = fileListEntry.key
                            val subFolderList = fileListEntry.value

                            val imageFolders = Stream.of(subFolderList)
                                    .map { this.createImageFolder(it) }
                                    .toList()

                            folderModel.addFolderSection(FolderModel.ContainerFolder()
                                    .setFile(parentDir)
                                    .setName(parentDir.name)
                                    .setFolders(imageFolders)
                            )
                        }
            }

            e.onNext(folderModel)
            e.onComplete()
        }
    }

    fun loadFolderModel(fromCacheFirst: Boolean): Observable<FolderModel> {
        return Observable.create { emitter ->

            Log.d(TAG, "loadFolderModel() called with: fromCacheFirst = [$fromCacheFirst]")

            if (fromCacheFirst) {
                try {

                    mFolderModelRWLock.readLock().lock()
                    if (mFolderModel != null) {

                        val clone = mFolderModel!!.clone() as FolderModel
                        Log.d(TAG, "loadFolderModel: get clone $clone of $mFolderModel")
                        emitter.onNext(clone)
                        emitter.onComplete()
                        return@create
                    }
                } finally {
                    mFolderModelRWLock.readLock().unlock()
                }
            }

            val watch = Watch.now()
            val folderModel = FolderModel()
            folderModel
                    .apply {

                        // image directory in SDCard/DCIM directory
                        loadImageDirectoryInto(this, SystemUtils.getDCIMDir())

                        // image directory in  SDCard/Picture directory
                        loadImageDirectoryInto(this, SystemUtils.getPicturesDir())
                    }
                    .also {

                        watch.logGlanceMS(TAG, "Load folder list")
                        val cloneModel: FolderModel
                        try {
                            mFolderModelRWLock.writeLock().lock()
                            mFolderModel = folderModel

                            cloneModel = mFolderModel?.clone() as FolderModel
                        } finally {
                            mFolderModelRWLock.writeLock().unlock()
                        }
                        watch.logGlanceMS(TAG, "Cache folder list")


                        emitter.onNext(cloneModel)
                        emitter.onComplete()
                    }
        }
    }

    @Throws(FileNotFoundException::class)
    private fun loadImageDirectoryInto(folderModel: FolderModel, picturesDir: File) {
        listImageDirectories(picturesDir, FOLDER_LIST_NAME_ASC_COMPARATOR)
                .let {
                    addContainerFolder(folderModel, picturesDir, it)
                }
    }

    fun loadRandomImage(): Observable<File> {
        return Observable.create { e ->

            val cameraDir = SystemUtils.getCameraDir()
            val files = cameraDir!!.listFiles()
            if (files == null) {
                e.onError(Exception("No files in Camera folder."))
                return@create
            }

            val file = files[Random().nextInt() % files.size]
            e.onNext(file)
            e.onComplete()
        }
    }

    fun loadFirstCameraImageFile(): Observable<File> {
        return Observable.create { e ->

            val cameraDir = SystemUtils.getCameraDir()
            val files = cameraDir!!.listFiles()
            if (files == null || files.size == 0) {
                e.onError(Exception("No files in Camera folder."))
                return@create
            }

            e.onNext(files[0])
            e.onComplete()
        }
    }

    private fun createThumbnailList(files: Array<File>?, thumbnailCount: Int): MutableList<File>? {
        // TODO: filter image

        var thumbFileList: MutableList<File>? = null
        if (files != null) {
            if (files.size > 0) {
                // 按照时间排序并取前三个文件
                thumbFileList = Stream.of(*files)
                        .sorted { file1, file2 -> java.lang.Long.compare(file2.lastModified(), file1.lastModified()) }
                        .takeWhileIndexed { index, value -> index < thumbnailCount }
                        .toList().toMutableList()
            }
        }

        return thumbFileList
    }

    private fun addContainerFolder(model: FolderModel, dir: File, mediaFolders: List<File>) {
        val containerFolder = FolderModel.ContainerFolder()
        val subFolders = LinkedList<ImageFolder>()

        if (!mShowHiddenFile) {
            mHiddenFolders.readConsume { fileList ->

                val destDirFileNameHashSet = HashSet(mHiddenFolders.`object`)

                var i = 0
                val s = mediaFolders.size
                while (i < s) {
                    val folder = mediaFolders[i]

                    if (!destDirFileNameHashSet.contains(folder)) {
                        subFolders.add(createImageFolder(folder))
                    } else {
                        //Log.d(TAG, "addContainerFolder: ignore folder [" + folder + "]");
                    }
                    i++
                }
            }
        } else {

            var i = 0
            val s = mediaFolders.size
            while (i < s) {
                val folder = mediaFolders[i]
                subFolders.add(createImageFolder(folder))
                i++
            }
        }

        containerFolder.mFile = dir
        containerFolder.mName = dir.name
        containerFolder.mFolders = subFolders

        model.addFolderSection(containerFolder)
    }

    private fun createImageFolder(folder: File?): ImageFolder {
        // todo merge with createThumbnailList

        val imageFiles = folder!!.listFiles(PathUtils.MEDIA_FILENAME_FILTER)
        return ImageFolder().apply {
            file = folder
            name = folder.name
            count = (imageFiles?.size ?: 0)

            setThumbList(createThumbnailList(imageFiles, DEFAULT_THUMBNAIL_COUNT))
            setMediaFiles(imageFiles)
        }
    }


    /**
     * TODO: 按照显示模式来对文件列表排序并取前三个文件
     * 扫描目录，产生 [RescanFolderThumbnailListMessage] 通知。
     */
    fun rescanDirectoryThumbnailList(dir: File): Observable<List<File>> {
        return Observable.create { e ->
            val imageFiles = dir.listFiles(PathUtils.MEDIA_FILENAME_FILTER)
            val thumbnailList = createThumbnailList(imageFiles, DEFAULT_THUMBNAIL_COUNT)

            try {
                mFolderModelRWLock.writeLock().lock()
                if (updateFileModelThumbnailList(mFolderModel, dir, thumbnailList)) {
                    Log.d(TAG, "已经更新目录缩略图列表：$dir")
                    val event = RescanFolderThumbnailListMessage()
                            .setDirectory(dir)
                            .setThumbnails(thumbnailList!!)
                    mBus.post(event)
                }
            } catch (th: Throwable) {
                e.onError(th)
                return@create
            } finally {
                mFolderModelRWLock.writeLock().unlock()

            }
            e.onNext(thumbnailList!!.toList())
            e.onComplete()
        }
    }

    private fun updateFileModelThumbnailList(folderModel: FolderModel?, dir: File, thumbnailList: List<File>?): Boolean {
        val imageFolder = getFileModelImageFolder(folderModel, dir)
        if (imageFolder != null) {
            if (org.apache.commons.collections4.ListUtils.isEqualList(imageFolder.thumbList, thumbnailList)) {
                Log.d(TAG, "updateFileModelThumbnailList: two thumbnail list (cache one and new one) are equal, don't update")
                return false
            }
            imageFolder.setThumbList(thumbnailList?.toMutableList())
            return true
        }
        return false
    }

    private fun getFileModelImageFolder(folderModel: FolderModel?, dir: File): ImageFolder? {
        var imageFolder: ImageFolder
        val containerFolders = folderModel!!.containerFolders
        var i = 0
        val parentFolderInfosSize = containerFolders?.size ?: 0
        while (i < parentFolderInfosSize) {
            containerFolders?.get(i)?.let {
                val folders = it.folders
                var i1 = 0
                val foldersSize = folders.size
                while (i1 < foldersSize) {
                    imageFolder = folders[i1]
                    if (imageFolder.file == dir) {
                        return imageFolder
                    }
                    i1++
                }
                i++
            }
        }
        return null
    }

    private fun cacheImageGroupList(mode: GroupMode) {

    }

    /**
     * Load grouped image list
     *
     * @param directory
     * @param mode
     * @param fromCacheFirst
     * @param order
     * @param way            @return
     */
    fun loadImageGroupList(directory: File, mode: GroupMode,
                           fromCacheFirst: Boolean, way: SortWay, order: SortOrder): Observable<List<ImageGroup>> {
        return Observable.create<List<ImageGroup>> { e ->
            val absolutePath = directory.absolutePath
            if (fromCacheFirst) {
                when (mode) {
                    GroupMode.DAY -> {
                        val imageGroups = mDayImageGroupsMap[absolutePath]
                        if (imageGroups != null) {
                            e.onNext(imageGroups)
                            e.onComplete()
                            return@create
                        }
                    }
                    GroupMode.WEEK -> {
                        val imageGroups = mWeekImageGroupsMap[absolutePath]
                        if (imageGroups != null) {
                            e.onNext(imageGroups)
                            e.onComplete()
                            return@create
                        }
                    }
                    GroupMode.MONTH -> {
                        val imageGroups = mMonthImageGroupsMap[absolutePath]
                        if (imageGroups != null) {
                            e.onNext(imageGroups)
                            e.onComplete()
                            return@create
                        }
                    }
                }
            }

            val imageFiles = ImageModule.listMediaFiles(directory)
            val sections = Stream.of(imageFiles)
                    .groupBy<Int> { file ->
                        val d = file.lastModified()
                        val date = Date(d)

                        when (mode) {
                            GroupMode.DAY -> return@groupBy DateTimeUtils.daysBeforeToday(date)
                            GroupMode.WEEK -> return@groupBy DateTimeUtils.weeksBeforeCurrentWeek(date)
                            GroupMode.MONTH -> return@groupBy DateTimeUtils.monthsBeforeCurrentMonth(date)
                            else -> return@groupBy 0
                        }
                    }
                    .sorted { o1, o2 -> Integer.compare(o1.key, o2.key) }
                    .collect(object : Collector<
                            MutableMap.MutableEntry<Int, List<File>>,
                            LinkedList<ImageGroup>,
                            LinkedList<ImageGroup>> {
                        override fun supplier(): Supplier<LinkedList<ImageGroup>> {
                            return Supplier { LinkedList<ImageGroup>() }
                        }

                        override fun accumulator(): BiConsumer<LinkedList<ImageGroup>, MutableMap.MutableEntry<Int, List<File>>> {
                            return BiConsumer<LinkedList<ImageGroup>, MutableMap.MutableEntry<Int, List<File>>> { sections, entry ->

                                val section = ImageGroup()
                                val files = entry.value
                                val itemList = Stream.of<File>(files)
                                        .map({ file ->
                                            val mediaFile = MediaFile()
                                            mediaFile.file = file
                                            mediaFile.setDate(Date(file.lastModified()))
                                            mediaFile
                                        })
                                        .toList()
                                val firstItem = ListUtils.firstOf<MediaFile>(itemList)
                                val lastItem = ListUtils.lastOf<MediaFile>(itemList)

                                section.setMediaFiles(itemList)
                                section.setStartDate(firstItem.getDate())
                                section.setEndDate(lastItem.getDate())

                                sections.add(section)
                            }
                        }

                        override fun finisher(): Function<LinkedList<ImageGroup>, LinkedList<ImageGroup>> {
                            return Function<LinkedList<ImageGroup>, LinkedList<ImageGroup>> { sections -> sections }
                        }
                    })

            e.onNext(sections)
            e.onComplete()
        }.doOnNext { imageGroups -> cacheSectionedImageGroup(directory.absolutePath, mode, imageGroups) }
    }


    /*
     * 扫描目录
     */

    /**
     * 产生 [RescanImageDirectoryMessage]
     *
     * @param destDir
     * @param onlyRescanDirInCache
     */
    private fun rescanImageDirectory(destDir: File, onlyRescanDirInCache: Boolean) {

        Log.d(TAG, "rescanImageDirectory() called with: destDir = [$destDir], onlyRescanDirInCache = [$onlyRescanDirInCache]")

        var loadImageList = false
        if (onlyRescanDirInCache) {
            try {
                mMediaFileListMapGuard.readLock()
                if (mMediaFileListMap.containsKey(destDir)) {
                    loadImageList = true
                }
            } finally {
                mMediaFileListMapGuard.readUnlock()
            }
        } else {
            loadImageList = true
        }

        if (loadImageList) {

            loadMediaFileList(destDir, LoadMediaFileParam()
                    .setFromCacheFirst(false)
                    .setLoadMediaInfo(false)
                    .setSortOrder(mSortOrder)
                    .setSortWay(mSortWay)
            )
                    .subscribe({ images ->
                        Log.d(TAG, "rescan directory : $destDir")

                        mBus.post(RescanImageDirectoryMessage().setDirectory(destDir))

                    }) { throwable -> Log.e(TAG, "rescan directory with exception : $throwable") }
        } else {
            Log.w(TAG, "rescanImageDirectory: 不扫描目录")
        }

        // Todo load grouped image list
        /*
        loadImageGroupList(destDir, GroupMode.WEEK, false, SortWay.DATE, SortOrder.DESC)
                .subscribe(imageGroups -> {
                    Log.d(TAG, "rescan week grouped directory: " + destDir);
                }, throwable -> {

                });

        loadImageGroupList(destDir, GroupMode.MONTH, false, SortWay.DATE, SortOrder.DESC)
                .subscribe(imageGroups -> {
                    Log.d(TAG, "rescan month grouped directory: " + destDir);
                }, throwable -> {

                });*/
    }

    private fun rescanFolderList(actionDesc: String) {

        loadFolderModel(false)
                .subscribeOn(Schedulers.io())
                .subscribe({ model -> mBus.post(RescanFolderListMessage(actionDesc)) })
                { throwable -> Log.d(TAG, "rescanFolderList: $throwable") }
    }

    /**
     * TODO 增加排序方式参数
     *
     * @param directory
     * @return
     * @throws FileNotFoundException
     */
    @Throws(FileNotFoundException::class)
    fun listImageDirectories(directory: File?, comparator: Comparator<File>): List<File> {
        if (directory == null) {
            throw FileNotFoundException("Cannot found camera directory.")
        }

        val watch = Watch.now()
        val allFiles = directory.listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
                ?: return LinkedList()
        watch.logGlanceMS(TAG, "list directory ")

        val allFileList = LinkedList(Arrays.asList(*allFiles))
        allFileList.add(directory)


        // Long.compare(file2.lastModified(), file.lastModified())
        val files = Stream.of(allFileList)
                .sorted(comparator)
                .toList()
        watch.logGlanceMS(TAG, "Sort list")
        return files
    }

    /*
     * 加载和缓存图片
     */
    private fun cacheSectionedImageGroup(directory: String?, mode: GroupMode, imageGroups: List<ImageGroup>?) {
        Log.d(TAG, "cacheSectionedImageGroup() called with: directory = [$directory], mode = [$mode")

        if (directory == null || imageGroups == null) {
            return
        }

        when (mode) {
            GroupMode.DEFAULT -> {
            }
            GroupMode.DAY -> mDayImageGroupsMap[directory] = imageGroups
            GroupMode.WEEK -> mWeekImageGroupsMap[directory] = imageGroups
            GroupMode.MONTH -> mMonthImageGroupsMap[directory] = imageGroups
        }
    }

    /**
     * 加载多媒体文件（图片、视频）列表，按照参数 `param` 指定的排序方式返回列表。
     *
     * @param directory
     * @param param     指定加载参数：是否优先从缓存加载，是否加载媒体文件信息，指定排序方式
     * @return
     */
    fun loadMediaFileList(directory: File?, param: LoadMediaFileParam): Observable<List<MediaFile>> {
        return Observable.create<List<MediaFile>> { e ->

            mSortWay = param.getSortWay()
            mSortOrder = param.getSortOrder()

            Log.d(TAG, "loadMediaFileList() called with: directory = [$directory], param = [$param]")
            if (directory == null) {
                e.onError(IllegalArgumentException("Argument 'directory' should not be null"))
                return@create
            }
            if (!directory.exists()) {
                e.onError(FileNotFoundException("Directory not found : $directory"))
                return@create
            }

            if (param.isFromCacheFirst()) {

                try {
                    val watch = Watch.now()
                    mMediaFileListMapGuard.readLock()
                    val mediaFileList = mMediaFileListMap[directory]
                    if (mediaFileList != null) {

                        // 1 需要加载媒体文件信息并且缓存已经加载文件信息
                        // 2 或者不需要加载媒体文件信息时，
                        // 可以从缓存读取
                        if (!param.isLoadMediaInfo() || mediaFileList.isLoadedExtraInfo()) {
                            if (mediaFileList.mediaFiles != null) {
                                val mediaFiles = Stream.of(mediaFileList.mediaFiles!!)
                                        .sorted(getMediaFileComparator(param))
                                        .toList()

                                Log.d(TAG, "loadMediaFileList: return cached media file list")
                                e.onNext(mediaFiles)
                                e.onComplete()
                                watch.logGlanceMS(TAG, "load media files from cache and sort it")
                                return@create
                            }
                        }
                    }
                } finally {
                    mMediaFileListMapGuard.readUnlock()
                }
            }

            val watch = Watch.now()
            // Get media file(image/video) list and sort
            val imageFiles = ImageModule.listMediaFiles(directory)

            val comparator = getMediaFileComparator(param)

            val sortedMediaFiles: List<MediaFile>
            if (param.isLoadMediaInfo()) {

                // Load video/image resolution, video duration, file disk usage,
                sortedMediaFiles = Stream.of(imageFiles)
                        .map { file ->

                            // File length
                            val mediaFile = MediaFile()
                            fillBasicFileInfo(mediaFile, file)

                            // 填充媒体文件额外信息

                            // Resolution
                            if (PathUtils.isVideoFile(file.absolutePath)) {
                                mediaFile.setVideoDuration(
                                        MediaUtils.getVideoFileDuration(mContext, file).toLong())
                            } else if (PathUtils.isStaticImageFile(file.absolutePath)) {
                                mediaFile.setMediaResolution(MediaUtils.getImageResolution(file))
                            }

                            mediaFile
                        }
                        .filter { file -> file.getFileSize() > 0 }
                        .sorted(comparator)
                        .toList()

                watch.logGlanceMS(TAG, "Load media files details")
            } else {

                sortedMediaFiles = Stream.of(imageFiles)
                        .map { file ->
                            val mediaFile = MediaFile()
                            fillBasicFileInfo(mediaFile, file)
                            mediaFile
                        }
                        .filter { file -> file.getFileSize() > 0 }
                        .sorted(comparator)
                        .toList()

                watch.logGlanceMS(TAG, "Load media files basic information")
            }

            // Cache media file list
            cacheImageList(directory, sortedMediaFiles, param)

            e.onNext(sortedMediaFiles)
            e.onComplete()
        }
                .doOnNext { fileList ->
                    if (ListUtils.isEmpty(fileList)) {
                        Log.w(TAG, "loadMediaFileList: no file int dir : " + directory!!)
                    }
                }
    }

    private fun fillBasicFileInfo(mediaFile: MediaFile, file: File) {
        mediaFile.file = file
        mediaFile.setFileSize(file.length())
        mediaFile.setDate(Date(file.lastModified()))
    }

    private fun getMediaFileComparator(param: LoadMediaFileParam): Comparator<MediaFile> {
        var comparator: Comparator<MediaFile>? = null
        when (param.getSortWay()) {

            SortWay.NAME -> {
                when (param.getSortOrder()) {

                    SortOrder.DESC -> comparator = MediaFile.MEDIA_FILE_NAME_DESC_COMPARATOR
                    SortOrder.ASC -> comparator = MediaFile.MEDIA_FILE_NAME_ASC_COMPARATOR
                    else -> comparator = MediaFile.MEDIA_FILE_NAME_DESC_COMPARATOR
                }
            }
            SortWay.SIZE -> when (param.getSortOrder()) {

                SortOrder.DESC -> comparator = MediaFile.MEDIA_FILE_LENGTH_DESC_COMPARATOR
                SortOrder.ASC -> comparator = MediaFile.MEDIA_FILE_LENGTH_ASC_COMPARATOR
                else -> comparator = MediaFile.MEDIA_FILE_LENGTH_DESC_COMPARATOR
            }
            SortWay.DATE -> {
                when (param.getSortOrder()) {

                    SortOrder.DESC -> comparator = MediaFile.MEDIA_FILE_DATE_DESC_COMPARATOR
                    SortOrder.ASC -> comparator = MediaFile.MEDIA_FILE_DATE_ASC_COMPARATOR
                    else -> comparator = MediaFile.MEDIA_FILE_DATE_ASC_COMPARATOR
                }
            }
            else -> {
                Log.w(TAG, "getMediaFileComparator: return default comparator for sort way : " + param.getSortWay())
                comparator = MediaFile.MEDIA_FILE_DATE_DESC_COMPARATOR
            }
        }
        return comparator
    }

    private fun cacheImageList(dir: File, mediaFiles: List<MediaFile>, param: LoadMediaFileParam) {
        //        Log.d(TAG, "cacheImageList() called with: dir = [" + dir + "], mediaFiles = [" + mediaFiles + "], param = [" + param + "]");

        mMediaFileListMapGuard.writeConsume { `object` ->
            `object`[dir] = MediaFileList()
                    .setMediaFiles(mediaFiles)
                    .setLoadedExtraInfo(param.isLoadMediaInfo())
        }
    }

    fun listMediaFiles(dir: File?): List<File> {
        if (!dir!!.isDirectory) {
            throw InvalidParameterException("参数 'dir' 对应的目录（" + dir.absolutePath + "）不存在：")
        }

        val allFiles = dir.listFiles(PathUtils.MEDIA_FILENAME_FILTER) ?: return LinkedList()

        return Arrays.asList(*allFiles)
        //        return Stream.of(allFiles)
        //                .sorted((file1, file2) -> Long.compare(file2.lastModified(), file1.lastModified()))
        //                .toList();
    }

    fun loadGalleryFolderList(): Observable<List<ImageFolder>> {

        return loadFolderModel(true)
                .map { folderModel ->
                    val containerFolders = folderModel.containerFolders

                    val imageFolderList = LinkedList<ImageFolder>()
                    folderModel.containerFolders?.let {
                        it.forEachIndexed { index, containerFolder ->
                            imageFolderList.addAll(containerFolder.folders)
                        }
                    }

                    imageFolderList
                }
    }

    fun getSectionFileName(position: Int): String? {
        try {
            mFolderModelRWLock.readLock().lock()

            val sectionForPosition = getSectionForPosition(position)
            val containerFolder = mFolderModel?.containerFolders?.get(sectionForPosition)

            return containerFolder?.mName ?: ""
        } finally {
            mFolderModelRWLock.readLock().unlock()
        }
    }

    @Deprecated("")
    internal fun removeCachedFiles(dirPath: String, files: List<File>) {
        mImageListMap.containsKey(dirPath)
        rescanImageDirectory(File(dirPath), false)
    }

    /**
     * 删除文件，并把在一个目录中的文件扫描一次目录，缓存，然后通知目录发送变化
     *
     * @param files 指定要删除的文件列表
     * @return 返回已经删除的文件个数和删除失败的文件列表
     */
    fun removeFiles(files: Collection<File>): Observable<Pair<Int, List<File>>> {
        return Observable.create { e ->

            val totalRemoved = intArrayOf(0)
            val removeFailedFiles = LinkedList<File>()

            // 文件分组来删除：按照目录来分组，每个目录执行一次批量删除，并重新扫描发送目录扫描通知
            Stream.of(files)
                    .groupBy { it.parent }
                    .forEach(object : Consumer<MutableMap.MutableEntry<String, List<File>>> {
                        override fun accept(stringListEntry: MutableMap.MutableEntry<String, List<File>>) {
                            val dir = stringListEntry.key
                            removeFilesWithNotification(dir, stringListEntry.value)
                        }

                        private fun removeFilesWithNotification(dir: String, files: List<File>) {
                            val count = intArrayOf(0)
                            val removeSuccessFiles = LinkedList<File>()
                            Stream.of(files)
                                    .filter { it.isFile }
                                    .forEach { file ->
                                        if (file.delete()) {
                                            count[0]++
                                            removeSuccessFiles.add(file)
                                        } else {
                                            removeFailedFiles.add(file)
                                        }
                                    }
                            totalRemoved[0] += removeSuccessFiles.size

                            // 扫描对应的图片目录
                            rescanImageDirectory(File(dir), false)

                            // 扫描文件夹列表
                            rescanFolderList("remove files")
                        }
                    })

            val result = Pair<Int, List<File>>(totalRemoved[0], removeFailedFiles)
            e.onNext(result)
            e.onComplete()
        }
    }

    /*
     * 目录管理
     */

    fun renameDirectory(srcDir: File?, newDirName: String): Observable<Boolean> {
        return Observable.create { e ->
            if (srcDir == null) {
                throw IllegalArgumentException("File should not be null")
            }

            if (!srcDir.exists()) {
                throw FileNotFoundException("File not found")
            }

            if (!srcDir.isDirectory) {
                throw IllegalStateException("File is not a directory.")
            }

            if (StringUtils.equals(newDirName, srcDir.name)) {
                throw IllegalArgumentException("The new directory name '$newDirName' equals to the source directory.")
            }

            val destDir = File(srcDir.parentFile, newDirName)
            FileUtils.moveDirectory(srcDir, destDir)

            try {
                mFolderModelRWLock.writeLock().lock()

                var found = false
                mFolderModel?.let {

                    mFolderModel?.containerFolders?.forEach {
                        val folders = it.folders

                        run subloop@{
                            folders.forEach {
                                if (it.file == srcDir) {
                                    found = true
                                    it.name = newDirName

                                    val newDir = File(srcDir.parentFile, newDirName)
                                    it.file = newDir

                                    Log.d(TAG, "renameDirectory: rename folder [" + srcDir.name + "] in cache to new name [" + newDirName + "]")

                                    // Notify
                                    mBus.post(RenameDirectoryMessage()
                                            .setOldDirectory(srcDir)
                                            .setNewDirectory(destDir))
                                    return@subloop
                                }
                            }
                        }

                        if (found) {
                            val sortedImageFolderList = Stream.of(folders)
                                    .sorted { o1, o2 -> StringUtils.compare(o1.name, o2.name) }
                                    .toList()
                            it.setFolders(sortedImageFolderList)

                            mBus.post(RescanFolderListMessage("renameDirectory"))
                        }
                    }
                }

                if (!found) {
                    Log.w(TAG, "renameDirectory: rename director successfully, but update model data failed.")

                    e.onError(Exception("Cannot found the specified directory : $srcDir"))
                    return@create
                }
            } catch (e1: Exception) {
                e1.printStackTrace()
            } finally {
                mFolderModelRWLock.writeLock().unlock()
            }


            e.onNext(true)
            e.onComplete()
        }
    }

    /**
     * Remove file.
     * Generate [RemoveFileMessage] event.
     *
     * @param file The file to remove.
     * @return
     */
    fun removeFile(file: File?): Observable<Boolean> {
        return Observable.create { e ->
            if (file == null) {
                throw IllegalArgumentException("File should not be null")
            }

            if (!file.exists()) {
                throw FileNotFoundException("File not found")
            }

            if (file.delete()) {

                mMediaFileListMapGuard.writeConsume { `object` ->
                    val mediaFileList = `object`[file.parentFile]
                    if (mediaFileList != null) {
                        val mediaFiles = mediaFileList.mediaFiles

                        val i = org.apache.commons.collections4.ListUtils.indexOf(mediaFiles) { object1 -> object1.file == file }
                        if (i != -1) {
                            mediaFiles!!.removeAt(i)
                            Log.d(TAG, "removeFile: $file at $i")
                        }
                    }
                }

                e.onNext(true)
                e.onComplete()

                mBus.post(RemoveFileMessage().setFile(file))
            }
        }
    }


    fun createFolder(dir: File?): Observable<Boolean> {

        return Observable.create { e ->
            if (dir == null) {
                throw IllegalArgumentException("dir must not be null.")
            }

            if (dir.exists()) {
                throw FileExistsException("File already exists : $dir")
            }

            FileUtils.forceMkdir(dir)

            // 更新缓存
            val containerFolders = Stream.of(mFolderModel!!.containerFolders)
                    .filter { value -> SystemUtils.isSameFile(value.getFile(), dir.parentFile) }
                    .limit(1)
                    .toList()

            if (!containerFolders.isEmpty()) {
                val containerFolder = containerFolders[0]

                containerFolder.mFolders?.add(0, createImageFolder(dir))

                containerFolder.setFolders(
                        Stream.of(containerFolder.mFolders)
                                .sorted({ o1, o2 -> o1.file.compareTo(o2.file) })
                                .toList())

                mBus.post(FolderModelChangeMessage())
            }


            //            loadFolderModel(false)
            //                    .subscribeOn(Schedulers.io())
            //                    .observeOn(Schedulers.io())
            //                    .subscribe(model -> {
            //                        mFolderModel = model;
            //                    }, throwable -> {
            //                        Log.d(TAG, "createFolder: reload folder model failed.");
            //                    });

            e.onNext(true)
            e.onComplete()
        }
    }

    /**
     * 删除目录，在操作成功后将产生 [FolderModelChangeMessage] EventBus 消息。
     *
     * @param dir
     * @param forceDelete
     * @return
     * @see FolderModelChangeMessage
     */
    fun removeFolder(dir: File?, forceDelete: Boolean): Observable<Boolean> {

        return Observable.create { e ->
            if (dir == null) {
                throw IllegalArgumentException("dir must not be null")
            }
            if (!dir.isDirectory) {
                throw IllegalArgumentException("Not a directory")
            }

            if (!forceDelete) {
                val files = dir.listFiles()
                if (files != null && files.size > 0) {
                    Log.d(TAG, "removeFolder: 有文件不能移除")
                    throw NotEmptyException("Directory is not empty.")
                }
            }

            // 删除目录，包括里面的任何子目录
            FileUtils.deleteDirectory(dir)

            // 通知目录删除
            mBus.post(cn.intret.app.picgo.model.event.DeleteFolderMessage(dir))

            try {
                mFolderModelRWLock.writeLock().lock()
                Stream.of(mFolderModel!!.containerFolders)
                        .forEach { containerFolder ->
                            val folders = containerFolder.folders
                            val i = org.apache.commons.collections4.ListUtils.indexOf<ImageFolder>(folders
                            ) { `object` -> SystemUtils.isSameFile(`object`.file, dir) }

                            if (i != -1) {
                                Log.d(TAG, "removeFolder: remove folder at $i")
                                containerFolder.mFolders?.removeAt(i)
                            }
                        }

                mBus.post(FolderModelChangeMessage())
            } finally {
                mFolderModelRWLock.writeLock().unlock()
            }

            e.onNext(true)
            e.onComplete()

        }
    }

    /*
     * 移动目录
     */

    fun moveFilesToDirectory(destDir: File, files: List<File>): Observable<Int> {
        return Observable.create { e ->
            val result = MoveFileResult()
            val i = moveFilesTo(destDir, files, false, result, false)

            Log.d(TAG, "moveFilesToDirectory: 移动" + files.size + "个文件到" + destDir)

            // Rescan source dirs
            Stream.of(files).groupBy { it.parent }
                    .forEach { entry -> rescanImageDirectory(File(entry.key), false) }

            // Rescan target dir
            rescanImageDirectory(destDir, false)

            e.onNext(i)
            e.onComplete()
        }
    }

    /**
     * 移动文件到指定的目录，在源目录和目标目录上都会产生 [RescanImageDirectoryMessage] 通知。
     *
     * @param destDir
     * @param sourceFiles
     * @param detectConflict
     * @param deleteIfTargetExists
     * @return
     */
    fun moveFilesToDirectory(destDir: File, sourceFiles: List<File>,
                             detectConflict: Boolean, deleteIfTargetExists: Boolean): Observable<MoveFileResult> {
        return Observable.create { e ->
            val result = MoveFileResult()
            val i = moveFilesTo(destDir, sourceFiles, detectConflict, result, deleteIfTargetExists)

            Log.d(TAG, "moveFilesToDirectory: 移动" + result.successFiles.size + "/" + sourceFiles.size + "个文件到" + destDir)

            // Rescan source dirs
            Stream.of(sourceFiles)
                    .groupBy { it.parent }
                    .forEach { entry -> rescanImageDirectory(File(entry.key), false) }

            // Rescan target dir
            rescanImageDirectory(destDir, false)

            e.onNext(result)
            e.onComplete()
        }
    }

    fun detectFileConflict(destDir: File, sourceFiles: List<File>): Observable<MoveFileDetectResult> {
        return Observable.create { e ->

            if (sourceFiles.isEmpty()) {
                throw IllegalArgumentException("Source file list is empty.")
            }

            val canMoveFiles = LinkedList<Pair<File, File>>()
            val conflictFiles = detectFileConflict(destDir, sourceFiles, canMoveFiles)

            val moveFileDetectResult = MoveFileDetectResult()
                    .setCanMoveFiles(canMoveFiles)
                    .setConflictFiles(conflictFiles)
                    .setTargetDir(destDir)

            e.onNext(moveFileDetectResult)
            e.onComplete()
        }
    }

    /**
     * 检测源文件列表在别的目录中是否出现
     *
     * @param sourceFiles
     * @return
     */
    fun detectFileExistence(sourceFiles: List<File>): Observable<DetectFileExistenceResult> {
        return Observable.create { e ->
            val result = DetectFileExistenceResult()
            try {
                mFolderModelRWLock.readLock().lock()

                mFolderModel?.containerFolders?.forEach {
                    for (imageFolder in it.folders) {

                        imageFolder.getMediaFiles()?.let {
                            val conflictFiles = intersectMoveFiles(
                                    imageFolder.file,
                                    it.toList(), sourceFiles, null)
                            if (!conflictFiles.isEmpty()) {
                                result.existedFiles?.put(imageFolder.file, Stream.of(conflictFiles)
                                        .map { fileFilePair -> fileFilePair.first }
                                        .toList())
                            }
                        }
                    }
                }

                e.onNext(result)
                e.onComplete()

            } catch (th: Throwable) {
                e.onError(th)
            } finally {
                mFolderModelRWLock.readLock().unlock()
            }
        }
    }

    /**
     * @param destDir
     * @param destDirFiles All the files should in the same directory
     * @param sourceFiles  The files are not always in the same directory.
     * @return
     */
    private fun intersectMoveFiles(destDir: File,
                                   destDirFiles: List<File>,
                                   sourceFiles: List<File>,
                                   outCanMoveFiles: MutableList<Pair<File, File>>?): List<Pair<File, File>> {
        outCanMoveFiles?.clear()

        if (destDirFiles.isEmpty()) {
            // No conflict files
            Stream.of(sourceFiles)
                    .forEach { file ->
                        outCanMoveFiles?.add(Pair(File(destDir, file.name), file))
                    }
            return ArrayList()
        }

        val result = ArrayList<Pair<File, File>>()

        val dir = destDirFiles[0].parentFile

        val destDirFileNames = PathUtils.fileListToNameList(destDirFiles)
        val destDirFileNameHashSet = HashSet(destDirFileNames)

        for (sourceFile in sourceFiles) {

            val name = sourceFile.name
            if (destDirFileNameHashSet.contains(name)) {
                result.add(Pair(File(dir, name), sourceFile))
                // TODO if the source files are in the same directory, we should uncomment the code below
                // destDirFileNameHashSet.remove(l);
            } else {
                outCanMoveFiles?.add(Pair(File(dir, name), sourceFile))
            }
        }
        return result
    }

    private fun moveFilesTo(destDir: File?, sourceFiles: List<File>, detectConflict: Boolean, outResult: MoveFileResult,
                            deleteIfExists: Boolean): Int {
        if (destDir == null) {
            throw IllegalArgumentException("Argument 'destDir' is null.")
        }

        if (!destDir.isDirectory) {
            throw IllegalArgumentException("Argument 'destDir' is not a valid directory.")
        }

        if (ListUtils.isEmpty(sourceFiles)) {
            return 0
        }

        // Detect file name conflict
        val canMoveFiles = LinkedList<Pair<File, File>>()
        val conflictFiles: List<Pair<File, File>>
        if (detectConflict) {
            conflictFiles = detectFileConflict(destDir, sourceFiles, canMoveFiles)
            outResult.conflictFiles = conflictFiles
        }

        val successFiles = LinkedList<Pair<File, File>>()
        val failedFiles = LinkedList<Pair<File, File>>()

        if (detectConflict) {

            var successCount = 0
            try {

                for (filePair in canMoveFiles) {
                    if (filePair.second == null) {
                        continue
                    }
                    FileUtils.moveFileToDirectory(filePair.second, destDir, true)
                    Log.d(TAG, "move file " + filePair.second + " to " + destDir)

                    successFiles.add(filePair)

                    ++successCount
                }

            } catch (e: Exception) {
                Log.e(TAG, "move files to $destDir failed.")
                e.printStackTrace()
            }

            outResult.successFiles = successFiles
            outResult.failedFiles = failedFiles

            return successCount
        } else {

            var successCount = 0
            var srcFile: File?
            try {

                for (i in sourceFiles.indices) {
                    srcFile = sourceFiles[i]
                    if (srcFile == null) {
                        continue
                    }
                    val destFile = File(destDir, srcFile.name)
                    if (destFile.exists() && deleteIfExists) {
                        Log.d(TAG, "moveFilesTo: delete target file first : $destFile")
                        destFile.delete()
                    }
                    FileUtils.moveFileToDirectory(srcFile, destDir, true)
                    Log.d(TAG, "move file $srcFile to $destDir")

                    successFiles.add(Pair(destFile, srcFile))

                    successCount++
                }
            } catch (e: IOException) {
                Log.e(TAG, "move files to $destDir failed.")
                e.printStackTrace()
            }

            outResult.successFiles = (successFiles)
            outResult.failedFiles = failedFiles
            return successCount
        }
    }

    /*
     * @return Conflict files
     */
    private fun detectFileConflict(destDir: File, sourceFiles: List<File>,
                                   outCanMoveFiles: MutableList<Pair<File, File>>): List<Pair<File, File>> {
        return intersectMoveFiles(destDir, Stream.of(*destDir.listFiles()).toList(),
                sourceFiles, outCanMoveFiles)

    }

    /**
     * 任何解决后的图片，它所在的目录都会被扫描一次。
     *
     * @param compareItems
     * @return
     * @
     */
    fun resolveFileNameConflict(compareItems: List<CompareItem>): Observable<CompareItemResolveResult> {
        return Observable.create { e ->
            if (ListUtils.isEmpty(compareItems)) {
                throw IllegalArgumentException("Argument 'compareItems' shouldn't be null/empty.")
            }

            val deletedFiles = LinkedList<File>()

            loop@ for (compareItem in compareItems) {

                check@ when (compareItem.result) {

                    ResolveResult.KEEP_SOURCE -> {
                        val targetFile = compareItem.targetFile
                        if (targetFile == null) {
                            e.onError(IllegalStateException("Cannot remove 'null' target file for " + compareItem.result))

                        } else {
                            if (!targetFile.exists()) {
                                e.onError(FileNotFoundException("File not found : $targetFile"))
                                continue@loop
                            }
                            if (targetFile.isDirectory) {
                                e.onError(IllegalStateException("File shouldn't be a directory : $targetFile"))
                                continue@loop
                            } else {
                                val delete = targetFile.delete()
                                if (delete) {
                                    deletedFiles.add(targetFile)
                                }
                                e.onNext(CompareItemResolveResult(compareItem, delete))
                            }
                        }
                    }
                    ResolveResult.KEEP_TARGET -> {
                        val sourceFile = compareItem.sourceFile
                        if (sourceFile == null) {
                            e.onError(IllegalStateException("Cannot remove 'null' source file for " + compareItem.sourceFile))
                            continue@loop
                        }

                        if (!sourceFile.exists()) {
                            e.onError(FileNotFoundException("File not found : $sourceFile"))
                            continue@loop
                        }

                        if (sourceFile.isDirectory) {
                            e.onError(IllegalStateException("Cannot remove a directory for  : " + compareItem.result))
                            continue@loop
                        }

                        val delete = sourceFile.delete()
                        if (delete) {
                            deletedFiles.add(sourceFile)
                        }
                        e.onNext(CompareItemResolveResult(compareItem, delete))
                    }
                    ResolveResult.KEEP_BOTH -> {
                        e.onNext(CompareItemResolveResult(compareItem, true))
                    }
                    ResolveResult.NONE -> e.onNext(CompareItemResolveResult(compareItem, false))
                    else -> {
                        continue@loop
                    }
                }
            } // END of for

            Stream.of(deletedFiles)
                    .groupBy { it.parentFile }
                    .forEach { fileListEntry ->
                        val dir = fileListEntry.key
                        rescanImageDirectory(dir, false)
                    }
            e.onComplete()
        }
    }

    fun loadImageFilesInfo(files: List<File>): Single<List<ImageFileInformation>> {
        return Single.create { e ->
            if (files == null) {
                throw IllegalArgumentException("参数为空")
            }


            val fileInformationList = Stream.of(files)
                    .map<ImageFileInformation> { file ->
                        try {
                            return@map getImageFileInformation(file)
                        } catch (e1: IOException) {
                            e1.printStackTrace()
                        }

                        null
                    }
                    .filter(Predicate<ImageFileInformation> { cn.intret.app.picgo.utils.Objects.nonNull(it) })
                    .toList()

            e.onSuccess(fileInformationList)
        }
    }

    fun loadImageFilesInfoMap(files: List<File>): Single<Map<File, ImageFileInformation>> {
        return Single.create { e ->
            if (files == null) {
                throw IllegalArgumentException("参数为空")
            }

            val map = LinkedHashMap<File, ImageFileInformation>()


            Stream.of(files).forEach { file ->
                try {
                    map[file] = getImageFileInformation(file)
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }
            }

            e.onSuccess(map)
        }
    }


    /**
     * @implNote https://developer.android.com/reference/android/media/ExifInterface.html
     */
    fun loadImageInfo(mediaFile: File?): Observable<ImageFileInformation> {
        return Observable.create { e ->
            if (mediaFile == null) {
                throw IllegalArgumentException("ImageFile should not be null.")
            }
            if (mediaFile.isDirectory) {
                throw IllegalArgumentException("File should not be a directory.")
            }

            val info = getImageFileInformation(mediaFile)

            e.onNext(info)
            e.onComplete()
        }
    }

    @Throws(IOException::class)
    private fun getImageFileInformation(mediaFile: File?): ImageFileInformation {
        val info: ImageFileInformation
        info = ImageFileInformation()

        val fileAbsPath = mediaFile!!.absolutePath
        val staticImageFile = PathUtils.isStaticImageFile(fileAbsPath)
        val videoFile = PathUtils.isVideoFile(fileAbsPath)
        if (staticImageFile) {
            val imageResolution = MediaUtils.getImageResolution(mediaFile)
            info.mediaResolution = imageResolution

        } else {
            if (videoFile) {
                val videoResolution = MediaUtils.getVideoResolution(mContext, mediaFile)

                info.mediaResolution = videoResolution
                info.videoDuration = MediaUtils.getVideoFileDuration(mContext, mediaFile)
            } else {
                Log.w(TAG, "loadImageInfo: don't load media file information : $mediaFile")
            }
        }

        info.setLastModified(mediaFile.lastModified())
        info.setFileLength(mediaFile.length())


        // Exif
        if (PathUtils.isExifFile(fileAbsPath)) {
            val exifInterface = ExifInterface(fileAbsPath)
            info.setExif(exifInterface)
        }
        return info
    }

    fun hiddenFolder(selectedDir: File): Observable<Boolean> {
        return Observable.create { e ->
            try {
                mFolderModelRWLock.writeLock().lock()

                var foundIndex = -1
                var foundSubIndex = -1

                mFolderModel?.containerFolders?.forEachIndexed folderContainerLoop@{ index, containerFolder ->

                    containerFolder.folders.forEachIndexed folderLoop@{ subIndex, imageFolder ->
                        if (imageFolder.file == selectedDir) {
                            foundIndex = index
                            foundSubIndex = index
                            return@folderLoop
                        }
                    }

                    if (foundSubIndex != -1) {
                        return@folderContainerLoop
                    }
                }

                if (foundIndex != -1) {
                    val removedFolder = mFolderModel?.containerFolders?.get(foundIndex)?.mFolders?.removeAt(foundSubIndex)
                    if (removedFolder == null) {
                        e.onError(RuntimeException("Remove folder model from cache failed."))
                    } else {
                        e.onNext(true)
                        e.onComplete()
                    }
                } else {
                    e.onNext(false)
                    e.onComplete()
                }
            } finally {
                mFolderModelRWLock.writeLock().unlock()
            }
        }
    }


    private val TAG = ImageModule::class.java.simpleName

    /**
     * 文件列表项获取的缩略图个数
     */
    private val DEFAULT_THUMBNAIL_COUNT = 3
    val FOLDER_LIST_NAME_ASC_COMPARATOR = Comparator<File> { file1, file2 -> StringUtils.compare(file1.name, file2.name) }

}

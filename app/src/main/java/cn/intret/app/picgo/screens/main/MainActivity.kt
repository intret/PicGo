package cn.intret.app.picgo.screens.main

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.PersistableBundle
import android.support.annotation.MainThread
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.app.SharedElementCallback
import android.support.v4.util.Pair
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.*
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import cn.intret.app.picgo.R
import cn.intret.app.picgo.app.MyApp
import cn.intret.app.picgo.model.event.*
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.model.image.data.*
import cn.intret.app.picgo.model.user.UserModule
import cn.intret.app.picgo.model.user.data.*
import cn.intret.app.picgo.screens.adapter.*
import cn.intret.app.picgo.screens.adapter.brvah.*
import cn.intret.app.picgo.screens.base.BaseDaggerAppCompatActivity
import cn.intret.app.picgo.screens.conflict.ConflictResolverDialogFragment
import cn.intret.app.picgo.screens.event.CurrentImageChangeMessage
import cn.intret.app.picgo.screens.event.MoveFileResultMessage
import cn.intret.app.picgo.screens.exclude.ExcludeFolderDialogFragment
import cn.intret.app.picgo.screens.floating.FloatWindowService
import cn.intret.app.picgo.screens.image.DragPhotoActivity
import cn.intret.app.picgo.screens.image.ImageActivity
import cn.intret.app.picgo.screens.move.MoveFileDialogFragment
import cn.intret.app.picgo.screens.pref.SettingActivity
import cn.intret.app.picgo.utils.*
import cn.intret.app.picgo.view.T9KeypadView
import cn.intret.app.picgo.widget.EmptyRecyclerView
import cn.intret.app.picgo.widget.RecyclerItemTouchListener
import cn.intret.app.picgo.widget.SectionDecoration
import cn.intret.app.picgo.widget.SuperRecyclerView
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.sectionedrecyclerview.ItemCoord
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.allenliu.badgeview.BadgeFactory
import com.allenliu.badgeview.BadgeView
import com.annimon.stream.Collector
import com.annimon.stream.Stream
import com.annimon.stream.function.BiConsumer
import com.annimon.stream.function.Function
import com.annimon.stream.function.Predicate
import com.annimon.stream.function.Supplier
import com.jakewharton.rxrelay2.BehaviorRelay
import com.orhanobut.logger.Logger
import com.pawegio.kandroid.w
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotterknife.bindView
import org.apache.commons.lang3.StringUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * Main screen of app
 * TODO: extract drawer to fragment
 */
class MainActivity : BaseDaggerAppCompatActivity(), MainContract.View {

    // ---------------------------------------------------------------------------------------------
    // 图片列表
    // ---------------------------------------------------------------------------------------------

    internal val mImageRefresh: SwipeRefreshLayout by bindView(R.id.refresh)
    internal val mImageList: SuperRecyclerView by bindView(R.id.img_list)
    internal val mEmptyView: View by bindView(R.id.empty_view)

    internal val mFloatingToolbar: Toolbar by bindView(R.id.floatingToolbar)
    internal val mModeRadioGroup: RadioGroup by bindView(R.id.view_mode)
    internal val mImageToolbar: ViewGroup by bindView(R.id.image_tool_bar)

    // ---------------------------------------------------------------------------------------------
    // 抽屉菜单/文件夹列表
    // ---------------------------------------------------------------------------------------------


    internal val mDrawerLayout: DrawerLayout by bindView(R.id.drawer_layout)
    internal val mFolderListRefresh: SwipeRefreshLayout by bindView(R.id.folder_list_refresh)
    internal val mFolderList: EmptyRecyclerView by bindView(R.id.drawer_folder_list)
    internal val mFolderListEmptyView: TextView by bindView(R.id.folder_list_empty_view)

    internal val mKeypadContainer: ViewGroup by bindView(R.id.t9_keypad_container)
    internal val mKeypad: T9KeypadView by bindView(R.id.t9_keypad)
    internal val mKeypadSwitchLayout: ViewGroup by bindView(R.id.keyboard_switch_layout)
    internal val mKeypadSwitch: ImageView by bindView(R.id.keyboard_switch)
    internal val mKeypadSwitchBadge: View by bindView(R.id.keyboard_switch_badge)

    private var mDialpadSwitchBadge: BadgeView? = null

    // ------------------------------------------------
    // ActionBar/Toolbar
    // ------------------------------------------------


    private var mToolbar: Toolbar? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null

    // ------------------------------------------------
    // Folder list
    // ------------------------------------------------

    private var mFolderListAdapter: FolderListAdapter? = null

    // ------------------------------------------------
    // The showing image list corresponding folder
    // ------------------------------------------------

    private var mCurrentFolder: File? = null
    private var mFolderAdapter: SectionedFolderListAdapter? = null

    // ------------------------------------------------
    // Floating view
    // ------------------------------------------------

    private var mStartFloatingIntent: Intent? = null


    // ---------------------------------------------------------------------------------------------
    // Image List
    // ---------------------------------------------------------------------------------------------


    /**
     * Default image list adapter cache
     * Key: Directory
     * Value: DefaultImageListAdapter
     */
    internal var mImageListAdapters: MutableMap<File, DefaultImageListAdapter> = LinkedHashMap()
    private var mCurrentImageAdapter: DefaultImageListAdapter? = null
    private var mGridLayoutManager: GridLayoutManager? = null

    // todo Map<ViewMode,BaseImageAdapter>

    internal var mDetailImageListAdapters: MutableMap<File, DetailImageAdapter> = LinkedHashMap()
    internal var mWeekSectionedImageListAdapters: MutableMap<String, SectionedImageListAdapter> = LinkedHashMap()
    internal var mDaySectionedImageListAdapters: MutableMap<String, SectionedImageListAdapter> = LinkedHashMap()
    internal var mMonthSectionedImageListAdapters: MutableMap<String, SectionedImageListAdapter> = LinkedHashMap()

    private var mGridLayout: GridLayoutManager? = null
    private var mCurrentAdapter: SectionedImageListAdapter? = null

    internal var mImageSelectCountRelay: BehaviorRelay<DefaultImageListAdapter> = BehaviorRelay.create()
    internal var mDetailImageSelectCountRelay: BehaviorRelay<DetailImageAdapter> = BehaviorRelay.create()

    // ------------------------------------------------
    // Image list configuration
    // ------------------------------------------------

    private var mSpanCount: Int = 0 // 图片列表网格列数
    private var mCurrentViewerImageIndex = -1 // 图片查看器显示的图片索引

    // Define how to show image list
    private var mViewState = ListViewState()
    private var mMovingViewState: Boolean = false

    // ------------------------------------------------
    // Data loading status
    // ------------------------------------------------

    private var mIsFolderListLoaded = false
    private var mIsImageListLoaded = false

    internal var mRecentHistory: MutableList<File> = LinkedList()
    private var mCurrentDetailImageAdapter: DetailImageAdapter? = null
    private var mLinearLayoutManager: LinearLayoutManager? = null
    private var mExpandableFolderAdapter: ExpandableFolderAdapter? = null
    private var mUpdateImageConflictFileDisposable: Disposable? = null
    private var mUpdateDetailImageListConflictDisposable: Disposable? = null

    @Inject
    internal lateinit var mPresenter: MainPresenter<MainActivity>

    // 拨号盘状态
    private var mEnableT9Filter = false
    private var mCurrentT9Number: String? = null

    private var mFolderItemContextMenu: ListPopupWindow? = null

    internal val currentSelectedCount: Pair<Int, Int>?
        get() = mapValueTo<Pair<Int, Int>>(mViewState.getViewMode(), MapAction1 {
            when (it) {

                ViewMode.GRID_VIEW -> if (mCurrentImageAdapter != null) {
                    return@MapAction1 Pair(
                            mCurrentImageAdapter!!.selectedItemCount, mCurrentDetailImageAdapter!!.selectedItemCount)
                }
                ViewMode.LIST_VIEW -> if (mCurrentDetailImageAdapter != null) {
                    return@MapAction1 Pair(
                            mCurrentDetailImageAdapter!!.selectedItemCount, mCurrentDetailImageAdapter!!.itemCount
                    )
                }
                ViewMode.UNKNOWN -> {
                }
                null -> {

                }
            }
            null
        }
        )

    private val imageAdapterSelectCountChangeRelay: BehaviorRelay<DefaultImageListAdapter>
        get() {
            if (mImageSelectCountRelay == null) {
                mImageSelectCountRelay = BehaviorRelay.create()
            }
            return mImageSelectCountRelay
        }

    private val detailAdapterSelectCountChangeRelay: BehaviorRelay<DetailImageAdapter>
        get() {
            if (mDetailImageSelectCountRelay == null) {
                mDetailImageSelectCountRelay = BehaviorRelay.create()
            }
            return mDetailImageSelectCountRelay
        }

    internal val currentAdapter: BaseImageAdapter<*, *>?
        get() {
            when (mViewState.getViewMode()) {

                ViewMode.GRID_VIEW -> return mCurrentImageAdapter
                ViewMode.LIST_VIEW -> return mCurrentDetailImageAdapter
                ViewMode.UNKNOWN -> {
                }
            }
            Log.w(TAG, "getCurrentAdapter: Fix this")
            return mCurrentImageAdapter
        }

    private val selectedFileList: List<String>
        get() = Stream.of(currentSelectedFilePathList).map { it.absolutePath }.toList()

    private val currentSelectedFilePathList: List<File>
        get() {
            when (mViewState.getViewMode()) {
                ViewMode.GRID_VIEW -> if (mCurrentImageAdapter != null) {
                    val selectedItems = mCurrentImageAdapter!!.selectedItems
                    return Stream.of(selectedItems)
                            .map({ it.file })
                            .toList()
                }
                ViewMode.LIST_VIEW -> {
                    if (mCurrentDetailImageAdapter != null) {
                        val selectedItems = mCurrentDetailImageAdapter!!.selectedItems
                        return Stream.of(selectedItems)
                                .map({ it.file })
                                .toList()

                    }
                }
                ViewMode.UNKNOWN -> {
                }
            }
            return LinkedList()
        }


    override fun onCreate(savedInstanceState: Bundle?) {

        var myApp = application as MyApp


        super.onCreate(savedInstanceState)

//        DaggerAppComponent.create()
//                .inject(this)

        val watch = Watch.now()
        setContentView(R.layout.activity_main)
        watch.logGlanceMS(TAG, "setContentView")


        ButterKnife.bind(this)
        watch.logGlanceMS(TAG, "ButterKnife")

        initStatusBar()
        initToolBar()
        initDrawer()
        watch.logGlanceMS(TAG, "init toolbar & drawer")


        initImageList()
        watch.logGlanceMS(TAG, "init image list")


        initTransition()
        watch.logGlanceMS(TAG, "")


        EventBus.getDefault().register(this)
        watch.logGlanceMS(TAG, "registerEventBus")


        watch.logTotalMS(TAG, "onCreate")
        //showFloatingWindow();


        // TODO: use dagger2 to inject it
//        mPresenter = MainPresenter(this)
    }

    override fun onStart() {
        Log.d(TAG, "onStart: ")

        super.onStart()

        val watch = Watch.now()
        //        loadFolderModel();
        reloadImageList(false)

        watch.logTotalMS(TAG, "onStart")
    }

    override fun onStop() {
        Log.d(TAG, "onStop: ")


        //        changeFloatingCount(FloatWindowService.MSG_DECREASE);
        super.onStop()
    }

    override fun onDestroy() {
        //        stopService(mStartFloatingIntent);
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "onSaveInstanceState() called with: outState = [$outState]")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState() called with: savedInstanceState = [$savedInstanceState]")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle, persistentState: PersistableBundle) {
        super.onRestoreInstanceState(savedInstanceState, persistentState)
        Log.d(TAG, "onRestoreInstanceState() called with: savedInstanceState = [$savedInstanceState], persistentState = [$persistentState]")
    }

    private fun initStatusBar() {
        //        StatusBarUtil.setColor(this, getResources().getColor(R.color.black), 0);
        //        StatusBarUtil.setTranslucent(this, 0);
    }

    private fun initImageList() {

        // Image list header
        mFloatingToolbar.inflateMenu(R.menu.image_action_menu)
        mFloatingToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_copy -> {
                    ToastUtils.toastShort(this@MainActivity, "copy")
                }
                R.id.action_move -> showMoveFileDialog()
                R.id.action_remove -> showRemoveFileDialog()
            }
            false
        }

        // Refresh
        mImageRefresh.setOnRefreshListener { reloadImageList(true) }

        // EmptyView
        mImageList.setEmptyView(mEmptyView)
        //initListViewToolbar();
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.main_menu, menu)

        val viewMode = UserModule.viewMode.get()
        if (viewMode == ViewMode.UNKNOWN) {
            Log.e(TAG, "onCreateOptionsMenu: invalid view mode : $viewMode")
        } else {

            val item = menu.findItem(R.id.app_bar_view_mode)
            when (viewMode) {
                ViewMode.GRID_VIEW -> item.setIcon(R.drawable.ic_grid_on_black_24px)
                ViewMode.LIST_VIEW -> item.setIcon(R.drawable.ic_list_black_24px)
            }
        }

        val showHiddenFile = UserModule.showHiddenFilePreference.get()
        val item = menu.findItem(R.id.app_bar_show_hidden_folder)
        if (item != null) {
            item.isChecked = showHiddenFile
        }

        return true
    }

    private fun initTransition() {

        ActivityCompat.setEnterSharedElementCallback(this, object : SharedElementCallback() {
            override fun onMapSharedElements(names: List<String>?, sharedElements: Map<String, View>?) {
                Log.d(TAG, "enter onMapSharedElements() called with: names = [$names], sharedElements = [$sharedElements]")
                super.onMapSharedElements(names, sharedElements)
            }
        })


        ActivityCompat.setExitSharedElementCallback(this, object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>?, sharedElements: MutableMap<String, View>?) {

                Log.d(TAG, "exit before onMapSharedElements() called with: names = [$names], sharedElements = [$sharedElements]")
                try {

                    if (mCurrentViewerImageIndex >= 0) {
                        var file: File? = null
                        when (mViewState.getViewMode()) {
                            ViewMode.GRID_VIEW -> {
                                val item = mCurrentImageAdapter!!.getItem(mCurrentViewerImageIndex)
                                if (item != null) {
                                    file = item.file
                                }
                            }

                            ViewMode.LIST_VIEW -> {
                                val item = mCurrentDetailImageAdapter!!.getItem(mCurrentViewerImageIndex)
                                if (item != null) {
                                    file = item.file
                                }
                            }
                        }

                        if (file != null) {
                            val filePath = file.absolutePath
                            val transitionName = TransitionUtils.generateTransitionName(filePath)
                            val fileTypeTransitionName = TransitionUtils.generateTransitionName(
                                    TransitionUtils.TRANSITION_PREFIX_FILETYPE, filePath)

                            val addFileTypeTransitionName = PathUtils.isVideoFile(filePath)

                            sharedElements!!.clear()

                            val vh = mImageList.findViewHolderForAdapterPosition(mCurrentViewerImageIndex)
                            if (vh is DefaultImageListAdapter.ViewHolder) {

                                sharedElements[transitionName] = vh.getView(R.id.image)

                                if (addFileTypeTransitionName) {
                                    sharedElements[fileTypeTransitionName] = vh.getView(R.id.file_type)
                                }
                            } else if (vh is DetailImageAdapter.ItemViewHolder) {
                                sharedElements[transitionName] = vh.getView(R.id.image)

                                if (addFileTypeTransitionName) {
                                    sharedElements[fileTypeTransitionName] = vh.getView(R.id.file_type)
                                }
                            }

                            names!!.clear()
                            names.add(transitionName)

                            if (addFileTypeTransitionName) {
                                names.add(fileTypeTransitionName)
                            }
                        }

                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }

                Log.d(TAG, "exit after onMapSharedElements() called with: names = [$names], sharedElements = [$sharedElements]")

                super.onMapSharedElements(names, sharedElements)
            }
        })

    }

    private fun showConflictDialog(destDir: File?, conflictFiles: List<android.util.Pair<File, File>>) {

        if (conflictFiles.isEmpty()) {
            Log.w(TAG, "showConflictDialog: conflict files is empty")
            return
        }
        Logger.d("Show conflict dialog : " + destDir!!)

        val strings = conflictFiles
                .filter { pair -> pair.second != null }
                .map { fileFilePair: android.util.Pair<File, File> -> fileFilePair.second?.absolutePath }
                .toList()


        val fragment = ConflictResolverDialogFragment.newInstance(destDir.absolutePath, ArrayList<String>(strings))
        fragment.show(supportFragmentManager, "Conflict Resolver Dialog")
    }

    // ---------------------------------------------------------------------------------------------
    // EventBus 消息处理
    // ---------------------------------------------------------------------------------------------

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: RecentOpenFolderListChangeMessage) {
        Log.d(TAG, "onEvent() called with: message = [$message]")

        mRecentHistory = Stream.of<RecentRecord>(message.getRecentRecord()!!).map<File> { recentRecord -> File(recentRecord.filePath) }.toList()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: MoveFileResultMessage) {

        message.getDestDir()
                ?.let { getDefaultImageListAdapter(it) }
                ?.let {
                    mFolderAdapter?.updateSelectedCount(message.getDestDir(), it.selectedItemCount)
                    mFolderAdapter?.updateItemCount(message.getDestDir(), it.itemCount)
                }

        message.getResult()?.successFiles?.let {
            Stream.of(it)
                    .groupBy({ fileFilePair -> fileFilePair.second.parentFile })
                    .forEach { objectListEntry ->
                        val dir = objectListEntry.key
                        val adapter = getDefaultImageListAdapter(dir)
                        if (adapter != null) {
                            val selectedCount = adapter.selectedItemCount
                            val itemCount = adapter.itemCount
                            mFolderAdapter!!.updateSelectedCount(message.getDestDir(), selectedCount)
                            mFolderAdapter!!.updateItemCount(message.getDestDir(), itemCount)
                        }
                    }
        }

        showConflictDialog(
                message.getDestDir(),
                message.getResult()!!.conflictFiles)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: ConflictResolveResultMessage) {
        val compareItems = message.getCompareItems()

        for (compareItem in compareItems) {
            if (compareItem.resolved) {


                val item = compareItem.compareItem
                when (item.result) {

                    ResolveResult.KEEP_SOURCE -> {
                    }
                    ResolveResult.KEEP_TARGET -> {
                    }
                    ResolveResult.KEEP_BOTH -> {
                    }
                    ResolveResult.NONE -> {
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: CurrentImageChangeMessage) {
        Log.d(TAG, "onEvent() called with: message = [$message]")
        mCurrentViewerImageIndex = message.getPosition()

        mImageList.scrollToPosition(mCurrentViewerImageIndex)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: RemoveFileMessage) {
        val file = message.getFile()
        if (file != null) {
            val dir = file.parentFile

            when (mViewState.getViewMode()) {

                ViewMode.GRID_VIEW -> {
                    val adapter = getDefaultImageListAdapter(dir)
                    if (adapter != null) {
                        Log.d(TAG, "onEvent: RemoveFileMessage ")
                        diffUpdateDefaultImageListAdapter(adapter, true, false)
                    }
                }
                ViewMode.LIST_VIEW -> {
                    val detailImageAdapter = getDetailImageAdapter(dir)
                    if (detailImageAdapter != null) {
                        diffUpdateDetailImageAdapter(detailImageAdapter, true, false)
                    }
                }
                ViewMode.UNKNOWN -> {
                }
            }


            updateFolderListItemThumbnailList(file.parentFile)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: DeleteFolderMessage) {

        if (mCurrentFolder != null && mCurrentFolder == message.getDir()) {
            mCurrentFolder = null
        }

        when (mViewState.getViewMode()) {

            ViewMode.GRID_VIEW -> {
                if (mCurrentImageAdapter != null) {
                    if (mCurrentImageAdapter!!.directory == message.getDir()) {
                        Log.w(TAG, "set mCurrentImageAdapter/mImageList.adapter to null")
                        mCurrentImageAdapter = null
                        mImageList.adapter = null
                    }
                }
            }
            ViewMode.LIST_VIEW -> {
                if (mCurrentDetailImageAdapter != null) {
                    if (mCurrentDetailImageAdapter!!.directory == message.getDir()) {
                        Log.w(TAG, "set mCurrentDetailImageAdapter/mImageList.adapter to null")
                        mCurrentDetailImageAdapter = null
                        mImageList.adapter = null
                    }
                }
            }
            ViewMode.UNKNOWN -> {
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: RescanFolderThumbnailListMessage) {
        Log.d(TAG, "onEvent() called with: message = [$message]")

        mFolderAdapter!!.updateThumbList(message.getDirectory(), message.getThumbnails().toMutableList())
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    fun onEvent(message: RescanImageDirectoryMessage) {

        Log.d(TAG, "onEvent() called with: message = [$message]")

        val dir = message.getDirectory()

        // 更新目录对应 Adapter
        val adapter = getDefaultImageListAdapter(dir)
        if (adapter != null) {
            diffUpdateDefaultImageListAdapter(adapter, true, false)
        }

        val detailImageAdapter = getDetailImageAdapter(dir)
        if (detailImageAdapter != null) {
            diffUpdateDetailImageAdapter(detailImageAdapter, true, false)
        }

        // 更新抽屉中的缩略图列表
        updateFolderListItemThumbnailList(dir)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: RescanFolderListMessage) {
        Logger.d("RescanFolderListMessage $message")

        reloadFolderList(true)
        //        diffUpdateFolderListAdapter(mFolderAdapter, true);
    }

    fun isCurrentShowDirectory(dir: File?): Boolean {
        when (mViewState.getViewMode()) {

            ViewMode.GRID_VIEW -> {
                if (mCurrentImageAdapter == null) {
                    return false
                }
                if (mCurrentImageAdapter!!.directory != null) {
                    return mCurrentImageAdapter!!.directory == dir
                }
            }
            ViewMode.LIST_VIEW -> {
                if (mCurrentDetailImageAdapter == null) {
                    return false
                }
                if (mCurrentDetailImageAdapter!!.directory != null) {
                    return mCurrentDetailImageAdapter!!.directory == dir
                }
            }
            ViewMode.UNKNOWN -> {
            }
        }
        return false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: RenameDirectoryMessage) {

        Logger.d("RenameDirectoryMessage $message")
        val oldDirectory = message.getOldDirectory()
        //mFolderAdapter.renameDirectory(oldDirectory, message.getNewDirectory());


        // Update Title & Subtitle
        if (isCurrentShowDirectory(message.getOldDirectory())) {
            val result = currentSelectedCount
            if (result != null) {
                updateActionBarTitle(message.getNewDirectory()!!.name)

                mToolbar!!.subtitle = getSubTitleText(result.first!!, result.second!!)
            }
        }

        // Update : Image list adapter
        val defaultImageListAdapter = getDefaultImageListAdapter(oldDirectory)
        if (defaultImageListAdapter != null) {

            mImageListAdapters.remove(oldDirectory)

            defaultImageListAdapter.directory = message.getNewDirectory()
            message.getNewDirectory()?.let {
                mImageListAdapters.put(it, defaultImageListAdapter)
            }
        }

        // Update : Detail image list adapter
        val detailImageAdapter = getDetailImageAdapter(oldDirectory)
        if (detailImageAdapter != null) {
            mDetailImageListAdapters.remove(oldDirectory)
            detailImageAdapter.directory = message.getNewDirectory()
            message.getNewDirectory()?.let {
                mDetailImageListAdapters.put(it, detailImageAdapter)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: FolderModelChangeMessage) {
        mIsFolderListLoaded = false
        reloadFolderList(true)
        //        loadFolderModel();
    }

    private fun diffUpdateDefaultImageListAdapter(adapter: DefaultImageListAdapter?, fromCacheFirst: Boolean, hideRefreshControlWhenFinish: Boolean) {
        if (adapter == null) {
            Log.w(TAG, "diffUpdateDefaultImageListAdapter: adapter is null")
            return
        }

        Log.d(TAG, String.format("差量更新图片列表 dir=%s fromCacheFirst=%s", adapter.directory, fromCacheFirst))

        val dir = adapter.directory
        if (dir == null) {
            Log.e(TAG, "diffUpdateDefaultImageListAdapter: the adapter didn't have the corresponding directory.")
            return
        }

        mPresenter.diffLoadMediaFileList(
                dir,
                fromCacheFirst,
                mViewState.getSortWay(),
                mViewState.getSortOrder(),
                hideRefreshControlWhenFinish,
                MainContract.LoadImageListPurpose.RefreshDefaultList
        )
    }

    private fun diffUpdateDetailImageAdapter(adapter: DetailImageAdapter, fromCacheFirst: Boolean, hideRefreshControl: Boolean) {

        val dir = adapter.directory
        if (dir == null) {
            Log.e(TAG, "diffUpdateDetailImageAdapter: adapter didn't have corresponding the directory.")
            return
        }

        mPresenter.diffLoadMediaFileList(dir,
                fromCacheFirst,
                mViewState.getSortWay(),
                mViewState.getSortOrder(),
                hideRefreshControl,
                MainContract.LoadImageListPurpose.RefreshDetailList)
    }

    private fun transferAdapterStatus(fromAdapter: DetailImageAdapter, toAdapter: DetailImageAdapter) {
        toAdapter.directory = fromAdapter.directory
        toAdapter.firstVisibleItem = fromAdapter.firstVisibleItem
    }

    private fun initListViewHeader() {

        mModeRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.btn_default -> {
                    switchToGroupMode(GroupMode.DEFAULT)
                }
                R.id.btn_week -> {
                    switchToGroupMode(GroupMode.WEEK)
                }
                R.id.btn_month -> {
                    switchToGroupMode(GroupMode.MONTH)
                }
            }
        }
    }

    private fun switchToGroupMode(mode: GroupMode) {
        mViewState.setGroupMode(mode)

        showImageList(mCurrentFolder, true, false, true)
    }

    override fun onBackPressed() {

        // Close drawer
        if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
            mDrawerLayout.closeDrawers()
            return
        }

        // // TODO: 2017/9/2 其他图片显示模式下，也要退出选择模式
        var isSelectionMode = false
        when (mViewState.getViewMode()) {

            ViewMode.GRID_VIEW -> {
                when (mViewState.getGroupMode()) {

                    GroupMode.DEFAULT -> if (mCurrentImageAdapter != null) {
                        isSelectionMode = mCurrentImageAdapter!!.isSelectionMode
                    }
                    GroupMode.DAY -> if (mCurrentAdapter != null) {

                    }
                    GroupMode.WEEK -> {
                    }
                    GroupMode.MONTH -> {
                    }
                }

            }
            ViewMode.LIST_VIEW -> {
                if (mCurrentDetailImageAdapter != null) {
                    isSelectionMode = mCurrentDetailImageAdapter!!.isSelectionMode
                }
            }
            ViewMode.UNKNOWN -> {
            }
        }

        if (isSelectionMode) {
            // 退出选择模式
            when (mViewState.getViewMode()) {

                ViewMode.GRID_VIEW -> when (mViewState.getGroupMode()) {

                    GroupMode.DEFAULT -> {
                        mCurrentImageAdapter!!.leaveSelectionMode()
                    }
                    GroupMode.DAY -> {
                    }
                    GroupMode.WEEK -> {
                    }
                    GroupMode.MONTH -> {
                    }
                }
                ViewMode.LIST_VIEW -> {
                    if (mCurrentDetailImageAdapter != null) {
                        mCurrentDetailImageAdapter!!.leaveSelectionMode()
                    }
                }
                ViewMode.UNKNOWN -> {
                }
            }
        } else {
            finish()
            //backToLauncher(this);
        }
    }

    fun backToLauncher(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        //        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME)
        context.startActivity(intent)
        finish()
    }

    private fun showFloatingWindow() {
        mStartFloatingIntent = Intent(this@MainActivity, FloatWindowService::class.java)
        startService(mStartFloatingIntent)
    }

    private fun initToolBar() {
        mToolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(mToolbar)

        mToolbar!!.setNavigationOnClickListener { v -> finish() }

        //        Preference<Boolean> pref = UserDataService.getInstance()
        //                .getPreferences()
        //                .getBoolean(UserDataService.PREF_KEY_SHOW_HIDDEN_FOLDER, false);
        //
        //        pref.asObservable()
        //                .observeOn(AndroidSchedulers.mainThread())
        //                .subscribe(aBoolean -> {
        //                    mToolbar.getMenu().findItem(R.id.app_bar_show_hidden_folder).setChecked(aBoolean);
        //                });
        //
        //        boolean showHiddenFile = pref.get();
        //        mToolbar.getMenu()
        //                .findItem(R.id.app_bar_show_hidden_folder)
        //                .setChecked(showHiddenFile).setCheckable(true);

        mToolbar!!.setOnMenuItemClickListener({ this.onMainMenuClick(it) })
    }

    private fun onMainMenuClick(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.app_bar_recent -> {
                showRecentHistoryMenu()
            }
            R.id.app_bar_search -> ToastUtils.toastShort(this@MainActivity, R.string.unimplemented)
            R.id.app_bar_setting -> {
                this@MainActivity.startActivity(Intent(this@MainActivity, SettingActivity::class.java))
            }
            R.id.app_bar_view_mode -> showViewModeMenu()
            R.id.app_bar_show_hidden_folder -> {

                menuItem.isChecked = !menuItem.isChecked

                // Save option
                val showHiddenFolderPref = UserModule.showHiddenFilePreference

                showHiddenFolderPref.set(menuItem.isChecked)

                reloadFolderList(false)
            }
            R.id.app_bar_view_hidden_folders -> {
                val fragment = ExcludeFolderDialogFragment.newInstance(ArrayList())
                fragment.show(supportFragmentManager, "Hidden File List Dialog")
            }
        }

        return true
    }

    private fun showViewModeMenu() {
        val popupMenu = PopupMenu(this, mToolbar!!, Gravity.RIGHT)
        popupMenu.inflate(R.menu.view_mode_menu)


        // 初始化菜单
        val menu = popupMenu.menu
        when (mViewState.getViewMode()) {
            ViewMode.UNKNOWN -> {

                setToolbarViewModeIcon(R.drawable.ic_grid_on_black_24px)

                val prefViewMode = UserModule.viewMode
                prefViewMode.set(ViewMode.GRID_VIEW)

                checkMenuItem(menu, R.id.item_grid_view, true)
            }

            ViewMode.GRID_VIEW -> checkMenuItem(menu, R.id.item_grid_view, true)
            ViewMode.LIST_VIEW -> checkMenuItem(menu, R.id.item_list_view, true)
        }

        when (mViewState.getSortWay()) {

            SortWay.NAME -> checkMenuItem(menu, R.id.sort_by_name, true)
            SortWay.SIZE -> {
            }
            SortWay.DATE -> checkMenuItem(menu, R.id.sort_by_date, true)
            SortWay.UNKNOWN -> {
            }
        }

        when (mViewState.getSortOrder()) {

            SortOrder.DESC -> checkMenuItem(menu, R.id.order_by_desc, true)
            SortOrder.ASC -> checkMenuItem(menu, R.id.order_by_asc, true)
            SortOrder.UNKNOWN -> {
            }
        }

        popupMenu.setOnMenuItemClickListener({ this.onClickViewModeMenuItem(it) })
        popupMenu.show()
    }

    private fun onClickViewModeMenuItem(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.item_grid_view -> switchToViewMode(ViewMode.GRID_VIEW)
            R.id.item_list_view -> switchToViewMode(ViewMode.LIST_VIEW)
            R.id.sort_by_name -> switchToSortWay(SortWay.NAME)
            R.id.sort_by_date -> switchToSortWay(SortWay.DATE)
            R.id.order_by_asc -> switchToSortOrder(SortOrder.ASC)
            R.id.order_by_desc -> switchToSortOrder(SortOrder.DESC)
        }
        return false
    }

    private fun switchToSortWay(way: SortWay) {
        if (moveToListState((mViewState.clone() as ListViewState).setSortWay(way))) {
            UserModule.sortWay.set(way)
        }
    }

    private fun switchToSortOrder(order: SortOrder) {
        if (moveToListState((mViewState.clone() as ListViewState).setSortOrder(order))) {
            UserModule.sortOrder.set(order)
        }
    }

    private fun switchToViewMode(viewMode: ViewMode) {

        if (moveToListState((mViewState.clone() as ListViewState).setViewMode(viewMode))) {

            when (viewMode) {

                ViewMode.GRID_VIEW -> this@MainActivity.setToolbarViewModeIcon(R.drawable.ic_grid_on_black_24px)
                ViewMode.LIST_VIEW -> this@MainActivity.setToolbarViewModeIcon(R.drawable.ic_list_black_24px)
                ViewMode.UNKNOWN -> {
                }
            }

            val prefViewMode = UserModule.viewMode
            prefViewMode.set(viewMode)
        }
    }

    private fun moveToListState(newState: ListViewState): Boolean {
        if (mMovingViewState) {
            ToastUtils.toastShort(this, R.string.wait_for_a_moment_previous_operation_is_going)
            return false
        }

        mMovingViewState = true
        Log.d(TAG, "moving to new state from : $mViewState")

        // 显示模式变化需要使用新的 Adapter
        val listModeChanged = newState.getViewMode() != mViewState.getViewMode()

        // 排序方式变化只需要对 Adapter 数据进行排序
        val sortOrderChanged = newState.getSortOrder() != mViewState.getSortOrder()
        val sortWayChanged = newState.getSortWay() != mViewState.getSortWay()

        var done = false
        if (listModeChanged) {
            if (mCurrentFolder != null) {
                showStatedImageList(mCurrentFolder, newState, false, false, true, true)
                mViewState = newState
                done = true
                Log.d(TAG, "Image list view is already switched to new view mode : $newState")

            } else {
                Log.w(TAG, "showViewModeMenu: mCurrentFolder is null, cannot switch to new list mode.")
            }
        } else {
            if (sortOrderChanged || sortWayChanged) {
                showStatedImageList(mCurrentFolder, newState, false, false, true, true)
                mViewState = newState
                done = true
                Log.d(TAG, "Image list view is already switched to new view sort way/order : $newState")

            }
        }

        mMovingViewState = false
        return done
    }

    private fun setToolbarViewModeIcon(resId: Int) {
        mToolbar!!.menu.findItem(R.id.app_bar_view_mode).setIcon(resId)
    }

    private fun showRecentHistoryMenu() {
        val popupMenu = PopupMenu(this, mToolbar!!, Gravity.RIGHT)
        val menu = popupMenu.menu
        for (i in mRecentHistory.indices) {
            val dir = mRecentHistory[i]
            val menuItem = menu.add(dir.name)
            menuItem.setIcon(R.drawable.ic_move_to_folder)
            menuItem.setOnMenuItemClickListener { item ->

                showImageList(dir, true, false, true)
                true
            }
        }

        popupMenu.show()
    }

    private fun initDrawer() {


        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false)

            mDrawerToggle = object : ActionBarDrawerToggle(this, mDrawerLayout,
                    mToolbar, R.string.drawer_open, R.string.drawer_close) {


                /** Called when a drawer has settled in a completely closed state.  */
                override fun onDrawerClosed(view: View) {
                    super.onDrawerClosed(view)
                    //getActionBar().setTitle(mTitle);
                    supportInvalidateOptionsMenu()
                    //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                /** Called when a drawer has settled in a completely open state.  */
                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    //getActionBar().setTitle(mDrawerTitle);
                    supportInvalidateOptionsMenu()
                    //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            }
            mDrawerToggle?.apply { isDrawerIndicatorEnabled = true }

            mDrawerLayout.addDrawerListener(mDrawerToggle!!)
            mDrawerToggle!!.syncState()
        }

        // Folder list
        mFolderList.setEmptyView(mFolderListEmptyView)
        mFolderListRefresh.setOnRefreshListener { reloadFolderList(false) }

        // 文件夹列表工具栏
        mDialpadSwitchBadge = BadgeFactory
                .createDot(this)
                .setBadgeGravity(Gravity.TOP or Gravity.RIGHT)
                .setWidthAndHeight(8, 8)
                .setBadgeBackground(resources.getColor(R.color.colorAccent))
                .setBadgeCount(-1)
                .setSpace(8, 16)
                .bind(mKeypadSwitchBadge)

        mDialpadSwitchBadge!!.visibility = View.GONE


        // DialPad
        mKeypad.keypadInputObservable
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe { input ->
                    if (mEnableT9Filter) {
                        filterFolderList(input.toString())
                    }
                }

    }

    private fun filterFolderList(t9NumberInput: String) {
        runOnUiThread {
            if (t9NumberInput.isEmpty()) {
                mDialpadSwitchBadge?.apply {
                    setBadgeCount(0)
                    visibility = View.INVISIBLE
                }
            } else {
                mDialpadSwitchBadge?.apply {
                    setBadgeCount(t9NumberInput.length)
                    visibility = View.VISIBLE
                }
            }
        }

        ImageModule
                .loadFolderModel(true, t9NumberInput)
                .map({ FolderListAdapterUtils.folderModelToSectionedFolderListAdapter(it) })
                .compose(RxUtils.applySchedulers())
                .subscribe({ newAdapter ->

                    if (mFolderList.adapter == null) {
                        mFolderList.adapter = newAdapter
                    } else {
                        val adapter = mFolderList.adapter
                        if (adapter is SectionedFolderListAdapter) {

                            newAdapter.itemCount.takeIf { it <= 0 }
                                    .run { w("Filtering with string '$t9NumberInput' results empty adapter") }

                            adapter.diffUpdate(newAdapter)
                            mCurrentT9Number = t9NumberInput
                        } else {
                            Log.w(TAG, "initDrawer:  没处理 dialpad 输入变更更新")
                        }
                    }

                }, { RxUtils.unhandledThrowable(it) })
    }

    // ---------------------------------------------------------------------------------------------
    // View onClick
    // ---------------------------------------------------------------------------------------------


    @OnClick(R.id.keyboard_switch_layout)
    fun onButtonClickKeypadLayout(view: View) {
        //        switchKeyboard();
    }

    @OnLongClick(R.id.keyboard_switch_layout)
    fun onLongClickFolderListToolKeypadLayout(view: View): Boolean {
        //        switchKeyboard();
        return true
    }

    @OnClick(R.id.keyboard_switch)
    fun onClickButtonDialpadSwitch(view: View) {
        switchKeyboard()
    }

    @OnLongClick(R.id.keyboard_switch)
    fun onLongClickButtonDialpadSwitch(view: View): Boolean {
        if (!TextUtils.isEmpty(mCurrentT9Number)) {
            //            filterFolderList("");
            mKeypad.clearT9Input()
            mKeypadContainer.visibility = View.INVISIBLE
        }
        return true
    }

    @OnClick(R.id.conflict_filter_switch)
    fun onClickButtonFolderToolFilterMode() {
        mFolderAdapter?.let {
            if (it.isFiltering()) {
                it.leaveFilterMode()
            } else {
                it.filter(
                        Predicate<SectionedFolderListAdapter.Item> { item ->
                            item.mItemSubType == SectionedFolderListAdapter.ItemSubType.CONFLICT_COUNT
                                    || item.mItemSubType == SectionedFolderListAdapter.ItemSubType.SOURCE_DIR
                        }
                )
            }
        }
    }

    @OnClick(R.id.btn_paste)
    fun onClickToolbarPaste(view: View) {

    }

    @OnClick(R.id.btn_move)
    fun onClickToolbarMove(view: View) {
        showMoveFileDialog()
    }

    @OnClick(R.id.btn_select_all)
    fun onClickToolbarSelectAll(view: View) {
        selectAdapterAllItems()
    }

    private fun selectAdapterAllItems() {
        when (mViewState.getViewMode()) {

            ViewMode.GRID_VIEW -> {
                if (mCurrentImageAdapter != null) {
                    mCurrentImageAdapter!!.selectAll()
                }
            }
            ViewMode.LIST_VIEW -> {
                if (mCurrentDetailImageAdapter != null) {
                    mCurrentDetailImageAdapter!!.selectAll()
                }
            }
            ViewMode.UNKNOWN -> {
            }
        }
    }

    @OnClick(R.id.btn_copy)
    fun onClickToolbarCopy(view: View) {

    }

    @OnClick(R.id.btn_delete)
    fun onClickToolbarDelete(view: View) {
        showRemoveFileDialog()
    }

    private fun switchKeyboard() {
        val isVisible = mKeypadContainer.visibility == View.VISIBLE
        val v = if (isVisible) View.INVISIBLE else View.VISIBLE
        if (v == View.VISIBLE && !mEnableT9Filter) {
            mEnableT9Filter = true
        }

        mKeypadContainer.visibility = v

        if (isVisible) {
            mKeypadSwitch.setImageResource(R.drawable.keyboard_show_selector)
        } else {
            mKeypadSwitch.setImageResource(R.drawable.keyboard_hide_selector)
        }
    }

    private fun showExpandableFolderList(model: FolderModel) {

        mExpandableFolderAdapter = FolderListAdapterUtils.folderModelToExpandableFolderAdapter(model)

        mExpandableFolderAdapter!!.setOnItemClickListener { baseQuickAdapter, view, i -> Log.d(TAG, "onItemClick() called with: baseQuickAdapter = [$baseQuickAdapter], view = [$view], i = [$i]") }
        mExpandableFolderAdapter!!.setOnInteractionListener { item -> showImageList(item.file, false, false, true) }

        val manager = GridLayoutManager(this, 1)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (mExpandableFolderAdapter!!.getItemViewType(position) == ExpandableFolderAdapter.TYPE_LEVEL_1) 1 else manager.spanCount
            }
        }

        mFolderList.adapter = mExpandableFolderAdapter
        // important! setLayoutManager should be called after setAdapter
        mFolderList.layoutManager = manager
        mExpandableFolderAdapter!!.expandAll()
    }


    // ---------------------------------------------------------------------------------------------
    // 文件夹列表
    // ---------------------------------------------------------------------------------------------

    private fun showFolderList(folderModel: FolderModel) {

        mIsFolderListLoaded = true

        val onItemClickListener = object : SectionedFolderListAdapter.OnItemClickListener {
            override fun onSectionHeaderClick(section: SectionedFolderListAdapter.Section, sectionIndex: Int, adapterPosition: Int) {
                val sectionExpanded = mFolderAdapter!!.isSectionExpanded(sectionIndex)
                if (sectionExpanded) {
                    mFolderAdapter!!.collapseSection(sectionIndex)
                } else {
                    mFolderAdapter!!.expandSection(sectionIndex)
                }
            }

            override fun onSectionHeaderOptionButtonClick(v: View, section: SectionedFolderListAdapter.Section, sectionIndex: Int) {
                Log.d(TAG, "onSectionHeaderOptionButtonClick() called with: section = [$section], sectionIndex = [$sectionIndex]")
                showFolderSectionHeaderOptionPopupMenu(v, section)
            }

            override fun onItemClick(sectionItem: SectionedFolderListAdapter.Section?, section: Int, item: SectionedFolderListAdapter.Item, relativePos: Int) {
                Log.d(TAG, "onItemClick: 显示目录图片 " + item.mFile)
                mDrawerLayout.closeDrawers()
                showImageList(item.mFile, true, false, true)
            }

            override fun onItemLongClick(v: View, sectionItem: SectionedFolderListAdapter.Section?, section: Int, item: SectionedFolderListAdapter.Item, relativePos: Int) {
                showFolderItemContextPopupWindow(v, item)
            }

            override fun onItemCloseClick(v: View, section: SectionedFolderListAdapter.Section, item: SectionedFolderListAdapter.Item, sectionIndex: Int, relativePosition: Int) {

            }
        }

        // Create adapter
        val listAdapter = FolderListAdapterUtils.folderModelToSectionedFolderListAdapter(folderModel)
        listAdapter
                .apply {
                    setShowHeaderOptionButton(true)
                    setShowSourceDirBadgeWhenEmpty(false)

                    mOnItemClickListener = onItemClickListener
                }
                .also { adatepr ->
                    mCurrentFolder?.let {
                        adatepr.selectItem(mCurrentFolder)
                        adatepr.setMoveFileSourceDir(mCurrentFolder)
                    }
                }


        if (mFolderListRefresh.isRefreshing) {
            mFolderListRefresh.isRefreshing = false
        }

        // RecyclerView layout
        mFolderList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        // Set adapter
        mFolderList.adapter = listAdapter
        mFolderAdapter = listAdapter

        // TODO Initial selection status
        if (mCurrentFolder != null) {
            //            listAdapter.selectItem(mCurrentFolder);
        }

        // List item click event
        val itemTouchListener = RecyclerItemTouchListener(this,
                mFolderList,
                { view, position ->
                    this.onClickFolderListItem(view, position)
                }
                ,
                { view, position ->
                    //onLongClickFolderListItem(view, position);
                })
        mFolderList.addOnItemTouchListener(itemTouchListener)

        // show firstOf folder's images in activity content field.

        /*
        SectionedFolderListAdapter.Section sectionItem = ListUtils.firstOf(sections);
        if (sectionItem != null) {
            SectionedFolderListAdapter.Item item = ListUtils.firstOf(sectionItem.getItems());
            if (item != null) {
                showImageList(item.getFile());
            }
        }*/
    }

    private fun showFolderSectionHeaderOptionPopupMenu(v: View, section: SectionedFolderListAdapter.Section) {
        val popupMenu = PopupMenu(v.context, v)
        popupMenu.inflate(R.menu.folder_header_option_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.create_folder -> {
                    section.file?.let { showCreateFolderDialog(it) }
                }
                R.id.folder_detail -> ToastUtils.toastShort(this@MainActivity, R.string.unimplemented)
            }
            false
        }
        popupMenu.show()
    }

    /**
     * @param targetDir 新建文件夹所在的目标目录
     */
    private fun showCreateFolderDialog(targetDir: File) {

        MaterialDialog.Builder(this@MainActivity)
                .title(R.string.create_folder)
                .input(R.string.input_new_folder_name,
                        R.string.new_folder_prefill,
                        false
                ) { dialog, input ->
                    val isValid = input.length > 1 && input.length <= 16
                    val actionButton = dialog.getActionButton(DialogAction.POSITIVE)
                    if (actionButton != null) {
                        actionButton.isClickable = isValid
                    }
                }
                .alwaysCallInputCallback()
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
                .positiveText(R.string.create_folder)
                .onPositive { dialog, which ->
                    val inputEditText = dialog.inputEditText
                    if (inputEditText != null) {
                        val folderName = inputEditText.editableText.toString()
                        val dir = File(targetDir, folderName)
                        ImageModule
                                .createFolder(dir)
                                .compose(RxUtils.applySchedulers())
                                .subscribe({ ok ->
                                    if (ok!!) {
                                        ToastUtils.toastLong(this@MainActivity, getString(R.string.created_folder_s, folderName))

                                    }
                                }) { throwable ->
                                    Log.d(TAG, "新建文件夹失败：" + throwable.message)
                                    ToastUtils.toastLong(this@MainActivity, R.string.create_folder_failed)
                                }
                    }
                }
                .negativeText(R.string.cancel)
                .show()
    }

    private fun onLongClickFolderListItem(view: View, position: Int) {

        SectionedListItemClickDispatcher<SectionedFolderListAdapter>(mFolderAdapter)
                .dispatch(position, object : SectionedListItemDispatchListener<SectionedRecyclerViewAdapter<SectionedViewHolder>> {
                    override fun onHeader(adapter: SectionedRecyclerViewAdapter<SectionedViewHolder>, coord: ItemCoord) {

                    }

                    override fun onFooter(adapter: SectionedRecyclerViewAdapter<SectionedViewHolder>, coord: ItemCoord) {

                    }

                    override fun onItem(adapter: SectionedRecyclerViewAdapter<SectionedViewHolder>, coord: ItemCoord) {
                        //                        showFolderItemContextPopupMenu(view, mFolderAdapter.getItem(coord));
                        showFolderItemContextPopupWindow(view, mFolderAdapter!!.getItem(coord))
                        //showFolderMoveToHereDialog(item.getFile());
                    }
                })
    }

    private fun showFolderItemContextPopupWindow(v: View, item: SectionedFolderListAdapter.Item?) {


        if (mFolderItemContextMenu != null) {
            Log.w(TAG, "showFolderItemContextPopupWindow: already shown the popup menu, " + item!!.mName)
            return
        }

        Logger.d("show context menu : " + item!!.mName)
        if (item.mFile == null) {
            Log.w(TAG, "show context menu for ${item.mName} with null file object.")
            return
        }

        val selectedDir = item.mFile

        // Window properties
        val backgroundDrawable = ColorDrawable(resources.getColor(R.color.gray_98))
        val contentWidth = resources.getDimensionPixelSize(R.dimen.pop_menu_width)
        val horizontalOffset = (-resources.getDimension(R.dimen.pop_menu_margin)).toInt()

        // Create menu items
        val items = LinkedList<PopupUtils.PopupMenuItem>()
        // Select all
        //        items.add(
        //                new PopupUtils.PopupMenuItem()
        //                        .setIcon(getResources().getDrawable(R.drawable.ic_filter_all_black_24px))
        //                        .setName(getResources().getString(R.string.select_all))
        //                        .setAction(() -> {
        //                            selectAdapterAllItems();
        //                            return true;
        //                        })
        //        );

        // Rename
        items.add(
                PopupUtils.PopupMenuItem()
                        .setIcon(resources.getDrawable(R.drawable.ic_mode_edit_black_24px))
                        .setName(getString(R.string.rename))
                        .setAction {
                            showFolderRenameDialog(item, selectedDir!!)
                            true
                        }
        )

        // Move
        items.add(
                PopupUtils.PopupMenuItem()
                        .setIcon(resources.getDrawable(R.drawable.ic_move_to_folder))
                        .setName(getString(R.string.move))
                        .setAction {
                            ToastUtils.toastShort(this@MainActivity, R.string.unimplemented)
                            true
                        }
        )

        // Delete
        items.add(
                PopupUtils.PopupMenuItem()
                        .setIcon(resources.getDrawable(R.drawable.ic_delete_black_24px))
                        .setName(getString(R.string.delete))
                        .setAction {
                            onClickMenuItemDeleteDirectory(item.mFile!!)
                            true
                        }
        )

        // Hide
        items.add(
                PopupUtils.PopupMenuItem()
                        .setIcon(resources.getDrawable(R.drawable.ic_remove_red_eye_black_24px))
                        .setName(getString(R.string.exclude_folder))
                        .setAction {
                            hideFolder(selectedDir)
                            true
                        }
        )

        // 根据是否有正在选中的文件显示“移动至这里”菜单项
        val gridViewModeAction = Action0 {
            val inSelectionModeAdapter = Stream.of(mImageListAdapters)
                    .filter { value -> value.value.isSelectionMode }
                    .toList()
            if (!inSelectionModeAdapter.isEmpty()) {

                items.add(
                        PopupUtils.PopupMenuItem()
                                .setIcon(null)
                                .setName(getString(R.string.move_selected_images_to_here))
                                .setAction {
                                    showFolderMoveToHereDialog(selectedDir!!)
                                    true
                                }
                )
            }
        }
        val listViewModeAction = Action0 {
            val inSelectionModeAdapter = Stream.of(mDetailImageListAdapters)
                    .filter { value -> value.value.isSelectionMode }
                    .toList()
            if (!inSelectionModeAdapter.isEmpty()) {

                items.add(
                        PopupUtils.PopupMenuItem()
                                .setIcon(null)
                                .setName(getString(R.string.move_selected_images_to_here))
                                .setAction {
                                    showFolderMoveToHereDialog(selectedDir!!)
                                    true
                                }
                )
            }
        }
        dispatchViewMode(gridViewModeAction, listViewModeAction)

        mFolderItemContextMenu = PopupUtils.build(this, items, v, backgroundDrawable, contentWidth, horizontalOffset, false)
        mFolderItemContextMenu!!.setOnDismissListener { mFolderItemContextMenu = null }
        mFolderItemContextMenu!!.show()
    }

    private fun onClickMenuItemDeleteDirectory(directory: File) {
        mPresenter.removeDirectory(directory, false)
    }

    private fun showForceDeleteFolderDialog(directory: File, length: Int) {
        val build = MaterialDialog.Builder(this@MainActivity)
                .title(R.string.directory_is_not_empty)
                .content(R.string.directory_s_is_not_empty__continue_will_delete_all_d_files_in_the_directory, directory, length)
                .positiveText(R.string.delete_all_files)
                .onPositive { dialog, which -> mPresenter.removeDirectory(directory, true) }
                .negativeText(android.R.string.cancel)
                .build()
        build.show()
    }

    private fun dispatchViewMode(gridAction: Action0?, listAction: Action0?) {
        when (mViewState.getViewMode()) {

            ViewMode.GRID_VIEW -> {
                gridAction?.accept()
            }
            ViewMode.LIST_VIEW -> listAction?.accept()
            ViewMode.UNKNOWN -> {
            }
        }
    }

    private fun showFolderItemContextPopupMenu(v: View, item: SectionedFolderListAdapter.Item) {

        val selectedDir = item.mFile

        val popupMenu = PopupMenu(this, v, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0)
        var menuOrder = 0
        var menuCategory = 0

        val menu = popupMenu.menu
        menu.add(menuCategory, R.id.menu_item_rename,
                menuOrder, getString(R.string.rename_folder_s, selectedDir?.name))

        menuOrder++
        menu.add(menuCategory, R.id.menu_item_move,
                menuOrder, getString(R.string.move_folder_s, item.mFile?.name))

        menuOrder++
        menu.add(menuCategory, R.id.menu_item_delete,
                menuOrder, getString(R.string.remove_folder_s, item.mFile?.name))


        menuCategory++
        menuOrder++
        menu.add(menuCategory, R.id.menu_item_hide,
                menuOrder, getString(R.string.exclude_folder))

        when (mViewState.getViewMode()) {

            ViewMode.GRID_VIEW -> {

                val inSelectionModeAdapter = Stream.of(mImageListAdapters)
                        .filter { value -> value.value.isSelectionMode }
                        .toList()
                if (!inSelectionModeAdapter.isEmpty()) {
                    menuOrder++
                    menu.add(menuCategory, R.id.menu_item_move_file_to_here,
                            menuOrder, getString(R.string.move_selected_images_to_here))
                }
            }
            ViewMode.LIST_VIEW -> {

                val inSelectionModeAdapter = Stream.of(mDetailImageListAdapters)
                        .filter { value -> value.value.isSelectionMode }
                        .toList()
                if (!inSelectionModeAdapter.isEmpty()) {
                    menuOrder++
                    menu.add(menuCategory, R.id.menu_item_move_file_to_here,
                            menuOrder, getString(R.string.move_selected_images_to_here))
                }

            }
            ViewMode.UNKNOWN -> {
            }
        }

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_item_delete -> {

                }
                R.id.menu_item_hide -> {
                    hideFolder(selectedDir)
                }
                R.id.menu_item_rename -> {
                    selectedDir?.let { showFolderRenameDialog(item, it) }
                }
                R.id.menu_item_move -> {

                }
                R.id.menu_item_move_file_to_here -> {
                    selectedDir?.let { showFolderMoveToHereDialog(it) }
                }
            }
            false
        }

        popupMenu.show()
    }

    private fun hideFolder(selectedDir: File?) {
        if (selectedDir == null) {
            Log.w(TAG, "hideFolder: hide null folder.")
            return
        }

        UserModule
                .addExcludeFolder(selectedDir)
                .subscribe({ ok ->
                    if (ok!!) {
                        ImageModule
                                .hiddenFolder(selectedDir)
                                .subscribe { removed ->
                                    if (removed!!) {
                                        mFolderAdapter!!.removeFolderItem(selectedDir)
                                    } else {
                                        ToastUtils.toastLong(this@MainActivity, R.string.operation_failed)
                                    }
                                }
                    }
                }, { RxUtils.unhandledThrowable(it) })
    }

    private fun showFolderItemContextMenuDialog(v: View, item: SectionedFolderListAdapter.Item) {


        val menuItems = LinkedList<String>()
        val selectedDir = item.mFile

        menuItems.add(getString(R.string.rename_folder_s, selectedDir?.name))
        menuItems.add(getString(R.string.move_folder_s, item.mFile?.name))
        menuItems.add(getString(R.string.remove_folder_s, item.mFile?.name))

        menuItems.add(getString(R.string.exclude_folder))

        // TODO adapter
        when (mViewState.getViewMode()) {

            ViewMode.GRID_VIEW -> {

                val inSelectionModeAdapter = Stream.of(mImageListAdapters)
                        .filter { value -> value.value.isSelectionMode }
                        .toList()
                if (!inSelectionModeAdapter.isEmpty()) {
                    menuItems.add(getString(R.string.move_selected_images_to_here))
                }
            }
            ViewMode.LIST_VIEW -> {

                val inSelectionModeAdapter = Stream.of(mDetailImageListAdapters)
                        .filter { value -> value.value.isSelectionMode }
                        .toList()
                if (!inSelectionModeAdapter.isEmpty()) {
                    menuItems.add(getString(R.string.move_selected_images_to_here))
                }

            }
            ViewMode.UNKNOWN -> {
            }
        }


        MaterialDialog.Builder(this)
                .title(R.string.folder_operations)
                .items(menuItems)
                .itemsCallback { dialog, itemView, position, text ->
                    when (position) {
                        0 -> { // rename
                            selectedDir?.let { showFolderRenameDialog(item, it) }
                        }
                        1 // 移动
                        -> {
                        }
                        2 // 删除
                        -> {
                            selectedDir?.let { showDeleteFolderDialog(it) }
                        }
                        3 -> {
                            showFolderMoveToHereDialog(selectedDir!!)
                        }
                        4 -> {

                        }
                    }
                }
                .show()
    }

    private fun showDeleteFolderDialog(selectedDir: File) {

        MaterialDialog.Builder(this)
                .title(R.string.remove_folder)
                .content(getString(R.string.confirm_to_remove_folder_s, selectedDir.name))
                .positiveText(R.string.delete)
                .onPositive { dialog, which ->

                    ImageModule
                            .removeFolder(selectedDir, false)
                            .compose(RxUtils.applySchedulers())
                            .subscribe({ aBoolean ->
                                if (aBoolean!!) {
                                    // UI 更新在 EventBus 消息中处理
                                }
                            }) { throwable -> ToastUtils.toastLong(this, R.string.remove_folder_failed) }
                }
                .negativeText(R.string.cancel)
                .show()
    }

    private fun showFolderRenameDialog(item: SectionedFolderListAdapter.Item?, dir: File) {
        val builder = MaterialDialog.Builder(this)
                .title(R.string.folder_rename)
                .alwaysCallInputCallback()
                .input(getString(R.string.input_new_directory_name), item!!.mName) { dlg, input ->

                    val isValid = !StringUtils.equals(input, item.mName)
                    Log.d(TAG, " name " + input + " " + item.mName + " " + isValid)

                    val actionButton = dlg.getActionButton(DialogAction.POSITIVE)
                    actionButton?.apply {
                        isEnabled = isValid
                        isClickable = isValid
                    }
                }
                .inputRange(1, 20)
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
                .onPositive { dialog1, which ->

                    val inputEditText = dialog1.inputEditText
                    if (inputEditText != null) {
                        val editableText = inputEditText.editableText
                        editableText?.let {

                            val newDirName = editableText.toString()

                        }
                        if (editableText != null) {
                            val newDirName = editableText.toString()

                            if (StringUtils.equals(newDirName, dir.name)) {
                                ToastUtils.toastLong(this, R.string.folder_name_has_no_changes)


                            }

                            ImageModule
                                    .renameDirectory(dir, newDirName)
                                    .compose(RxUtils.applySchedulers())
                                    .subscribe({ ok ->
                                        if (ok!!) {
                                            ToastUtils.toastLong(this, getString(R.string.already_rename_folder))
                                        }
                                    }) { throwable ->

                                    }
                        }
                    }
                }.negativeText(R.string.cancel)
        val dlg = builder.build()
        val actionButton = dlg.getActionButton(DialogAction.POSITIVE)
        actionButton.isEnabled = false
        actionButton.isClickable = false

        dlg.show()
    }

    private fun onClickFolderListItem(view: View, position: Int) {
        SectionedListItemClickDispatcher<SectionedFolderListAdapter>(mFolderAdapter)
                .dispatchItemClick(position) { adapter, coord ->
                    val item = adapter.getItem(coord)
                    item?.let {
                        val selectedItem = adapter.selectedItem
                        val moveFileSourceDir = adapter.getMoveFileSourceDir()
                        if (it.mFile == moveFileSourceDir) {
                            Log.w(TAG, "onClickFolderListItem: 点击的项是移动操作的源目录，不显示冲突对话框")
                            return@dispatchItemClick
                        }

                        if (!ListUtils.isEmpty(it.conflictFiles)) {

                            it.mFile?.let {

                                val fragment = ConflictResolverDialogFragment
                                        .newInstance(it.absolutePath,
                                                ArrayList(
                                                        Stream.of(item.conflictFiles)
                                                                .map { file -> File(moveFileSourceDir, file.name) }
                                                                .map { it.absolutePath }
                                                                .toList())
                                        )
                                fragment.show(supportFragmentManager, "Conflict Resolver Dialog")
                            }
                        } else {
                            Log.d(TAG, "onItemClick: 显示 " + it.mFile)
                            adapter.selectItem(item.mFile)
                            showImageList(item.mFile, false, true, true)
                            //                    mDrawerLayout.closeDrawers();
                        }
                    }
                }
    }

    private fun showFolderMoveToHereDialog(dir: File) {

        var items: List<FlatFolderListAdapter.Item>? = null
        when (mViewState.getViewMode()) {

            ViewMode.GRID_VIEW -> {
                val selectionModeAdapters = Stream.of(mImageListAdapters)
                        .filter { value -> value.value.isSelectionMode }
                        .toList()
                if (!selectionModeAdapters.isEmpty()) {

                    /*
                    {
                        List<String> menuTitles = Stream.of(selectionModeAdapters)
                                .map(entry -> {
                                    DefaultImageListAdapter value = entry.getValue();
                                    return this.getString(R.string.main_drawer_menu_item_move__item__to__folder,
                                            value.getDirectory().getName(), value.getSelectedItemCount());
                                })
                                .toList();

                        new MaterialDialog.Builder(this)
                                .title(getString(R.string.folder_s_operations, dir.getName()))
                                .items(menuTitles)
                                .itemsCallback((dialog, itemView, position, text) -> {
                                    Map.Entry<String, DefaultImageListAdapter> entry = selectionModeAdapters.get(position);
                                    File directory = entry.getValue().getDirectory();
                                })
                                .show();
                    }*/


                    items = Stream.of(selectionModeAdapters)
                            .map { entry ->
                                FlatFolderListAdapter.Item().apply {

                                    mDirectory = entry.value.directory
                                    mCount = (entry.value.selectedItemCount)
                                    mThumbList = entry.value.getSelectedItemUntil(MOVE_FILE_DIALOG_THUMBNAIL_COUNT).map { it.file }.toList()
                                }
                            }.toList()
                }
            }
            ViewMode.LIST_VIEW -> {
                val inSelectionModeAdapters = Stream.of(mDetailImageListAdapters)
                        .filter { value -> value.value.isSelectionMode }
                        .toList()
                if (!inSelectionModeAdapters.isEmpty()) {
                    items = Stream.of(inSelectionModeAdapters)
                            .map { entry ->
                                FlatFolderListAdapter.Item()
                                        .setDirectory(entry.value.directory)
                                        .setCount(entry.value.selectedItemCount)
                                        .setThumbList(
                                                Stream.of(entry.value
                                                        .getSelectedItemUntil(MOVE_FILE_DIALOG_THUMBNAIL_COUNT))
                                                        .map({ it.file })
                                                        .toList()
                                        )
                            }.toList()

                }
            }
            ViewMode.UNKNOWN -> {
            }
        }

        if (items != null) {
            val selectedFolderListAdapter = FlatFolderListAdapter(items)
            MaterialDialog.Builder(this)
                    .title(getString(R.string.selected_files_operation))
                    // second parameter is an optional layout manager. Must be a LinearLayoutManager or GridLayoutManager.
                    .adapter(selectedFolderListAdapter, LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false))
                    .positiveText(R.string.move_to_here)
                    .negativeText(R.string.copy_to_here)
                    .onPositive { dialog, which ->
                        if (which == DialogAction.POSITIVE) {
                            val selectedItem = selectedFolderListAdapter.selectedItem
                            Stream.of(selectedItem)
                                    .map({ it.mDirectory })
                                    .forEach { f ->
                                        // TODO show progress
                                        moveAdapterSelectedFilesToDir(dir)
                                    }
                        }
                    }
                    .onNegative { dialog, which -> ToastUtils.toastShort(this, R.string.unimplemented) }
                    .show()
        }
    }


    // ---------------------------------------------------------------------------------------------
    // 文件操作
    // ---------------------------------------------------------------------------------------------

    private fun moveAdapterSelectedFilesToDir(destDir: File) {
        ImageModule
                .moveFilesToDirectory(destDir,
                        currentSelectedFilePathList,
                        true,
                        false)
                .compose(RxUtils.applySchedulers())
                .subscribe({ result ->
                    when (mViewState.getViewMode()) {

                        ViewMode.GRID_VIEW -> {
                            mFolderAdapter!!.updateSelectedCount(
                                    mCurrentImageAdapter!!.directory,
                                    mCurrentImageAdapter!!.selectedItemCount)
                        }
                        ViewMode.LIST_VIEW -> {
                            mFolderAdapter!!.updateSelectedCount(
                                    mCurrentDetailImageAdapter!!.directory,
                                    mCurrentDetailImageAdapter!!.selectedItemCount)
                        }
                        ViewMode.UNKNOWN -> {
                        }
                    }

                    ToastUtils.toastLong(this, getString(R.string.already_moved_d_files, result.successFiles.size))
                }) { throwable -> ToastUtils.toastLong(this, R.string.move_files_failed) }
    }


    // ---------------------------------------------------------------------------------------------
    // 文件夹列表
    // ---------------------------------------------------------------------------------------------


    private fun showFolderModel(model: FolderModel) {

        val sectionItems = LinkedList<SectionFolderListAdapter.SectionItem>()

        val containerFolders = model.containerFolders
        var i = 0
        val s = containerFolders?.size ?: 0
        while (i < s) {
            folderInfoToItem(containerFolders?.get(i))
                    ?.let {
                        sectionItems.add(it)
                    }
            i++
        }

        val listAdapter = SectionFolderListAdapter(sectionItems)

        listAdapter.setOnItemClickListener { sectionItem, item ->
            mDrawerLayout.closeDrawers()
            showImageList(item.file, true, false, true)
        }
        mFolderList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mFolderList.adapter = listAdapter
        mFolderList.addOnItemTouchListener(
                RecyclerItemTouchListener(
                        this,
                        mFolderList,
                        { view, position -> Log.d(TAG, "onItemClick() called with: view = [$view], position = [$position]") }, null
                )
        )

        val sectionItem = ListUtils.firstOf(sectionItems)
        if (sectionItem != null) {
            val item = ListUtils.firstOf(sectionItem.items)
            if (item != null) {
                showImageList(item.file, true, false, true)
            }
        }
        //        // show firstOf folder's images in activity content field.
        //        if (sectionItems.size() > 0) {
        //
        //            SectionFolderListAdapter.Section item = sectionItems.get(0);
        //            List<SectionFolderListAdapter.Item> items = item.getItems();
        //            if (items != null && !items.isEmpty()) {
        //                showImageList(items.get(0).getFile());
        //            }
        //        }
    }

    private fun folderInfoToItem(containerFolder: FolderModel.ContainerFolder?): SectionFolderListAdapter.SectionItem? {
        return containerFolder?.let {

            val sectionItem = SectionFolderListAdapter.SectionItem()
            sectionItem.apply {
                name = it.name
                file = it.file
                items = containerFolder.subFolders
                        ?.map { item ->
                            SectionFolderListAdapter.Item()
                                    .setFile(item.file)
                                    .setName(item.name)
                                    .setCount(item.count)
                                    .setThumbList(item.thumbnailList)
                        }
                        ?.toList()

            }
        }
    }

    @Deprecated("")
    private fun showFolders(imageFolders: List<ImageFolder>) {

        val items = Stream.of(imageFolders)
                .map { FolderListAdapterUtils.imageFolderToFolderListAdapterItem(it) }
                .toList()
        showFolderItems(items)
    }

    @Deprecated("")
    @MainThread
    private fun showFolderItems(items: List<FolderListAdapter.Item>) {

        mFolderListAdapter = FolderListAdapter(items)
        mFolderListAdapter!!.setOnItemEventListener { item ->

            mDrawerLayout.closeDrawers()

            showImageList(item.directory, true, false, true)

        }
        mFolderList.addItemDecoration(SectionDecoration(this, object : SectionDecoration.DecorationCallback {
            override fun getGroupId(position: Int): Long {
                return ImageModule.getSectionForPosition(position).toLong()
                //                return mFolderListAdapter.getItemCount();
            }

            override fun getGroupFirstLine(position: Int): String {
                return ImageModule.getSectionFileName(position) ?: ""
            }
        }))
        mFolderList.adapter = mFolderListAdapter

        //        // show firstOf folder's images in activity content field.
        //        if (items != null && items.size() > 0) {
        //
        //            FolderListAdapter.Item item = items.get(0);
        //            showImageList(item.getDirectory());
        //        }
    }


    // ---------------------------------------------------------------------------------------------
    // Actionbar
    // ---------------------------------------------------------------------------------------------

    fun updateActionBarTitle(name: String) {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = name
        }
    }


    // ---------------------------------------------------------------------------------------------
    // Image list
    // ---------------------------------------------------------------------------------------------

    private fun reloadCurrentImageList() {
        showImageList(mCurrentFolder, false, true, true)
    }

    @MainThread
    private fun showImageList(directory: File?, selectFolderListItem: Boolean, showRefreshing: Boolean, fromCacheFirst: Boolean) {

        showStatedImageList(directory, mViewState, false, selectFolderListItem, showRefreshing, fromCacheFirst)
    }

    /**
     * @param directory
     * @param state
     * @param forceReload          true, create new adapter and use it. false, reload current adapter and do list view diff update.
     * @param selectFolderListItem
     * @param showRefreshing
     * @param fromCacheFirst
     */
    private fun showStatedImageList(directory: File?, state: ListViewState, forceReload: Boolean,
                                    selectFolderListItem: Boolean, showRefreshing: Boolean, fromCacheFirst: Boolean) {

        if (directory == null) {
            Log.e(TAG, "showStatedImageList: show empty folder")
            return
        }

        Logger.d("Show directory : $directory")

        mCurrentFolder = directory

        // Update folder list select status
        if (selectFolderListItem && mFolderAdapter != null) {
            mFolderAdapter!!.selectItem(directory)
        }

        // Action bar title
        updateActionBarTitle(directory.name)


        when (state.getViewMode()) {

            ViewMode.GRID_VIEW ->
                // Image list based on view mode
                if (mViewState.getGroupMode() == GroupMode.DEFAULT) {
                    showDefaultImageList(directory, showRefreshing, fromCacheFirst, state, forceReload)
                } else {
                    showSectionedImageList(directory, showRefreshing, mViewState.getGroupMode(), state, forceReload, fromCacheFirst)
                }
            ViewMode.LIST_VIEW -> showDetailImageList(directory, showRefreshing, fromCacheFirst, state, forceReload)
            else -> Log.w(TAG, "showStatedImageList: unknown view mode")
        }
    }

    @MainThread
    private fun showImageList(directory: File?,
                              fromViewMode: ViewMode,
                              toViewMode: ViewMode,
                              updateFolderListSelectItem: Boolean,
                              showRefreshing: Boolean,
                              fromCacheFirst: Boolean) {

        Log.d(TAG, "showImageList() called with: directory = [$directory], updateFolderListSelectItem = [$updateFolderListSelectItem], fromViewMode = [$fromViewMode], toViewMode = [$toViewMode]")


        if (directory == null) {
            Log.w(TAG, "show empty folder")
            return
        }

        //        switch (fromViewMode) {
        //
        //            case GRID_VIEW:
        //                if (toViewMode == ViewMode.GRID_VIEW) {
        //
        //                    if (mCurrentImageAdapter != null) {
        //                        if (mCurrentImageAdapter.getDirectory().equals(directory)) {
        //                            Log.w(TAG, "showImageList: 目录已经显示在 " + toViewMode + " 模式下");
        //                            return;
        //                        }
        //                    }
        //                }
        //                break;
        //            case LIST_VIEW:
        //                if (toViewMode == ViewMode.LIST_VIEW
        //                        && mCurrentDetailImageAdapter != null
        //                        && mCurrentDetailImageAdapter.getDirectory().equals(directory)) {
        //
        //                    Log.w(TAG, "showImageList: 目录已经显示在 " + toViewMode + " 模式下");
        //                    return;
        //                }
        //                break;
        //            case UNKNOWN:
        //                break;
        //        }

        mCurrentFolder = directory

        // Update folder list select status
        if (updateFolderListSelectItem && mFolderAdapter != null) {
            mFolderAdapter!!.selectItem(directory)
        }

        // Action bar title
        updateActionBarTitle(directory.name)

        when (toViewMode) {
            ViewMode.GRID_VIEW -> {
                // Image list based on view mode
                if (mViewState.getGroupMode() == GroupMode.DEFAULT) {
                    showDefaultImageList(directory, showRefreshing, fromCacheFirst, mViewState, false)
                } else {
                    Log.w(TAG, "showImageList: fromCacheFirst is not used.")
                    //showSectionedImageList(directory, mViewState.getGroupMode());
                }
            }
            ViewMode.LIST_VIEW -> {
                //                showDetailImageList(directory, showRefreshing, fromCacheFirst, state, forceReload);
            }
            else -> Log.w(TAG, "showImageList: 没有显示图片目录，未知视图模式：$toViewMode")
        }
    }

    private fun showDetailImageList(directory: File, showRefreshing: Boolean, fromCacheFirst: Boolean, state: ListViewState, forceReload: Boolean) {

        val cachedAdapter = getDetailImageAdapter(directory, state)
        if (cachedAdapter == null || forceReload) {

            Logger.d("showDetailImageList 加载并显示目录 $directory")


            mImageRefresh.isRefreshing = showRefreshing

            ImageModule
                    .loadMediaFileList(directory,
                            LoadMediaFileParam()
                                    .setFromCacheFirst(fromCacheFirst)
                                    .setLoadMediaInfo(true)
                                    .setSortWay(state.getSortWay())
                                    .setSortOrder(state.getSortOrder())
                    )
                    .map { this.imagesToDetailListItems(it) }
                    .map { items -> createDetailImageAdapter(directory, items) }
                    // cache adapter
                    .doOnNext { detailImageAdapter -> mDetailImageListAdapters[directory] = detailImageAdapter }
                    .compose(RxUtils.applySchedulers())
                    .subscribe(
                            { adapter -> showDetailImageListAdapter(adapter, showRefreshing) },
                            { RxUtils.unhandledThrowable(it) })
        } else {
            Logger.d("showDetailImageList 显示缓存中的目录 $directory")

            showDetailImageListAdapter(cachedAdapter, showRefreshing)
        }
    }

    private fun getDetailImageAdapter(directory: File?): DetailImageAdapter? {
        return mDetailImageListAdapters[directory]
    }

    private fun getDetailImageAdapter(directory: File, state: ListViewState): DetailImageAdapter? {

        val adapter = mDetailImageListAdapters[directory]
        if (adapter != null) {
            val data = adapter.data
            val items = Stream.of(data)
                    .sorted(getDetailImageListItemComparator(state))
                    .toList()
            adapter.setNewData(items)
        }
        return adapter
    }

    private fun mediaFileToDetailItem(mediaFile: MediaFile): DetailImageAdapter.Item {
        val item = DetailImageAdapter.Item()

        item.file = mediaFile.file
        item.date = mediaFile.date
        item.videoDuration = mediaFile.videoDuration
        item.fileSize = mediaFile.fileSize
        item.mediaResolution = mediaFile.mediaResolution
        return item
    }

    private fun createDetailImageAdapter(directory: File, items: List<DetailImageAdapter.Item>): DetailImageAdapter {
        val adapter = DetailImageAdapter(R.layout.item_image_detail, items)
        adapter.directory = directory

        adapter.isDetectMoves = true

        // Setup adapter
        val onInteractionListener = object : BaseSelectableAdapter.OnInteractionListener<BaseSelectableAdapter<*, *>, DetailImageAdapter.Item> {
            override fun onItemLongClick(item: DetailImageAdapter.Item, position: Int) {

                if (!item.isSelected) {
                    Logger.d("通过长按图片进入选择模式：" + item.file)
                    mImageList.setDragSelectActive(true, position)
                }
            }

            override fun onItemCheckedChanged(item: DetailImageAdapter.Item) {

            }

            override fun onItemClicked(item: DetailImageAdapter.Item, view: View, position: Int) {
                onClickImageListItem(view, position, item.file)
            }

            override fun onSelectionModeChange(baseAdapter: BaseSelectableAdapter<*, *>, isSelectionMode: Boolean) {
                val adapter = baseAdapter as DetailImageAdapter
                Log.d(TAG, "onSelectionModeChange: isSelectionMode $isSelectionMode")
                if (isSelectionMode) {

                    changeFloatingCount(FloatWindowService.MSG_INCREASE)


                    setImageToolbarVisible(true)
                    //            mFloatingToolbar.showContextMenu();
                } else {
                    changeFloatingCount(FloatWindowService.MSG_DECREASE)

                    setImageToolbarVisible(false)

                    //            mFloatingToolbar.dismissPopupMenus();
                }

                updateActionBarTitleCount(if (isSelectionMode) baseAdapter.getSelectedItemCount() else 0, adapter.directory, baseAdapter.getItemCount())
            }

            override fun onSelectedItemLongClick(view: View, position: Int, item: DetailImageAdapter.Item) {
                Log.d(TAG, "onSelectedItemLongClick() called with: view = [$view], position = [$position], item = [$item]")
            }

            override fun onSelectedCountChange(baseAdapter: BaseSelectableAdapter<*, *>, selectedCount: Int) {
                Log.d(TAG, "onSelectedCountChange() called with: baseAdapter = [$baseAdapter], selectedCount = [$selectedCount]")

                val adapter = baseAdapter as DetailImageAdapter
                updateActionBarTitleCount(selectedCount, adapter.directory, baseAdapter.getItemCount())

                // 通知选中个数变化
                detailAdapterSelectCountChangeRelay.accept(adapter)
            }
        }

        adapter.onInteractionListener = onInteractionListener


        return adapter
    }

    private fun showSectionedImageList(directory: File, showRefreshing: Boolean, groupMode: GroupMode, state: ListViewState, forceReload: Boolean, fromCacheFirst: Boolean) {

        if (groupMode == GroupMode.DEFAULT) {
            throw IllegalStateException("Group mode shouldn't be 'DEFAULT'.")
        }

        Log.d(TAG, "showSectionedImageList() called with: directory = [$directory], groupMode = [$groupMode]")

        run {
            val listAdapter = getSectionedImageListAdapter(directory, mViewState.getGroupMode())
            if (listAdapter == null || forceReload) {
                mImageRefresh.isRefreshing = showRefreshing

                ImageModule
                        .loadImageGroupList(directory, groupMode, fromCacheFirst, state.getSortWay(), state.getSortOrder())
                        .map { this.sortImageGroupByViewMode(it) }
                        .map { sectionList ->
                            val adapter = SectionedImageListAdapter(sectionList)
                            adapter.directory = directory
                            adapter.groupMode = groupMode
                            adapter
                        }
                        .compose(RxUtils.applySchedulers())
                        .subscribe(
                                { adapter ->
                                    putGroupMode(directory, groupMode, adapter)
                                    Log.d(TAG, "show newly sectioned list adapter : " + directory.name)
                                    showSectionedImageList(adapter)

                                    if (showRefreshing) {
                                        mImageRefresh.isRefreshing = false
                                    }
                                }) { throwable -> ToastUtils.toastLong(this@MainActivity, R.string.load_pictures_failed) }

                //            loadAdapterSections(directory, groupMode)
                //                    .map(SectionedImageListAdapter::new)
                //                    .subscribeOn(Schedulers.io())
                //                    .doOnNext(adapter -> {
                //                        putGroupMode(directory, groupMode, adapter);
                //                    })
                //                    .observeOn(AndroidSchedulers.mainThread())
                //                    .subscribe(this::showSectionedImageList);
            } else {
                Log.d(TAG, "show cached sectioned list adapter : " + directory.name)
                showSectionedImageList(listAdapter)

                showRefreshing
                        .takeIf { it }
                        .run { mImageRefresh.isRefreshing = false }
            }
        }
    }

    private fun sortImageGroupByViewMode(imageGroups: List<ImageGroup>): List<SectionedImageListAdapter.Section> {
        // 项排序
        var way = SortWay.DATE
        var order = SortOrder.DESC
        if (mViewState.getViewMode() == ViewMode.LIST_VIEW) {
            way = SortWay.NAME
            order = SortOrder.ASC
        }
        return imageGroupsToAdapter(imageGroups, way, order)
    }

    private fun putGroupMode(directory: File, groupMode: GroupMode, adapter: SectionedImageListAdapter) {
        when (groupMode) {

            GroupMode.DEFAULT -> {
            }
            GroupMode.DAY -> {
                mDaySectionedImageListAdapters[directory.absolutePath] = adapter
            }
            GroupMode.WEEK -> {
                mWeekSectionedImageListAdapters[directory.absolutePath] = adapter
            }
            GroupMode.MONTH -> {
                mMonthSectionedImageListAdapters[directory.absolutePath] = adapter
            }
        }
    }

    private fun imageGroupsToAdapter(imageGroups: List<ImageGroup>, sortWay: SortWay, order: SortOrder): List<SectionedImageListAdapter.Section> {

        val sections = LinkedList<SectionedImageListAdapter.Section>()
        var i = 0
        val imageGroupsSize = imageGroups.size
        while (i < imageGroupsSize) {
            val imageGroup = imageGroups[i]
            sections.add(imageGroupToAdapterSection(imageGroup, sortWay, order))
            i++
        }


        return sections
    }

    private fun imageGroupToAdapterSection(imageGroup: ImageGroup, sortWay: SortWay, sortOrder: SortOrder)
            : SectionedImageListAdapter.Section {
        val section = SectionedImageListAdapter.Section()

        section.startDate = imageGroup.startDate
        section.endDate = imageGroup.endDate

        val mediaFiles = imageGroup.mediaFiles.let {
            val items = LinkedList<SectionedImageListAdapter.Item>()
            var i = 0
            val imagesSize = it.size
            while (i < imagesSize) {
                val mediaFile = it[i]
                items.add(SectionedImageListAdapter.Item()
                        .setFile(mediaFile.file)
                        .setDate(mediaFile.date)
                )
                i++
            }

            val nameAscComparator = { o1: SectionedImageListAdapter.Item, o2: SectionedImageListAdapter.Item -> o1.file.name.compareTo(o2.file.name) }
            val nameDescComparator = { o1: SectionedImageListAdapter.Item, o2: SectionedImageListAdapter.Item -> o2.file.name.compareTo(o1.file.name) }

            val dateAscComparator = { o1: SectionedImageListAdapter.Item, o2: SectionedImageListAdapter.Item -> o1.date.compareTo(o2.date) }

            val dateDescComparator = { o1: SectionedImageListAdapter.Item, o2: SectionedImageListAdapter.Item -> o2.date.compareTo(o1.date) }


            val sortedItems: List<SectionedImageListAdapter.Item>
            if (sortWay == SortWay.NAME) {
                if (sortOrder == SortOrder.ASC) {
                    sortedItems = Stream.of(items)
                            .sorted(nameAscComparator)
                            .toList()
                } else {
                    sortedItems = Stream.of(items)
                            .sorted(nameDescComparator)
                            .toList()
                }

                section.items = sortedItems
            } else if (sortWay == SortWay.SIZE) {

                sortedItems = items

            } else if (sortWay == SortWay.DATE) {
                if (sortOrder == SortOrder.ASC) {
                    sortedItems = Stream.of(items)
                            .sorted(dateAscComparator)
                            .toList()
                } else {
                    sortedItems = Stream.of(items)
                            .sorted(dateDescComparator)
                            .toList()
                }
            } else {
                sortedItems = items
            }

            section.items = sortedItems
        }

        section.description = getSectionDescription(section.startDate, this@MainActivity.mViewState.getGroupMode())
        return section
    }

    private fun getSectionedImageListAdapter(directory: File, groupMode: GroupMode): SectionedImageListAdapter? {
        var listAdapter: SectionedImageListAdapter? = null

        when (groupMode) {

            GroupMode.DEFAULT -> {
                throw IllegalStateException("groupMode parameter shouldn't be 'DEFAULT'.")
            }
            GroupMode.DAY -> listAdapter = mDaySectionedImageListAdapters[directory.absolutePath]
            GroupMode.WEEK -> {
                listAdapter = mWeekSectionedImageListAdapters[directory.absolutePath]
            }
            GroupMode.MONTH -> {
                listAdapter = mMonthSectionedImageListAdapters[directory.absolutePath]
            }
        }
        return listAdapter
    }

    private fun loadAdapterSections(directory: File, mode: GroupMode): Observable<LinkedList<SectionedImageListAdapter.Section>> {
        return Observable.create { e ->

            val imageFiles = ImageModule.listMediaFiles(directory)
            val sections = Stream.of(imageFiles)
                    .groupBy<Int> { file ->
                        val d = file.lastModified()
                        val date = Date(d)

                        Log.d(TAG, "loadAdapterSections() called with: directory = [$directory], mode = [$mode]")

                        when (mode) {
                            GroupMode.DAY -> return@groupBy DateTimeUtils.daysBeforeToday(date)
                            GroupMode.WEEK -> return@groupBy DateTimeUtils.weeksBeforeCurrentWeek(date)
                            GroupMode.MONTH -> return@groupBy DateTimeUtils.monthsBeforeCurrentMonth(date)
                            else -> return@groupBy DateTimeUtils.daysBeforeToday(date)
                        }
                    }
                    .sorted { o1, o2 -> Integer.compare(o1.key, o2.key) }
                    .collect(
                            object : Collector<
                                    MutableMap.MutableEntry<Int, List<File>>,
                                    LinkedList<SectionedImageListAdapter.Section>,
                                    LinkedList<SectionedImageListAdapter.Section>> {
                                override fun supplier(): Supplier<LinkedList<SectionedImageListAdapter.Section>> {
                                    return Supplier { LinkedList<SectionedImageListAdapter.Section>() }
                                }

                                override fun accumulator(): BiConsumer<LinkedList<SectionedImageListAdapter.Section>, MutableMap.MutableEntry<Int, List<File>>> {
                                    return BiConsumer<LinkedList<SectionedImageListAdapter.Section>, MutableMap.MutableEntry<Int, List<File>>> { sections, entry ->
                                        val section = SectionedImageListAdapter.Section()
                                        val files = entry.value
                                        val itemList = Stream.of<File>(files)
                                                .map({ file ->
                                                    SectionedImageListAdapter.Item()
                                                            .setFile(file)
                                                            .setDate(Date(file.lastModified()))
                                                })
                                                .toList()
                                        val firstItem = ListUtils.firstOf(itemList)
                                        val lastItem = ListUtils.lastOf(itemList)

                                        section.items = itemList
                                        section.startDate = firstItem.date
                                        section.endDate = lastItem.date
                                        section.description = getSectionDescription(firstItem.date, this@MainActivity.mViewState.getGroupMode())

                                        sections.add(section)
                                    }
                                }

                                override fun finisher(): Function<LinkedList<SectionedImageListAdapter.Section>, LinkedList<SectionedImageListAdapter.Section>> {
                                    return Function<LinkedList<SectionedImageListAdapter.Section>, LinkedList<SectionedImageListAdapter.Section>> { sections -> sections }
                                }
                            })

            e.onNext(sections)
            e.onComplete()
        }
    }

    private fun getSectionDescription(date: Date, groupMode: GroupMode): String {

        when (groupMode) {

            GroupMode.DEFAULT -> return ""
            GroupMode.DAY -> return DateTimeUtils.friendlyDayDescription(resources, date)
            GroupMode.WEEK -> return DateTimeUtils.friendlyWeekDescription(resources, date)
            GroupMode.MONTH -> return DateTimeUtils.friendlyMonthDescription(resources, date)
            else -> return ""
        }
    }

    private fun showSectionedImageList(listAdapter: SectionedImageListAdapter) {
        mCurrentAdapter = listAdapter

        mGridLayout = GridLayoutManager(this, mSpanCount, GridLayoutManager.VERTICAL, false)
        mImageList.layoutManager = mGridLayout
        listAdapter.setLayoutManager(mGridLayout)
        if (mImageList.adapter == null) {
            Log.d(TAG, "setAdapter() called with: listAdapter = [$listAdapter]")
            mImageList.adapter = listAdapter
        } else {
            Log.d(TAG, "swapAdapter() called with: listAdapter = [$listAdapter]")
            mImageList.swapAdapter(listAdapter, false)
        }
        mImageList.addOnItemTouchListener(
                RecyclerItemTouchListener(
                        this,
                        mImageList,
                        RecyclerItemTouchListener.OnItemClickListener { view, position ->
                            if (listAdapter.isHeader(position) || listAdapter.isFooter(position)) {
                                return@OnItemClickListener
                            } else {
                                val relativePosition = listAdapter.getRelativePosition(position)
                                listAdapter.onClickItem(relativePosition)
                            }
                        }, null
                ))
    }

    private fun showDefaultImageList(directory: File, showRefreshing: Boolean, fromCacheFirst: Boolean, state: ListViewState, forceCreateAdapter: Boolean) {
        Log.d(TAG, "showDefaultImageList() called with: directory = [$directory], showRefreshing = [$showRefreshing], fromCacheFirst = [$fromCacheFirst]")

        val listAdapter = getDefaultImageListAdapter(directory, state)
        if (forceCreateAdapter || listAdapter == null) {
            Logger.d("showDefaultImageList 加载并显示目录 $directory")

            mImageRefresh.isRefreshing = showRefreshing

            ImageModule
                    .loadMediaFileList(directory,
                            LoadMediaFileParam()
                                    .setFromCacheFirst(fromCacheFirst)
                                    .setLoadMediaInfo(false)
                                    .setSortOrder(state.getSortOrder())
                                    .setSortWay(state.getSortWay())
                    )
                    .compose(RxUtils.applySchedulers())
                    .map { this.mediaFilesToListItems(it) }
                    .map { items -> createDefaultImageAdapter(directory, items) }
                    .doOnNext { adapter -> cacheImageAdapter(directory, adapter) }
                    .doOnError { throwable -> Toast.makeText(this, R.string.load_pictures_failed, Toast.LENGTH_SHORT).show() }
                    .subscribe({ adapter1 -> showGridImageListAdapter(adapter1, showRefreshing) }, { RxUtils.unhandledThrowable(it) })

        } else {
            Logger.d("showDefaultImageList 切换显示目录 $directory")

            showGridImageListAdapter(listAdapter, showRefreshing)
        }
    }

    private fun getDefaultImageListAdapter(directory: File?): DefaultImageListAdapter? {
        return mImageListAdapters[directory]
    }

    /**
     * Get adapter from cache, and apply item sorting.
     *
     * @param directory
     * @param state
     * @return
     */
    private fun getDefaultImageListAdapter(directory: File, state: ListViewState): DefaultImageListAdapter? {
        val adapter = mImageListAdapters[directory]
        if (adapter != null) {
            val data = adapter.data
            val comparator = getDefaultImageListItemComparator(state)

            val items = Stream.of(data)
                    .sorted(comparator)
                    .toList()

            adapter.setNewData(items)
        }
        return adapter
    }

    private fun getDetailImageListItemComparator(state: ListViewState): Comparator<DetailImageAdapter.Item> {
        when (state.getSortWay()) {

            SortWay.NAME -> {
                when (state.getSortOrder()) {
                    SortOrder.DESC -> return Comparator<DetailImageAdapter.Item> { o1, o2 -> o2.file.name.compareTo(o1.file.name) }
                    SortOrder.ASC -> {
                        return Comparator<DetailImageAdapter.Item> { o1, o2 -> o1.file.name.compareTo(o2.file.name) }
                    }
                }

                // default sort in desc order
                return Comparator<DetailImageAdapter.Item> { o1, o2 -> o2.file.name.compareTo(o1.file.name) }
            }
            SortWay.SIZE -> {
                when (state.getSortOrder()) {

                    SortOrder.DESC -> return Comparator<DetailImageAdapter.Item> { o1, o2 -> java.lang.Long.compare(o2.fileSize, o1.fileSize) }
                    SortOrder.ASC -> return Comparator<DetailImageAdapter.Item> { o1, o2 -> java.lang.Long.compare(o1.fileSize, o2.fileSize) }
                }
                return Comparator<DetailImageAdapter.Item> { o1, o2 -> java.lang.Long.compare(o2.fileSize, o1.fileSize) }
            }
            SortWay.DATE -> {
                when (state.getSortOrder()) {

                    SortOrder.DESC -> return Comparator<DetailImageAdapter.Item> { o1, o2 -> o2.date.compareTo(o1.date) }
                    SortOrder.ASC -> return Comparator<DetailImageAdapter.Item> { o1, o2 -> o1.date.compareTo(o2.date) }
                }
                return Comparator<DetailImageAdapter.Item> { o1, o2 -> o2.date.compareTo(o1.date) }
            }
        }

        // default : sort by date, sort in desc order
        return Comparator<DetailImageAdapter.Item> { o1, o2 -> o2.date.compareTo(o1.date) }
    }

    private fun getDefaultImageListItemComparator(state: ListViewState): Comparator<DefaultImageListAdapter.Item> {
        when (state.getSortWay()) {

            SortWay.NAME -> {
                when (state.getSortOrder()) {
                    SortOrder.DESC -> return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> o2.file.name.compareTo(o1.file.name) }
                    SortOrder.ASC -> {
                        return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> o1.file.name.compareTo(o2.file.name) }
                    }
                }

                // default sort in desc order
                return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> o2.file.name.compareTo(o1.file.name) }
            }
            SortWay.SIZE -> {
                when (state.getSortOrder()) {

                    SortOrder.DESC -> return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> java.lang.Long.compare(o2.fileSize, o1.fileSize) }
                    SortOrder.ASC -> return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> java.lang.Long.compare(o1.fileSize, o2.fileSize) }
                }
                return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> java.lang.Long.compare(o2.fileSize, o1.fileSize) }
            }
            SortWay.DATE -> {
                when (state.getSortOrder()) {

                    SortOrder.DESC -> return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> o2.date.compareTo(o1.date) }
                    SortOrder.ASC -> return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> o1.date.compareTo(o2.date) }
                }
                return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> o2.date.compareTo(o1.date) }
            }
        }

        // default : sort by date, sort in desc order
        return Comparator<DefaultImageListAdapter.Item> { o1, o2 -> o2.date.compareTo(o1.date) }
    }


    private fun cacheImageAdapter(directory: File, adapter: DefaultImageListAdapter) {
        mImageListAdapters[directory] = adapter
    }

    private fun createDefaultImageAdapter(directory: File, items: List<DefaultImageListAdapter.Item>): DefaultImageListAdapter {

        val adapter = DefaultImageListAdapter(R.layout.image_list_item, items)

        // 配置 Adapter
        adapter.directory = directory
        adapter.isDetectMoves = true

        // 图片列表交互
        adapter.onInteractionListener = object : BaseSelectableAdapter.OnInteractionListener<BaseSelectableAdapter<*, *>, DefaultImageListAdapter.Item> {
            override fun onItemLongClick(item: DefaultImageListAdapter.Item, position: Int) {

                if (!item.isSelected) {
                    Logger.d("通过长按图片进入选择模式：" + item.file)
                    mImageList.setDragSelectActive(true, position)
                }
            }

            override fun onItemCheckedChanged(item: DefaultImageListAdapter.Item) {

            }

            override fun onItemClicked(item: DefaultImageListAdapter.Item, view: View, position: Int) {
                onClickImageListItem(view, position, item.file)
            }

            override fun onSelectionModeChange(adapter: BaseSelectableAdapter<*, *>, isSelectionMode: Boolean) {

                val a = adapter as DefaultImageListAdapter
                Log.d(TAG, "onSelectionModeChange: isSelectionMode $isSelectionMode")
                if (isSelectionMode) {

                    changeFloatingCount(FloatWindowService.MSG_INCREASE)

                    setImageToolbarVisible(true)
                    //            mFloatingToolbar.showContextMenu();
                } else {
                    changeFloatingCount(FloatWindowService.MSG_DECREASE)

                    setImageToolbarVisible(false)
                    //            mFloatingToolbar.dismissPopupMenus();
                }

                updateActionBarTitleCount(if (isSelectionMode) adapter.getSelectedItemCount() else 0, a.directory, adapter.getItemCount())
            }

            override fun onSelectedCountChange(adapter: BaseSelectableAdapter<*, *>, selectedCount: Int) {

                Log.d(TAG, "onSelectedCountChange() called with: adapter = [$adapter], selectedCount = [$selectedCount]")

                val listAdapter = adapter as DefaultImageListAdapter

                // Actionbar title
                updateActionBarTitleCount(selectedCount, listAdapter.directory, adapter.getItemCount())

                // 通知选中个数变化
                imageAdapterSelectCountChangeRelay.accept(listAdapter)


                //                List<DefaultImageListAdapter.Item> selectedItems = listAdapter.getSelectedItems();

                //
                //                ImageService.getInstance()
                //                        .detectFileConflict(((BaseImageAdapter) adapter).getDirectory(),
                //                                Stream.of(selectedItems).map(DefaultImageListAdapter.Item::getFile).toList());
            }

            override fun onSelectedItemLongClick(view: View, position: Int, item: DefaultImageListAdapter.Item) {
                val selectedCount = mCurrentImageAdapter!!.selectedItemCount

                val menuItems = LinkedList<String>()
                menuItems.add(getString(R.string.copy_d_files_to, selectedCount))
                menuItems.add(getString(R.string.move_d_files_to, selectedCount))
                menuItems.add(getString(R.string.remove_d_files, selectedCount))

                MaterialDialog.Builder(this@MainActivity)
                        .title(getString(R.string.selected_files_operation))
                        .items(menuItems)
                        .itemsCallback { dialog, itemView, pos, text ->
                            when (pos) {
                                0 -> {
                                    // copy
                                    ToastUtils.toastShort(this@MainActivity, R.string.unimplemented)
                                }
                                1 -> {
                                    // move
                                    showMoveFileDialog()
                                }
                                2 -> {
                                    // remove
                                    ToastUtils.toastShort(this@MainActivity, R.string.unimplemented)
                                }
                            }
                        }
                        .show()
            }
        }

        return adapter
    }

    /**
     * @param selectedFiles 来自当前 adapter 中的被选中文件列表
     */
    private fun mediaFilesToListItems(mediaFiles: List<MediaFile>, selectedFiles: List<File>? = null): List<DefaultImageListAdapter.Item> {
        return Stream.of(mediaFiles)
                .map { image ->

                    val item = DefaultImageListAdapter.Item()
                    item.file = image.file
                    item.date = image.date
                    item.fileSize = image.getFileSize()

                    if (selectedFiles != null) {
                        if (selectedFiles.contains(item.file)) {
                            item.isSelected = true
                        }
                    }
                    item
                }
                .toList()
    }

    private fun imagesToDetailListItems(mediaFiles: List<MediaFile>): List<DetailImageAdapter.Item> {
        return Stream.of(mediaFiles).map { this.mediaFileToDetailItem(it) }.toList()
    }

    private fun showGridImageListAdapter(adapterToShow: DefaultImageListAdapter, showRefreshing: Boolean) {

        // Save recent accessed folder
        addRecentHistoryRecord(adapterToShow.directory)

        // Update action bar title for new shown adapter
        updateActionBarTitle(adapterToShow.directory.name)
        updateActionBarTitleCount(adapterToShow.selectedItemCount, adapterToShow.directory, adapterToShow.itemCount)

        // Setup adapter
        //        adapter.setOnInteractionListener(this);

        // Remember the current adapter's corresponding fist visible item,
        // We will restore it when user switch back to current adapter
        if (mCurrentImageAdapter != null) {
            val layoutManager = mImageList.layoutManager
            if (layoutManager is GridLayoutManager) {
                val i = layoutManager.findFirstVisibleItemPosition()
                mCurrentImageAdapter!!.firstVisibleItem = i
            } else if (layoutManager is LinearLayoutManager) {
                val i = layoutManager.findFirstVisibleItemPosition()
                mCurrentImageAdapter!!.firstVisibleItem = i
            } else if (layoutManager is StaggeredGridLayoutManager) {
                val a = IntArray(100)
                layoutManager.findFirstVisibleItemPositions(a)
                if (a.size > 0) {
                    mCurrentImageAdapter!!.firstVisibleItem = a[0]
                }
            }
        }
        mCurrentImageAdapter = adapterToShow

        // Sync data with folder list adapter
        mFolderAdapter?.let { it.mMoveFileSourceDir = adapterToShow.directory }

        // RecyclerView Layout
        mGridLayoutManager = GridLayoutManager(this, mSpanCount, GridLayoutManager.VERTICAL, false)
        mImageList.layoutManager = mGridLayoutManager

        setImageToolbarVisible(mCurrentImageAdapter!!.isSelectionMode)

        // Item event
        if (mImageList.adapter == null) {
            mImageList.adapter = adapterToShow
        } else {
            mImageList.adapter = adapterToShow
        }
        try {
            mCurrentImageAdapter!!.bindToRecyclerView(mImageList)
        } catch (t: Throwable) {
        }

        val firstVisibleItem = adapterToShow.firstVisibleItem
        if (firstVisibleItem != RecyclerView.NO_POSITION) {
            mImageList.scrollToPosition(firstVisibleItem)
        }

        if (showRefreshing) {
            mImageRefresh.isRefreshing = false
        }
    }

    private fun showDetailImageListAdapter(adapter: DetailImageAdapter, showRefreshing: Boolean) {

        // Save recent accessed folder
        addRecentHistoryRecord(adapter.directory)

        // Update action bar title for new shown adapter
        updateActionBarTitle(adapter.directory.name)
        updateActionBarTitleCount(adapter.selectedItemCount, adapter.directory, adapter.itemCount)

        // 当前显示的图片位置
        // TODO 更严谨的计算显示位置
        if (mCurrentDetailImageAdapter != null) {
            val layoutManager = mImageList.layoutManager
            val i: Int
            if (layoutManager is GridLayoutManager) {
                i = layoutManager.findFirstVisibleItemPosition()
                mCurrentDetailImageAdapter!!.firstVisibleItem = i
            } else if (layoutManager is LinearLayoutManager) {
                i = layoutManager.findFirstVisibleItemPosition()
                mCurrentDetailImageAdapter!!.firstVisibleItem = i
            }
        }
        mCurrentDetailImageAdapter = adapter

        // Folder List : Sync data with folder list
        mFolderAdapter?.let { it.mMoveFileSourceDir = adapter.directory }

        // RecyclerView Layout
        mLinearLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mImageList.layoutManager = mLinearLayoutManager
        try {
            mCurrentDetailImageAdapter!!.bindToRecyclerView(mImageList)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        if (mCurrentDetailImageAdapter != null) {
            setImageToolbarVisible(mCurrentDetailImageAdapter!!.isSelectionMode)
        }

        // Item event
        if (mImageList.adapter == null) {
            mImageList.adapter = adapter
        } else {
            mImageList.adapter = adapter
        }

        val firstVisibleItem = adapter.firstVisibleItem
        if (firstVisibleItem != RecyclerView.NO_POSITION) {
            mImageList.scrollToPosition(firstVisibleItem)
        }

        if (showRefreshing) {
            mImageRefresh.isRefreshing = false
        }
    }

    private fun setImageToolbarVisible(visible: Boolean) {
        //        mFloatingToolbar.setVisibility(View.VISIBLE);

        val deBounceTime = 1000
        when (mViewState.getViewMode()) {

            ViewMode.GRID_VIEW ->

                if (visible) {

                    if (mUpdateImageConflictFileDisposable == null) {
                        mUpdateImageConflictFileDisposable = imageAdapterSelectCountChangeRelay
                                .debounce(deBounceTime.toLong(), TimeUnit.MILLISECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        { defaultImageListAdapter ->
                                            val sourceFiles = Stream.of(defaultImageListAdapter.selectedItems)
                                                    .map({ it.file })
                                                    .toList()
                                            updateFolderListConflictItems(sourceFiles)
                                        },
                                        { RxUtils.unhandledThrowable(it) })
                    }
                } else {

                    // 清空文件夹列表冲突 badge
                    if (mFolderAdapter != null) {
                        mFolderAdapter!!.updateConflictFiles(LinkedHashMap())
                    }
                    //                    if (mUpdateImageConflictFileDisposable != null) {
                    //                        mUpdateImageConflictFileDisposable.dispose();
                    //                    }
                }
            ViewMode.LIST_VIEW -> if (visible) {

                if (mUpdateDetailImageListConflictDisposable == null) {
                    mUpdateDetailImageListConflictDisposable = detailAdapterSelectCountChangeRelay
                            .debounce(deBounceTime.toLong(), TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ detailImageAdapter ->
                                val sourceFiles = Stream.of(detailImageAdapter.selectedItems)
                                        .map({ it.file })
                                        .toList()
                                updateFolderListConflictItems(sourceFiles)
                            }, { RxUtils.unhandledThrowable(it) })
                }
            } else {
                if (mFolderAdapter != null) {
                    mFolderAdapter!!.updateConflictFiles(LinkedHashMap())
                }
            }
            ViewMode.UNKNOWN -> {
            }
        }

        mImageToolbar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun clearFolderListConflictItems() {
        mFolderAdapter!!.updateConflictFiles(LinkedHashMap())
    }

    private fun updateFolderListConflictItems(sourceFiles: List<File>) {

        ImageModule
                .detectFileExistence(sourceFiles)
                .compose(RxUtils.applySchedulers())
                .subscribe({ detectFileExistenceResult ->
                    detectFileExistenceResult?.existedFiles?.let {
                        mFolderAdapter?.updateConflictFiles(it.toMap())
                    }
                },
                        { RxUtils.unhandledThrowable(it) })
    }

    private fun onClickImageListItem(imageView: View, position: Int, mediaFile: File) {
        try {
            val absolutePath = mediaFile.absolutePath
            if (PathUtils.isVideoFile(absolutePath)) {
                // Play video
                ShareUtils.playVideo(this, absolutePath)
            } else {
                startImageViewerActivity(mediaFile.parentFile, imageView, position, mediaFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "image list item click exception : " + e.message)
        }

    }

    private fun addRecentHistoryRecord(directory: File) {

        UserModule
                .addOpenFolderRecentRecord(directory)
                .compose(RxUtils.applySchedulers())
                .subscribe({
                    val i1 = org.apache.commons.collections4.ListUtils.indexOf(mRecentHistory) { file -> file == directory }
                    if (i1 != -1) {
                        mRecentHistory.removeAt(i1)
                    }
                    mRecentHistory.add(0, directory)

                }, { RxUtils.unhandledThrowable(it) })
    }


    private fun reloadFolderList(fromCacheFirst: Boolean) {
        if (mFolderAdapter != null) {
            mPresenter.loadFolderList(fromCacheFirst, true)
        } else {
            mPresenter.loadFolderList(fromCacheFirst)
        }
    }

    private fun reloadImageList(hideRefreshControlWhenFinish: Boolean) {

        if (mIsImageListLoaded) {

            // Reload image list
            if (mViewState.getViewMode() == ViewMode.GRID_VIEW) {

                if (mCurrentImageAdapter != null) {

                    updateFolderListItemThumbnailList(mCurrentImageAdapter!!.directory)

                    diffUpdateDefaultImageListAdapter(mCurrentImageAdapter, false, hideRefreshControlWhenFinish)
                }
            } else if (mViewState.getViewMode() == ViewMode.LIST_VIEW) {

                if (mCurrentDetailImageAdapter != null) {
                    updateFolderListItemThumbnailList(mCurrentDetailImageAdapter!!.directory)

                    diffUpdateDetailImageAdapter(mCurrentDetailImageAdapter!!, false, hideRefreshControlWhenFinish)
                }
            } else {
                Log.e(TAG, "reloadImageList: unknown handled mode : " + mViewState.getViewMode())
            }

        } else {
            mSpanCount = DEFAULT_IMAGE_LIST_COLUMN_COUNT // columns

            // Item spacing
            //            LayoutMarginDecoration marginDecoration =
            //                    new LayoutMarginDecoration(mSpanCount, getResources().getDimensionPixelSize(R.dimen.image_list_item_space));
            //
            //            mImageList.addItemDecoration(marginDecoration);

            // Load recent history and show the first one
            // 根据用户界面偏好设置来显示图片列表
            Logger.d("initial loading image List ")

            mPresenter.loadInitialPreference()
            mIsImageListLoaded = true
        }

        //        mImageList.setLayoutManager(new StaggeredGridLayoutManager(mSpanCount, StaggeredGridLayoutManager.VERTICAL));
        //        mImageList.setAdapter(new DefaultImageListAdapter(new LinkedList<DefaultImageListAdapter.Item>()));
        //        GridLayoutManager layout = new GridLayoutManager(this, mSpanCount, GridLayoutManager.VERTICAL, false);
        //        mImageList.setLayoutManager(layout);
        //SectionedImageListAdapter adapter = new SectionedImageListAdapter(new LinkedList<SectionedImageListAdapter.Section>());
        //adapter.setLayoutManager(layout);
        //mImageList.setAdapter(adapter);

        //        mImageList.addItemDecoration(new GridSpacingItemDecoration(mSpanCount,
        //                getResources().getDimensionPixelSize(R.dimen.image_list_item_space), true));
    }

    private fun updateFolderListItemThumbnailList(directory: File?) {
        if (directory == null) {
            Log.e(TAG, "updateFolderListItemThumbnailList: directory is null/empty")
            return
        }
        ImageModule
                .rescanDirectoryThumbnailList(directory)
                .compose(RxUtils.applySchedulers())
                .subscribe({ files ->

                }, { RxUtils.unhandledThrowable(it) })
    }

    private fun loadImages(directory: File): Observable<List<DefaultImageListAdapter.Item>> {
        return Observable.create { e ->
            val items = LinkedList<DefaultImageListAdapter.Item>()
            val images = ImageModule.listMediaFiles(directory)

            for (file in images) {

                val item = DefaultImageListAdapter.Item()
                item.file = file
                items.add(item)
            }

            e.onNext(items)
            e.onComplete()
        }
    }

    private fun changeFloatingCount(msgIncrease: Int) {
        val intent = Intent()
        intent.action = FloatWindowService.UPDATE_ACTION
        intent.putExtra(FloatWindowService.EXTRA_MSG, msgIncrease)
        sendBroadcast(intent)
    }

    private fun startImageViewerActivity(directory: File, view: View, position: Int, file: File) {

        Logger.d("查看图片 %d：%s", position, file.absoluteFile)

        mCurrentViewerImageIndex = position
        val imageView = view.findViewById<View>(R.id.image)
        if (imageView == null || imageView !is ImageView) {
            ToastUtils.toastLong(this, getString(R.string.cannot_get_image_for_position, position))
            return
        }

        // Construct an Intent as normal
        val intent = ImageActivity.newIntentViewFileList(this, directory.absolutePath,
                position, mViewState.getSortOrder(), mViewState.getSortWay())

        // BEGIN_INCLUDE(start_activity)
        /**
         * Now create an [android.app.ActivityOptions] instance using the
         * [ActivityOptionsCompat.makeSceneTransitionAnimation] factory
         * method.
         */
        val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,

                // Now we provide a list of Pair items which contain the view we can transitioning
                // from, and the name of the view it is transitioning to, in the launched activity
                Pair(imageView, TransitionUtils.generateTransitionName(file.absolutePath)))
        // Now we can start the Activity, providing the activity options as a bundle
        ActivityCompat.startActivity(this, intent, activityOptions.toBundle())
        // END_INCLUDE(start_activity)
    }

    fun startPhotoActivity(context: Context, file: File, imageView: View) {
        val intent = Intent(context, DragPhotoActivity::class.java)
        val location = IntArray(2)

        //imageView.getLocationOnScreen(location);
        imageView.getLocationInWindow(location)

        intent.putExtra(DragPhotoActivity.EXTRA_LEFT, location[0])
        intent.putExtra(DragPhotoActivity.EXTRA_TOP, location[1])
        intent.putExtra(DragPhotoActivity.EXTRA_HEIGHT, imageView.height)
        intent.putExtra(DragPhotoActivity.EXTRA_WIDTH, imageView.width)
        intent.putExtra(DragPhotoActivity.EXTRA_FILE_PATH, file.absolutePath)
        intent.putExtra(DragPhotoActivity.EXTRA_FULLSCREEN, false)

        context.startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun updateActionBarTitleCount(selectedCount: Int, directory: File, itemCount: Int) {
        updateToolbarSubTitleCount(selectedCount, directory, itemCount)
        //        File dir = adapter.getDirectory();
        //        if (mFolderAdapter != null) {
        //            mFolderAdapter.updateSelectedCount(dir, selectedCount);
        //        }
        //
        //        // Update title
        //        String title;
        //        if (selectedCount <= 0) {
        //            title = dir.getName();
        //        } else {
        //            title = getResources().getString(R.string.fmt_main_image_selected_title, dir.getName(), selectedCount, adapter.getItemCount());
        //        }
        //        mToolbar.setTitle(title);
    }

    private fun updateToolbarSubTitleCount(selectedCount: Int, directory: File, itemCount: Int) {
        if (mFolderAdapter != null) {
            mFolderAdapter!!.updateSelectedCount(directory, selectedCount)
        }

        // Update title
        val title: String
        title = getSubTitleText(selectedCount, itemCount)
        mToolbar!!.subtitle = title
    }

    private fun getSubTitleText(selectedCount: Int, itemCount: Int): String {
        val title: String
        if (selectedCount <= 0) {
            title = ""
        } else {
            title = resources.getString(R.string.percent_d_d, selectedCount, itemCount)
        }
        return title
    }

    private fun showRemoveFileDialog() {
        val currImageAdapter = currentAdapter

        val selectCount = currImageAdapter!!.selectedItemCount

        val countStr = selectCount.toString()
        val countSB = SpannableStringBuilder.valueOf(selectCount.toString())
        val color = resources.getColor(R.color.colorAccent)
        val foregroundColorSpan = ForegroundColorSpan(color)
        countSB.setSpan(foregroundColorSpan, 0, countStr.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        MaterialDialog.Builder(this)
                .title(R.string.remove_files)
                .content(getString(R.string.confirm_to_remove_files_in_folder_s,
                        currImageAdapter.directory.name, selectCount))
                .positiveText(R.string.confirm)
                .onPositive { dialog, which ->


                    val selectedFiles = currImageAdapter.selectedFiles

                    ImageModule
                            .removeFiles(selectedFiles)
                            .compose(RxUtils.applySchedulers())
                            .subscribe({ integerListPair ->
                                val total = integerListPair.first
                                val failedFiles = integerListPair.second
                                if (!failedFiles.isEmpty()) {
                                    ToastUtils.toastLong(this,
                                            getString(R.string.remove_d_files_success_with_d_files_failed_with_d_files,
                                                    total, total!! - failedFiles.size, failedFiles.size))
                                } else {
                                    ToastUtils.toastLong(this, getString(R.string.already_removed_d_files, total))
                                }
                            }) { throwable ->
                                throwable.printStackTrace()
                                ToastUtils.toastLong(this, R.string.remove_file_failed)
                            }
                }
                .negativeText(R.string.cancel)
                .show()
    }

    private fun showMoveFileDialog() {

        val selectedFiles = selectedFileList
        showMoveFileFragmentDialog(ArrayList(selectedFiles))

        /*ImageService.getInstance()
                .loadFolderModel(true)
                .compose(applySchedulers())
                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .subscribe(adapter -> {
                    showMoveFileDialog(selectedFiles, adapter);
                });*/
    }

    private fun <Return> mapValueTo(viewMode: ViewMode, mapAction: MapAction1<Return, ViewMode>): Return {
        return mapAction.onMap(viewMode)
    }

    private fun showMoveFileFragmentDialog(selectedFiles: ArrayList<String>) {
        val fragment = MoveFileDialogFragment.newInstance(selectedFiles)
        fragment.show(supportFragmentManager, "move file dialog")
    }

    private fun showMoveFileDialog(selectedFiles: List<File>, adapter: SectionedFolderListAdapter) {
        val contentView = createMoveFileDialogContentView(adapter)

        MaterialDialog.Builder(this)
                .title(R.string.move_selected_files_to)
                .customView(contentView, false)
                .positiveText(R.string.move_file)
                .negativeText(R.string.cancel)
                .onPositive { dialog, which ->
                    val tag = contentView.getTag(R.id.item)
                    if (tag != null && tag is SectionedFolderListAdapter.Item) {

                        tag.mFile?.let {
                            ImageModule
                                    .moveFilesToDirectory(it, selectedFiles)
                                    .compose(RxUtils.applySchedulers())
                                    .subscribe({ count ->
                                        if (count == selectedFiles.size) {
                                            ToastUtils.toastLong(this@MainActivity, this@MainActivity.getString(R.string.already_moved_d_files, count))
                                        } else if (count > 0 && count < selectedFiles.size) {
                                            // 部分文件移动失败
                                            ToastUtils.toastShort(this@MainActivity, R.string.move_files_successfully_but_)
                                        } else {
                                            ToastUtils.toastShort(this@MainActivity, R.string.move_files_failed)
                                        }
                                    }) { throwable -> ToastUtils.toastShort(this@MainActivity, R.string.move_files_failed) }
                        }
                    }
                }
                .onNegative { dialog, which ->

                }
                //                            .adapter(adapter, new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false))
                .show()
    }

    private fun createMoveFileDialogContentView(adapter: SectionedFolderListAdapter): View {
        val root = LayoutInflater.from(this)
                .inflate(R.layout.move_file_dialog, null, false)

        val recyclerView = root.findViewById<RecyclerView>(R.id.folder_list)
        val desc = root.findViewById<TextView>(R.id.desc)

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter

        val clickListener = { view: View, position: Int ->
            SectionedListItemClickDispatcher(adapter)
                    .dispatch(position, object : SectionedListItemDispatchListener<SectionedRecyclerViewAdapter<*>> {
                        override fun onHeader(adapter: SectionedRecyclerViewAdapter<*>, coord: ItemCoord) {
                            val sectionExpanded = adapter.isSectionExpanded(coord.section())
                            if (sectionExpanded) {
                                adapter.collapseSection(coord.section())
                            } else {
                                adapter.expandSection(coord.section())
                            }
                        }

                        override fun onFooter(adapter: SectionedRecyclerViewAdapter<*>, coord: ItemCoord) {

                        }

                        override fun onItem(adapter: SectionedRecyclerViewAdapter<*>, coord: ItemCoord) {
                            val item = mFolderAdapter!!.getItem(coord)

                            desc.text = getString(R.string.move_selected_images_to_directory_s, item?.mFile?.name
                                    ?: "")
                            root.setTag(R.id.item, item)
                        }
                    })
        }
        val longClickListener = { view: View, position: Int ->

        }
        recyclerView.addOnItemTouchListener(RecyclerItemTouchListener(this, recyclerView, clickListener, longClickListener))

        return root
    }

    // ---------------------------------------------------------------------------------------------
    // MVP View interface
    // ---------------------------------------------------------------------------------------------


    override fun onLoadedUserInitialPreferences(userInitialPreferences: UserInitialPreferences) {
        mRecentHistory = Stream.of<RecentRecord>(userInitialPreferences.recentRecords).map<File> { r -> File(r.filePath) }.toList()

        mViewState.setViewMode(userInitialPreferences.viewMode)
        mViewState.setSortWay(userInitialPreferences.sortWay)
        mViewState.setSortOrder(userInitialPreferences.sortOrder)

        if (ListUtils.isEmpty(mRecentHistory)) {
            Logger.w("reloadImageList: 最近访问目录列表为空，加载相机相册")
            showImageList(SystemUtils.getCameraDir(), true, false, true)
        } else {
            val recentDir = ListUtils.firstOf(mRecentHistory)

            Logger.d("reloadImageList: 显示最近显示目录 : $recentDir")
            showImageList(recentDir, true, false, true)
        }
    }

    override fun onErrorMessage(msg: Int) {
        ToastUtils.toastShort(this, msg)
    }

    override fun onLoadedFolderModel(folderModel: FolderModel) {
        Log.w(TAG, "onLoadedFolderModel() called with folderModel = [$folderModel]")
        showFolderList(folderModel)
    }

    override fun onLoadedFolderModel(folderModel: FolderModel, diff: Boolean) {
        if (mFolderAdapter == null) {
            if (diff) {
                val newAdapter = FolderListAdapterUtils.folderModelToSectionedFolderListAdapter(folderModel)
                if (newAdapter.sections.isEmpty()) {
                    mFolderListEmptyView.visibility = View.VISIBLE
                    mFolderListEmptyView.setText(R.string.folder_list_empty_text)
                } else {
                    mFolderListEmptyView.visibility = View.GONE
                }
                mFolderAdapter!!.diffUpdate(newAdapter)
                // TODO 参数化 refreshing
            } else {
                onLoadedFolderModel(folderModel)
            }
        } else {
            onLoadedFolderModel(folderModel)
        }

        mFolderListRefresh.isRefreshing = false
    }

    override fun onLoadFolderModelFailed(throwable: Throwable) {
        RxUtils.unhandledThrowable(throwable)
        mFolderListRefresh.isRefreshing = false
    }

    override fun onDeletedDirectory(dir: File) {
        ToastUtils.toastLong(this, R.string.already_deleted_folder_s, dir.absolutePath)
    }

    override fun onDeleteDirectoryFailed(dir: File, throwable: Throwable) {
        ToastUtils.toastLong(this, R.string.force_to_delete_folder_s_failed, dir.absolutePath)
    }

    override fun onDeleteNotEmptyDirectoryFailed(dir: File, fileCount: Int?) {
        showForceDeleteFolderDialog(dir, fileCount!!)
    }

    override fun onDiffDetailMediaFiles(dir: File, mediaFiles: List<MediaFile>, hideRefreshControl: Boolean, purpose: MainContract.LoadImageListPurpose) {
        if (hideRefreshControl) {
            mImageRefresh.isRefreshing = false
        }

        if (purpose === MainContract.LoadImageListPurpose.RefreshDetailList) {
            val adapter = getDetailImageAdapter(dir)
            if (adapter != null) {

                val items = imagesToDetailListItems(mediaFiles)
                val detailImageAdapter = DetailImageAdapter(R.layout.item_image_detail, items)

                transferAdapterStatus(adapter, detailImageAdapter)

                adapter.diffUpdate(items)
                detailAdapterSelectCountChangeRelay.accept(adapter)
            }
        } else if (purpose === MainContract.LoadImageListPurpose.RefreshDefaultList) {

            val adapter = getDefaultImageListAdapter(dir)


            val items = mediaFilesToListItems(mediaFiles, adapter!!.selectedFiles)
            adapter.diffUpdate(items)
            imageAdapterSelectCountChangeRelay.accept(adapter)
        }
    }

    override fun onDiffLoadDetailMediaFileFailed(dir: File, hideRefreshControl: Boolean, purpose: MainContract.LoadImageListPurpose) {

        if (purpose === MainContract.LoadImageListPurpose.RefreshDefaultList || purpose === MainContract.LoadImageListPurpose.RefreshDetailList) {
            if (hideRefreshControl) {
                mImageRefresh.isRefreshing = false
            }
        }
    }

    override fun onDiffLoadedDefaultMediaFiles(dir: File, mediaFiles: List<MediaFile>, hideRefreshControl: Boolean) {

    }

    override fun onDiffLoadDefaultMediaFileFailed(dir: File, hideRefreshControl: Boolean) {

    }

    companion object {

        private val TAG = "MainActivity"
        // 文件夹列表缩略图数量
        val MOVE_FILE_DIALOG_THUMBNAIL_COUNT = 3
        val DEFAULT_IMAGE_LIST_COLUMN_COUNT = 3

        internal fun checkMenuItem(menu: Menu?, id: Int, checked: Boolean) {
            if (menu == null) {
                return
            }
            val item = menu.findItem(id)
            if (item != null) {
                item.isChecked = checked
            }
        }
    }
}

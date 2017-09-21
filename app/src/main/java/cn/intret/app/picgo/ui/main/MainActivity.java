package cn.intret.app.picgo.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.annimon.stream.Collector;
import com.annimon.stream.Stream;
import com.annimon.stream.function.BiConsumer;
import com.annimon.stream.function.Function;
import com.annimon.stream.function.Supplier;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.f2prateek.rx.preferences2.Preference;
import com.jakewharton.rxrelay2.BehaviorRelay;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.ConflictResolverDialogFragment;
import cn.intret.app.picgo.model.FolderModel;
import cn.intret.app.picgo.model.GroupMode;
import cn.intret.app.picgo.model.ImageService;
import cn.intret.app.picgo.model.LoadMediaFileParam;
import cn.intret.app.picgo.model.MediaFile;
import cn.intret.app.picgo.model.ImageFolder;
import cn.intret.app.picgo.model.ImageGroup;
import cn.intret.app.picgo.model.SortOrder;
import cn.intret.app.picgo.model.SortWay;
import cn.intret.app.picgo.model.UserDataService;
import cn.intret.app.picgo.model.ViewMode;
import cn.intret.app.picgo.model.event.FolderModelChangeMessage;
import cn.intret.app.picgo.model.event.RecentOpenFolderListChangeMessage;
import cn.intret.app.picgo.model.event.RemoveFileMessage;
import cn.intret.app.picgo.model.event.RenameDirectoryMessage;
import cn.intret.app.picgo.model.event.RescanFolderListMessage;
import cn.intret.app.picgo.model.event.RescanFolderThumbnailListMessage;
import cn.intret.app.picgo.model.event.RescanImageDirectoryMessage;
import cn.intret.app.picgo.ui.adapter.brvah.BaseImageAdapter;
import cn.intret.app.picgo.ui.adapter.brvah.DefaultImageListAdapter;
import cn.intret.app.picgo.ui.adapter.brvah.DetailImageAdapter;
import cn.intret.app.picgo.ui.adapter.brvah.ExpandableFolderAdapter;
import cn.intret.app.picgo.ui.adapter.FlatFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.brvah.FolderListAdapter;
import cn.intret.app.picgo.ui.adapter.brvah.FolderListAdapterUtils;
import cn.intret.app.picgo.ui.adapter.ImageTransitionNameGenerator;
import cn.intret.app.picgo.ui.adapter.brvah.BaseSelectableAdapter;
import cn.intret.app.picgo.ui.adapter.brvah.FolderItem;
import cn.intret.app.picgo.ui.adapter.SectionFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.SectionedFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.SectionedImageListAdapter;
import cn.intret.app.picgo.ui.adapter.SectionedListItemClickDispatcher;
import cn.intret.app.picgo.ui.adapter.SectionedListItemDispatchListener;
import cn.intret.app.picgo.ui.event.CurrentImageChangeMessage;
import cn.intret.app.picgo.ui.floating.FloatWindowService;
import cn.intret.app.picgo.ui.pref.SettingActivity;
import cn.intret.app.picgo.utils.Action0;
import cn.intret.app.picgo.utils.DateTimeUtils;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.MapAction1;
import cn.intret.app.picgo.utils.PathUtils;
import cn.intret.app.picgo.utils.PopupUtils;
import cn.intret.app.picgo.utils.RxUtils;
import cn.intret.app.picgo.utils.SystemUtils;
import cn.intret.app.picgo.utils.ToastUtils;
import cn.intret.app.picgo.view.T9KeypadView;
import cn.intret.app.picgo.widget.EmptyRecyclerView;
import cn.intret.app.picgo.widget.RecyclerItemTouchListener;
import cn.intret.app.picgo.widget.SectionDecoration;
import cn.intret.app.picgo.widget.SuperRecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class MainActivity extends BaseAppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final int MOVE_FILE_DIALOG_THUMBNEIL_COUNT = 3;
    public static final int DEFAULT_IMAGE_LIST_COLLUMN_COUNT = 4;

    @BindView(R.id.refresh) SwipeRefreshLayout mRefresh;
    @BindView(R.id.img_list) SuperRecyclerView mImageList;
    @BindView(R.id.empty_view) View mEmptyView;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;

    @BindView(R.id.drawer_folder_list) EmptyRecyclerView mFolderList;
    @BindView(R.id.folder_list_empty_view) TextView mFolderListEmptyView;

    @BindView(R.id.view_mode) RadioGroup mModeRadioGroup;
    @BindView(R.id.floatingToolbar) Toolbar mFloatingToolbar;

    @BindView(R.id.image_tool_bar) ViewGroup mImageToolbar;

    @BindView(R.id.t9_keypad_container) ViewGroup mKeypadContainer;
    @BindView(R.id.t9_keypad) T9KeypadView mKeypad;
    @BindView(R.id.keyboard_switch_layout) ViewGroup mKeypadSwitchLayout;
    @BindView(R.id.keyboard_switch_image_view) ImageView mKeypadSwitch;


    /*
     * ActionBar/Toolbar
     */

    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;

    /*
     * Folder list
     */
    private FolderListAdapter mFolderListAdapter;
    /*
     * The image list current showing folder
     */
    private File mCurrentFolder;
    private SectionedFolderListAdapter mFolderAdapter;

    /*
     * Floating view
     */
    private Intent mStartFloatingIntent;

    /*
     * Image List
     */

    /**
     * Key: Directory
     */
    Map<File, DefaultImageListAdapter> mImageListAdapters = new LinkedHashMap<>();
    private DefaultImageListAdapter mCurrentImageAdapter;
    private GridLayoutManager mGridLayoutManager;

    // todo Map<ViewMode,BaseImageAdapter>

    Map<File, DetailImageAdapter> mDetailImageListAdapters = new LinkedHashMap<>();
    Map<String, SectionedImageListAdapter> mWeekSectionedImageListAdapters = new LinkedHashMap<>();
    Map<String, SectionedImageListAdapter> mDaySectionedImageListAdapters = new LinkedHashMap<>();
    Map<String, SectionedImageListAdapter> mMonthSectionedImageListAdapters = new LinkedHashMap<>();

    private GridLayoutManager mGridLayout;
    private SectionedImageListAdapter mCurrentAdapter;

    BehaviorRelay<DefaultImageListAdapter> mImageSelectCountRelay;
    BehaviorRelay<DetailImageAdapter> mDetailImageSelectCountRelay;

    // Image list configuration

    private int mSpanCount;
    // 图片查看器显示的图片索引
    private int mCurrentViewerImageIndex = -1;

    private GroupMode mGroupMode = GroupMode.DEFAULT;

    private ViewMode mViewMode = ViewMode.GRID_VIEW;

    /*
     * Data loading status
     */
    private boolean mIsFolderListLoaded = false;
    private boolean mIsImageListLoaded = false;

    List<File> mRecentHistory = new LinkedList<>();
    private DetailImageAdapter mCurrentDetailImageAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private ExpandableFolderAdapter mExpandableFolderAdapter;
    private Disposable mUpdateImageConflictFileDisposable;
    private Disposable mUpdateDetailImageListConflictDisposable;
    private ListPopupWindow mFolderItemContextMenu;
    private SortWay mSortWay = SortWay.UNKNOWN;
    private SortOrder mSortOrder = SortOrder.UNKNOWN;
    private boolean mEnableT9Filter = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initToolBar();
        initDrawer();
        initImageList();


        initTransition();

        EventBus.getDefault().register(this);
        //showFloatingWindow();
    }

    private void initImageList() {

        // Image list header
        mFloatingToolbar.inflateMenu(R.menu.image_action_menu);
        mFloatingToolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_copy: {
                    ToastUtils.toastShort(MainActivity.this, "copy");
                }
                break;
                case R.id.action_move:
                    showMoveFileDialog();
                    break;
                case R.id.action_remove:
                    showRemoveFileDialog();
                    break;
            }
            return false;
        });

        // Refresh
        mRefresh.setOnRefreshListener(() -> reloadImageList(true));

        //initListViewToolbar();
    }

    private void initListViewToolbar() {
//        mFloatingToolbar.setClickListener(new FloatingToolbar.ItemClickListener() {
//            @Override
//            public void onItemClick(MenuItem item) {
//                Log.d(TAG,"onItemClick() called with: item = [" + item + "]");
//            }
//
//            @Override
//            public void onItemLongClick(MenuItem item) {
//                Log.d(TAG,"onItemLongClick() called with: item = [" + item + "]");
//            }
//        });
////        mFloatingToolbar.attachFab(mFab);
////        mFloatingToolbar.handleFabClick(true);
//        mFloatingToolbar.enableAutoHide(true);


    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");

        super.onStart();

        loadFolderList();
        reloadImageList(false);
    }

    @Override
    protected void onResume() {
//        changeFloatingCount(FloatWindowService.MSG_INCREASE);

//        mDrawerLayout.openDrawer(Gravity.LEFT);
        super.onResume();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: ");


//        changeFloatingCount(FloatWindowService.MSG_DECREASE);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
//        stopService(mStartFloatingIntent);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);

        ViewMode viewMode = UserDataService.getInstance().getViewMode().get();
        if (viewMode == ViewMode.UNKNOWN) {
            Log.e(TAG, "onCreateOptionsMenu: invalid view mode : " + viewMode);
        } else {

            MenuItem item = menu.findItem(R.id.app_bar_view_mode);
            switch (viewMode) {
                case GRID_VIEW:
                    item.setIcon(R.drawable.ic_grid_on_black_24px);
                    break;
                case LIST_VIEW:
                    item.setIcon(R.drawable.ic_list_black_24px);
                    break;
            }
        }

        Boolean showHiddenFile = UserDataService.getInstance().getShowHiddenFilePreference().get();
        MenuItem item = menu.findItem(R.id.app_bar_show_hidden_folder);
        if (item != null) {
            item.setChecked(showHiddenFile);
        }

        return true;
    }

    private void initTransition() {

        ActivityCompat.setEnterSharedElementCallback(this, new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                Log.d(TAG, "enter onMapSharedElements() called with: names = [" + names + "], sharedElements = [" + sharedElements + "]");
                super.onMapSharedElements(names, sharedElements);
            }
        });


        ActivityCompat.setExitSharedElementCallback(this, new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {

                Log.d(TAG, "exit before onMapSharedElements() called with: names = [" + names + "], sharedElements = [" + sharedElements + "]");
                try {

                    if (mCurrentViewerImageIndex >= 0) {
                        File file = null;
                        switch (mViewMode) {
                            case GRID_VIEW: {
                                DefaultImageListAdapter.Item item = mCurrentImageAdapter.getItem(mCurrentViewerImageIndex);
                                if (item != null) {
                                    file = item.getFile();
                                }
                            }
                            break;

                            case LIST_VIEW: {
                                DetailImageAdapter.Item item = mCurrentDetailImageAdapter.getItem(mCurrentViewerImageIndex);
                                if (item != null) {
                                    file = item.getFile();
                                }
                            }
                            break;
                        }

                        if (file != null) {
                            String filePath = file.getAbsolutePath();
                            String transitionName = ImageTransitionNameGenerator.generateTransitionName(filePath);
                            String fileTypeTransitionName = ImageTransitionNameGenerator.generateTransitionName(
                                    ImageTransitionNameGenerator.TRANSITION_PREFIX_FILETYPE, filePath);

                            boolean addFileTypeTransitionName = PathUtils.isVideoFile(filePath);

                            sharedElements.clear();

                            RecyclerView.ViewHolder vh = mImageList.findViewHolderForAdapterPosition(mCurrentViewerImageIndex);
                            if (vh instanceof DefaultImageListAdapter.ViewHolder) {
                                DefaultImageListAdapter.ViewHolder viewHolder = ((DefaultImageListAdapter.ViewHolder) vh);

                                sharedElements.put(transitionName, viewHolder.getView(R.id.image));

                                if (addFileTypeTransitionName) {
                                    sharedElements.put(fileTypeTransitionName, viewHolder.getView(R.id.file_type));
                                }
                            } else if (vh instanceof DetailImageAdapter.ItemViewHolder) {
                                DetailImageAdapter.ItemViewHolder viewHolder = (DetailImageAdapter.ItemViewHolder) vh;
                                sharedElements.put(transitionName, viewHolder.getView(R.id.image));

                                if (addFileTypeTransitionName) {
                                    sharedElements.put(fileTypeTransitionName, viewHolder.getView(R.id.file_type));
                                }
                            }

                            names.clear();
                            names.add(transitionName);

                            if (addFileTypeTransitionName) {
                                names.add(fileTypeTransitionName);
                            }
                        }

                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                Log.d(TAG, "exit after onMapSharedElements() called with: names = [" + names + "], sharedElements = [" + sharedElements + "]");

                super.onMapSharedElements(names, sharedElements);
            }
        });

    }

    private void showConflictDialog(File destDir, List<android.util.Pair<File, File>> conflictFiles) {

        if (conflictFiles.isEmpty()) {
            Log.w(TAG, "showConflictDialog: conflict files is empty");
            return;
        }

        List<String> strings = Stream.of(conflictFiles).map(fileFilePair -> fileFilePair.second.getAbsolutePath()).toList();
        ConflictResolverDialogFragment fragment = ConflictResolverDialogFragment.newInstance(destDir.getAbsolutePath(), new ArrayList<>(strings));
        fragment.show(getSupportFragmentManager(), "Conflict Resolver Dialog");
    }

    /*
     * 消息处理
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RecentOpenFolderListChangeMessage message) {
        Log.d(TAG, "onEvent() called with: message = [" + message + "]");

        mRecentHistory = Stream.of(message.getRecentRecord()).map(recentRecord -> new File(recentRecord.getFilePath())).toList();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MoveFileResultMessage message) {

        File destDir = message.getDestDir();
        if (destDir != null) {
            DefaultImageListAdapter adapter = mImageListAdapters.get(destDir);
            if (adapter != null) {
                int selectedCount = adapter.getSelectedItemCount();
                int itemCount = adapter.getItemCount();
                mFolderAdapter.updateSelectedCount(message.getDestDir(), selectedCount);
                mFolderAdapter.updateItemCount(message.getDestDir(), itemCount);
            }
        }

        List<android.util.Pair<File, File>> successFiles = message.getResult().getSuccessFiles();
        if (successFiles != null) {
            Stream.of(successFiles)
                    .groupBy(fileFilePair -> fileFilePair.second.getParentFile())
                    .forEach(objectListEntry -> {
                        File dir = objectListEntry.getKey();
                        DefaultImageListAdapter adapter = mImageListAdapters.get(dir);
                        if (adapter != null) {
                            int selectedCount = adapter.getSelectedItemCount();
                            int itemCount = adapter.getItemCount();
                            mFolderAdapter.updateSelectedCount(message.getDestDir(), selectedCount);
                            mFolderAdapter.updateItemCount(message.getDestDir(), itemCount);
                        }
                    });
        }

        showConflictDialog(
                message.getDestDir(),
                message.getResult().getConflictFiles());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CurrentImageChangeMessage message) {
        Log.d(TAG, "onEvent() called with: message = [" + message + "]");
        mCurrentViewerImageIndex = message.getPosition();

        mImageList.scrollToPosition(mCurrentViewerImageIndex);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RemoveFileMessage message) {
        File file = message.getFile();
        if (file != null) {
            File dir = file.getParentFile();

            switch (mViewMode) {

                case GRID_VIEW: {
                    DefaultImageListAdapter adapter = mImageListAdapters.get(dir);
                    if (adapter != null) {
                        Log.d(TAG, "onEvent: RemoveFileMessage ");
                        diffUpdateDefaultImageListAdapter(adapter, true, false);
                    }
                }
                break;
                case LIST_VIEW: {
                    DetailImageAdapter detailImageAdapter = mDetailImageListAdapters.get(dir);
                    if (detailImageAdapter != null) {
                        diffUpdateDetailImageAdapter(detailImageAdapter, true, false);
                    }
                }
                break;
                case UNKNOWN:
                    break;
            }


            updateFolderListItemThumbnailList(file.getParentFile());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RescanFolderThumbnailListMessage message) {
        Log.d(TAG, "onEvent() called with: message = [" + message + "]");


        mFolderAdapter.updateThumbList(message.getDirectory(), message.getThumbnails());

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(RescanImageDirectoryMessage message) {

        Log.d(TAG, "onEvent() called with: message = [" + message + "]");

        File dir = message.getDirectory();

        // 更新目录对应 Adapter
        DefaultImageListAdapter adapter = mImageListAdapters.get(dir);
        if (adapter != null) {
            diffUpdateDefaultImageListAdapter(adapter, true, false);
        }

        DetailImageAdapter detailImageAdapter = mDetailImageListAdapters.get(dir);
        if (detailImageAdapter != null) {
            diffUpdateDetailImageAdapter(detailImageAdapter, true, false);
        }


        // 更新抽屉中的缩略图列表
        updateFolderListItemThumbnailList(dir);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RescanFolderListMessage message) {
        com.orhanobut.logger.Logger.d("RescanFolderListMessage " + message);
        diffUpdateFolderListAdapter(mFolderAdapter, true);
    }

    public boolean isCurrentShowDirectory(File dir) {
        switch (mViewMode) {

            case GRID_VIEW:
                if (mCurrentImageAdapter == null) {
                    return false;
                }
                if (mCurrentImageAdapter.getDirectory() != null) {
                    return mCurrentImageAdapter.getDirectory().equals(dir);
                }
                break;
            case LIST_VIEW:
                if (mCurrentDetailImageAdapter == null) {
                    return false;
                }
                if (mCurrentDetailImageAdapter.getDirectory() != null) {
                    return mCurrentDetailImageAdapter.getDirectory().equals(dir);
                }
                break;
            case UNKNOWN:
                break;
        }
        return false;
    }

    Pair<Integer, Integer> getCurrentSelectedCount() {

        return mapValueTo(mViewMode, element -> {
                    switch (element) {

                        case GRID_VIEW:
                            if (mCurrentImageAdapter != null) {
                                return new Pair<Integer, Integer>(
                                        mCurrentImageAdapter.getSelectedItemCount(), mCurrentDetailImageAdapter.getSelectedItemCount());
                            }
                            break;
                        case LIST_VIEW:
                            if (mCurrentDetailImageAdapter != null) {
                                return new Pair<Integer, Integer>(
                                        mCurrentDetailImageAdapter.getSelectedItemCount(), mCurrentDetailImageAdapter.getItemCount()
                                );
                            }
                            break;
                        case UNKNOWN:
                            break;
                    }
                    return null;
                }
        );
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RenameDirectoryMessage message) {

        com.orhanobut.logger.Logger.d("RenameDirectoryMessage " + message);
        File oldDirectory = message.getOldDirectory();
        //mFolderAdapter.renameDirectory(oldDirectory, message.getNewDirectory());


        // Update Title & Subtitle
        if (isCurrentShowDirectory(message.getOldDirectory())) {
            Pair<Integer, Integer> result = getCurrentSelectedCount();
            if (result != null) {
                updateActionBarTitle(message.getNewDirectory().getName());

                mToolbar.setSubtitle(getSubTitleText(result.first, result.second));
            }
        }

        // Update : Image list adapter
        DefaultImageListAdapter defaultImageListAdapter = mImageListAdapters.get(oldDirectory);
        if (defaultImageListAdapter != null) {

            mImageListAdapters.remove(oldDirectory);

            defaultImageListAdapter.setDirectory(message.getNewDirectory());
            mImageListAdapters.put(message.getNewDirectory(), defaultImageListAdapter);
        }

        // Update : Detail image list adapter
        DetailImageAdapter detailImageAdapter = mDetailImageListAdapters.get(oldDirectory);
        if (detailImageAdapter != null) {
            mDetailImageListAdapters.remove(oldDirectory);
            detailImageAdapter.setDirectory(message.getNewDirectory());
            mDetailImageListAdapters.put(message.getNewDirectory(), detailImageAdapter);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(FolderModelChangeMessage message) {
        mIsFolderListLoaded = false;
        loadFolderList();
    }

    private void diffUpdateFolderListAdapter(SectionedFolderListAdapter adapter, boolean fromCacheFirst) {

        ImageService.getInstance()
                .loadFolderList(fromCacheFirst)
                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .compose(workAndShow())
                .subscribe(adapter::diffUpdate,
                        RxUtils::unhandledThrowable);
    }

    static class DefaultImageListDiffCallback extends BaseQuickAdapter.DiffUtilCallback<DefaultImageListAdapter.Item> {

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            DefaultImageListAdapter.Item oldItem = getOldItem(oldItemPosition);
            DefaultImageListAdapter.Item newItem = getNewItem(newItemPosition);
            return oldItem.getFile().equals(newItem.getFile());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            DefaultImageListAdapter.Item oldItem = getOldItem(oldItemPosition);
            DefaultImageListAdapter.Item newItem = getNewItem(newItemPosition);

            boolean isSameSelected = (oldItem.isSelected() == newItem.isSelected());
            if (!isSameSelected) {

                Log.d(TAG, String.format("areContentsTheSame false: old item at %d : %s, new item at %d : %s",
                        oldItemPosition, oldItem.getFile(), newItemPosition, newItem.getFile()));
            }
            return isSameSelected;
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            //实现这个回调,更加精确的进行局部更新,可以用来解决局部刷新Item时,item闪烁问题,但相对来说,实现复杂度增加.
            DefaultImageListAdapter.Item oldItem = getOldItem(oldItemPosition);
            DefaultImageListAdapter.Item newItem = getNewItem(newItemPosition);
            Bundle payload = new Bundle();

            Log.d(TAG, "getChangePayload() called with: oldItemPosition = [" + oldItemPosition + "], newItemPosition = [" + newItemPosition + "]");

            payload.putBoolean(DefaultImageListAdapter.PAYLOAD_SELECTED, newItem.isSelected());

            return payload;
        }
    }

    private void diffUpdateDefaultImageListAdapter(DefaultImageListAdapter adapter, boolean fromCacheFirst, boolean hideRefreshControlWhenFinish) {
        Log.d(TAG, String.format("差量更新图片列表 dir=%s fromCacheFirst=%s", adapter.getDirectory(), fromCacheFirst));

        File dir = adapter.getDirectory();
        if (dir == null) {
            Log.e(TAG, "diffUpdateDefaultImageListAdapter: the adapter didn't have the corresponding directory.");
            return;
        }
        ImageService.getInstance()
                .loadMediaFileList(dir,
                        new LoadMediaFileParam()
                                .setFromCacheFirst(fromCacheFirst)
                                .setLoadMediaInfo(false)
                                .setSortOrder(mSortOrder)
                                .setSortWay(mSortWay)
                )
                .map((mediaFiles) -> mediaFilesToListItems(mediaFiles, adapter.getSelectedFiles()))
                .compose(workAndShow())
                .subscribe((newData) -> {
                    if (hideRefreshControlWhenFinish) {
                        mRefresh.setRefreshing(false);
                    }
                    adapter.diffUpdate(newData);
                }, (throwable) -> {
                    if (hideRefreshControlWhenFinish) {
                        mRefresh.setRefreshing(false);
                    }
                    RxUtils.unhandledThrowable(throwable);
                });
    }

    private void diffUpdateDetailImageAdapter(@NonNull DetailImageAdapter adapter, boolean fromCacheFirst, boolean hideRefreshControl) {

        File dir = adapter.getDirectory();
        if (dir == null) {
            Log.e(TAG, "diffUpdateDetailImageAdapter: adapter didn't have corresponding the directory.");
            return;
        }

        ImageService.getInstance()
                .loadMediaFileList(dir,
                        new LoadMediaFileParam()
                                .setFromCacheFirst(fromCacheFirst)
                                .setLoadMediaInfo(true)
                                .setSortWay(mSortWay)
                                .setSortOrder(mSortOrder)
                )
                .map(this::imagesToDetailListItems)
                .map(items -> {
                    DetailImageAdapter detailImageAdapter = new DetailImageAdapter(R.layout.item_image_detail, items);
                    transferAdapterStatus(adapter, detailImageAdapter);
                    return detailImageAdapter;
                })
                .compose(workAndShow())
                .subscribe(newAdapter -> {
                            if (hideRefreshControl) {
                                mRefresh.setRefreshing(false);
                            }
                            diffUpdateAdapter(adapter, newAdapter);
                            //showDetailImageListAdapter(newAdapter);
                        },
                        (throwable) -> {
                            if (hideRefreshControl) {
                                mRefresh.setRefreshing(false);
                            }
                            RxUtils.unhandledThrowable(throwable);
                        });
    }

    private void transferAdapterStatus(DetailImageAdapter fromAdapter, DetailImageAdapter toAdapter) {
        toAdapter.setDirectory(fromAdapter.getDirectory());
        toAdapter.setFirstVisibleItem(fromAdapter.getFirstVisibleItem());
    }

    private void diffUpdateAdapter(@NonNull DetailImageAdapter oldAdapter, final DetailImageAdapter newAdapter) {

        oldAdapter.diffUpdate(newAdapter.getData());
//        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
//            @Override
//            public int getOldListSize() {
//                return oldAdapter.getItemCount();
//            }
//
//            @Override
//            public int getNewListSize() {
//                return newAdapter.getItemCount();
//            }
//
//            @Override
//            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
//                DetailImageAdapter.Item oldItem = oldAdapter.getItem(oldItemPosition);
//                DetailImageAdapter.Item newItem = newAdapter.getItem(newItemPosition);
//
//                // 两个 item 的文件相同
//                return !(oldItem == null || newItem == null) && oldItem.getFile().equals(newItem.getFile());
//            }
//
//            @Override
//            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
//                DetailImageAdapter.Item oldItem = oldAdapter.getItem(oldItemPosition);
//                DetailImageAdapter.Item newItem = newAdapter.getItem(newItemPosition);
//
//                // no partial update
//                return false;
//            }
//        });
//
//        diffResult.dispatchUpdatesTo(oldAdapter);
    }

    public String getTransitionName(String filename) {
        return "imagelist:item:" + filename.toLowerCase();
    }

    private void startTestDetailActivity(Context context, File file, View view) {

        // Construct an Intent as normal
        Intent intent = ImageViewerActivity.newIntentViewFile(this, file);

        // BEGIN_INCLUDE(start_activity)
        ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,

                // Now we provide a list of Pair items which contain the view we can transitioning
                // from, and the name of the view it is transitioning to, in the launched activity
                new Pair<View, String>(view, getTransitionName(file.getName())));
        // Now we can start the Activity, providing the activity options as a bundle
        ActivityCompat.startActivity(this, intent, activityOptions.toBundle());
        // END_INCLUDE(start_activity)
    }

    private void initListViewHeader() {

        mModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.btn_default: {
                    switchToViewMode(GroupMode.DEFAULT);
                }
                break;
                case R.id.btn_week: {
                    switchToViewMode(GroupMode.WEEK);
                }
                break;
                case R.id.btn_month: {
                    switchToViewMode(GroupMode.MONTH);
                }
                break;
            }
        });
    }

    private void switchToViewMode(GroupMode mode) {
        mGroupMode = mode;
        showDirectoryImages(mCurrentFolder, true);
    }

    @Override
    public void onBackPressed() {

        // // TODO: 2017/9/2 其他图片显示模式的退出选择操作
        boolean isSelectionMode = false;
        switch (mViewMode) {

            case GRID_VIEW: {
                switch (mGroupMode) {

                    case DEFAULT:
                        if (mCurrentImageAdapter != null) {
                            isSelectionMode = mCurrentImageAdapter.isSelectionMode();
                        }
                        break;
                    case DAY:
                        if (mCurrentAdapter != null) {

                        }
                        break;
                    case WEEK:
                        break;
                    case MONTH:
                        break;
                }

            }
            break;
            case LIST_VIEW: {
                if (mCurrentDetailImageAdapter != null) {
                    isSelectionMode = mCurrentDetailImageAdapter.isSelectionMode();
                }
            }
            break;
            case UNKNOWN:
                break;
        }

        if (isSelectionMode) {
            // 退出选择模式
            switch (mViewMode) {

                case GRID_VIEW:
                    switch (mGroupMode) {

                        case DEFAULT: {
                            mCurrentImageAdapter.leaveSelectionMode();
                        }
                        break;
                        case DAY:
                            break;
                        case WEEK:
                            break;
                        case MONTH:
                            break;
                    }
                    break;
                case LIST_VIEW: {
                    mCurrentDetailImageAdapter.leaveSelectionMode();
                }
                break;
                case UNKNOWN:
                    break;
            }
        } else {
            backToLauncher(this);
        }
    }

    public void backToLauncher(Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        context.startActivity(intent);
    }

    private void showFloatingWindow() {
        mStartFloatingIntent = new Intent(MainActivity.this, FloatWindowService.class);
        startService(mStartFloatingIntent);
    }

    private void initToolBar() {
        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);

        mToolbar.setNavigationOnClickListener(v -> finish());

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

        mToolbar.setOnMenuItemClickListener(this::onMainMenuClick);
    }

    private boolean onMainMenuClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.app_bar_recent: {
                showRecentHistoryMenu();
            }
            break;
            case R.id.app_bar_search:
                ToastUtils.toastShort(MainActivity.this, R.string.unimplemented);
                break;
            case R.id.app_bar_setting: {
                MainActivity.this.startActivity(new Intent(MainActivity.this, SettingActivity.class));
            }
            break;
            case R.id.app_bar_view_mode:
                showViewModeMenu();
                break;
            case R.id.app_bar_show_hidden_folder: {

                menuItem.setChecked(!menuItem.isChecked());

                // Save option
                Preference<Boolean> showHiddenFolderPref = UserDataService.getInstance()
                        .getShowHiddenFilePreference();

                showHiddenFolderPref.set(menuItem.isChecked());

                reloadFolderList();
            }
            break;
            case R.id.app_bar_view_hidden_folders:
                ExcludeFolderDialogFragment fragment = ExcludeFolderDialogFragment.newInstance(new ArrayList<String>());
                fragment.show(getSupportFragmentManager(), "Hidden File List Dialog");
                break;
        }

        return true;
    }

    private void showViewModeMenu() {
        PopupMenu popupMenu = new PopupMenu(this, mToolbar, Gravity.RIGHT);
        popupMenu.inflate(R.menu.view_mode_menu);

        ViewMode mode = UserDataService.getInstance()
                .getStringPreference(UserDataService.PREF_KEY_IMAGE_VIEW_MODE, ViewMode::fromString);


        Preference<String> viewModePref = UserDataService.getInstance()
                .getPreferences()
                .getString(UserDataService.PREF_KEY_IMAGE_VIEW_MODE);


        MenuItem itemGridView = popupMenu.getMenu().findItem(R.id.item_grid_view);
        MenuItem itemListView = popupMenu.getMenu().findItem(R.id.item_list_view);
        if (mode == ViewMode.UNKNOWN) {

            setToolbarViewModeIcon(R.drawable.ic_grid_on_black_24px);

            viewModePref.set(UserDataService.VIEW_MODE_GRID_VIEW);

            itemGridView.setChecked(true);

        } else {

            switch (mode) {
                case GRID_VIEW:
                    itemGridView.setChecked(true);
                    break;
                case LIST_VIEW:
                    itemListView.setChecked(true);
                    break;
            }
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.item_grid_view: {
                    Log.d(TAG, "showViewModeMenu: 切换显示网格视图");
                    setToolbarViewModeIcon(R.drawable.ic_grid_on_black_24px);
                    if (mCurrentFolder != null) {
                        showDirectoryImages(mCurrentFolder, false, mViewMode, ViewMode.GRID_VIEW);
                    } else {
                        Log.w(TAG, "showViewModeMenu: mCurrentFolder is null");
                    }

                    viewModePref.set(ViewMode.GRID_VIEW.toString());
                    mViewMode = ViewMode.GRID_VIEW;
                }
                break;
                case R.id.item_list_view: {
                    Log.d(TAG, "showViewModeMenu: 切换显示列表视图");
                    setToolbarViewModeIcon(R.drawable.ic_list_black_24px);

                    if (mCurrentFolder != null) {
                        showDirectoryImages(mCurrentFolder, false, mViewMode, ViewMode.LIST_VIEW);
                    } else {
                        Log.w(TAG, "showViewModeMenu: mCurrentFolder is null");
                    }

                    viewModePref.set(ViewMode.LIST_VIEW.toString());

                    mViewMode = ViewMode.LIST_VIEW;
                }
                break;
            }
            return false;
        });
        popupMenu.show();
    }

    private void setToolbarViewModeIcon(int resId) {
        mToolbar.getMenu().findItem(R.id.app_bar_view_mode).setIcon(resId);
    }

    private void showRecentHistoryMenu() {
        PopupMenu popupMenu = new PopupMenu(this, mToolbar, Gravity.RIGHT);
        Menu menu = popupMenu.getMenu();
        for (int i = 0; i < mRecentHistory.size(); i++) {
            File dir = mRecentHistory.get(i);
            MenuItem menuItem = menu.add(dir.getName());
            menuItem.setIcon(R.drawable.ic_move_to_folder);
            menuItem.setOnMenuItemClickListener(item -> {

                showDirectoryImages(dir, true);
                return true;
            });
        }

        popupMenu.show();
    }

    private void initDrawer() {


        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false);

            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                    mToolbar, R.string.drawer_open, R.string.drawer_close) {

                /** Called when a drawer has settled in a completely closed state. */
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    //getActionBar().setTitle(mTitle);
                    supportInvalidateOptionsMenu();
                    //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                /** Called when a drawer has settled in a completely open state. */
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    //getActionBar().setTitle(mDrawerTitle);
                    supportInvalidateOptionsMenu();
                    //invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            };
            mDrawerToggle.setDrawerIndicatorEnabled(true);

            mDrawerLayout.addDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
        }

        //mTitle = mDrawerTitle = getTitle();


        // DialPad
        mKeypad.getDialpadInputObservable()
                .debounce(300, TimeUnit.MILLISECONDS)
                .subscribe(input -> {
                    if (mEnableT9Filter) {

                        ImageService.getInstance()
                                .loadFolderList(true, input.toString())
                                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                                .compose(RxUtils.workAndShow())
                                .subscribe(newAdapter -> {

                                    RecyclerView.Adapter adapter = mFolderList.getAdapter();
                                    if (adapter instanceof SectionedFolderListAdapter) {
                                        SectionedFolderListAdapter currAdapter = (SectionedFolderListAdapter) adapter;
                                        currAdapter.diffUpdate(newAdapter);
                                    } else {
                                        Log.w(TAG, "initDrawer:  没处理 dialpad 输入变更更新");
                                    }

                                }, RxUtils::unhandledThrowable);
                    }
                });
    }

    @OnClick(R.id.keyboard_switch_layout)
    public void onButtonClickKeypadLayout(View view) {
        switchKeyboard();
    }

    @OnClick(R.id.keyboard_switch_image_view)
    public void onButtonClickKeypadSwitch(View view) {
        switchKeyboard();
    }

    @OnClick(R.id.btn_paste)
    public void onClickToolbarPaste(View view) {

    }

    @OnClick(R.id.btn_move)
    public void onClickToolbarMove(View view) {
        showMoveFileDialog();
    }

    @OnClick(R.id.btn_select_all)
    public void onClickToolbarSelectAll(View view) {

        switch (mViewMode) {

            case GRID_VIEW: {
                if (mCurrentImageAdapter != null) {
                    mCurrentImageAdapter.selectAll();
                }
            }
            break;
            case LIST_VIEW: {
                if (mCurrentDetailImageAdapter != null) {
                    mCurrentDetailImageAdapter.selectAll();
                }
            }
            break;
            case UNKNOWN:
                break;
        }
    }

    @OnClick(R.id.btn_copy)
    public void onClickToolbarCopy(View view) {

    }

    @OnClick(R.id.btn_delete)
    public void onClickToolbarDelete(View view) {
        showRemoveFileDialog();
    }

    private void switchKeyboard() {
        boolean isVisible = mKeypadContainer.getVisibility() == View.VISIBLE;
        int v = isVisible ? View.INVISIBLE : View.VISIBLE;
        if (v == View.VISIBLE && !mEnableT9Filter) {
            mEnableT9Filter = true;
        }

        mKeypadContainer.setVisibility(v);

        if (isVisible) {
            mKeypadSwitch.setBackgroundResource(R.drawable.keyboard_show_selector);
        } else {
            mKeypadSwitch.setBackgroundResource(R.drawable.keyboard_hide_selector);
        }
    }

    private void loadFolderList() {
        if (mIsFolderListLoaded) {
            Log.w(TAG, "loadFolderList: TODO 检查文件列表变化");
        } else {

            mFolderList.setEmptyView(mFolderListEmptyView);

            // 初始化相册文件夹列表
            ImageService.getInstance()
                    .loadFolderList(true)
                    .compose(workAndShow())
                    .doOnError(throwable -> {
                        mFolderListEmptyView.setText(R.string.load_folder_list_failed);
                        Log.w(TAG, "loadFolderList: TODO show error message ");
                    })
                    .subscribe(this::showFolderList, RxUtils::unhandledThrowable);
        }
    }

    private void showExpandableFolderList(FolderModel model) {

        mExpandableFolderAdapter = FolderListAdapterUtils.folderModelToExpandableFolderAdapter(model);

        mExpandableFolderAdapter.setOnItemClickListener((baseQuickAdapter, view, i) ->
                Log.d(TAG, "onItemClick() called with: baseQuickAdapter = [" + baseQuickAdapter + "], view = [" + view + "], i = [" + i + "]"));
        mExpandableFolderAdapter.setOnInteractionListener(item -> showDirectoryImages(item.getFile(), false));

        final GridLayoutManager manager = new GridLayoutManager(this, 1);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mExpandableFolderAdapter.getItemViewType(position) == ExpandableFolderAdapter.TYPE_LEVEL_1 ? 1 : manager.getSpanCount();
            }
        });

        mFolderList.setAdapter(mExpandableFolderAdapter);
        // important! setLayoutManager should be called after setAdapter
        mFolderList.setLayoutManager(manager);
        mExpandableFolderAdapter.expandAll();
    }

    /*
     * 文件夹列表
     */

    private void showFolderList(FolderModel model) {

        mIsFolderListLoaded = true;

        SectionedFolderListAdapter.OnItemClickListener onItemClickListener = new SectionedFolderListAdapter.OnItemClickListener() {
            @Override
            public void onSectionHeaderClick(SectionedFolderListAdapter.Section section, int sectionIndex, int adapterPosition) {
                boolean sectionExpanded = mFolderAdapter.isSectionExpanded(sectionIndex);
                if (sectionExpanded) {
                    mFolderAdapter.collapseSection(sectionIndex);
                } else {
                    mFolderAdapter.expandSection(sectionIndex);
                }
            }

            @Override
            public void onSectionHeaderOptionButtonClick(View v, SectionedFolderListAdapter.Section section, int sectionIndex) {
                Log.d(TAG, "onSectionHeaderOptionButtonClick() called with: section = [" + section + "], sectionIndex = [" + sectionIndex + "]");
                showFolderSectionHeaderOptionPopupMenu(v, section);
            }

            @Override
            public void onItemClick(SectionedFolderListAdapter.Section sectionItem, int section, SectionedFolderListAdapter.Item item, int relativePos) {
                Log.d(TAG, "onItemClick: 显示目录图片 " + item.getFile());
                mDrawerLayout.closeDrawers();
                showDirectoryImages(item.getFile(), true);
            }

            @Override
            public void onItemLongClick(View v, SectionedFolderListAdapter.Section sectionItem, int section, SectionedFolderListAdapter.Item item, int relativePos) {
                showFolderItemContextPopupWindow(v, item);
            }
        };

        // Create adapter
        SectionedFolderListAdapter listAdapter = FolderListAdapterUtils.folderModelToSectionedFolderListAdapter(model);
        listAdapter.setShowHeaderOptionButton(true);
        listAdapter.setOnItemClickListener(onItemClickListener);
        listAdapter.setShowSourceDirBadgeWhenEmpty(false);

        // RecyclerView layout
        mFolderList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // Set adapter
        mFolderList.setAdapter(listAdapter);
        mFolderAdapter = listAdapter;

        // TODO Initial selection status
        if (mCurrentFolder != null) {
            listAdapter.setMoveFileSourceDir(mCurrentFolder);
            listAdapter.scrollToItem(mCurrentFolder);
        }

        // List item click event
        RecyclerItemTouchListener itemTouchListener = new RecyclerItemTouchListener(this,
                mFolderList,
                this::onFolderListItemClick,
                (view, position) -> {
                    //onFolderListItemLongClick(view, position);
                }
        );
        mFolderList.addOnItemTouchListener(itemTouchListener);

        // show firstOf folder's images in activity content field.

        /*
        SectionedFolderListAdapter.Section sectionItem = ListUtils.firstOf(sections);
        if (sectionItem != null) {
            SectionedFolderListAdapter.Item item = ListUtils.firstOf(sectionItem.getItems());
            if (item != null) {
                showDirectoryImages(item.getFile());
            }
        }*/
    }

    private void showFolderSectionHeaderOptionPopupMenu(View v, SectionedFolderListAdapter.Section section) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        popupMenu.inflate(R.menu.folder_header_option_menu);
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.create_folder: {
                    new MaterialDialog.Builder(MainActivity.this)
                            .title(R.string.create_folder)
                            .input(R.string.input_new_folder_name, R.string.new_folder_prefill, false, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                    boolean isValid = input.length() > 1 && input.length() <= 16;
                                    MDButton actionButton = dialog.getActionButton(DialogAction.POSITIVE);
                                    if (actionButton != null) {
                                        actionButton.setClickable(isValid);
                                    }
                                }
                            })
                            .alwaysCallInputCallback()
                            .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI)
                            .positiveText(R.string.create_folder)
                            .onPositive((dialog, which) -> {
                                EditText inputEditText = dialog.getInputEditText();
                                if (inputEditText != null) {
                                    String folderName = inputEditText.getEditableText().toString();
                                    File dir = new File(section.getFile(), folderName);
                                    ImageService.getInstance()
                                            .createFolder(dir)
                                            .compose(workAndShow())
                                            .subscribe(ok -> {
                                                if (ok) {
                                                    ToastUtils.toastLong(MainActivity.this, getString(R.string.created_folder_s, folderName));

                                                }
                                            }, throwable -> {
                                                Log.d(TAG, "新建文件夹失败：" + throwable.getMessage());
                                                ToastUtils.toastLong(MainActivity.this, R.string.create_folder_failed);
                                            });
                                }
                            })
                            .negativeText(R.string.cancel)
                            .show();
                }
                break;
                case R.id.folder_detail:
                    ToastUtils.toastShort(MainActivity.this, R.string.unimplemented);
                    break;
            }
            return false;
        });
        popupMenu.show();
    }

    private void onFolderListItemLongClick(View view, int position) {


        new SectionedListItemClickDispatcher<>(mFolderAdapter)
                .dispatch(position, new SectionedListItemDispatchListener() {
                    @Override
                    public void onHeader(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {

                    }

                    @Override
                    public void onFooter(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {

                    }

                    @Override
                    public void onItem(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {
//                        showFolderItemContextPopupMenu(view, mFolderAdapter.getItem(coord));
                        showFolderItemContextPopupWindow(view, mFolderAdapter.getItem(coord));
                        //showFolderMoveToHereDialog(item.getFile());
                    }
                });
    }

    private void showFolderItemContextPopupWindow(View v, SectionedFolderListAdapter.Item item) {

        if (mFolderItemContextMenu != null) {
            Log.w(TAG, "showFolderItemContextPopupWindow: already shown the popup menu, " + item.getName());
            return;
        }

        com.orhanobut.logger.Logger.d("show context menu : " + item.getName());
        File selectedDir = item.getFile();

        // Window properties
        ColorDrawable backgroundDrawable = new ColorDrawable(getResources().getColor(R.color.gray_98));
        int contentWidth = getResources().getDimensionPixelSize(R.dimen.pop_menu_width);
        int horizontalOffset = (int) -getResources().getDimension(R.dimen.pop_menu_margin);

        // Create menu items
        List<PopupUtils.PopupMenuItem> items = new LinkedList<>();
        // Rename
        items.add(
                new PopupUtils.PopupMenuItem()
                        .setIcon(getResources().getDrawable(R.drawable.ic_mode_edit_black_24px))
                        .setName(getString(R.string.rename))
                        .setAction(() -> {
                            showFolderRenameDialog(item, selectedDir);
                            return true;
                        })
        );

        // Move
        items.add(
                new PopupUtils.PopupMenuItem()
                        .setIcon(getResources().getDrawable(R.drawable.ic_move_to_folder))
                        .setName(getString(R.string.move))
                        .setAction(() -> {
                            ToastUtils.toastShort(MainActivity.this, R.string.unimplemented);
                            return true;
                        })
        );

        // Delete
        items.add(
                new PopupUtils.PopupMenuItem()
                        .setIcon(getResources().getDrawable(R.drawable.ic_delete_black_24px))
                        .setName(getString(R.string.delete))
                        .setAction(() -> {
                            ToastUtils.toastShort(MainActivity.this, R.string.unimplemented);
                            return true;
                        })
        );

        // Hide
        items.add(
                new PopupUtils.PopupMenuItem()
                        .setIcon(getResources().getDrawable(R.drawable.ic_remove_red_eye_black_24px))
                        .setName(getString(R.string.exclude_folder))
                        .setAction(() -> {
                            hideFolder(selectedDir);
                            return true;
                        })
        );

        // 根据是否有正在选中的文件显示“移动至这里”菜单项
        Action0 gridViewModeAction = () -> {
            List<Map.Entry<File, DefaultImageListAdapter>> inSelectionModeAdapter = Stream.of(mImageListAdapters)
                    .filter(value -> value.getValue().isSelectionMode())
                    .toList();
            if (!inSelectionModeAdapter.isEmpty()) {

                items.add(
                        new PopupUtils.PopupMenuItem()
                                .setIcon(null)
                                .setName(getString(R.string.move_selected_images_to_here))
                                .setAction(() -> {
                                    showFolderMoveToHereDialog(selectedDir);
                                    return true;
                                })
                );
            }
        };
        Action0 listViewModeAction = () -> {
            List<Map.Entry<File, DetailImageAdapter>> inSelectionModeAdapter = Stream.of(mDetailImageListAdapters)
                    .filter(value -> value.getValue().isSelectionMode())
                    .toList();
            if (!inSelectionModeAdapter.isEmpty()) {

                items.add(
                        new PopupUtils.PopupMenuItem()
                                .setIcon(null)
                                .setName(getString(R.string.move_selected_images_to_here))
                                .setAction(() -> {
                                    showFolderMoveToHereDialog(selectedDir);
                                    return true;
                                })
                );
            }
        };
        dispatchViewMode(gridViewModeAction, listViewModeAction);

        mFolderItemContextMenu = PopupUtils.build(this, items, v, backgroundDrawable, contentWidth, horizontalOffset, false);
        mFolderItemContextMenu.setOnDismissListener(() -> mFolderItemContextMenu = null);
        mFolderItemContextMenu.show();
    }

    private void dispatchViewMode(Action0 gridAction, Action0 listAction) {
        switch (mViewMode) {

            case GRID_VIEW: {
                if (gridAction != null) {
                    gridAction.accept();
                }
            }
            break;
            case LIST_VIEW:
                if (listAction != null) {
                    listAction.accept();
                }
                break;
            case UNKNOWN:
                break;
        }
    }

    private void showFolderItemContextPopupMenu(View v, SectionedFolderListAdapter.Item item) {

        File selectedDir = item.getFile();

        PopupMenu popupMenu = new PopupMenu(this, v, Gravity.NO_GRAVITY, R.attr.actionOverflowMenuStyle, 0);
        int menuOrder = 0;
        int menuCategory = 0;

        Menu menu = popupMenu.getMenu();
        menu.add(menuCategory, R.id.menu_item_rename,
                menuOrder, getString(R.string.rename_folder_s, selectedDir.getName()));

        menuOrder++;
        menu.add(menuCategory, R.id.menu_item_move,
                menuOrder, getString(R.string.move_folder_s, item.getFile().getName()));

        menuOrder++;
        menu.add(menuCategory, R.id.menu_item_delete,
                menuOrder, getString(R.string.remove_folder_s, item.getFile().getName()));


        menuCategory++;
        menuOrder++;
        menu.add(menuCategory, R.id.menu_item_hide,
                menuOrder, getString(R.string.exclude_folder));

        switch (mViewMode) {

            case GRID_VIEW: {

                List<Map.Entry<File, DefaultImageListAdapter>> inSelectionModeAdapter = Stream.of(mImageListAdapters)
                        .filter(value -> value.getValue().isSelectionMode())
                        .toList();
                if (!inSelectionModeAdapter.isEmpty()) {
                    menuOrder++;
                    menu.add(menuCategory, R.id.menu_item_move_file_to_here,
                            menuOrder, getString(R.string.move_selected_images_to_here));
                }
            }
            break;
            case LIST_VIEW: {

                List<Map.Entry<File, DetailImageAdapter>> inSelectionModeAdapter = Stream.of(mDetailImageListAdapters)
                        .filter(value -> value.getValue().isSelectionMode())
                        .toList();
                if (!inSelectionModeAdapter.isEmpty()) {
                    menuOrder++;
                    menu.add(menuCategory, R.id.menu_item_move_file_to_here,
                            menuOrder, getString(R.string.move_selected_images_to_here));
                }

            }
            break;
            case UNKNOWN:
                break;
        }

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_item_delete: {

                }
                break;
                case R.id.menu_item_hide: {
                    hideFolder(selectedDir);
                }
                break;
                case R.id.menu_item_rename: {
                    showFolderRenameDialog(item, selectedDir);
                }
                break;
                case R.id.menu_item_move: {

                }
                break;
                case R.id.menu_item_move_file_to_here: {
                    showFolderMoveToHereDialog(selectedDir);
                }
                break;
            }
            return false;
        });

        popupMenu.show();
    }

    private void hideFolder(File selectedDir) {
        if (selectedDir == null) {
            Log.w(TAG, "hideFolder: hide null folder.");
            return;
        }

        UserDataService.getInstance()
                .addHiddenFolder(selectedDir)
                .subscribe(ok -> {
                    if (ok) {
                        ImageService.getInstance()
                                .hiddenFolder(selectedDir)
                                .subscribe(removed -> {
                                    if (removed) {
                                        mFolderAdapter.removeFolderItem(selectedDir);
                                    } else {
                                        ToastUtils.toastLong(MainActivity.this, R.string.operation_failed);
                                    }
                                });
                    }
                }, RxUtils::unhandledThrowable)
        ;
    }

    private void showFolderItemContextMenuDialog(View v, SectionedFolderListAdapter.Item item) {


        LinkedList<String> menuItems = new LinkedList<>();
        File selectedDir = item.getFile();

        menuItems.add(getString(R.string.rename_folder_s, selectedDir.getName()));
        menuItems.add(getString(R.string.move_folder_s, item.getFile().getName()));
        menuItems.add(getString(R.string.remove_folder_s, item.getFile().getName()));

        menuItems.add(getString(R.string.exclude_folder));

        // TODO adapter
        switch (mViewMode) {

            case GRID_VIEW: {

                List<Map.Entry<File, DefaultImageListAdapter>> inSelectionModeAdapter = Stream.of(mImageListAdapters)
                        .filter(value -> value.getValue().isSelectionMode())
                        .toList();
                if (!inSelectionModeAdapter.isEmpty()) {
                    menuItems.add(getString(R.string.move_selected_images_to_here));
                }
            }
            break;
            case LIST_VIEW: {

                List<Map.Entry<File, DetailImageAdapter>> inSelectionModeAdapter = Stream.of(mDetailImageListAdapters)
                        .filter(value -> value.getValue().isSelectionMode())
                        .toList();
                if (!inSelectionModeAdapter.isEmpty()) {
                    menuItems.add(getString(R.string.move_selected_images_to_here));
                }

            }
            break;
            case UNKNOWN:
                break;
        }


        new MaterialDialog.Builder(this)
                .title(R.string.folder_operations)
                .items(menuItems)
                .itemsCallback((dialog, itemView, position, text) -> {
                    switch (position) {
                        case 0: { // rename
                            showFolderRenameDialog(item, selectedDir);
                        }
                        break;
                        case 1: // 移动
                            break;
                        case 2: // 删除
                            showFolderDeleteDialog(selectedDir);
                            break;
                        case 3: {
                            showFolderMoveToHereDialog(selectedDir);
                        }
                        break;
                        case 4: {

                        }
                    }
                })
                .show();
    }

    private void showFolderDeleteDialog(File selectedDir) {

        new MaterialDialog.Builder(this)
                .title(R.string.remove_folder)
                .content(getString(R.string.confirm_to_remove_folder_s, selectedDir.getName()))
                .positiveText(R.string.delete)
                .onPositive((dialog, which) -> {
                    ImageService.getInstance()
                            .removeFolder(selectedDir)
                            .compose(workAndShow())
                            .subscribe(aBoolean -> {
                                if (aBoolean) {

                                }
                            }, throwable -> {
                                ToastUtils.toastLong(this, R.string.remove_folder_failed);
                            });
                })
                .negativeText(R.string.cancel)
                .show()
        ;
    }

    private void showFolderRenameDialog(SectionedFolderListAdapter.Item item, File dir) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(this)
                .title(R.string.folder_rename)
                .alwaysCallInputCallback()
                .input(getString(R.string.input_new_directory_name), item.getName(),
                        (dlg, input) -> {
                            boolean isValid = !StringUtils.equals(input, item.getName());
                            Log.d(TAG, " name " + input + " " + item.getName() + " " + isValid);
                            MDButton actionButton = dlg.getActionButton(DialogAction.POSITIVE);
                            actionButton.setEnabled(isValid);
                            actionButton.setClickable(isValid);
                        })
                .inputRange(1, 20)
                .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI)
                .onPositive((dialog1, which) -> {

                    EditText inputEditText = dialog1.getInputEditText();
                    if (inputEditText != null) {
                        Editable editableText = inputEditText.getEditableText();
                        if (editableText != null) {
                            String newDirName = editableText.toString();

                            if (StringUtils.equals(newDirName, dir.getName())) {
                                ToastUtils.toastLong(this, R.string.folder_name_has_no_changes);
                                return;
                            }

                            ImageService.getInstance()
                                    .renameDirectory(dir, newDirName)
                                    .compose(workAndShow())
                                    .subscribe(ok -> {
                                        if (ok) {
                                            ToastUtils.toastLong(this, getString(R.string.already_rename_folder));
                                        }
                                    }, throwable -> {

                                    })
                            ;
                        }
                    }
                }).negativeText(R.string.cancel);
        MaterialDialog dlg = builder.build();
        MDButton actionButton = dlg.getActionButton(DialogAction.POSITIVE);
        actionButton.setEnabled(false);
        actionButton.setClickable(false);

        dlg.show();
    }

    private void onFolderListItemClick(View view, int position) {
        new SectionedListItemClickDispatcher<>(mFolderAdapter)
                .dispatchItemClick(position, (adapter, coord) -> {
                    SectionedFolderListAdapter.Item item = adapter.getItem(coord);

                    if (!ListUtils.isEmpty(item.getConflictFiles())) {

                        ToastUtils.toastShort(MainActivity.this, R.string.unimplemented);
                    } else {
                        Log.d(TAG, "onItemClick: 显示 " + item.getFile());
                        adapter.selectItem(item.getFile());
                        showDirectoryImages(item.getFile(), false);
//                    mDrawerLayout.closeDrawers();
                    }
                });
    }

    private void showFolderMoveToHereDialog(File dir) {

        List<FlatFolderListAdapter.Item> items = null;
        switch (mViewMode) {

            case GRID_VIEW: {
                List<Map.Entry<File, DefaultImageListAdapter>> selectionModeAdapters =
                        Stream.of(mImageListAdapters)
                                .filter(value -> value.getValue().isSelectionMode())
                                .toList();
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
                            .map(entry -> new FlatFolderListAdapter.Item()
                                    .setDirectory(entry.getValue().getDirectory())
                                    .setCount(entry.getValue().getSelectedItemCount())
                                    .setThumbList(
                                            Stream.of(entry.getValue()
                                                    .getSelectedItemUntil(MOVE_FILE_DIALOG_THUMBNEIL_COUNT))
                                                    .map(DefaultImageListAdapter.Item::getFile)
                                                    .toList()
                                    )).toList();
                }
            }
            break;
            case LIST_VIEW: {
                List<Map.Entry<File, DetailImageAdapter>> inSelectionModeAdapters = Stream.of(mDetailImageListAdapters)
                        .filter(value -> value.getValue().isSelectionMode())
                        .toList();
                if (!inSelectionModeAdapters.isEmpty()) {
                    items = Stream.of(inSelectionModeAdapters)
                            .map(entry -> new FlatFolderListAdapter.Item()
                                    .setDirectory(entry.getValue().getDirectory())
                                    .setCount(entry.getValue().getSelectedItemCount())
                                    .setThumbList(
                                            Stream.of(entry.getValue()
                                                    .getSelectedItemUntil(MOVE_FILE_DIALOG_THUMBNEIL_COUNT))
                                                    .map(DetailImageAdapter.Item::getFile)
                                                    .toList()
                                    )).toList();

                }
            }
            break;
            case UNKNOWN:
                break;
        }

        if (items != null) {
            FlatFolderListAdapter selectedFolderListAdapter = new FlatFolderListAdapter(items);
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.selected_files_operation))
                    // second parameter is an optional layout manager. Must be a LinearLayoutManager or GridLayoutManager.
                    .adapter(selectedFolderListAdapter, new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false))
                    .positiveText(R.string.move_to_here)
                    .negativeText(R.string.copy_to_here)
                    .onPositive((dialog, which) -> {
                        if (which == DialogAction.POSITIVE) {
                            List<FlatFolderListAdapter.Item> selectedItem = selectedFolderListAdapter.getSelectedItem();
                            Stream.of(selectedItem)
                                    .map(FlatFolderListAdapter.Item::getDirectory)
                                    .forEach(f -> {
                                        // TODO show progress
                                        moveAdapterSelectedFilesToDir(dir);
                                    });
                        }
                    })
                    .onNegative(((dialog, which) -> {
                        ToastUtils.toastShort(this, R.string.unimplemented);
                    }))
                    .show()
            ;
        }
    }

    /*
     * 文件操作
     */

    private void moveAdapterSelectedFilesToDir(File destDir) {
        ImageService.getInstance()
                .moveFilesToDirectory(destDir,
                        getCurrentSelectedFilePathList(),
                        true,
                        false)
                .compose(workAndShow())
                .subscribe(result -> {
                    switch (mViewMode) {

                        case GRID_VIEW: {
                            mFolderAdapter.updateSelectedCount(
                                    mCurrentImageAdapter.getDirectory(),
                                    mCurrentImageAdapter.getSelectedItemCount());
                        }
                        break;
                        case LIST_VIEW: {
                            mFolderAdapter.updateSelectedCount(
                                    mCurrentDetailImageAdapter.getDirectory(),
                                    mCurrentDetailImageAdapter.getSelectedItemCount());
                        }
                        break;
                        case UNKNOWN:
                            break;
                    }

                    ToastUtils.toastLong(this, getString(R.string.already_moved_d_files, result.getSuccessFiles().size()));
                }, throwable -> {
                    ToastUtils.toastLong(this, R.string.move_files_failed);
                });
    }


    /*
     * 文件夹列表
     */
    private void showFolderModel(FolderModel model) {

        List<SectionFolderListAdapter.SectionItem> sectionItems = new LinkedList<>();

        List<FolderModel.ContainerFolder> containerFolders = model.getContainerFolders();
        for (int i = 0, s = containerFolders.size(); i < s; i++) {
            sectionItems.add(folderInfoToItem(containerFolders.get(i)));
        }

        SectionFolderListAdapter listAdapter = new SectionFolderListAdapter(sectionItems);

        listAdapter.setOnItemClickListener((sectionItem, item) -> {
            mDrawerLayout.closeDrawers();
            showDirectoryImages(item.getFile(), true);
        });
        mFolderList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mFolderList.setAdapter(listAdapter);
        mFolderList.addOnItemTouchListener(
                new RecyclerItemTouchListener(
                        this,
                        mFolderList,
                        (view, position) -> Log.d(TAG, "onItemClick() called with: view = [" + view + "], position = [" + position + "]"),
                        null
                )
        );

        SectionFolderListAdapter.SectionItem sectionItem = ListUtils.firstOf(sectionItems);
        if (sectionItem != null) {
            SectionFolderListAdapter.Item item = ListUtils.firstOf(sectionItem.getItems());
            if (item != null) {
                showDirectoryImages(item.getFile(), true);
            }
        }
//        // show firstOf folder's images in activity content field.
//        if (sectionItems.size() > 0) {
//
//            SectionFolderListAdapter.Section item = sectionItems.get(0);
//            List<SectionFolderListAdapter.Item> items = item.getItems();
//            if (items != null && !items.isEmpty()) {
//                showDirectoryImages(items.get(0).getFile());
//            }
//        }
    }

    private SectionFolderListAdapter.SectionItem folderInfoToItem(FolderModel.ContainerFolder containerFolder) {
        SectionFolderListAdapter.SectionItem sectionItem = new SectionFolderListAdapter.SectionItem();
        sectionItem.setName(containerFolder.getName());
        sectionItem.setFile(containerFolder.getFile());
        sectionItem.setItems(
                Stream.of(containerFolder.getFolders())
                        .map(item -> new SectionFolderListAdapter.Item()
                                .setFile(item.getFile())
                                .setName(item.getName())
                                .setCount(item.getCount())
                                .setThumbList(item.getThumbList())
                        )
                        .toList()
        );
        return sectionItem;
    }

    @Deprecated
    private void showFolders(List<ImageFolder> imageFolders) {

        List<FolderListAdapter.Item> items = Stream.of(imageFolders)
                .map(FolderListAdapterUtils::imageFolderToFolderListAdapterItem)
                .toList();
        showFolderItems(items);
    }

    @Deprecated
    @MainThread
    private void showFolderItems(List<FolderListAdapter.Item> items) {

        mFolderListAdapter = new FolderListAdapter(items);
        mFolderListAdapter.setOnItemEventListener(item -> {

            mDrawerLayout.closeDrawers();

            showDirectoryImages(item.getDirectory(), true);

        });
        mFolderList.addItemDecoration(new SectionDecoration(this, new SectionDecoration.DecorationCallback() {
            @Override
            public long getGroupId(int position) {
                return ImageService.getInstance().getSectionForPosition(position);
//                return mFolderListAdapter.getItemCount();
            }

            @Override
            public String getGroupFirstLine(int position) {
                return ImageService.getInstance().getSectionFileName(position);
            }
        }));
        mFolderList.setAdapter(mFolderListAdapter);

//        // show firstOf folder's images in activity content field.
//        if (items != null && items.size() > 0) {
//
//            FolderListAdapter.Item item = items.get(0);
//            showDirectoryImages(item.getDirectory());
//        }
    }

    /*
     * Actionbar
     */

    public void updateActionBarTitle(String name) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(name);
        }
    }

    /*
     * Image list
     */


    @MainThread
    private void showDirectoryImages(File directory, boolean updateFolderList) {
        showDirectoryImages(directory, updateFolderList, mViewMode, mViewMode);
    }

    @MainThread
    private void showDirectoryImages(File directory, boolean updateFolderList, ViewMode fromViewMode, ViewMode toViewMode) {

        Log.d(TAG, "showDirectoryImages() called with: directory = [" + directory + "], updateFolderList = [" + updateFolderList + "], fromViewMode = [" + fromViewMode + "], toViewMode = [" + toViewMode + "]");


        if (directory == null) {
            Log.w(TAG, "show empty folder");
            return;
        }

        switch (fromViewMode) {

            case GRID_VIEW:
                if (toViewMode == ViewMode.GRID_VIEW) {

                    if (mCurrentImageAdapter != null) {
                        if (mCurrentImageAdapter.getDirectory().equals(directory)) {
                            Log.w(TAG, "showDirectoryImages: 目录已经显示在 " + toViewMode + " 模式下");
                            return;
                        }
                    }
                }
                break;
            case LIST_VIEW:
                if (toViewMode == ViewMode.LIST_VIEW
                        && mCurrentDetailImageAdapter != null
                        && mCurrentDetailImageAdapter.getDirectory().equals(directory)) {

                    Log.w(TAG, "showDirectoryImages: 目录已经显示在 " + toViewMode + " 模式下");
                    return;
                }
                break;
            case UNKNOWN:
                break;
        }

        mCurrentFolder = directory;

        // Update folder list select status
        if (updateFolderList && mFolderAdapter != null) {
            mFolderAdapter.selectItem(directory);
        }

        // Action bar title
        updateActionBarTitle(directory.getName());

        switch (toViewMode) {
            case GRID_VIEW: {
                // Image list based on view mode
                if (mGroupMode == GroupMode.DEFAULT) {
                    showDefaultImageList(directory);
                } else {
                    showSectionedImageList(directory, mGroupMode);
                }
            }
            break;
            case LIST_VIEW: {
                showDetailImageList(directory);
            }
            break;
            default:
                Log.w(TAG, "showDirectoryImages: 没有显示图片目录，未知视图模式：" + toViewMode);
                break;
        }
    }

    private void showDetailImageList(File directory) {
        DetailImageAdapter adapter = mDetailImageListAdapters.get(directory);
        if (adapter == null) {

            ImageService.getInstance()
                    .loadMediaFileList(directory,
                            new LoadMediaFileParam()
                                    .setFromCacheFirst(true)
                                    .setLoadMediaInfo(true)
                                    .setSortWay(mSortWay)
                                    .setSortOrder(mSortOrder)
                    )
                    .map( this::imagesToDetailListItems)
                    .map(items -> createDetailImageAdapter(directory, items))
                    // cache adapter
                    .doOnNext(detailImageAdapter -> mDetailImageListAdapters.put(directory, detailImageAdapter))
                    .compose(workAndShow())
                    .subscribe(this::showDetailImageListAdapter, RxUtils::unhandledThrowable);
        } else {
            showDetailImageListAdapter(adapter);
        }
    }

    @NonNull
    private DetailImageAdapter.Item mediaFileToDetailItem(MediaFile mediaFile) {
        DetailImageAdapter.Item item = new DetailImageAdapter.Item();

        item.setFile(mediaFile.getFile());
        item.setDate(mediaFile.getDate());
        item.setVideoDuration(mediaFile.getVideoDuration());
        item.setFileSize(mediaFile.getFileSize());
        item.setMediaResolution(mediaFile.getMediaResolution());
        return item;
    }

    @NonNull
    private DetailImageAdapter createDetailImageAdapter(File directory, List<DetailImageAdapter.Item> items) {
        DetailImageAdapter listAdapter = new DetailImageAdapter(R.layout.item_image_detail, items);
        listAdapter.setDirectory(directory);

        listAdapter.setDetectMoves(true);

        return listAdapter;
    }

    private void showSectionedImageList(File directory, GroupMode groupMode) {

        if (groupMode == GroupMode.DEFAULT) {
            throw new IllegalStateException("Group mode shouldn't be 'DEFAULT'.");
        }

        Log.d(TAG, "showSectionedImageList() called with: directory = [" + directory + "], groupMode = [" + groupMode + "]");

        {
            SectionedImageListAdapter listAdapter = getSectionedImageListAdapter(directory, mGroupMode);
            if (listAdapter != null) {
                Log.d(TAG, "show cached sectioned list adapter : " + directory.getName());
                showSectionedImageList(listAdapter);
            } else {
                ImageService.getInstance()
                        .loadImageGroupList(directory, groupMode, true, SortWay.DATE, SortOrder.DESC)
                        .map(this::sortImageGroupByViewMode)
                        .map((sectionList) -> {
                            SectionedImageListAdapter adapter = new SectionedImageListAdapter(sectionList);
                            adapter.setDirectory(directory);
                            adapter.setGroupMode(groupMode);
                            return adapter;
                        })
                        .compose(workAndShow())
                        .subscribe(adapter -> {
                            putGroupMode(directory, groupMode, adapter);
                            Log.d(TAG, "show newly sectioned list adapter : " + directory.getName());
                            showSectionedImageList(adapter);
                        }, throwable -> {
                            ToastUtils.toastLong(MainActivity.this, R.string.load_pictures_failed);
                        });

//            loadAdapterSections(directory, groupMode)
//                    .map(SectionedImageListAdapter::new)
//                    .subscribeOn(Schedulers.io())
//                    .doOnNext(adapter -> {
//                        putGroupMode(directory, groupMode, adapter);
//                    })
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(this::showSectionedImageList);
            }
        }
    }

    private List<SectionedImageListAdapter.Section> sortImageGroupByViewMode(List<ImageGroup> imageGroups) {
        // 项排序
        SortWay way = SortWay.DATE;
        SortOrder order = SortOrder.DESC;
        if (mViewMode == ViewMode.LIST_VIEW) {
            way = SortWay.NAME;
            order = SortOrder.ASC;
        }
        return imageGroupsToAdapter(imageGroups, way, order);
    }

    private void putGroupMode(File directory, GroupMode groupMode, SectionedImageListAdapter adapter) {
        switch (groupMode) {

            case DEFAULT:
                break;
            case DAY: {
                mDaySectionedImageListAdapters.put(directory.getAbsolutePath(), adapter);
            }
            break;
            case WEEK: {
                mWeekSectionedImageListAdapters.put(directory.getAbsolutePath(), adapter);
            }
            break;
            case MONTH: {
                mMonthSectionedImageListAdapters.put(directory.getAbsolutePath(), adapter);
            }
            break;
        }
    }

    private List<SectionedImageListAdapter.Section> imageGroupsToAdapter(List<ImageGroup> imageGroups, SortWay sortWay, SortOrder order) {

        List<SectionedImageListAdapter.Section> sections = new LinkedList<>();
        for (int i = 0, imageGroupsSize = imageGroups.size(); i < imageGroupsSize; i++) {
            ImageGroup imageGroup = imageGroups.get(i);
            sections.add(imageGroupToAdapterSection(imageGroup, sortWay, order));
        }


        return sections;
    }

    private SectionedImageListAdapter.Section imageGroupToAdapterSection(ImageGroup imageGroup, SortWay sortWay, SortOrder sortOrder) {
        SectionedImageListAdapter.Section section = new SectionedImageListAdapter.Section();

        section.setStartDate(imageGroup.getStartDate());
        section.setEndDate(imageGroup.getEndDate());

        List<MediaFile> mediaFiles = imageGroup.getMediaFiles();
        if (mediaFiles != null) {
            List<SectionedImageListAdapter.Item> items = new LinkedList<>();
            for (int i = 0, imagesSize = mediaFiles.size(); i < imagesSize; i++) {
                MediaFile mediaFile = mediaFiles.get(i);
                items.add(new SectionedImageListAdapter.Item()
                        .setFile(mediaFile.getFile())
                        .setDate(mediaFile.getDate())
                );
            }

            Comparator<SectionedImageListAdapter.Item> nameAscComparator = (o1, o2)
                    -> o1.getFile().getName().compareTo(o2.getFile().getName());
            Comparator<SectionedImageListAdapter.Item> nameDescComparator = (o1, o2)
                    -> o2.getFile().getName().compareTo(o1.getFile().getName());

            Comparator<SectionedImageListAdapter.Item> dateAscComparator = (o1, o2)
                    -> o1.getDate().compareTo(o2.getDate());

            Comparator<SectionedImageListAdapter.Item> dateDescComparator = (o1, o2)
                    -> o2.getDate().compareTo(o1.getDate());


            List<SectionedImageListAdapter.Item> sortedItems;
            if (sortWay == SortWay.NAME) {
                if (sortOrder == SortOrder.ASC) {
                    sortedItems = Stream.of(items)
                            .sorted(nameAscComparator)
                            .toList();
                } else {
                    sortedItems = Stream.of(items)
                            .sorted(nameDescComparator)
                            .toList();
                }

                section.setItems(sortedItems);
            } else if (sortWay == SortWay.SIZE) {

                sortedItems = items;

            } else if (sortWay == SortWay.DATE) {
                if (sortOrder == SortOrder.ASC) {
                    sortedItems = Stream.of(items)
                            .sorted(dateAscComparator)
                            .toList();
                } else {
                    sortedItems = Stream.of(items)
                            .sorted(dateDescComparator)
                            .toList();
                }
            } else {
                sortedItems = items;
            }

            section.setItems(sortedItems);
        }
        section.setDescription(getSectionDescription(section.getStartDate(), MainActivity.this.mGroupMode));
        return section;
    }

    @Nullable
    private SectionedImageListAdapter getSectionedImageListAdapter(File directory, GroupMode groupMode) {
        SectionedImageListAdapter listAdapter = null;

        switch (groupMode) {

            case DEFAULT: {
                throw new IllegalStateException("groupMode parameter shouldn't be 'DEFAULT'.");
            }
            case DAY:
                listAdapter = mDaySectionedImageListAdapters.get(directory.getAbsolutePath());
                break;
            case WEEK: {
                listAdapter = mWeekSectionedImageListAdapters.get(directory.getAbsolutePath());
            }
            break;
            case MONTH: {
                listAdapter = mMonthSectionedImageListAdapters.get(directory.getAbsolutePath());
            }
            break;
        }
        return listAdapter;
    }

    private Observable<LinkedList<SectionedImageListAdapter.Section>> loadAdapterSections(File directory, GroupMode mode) {
        return Observable.create(e -> {

            List<File> imageFiles = ImageService.getInstance().listMediaFiles(directory);
            LinkedList<SectionedImageListAdapter.Section> sections = Stream.of(imageFiles)
                    .groupBy(file -> {
                        long d = file.lastModified();
                        Date date = new Date(d);

                        Log.d(TAG, "loadAdapterSections() called with: directory = [" + directory + "], mode = [" + mode + "]");

                        switch (mode) {
                            case DAY:
                                return DateTimeUtils.daysBeforeToday(date);
                            case WEEK:
                                return DateTimeUtils.weeksBeforeCurrentWeek(date);
                            case MONTH:
                                return DateTimeUtils.monthsBeforeCurrentMonth(date);
                            default:
                                return DateTimeUtils.daysBeforeToday(date);
                        }
                    })
                    .sorted((o1, o2) -> Integer.compare(o1.getKey(), o2.getKey()))
                    .collect(new Collector<Map.Entry<Integer, List<File>>,
                            LinkedList<SectionedImageListAdapter.Section>,
                            LinkedList<SectionedImageListAdapter.Section>>() {
                        @Override
                        public Supplier<LinkedList<SectionedImageListAdapter.Section>> supplier() {
                            return LinkedList::new;
                        }

                        @Override
                        public BiConsumer<LinkedList<SectionedImageListAdapter.Section>,
                                Map.Entry<Integer, List<File>>> accumulator() {
                            return (sections, entry) -> {

                                SectionedImageListAdapter.Section section = new SectionedImageListAdapter.Section();
                                List<File> files = entry.getValue();
                                List<SectionedImageListAdapter.Item> itemList = Stream.of(files)
                                        .map(file -> new SectionedImageListAdapter.Item()
                                                .setFile(file)
                                                .setDate(new Date(file.lastModified())))
                                        .toList();
                                SectionedImageListAdapter.Item firstItem = ListUtils.firstOf(itemList);
                                SectionedImageListAdapter.Item lastItem = ListUtils.lastOf(itemList);

                                section.setItems(itemList);
                                section.setStartDate(firstItem.getDate());
                                section.setEndDate(lastItem.getDate());
                                section.setDescription(getSectionDescription(firstItem.getDate(), MainActivity.this.mGroupMode));

                                sections.add(section);
                            };
                        }

                        @Override
                        public Function<LinkedList<SectionedImageListAdapter.Section>,
                                LinkedList<SectionedImageListAdapter.Section>> finisher() {
                            return sections -> sections;
                        }
                    });

            e.onNext(sections);
            e.onComplete();
        });
    }

    private String getSectionDescription(Date date, GroupMode groupMode) {

        switch (groupMode) {

            case DEFAULT:
                return "";
            case DAY:
                return DateTimeUtils.friendlyDayDescription(getResources(), date);
            case WEEK:
                return DateTimeUtils.friendlyWeekDescription(getResources(), date);
            case MONTH:
                return DateTimeUtils.friendlyMonthDescription(getResources(), date);
            default:
                return "";
        }
    }

    private void showSectionedImageList(SectionedImageListAdapter listAdapter) {
        mCurrentAdapter = listAdapter;

        mGridLayout = new GridLayoutManager(this, mSpanCount, GridLayoutManager.VERTICAL, false);
        mImageList.setLayoutManager(mGridLayout);
        listAdapter.setLayoutManager(mGridLayout);
        if (mImageList.getAdapter() == null) {
            Log.d(TAG, "setAdapter() called with: listAdapter = [" + listAdapter + "]");
            mImageList.setAdapter(listAdapter);
        } else {
            Log.d(TAG, "swapAdapter() called with: listAdapter = [" + listAdapter + "]");
            mImageList.swapAdapter(listAdapter, false);
        }
        mImageList.addOnItemTouchListener(
                new RecyclerItemTouchListener(
                        this,
                        mImageList,
                        (view, position) -> {
                            if (listAdapter.isHeader(position) || listAdapter.isFooter(position)) {
                                return;
                            } else {
                                ItemCoord relativePosition = listAdapter.getRelativePosition(position);
                                listAdapter.onClickItem(relativePosition);
                            }
                        },
                        null
                ));
    }

    private void showDefaultImageList(File directory) {
        DefaultImageListAdapter listAdapter = mImageListAdapters.get(directory);
        if (listAdapter != null) {
            com.orhanobut.logger.Logger.d("showDefaultImageList 切换显示目录 " + directory);
            showGridImageListAdapter(listAdapter);

        } else {

            com.orhanobut.logger.Logger.d("showDefaultImageList 加载并显示目录 " + directory);

            ImageService.getInstance()
                    .loadMediaFileList(directory,
                            new LoadMediaFileParam()
                                    .setFromCacheFirst(true)
                                    .setLoadMediaInfo(false)
                                    .setSortOrder(mSortOrder)
                                    .setSortWay(mSortWay)
                    )
                    .compose(workAndShow())
                    .map(this::mediaFilesToListItems)
                    .map(items -> itemsToAdapter(directory, items))
                    .doOnNext(adapter -> cacheImageAdapter(directory, adapter))
                    .doOnError(throwable -> Toast.makeText(this, R.string.load_pictures_failed, Toast.LENGTH_SHORT).show())
                    .subscribe(this::showGridImageListAdapter, RxUtils::unhandledThrowable);

        }
    }

    private void cacheImageAdapter(File directory, DefaultImageListAdapter adapter) {
        mImageListAdapters.put(directory, adapter);
    }

    private DefaultImageListAdapter itemsToAdapter(File directory, List<DefaultImageListAdapter.Item> items) {
        DefaultImageListAdapter adapter = createImageListAdapter(items);
        adapter.setDirectory(directory);
        adapter.setDiffUtilCallback(new DefaultImageListDiffCallback());
        adapter.setDetectMoves(true);
        return adapter;
    }

    private List<DefaultImageListAdapter.Item> mediaFilesToListItems(final List<MediaFile> mediaFiles) {
        return mediaFilesToListItems(mediaFiles, null);
    }

    /**
     * @param selectedFiles 来自当前 adapter 中的被选中文件列表
     */
    private List<DefaultImageListAdapter.Item> mediaFilesToListItems(final List<MediaFile> mediaFiles, final List<File> selectedFiles) {
        return Stream.of(mediaFiles)
                .map(image -> {

                    DefaultImageListAdapter.Item item = new DefaultImageListAdapter.Item();
                    item.setFile(image.getFile());
                    if (selectedFiles != null) {
                        if (selectedFiles.contains(item.getFile())) {
                            item.setSelected(true);
                        }
                    }
                    return item;
                })
                .toList();
    }

    private List<DetailImageAdapter.Item> imagesToDetailListItems(List<MediaFile> mediaFiles) {
        return Stream.of(mediaFiles).map(this::mediaFileToDetailItem).toList();
    }

    private void showGridImageListAdapter(DefaultImageListAdapter adapter) {

        // Save recent accessed folder
        addRecentHistoryRecord(adapter.getDirectory());

        // Update action bar title for new shown adapter
        updateActionBarTitle(adapter.getDirectory().getName());
        updateActionBarTitleCount(adapter.getSelectedItemCount(), adapter.getDirectory(), adapter.getItemCount());

        // Setup adapter
//        adapter.setOnInteractionListener(this);

        // Remember the current adapter's corresponding fist visible item,
        // We will restore it when user switch back to current adapter
        if (mCurrentImageAdapter != null) {
            RecyclerView.LayoutManager layoutManager = mImageList.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                int i = ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
                mCurrentImageAdapter.setFirstVisibleItem(i);
            } else if (layoutManager instanceof LinearLayoutManager) {
                int i = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                mCurrentImageAdapter.setFirstVisibleItem(i);
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                int[] a = new int[100];
                ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(a);
                if (a.length > 0) {
                    mCurrentImageAdapter.setFirstVisibleItem(a[0]);
                }
            }
        }
        mCurrentImageAdapter = adapter;

        // Sync data with folder list adapter
        if (mFolderAdapter != null) {
            mFolderAdapter.setMoveFileSourceDir(adapter.getDirectory());
        }

        // RecyclerView Layout
        mGridLayoutManager = new GridLayoutManager(this, mSpanCount, GridLayoutManager.VERTICAL, false);
        mImageList.setLayoutManager(mGridLayoutManager);

        setImageToolbarVisible(mCurrentImageAdapter.isSelectionMode());

        // Item event
        if (mImageList.getAdapter() == null) {
            mImageList.setAdapter(adapter);
        } else {
            mImageList.setAdapter(adapter);
        }
        try {
            mCurrentImageAdapter.bindToRecyclerView(mImageList);
        } catch (Throwable t) {
        }

        int firstVisibleItem = adapter.getFirstVisibleItem();
        if (firstVisibleItem != RecyclerView.NO_POSITION) {
            mImageList.scrollToPosition(firstVisibleItem);
        }
    }

    private void showDetailImageListAdapter(DetailImageAdapter adapter) {

        // Save recent accessed folder
        addRecentHistoryRecord(adapter.getDirectory());

        // Update action bar title for new shown adapter
        updateActionBarTitle(adapter.getDirectory().getName());
        updateActionBarTitleCount(adapter.getSelectedItemCount(), adapter.getDirectory(), adapter.getItemCount());

        // Setup adapter
        BaseSelectableAdapter.OnInteractionListener<BaseSelectableAdapter, DetailImageAdapter.Item> onInteractionListener = new DetailImageAdapter.OnInteractionListener<
                BaseSelectableAdapter,
                DetailImageAdapter.Item>() {
            @Override
            public void onItemLongClick(DetailImageAdapter.Item item, int position) {
                Log.d(TAG, "onItemLongClick: " + item);
                if (!item.isSelected()) {
                    //mImageList.setDragSelectActive(true, position);
                }
            }

            @Override
            public void onItemCheckedChanged(DetailImageAdapter.Item item) {

            }

            @Override
            public void onItemClicked(DetailImageAdapter.Item item, View view, int position) {
                onClickItem(view, position, item.getFile());
            }

            @Override
            public void onSelectionModeChange(BaseSelectableAdapter baseAdapter, boolean isSelectionMode) {
                DetailImageAdapter adapter = (DetailImageAdapter) baseAdapter;
                Log.d(TAG, "onSelectionModeChange: isSelectionMode " + isSelectionMode);
                if (isSelectionMode) {

                    changeFloatingCount(FloatWindowService.MSG_INCREASE);


                    setImageToolbarVisible(true);
//            mFloatingToolbar.showContextMenu();
                } else {
                    changeFloatingCount(FloatWindowService.MSG_DECREASE);

                    setImageToolbarVisible(false);

//            mFloatingToolbar.dismissPopupMenus();
                }

                updateActionBarTitleCount(isSelectionMode ? baseAdapter.getSelectedItemCount() : 0, adapter.getDirectory(), baseAdapter.getItemCount());
            }

            @Override
            public void onSelectedItemLongClick(View view, int position, DetailImageAdapter.Item item) {
                Log.d(TAG, "onSelectedItemLongClick() called with: view = [" + view + "], position = [" + position + "], item = [" + item + "]");
            }

            @Override
            public void onSelectedCountChange(BaseSelectableAdapter baseAdapter, int selectedCount) {
                Log.d(TAG, "onSelectedCountChange() called with: baseAdapter = [" + baseAdapter + "], selectedCount = [" + selectedCount + "]");

                DetailImageAdapter adapter = (DetailImageAdapter) baseAdapter;
                updateActionBarTitleCount(selectedCount, adapter.getDirectory(), baseAdapter.getItemCount());
            }
        };

        adapter.setOnInteractionListener(onInteractionListener);


        // 当前显示的图片位置
        // TODO 更严谨的计算显示位置
        if (mCurrentDetailImageAdapter != null) {
            RecyclerView.LayoutManager layoutManager = mImageList.getLayoutManager();
            int i;
            if (layoutManager instanceof GridLayoutManager) {
                i = ((GridLayoutManager) layoutManager).findFirstVisibleItemPosition();
                mCurrentDetailImageAdapter.setFirstVisibleItem(i);
            } else if (layoutManager instanceof LinearLayoutManager) {
                i = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
                mCurrentDetailImageAdapter.setFirstVisibleItem(i);
            }
        }
        mCurrentDetailImageAdapter = adapter;

        // Folder List : Sync data with folder list
        if (mFolderAdapter != null) {
            mFolderAdapter.setMoveFileSourceDir(adapter.getDirectory());
        }

        // RecyclerView Layout
        mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mImageList.setLayoutManager(mLinearLayoutManager);
        try {
            mCurrentDetailImageAdapter.bindToRecyclerView(mImageList);
        } catch (Throwable t) {
        }

        if (mCurrentDetailImageAdapter != null) {
            setImageToolbarVisible(mCurrentDetailImageAdapter.isSelectionMode());
        }

        // Item event
        if (mImageList.getAdapter() == null) {
            mImageList.setAdapter(adapter);
        } else {
            mImageList.setAdapter(adapter);
        }

        int firstVisibleItem = adapter.getFirstVisibleItem();
        if (firstVisibleItem != RecyclerView.NO_POSITION) {
            mImageList.scrollToPosition(firstVisibleItem);
        }
    }

    private void setImageToolbarVisible(boolean visible) {
//        mFloatingToolbar.setVisibility(View.VISIBLE);

        int deBounceTime = 1000;
        switch (mViewMode) {

            case GRID_VIEW:

                if (visible) {

                    if (mUpdateImageConflictFileDisposable == null) {
                        mUpdateImageConflictFileDisposable = getImageAdapterSelectCountChangeRelay()
                                .debounce(deBounceTime, TimeUnit.MILLISECONDS)
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(defaultImageListAdapter -> {
                                    List<File> sourceFiles = Stream.of(defaultImageListAdapter.getSelectedItems())
                                            .map(DefaultImageListAdapter.Item::getFile)
                                            .toList();
                                    updateFolderListConflictItems(sourceFiles);
                                }, RxUtils::unhandledThrowable);
                    }
                } else {
                    if (mFolderAdapter != null) {
                        mFolderAdapter.updateConflictFiles(new LinkedHashMap<>());
                    }
//                    if (mUpdateImageConflictFileDisposable != null) {
//                        mUpdateImageConflictFileDisposable.dispose();
//                    }
                }
                break;
            case LIST_VIEW:
                if (visible) {

                    if (mUpdateDetailImageListConflictDisposable == null) {
                        mUpdateDetailImageListConflictDisposable = getDetailAdapterSelectCountChangeRelay()
                                .debounce(deBounceTime, TimeUnit.MILLISECONDS)
                                .subscribeOn(AndroidSchedulers.mainThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(detailImageAdapter -> {
                                    List<File> sourceFiles = Stream.of(detailImageAdapter.getSelectedItems())
                                            .map(DetailImageAdapter.Item::getFile)
                                            .toList();
                                    updateFolderListConflictItems(sourceFiles);
                                }, RxUtils::unhandledThrowable);
                    }
                } else {
                    if (mFolderAdapter != null) {
                        mFolderAdapter.updateConflictFiles(new LinkedHashMap<>());
                    }
                }
                break;
            case UNKNOWN:
                break;
        }

        mImageToolbar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void clearFolderListConflictItems() {
        mFolderAdapter.updateConflictFiles(new LinkedHashMap<>());
    }

    private void updateFolderListConflictItems(List<File> sourceFiles) {

        ImageService.getInstance()
                .detectFileExistence(sourceFiles)
                .compose(workAndShow())
                .subscribe(detectFileExistenceResult -> {
                    mFolderAdapter.updateConflictFiles(detectFileExistenceResult.getExistedFiles());
                }, RxUtils::unhandledThrowable);
    }

    private void onClickItem(View view, int position, File mediaFile) {
        try {
            String absolutePath = mediaFile.getAbsolutePath();
            if (PathUtils.isVideoFile(absolutePath)) {
                Uri videoUri = Uri.parse(absolutePath);
                Intent intent = new Intent(Intent.ACTION_VIEW, videoUri);
                intent.setDataAndType(videoUri, "video/" + FilenameUtils.getExtension(absolutePath));
                startActivity(intent);
            } else {
                startImageViewerActivity(mediaFile.getParentFile(), view, position, mediaFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "image list item click exception : " + e.getMessage());
        }
    }

    private void addRecentHistoryRecord(File directory) {

        UserDataService.getInstance()
                .addOpenFolderRecentRecord(directory)
                .compose(workAndShow())
                .subscribe(aBoolean -> {
                    int i1 = org.apache.commons.collections4.ListUtils.indexOf(mRecentHistory, file -> file.equals(directory));
                    if (i1 != -1) {
                        mRecentHistory.remove(i1);
                    }
                    mRecentHistory.add(0, directory);

                }, RxUtils::unhandledThrowable);
    }

    private void reloadFolderList() {
        if (mFolderAdapter != null) {
            diffUpdateFolderListAdapter(mFolderAdapter, false);
        } else {
            ImageService.getInstance()
                    .loadFolderList(false)
                    .compose(workAndShow())
                    .subscribe(this::showFolderList, RxUtils::unhandledThrowable);
        }
    }

    private void reloadImageList(boolean hideRefreshControlWhenFinish) {

        if (mIsImageListLoaded) {

            // Reload image list
            if (mViewMode == ViewMode.GRID_VIEW) {

                if (mCurrentImageAdapter != null) {

                    updateFolderListItemThumbnailList(mCurrentImageAdapter.getDirectory());

                    diffUpdateDefaultImageListAdapter(mCurrentImageAdapter, false, hideRefreshControlWhenFinish);
                }
            } else if (mViewMode == ViewMode.LIST_VIEW) {

                if (mCurrentDetailImageAdapter != null) {
                    updateFolderListItemThumbnailList(mCurrentDetailImageAdapter.getDirectory());

                    diffUpdateDetailImageAdapter(mCurrentDetailImageAdapter, false, hideRefreshControlWhenFinish);
                }
            } else {
                Log.e(TAG, "reloadImageList: unknown handled mode : " + mViewMode);
            }

        } else {
            mSpanCount = DEFAULT_IMAGE_LIST_COLLUMN_COUNT; // columns

            // EmptyView
            mImageList.setEmptyView(mEmptyView);

            // Item spacing
//            LayoutMarginDecoration marginDecoration =
//                    new LayoutMarginDecoration(mSpanCount, getResources().getDimensionPixelSize(R.dimen.image_list_item_space));
//
//            mImageList.addItemDecoration(marginDecoration);

            Observable<ViewMode> viewModeObservable = Observable.just(1)
                    .map(integer -> UserDataService.getInstance()
                            .getStringPreference(UserDataService.PREF_KEY_IMAGE_VIEW_MODE, ViewMode::fromString));

            // Load recent history and show the first one

            // 把最近文件列表和视图模式加载后合并
            Log.d(TAG, "reloadImageList: ");

            UserDataService.getInstance().loadInitialPreference(true)
                    .compose(workAndShow())
                    .doOnNext(userInitialPreferences -> Log.d(TAG, "Loaded user initial preference : " + userInitialPreferences))
                    .subscribe(userInitialPreferences -> {
                        mRecentHistory = Stream.of(userInitialPreferences.getRecentRecords()).map(r -> new File(r.getFilePath())).toList();
                        mViewMode = userInitialPreferences.getViewMode();
                        mSortWay = userInitialPreferences.getSortWay();
                        mSortOrder = userInitialPreferences.getSortOrder();

                        if (ListUtils.isEmpty(mRecentHistory)) {
                            Log.w(TAG, "reloadImageList: 最近访问目录列表为空，加载相机相册");
                            showDirectoryImages(SystemUtils.getCameraDir(), true);
                        } else {
                            File recentDir = ListUtils.firstOf(mRecentHistory);

                            Log.d(TAG, "reloadImageList: 显示最近显示目录 : " + recentDir);
                            showDirectoryImages(recentDir, true);
                        }
                    }, throwable -> {
                        ToastUtils.toastShort(this, R.string.failed_to_load_preference);
                    });

//            Observable.combineLatest(
//                    UserDataService.getInstance().loadRecentOpenFolders(true),
//                    viewModeObservable,
//                    Pair::new)
//                    .compose(workAndShow())
//                    .subscribe(listViewModePair -> {
//                                List<RecentRecord> recentRecords = listViewModePair.first;
//
//                                Log.d(TAG, "reloadImageList: loaded recent history and view mode : "
//                                        + recentRecords + " " + listViewModePair.second);
//                                mRecentHistory = Stream.of(recentRecords).map(r -> new File(r.getFilePath())).toList();
//                                mViewMode = listViewModePair.second;
//
//
//                            },
//                            RxUtils::unhandledThrowable);

//            UserDataService.getInstance()
//                    .loadRecentOpenFolders(true)
//                    .compose(workAndShow())
//                    .subscribe(recentRecords -> {
//
//                        mRecentHistory = Stream.of(recentRecords).map(r -> new File(r.getFilePath())).toList();
//
//                        mViewMode = UserDataService.getInstance()
//                                .getStringPreference(UserDataService.PREF_KEY_IMAGE_VIEW_MODE, ViewMode::fromString);
//
//
//                        if (ListUtils.isEmpty(recentRecords)) {
//                            showDirectoryImages(SystemUtils.getCameraDir(), true);
//                        } else {
//                            RecentRecord recentRecord = ListUtils.firstOf(recentRecords);
//
//                            File directory = new File(recentRecord.getFilePath());
//                            Log.d(TAG, "reloadImageList: show recent access folder : " + directory);
//                            showDirectoryImages(directory, true);
//                        }
//
//                    }, RxUtils::unhandledThrowable);

            mIsImageListLoaded = true;
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

    private void updateFolderListItemThumbnailList(File directory) {
        if (directory == null) {
            Log.e(TAG, "updateFolderListItemThumbnailList: directory is null/empty");
            return;
        }
        ImageService.getInstance()
                .rescanDirectoryThumbnailList(directory)
                .compose(workAndShow())
                .subscribe(files -> {

                }, RxUtils::unhandledThrowable);
    }

    private DefaultImageListAdapter createImageListAdapter(List<DefaultImageListAdapter.Item> items) {
        DefaultImageListAdapter defaultImageListAdapter = new DefaultImageListAdapter(R.layout.image_list_item, items);
        defaultImageListAdapter.setOnInteractionListener(new BaseSelectableAdapter.OnInteractionListener<BaseSelectableAdapter, DefaultImageListAdapter.Item>() {
            @Override
            public void onItemLongClick(DefaultImageListAdapter.Item item, int position) {
                Log.d(TAG, "onItemLongClick: " + item);
                if (!item.isSelected()) {
                    mImageList.setDragSelectActive(true, position);
                }
            }

            @Override
            public void onItemCheckedChanged(DefaultImageListAdapter.Item item) {

            }

            @Override
            public void onItemClicked(DefaultImageListAdapter.Item item, View view, int position) {
                onClickItem(view, position, item.getFile());
            }

            @Override
            public void onSelectionModeChange(BaseSelectableAdapter adapter, boolean isSelectionMode) {

                DefaultImageListAdapter a = (DefaultImageListAdapter) adapter;
                Log.d(TAG, "onSelectionModeChange: isSelectionMode " + isSelectionMode);
                if (isSelectionMode) {

                    changeFloatingCount(FloatWindowService.MSG_INCREASE);

                    setImageToolbarVisible(true);
//            mFloatingToolbar.showContextMenu();
                } else {
                    changeFloatingCount(FloatWindowService.MSG_DECREASE);

                    setImageToolbarVisible(false);
//            mFloatingToolbar.dismissPopupMenus();
                }

                updateActionBarTitleCount(isSelectionMode ? adapter.getSelectedItemCount() : 0, a.getDirectory(), adapter.getItemCount());
            }

            @Override
            public void onSelectedItemLongClick(View view, int position, DefaultImageListAdapter.Item item) {
                int selectedCount = mCurrentImageAdapter.getSelectedItemCount();

                LinkedList<String> menuItems = new LinkedList<>();
                menuItems.add(getString(R.string.copy_d_files_to, selectedCount));
                menuItems.add(getString(R.string.move_d_files_to, selectedCount));
                menuItems.add(getString(R.string.remove_d_files, selectedCount));

                new MaterialDialog.Builder(MainActivity.this)
                        .title(getString(R.string.selected_files_operation))
                        .items(menuItems)
                        .itemsCallback((dialog, itemView, pos, text) -> {
                            switch (pos) {
                                case 0: {
                                    // copy
                                    ToastUtils.toastShort(MainActivity.this, R.string.unimplemented);
                                }
                                break;
                                case 1: {
                                    // move
                                    showMoveFileDialog();
                                }
                                break;
                                case 2: {
                                    // remove
                                    ToastUtils.toastShort(MainActivity.this, R.string.unimplemented);
                                }
                                break;
                            }
                        })
                        .show();
            }

            @Override
            public void onSelectedCountChange(BaseSelectableAdapter adapter, int selectedCount) {
                DefaultImageListAdapter listAdapter = (DefaultImageListAdapter) adapter;

                updateActionBarTitleCount(selectedCount, listAdapter.getDirectory(), adapter.getItemCount());

                List<DefaultImageListAdapter.Item> selectedItems = listAdapter.getSelectedItems();

                getImageAdapterSelectCountChangeRelay()
                        .accept(listAdapter);

                ImageService.getInstance()
                        .detectMoveFileConflict(((BaseImageAdapter) adapter).getDirectory(),
                                Stream.of(selectedItems).map(DefaultImageListAdapter.Item::getFile).toList());
            }
        });
        return defaultImageListAdapter;
    }

    private BehaviorRelay<DefaultImageListAdapter> getImageAdapterSelectCountChangeRelay() {
        if (mImageSelectCountRelay == null) {
            mImageSelectCountRelay = BehaviorRelay.create();
        }
        return mImageSelectCountRelay;
    }

    private BehaviorRelay<DetailImageAdapter> getDetailAdapterSelectCountChangeRelay() {
        if (mDetailImageSelectCountRelay == null) {
            mDetailImageSelectCountRelay = BehaviorRelay.create();
        }
        return mDetailImageSelectCountRelay;
    }

    private Observable<List<DefaultImageListAdapter.Item>> loadImages(File directory) {
        return Observable.create(e -> {
            LinkedList<DefaultImageListAdapter.Item> items = new LinkedList<DefaultImageListAdapter.Item>();
            List<File> images = ImageService.getInstance().listMediaFiles(directory);

            for (File file : images) {

                DefaultImageListAdapter.Item item = new DefaultImageListAdapter.Item();
                item.setFile(file);
                items.add(item);
            }

            e.onNext(items);
            e.onComplete();
        });
    }

    private void changeFloatingCount(int msgIncrease) {
        Intent intent = new Intent();
        intent.setAction(FloatWindowService.UPDATE_ACTION);
        intent.putExtra(FloatWindowService.EXTRA_MSG, msgIncrease);
        sendBroadcast(intent);
    }

    private void startImageViewerActivity(File directory, View view, int position, File file) {

        com.orhanobut.logger.Logger.d("查看图片 %d：%s", position, file.getAbsoluteFile());

        mCurrentViewerImageIndex = position;
        View imageView = view.findViewById(R.id.image);
        if (imageView == null || !(imageView instanceof ImageView)) {
            ToastUtils.toastLong(this, getString(R.string.cannot_get_image_for_position, position));
            return;
        }

        // Construct an Intent as normal
        Intent intent = ImageViewerActivity.newIntentViewFileList(this, directory.getAbsolutePath(),
                position, mSortOrder, mSortWay);

        // BEGIN_INCLUDE(start_activity)
        /**
         * Now create an {@link android.app.ActivityOptions} instance using the
         * {@link ActivityOptionsCompat#makeSceneTransitionAnimation(Activity, Pair[])} factory
         * method.
         */
        ActivityOptionsCompat activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,

                // Now we provide a list of Pair items which contain the view we can transitioning
                // from, and the name of the view it is transitioning to, in the launched activity
                new Pair<View, String>(imageView, ImageTransitionNameGenerator.generateTransitionName(file.getAbsolutePath())));
        // Now we can start the Activity, providing the activity options as a bundle
        ActivityCompat.startActivity(this, intent, activityOptions.toBundle());
        // END_INCLUDE(start_activity)
    }

    public void startPhotoActivity(Context context, File file, View imageView) {
        Intent intent = new Intent(context, DragPhotoActivity.class);
        int location[] = new int[2];

        //imageView.getLocationOnScreen(location);
        imageView.getLocationInWindow(location);

        intent.putExtra(DragPhotoActivity.EXTRA_LEFT, location[0]);
        intent.putExtra(DragPhotoActivity.EXTRA_TOP, location[1]);
        intent.putExtra(DragPhotoActivity.EXTRA_HEIGHT, imageView.getHeight());
        intent.putExtra(DragPhotoActivity.EXTRA_WIDTH, imageView.getWidth());
        intent.putExtra(DragPhotoActivity.EXTRA_FILE_PATH, file.getAbsolutePath());
        intent.putExtra(DragPhotoActivity.EXTRA_FULLSCREEN, false);

        context.startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void updateActionBarTitleCount(int selectedCount, File directory, int itemCount) {
        updateToolbarSubTitleCount(selectedCount, directory, itemCount);
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

    private void updateToolbarSubTitleCount(int selectedCount, File directory, int itemCount) {
        File dir = directory;
        if (mFolderAdapter != null) {
            mFolderAdapter.updateSelectedCount(dir, selectedCount);
        }

        // Update title
        String title;
        title = getSubTitleText(selectedCount, itemCount);
        mToolbar.setSubtitle(title);
    }

    @NonNull
    private String getSubTitleText(int selectedCount, int itemCount) {
        String title;
        if (selectedCount <= 0) {
            title = "";
        } else {
            title = getResources().getString(R.string.percent_d_d, selectedCount, itemCount);
        }
        return title;
    }

    BaseImageAdapter getCurrentAdapter() {
        switch (mViewMode) {

            case GRID_VIEW:
                return mCurrentImageAdapter;
            case LIST_VIEW:
                return mCurrentDetailImageAdapter;
            case UNKNOWN:
                break;
        }
        Log.w(TAG, "getCurrentAdapter: Fix this");
        return mCurrentImageAdapter;
    }

    private void showRemoveFileDialog() {
        BaseImageAdapter currImageAdapter = getCurrentAdapter();

        int selectCount = currImageAdapter.getSelectedItemCount();

        String countStr = String.valueOf(selectCount);
        SpannableStringBuilder countSB = SpannableStringBuilder.valueOf(String.valueOf(selectCount));
        int color = getResources().getColor(R.color.colorAccent);
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(color);
        countSB.setSpan(foregroundColorSpan, 0, countStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        new MaterialDialog.Builder(this)
                .title(R.string.remove_files)
                .content(getString(R.string.confirm_to_remove_files_in_folder_s,
                        currImageAdapter.getDirectory().getName(), selectCount))
                .positiveText(R.string.confirm)
                .onPositive((dialog, which) -> {


                    List<File> selectedFiles = currImageAdapter.getSelectedFiles();

                    ImageService.getInstance()
                            .removeFiles(selectedFiles)
                            .compose(workAndShow())
                            .subscribe(integerListPair -> {
                                Integer total = integerListPair.first;
                                List<File> failedFiles = integerListPair.second;
                                if (!failedFiles.isEmpty()) {
                                    ToastUtils.toastLong(this,
                                            getString(R.string.remove_d_files_success_with_d_files_failed_with_d_files,
                                                    total, total - failedFiles.size(), failedFiles.size()));
                                } else {
                                    ToastUtils.toastLong(this, getString(R.string.already_removed_d_files, total));
                                }
                            }, throwable -> {
                                throwable.printStackTrace();
                                ToastUtils.toastLong(this, R.string.remove_file_failed);
                            });
                })
                .negativeText(R.string.cancel)
                .show();
    }

    private void showMoveFileDialog() {

        List<String> selectedFiles = getSelectedFileList();
        showMoveFileFragmentDialog(new ArrayList<>(selectedFiles));

        /*ImageService.getInstance()
                .loadFolderList(true)
                .compose(workAndShow())
                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .subscribe(adapter -> {
                    showMoveFileDialog(selectedFiles, adapter);
                });*/
    }

    private <R> R mapValueTo(ViewMode viewMode, MapAction1<R, ViewMode> mapAction) {
        return mapAction.onMap(viewMode);
    }

    private List<String> getSelectedFileList() {
        return Stream.of(getCurrentSelectedFilePathList()).map(File::getAbsolutePath).toList();
    }

    private List<File> getCurrentSelectedFilePathList() {
        switch (mViewMode) {
            case GRID_VIEW:
                if (mCurrentImageAdapter != null) {
                    List<DefaultImageListAdapter.Item> selectedItems = mCurrentImageAdapter.getSelectedItems();
                    return Stream.of(selectedItems)
                            .map(DefaultImageListAdapter.Item::getFile)
                            .toList();
                }
                break;
            case LIST_VIEW: {
                if (mCurrentDetailImageAdapter != null) {
                    List<DetailImageAdapter.Item> selectedItems = mCurrentDetailImageAdapter.getSelectedItems();
                    return Stream.of(selectedItems)
                            .map(DetailImageAdapter.Item::getFile)
                            .toList();

                }
            }
            break;
            case UNKNOWN:
                break;
        }
        return new LinkedList<File>();
    }

    private void showMoveFileFragmentDialog(ArrayList<String> selectedFiles) {
        MoveFileDialogFragment fragment = MoveFileDialogFragment.newInstance(selectedFiles);
        fragment.show(getSupportFragmentManager(), "move file dialog");
    }

    private void showMoveFileDialog(List<File> selectedFiles, SectionedFolderListAdapter adapter) {
        View contentView = createMoveFileDialogContentView(adapter);

        new MaterialDialog.Builder(this)
                .title(R.string.move_selected_files_to)
                .customView(contentView, false)
                .positiveText(R.string.move_file)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    Object tag = contentView.getTag(R.id.item);
                    if (tag != null && tag instanceof SectionedFolderListAdapter.Item) {

                        File destDir = ((SectionedFolderListAdapter.Item) tag).getFile();
                        ImageService.getInstance()
                                .moveFilesToDirectory(destDir, selectedFiles)
                                .compose(workAndShow())
                                .subscribe(count -> {
                                    if (count == selectedFiles.size()) {
                                        ToastUtils.toastLong(MainActivity.this, MainActivity.this.getString(R.string.already_moved_d_files, count));
                                    } else if (count > 0 && count < selectedFiles.size()) {
                                        // 部分文件移动失败
                                        ToastUtils.toastShort(MainActivity.this, R.string.move_files_successfully_but_);
                                    } else {
                                        ToastUtils.toastShort(MainActivity.this, R.string.move_files_failed);
                                    }
                                }, throwable -> {
                                    ToastUtils.toastShort(MainActivity.this, R.string.move_files_failed);
                                });
                    }
                })
                .onNegative((dialog, which) -> {

                })
//                            .adapter(adapter, new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false))
                .show();
    }

    private View createMoveFileDialogContentView(SectionedFolderListAdapter adapter) {
        View root = LayoutInflater.from(this)
                .inflate(R.layout.move_file_dialog, null, false);

        RecyclerView recyclerView = (RecyclerView) root.findViewById(R.id.folder_list);
        TextView desc = (TextView) root.findViewById(R.id.desc);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);

        RecyclerItemTouchListener.OnItemClickListener clickListener = (view, position) -> {
            new SectionedListItemClickDispatcher(adapter)
                    .dispatch(position, new SectionedListItemDispatchListener() {
                        @Override
                        public void onHeader(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {
                            boolean sectionExpanded = adapter.isSectionExpanded(coord.section());
                            if (sectionExpanded) {
                                adapter.collapseSection(coord.section());
                            } else {
                                adapter.expandSection(coord.section());
                            }
                        }

                        @Override
                        public void onFooter(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {

                        }

                        @Override
                        public void onItem(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {
                            SectionedFolderListAdapter.Item item = mFolderAdapter.getItem(coord);

                            desc.setText(getString(R.string.move_selected_images_to_directory_s, item.getFile().getName()));
                            root.setTag(R.id.item, item);
                        }
                    });
        };
        RecyclerItemTouchListener.OnItemLongClickListener longClickListener = (view, position) -> {

        };
        recyclerView.addOnItemTouchListener(new RecyclerItemTouchListener(this, recyclerView, clickListener, longClickListener));

        return root;
    }
}

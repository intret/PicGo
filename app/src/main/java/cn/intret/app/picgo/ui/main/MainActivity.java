package cn.intret.app.picgo.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
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

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.ConflictResolverDialogFragment;
import cn.intret.app.picgo.model.RecentRecord;
import cn.intret.app.picgo.model.event.RecentOpenFolderListChangeMessage;
import cn.intret.app.picgo.model.event.RescanFolderListMessage;
import cn.intret.app.picgo.model.event.RescanFolderThumbnailListMessage;
import cn.intret.app.picgo.model.event.RescanImageDirectoryMessage;
import cn.intret.app.picgo.model.FolderModel;
import cn.intret.app.picgo.model.event.FolderModelChangeMessage;
import cn.intret.app.picgo.model.GroupMode;
import cn.intret.app.picgo.model.Image;
import cn.intret.app.picgo.model.ImageFolder;
import cn.intret.app.picgo.model.ImageGroup;
import cn.intret.app.picgo.model.event.RemoveFileMessage;
import cn.intret.app.picgo.model.event.RenameDirectoryMessage;
import cn.intret.app.picgo.model.SystemImageService;
import cn.intret.app.picgo.model.UserDataService;
import cn.intret.app.picgo.ui.adapter.FlatFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.FolderListAdapter;
import cn.intret.app.picgo.ui.adapter.ImageListAdapter;
import cn.intret.app.picgo.ui.adapter.ImageTransitionNameGenerator;
import cn.intret.app.picgo.ui.adapter.SectionFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.SectionedFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.FolderListAdapterUtils;
import cn.intret.app.picgo.ui.adapter.SectionedImageListAdapter;
import cn.intret.app.picgo.ui.adapter.SectionedListItemClickDispatcher;
import cn.intret.app.picgo.ui.adapter.SectionedListItemDispatchListener;
import cn.intret.app.picgo.ui.event.CurrentImageChangeMessage;
import cn.intret.app.picgo.ui.floating.FloatWindowService;
import cn.intret.app.picgo.ui.pref.SettingActivity;
import cn.intret.app.picgo.utils.DateTimeUtils;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.PathUtils;
import cn.intret.app.picgo.utils.RxUtils;
import cn.intret.app.picgo.utils.SystemUtils;
import cn.intret.app.picgo.utils.ToastUtils;
import cn.intret.app.picgo.view.T9KeypadView;
import cn.intret.app.picgo.widget.RecyclerItemTouchListener;
import cn.intret.app.picgo.widget.SectionDecoration;
import io.reactivex.Observable;

public class MainActivity extends BaseAppCompatActivity implements ImageListAdapter.OnInteractionListener {

    public static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    private static final String TAG = "MainActivity";

    @BindView(R.id.img_list) RecyclerView mImageList;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.drawer_folder_list) RecyclerView mFolderList;

    @BindView(R.id.view_mode) RadioGroup mModeRadioGroup;
    @BindView(R.id.floatingToolbar) Toolbar mFloatingToolbar;
//    @BindView(R.id.fab) FloatingActionButton mFab;

    @BindView(R.id.t9_keypad_container) ViewGroup mKeypadContainer;
    @BindView(R.id.t9_keypad) T9KeypadView mKeypad;
    @BindView(R.id.keyboard_switch_layout) ViewGroup mKeypadSwitchLayout;
    @BindView(R.id.keyboard_switch_image_view) ImageView mKeypadSwitch;

    private int mSpanCount;

    private ActionBarDrawerToggle mDrawerToggle;

    private FolderListAdapter mFolderListAdapter;
    private Intent mStartFloatingIntent;

    /**
     * Key: file absolute path
     * Value: ImageListAdapter
     */
    Map<String, ImageListAdapter> mImageListAdapters = new LinkedHashMap<>();
    private ImageListAdapter mCurrentImageAdapter;
    private GridLayoutManager mGridLayoutManager;

    Map<String, SectionedImageListAdapter> mSectionedImageListAdapters = new LinkedHashMap<>();
    Map<String, SectionedImageListAdapter> mWeekSectionedImageListAdapters = new LinkedHashMap<>();
    Map<String, SectionedImageListAdapter> mDaySectionedImageListAdapters = new LinkedHashMap<>();
    Map<String, SectionedImageListAdapter> mMonthSectionedImageListAdapters = new LinkedHashMap<>();
    private GridLayoutManager mGridLayout;
    private SectionedImageListAdapter mCurrentAdapter;
    private boolean mIsFolderListLoaded = false;
    private boolean mIsImageListLoaded = false;
    private GroupMode mGroupMode = GroupMode.DEFAULT;
    private File mCurrentFolder;
    private int mCurrentShownImageIndex = -1;
    private SectionedFolderListAdapter mFolderAdapter;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initToolBar();
        initDrawer();
        initListViewHeader();
        initListViewToolbar();


        initTransition();

        EventBus.getDefault().register(this);
        //showFloatingWindow();
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

        mFloatingToolbar.inflateMenu(R.menu.selected_images_action);
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
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");

        super.onStart();

        loadFolderList();
        loadImageList();
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
                if (mCurrentShownImageIndex != -1) {
                    ImageListAdapter.Item item = mCurrentImageAdapter.getItem(mCurrentShownImageIndex);
                    String filePath = item.getFile().getAbsolutePath();
                    String transitionName = ImageTransitionNameGenerator.generateTransitionName(filePath);
                    String fileTypeTransitionName = ImageTransitionNameGenerator.generateTransitionName(
                            ImageViewerActivity.TRANSITION_PREFIX_FILETYPE, filePath);

                    boolean addFileTypeTransitionName = PathUtils.isVideoFile(filePath);

                    sharedElements.clear();

                    RecyclerView.ViewHolder vh = mImageList.findViewHolderForAdapterPosition(mCurrentShownImageIndex);
                    if (vh instanceof ImageListAdapter.ViewHolder) {
                        ImageListAdapter.ViewHolder viewHolder = ((ImageListAdapter.ViewHolder) vh);

                        sharedElements.put(transitionName, viewHolder.getImage());

                        if (addFileTypeTransitionName) {
                            sharedElements.put(fileTypeTransitionName, viewHolder.getFileType());
                        }
                    }

                    names.clear();
                    names.add(transitionName);

                    if (addFileTypeTransitionName) {
                        names.add(fileTypeTransitionName);
                    }
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
            ImageListAdapter adapter = mImageListAdapters.get(destDir.getAbsolutePath());
            if (adapter != null) {
                int selectedCount = adapter.getSelectedCount();
                int itemCount = adapter.getItemCount();
                mFolderAdapter.updateSelectedCount(message.getDestDir(), selectedCount);
                mFolderAdapter.updateItemCount(message.getDestDir(), itemCount);
            }
        }

        List<android.util.Pair<File, File>> successFiles = message.getResult().getSuccessFiles();
        if (successFiles != null) {
            Stream.of(successFiles)
                    .groupBy(fileFilePair -> fileFilePair.second.getParent())
                    .forEach(objectListEntry -> {
                        String dir = objectListEntry.getKey();
                        ImageListAdapter adapter = mImageListAdapters.get(dir);
                        if (adapter != null) {
                            int selectedCount = adapter.getSelectedCount();
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
        mCurrentShownImageIndex = message.getPosition();

        mImageList.scrollToPosition(mCurrentShownImageIndex);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RemoveFileMessage message) {
        File file = message.getFile();
        if (file != null) {
            String dir = file.getParent();

            ImageListAdapter adapter = mImageListAdapters.get(dir);
            if (adapter != null) {
                Log.d(TAG, "onEvent: RemoveFileMessage ");
                diffUpdateImageListAdapter(adapter, true);
            }

            SystemImageService.getInstance()
                    .rescanDirectoryThumbnailList(file.getParentFile())
                    .compose(workAndShow())
                    .subscribe(files -> {

                    });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RescanFolderThumbnailListMessage message) {
        Log.d(TAG, "onEvent() called with: message = [" + message + "]");

        mFolderAdapter.updateThumbList(message.getDirectory(), message.getThumbnails());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(RescanImageDirectoryMessage message) {
        File dir = message.getDirectory();

        ImageListAdapter adapter = mImageListAdapters.get(dir.getAbsolutePath());
        if (adapter != null) {
            diffUpdateImageListAdapter(adapter, true);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RescanFolderListMessage message) {
        com.orhanobut.logger.Logger.d("RescanFolderListMessage " + message);
        diffUpdateFolderListAdapter(mFolderAdapter);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RenameDirectoryMessage message) {

        com.orhanobut.logger.Logger.d("RenameDirectoryMessage " + message);
        File oldDirectory = message.getOldDirectory();
        mFolderAdapter.renameDirectory(oldDirectory, message.getNewDirectory());


        // Update Title & Subtitle
        if (mCurrentImageAdapter != null) {
            if (mCurrentImageAdapter.getDirectory().equals(message.getOldDirectory())) {

                updateActionBarTitle(message.getNewDirectory().getName());

                String title = getSubTitleText(mCurrentImageAdapter.getSelectedCount(), mCurrentImageAdapter.getItemCount());
                mToolbar.setSubtitle(title);
            }
        }

        // Update : Image list adapter
        String oldDirAbsolutePath = oldDirectory.getAbsolutePath();
        ImageListAdapter imageListAdapter = mImageListAdapters.get(oldDirAbsolutePath);
        if (imageListAdapter != null) {

            mImageListAdapters.remove(oldDirAbsolutePath);

            imageListAdapter.setDirectory(message.getNewDirectory());
            mImageListAdapters.put(message.getNewDirectory().getAbsolutePath(), imageListAdapter);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(FolderModelChangeMessage message) {
        mIsFolderListLoaded = false;
        loadFolderList();
    }

    private void diffUpdateFolderListAdapter(SectionedFolderListAdapter adapter) {
        logger.info("差量更新文件夹列表");

        SystemImageService.getInstance()
                .loadFolderListModel(true)
                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .compose(workAndShow())
                .subscribe(adapter::diffUpdateItems,
                        Throwable::printStackTrace);
    }

    private void diffUpdateImageListAdapter(ImageListAdapter adapter, boolean fromCacheFirst) {
        Log.d(TAG, String.format("差量更新图片列表 dir=%s fromCacheFirst=%s", adapter.getDirectory(), fromCacheFirst));

        File dir = adapter.getDirectory();
        SystemImageService.getInstance()
                .loadImageList(dir, fromCacheFirst)
                .map(this::imagesToListItems)
                .compose(workAndShow())
                .subscribe(adapter::diffUpdateWithItems,
                        Throwable::printStackTrace);
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
        showDirectoryImageList(mCurrentFolder, true);
    }

    @Override
    public void onBackPressed() {

        // // TODO: 2017/9/2 其他图片显示模式的退出选择操作
        boolean isSelectionMode = false;
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

        if (isSelectionMode) {
            // 退出选择模式
            switch (mGroupMode) {

                case DEFAULT: {
                    mCurrentImageAdapter.leaveSelectionMode();
                    onSelectionModeChange(mCurrentImageAdapter, false);
                }
                break;
                case DAY:
                    break;
                case WEEK:
                    break;
                case MONTH:
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

        mToolbar.setOnMenuItemClickListener(item -> {

            switch (item.getItemId()) {
                case R.id.app_bar_recent: {
                    showRecentHistoryMenu();
                }
                break;
                case R.id.app_bar_search:
                    ToastUtils.toastShort(MainActivity.this, "search");
                    break;
                case R.id.app_bar_setting: {
                    MainActivity.this.startActivity(new Intent(MainActivity.this, SettingActivity.class));
//                    ToastUtils.toastShort(MainActivity.this, "setting");
                }
                break;
                case R.id.app_bar_view_mode:
                    ToastUtils.toastShort(MainActivity.this, "viewmode");
                    break;
            }

            return true;
        });
    }

    private void showRecentHistoryMenu() {
        PopupMenu popupMenu = new PopupMenu(this, mToolbar, Gravity.RIGHT);
        Menu menu = popupMenu.getMenu();
        for (int i = 0; i < mRecentHistory.size(); i++) {
            File dir = mRecentHistory.get(i);
            MenuItem menuItem = menu.add(dir.getName());
            menuItem.setIcon(R.drawable.ic_move_to_folder);
            menuItem.setOnMenuItemClickListener(item -> {

                showDirectoryImageList(dir, true);
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
        mKeypad.getDialpadInputObservable().subscribe(input -> {
            SystemImageService.getInstance()
                    .loadFolderListModel(true, input.toString())
                    .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                    .compose(RxUtils.workAndShow())
                    .subscribe(newAdapter -> {

                        SectionedFolderListAdapter currAdapter = (SectionedFolderListAdapter) mFolderList.getAdapter();
                        if (currAdapter != null) {
                            currAdapter.diffUpdateItems(newAdapter);
                        }

                    }, Throwable::printStackTrace);
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

    private void switchKeyboard() {
        boolean isVisible = mKeypadContainer.getVisibility() == View.VISIBLE;
        int v = isVisible ? View.INVISIBLE : View.VISIBLE;
        mKeypadContainer.setVisibility(v);

        if (isVisible) {
            mKeypadSwitch.setBackgroundResource(R.drawable.keyboard_show_selector);
        } else {
            mKeypadSwitch.setBackgroundResource(R.drawable.keyboard_hide_selector);
        }
    }

    private void loadFolderList() {
        if (mIsFolderListLoaded) {
            Log.d(TAG, "loadFolderList: TODO 检查文件列表变化");
        } else {
            // 初始化相册文件夹列表
            SystemImageService.getInstance()
                    .loadFolderListModel(true)
                    .compose(workAndShow())
                    .doOnError(Throwable::printStackTrace)
                    .subscribe(this::showFolderList);
        }

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
                showDirectoryImageList(item.getFile(), true);
            }

            @Override
            public void onItemLongClick(SectionedFolderListAdapter.Section sectionItem, int section, SectionedFolderListAdapter.Item item, int relativePos) {
                showFolderItemContextMenuDialog(item);
            }
        };

        // Create adapter
        SectionedFolderListAdapter listAdapter = FolderListAdapterUtils.folderModelToSectionedFolderListAdapter(model);
        listAdapter.setShowHeaderOptionButton(true);
        listAdapter.setOnItemClickListener(onItemClickListener);

        // RecyclerView layout
        mFolderList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // Set adapter
        mFolderList.setAdapter(listAdapter);
        mFolderAdapter = listAdapter;

        // List item click event
        RecyclerItemTouchListener itemTouchListener = new RecyclerItemTouchListener(this,
                mFolderList,
                (view, position) -> onFolderListItemClick(position),
                (view, position) -> onFolderListItemLongClick(position)
        );
//        mFolderList.addOnItemTouchListener(itemTouchListener);

        // show firstOf folder's images in activity content field.

        /*
        SectionedFolderListAdapter.Section sectionItem = ListUtils.firstOf(sections);
        if (sectionItem != null) {
            SectionedFolderListAdapter.Item item = ListUtils.firstOf(sectionItem.getItems());
            if (item != null) {
                showDirectoryImageList(item.getFile());
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
                                    SystemImageService.getInstance()
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
                    break;
            }
            return false;
        });
        popupMenu.show();
    }

    private void onFolderListItemLongClick(int position) {
        new SectionedListItemClickDispatcher(mFolderAdapter)
                .dispatch(position, new SectionedListItemDispatchListener() {
                    @Override
                    public void onHeader(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {

                    }

                    @Override
                    public void onFooter(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {

                    }

                    @Override
                    public void onItem(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {
                        SectionedFolderListAdapter.Item item = mFolderAdapter.getItem(coord);
                        showFolderItemContextMenuDialog(item);
                        //showActionDialogForFile(item.getFile());
                    }
                });
    }

    private void showFolderItemContextMenuDialog(SectionedFolderListAdapter.Item item) {
        LinkedList<String> menuItems = new LinkedList<>();
        File selectedDir = item.getFile();

        menuItems.add(getString(R.string.rename_folder_s, selectedDir.getName()));
        menuItems.add(getString(R.string.move_folder_s, item.getFile().getName()));
        menuItems.add(getString(R.string.remove_folder_s, item.getFile().getName()));

        List<Map.Entry<String, ImageListAdapter>> inSelectionModeAdapter = Stream.of(mImageListAdapters)
                .filter(value -> value.getValue().isSelectionMode())
                .toList();

        if (!inSelectionModeAdapter.isEmpty()) {
            menuItems.add(getString(R.string.move_selected_images_to_here));
        }

        new MaterialDialog.Builder(this)
                .title(R.string.folder_operations)
                .items(menuItems)
                .itemsCallback((dialog, itemView, position, text) -> {
                    switch (position) {
                        case 0: { // rename
                            showRenameFolderDialog(item, selectedDir);
                        }
                        break;
                        case 1: // 移动
                            break;
                        case 2: // 删除
                            showDeleteFolderDialog(selectedDir);
                            break;
                        case 3: {
                            showActionDialogForFile(selectedDir);
                        }
                        break;
                    }
                })
                .show();
    }

    private void showDeleteFolderDialog(File selectedDir) {

        new MaterialDialog.Builder(this)
                .title(R.string.remove_folder)
                .content(getString(R.string.confirm_to_remove_folder_s, selectedDir.getName()))
                .positiveText(R.string.delete)
                .onPositive((dialog, which) -> {
                    SystemImageService.getInstance()
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

    private void showRenameFolderDialog(SectionedFolderListAdapter.Item item, File dir) {
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

                            SystemImageService.getInstance()
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

    private void onFolderListItemClick(int position) {
        new SectionedListItemClickDispatcher<>(mFolderAdapter)
                .dispatchItemClick(position, (adapter, coord) -> {
                    SectionedFolderListAdapter.Item item = adapter.getItem(coord);

                    Log.d(TAG, "onItemClick: 显示 " + item.getFile());
//                        mDrawerLayout.closeDrawers();
                    showDirectoryImageList(item.getFile(), false);
                });
    }

    private void showActionDialogForFile(File dir) {

        List<Map.Entry<String, ImageListAdapter>> selectionModeAdapters = Stream.of(mImageListAdapters)
                .filter(value -> value.getValue().isSelectionMode())
                .toList();
        if (!selectionModeAdapters.isEmpty()) {

            List<String> names = Stream.of(selectionModeAdapters)
                    .map(entry -> {
                        ImageListAdapter value = entry.getValue();
                        return this.getString(R.string.main_drawer_menu_item_move__item__to__folder,
                                value.getDirectory().getName(), value.getSelectedCount());
                    })
                    .toList();

//            new MaterialDialog.Builder(this)
//                    .title(getString(R.string.folder_s_operations, item.getName()))
//                    .items(names)
//                    .itemsCallback((dialog, itemView, position, text) -> {
//                        Map.Entry<String, ImageListAdapter> entry = selectionModeAdapters.get(position);
//                        File directory = entry.getValue().getDirectory();
//                        Log.d(TAG,String.format("on menu item selection : move files from '%s' to '%s'", directory, item.getFile()));
//                    })
//                    .show();


            List<FlatFolderListAdapter.Item> items = Stream.of(selectionModeAdapters)
                    .map(entry -> new FlatFolderListAdapter.Item()
                            .setDirectory(entry.getValue().getDirectory())
                            .setCount(entry.getValue().getSelectedCount())
                            .setThumbList(
                                    Stream.of(entry.getValue()
                                            .getSelectedItemUntil(3))
                                            .map(ImageListAdapter.Item::getFile)
                                            .toList()
                            )).toList();

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
                                        moveAdapterSelectedFilesToDir(f, dir);
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

    private void moveAdapterSelectedFilesToDir(File srcDir, File destDir) {
        SystemImageService.getInstance()
                .moveFilesToDirectory(destDir,
                        Stream.of(
                                mImageListAdapters
                                        .get(srcDir.getAbsolutePath())
                                        .getSelectedItems()
                        )
                                .map(ImageListAdapter.Item::getFile)
                                .toList()
                        ,
                        true,
                        false
                ).compose(workAndShow())
                .subscribe(result -> {

                    mFolderAdapter.updateSelectedCount(mCurrentImageAdapter.getDirectory(),
                            mCurrentImageAdapter.getSelectedCount());

                    ToastUtils.toastLong(this, getString(R.string.already_moved_d_files, result.getSuccessFiles().size()));
                }, throwable -> {
                    ToastUtils.toastLong(this, R.string.move_files_failed);
                });
    }

    private void showFolderModel(FolderModel model) {

        List<SectionFolderListAdapter.SectionItem> sectionItems = new LinkedList<>();

        List<FolderModel.ParentFolderInfo> parentFolderInfos = model.getParentFolderInfos();
        for (int i = 0, s = parentFolderInfos.size(); i < s; i++) {
            sectionItems.add(folderInfoToItem(parentFolderInfos.get(i)));
        }

        SectionFolderListAdapter listAdapter = new SectionFolderListAdapter(sectionItems);

        listAdapter.setOnItemClickListener((sectionItem, item) -> {
            mDrawerLayout.closeDrawers();
            showDirectoryImageList(item.getFile(), true);
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
                showDirectoryImageList(item.getFile(), true);
            }
        }
//        // show firstOf folder's images in activity content field.
//        if (sectionItems.size() > 0) {
//
//            SectionFolderListAdapter.Section item = sectionItems.get(0);
//            List<SectionFolderListAdapter.Item> items = item.getItems();
//            if (items != null && !items.isEmpty()) {
//                showDirectoryImageList(items.get(0).getFile());
//            }
//        }
    }

    private SectionFolderListAdapter.SectionItem folderInfoToItem(FolderModel.ParentFolderInfo parentFolderInfo) {
        SectionFolderListAdapter.SectionItem sectionItem = new SectionFolderListAdapter.SectionItem();
        sectionItem.setName(parentFolderInfo.getName());
        sectionItem.setFile(parentFolderInfo.getFile());
        sectionItem.setItems(
                Stream.of(parentFolderInfo.getFolders())
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
                .map(this::folderInfoToFolderListAdapterItem)
                .toList();
        showFolderItems(items);
    }

    private FolderListAdapter.Item folderInfoToFolderListAdapterItem(ImageFolder imageFolder) {
        return new FolderListAdapter.Item()
                .setName(imageFolder.getName())
                .setCount(imageFolder.getCount())
                .setDirectory(imageFolder.getFile())
                .setThumbList(imageFolder.getThumbList())
                ;
    }

    @Deprecated
    @MainThread
    private void showFolderItems(List<FolderListAdapter.Item> items) {

        mFolderListAdapter = new FolderListAdapter(items);
        mFolderListAdapter.setOnItemEventListener(item -> {

            mDrawerLayout.closeDrawers();

            showDirectoryImageList(item.getDirectory(), true);

        });
        mFolderList.addItemDecoration(new SectionDecoration(this, new SectionDecoration.DecorationCallback() {
            @Override
            public long getGroupId(int position) {
                return SystemImageService.getInstance().getSectionForPosition(position);
//                return mFolderListAdapter.getItemCount();
            }

            @Override
            public String getGroupFirstLine(int position) {
                return SystemImageService.getInstance().getSectionFileName(position);
            }
        }));
        mFolderList.setAdapter(mFolderListAdapter);

//        // show firstOf folder's images in activity content field.
//        if (items != null && items.size() > 0) {
//
//            FolderListAdapter.Item item = items.get(0);
//            showDirectoryImageList(item.getDirectory());
//        }
    }

    public void updateActionBarTitle(String name) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(name);
        }
    }

    @MainThread
    private void showDirectoryImageList(File directory, boolean updateFolderList) {
        mCurrentFolder = directory;

        if (updateFolderList && mFolderAdapter != null) {
            mFolderAdapter.selectItem(directory);
        }

        updateActionBarTitle(directory.getName());

        if (mGroupMode == GroupMode.DEFAULT) {
            showImageList(directory);
        } else {
            showSectionedImageList(directory, mGroupMode);
        }
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
                SystemImageService.getInstance()
                        .loadImageGroupList(directory, groupMode, true)
                        .map(this::imageGroupsToAdapter)
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

    private List<SectionedImageListAdapter.Section> imageGroupsToAdapter(List<ImageGroup> imageGroups) {
        List<SectionedImageListAdapter.Section> sections = new LinkedList<>();
        for (int i = 0, imageGroupsSize = imageGroups.size(); i < imageGroupsSize; i++) {
            ImageGroup imageGroup = imageGroups.get(i);
            sections.add(imageGroupToAdapterSection(imageGroup));
        }
        return sections;
    }

    private SectionedImageListAdapter.Section imageGroupToAdapterSection(ImageGroup imageGroup) {
        SectionedImageListAdapter.Section section = new SectionedImageListAdapter.Section();

        section.setStartDate(imageGroup.getStartDate());
        section.setEndDate(imageGroup.getEndDate());

        List<Image> images = imageGroup.getImages();
        if (images != null) {
            List<SectionedImageListAdapter.Item> items = new LinkedList<>();
            for (int i = 0, imagesSize = images.size(); i < imagesSize; i++) {
                Image image = images.get(i);
                items.add(new SectionedImageListAdapter.Item()
                        .setFile(image.getFile())
                        .setDate(image.getDate())
                );
            }
            section.setItems(items);
        }
        section.setDescription(getDescription(section.getStartDate(), MainActivity.this.mGroupMode));
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

            List<File> imageFiles = SystemImageService.getInstance().listImageFiles(directory);
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
                                section.setDescription(getDescription(firstItem.getDate(), MainActivity.this.mGroupMode));

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

    private String getDescription(Date date, GroupMode groupMode) {

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

    private void showImageList(File directory) {
        ImageListAdapter listAdapter = mImageListAdapters.get(directory.getAbsolutePath());
        if (listAdapter != null) {
            com.orhanobut.logger.Logger.d("showImageList 切换显示目录 " + directory);
            showImageListAdapter(listAdapter);

        } else {

            com.orhanobut.logger.Logger.d("showImageList 加载并显示目录 " + directory);

            SystemImageService.getInstance()
                    .loadImageList(directory, true)
                    .compose(workAndShow())
                    .map(this::imagesToListItems)
                    .map(items -> itemsToAdapter(directory, items))
                    .doOnNext(adapter -> cacheImageAdapter(directory, adapter))
                    .doOnError(throwable -> {
                        throwable.printStackTrace();
                        Toast.makeText(this, R.string.load_pictures_failed, Toast.LENGTH_SHORT).show();
                    })
                    .subscribe(this::showImageListAdapter,
                            throwable -> {
                            });

        }
    }

    private void cacheImageAdapter(File directory, ImageListAdapter adapter) {
        mImageListAdapters.put(directory.getAbsolutePath(), adapter);
    }

    private ImageListAdapter itemsToAdapter(File directory, List<ImageListAdapter.Item> items) {
        ImageListAdapter adapter = createImageListAdapter(items);
        adapter.setDirectory(directory);
        return adapter;
    }

    private List<ImageListAdapter.Item> imagesToListItems(List<Image> images) {
        return Stream.of(images).map(image -> new ImageListAdapter.Item().setFile(image.getFile())).toList();
    }

    List<File> mRecentHistory = new LinkedList<>();

    private void showImageListAdapter(ImageListAdapter adapter) {

        // Save recent accessed folder
        addRecentHistoryRecord(adapter, adapter.getDirectory());

        // Update action bar title for new shown adapter
        updateActionBarTitle(adapter.getDirectory().getName());
        updateActionBarTitleCount(adapter, adapter.getSelectedCount());

        // Setup adapter
        adapter.setOnInteractionListener(this);
        if (mCurrentImageAdapter != null) {
            int i = ((GridLayoutManager) mImageList.getLayoutManager()).findFirstVisibleItemPosition();
            mCurrentImageAdapter.saveFirstVisibleItemPosition(i);
        }
        mCurrentImageAdapter = adapter;

        // RecyclerView Layout
        mGridLayoutManager = new GridLayoutManager(this, mSpanCount, GridLayoutManager.VERTICAL, false);
        mImageList.setLayoutManager(mGridLayoutManager);

        mFloatingToolbar.setVisibility(mCurrentImageAdapter.isSelectionMode() ? View.VISIBLE : View.GONE);

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

    private void addRecentHistoryRecord(ImageListAdapter adapter, File directory) {

        UserDataService.getInstance().addOpenFolderRecentRecord(directory)
                .subscribe(aBoolean -> {
                    int i1 = org.apache.commons.collections4.ListUtils.indexOf(mRecentHistory, file -> file.equals(adapter.getDirectory()));
                    if (i1 != -1) {
                        mRecentHistory.remove(i1);
                    }
                    mRecentHistory.add(0, directory);

                }, Throwable::printStackTrace);
    }

    private void loadImageList() {

        if (mIsImageListLoaded) {

            Log.d(TAG, "loadImageList: TODO 检查并更新图片文件");

            if (mCurrentImageAdapter != null) {
                File directory = mCurrentImageAdapter.getDirectory();
                if (directory != null) {
                    diffUpdateImageListAdapter(mCurrentImageAdapter, false);
                }
            }

        } else {
            mSpanCount = 3; // columns

            // Item Click event TODO: move code to adapter
            mImageList.addOnItemTouchListener(new RecyclerItemTouchListener(this,
                    mImageList,
                    (view, position) -> {

                        if (mCurrentImageAdapter != null) {

                            // let adapter consume click event
                            mCurrentImageAdapter.handleItemClickEvent(view, position);
                        }
                    },
                    ((view, position) -> {
                        if (mCurrentImageAdapter != null) {
                            mCurrentImageAdapter.handleItemLongClickEvent(view, position);
                        }
                    })
            ));

            // Item spacing
//            LayoutMarginDecoration marginDecoration =
//                    new LayoutMarginDecoration(mSpanCount, getResources().getDimensionPixelSize(R.dimen.image_list_item_space));
//
//            mImageList.addItemDecoration(marginDecoration);

            UserDataService.getInstance()
                    .loadRecentOpenFolders(true)
                    .compose(workAndShow())
                    .subscribe(recentRecords -> {

                        mRecentHistory = Stream.of(recentRecords).map(r -> new File(r.getFilePath())).toList();

                        if (ListUtils.isEmpty(recentRecords)) {
                            showDirectoryImageList(SystemUtils.getCameraDir(), true);
                        } else {
                            RecentRecord recentRecord = ListUtils.firstOf(recentRecords);

                            File directory = new File(recentRecord.getFilePath());
                            Log.d(TAG, "loadImageList: show recent access folder : " + directory);
                            showDirectoryImageList(directory, true);
                        }

                    }, Throwable::printStackTrace);


            mIsImageListLoaded = true;
        }

//        mImageList.setLayoutManager(new StaggeredGridLayoutManager(mSpanCount, StaggeredGridLayoutManager.VERTICAL));
//        mImageList.setAdapter(new ImageListAdapter(new LinkedList<ImageListAdapter.Item>()));
//        GridLayoutManager layout = new GridLayoutManager(this, mSpanCount, GridLayoutManager.VERTICAL, false);
//        mImageList.setLayoutManager(layout);
        //SectionedImageListAdapter adapter = new SectionedImageListAdapter(new LinkedList<SectionedImageListAdapter.Section>());
        //adapter.setLayoutManager(layout);
        //mImageList.setAdapter(adapter);

//        mImageList.addItemDecoration(new GridSpacingItemDecoration(mSpanCount,
//                getResources().getDimensionPixelSize(R.dimen.image_list_item_space), true));
    }

    private ImageListAdapter createImageListAdapter(List<ImageListAdapter.Item> items) {
        return new ImageListAdapter(items)
                .setSpanCount(mSpanCount)
                .setOnInteractionListener(this);
    }


    private Observable<List<ImageListAdapter.Item>> loadImages(File directory) {
        return Observable.create(e -> {
            LinkedList<ImageListAdapter.Item> items = new LinkedList<ImageListAdapter.Item>();
            List<File> images = SystemImageService.getInstance().listImageFiles(directory);

            for (File file : images) {
                items.add(new ImageListAdapter.Item().setFile(file));
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

    @Override
    public void onItemClicked(ImageListAdapter.Item item, View view, int position) {

        //startImageViewerActivity(item, mCurrentImageAdapter.getDirectory(), view, 0);

        //startPhotoActivity(this, item.getFile(), view);
//        Intent intent = ImageViewerActivity.newIntentViewFile(this, item.getFile());
//        startActivity(intent);

        // handle event
        try {
            Log.d(TAG, "Clicked item at position " + position + " " + item.getFile() + " " + item.getTransitionName());
            startImageViewerActivity(item, mCurrentImageAdapter.getDirectory(), view, position);
        } catch (Exception e) {
            Log.e(TAG, "image list item click exception : " + e.getMessage());
        }
    }

    @Override
    public void onItemLongClick(ImageListAdapter.Item item) {
        Log.d(TAG, "onItemLongClick: " + item);
    }

    @Override
    public void onItemCheckedChanged(ImageListAdapter.Item item) {
        Log.d(TAG, "onItemCheckedChanged: " + item);
    }

    private void startImageViewerActivity(ImageListAdapter.Item item, File directory, View view, int position) {

        com.orhanobut.logger.Logger.d("查看图片 %d：%s", position, item.getFile().getAbsoluteFile());

        mCurrentShownImageIndex = position;
        View imageView = view.findViewById(R.id.image);
        if (imageView == null || !(imageView instanceof ImageView)) {
            ToastUtils.toastLong(this, getString(R.string.cannot_get_image_for_position, position));
            return;
        }

        // Construct an Intent as normal
        Intent intent = ImageViewerActivity.newIntentViewFileList(this, directory.getAbsolutePath(),
                position);

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
                new Pair<View, String>(imageView, item.getTransitionName()));
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

    @Override
    public void onSelectionModeChange(ImageListAdapter adapter, boolean isSelectionMode) {
        Log.d(TAG, "onSelectionModeChange: isSelectionMode " + isSelectionMode);
        if (isSelectionMode) {

            changeFloatingCount(FloatWindowService.MSG_INCREASE);


            mFloatingToolbar.setVisibility(View.VISIBLE);
//            mFloatingToolbar.showContextMenu();
        } else {
            changeFloatingCount(FloatWindowService.MSG_DECREASE);

            mFloatingToolbar.setVisibility(View.GONE);
//            mFloatingToolbar.dismissPopupMenus();
        }

        updateActionBarTitleCount(adapter, isSelectionMode ? adapter.getSelectedCount() : 0);
    }

    @Override
    public void onSelectedCountChange(ImageListAdapter adapter, int selectedCount) {
        File dir = adapter.getDirectory();

        updateActionBarTitleCount(adapter, selectedCount);
    }

    private void updateActionBarTitleCount(ImageListAdapter adapter, int selectedCount) {
        updateToolbarSubTitleCount(adapter, selectedCount);
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

    private void updateToolbarSubTitleCount(ImageListAdapter adapter, int selectedCount) {
        File dir = adapter.getDirectory();
        if (mFolderAdapter != null) {
            mFolderAdapter.updateSelectedCount(dir, selectedCount);
        }

        // Update title
        String title;
        title = getSubTitleText(selectedCount, adapter.getItemCount());
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

    @Override
    public void onDragBegin(View view, int position, ImageListAdapter.Item item) {

        int selectedCount = mCurrentImageAdapter.getSelectedCount();

        LinkedList<String> menuItems = new LinkedList<>();
        menuItems.add(getString(R.string.copy_d_files_to, selectedCount));
        menuItems.add(getString(R.string.move_d_files_to, selectedCount));
        menuItems.add(getString(R.string.remove_d_files, selectedCount));

        new MaterialDialog.Builder(this)
                .title(getString(R.string.selected_files_operation))
                .items(menuItems)
                .itemsCallback((dialog, itemView, pos, text) -> {
                    switch (pos) {
                        case 0: {
                            // copy
                            ToastUtils.toastShort(this, R.string.unimplemented);
                        }
                        break;
                        case 1: {
                            // move
                            showMoveFileDialog();
                        }
                        break;
                        case 2: {
                            // remove
                            ToastUtils.toastShort(this, R.string.unimplemented);
                        }
                        break;
                    }
                })
                .show();
    }

    private void showRemoveFileDialog() {
        int sectionCount = mCurrentImageAdapter.getSelectedCount();

        String countStr = String.valueOf(sectionCount);
        SpannableStringBuilder countSB = SpannableStringBuilder.valueOf(String.valueOf(sectionCount));
        int color = getResources().getColor(R.color.colorAccent);
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(color);
        countSB.setSpan(foregroundColorSpan, 0, countStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        new MaterialDialog.Builder(this)
                .title(R.string.remove_files)
                .content(getString(R.string.confirm_to_remove_files_in_folder_s,
                        mCurrentImageAdapter.getDirectory().getName(), sectionCount))
                .positiveText(R.string.confirm)
                .onPositive((dialog, which) -> {

                    List<ImageListAdapter.Item> selectedItems = mCurrentImageAdapter.getSelectedItems();
                    List<File> files = Stream.of(selectedItems)
                            .map(ImageListAdapter.Item::getFile)
                            .toList();

                    SystemImageService.getInstance()
                            .removeFiles(files)
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

        List<ImageListAdapter.Item> selectedItems = mCurrentImageAdapter.getSelectedItems();
        List<String> selectedFiles = Stream.of(selectedItems)
                .map(ImageListAdapter.Item::getFile)
                .map(File::getAbsolutePath)
                .toList();
        showMoveFileFragmentDialog(new ArrayList<>(selectedFiles));

        /*SystemImageService.getInstance()
                .loadFolderListModel(true)
                .compose(workAndShow())
                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .subscribe(adapter -> {
                    showMoveFileDialog(selectedFiles, adapter);
                });*/
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
                        SystemImageService.getInstance()
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

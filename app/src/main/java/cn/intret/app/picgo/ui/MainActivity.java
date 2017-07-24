package cn.intret.app.picgo.ui;

import android.content.Intent;
import android.support.annotation.MainThread;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.service.GalleryService;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements WaterfallImageListAdapter.OnItemEventListener {

    private static final String TAG = "MainActivity";

    @BindView(R.id.img_list) RecyclerView mImageList;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.drawer_folder_list) RecyclerView mDrawerFolderList;

    private WaterfallImageListAdapter mImageListAdapter;
    private int mSpanCount;

    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    private CharSequence mDrawerTitle;
    private FolderListAdapter mFolderListAdapter;
    private Intent mStartFloatingIntent;

    /**
     * Key: file absolute path
     * Value: WaterfallImageListAdapter
     */
    Map<String, WaterfallImageListAdapter> mImageAdapters = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initAppBar();
        initDrawer();
        initImageList();
        //showFloatingWindow();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
//        changeFloatingCount(FloatWindowService.MSG_INCREASE);

        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

//        changeFloatingCount(FloatWindowService.MSG_DECREASE);
    }

    @Override
    protected void onDestroy() {
//        stopService(mStartFloatingIntent);

        super.onDestroy();
    }

    private void showFloatingWindow() {
        mStartFloatingIntent = new Intent(MainActivity.this, FloatWindowService.class);
        startService(mStartFloatingIntent);
    }

    private void initAppBar() {


    }

    private void initDrawer() {
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(false);

            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                    myToolbar, R.string.drawer_open, R.string.drawer_close) {

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


        // 初始化相册文件夹列表
        mDrawerFolderList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        loadFolderList();
    }

    private void loadFolderList() {
        loadGalleryFolderList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                })
                .subscribe(this::showFolderItems);
    }

    @MainThread
    private void showFolderItems(List<FolderListAdapter.Item> items) {

        mFolderListAdapter = new FolderListAdapter(items);
        mFolderListAdapter.setOnItemEventListener(item -> {
            showImageList(item);

            mDrawerLayout.closeDrawers();
            Log.d(TAG, "initDrawer: 切换显示目录 " + item);
        });
        mDrawerFolderList.setAdapter(mFolderListAdapter);

        // show first folder's images in activity content field.
        if (items != null && items.size() > 0) {

            FolderListAdapter.Item item = items.get(0);
            showImageList(item);
        }
    }

    public void changeTitle(File dir) {
        if (dir != null) {
            String name = dir.getName();
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(name);
            }
        }
    }

    @MainThread
    private void showImageList(FolderListAdapter.Item item) {

        changeTitle(item.getDirectory());

        File file = item.getDirectory();
        WaterfallImageListAdapter imageListAdapter = mImageAdapters.get(file.getAbsolutePath());
        if (imageListAdapter != null) {
            showImageListAdapter(imageListAdapter);

        } else {
            loadGalleryImages(item.getDirectory())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(throwable -> {
                        throwable.printStackTrace();
                        Toast.makeText(this, R.string.load_gallery_failed, Toast.LENGTH_SHORT).show();
                    })
                    .subscribe(items -> {
                                WaterfallImageListAdapter adapter = createWaterfallImageListAdapter(items);

                                mImageAdapters.put(item.getDirectory().getAbsolutePath(), adapter);

                                showImageListAdapter(adapter);

                            }, throwable -> {

                            }, () -> {

                            }
                    );

        }
    }

    private void showImageListAdapter(WaterfallImageListAdapter adapter) {
        adapter.setOnItemEventListener(this);
        mImageListAdapter = adapter;

        mImageList.setLayoutManager(new StaggeredGridLayoutManager(mSpanCount, StaggeredGridLayoutManager.VERTICAL));
        mImageList.swapAdapter(mImageListAdapter, true);
    }


    private void initImageList() {

//        mImageList.addOnScrollListener();

        mSpanCount = 6; // columns
        //mImageList.setLayoutManager(new StaggeredGridLayoutManager(mSpanCount, StaggeredGridLayoutManager.VERTICAL));
    }

    private WaterfallImageListAdapter createWaterfallImageListAdapter(List<WaterfallImageListAdapter.Item> items) {
        return new WaterfallImageListAdapter(items)
                .setSpanCount(mSpanCount)
                .setOnItemEventListener(this);
    }

    private Observable<List<FolderListAdapter.Item>> loadGalleryFolderList() {
        return Observable.create(emitter -> {
            LinkedList<FolderListAdapter.Item> items = new LinkedList<FolderListAdapter.Item>();
            List<File> folders = GalleryService.getInstance().getAllGalleryFolders();

            for (File file : folders) {
                File[] files = file.listFiles();
                items.add(new FolderListAdapter.Item()
                        .setDirectory(file)
                        .setName(file.getName())
                        .setCount(files == null ? 0 : files.length)
                );
            }

            emitter.onNext(items);
            emitter.onComplete();
        });
    }

    private Observable<List<WaterfallImageListAdapter.Item>> loadGalleryImages(File directory) {
        return Observable.<List<WaterfallImageListAdapter.Item>>create(e -> {
            LinkedList<WaterfallImageListAdapter.Item> items = new LinkedList<WaterfallImageListAdapter.Item>();
            List<File> images = GalleryService.getInstance().loadAllFolderImages(directory);

            for (File file : images) {
                items.add(new WaterfallImageListAdapter.Item().setFile(file));
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
    public void onItemLongClick(WaterfallImageListAdapter.Item item) {
        Log.d(TAG, "onItemLongClick: " + item);
    }

    @Override
    public void onItemCheckedChanged(WaterfallImageListAdapter.Item item) {
        Log.d(TAG, "onItemCheckedChanged: " + item);
    }

    @Override
    public void onSelectionModeChange(boolean isSelectionMode) {
        Log.d(TAG, "onSelectionModeChange: isSelectionMode " + isSelectionMode);
        if (isSelectionMode) {
            changeFloatingCount(FloatWindowService.MSG_INCREASE);
        } else {
            changeFloatingCount(FloatWindowService.MSG_DECREASE);
        }
    }

    @Override
    public void onDragStared() {

    }
}

package cn.intret.app.picgo.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.annimon.stream.Stream;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.ImageFolder;
import cn.intret.app.picgo.model.FolderModel;
import cn.intret.app.picgo.model.SystemImageService;
import cn.intret.app.picgo.ui.adapter.FolderListAdapter;
import cn.intret.app.picgo.ui.adapter.ImageListAdapter;
import cn.intret.app.picgo.ui.adapter.RecyclerItemClickListener;
import cn.intret.app.picgo.ui.adapter.SectionDecoration;
import cn.intret.app.picgo.ui.adapter.SectionFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.SectionedFolderListAdapter;
import cn.intret.app.picgo.ui.floating.FloatWindowService;
import cn.intret.app.picgo.utils.ListUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements ImageListAdapter.OnItemEventListener {

    private static final String TAG = "MainActivity";

    @BindView(R.id.img_list) RecyclerView mImageList;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.drawer_folder_list) RecyclerView mDrawerFolderList;

    private ImageListAdapter mCurrentImageListAdapter;
    private int mSpanCount;

    private ActionBarDrawerToggle mDrawerToggle;

    private FolderListAdapter mFolderListAdapter;
    private Intent mStartFloatingIntent;

    /**
     * Key: file absolute path
     * Value: ImageListAdapter
     */
    Map<String, ImageListAdapter> mImageListAdapters = new LinkedHashMap<>();
    private StaggeredGridLayoutManager mGridLayoutManager;


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

        mDrawerLayout.openDrawer(Gravity.LEFT);
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

        initFolderList();
    }

    private void initFolderList() {
        // 初始化相册文件夹列表
        SystemImageService.getInstance().loadAvailableFolderListModel()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .subscribe(this::showFolderList);
    }

    private void showFolderList(FolderModel model) {

        List<SectionedFolderListAdapter.SectionItem> sectionItems = new LinkedList<>();

        List<FolderModel.ParentFolderInfo> parentFolderInfos = model.getParentFolderInfos();
        for (int i = 0, s = parentFolderInfos.size(); i < s; i++) {
            sectionItems.add(parentFolderToItem(parentFolderInfos.get(i)));
        }

        SectionedFolderListAdapter listAdapter = new SectionedFolderListAdapter(sectionItems);

//        listAdapter.setOnItemClickListener((sectionItem, item) -> {
//            mDrawerLayout.closeDrawers();
//            showDirectoryImageList(item.getFile());
//        });
        mDrawerFolderList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mDrawerFolderList.setAdapter(listAdapter);
        mDrawerFolderList.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ItemCoord relativePosition = listAdapter.getRelativePosition(position);
                boolean header = listAdapter.isHeader(position);
                boolean footer = listAdapter.isFooter(position);
                if (header) {
                    boolean sectionExpanded = listAdapter.isSectionExpanded(relativePosition.section());
                    if (sectionExpanded) {
                        listAdapter.collapseSection(relativePosition.section());
                    } else {
                        listAdapter.expandSection(relativePosition.section());
                    }
                } else if (footer) {
                    Log.d(TAG, "onItemClick: footer clicked");
                } else {
                    SectionedFolderListAdapter.Item item = listAdapter.getItem(relativePosition);
                    Log.d(TAG, "onItemClick: 显示 " + item.getFile());
                    mDrawerLayout.closeDrawers();
                    showDirectoryImageList(item.getFile());
                }
                Log.d(TAG, "onItemClick() called with: view = [" + view + "], position = [" + position + "]");
            }
        }));

        // show firstOf folder's images in activity content field.
        SectionedFolderListAdapter.SectionItem sectionItem = ListUtils.firstOf(sectionItems);
        if (sectionItem != null) {
            SectionedFolderListAdapter.Item item = ListUtils.firstOf(sectionItem.getItems());
            if (item != null) {
                showDirectoryImageList(item.getFile());
            }
        }
    }

    private SectionedFolderListAdapter.SectionItem parentFolderToItem(FolderModel.ParentFolderInfo parentFolderInfo) {
        SectionedFolderListAdapter.SectionItem sectionItem = new SectionedFolderListAdapter.SectionItem();

        return sectionItem.setName(parentFolderInfo.getName())
                .setFile(parentFolderInfo.getFile())
                .setItems(
                        Stream.of(parentFolderInfo.getFolders())
                                .map(item -> new SectionedFolderListAdapter.Item()
                                        .setFile(item.getFile())
                                        .setName(item.getName())
                                        .setCount(item.getCount())
                                        .setThumbList(item.getThumbList())
                                )
                                .toList()
                );
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
            showDirectoryImageList(item.getFile());
        });
        mDrawerFolderList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mDrawerFolderList.setAdapter(listAdapter);
        mDrawerFolderList.addOnItemTouchListener(new RecyclerItemClickListener(this, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Log.d(TAG, "onItemClick() called with: view = [" + view + "], position = [" + position + "]");
            }
        }));

        SectionFolderListAdapter.SectionItem sectionItem = ListUtils.firstOf(sectionItems);
        if (sectionItem != null) {
            SectionFolderListAdapter.Item item = ListUtils.firstOf(sectionItem.getItems());
            if (item != null) {
                showDirectoryImageList(item.getFile());
            }
        }
        // show firstOf folder's images in activity content field.
        if (sectionItems.size() > 0) {

            SectionFolderListAdapter.SectionItem item = sectionItems.get(0);
            List<SectionFolderListAdapter.Item> items = item.getItems();
            if (items != null && !items.isEmpty()) {
                showDirectoryImageList(items.get(0).getFile());
            }
        }
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

    @MainThread
    private void showFolderItems(List<FolderListAdapter.Item> items) {

        mFolderListAdapter = new FolderListAdapter(items);
        mFolderListAdapter.setOnItemEventListener(item -> {

            mDrawerLayout.closeDrawers();

            showDirectoryImageList(item.getDirectory());

        });
        mDrawerFolderList.addItemDecoration(new SectionDecoration(this, new SectionDecoration.DecorationCallback() {
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
        mDrawerFolderList.setAdapter(mFolderListAdapter);

        // show firstOf folder's images in activity content field.
        if (items != null && items.size() > 0) {

            FolderListAdapter.Item item = items.get(0);
            showDirectoryImageList(item.getDirectory());
        }
    }

    public void changeTitle(String name) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(name);
        }
    }

    @MainThread
    private void showDirectoryImageList(File directory) {

        changeTitle(directory.getName());

        ImageListAdapter imageListAdapter = mImageListAdapters.get(directory.getAbsolutePath());
        if (imageListAdapter != null) {
            Log.d(TAG, "showDirectoryImageList: 切换显示目录 " + directory);
            showImageListAdapter(imageListAdapter);

        } else {
            loadGalleryImages(directory)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(throwable -> {
                        throwable.printStackTrace();
                        Toast.makeText(this, R.string.load_gallery_failed, Toast.LENGTH_SHORT).show();
                    })
                    .subscribe(items -> {
                                ImageListAdapter adapter = createImageListAdapter(items);

                                mImageListAdapters.put(directory.getAbsolutePath(), adapter);

                                showImageListAdapter(adapter);

                            }, throwable -> {

                            }, () -> {

                            }
                    );

        }
    }

    private void showImageListAdapter(ImageListAdapter adapter) {
        adapter.setOnItemEventListener(this);

        mCurrentImageListAdapter = adapter;

        mGridLayoutManager = new StaggeredGridLayoutManager(mSpanCount, StaggeredGridLayoutManager.VERTICAL);
        mImageList.swapAdapter(mCurrentImageListAdapter, false);
    }


    private void initImageList() {

        mSpanCount = 4; // columns

        mImageList.setLayoutManager(new StaggeredGridLayoutManager(mSpanCount, StaggeredGridLayoutManager.VERTICAL));
        mImageList.setAdapter(new ImageListAdapter(new LinkedList<ImageListAdapter.Item>()));
//        mImageList.addItemDecoration(new GridSpacingItemDecoration(mSpanCount,
//                getResources().getDimensionPixelSize(R.dimen.image_list_item_space), true));
    }

    private ImageListAdapter createImageListAdapter(List<ImageListAdapter.Item> items) {
        return new ImageListAdapter(items)
                .setSpanCount(mSpanCount)
                .setOnItemEventListener(this);
    }


    private Observable<List<ImageListAdapter.Item>> loadGalleryImages(File directory) {
        return Observable.<List<ImageListAdapter.Item>>create(e -> {
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
    public void onItemLongClick(ImageListAdapter.Item item) {
        Log.d(TAG, "onItemLongClick: " + item);
    }

    @Override
    public void onItemCheckedChanged(ImageListAdapter.Item item) {
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

package cn.intret.app.picgo.ui;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemLongClick;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.service.GalleryService;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements WaterfallImageListAdapter.OnItemEventListener {

    private static final String TAG = "MainActivity";

    @BindView(R.id.img_list)
    RecyclerView mImageList;
    private WaterfallImageListAdapter mImageListAdapter;
    private int mSpanCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initImageList();
    }


    private void initImageList() {

//        mImageList.addOnScrollListener();

        mSpanCount = 4; // columns
        mImageList.setLayoutManager(new StaggeredGridLayoutManager(mSpanCount, StaggeredGridLayoutManager.VERTICAL));

        loadGalleryImages()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    throwable.printStackTrace();
                    Toast.makeText(this, R.string.load_gallery_failed, Toast.LENGTH_SHORT).show();
                })
                .subscribe(items -> {
                            mImageListAdapter = createWaterfallImageListAdapter(items);
                            mImageList.setAdapter(mImageListAdapter);

                        }, throwable -> {

                        }, () -> {

                        }
                );

        mImageList.setAdapter(mImageListAdapter);
    }

    private WaterfallImageListAdapter createWaterfallImageListAdapter(List<WaterfallImageListAdapter.Item> items) {
        return new WaterfallImageListAdapter(items)
                .setSpanCount(mSpanCount)
                .setOnItemEventListener(this);
    }

    private Observable<List<WaterfallImageListAdapter.Item>> loadGalleryImages() {
        return Observable.<List<WaterfallImageListAdapter.Item>>create(e -> {
            LinkedList<WaterfallImageListAdapter.Item> items = new LinkedList<WaterfallImageListAdapter.Item>();
            List<File> pictures = GalleryService.getInstance().loadLatestPictures(400);

            for (File file : pictures) {
                items.add(new WaterfallImageListAdapter.Item().setFile(file));
            }

            e.onNext(items);
            e.onComplete();
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    }
}

package cn.intret.app.picgo.ui.main;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.util.Log;
import android.util.Size;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.orhanobut.logger.Logger;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.ImageFileInformation;
import cn.intret.app.picgo.model.ImageService;
import cn.intret.app.picgo.model.LoadMediaFileParam;
import cn.intret.app.picgo.model.MediaFile;
import cn.intret.app.picgo.model.SortOrder;
import cn.intret.app.picgo.model.SortWay;
import cn.intret.app.picgo.model.ViewMode;
import cn.intret.app.picgo.model.event.RemoveFileMessage;
import cn.intret.app.picgo.ui.adapter.brvah.DefaultImageListAdapter;
import cn.intret.app.picgo.ui.adapter.TransitionUtils;
import cn.intret.app.picgo.ui.event.CurrentImageChangeMessage;
import cn.intret.app.picgo.utils.FileSizeUtils;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.MediaUtils;
import cn.intret.app.picgo.utils.PathUtils;
import cn.intret.app.picgo.utils.RxUtils;
import cn.intret.app.picgo.utils.SystemUtils;
import cn.intret.app.picgo.utils.ToastUtils;
import cn.intret.app.picgo.utils.ViewUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import jp.wasabeef.blurry.Blurry;
import pl.droidsonroids.gif.GifDrawable;

/**
 * Image viewer with view pager.
 *
 * @implNote FIXED problems at : https://stackoverflow.com/questions/30628543/fragment-shared-element-transitions-dont-work-with-viewpager
 */
public class ImageActivity extends BaseAppCompatActivity implements ImageFragment.OnFragmentInteractionListener {

    private static final String TAG = ImageActivity.class.getSimpleName();
    private static final String EXTRA_IMAGE_FILE_PATH = "extra:file_name";
    private static final String EXTRA_IMAGE_DIR_PATH = "extra:dir_path";
    private static final String EXTRA_IMAGE_ITEM_POSITION = "extra:image_item_position";
    private static final String EXTRA_VIEW_MODE = "extra:view_mode";
    private static final String EXTRA_SORT_WAY = "extra:sort_way";
    private static final String EXTRA_SORT_ORDER = "extra:sort_order";

    private static final String EXTRA_PARAM_FILE_PATH = "viewer:param:filepath";
    public static final String TRANSITION_NAME_IMAGE = "viewer:image";

    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.viewpager) ViewPager mViewPager;
    @BindView(R.id.bottom_toolbar) ViewGroup mBottomToolbar;

    @BindView(R.id.index) TextView mBrief;
    @BindView(R.id.resolution) TextView mResolution;
    @BindView(R.id.btn_delete) ImageView mBtnDelete;
    @BindView(R.id.btn_detail) ImageView mBtnDetail;
    @BindView(R.id.pager_container) ViewGroup mPagerContainer;

    @BindView(R.id.detail_container) ViewGroup mDetailRootLayout;
    @BindView(R.id.blur_layout) ImageView mBlurLayout;
    @BindView(R.id.file_detail_container) ViewGroup mDetailContainer;
    @BindView(R.id.mask_view) View mMaskView;

    private PagerAdapter mImageAdapter;
    private String mImageFilePath;
    private ImagePagerAdapter mImagePagerAdapter;
    private LinkedList<Image> mImages;
    private PhotoView mPhotoView;
    private ImageFragmentStatePagerAdapter mPagerAdapter;
    private String mTransitionName;
    private String mDirPath;
    private int mItemPosition;
    private boolean mCancelExitTransition;
    private int mCurrentItem = -1;
    private ViewMode mViewMode;
    private SortWay mSortWay;
    private SortOrder mSortOrder;
    private AnimationType mAnimationType = AnimationType.FADE_IN_FADE_OUT;
    private boolean mCurrentFullscreen;
    private AnimatorSet mFullscreenAnimatorSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.postponeEnterTransition(this);
//        supportPostponeEnterTransition();

        View decorView = getWindow().getDecorView();

        setContentView(R.layout.activity_image);

        ButterKnife.bind(this);

        extractIntentData();

        initToolbar();

        loadImageFiles();

        initImageTransition();
    }

    private void initToolbar() {
//        DrawableCompat.setTint(mBtnDelete.getDrawable(), ContextCompat.getColor(this, R.color.white));
//        DrawableCompat.setTint(mBtnDetail.getDrawable(), ContextCompat.getColor(this, R.color.white));

        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.hide();
        }

        initStatusBar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            ActivityCompat.finishAfterTransition(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * https://developer.android.com/training/system-ui/status.html
     */
    private void initStatusBar() {

//        View decorView = getWindow().getDecorView();
//        decorView.setOnSystemUiVisibilityChangeListener
//                (new View.OnSystemUiVisibilityChangeListener() {
//                    @Override
//                    public void onSystemUiVisibilityChange(int visibility) {
//                        // Note that system bars will only be "visible" if none of the
//                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
//                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
//                            // TODO: The system bars are visible. Make any desired
//                            // adjustments to your UI, such as showing the action bar or
//                            // other navigational controls.
//                        } else {
//                            // TODO: The system bars are NOT visible. Make any desired
//                            // adjustments to your UI, such as hiding the action bar or
//                            // other navigational controls.
//                        }
//                    }
//                });
//        StatusBarUtil.setTranslucent(this);
//        StatusBarUtil.setTranslucent(this, 125);

//        StatusBarUtil.setColor(this, getResources().getColor(R.color.black));
    }

    private void initImageTransition() {
        ActivityCompat.setEnterSharedElementCallback(this, new SharedElementCallback() {

            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {

                Log.d(TAG, "imageView enter before onMapSharedElements() called with: names = [" + names + "], sharedElements = [" + sharedElements + "]");
                if (mCurrentItem != -1) {
                    Image item = mPagerAdapter.getImage(mCurrentItem);

                    ImageFragment fragment = mPagerAdapter.getRegisteredFragment(mCurrentItem);

                    String absolutePath = item.getFile().getAbsolutePath();
                    String transitionName = TransitionUtils.generateTransitionName(absolutePath);
                    String fileTypeTransitionName = TransitionUtils.generateTransitionName(TransitionUtils.TRANSITION_PREFIX_FILETYPE, absolutePath);

                    names.clear();
                    names.add(transitionName);
                    //names.add(fileTypeTransitionName);

                    sharedElements.clear();
                    PhotoView image = fragment.getImage();
                    if (image != null) {
                        sharedElements.put(transitionName, fragment.getImage());
                    } else {
                        View iv = null;
                        View root = fragment.getView();
                        if (root != null) {
                            iv = root.findViewById(R.id.image);
                        }

                        if (iv != null) {
                            sharedElements.put(transitionName, iv);
                        } else {
                            Log.e(TAG, "imageView enter onMapSharedElements: cannot get PhotoView instance.");
                        }
                    }

//                    ImageView fileType = fragment.getFileType();
//                    if (fileType != null) {
//                        sharedElements.put(fileTypeTransitionName, fragment.getFileType());
//                    } else {
//                        View iv = null;
//                        View root = fragment.getView();
//                        if (root != null) {
//                            iv = root.findViewById(R.id.file_type);
//                        }
//
//                        if (iv != null) {
//                            sharedElements.put(fileTypeTransitionName, iv);
//                        } else {
//                            Log.e(TAG, "imageView enter onMapSharedElements: cannot get FileType ImageView instance.");
//                        }
//                    }
                }
                Log.d(TAG, "imageView enter after onMapSharedElements() called with: names = [" + names + "], sharedElements = [" + sharedElements + "]");
                super.onMapSharedElements(names, sharedElements);
            }
        });

        ActivityCompat.setExitSharedElementCallback(this, new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                Log.d(TAG, "imageView enter before onMapSharedElements() called with: names = [" + names + "], sharedElements = [" + sharedElements + "]");
                int currentItem = mViewPager.getCurrentItem();
                if (currentItem != -1) {
                    Image item = mPagerAdapter.getImage(currentItem);
                    sharedElements.clear();
                    String transitionName = TransitionUtils.generateTransitionName(item.getFile().getAbsolutePath());
                    String fileTypeTransitionName = TransitionUtils.generateTransitionName(TransitionUtils.TRANSITION_PREFIX_FILETYPE, item.getFile().getAbsolutePath());

                    sharedElements.put(transitionName, ((ImageFragment) mPagerAdapter.getItem(currentItem)).getImage());
                    sharedElements.put(fileTypeTransitionName, ((ImageFragment) mPagerAdapter.getItem(currentItem)).getFileType());
                }
                Log.d(TAG, "imageView enter after onMapSharedElements() called with: names = [" + names + "], sharedElements = [" + sharedElements + "]");
                super.onMapSharedElements(names, sharedElements);
            }
        });
    }

    @OnClick(R.id.btn_delete)
    public void onClickDeleteButton(View view) {

        ImageService.getInstance()
                .removeFile(mPagerAdapter.getImage(mCurrentItem).getFile())
                .subscribe(aBoolean -> {
                    if (aBoolean) {
                    } else {
                        ToastUtils.toastShort(this, R.string.remove_file_failed);
                    }
                }, throwable -> {
                    ToastUtils.toastShort(this, R.string.remove_file_failed);
                });

    }

    @OnClick(R.id.btn_detail)
    public void onClickDetailButton(View view) {

        if (mDetailRootLayout.getVisibility() == View.VISIBLE) {
            hideImageDetailViews();

        } else {

            Blurry.with(this)
                    .radius(25)
                    .sampling(2)
                    .async()
                    .animate()
                    .capture(mViewPager)
                    .into(mBlurLayout);

            File showingImageFile = getShowingImageFile();
            ImageService.getInstance()
                    .loadImageInfo(showingImageFile)
                    .compose(workAndShow())
                    .subscribe(info -> updateFileDetailView(showingImageFile, info));

            mBlurLayout.setOnClickListener(v -> {
                hideImageDetailViews();
            });

            Observable.just(mDetailRootLayout)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(viewGroup -> {
                        viewGroup.setVisibility(View.VISIBLE);
                    }, RxUtils::unhandledThrowable);
        }
    }

    private void updateFileDetailView(File showingImageFile, ImageFileInformation info) {
        // 文件大小
        ViewUtils.setText(mDetailContainer,
                R.id.value_file_size,
                FileSizeUtils.formatFileSize(info.getFileLength(), false));

        // 路径
        ViewUtils.setText(mDetailContainer,
                R.id.value_file_path,
                showingImageFile.getAbsolutePath());

        // 日期
        if (info.getExif() != null) {
            ViewUtils.setText(
                    mDetailContainer,
                    R.id.value_capture_time,
                    DateFormatUtils.format(info.getLastModified(),
                            FastDateFormat.getDateInstance(FastDateFormat.FULL).getPattern()));
        }

        // 分辨率
        String sizeString = getResources().getString(R.string.image_size_d_d,
                info.getMediaResolution().getWidth(), info.getMediaResolution().getHeight());

        ViewUtils.setText(mDetailContainer,
                R.id.value_resolution,
                MediaUtils.isValidSize(info.getMediaResolution()) ? sizeString : "-");
    }

    private void hideImageDetailViews() {
        mBlurLayout.setImageDrawable(null);
        mDetailRootLayout.setVisibility(View.INVISIBLE);
//        mBlurLayout.setVisibility(View.INVISIBLE);
    }

    File getShowingImageFile() {
        return mPagerAdapter.getImage(mCurrentItem).getFile();
    }

    @OnClick(R.id.btn_share)
    public void onClickShareButton(View view) {

        File currentFile = getShowingImageFile();
        Logger.d("share file : " + currentFile);

//        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
//        shareIntent.setType("image/jpg");
//        final File photoFile = new File(getFilesDir(), currentFile.getAbsolutePath());
//        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(photoFile));
//        startActivity(Intent.createChooser(shareIntent, "Share image using"));

//        ShareIntentBuilder.from(this)
//                .ignoreSpecification()
//
//                .stream(Uri.parse("content://" + currentFile), "application/octet-stream" )
//                .share(currentFile.getName());
        {
            if (PathUtils.isStaticImageFile(currentFile.getAbsolutePath())) {

//                ContentValues image = getImageContent(currentFile, currentFile.getName(), this);
//                Uri result = this.getContentResolver()
//                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image);

                // open share options
//                openShareOptions("Share title", "share message", result, this);
            }
        }
//        Intent shareIntent = new Intent();
//        shareIntent.setAction(Intent.ACTION_SEND);
//        shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
//        shareIntent.setType("image/jpeg");
//        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));

        ToastUtils.toastShort(this, R.string.unimplemented);
    }

    public static ContentValues getImageContent(File parent, String imageName, AppCompatActivity activity) {
        ContentValues image = new ContentValues();
        image.put(MediaStore.Images.Media.TITLE, activity.getString(R.string.app_name));
        image.put(MediaStore.Images.Media.DISPLAY_NAME, imageName);
        image.put(MediaStore.Images.Media.DESCRIPTION, imageName);
        image.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        image.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        image.put(MediaStore.Images.Media.ORIENTATION, 0);
        String filePath = parent.toString();
        image.put(MediaStore.Images.ImageColumns.BUCKET_ID, filePath.toLowerCase().hashCode());
        image.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, parent.getName().toLowerCase());
        image.put(MediaStore.Images.Media.SIZE, parent.length());
        image.put(MediaStore.Images.Media.DATA, parent.getAbsolutePath());
        return image;
    }

    public static void openShareOptions(String title, String description,
                                        final Uri imageUrl, final AppCompatActivity activity) {

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");
        share.putExtra(Intent.EXTRA_STREAM, imageUrl);
        activity.startActivity(Intent.createChooser(share, "Choose app to share"));


//        new BottomDialog.Builder(activity)
//                .setTitle(title)
//                .setContent(description)
//                .setPositiveText("SHARE")
//                .setPositiveBackgroundColorResource(R.color.colorPrimary)
//                .setPositiveTextColorResource(android.R.color.white)
//                .setNegativeText("OPEN")
//                .setNegativeTextColorResource(R.color.colorPrimaryDark)
//                .onPositive(new BottomDialog.ButtonCallback() {
//                    @Override
//                    public void onClick(BottomDialog dialog) {
//                        // share image globally
//                        Intent share = new Intent(Intent.ACTION_SEND);
//                        share.setType("image/jpeg");
//                        share.putExtra(Intent.EXTRA_STREAM, imageUrl);
//                        activity.startActivity(Intent.createChooser(share, "Choose app to share"));
//                        dialog.dismiss();
//                    }
//                }).onNegative(new BottomDialog.ButtonCallback() {
//            @Override
//            public void onClick(BottomDialog dialog) {
//                // opining default gallery app
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_VIEW);
//                intent.setDataAndType(imageUrl, "image/*");
//                activity.startActivity(intent);
//                dialog.dismiss();
//            }
//        }).show();
    }


    @Override
    protected void onStart() {
        super.onStart();

//        showRandomImage();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: ");
        //
        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (ViewUtils.isShow(mDetailRootLayout)) {
            hideImageDetailViews();
            return;
        }

        super.onBackPressed();
    }

    public static Intent newIntentViewFile(Context context, File file) {
        Intent intent = new Intent(context, ImageActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(EXTRA_IMAGE_FILE_PATH, file.getAbsolutePath());
        return intent;
    }

    public static Intent newIntentViewFileList(Context context, String dirAbsolutePath, int itemModelPosition, SortOrder sortOrder, SortWay sortWay) {
        Intent intent = new Intent(context, ImageActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(EXTRA_IMAGE_DIR_PATH, dirAbsolutePath);
        intent.putExtra(EXTRA_IMAGE_ITEM_POSITION, itemModelPosition);

        intent.putExtra(EXTRA_SORT_WAY, sortWay);
        intent.putExtra(EXTRA_SORT_ORDER, sortOrder);
        return intent;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TapImageMessage message) {
        ActivityCompat.finishAfterTransition(this);
//        switchFullscreen();
    }

    private void switchFullscreen() {

//        ActionBar ab = getSupportActionBar();
//        if (ab != null) {
//            if (ab.isShowing()) {
//                ab.hide();
//            } else {
//                ab.show();
//            }
//        }

        //switchFullScreenMode(null);
    }

    /**
     * 在全屏模式和正常模式之间切换显示
     */
    private void switchFullScreenMode(List<Animator> amList) {

        mCurrentFullscreen = isCurrentFullscreen();

        // 全屏切换动画
        List<Animator> animators = new LinkedList<>(org.apache.commons.collections4.ListUtils.emptyIfNull(amList));

        if (mAnimationType == AnimationType.FADE_IN_FADE_OUT) {

            ObjectAnimator e = alphaAnimatorOfView(mBottomToolbar);
            e.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                }
            });
            animators.add(e);

        } else if (mAnimationType == AnimationType.SLIDE_IN_SLIDE_OUT) {
            animators.add(animatorOfBottomToolbar()); // Actionbar animation
        }

        // Cancel previous animation
        if (mFullscreenAnimatorSet != null) {
            if (mFullscreenAnimatorSet.isRunning()) {
                return;
            }
        }

        Log.d(TAG, "switchFullScreenMode: start animation");

        // Start new animation
        mFullscreenAnimatorSet = new AnimatorSet();
        mFullscreenAnimatorSet.setDuration(200);
        mFullscreenAnimatorSet.playTogether(animators);
        mFullscreenAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
//                mBottomToolbar.setEnabled(false);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
//                mBottomToolbar.setEnabled(false);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mFullscreenAnimatorSet.start();

    }

    private enum AnimationType {
        FADE_IN_FADE_OUT,
        SLIDE_IN_SLIDE_OUT
    }

    // 保存动画过程中的值
    float mBottomToolbarTranslationY = 0;

    boolean isCurrentFullscreen() {
        if (mAnimationType == AnimationType.SLIDE_IN_SLIDE_OUT) {
            return mBottomToolbar.getTranslationY() == mBottomToolbar.getHeight();
        } else if (mAnimationType == AnimationType.FADE_IN_FADE_OUT) {
            return mBottomToolbar.getAlpha() == 0;
        }
        return false;
    }


    public static final float VIEW_FULL_SCREEN_ALPHA = 0.0f; // View 全屏模式下的 Alpha
    public static final float VIEW_NORMAL_ALPHA = 1.0f; // View 正常模式下的 Alpha

    private ObjectAnimator alphaAnimatorOfView(View v) {
        float alphaStart = v.getAlpha();
        float alphaEnd = isCurrentFullscreen() ? VIEW_NORMAL_ALPHA : VIEW_FULL_SCREEN_ALPHA;
        return ObjectAnimator.ofFloat(v, "alpha", alphaStart, alphaEnd);
    }


    @NonNull
    private ObjectAnimator animatorOfBottomToolbar() {

        View target = mBottomToolbar;
        float targetHeight = target.getHeight();
        float animateStartTy = target.getTranslationY();
        float animateEndTy = isCurrentFullscreen() ? 0 : targetHeight;

        ObjectAnimator abAnimator = ObjectAnimator.ofFloat(target, "translationY", animateStartTy, animateEndTy);

        abAnimator.addUpdateListener(animation -> {
            mBottomToolbarTranslationY = (float) animation.getAnimatedValue();
        });

        return abAnimator;
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DragImageExitMessage message) {
        ActivityCompat.finishAfterTransition(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ImageAnimationStartMessage message) {
//        View decorView = getWindow().getDecorView();

        // Hide the status bar.
//        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
//        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.

        //        StatusBarUtils.hideStatusBar(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RemoveFileMessage message) {

        mPagerAdapter.removeFile(message.getFile());
//        int currentItem = mViewPager.getCurrentItem();
//        if (SystemUtils.isSameFile(message.getFile(), mPagerAdapter.getImage(currentItem).getFile())) {
//            try {
//
//                mPagerAdapter.removeItem(currentItem);
//                mPagerAdapter.notifyDataSetChanged();
//
//                ToastUtils.toastLong(this, getResolutionString(R.string.already_removed_d_files, 1));
//
//            } catch (Throwable throwable) {
//
//                ToastUtils.toastLong(this, getResolutionString(R.string.remove_file_failed));
//            }
//
//        }
    }

    private void loadImageFiles() {

        if (mDirPath != null && mItemPosition != -1) {

            // 浏览文件列表
            ImageService.getInstance()
                    .loadMediaFileList(new File(mDirPath),
                            new LoadMediaFileParam()
                                    .setFromCacheFirst(true)
                                    .setLoadMediaInfo(false)
                                    .setSortOrder(mSortOrder)
                                    .setSortWay(mSortWay)
                    )
                    .compose(workAndShow())
                    .map(this::imageListToImageListAdapter)
                    .subscribe(adapter -> {
                        showImageAdapter(adapter, mItemPosition);
                    }, throwable -> {

                    });

        } else if (mImageFilePath != null) {

            // 浏览单个文件
            Observable.just(mImageFilePath)
                    .map(File::new)
                    .map(file -> {
                        MediaFile mediaFile = new MediaFile();
                        mediaFile.setFile(file);
                        mediaFile.setDate(new Date(file.lastModified()));
                        return mediaFile;
                    })
                    .map(ListUtils::objectToLinkedList)
                    .map(this::imageListToImageListAdapter)
                    .subscribe(adapter -> {
                        showImageAdapter(adapter, 0);
                    }, RxUtils::unhandledThrowable)
            ;
        }
    }

    private void showImageAdapter(ImageFragmentStatePagerAdapter adapter, int position) {
        Log.d(TAG, "showImageAdapter() called with: adapter = [" + adapter + "], position = [" + position + "]");

        mPagerAdapter = adapter;
        mPagerAdapter.setAnimatedItemPosition(position);

        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {

                Log.d(TAG, "onPageSelected() called with: position = [" + position + "]");

                mCurrentItem = position;

                showImageInfo(position);

                EventBus.getDefault().post(new CurrentImageChangeMessage().setPosition(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });


        if (position != -1) {
            mCurrentItem = position;
            mViewPager.setCurrentItem(position);

            showImageInfo(position);
        } else {
            mCurrentItem = 0;
            mViewPager.setCurrentItem(0);
            showImageInfo(0);
        }
    }

    private void showImageInfo(int position) {
        Image image = mPagerAdapter.getImage(position);
        int total = mPagerAdapter.getCount();

        String imagePosition = getString(R.string.percent_d_d, position + 1, total);
        mBrief.setText(imagePosition);

        // Resolution
        ImageService.getInstance()
                .loadImageInfo(image.getFile())
                .compose(workAndShow())
                .subscribe(imageFileInformation -> {
                    String resText = null;

                    Size mediaResolution = imageFileInformation.getMediaResolution();
                    resText = MediaUtils.getResolutionString(this, mediaResolution);

                    mResolution.setText(resText);

                }, throwable -> {
                    mResolution.setText("-");
                });

        // Title : file name
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(image.getFile().getName());

            // Subtitle : index
            //actionBar.setSubtitle(imagePosition);
        }
    }

    @NonNull
    private ImageFragmentStatePagerAdapter imageListToImageListAdapter(List<MediaFile> mediaFiles) {
        ImageFragmentStatePagerAdapter adapter = new ImageFragmentStatePagerAdapter(getSupportFragmentManager());
        adapter.setImages(imagesToImages(mediaFiles));
        return adapter;
    }

    private List<DefaultImageListAdapter.Item> imagesToItems(List<MediaFile> mediaFiles) {
        return Stream.of(mediaFiles)
                .map(image -> {
                    DefaultImageListAdapter.Item item = new DefaultImageListAdapter.Item();
                    item.setFile(image.getFile());
                    return item;
                })
                .toList();
    }

    private List<Image> imagesToImages(List<MediaFile> mediaFiles) {
        return Stream.of(mediaFiles)
                .map(image -> new Image().setFile(image.getFile()))
                .toList();
    }

    private void loadItem() {
        // Set the title TextView to the item's name and author
        //mHeaderTitle.setText(getResolutionString(R.string.image_header, mItem.getName(), mItem.getAuthor()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && addTransitionListener()) {
            // If we're running on Lollipop and we have added a listener to the shared element
            // transition, load the thumbnail. The listener will load the full-size image when
            // the transition is complete.
            loadThumbnail();
        } else {
            // If all other cases we should just load the full-size image now
            loadFullSizeImage();
        }
    }

    /**
     * Try and add a {@link Transition.TransitionListener} to the entering shared element
     * {@link Transition}. We do this so that we can load the full-size image after the transition
     * has completed.
     *
     * @return true if we were successful in adding a listener to the enter transition
     */
    private boolean addTransitionListener() {
        final Transition transition = getWindow().getSharedElementEnterTransition();

        if (transition != null) {
            // There is an entering shared element transition so add a listener to it
            transition.addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    // As the transition has ended, we can now load the full-size image
                    loadFullSizeImage();

                    // Make sure we remove ourselves as a listener
                    transition.removeListener(this);
                }

                @Override
                public void onTransitionStart(Transition transition) {
                    // No-op
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                    // Make sure we remove ourselves as a listener
                    transition.removeListener(this);
                }

                @Override
                public void onTransitionPause(Transition transition) {
                    // No-op
                }

                @Override
                public void onTransitionResume(Transition transition) {
                    // No-op
                }
            });
            return true;
        }

        // If we reach here then we have not added a listener
        return false;
    }


    /**
     * Load the item's thumbnail image into our {@link ImageView}.
     */
    private void loadThumbnail() {
        Glide.with(mPhotoView.getContext())
                .load(mImages.get(0).getFile())
                .into(mPhotoView);
    }

    /**
     * Load the item's full-size image into our {@link ImageView}.
     */
    private void loadFullSizeImage() {

        Glide.with(mPhotoView.getContext())
                .load(mImages.get(0).getFile())
                .apply(RequestOptions.noAnimation())
                .into(mPhotoView);
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        mImageFilePath = intent.getStringExtra(EXTRA_IMAGE_FILE_PATH);

        mDirPath = intent.getStringExtra(EXTRA_IMAGE_DIR_PATH);
        mItemPosition = intent.getIntExtra(EXTRA_IMAGE_ITEM_POSITION, -1);
//        mViewMode = (ViewMode) intent.getSerializableExtra(EXTRA_VIEW_MODE);

        mSortWay = (SortWay) intent.getSerializableExtra(EXTRA_SORT_WAY);
        mSortOrder = (SortOrder) intent.getSerializableExtra(EXTRA_SORT_ORDER);
    }

    private void showRandomImage() {
        mBrief.setText(mImageFilePath);
//        ImageService.getInstance().loadRandomImage()
        Observable.just(mImageFilePath)
                .map(File::new)
                .compose(workAndShow())
                .map(file -> new Image().setFile(file))
                .subscribe(image -> {
                    LinkedList<Image> images = new LinkedList<>();
                    images.add(image);

                    ImagePagerAdapter imagePagerAdapter = new ImagePagerAdapter(images);
                    imagePagerAdapter.setOnClickImageListener(image12 -> {
                        Log.d(TAG, "showRandomImage() called");
                    });
                    imagePagerAdapter.setOnLongClickImageListener(image1 -> {
                        return false;
                    });

                    mViewPager.setAdapter(imagePagerAdapter);
                    mViewPager.setCurrentItem(0);
                });

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    private static class Image {
        File mFile;
        Type mType;
        int mViewId;

        public Image setViewId(int viewId) {
            mViewId = viewId;
            return this;
        }

        public int getViewId() {
            return mViewId;
        }

        public File getFile() {
            return mFile;
        }

        public Image setFile(File file) {
            mFile = file;
            return this;
        }

        public Image setType(Type type) {
            this.mType = type;
            return this;
        }

        enum Type {
            GIF,
            BITMAP
        }

        boolean isGif() {
            return mFile != null && FilenameUtils.isExtension(mFile.getName(), "gif");
        }

        boolean isBitmap() {
            return !isGif();
        }
    }

    private static class ImagePagerAdapter extends PagerAdapter {

        public interface OnClickImageListener {
            void onClick(Image image);
        }

        public interface OnLongClickImageListener {
            boolean onLongClick(Image image);
        }

        List<Image> mImages = new LinkedList<>();

        ImagePagerAdapter(List<Image> images) {
            if (images != null) {
                mImages = images;
            }
        }

        int getViewId(int position) {
            if (position < 0 || position >= mImages.size()) {
                throw new IllegalArgumentException("Invalid position '" + position + "'.");
            }
            Image image = mImages.get(position);
            return image.getViewId();
        }

        public OnClickImageListener getOnClickImageListener() {
            return mOnClickImageListener;
        }

        public ImagePagerAdapter setOnClickImageListener(OnClickImageListener onClickImageListener) {
            mOnClickImageListener = onClickImageListener;
            return this;
        }

        public OnLongClickImageListener getOnLongClickImageListener() {
            return mOnLongClickImageListener;
        }

        public ImagePagerAdapter setOnLongClickImageListener(OnLongClickImageListener onLongClickImageListener) {
            mOnLongClickImageListener = onLongClickImageListener;
            return this;
        }

        OnClickImageListener mOnClickImageListener;
        OnLongClickImageListener mOnLongClickImageListener;

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.d(TAG, "instantiateItem() called with: container = [" + container + "], position = [" + position + "]");

            PhotoView view;

            Image image = mImages.get(position);

            if (image.isBitmap()) {
                PhotoView photoView = new PhotoView(container.getContext());
                int id = View.generateViewId();
                image.setViewId(id);
                photoView.setId(id);

                photoView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                photoView.setScaleType(ImageView.ScaleType.CENTER);
                // Now just add PhotoView to ViewPager and return it
                container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                // 加载原始图片
                Glide.with(container.getContext())
                        //.asBitmap()
                        .load(image.getFile())
                        .apply(RequestOptions.fitCenterTransform())
                        //.transition(BitmapTransitionOptions.withCrossFade())
                        .into(photoView);

                view = photoView;

            } else if (image.isGif()) {

//                Observable.just(image)
//                        .map(image1 -> image.getFile())
//                        .map(GifDrawable::new);


                GifDrawable drawable;
                try {
                    drawable = new GifDrawable(image.getFile());

                    PhotoView gifImageView = new PhotoView(container.getContext());
                    gifImageView.setImageDrawable(drawable);
                    gifImageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    gifImageView.setScaleType(ImageView.ScaleType.CENTER);
                    container.addView(gifImageView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                    view = gifImageView;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {

                return null;
            }

            // 图片点击和长按事件
            view.setOnViewTapListener((view1, x, y) -> {
                if (mOnClickImageListener != null) {
                    mOnClickImageListener.onClick(image);
                }
            });

            view.setOnLongClickListener(v -> mOnLongClickImageListener != null && mOnLongClickImageListener.onLongClick(image));

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

    /**
     * 大量图片列表适配器
     */
    private class ImageFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

        List<Image> mImages = new LinkedList<>();

        boolean removeItem(int position) {
            if (!(position >= 0 && position < mImages.size())) {
                throw new IllegalArgumentException("Invalid position " + position);
            }

            mImages.remove(position);
            notifyDataSetChanged();

            return true;
        }

        Image getImage(int position) {
            if (position < 0 || position >= mImages.size()) {
                throw new IndexOutOfBoundsException(String.format("Position %d out of bounds(0,%d)", position, mImages.size() - 1));
            }
            return mImages.get(position);
        }

        int mAnimatedItemPosition = -1;
        private SparseArray<ImageFragment> registeredFragments = new SparseArray<>();

        public ImageFragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }

        public ImageFragmentStatePagerAdapter setAnimatedItemPosition(int animatedItemPosition) {
            mAnimatedItemPosition = animatedItemPosition;
            return this;
        }

        ImageFragmentStatePagerAdapter setImages(List<Image> images) {
            if (images != null) {
                mImages = images;
            }
            return this;
        }

        ImageFragmentStatePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getItemPosition(Object object) {
            Fragment fragment = (Fragment) object;
            if (fragment instanceof ImageFragment) {
                return POSITION_NONE;
            }
            return super.getItemPosition(object);
        }

        @Override
        public Fragment getItem(int position) {
            Image image = mImages.get(position);

            Log.d(TAG, "get fragment item for file " + image.getFile());

            String absolutePath = image.getFile().getAbsolutePath();
            String transitionName = TransitionUtils.generateTransitionName(absolutePath);
            String fileTypeTransitionName = TransitionUtils.generateTransitionName(TransitionUtils.TRANSITION_PREFIX_FILETYPE, absolutePath);

            boolean performEnterTransition = position == mAnimatedItemPosition;
            if (performEnterTransition) {
                Log.d(TAG, "get fragment item : PerformEnterTransition for position " + position + " " + transitionName);
            }

            ImageFragment fragment = ImageFragment.newInstance(absolutePath, transitionName, fileTypeTransitionName, performEnterTransition);
            registeredFragments.put(position, fragment);

            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Log.d(TAG, "remove fragment at position " + position + " " + object);
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return mImages.size();
        }

        public void removeFile(File file) {
            int i = org.apache.commons.collections4.ListUtils.indexOf(mImages, image -> SystemUtils.isSameFile(image.getFile(), file));
            if (i != -1) {
                mImages.remove(i);
                notifyDataSetChanged();
            }
        }
    }
}

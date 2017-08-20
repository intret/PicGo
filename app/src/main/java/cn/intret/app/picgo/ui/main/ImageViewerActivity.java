package cn.intret.app.picgo.ui.main;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.transition.Transition;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.jaeger.library.StatusBarUtil;

import org.apache.commons.io.FilenameUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.RemoveFileMessage;
import cn.intret.app.picgo.model.SystemImageService;
import cn.intret.app.picgo.ui.adapter.ImageListAdapter;
import cn.intret.app.picgo.ui.adapter.ImageTransitionNameGenerator;
import cn.intret.app.picgo.ui.event.CurrentImageChangeMessage;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.SystemUtils;
import cn.intret.app.picgo.utils.ToastUtils;
import io.reactivex.Observable;
import pl.droidsonroids.gif.GifDrawable;

/**
 * Image viewer with view pager.
 *
 * @implNote FIXED problems at : https://stackoverflow.com/questions/30628543/fragment-shared-element-transitions-dont-work-with-viewpager
 */
public class ImageViewerActivity extends BaseAppCompatActivity implements ImageFragment.OnFragmentInteractionListener {

    private static final String TAG = ImageViewerActivity.class.getSimpleName();
    private static final String EXTRA_IMAGE_FILE_PATH = "extra:file_name";
    private static final String EXTRA_IMAGE_DIR_PATH = "extra:dir_path";
    private static final String EXTRA_IMAGE_ITEM_POSITION = "extra:image_item_position";

    private static final String EXTRA_TRANSITION_NAME = "transition_name";
    private static final String EXTRA_PARAM_FILE_PATH = "viewer:param:filepath";
    public static final String TRANSITION_NAME_IMAGE = "viewer:image";

    @BindView(R.id.viewpager) ViewPager mViewPager;
        @BindView(R.id.brief) TextView mBrief;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportPostponeEnterTransition();

        setContentView(R.layout.activity_image_viewer);
        initStatusBar();

        ButterKnife.bind(this);

        extractIntentData();

        showImageFile();

        initImageTransition();
    }

    private void initStatusBar() {
        StatusBarUtil.setTranslucent(this, 255);

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
                    String transitionName = ImageTransitionNameGenerator.generateTransitionName(absolutePath);
                    names.clear();
                    names.add(transitionName);

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
                            Log.e(TAG, "imageView enter onMapSharedElements: cannot get PhotoView instance." );
                        }
                    }
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
                    String transitionName = ImageTransitionNameGenerator.generateTransitionName(item.getFile().getAbsolutePath());
                    sharedElements.put(transitionName, ((ImageFragment) mPagerAdapter.getItem(currentItem)).getImage());
                }
                Log.d(TAG, "imageView enter after onMapSharedElements() called with: names = [" + names + "], sharedElements = [" + sharedElements + "]");
                super.onMapSharedElements(names, sharedElements);
            }
        });
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

    public static Intent newIntentViewFile(Context context, File file, String transitionName) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(EXTRA_IMAGE_FILE_PATH, file.getAbsolutePath());
        intent.putExtra(EXTRA_TRANSITION_NAME, transitionName);
        return intent;
    }

    public static Intent newIntentViewFileList(Context context, String dirAbsolutePath, int itemModelPosition,
                                               String transitionName) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(EXTRA_IMAGE_DIR_PATH, dirAbsolutePath);
        intent.putExtra(EXTRA_IMAGE_ITEM_POSITION, itemModelPosition);

        intent.putExtra(EXTRA_TRANSITION_NAME, transitionName);
        return intent;
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
//                ToastUtils.toastLong(this, getString(R.string.already_removed_d_files, 1));
//
//            } catch (Throwable throwable) {
//
//                ToastUtils.toastLong(this, getString(R.string.remove_file_failed));
//            }
//
//        }
    }

    private void showImageFile() {

        if (mDirPath != null && mItemPosition != -1) {

            SystemImageService.getInstance()
                    .loadImageList(new File(mDirPath), true)
                    .compose(workAndShow())
                    .map(this::imageListToImageListAdapter)
                    .subscribe(adapter -> {
                        showImageAdapter(adapter, mItemPosition);
                    }, throwable -> {

                    });

        } else if (mImageFilePath != null){

            Observable.just(mImageFilePath)
                    .map(File::new)
                    .map(file -> new cn.intret.app.picgo.model.Image().setFile(file).setDate(new Date(file.lastModified())))
                    .map(ListUtils::objectToLinkedList)
                    .map(this::imageListToImageListAdapter)
                    .subscribe(adapter -> {
                        showImageAdapter(adapter, 0);
                    })
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

                Image image = adapter.getImage(position);
                showImageBriefInfo(position);

                EventBus.getDefault().post(new CurrentImageChangeMessage().setPosition(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if (position != -1) {
            mCurrentItem = position;
            mViewPager.setCurrentItem(position);

            showImageBriefInfo(position);
        } else {
            mCurrentItem = 0;
            mViewPager.setCurrentItem(0);
            showImageBriefInfo(0);
        }
    }

    private void showImageBriefInfo(int position) {
        Image image = mPagerAdapter.getImage(position);
        mBrief.setText(image.getFile().getAbsolutePath());
    }

    @NonNull
    private ImageFragmentStatePagerAdapter imageListToImageListAdapter(List<cn.intret.app.picgo.model.Image> images) {
        ImageFragmentStatePagerAdapter adapter = new ImageFragmentStatePagerAdapter(getSupportFragmentManager());
        adapter.setImages(imagesToImages(images));
        return adapter;
    }

    private List<ImageListAdapter.Item> imagesToItems(List<cn.intret.app.picgo.model.Image> images) {
        return Stream.of(images)
                .map(image -> new ImageListAdapter.Item().setFile(image.getFile()))
                .toList();
    }

    private List<Image> imagesToImages(List<cn.intret.app.picgo.model.Image> images) {
        return Stream.of(images)
                .map(image -> new Image().setFile(image.getFile()))
                .toList();
    }

    private void loadItem() {
        // Set the title TextView to the item's name and author
        //mHeaderTitle.setText(getString(R.string.image_header, mItem.getName(), mItem.getAuthor()));

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

        mTransitionName = intent.getStringExtra(EXTRA_TRANSITION_NAME);
    }

    private void showRandomImage() {
        mBrief.setText(mImageFilePath);
//        SystemImageService.getInstance().loadRandomImage()
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
                        .asBitmap()
                        .load(image.getFile())
                        .apply(RequestOptions.fitCenterTransform())
                        .transition(BitmapTransitionOptions.withCrossFade())
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
            String transitionName = ImageTransitionNameGenerator.generateTransitionName(absolutePath);
            boolean performEnterTransition = position == mAnimatedItemPosition;
            if (performEnterTransition) {
                Log.d(TAG, "get fragment item : PerformEnterTransition for position " + position + " " + transitionName);
            }

            ImageFragment fragment = ImageFragment.newInstance(absolutePath, transitionName, performEnterTransition);
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

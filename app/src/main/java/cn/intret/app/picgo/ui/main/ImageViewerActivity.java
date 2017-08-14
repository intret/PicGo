package cn.intret.app.picgo.ui.main;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.transition.Transition;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.widget.HackyViewPager;
import io.reactivex.Observable;
import pl.droidsonroids.gif.GifDrawable;


public class ImageViewerActivity extends BaseAppCompatActivity implements ImageFragment.OnFragmentInteractionListener {

    private static final String TAG = "ImageView";
    private static final String EXTRA_FILE_NAME = "EXTRA_FILE_NAME";
    public static final String EXTRA_PARAM_FILE_PATH = "viewer:param:filepath";
    public static final String TRANSITION_NAME_IMAGE = "viewer:image";

        @BindView(R.id.viewpager) HackyViewPager mViewPager;
        @BindView(R.id.brief) TextView mBrief;
        private PagerAdapter mImageAdapter;
        private String mFile;
        private ImagePagerAdapter mImagePagerAdapter;
        private LinkedList<Image> mImages;
        private PhotoView mPhotoView;
    private ImageFragmentStatePagerAdapter mPagerAdapter;

    public static Intent newIntentViewFile(Context context, File file) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(EXTRA_FILE_NAME, file.getAbsolutePath());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // https://stackoverflow.com/questions/30628543/fragment-shared-element-transitions-dont-work-with-viewpager
        ActivityCompat.postponeEnterTransition(this);

        setContentView(R.layout.activity_image_viewer);

        ButterKnife.bind(this);

        extractIntentData();

        {
            Observable.just(mFile)
                    .map(File::new)
                    .map(file -> new Image().setFile(file))
                    .subscribe(image -> {
                        mImages = new LinkedList<>();
                        mImages.add(image);

                        mPagerAdapter = new ImageFragmentStatePagerAdapter(getSupportFragmentManager());
                        mPagerAdapter.setImages(mImages);

//                        mImagePagerAdapter = new ImagePagerAdapter(mImages);
                        mViewPager.setAdapter(mPagerAdapter);
                        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                            @Override
                            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                            }

                            @Override
                            public void onPageSelected(int position) {

                            }

                            @Override
                            public void onPageScrollStateChanged(int state) {

                            }
                        });

                        mViewPager.setCurrentItem(0);
                    })
            ;
        }

//        int id = mImagePagerAdapter.getViewId(0);
//
//        mPhotoView = (PhotoView) findViewById(id);
//        if (mPhotoView != null) {
//            ViewCompat.setTransitionName(mPhotoView, TRANSITION_NAME_IMAGE);
//            loadItem();
//        } else {
//            Log.e(TAG, String.format("onCreate: cannot found view id %d.", id));
//        }
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
        mFile = intent.getStringExtra(EXTRA_FILE_NAME);
    }

    @Override
    protected void onStart() {
        super.onStart();

//        showRandomImage();
    }

    private void showRandomImage() {
        mBrief.setText(mFile);
//        SystemImageService.getInstance().loadRandomImage()
        Observable.just(mFile)
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

    static class Image {
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

    static class ImagePagerAdapter extends PagerAdapter {

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
    class ImageFragmentStatePagerAdapter extends FragmentStatePagerAdapter {

        List<Image> mImages = new LinkedList<>();

        public ImageFragmentStatePagerAdapter setImages(List<Image> images) {
            if (images != null) {
                mImages = images;
            }
            return this;
        }

        public ImageFragmentStatePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Image image = mImages.get(position);

            return ImageFragment.newInstance(image.getFile().getAbsolutePath(), TRANSITION_NAME_IMAGE);
        }

        @Override
        public int getCount() {
            return mImages.size();
        }
    }
}

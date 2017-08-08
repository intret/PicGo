package cn.intret.app.picgo.ui.main;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.SystemImageService;
import cn.intret.app.picgo.widget.HackyViewPager;
import io.reactivex.Observable;
import pl.droidsonroids.gif.GifDrawable;

public class ImageViewerActivity extends BaseAppCompatActivity {

    private static final String TAG = "ImageView";
    private static final String EXTRA_FILE_NAME = "EXTRA_FILE_NAME";

    @BindView(R.id.viewpager) HackyViewPager mViewPager;
    @BindView(R.id.brief) TextView mBrief;
    private android.support.v4.view.PagerAdapter mImageAdapter;
    private String mFile;

    public static Intent newIntentViewSingleFile(Context context, File file) {
        Intent intent = new Intent(context, ImageViewerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(EXTRA_FILE_NAME, file.getAbsolutePath());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ButterKnife.bind(this);

        extractIntentData();
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        mFile = intent.getStringExtra(EXTRA_FILE_NAME);
    }

    @Override
    protected void onStart() {
        super.onStart();

        showRandomImage();
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

    static class Image {
        File mFile;
        Type mType;

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

    static class ImagePagerAdapter extends android.support.v4.view.PagerAdapter {

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
            PhotoView view;

            Image image = mImages.get(position);

            if (image.isBitmap()) {
                PhotoView photoView = new PhotoView(container.getContext());

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

    class PagerAdapter extends FragmentStatePagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return null;
        }

        @Override
        public int getCount() {
            return 0;
        }
    }
}

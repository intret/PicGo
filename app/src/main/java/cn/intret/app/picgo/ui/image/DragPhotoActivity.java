package cn.intret.app.picgo.ui.image;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

import cn.intret.app.picgo.R;

public class DragPhotoActivity extends AppCompatActivity {
    public static final java.lang.String EXTRA_FILE_PATH = "extra_file_path";

    public static final String EXTRA_LEFT = "left";
    public static final String EXTRA_TOP = "top";
    public static final String EXTRA_HEIGHT = "height";
    public static final String EXTRA_WIDTH = "width";
    public static final String EXTRA_FULLSCREEN = "fullscreen";

    private ViewPager mViewPager;
    private List<String> mList;
    private DragPhotoView[] mPhotoViews;

    int mOriginLeft;
    int mOriginTop;
    int mOriginHeight;
    int mOriginWidth;
    int mOriginCenterX;
    int mOriginCenterY;
    private float mTargetHeight;
    private float mTargetWidth;
    private float mScaleX;
    private float mScaleY;
    private float mTranslationX;
    private float mTranslationY;
    private String mFilePath;
    private boolean mFullScreen;
    private int mAnimationDuration = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        {
            Bundle extras = getIntent().getExtras();
            mFilePath = extras.getString(EXTRA_FILE_PATH);
            mOriginLeft = extras.getInt(EXTRA_LEFT);
            mOriginTop = extras.getInt(EXTRA_TOP);
            mOriginHeight = extras.getInt(EXTRA_HEIGHT);
            mOriginWidth = extras.getInt(EXTRA_WIDTH);
            mFullScreen = extras.getBoolean(EXTRA_FULLSCREEN);

            mOriginCenterX = mOriginLeft + mOriginWidth / 2;
            mOriginCenterY = mOriginTop + mOriginHeight / 2;
        }
        if (mFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        setContentView(R.layout.activity_drag_photo);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimary));
        }
        mViewPager = findViewById(R.id.viewpager);


        mList = new ArrayList<>();

        mList.add(mFilePath);

        mPhotoViews = new DragPhotoView[mList.size()];

        for (int i = 0; i < mPhotoViews.length; i++) {
            DragPhotoView photoView;

            photoView = (DragPhotoView) View.inflate(this, R.layout.item_viewpager, null);
            //photoView.setImageURI(Uri.fromFile(new File(mPerformEnterTransition)));

            mPhotoViews[i] = photoView;
            // TODO: 更换图片

            photoView.setOnTapListener(view -> finishWithAnimation());

            photoView.setOnExitListener((view, x, y, w, h) -> performExitAnimation(view, x, y, w, h));
        }

        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return mList.size();
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(mPhotoViews[position]);
                return mPhotoViews[position];
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(mPhotoViews[position]);
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }
        });

        mViewPager.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mViewPager.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        int[] location = new int[2];

                        final DragPhotoView photoView = mPhotoViews[0];
                        photoView.getLocationOnScreen(location);

                        Glide.with(DragPhotoActivity.this)
                                .asDrawable()
                                .load(mFilePath)
                                .apply(RequestOptions.centerInsideTransform())

                                .into(photoView);


                        mTargetHeight = (float) photoView.getHeight();
                        mTargetWidth = (float) photoView.getWidth();
                        mScaleX = (float) mOriginWidth / mTargetWidth;
                        mScaleY = (float) mOriginHeight / mTargetHeight;

                        float targetCenterX = location[0] + mTargetWidth / 2;
                        float targetCenterY = location[1] + mTargetHeight / 2;

                        mTranslationX = mOriginCenterX - targetCenterX;
                        mTranslationY = mOriginCenterY - targetCenterY;
                        photoView.setTranslationX(mTranslationX);
                        photoView.setTranslationY(mTranslationY);

                        photoView.setScaleX(mScaleX);
                        photoView.setScaleY(mScaleY);

                        performEnterAnimation();

                        for (int i = 0; i < mPhotoViews.length; i++) {
                            mPhotoViews[i].setMinScale(mScaleX);
                        }
                    }
                });
    }

    /**
     * ===================================================================================
     * <p>
     * 底下是低版本"共享元素"实现   不需要过分关心  如有需要 可作为参考.
     * <p>
     * Code  under is shared transitions in all android versions implementation
     */
    private void performExitAnimation(final DragPhotoView view, float x, float y, float w, float h) {
        view.finishAnimationCallBack();
        float viewX = mTargetWidth / 2 + x - mTargetWidth * mScaleX / 2;
        float viewY = mTargetHeight / 2 + y - mTargetHeight * mScaleY / 2;
        view.setX(viewX);
        view.setY(viewY);

        float centerX = view.getX() + mOriginWidth / 2;
        float centerY = view.getY() + mOriginHeight / 2;

        float translateX = mOriginCenterX - centerX;
        float translateY = mOriginCenterY - centerY;


        ValueAnimator translateXAnimator = ValueAnimator.ofFloat(view.getX(), view.getX() + translateX);
        translateXAnimator.addUpdateListener(valueAnimator -> view.setX((Float) valueAnimator.getAnimatedValue()));
        translateXAnimator.setDuration(mAnimationDuration);
        translateXAnimator.start();
        ValueAnimator translateYAnimator = ValueAnimator.ofFloat(view.getY(), view.getY() + translateY);
        translateYAnimator.addUpdateListener(valueAnimator -> view.setY((Float) valueAnimator.getAnimatedValue()));
        translateYAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                finish();
                overridePendingTransition(0, 0);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        translateYAnimator.setDuration(mAnimationDuration);
        translateYAnimator.start();
    }

    private void finishWithAnimation() {

        final DragPhotoView photoView = mPhotoViews[0];
        ValueAnimator translateXAnimator = ValueAnimator.ofFloat(0, mTranslationX);
        translateXAnimator.addUpdateListener(valueAnimator -> photoView.setX((Float) valueAnimator.getAnimatedValue()));

        translateXAnimator.setDuration(mAnimationDuration);
        translateXAnimator.start();

        ValueAnimator translateYAnimator = ValueAnimator.ofFloat(0, mTranslationY);
        translateYAnimator.addUpdateListener(valueAnimator -> photoView.setY((Float) valueAnimator.getAnimatedValue()));
        translateYAnimator.setDuration(mAnimationDuration);
        translateYAnimator.start();

        ValueAnimator scaleYAnimator = ValueAnimator.ofFloat(1, mScaleY);
        scaleYAnimator.addUpdateListener(valueAnimator -> photoView.setScaleY((Float) valueAnimator.getAnimatedValue()));
        scaleYAnimator.setDuration(mAnimationDuration);
        scaleYAnimator.start();

        ValueAnimator scaleXAnimator = ValueAnimator.ofFloat(1, mScaleX);
        scaleXAnimator.addUpdateListener(valueAnimator -> photoView.setScaleX((Float) valueAnimator.getAnimatedValue()));

        scaleXAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                finish();
                overridePendingTransition(0, 0);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        scaleXAnimator.setDuration(mAnimationDuration);
        scaleXAnimator.start();
    }

    private void performEnterAnimation() {
        final DragPhotoView photoView = mPhotoViews[0];
        ValueAnimator translateXAnimator = ValueAnimator.ofFloat(photoView.getX(), 0);
        translateXAnimator.addUpdateListener(valueAnimator -> photoView.setX((Float) valueAnimator.getAnimatedValue()));
        translateXAnimator.setDuration(mAnimationDuration);
        translateXAnimator.start();

        ValueAnimator translateYAnimator = ValueAnimator.ofFloat(photoView.getY(), 0);
        translateYAnimator.addUpdateListener(valueAnimator -> photoView.setY((Float) valueAnimator.getAnimatedValue()));
        translateYAnimator.setDuration(mAnimationDuration);
        translateYAnimator.start();

        ValueAnimator scaleYAnimator = ValueAnimator.ofFloat(mScaleY, 1);
        scaleYAnimator.addUpdateListener(valueAnimator -> photoView.setScaleY((Float) valueAnimator.getAnimatedValue()));
        scaleYAnimator.setDuration(mAnimationDuration);
        scaleYAnimator.start();

        ValueAnimator scaleXAnimator = ValueAnimator.ofFloat(mScaleX, 1);
        scaleXAnimator.addUpdateListener(valueAnimator -> photoView.setScaleX((Float) valueAnimator.getAnimatedValue()));
        scaleXAnimator.setDuration(mAnimationDuration);
        scaleXAnimator.start();
    }

    @Override
    public void onBackPressed() {
        finishWithAnimation();
    }
}

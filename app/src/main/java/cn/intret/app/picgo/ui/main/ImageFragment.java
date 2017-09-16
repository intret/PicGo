package cn.intret.app.picgo.ui.main;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.orhanobut.logger.Logger;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.SystemImageService;
import cn.intret.app.picgo.ui.event.CancelExitTransitionMessage;
import cn.intret.app.picgo.ui.event.ImageFragmentSelectionChangeMessage;
import cn.intret.app.picgo.utils.PathUtils;
import cn.intret.app.picgo.utils.ToastUtils;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ImageFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageFragment extends Fragment {

    public static final String ARG_FILE_PATH = "arg_file_path";
    private static final String ARG_IMAGE_TRANSITION_NAME = "arg_transition_name";
    private static final String ARG_FILE_TYPE_TRANSITION_NAME = "arg_file_type_transition_name";
    private static final String TAG = "ImageFragment";
    private static final String ARG_PERFORM_ENTER_TRANSITION = "arg_perform_enter_transition";

    // TODO: Rename and change types of parameters
    private String mFilePath;

    private OnFragmentInteractionListener mListener;
    private boolean mIsLoaded = false;

    @BindView(R.id.image) PhotoView mImage;
    @BindView(R.id.gif_image) GifImageView mGifImageView;
    @BindView(R.id.file_type) ImageView mFileType;
    @BindView(R.id.empty_view) View mEmptyView;
    @BindView(R.id.empty_layout) View mEmptyLayout;
    @BindView(R.id.image_container) ViewGroup mRootLayout;

    private String mImageTransitionName;
    private String mFileTypeTransitionName;
    private boolean mPerformEnterTransition = false;
    private boolean mPerformExitTransition = true;
    private boolean mAlreadyPerformAnimation = false;

    public PhotoView getImage() {
        return mImage;
    }
    public ImageView getFileType() {
        return mFileType;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFilePath = getArguments().getString(ARG_FILE_PATH);
            mPerformEnterTransition = getArguments().getBoolean(ARG_PERFORM_ENTER_TRANSITION);
            mImageTransitionName = getArguments().getString(ARG_IMAGE_TRANSITION_NAME);
            mFileTypeTransitionName = getArguments().getString(ARG_FILE_TYPE_TRANSITION_NAME);

            Log.d(TAG, "onCreate: create fragment for file " + mFilePath + ", transition name :" + mImageTransitionName);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_image, container, false);
        ButterKnife.bind(this, rootView);

        //if (mPerformEnterTransition) {
        tryToSetTransitionNameFromIntent();
        //}

        mImage.setOnClickListener(v -> {
            handleClickEvent();
        });

        mRootLayout.setOnClickListener(v -> {
            handleClickEvent();
        });

        mImage.setOnLongClickListener(v -> {
            showImageDialog();
            return true;
        });

        SimpleTarget<Drawable> target = new SimpleTarget<Drawable>() {
            @Override
            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                mImage.setImageDrawable(resource);
            }
        };

        ViewTarget<ImageView, Drawable> viewTarget = new ViewTarget<ImageView, Drawable>(mImage) {
            @Override
            public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                getView().setImageDrawable(resource);
            }

            @Override
            public void onStart() {
                Log.d(TAG, "onStart() called");
                super.onStart();
            }
        };


//        ActivityCompat.startPostponedEnterTransition(activity);
        if (mPerformEnterTransition) {

            //scheduleStartPostponedTransition(mImage);
        }

        mImage.setScaleType(ImageView.ScaleType.FIT_CENTER);


        if (PathUtils.isGifFile(mFilePath)) {
            try {

                GifDrawable gifDrawable = new GifDrawable(mFilePath);
                mImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                mImage.setImageDrawable(gifDrawable);

                scheduleStartPostponedTransition(mImage);
                gifDrawable.start();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {

            RequestBuilder<Drawable> request = Glide.with(this)
                    .asDrawable()
                    .load(mFilePath)
                    .apply(RequestOptions.skipMemoryCacheOf(false))
                    .apply(RequestOptions.fitCenterTransform())
                    .transition(DrawableTransitionOptions.withCrossFade());

            if (PathUtils.isStaticImageFile(mFilePath) || PathUtils.isVideoFile(mFilePath)) {
                request.listener(new ImageRequestObserver(mPerformEnterTransition));
                request.into(mImage);
            } else {
                scheduleStartPostponedTransition(mImage);
                request.into(mImage);
            }
        }

        // 文件类型图标
        if (PathUtils.isVideoFile(mFilePath)) {

            mFileType.setVisibility(View.VISIBLE);
            mFileType.setImageResource(R.drawable.ic_play_circle_filled_white_48px);
            mFileType.setOnClickListener(v -> {
                ToastUtils.toastShort(this.getContext(), R.string.unimplemented);
            });
        } else if (PathUtils.isGifFile(mFilePath)) {
//            mGifImageView.setImageURI();

//            mFileType.setVisibility(View.VISIBLE);
//            mFileType.setImageResource(R.drawable.ic_gif_black_48px);
//            mFileType.setOnClickListener(v -> {
//                ToastUtils.toastShort(this.getContext(), R.string.unimplemented);
//            });
        } else {
            mFileType.setVisibility(View.GONE);
        }

        return rootView;
    }

    private void showImageDialog() {
        new MaterialDialog.Builder(this.getContext())
                .items(R.array.image_viewer_context_menu_items)
                .itemsCallback((dialog, itemView, position, text) -> {
                    switch (position) {
                        case 0: // delete files
                            SystemImageService.getInstance()
                                    .removeFile(new File(mFilePath))
                                    .subscribe(aBoolean -> {
                                            if (aBoolean) {

                                            }
                                        }, throwable -> {

                                        }
                            );
                            break;
                        case 1: // rename
                            break;
                        case 2: // move
                            break;
                    }
                })
                .show();
    }

    private void handleClickEvent() {
        if (mPerformExitTransition) {
            if (getActivity() != null) {
                Logger.d("退出查看图片：%s", mFilePath);
                ActivityCompat.finishAfterTransition(getActivity());
            }
        } else {
            getActivity().finish();
        }
    }

    private class ImageRequestObserver implements RequestListener<Drawable> {

        boolean mPerformEnterTransition;

        public ImageRequestObserver(boolean performEnterTransition) {
            mPerformEnterTransition = performEnterTransition;
        }

        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            mEmptyLayout.setVisibility(View.VISIBLE);
            Log.e(TAG, "onLoadFailed: 图片加载失败 : " + e);
            starEnterAnimation();
            return true;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {

            Log.d(TAG, "onResourceReady: 图片加载成功，启动进入动画");
            starEnterAnimation();
            return false;
        }

        private void starEnterAnimation() {
            if (mPerformEnterTransition) {
                FragmentActivity activity = ImageFragment.this.getActivity();
                if (activity != null) {
                    ActivityCompat.startPostponedEnterTransition(activity);
                } else {
                    Log.e(TAG, "onResourceReady: 应该启动进入动画，但是不能获取 Activity");
                }
//                startPostponedEnterTransition();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CancelExitTransitionMessage message) {
//        mPerformExitTransition = false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ImageFragmentSelectionChangeMessage message) {
        // Clear transition name
        if (StringUtils.equals(message.getTransitionName(), mImageTransitionName)) {
            Log.d(TAG, "onEvent() 设置 transition name:" + mFilePath);
            //tryToSetTransitionNameFromIntent();
        } else {
            Log.d(TAG, "onEvent() 清理 transition name:" + mFilePath);
            //mImage.setTransitionName(null);
        }
    }


    private void tryToSetTransitionNameFromIntent() {
        if (mImageTransitionName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTransitionNamesLollipop();
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart() called " + mImageTransitionName);
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop() called " + mImageTransitionName);

        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: " + mImageTransitionName);
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach: " + mImageTransitionName);
        super.onDetach();
        mListener = null;
    }

    public ImageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param filePath               File path of image to present
     * @param imageTransitionName
     * @param fileTypeTransitionName
     *@param performEnterTransition  @return A new instance of fragment ImageFragment.
     */
    public static ImageFragment newInstance(String filePath, String imageTransitionName, String fileTypeTransitionName, boolean performEnterTransition) {
        ImageFragment fragment = new ImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        args.putString(ARG_IMAGE_TRANSITION_NAME, imageTransitionName);
        args.putString(ARG_FILE_TYPE_TRANSITION_NAME, fileTypeTransitionName);
        args.putBoolean(ARG_PERFORM_ENTER_TRANSITION, performEnterTransition);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * https://github.com/codepath/android_guides/wiki/Shared-Element-Activity-Transition
     * <p>
     * Schedules the shared element transition to be started immediately
     * after the shared element has been measured and laid out within the
     * activity's view hierarchy. Some common places where it might make
     * sense to call this method are:
     * <p>
     * (1) Inside a Fragment's onCreateView() method (if the shared element
     * lives inside a Fragment hosted by the called Activity).
     * <p>
     * (2) Inside a Picasso Callback object (if you need to wait for Picasso to
     * asynchronously load/scale a bitmap before the transition can begin).
     **/
    private void scheduleStartPostponedTransition(final View sharedElement) {
//        sharedElement.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                sharedElement.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                ActivityCompat.startPostponedEnterTransition(ImageFragment.this.getActivity());
//            }
//        });

//        ViewTreeObserver.OnPreDrawListener listener = new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
//
//                FragmentActivity activity = ImageFragment.this.getActivity();
//                if (activity != null) {
//                    Log.d(TAG, "onPreDraw: perform enter transition for " + mImageTransitionName + " " + mFilePath);
//                    ActivityCompat.startPostponedEnterTransition(activity);
//                } else {
//                    Log.e(TAG, "Cannot perform enter transition for fragment because no attached activity.");
//                }
//                return true;
//            }
//        };
        sharedElement.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawObserver(sharedElement));
    }

    class OnPreDrawObserver implements ViewTreeObserver.OnPreDrawListener {

        View mView;
        OnPreDrawObserver(View view) {
            mView = view;
        }

        @Override
        public boolean onPreDraw() {

            if (mAlreadyPerformAnimation) {
                return true;
            }

            mAlreadyPerformAnimation = true;

            ViewTreeObserver viewTreeObserver = mView.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.removeOnPreDrawListener(OnPreDrawObserver.this);

                FragmentActivity activity = ImageFragment.this.getActivity();
                if (activity != null) {
                    Log.d(TAG, "onPreDraw: perform enter transition for " + mImageTransitionName + " " + mFilePath);
                    ActivityCompat.startPostponedEnterTransition(activity);
                } else {
                    Log.w(TAG, "Cannot perform enter transition for fragment because no attached activity.");
                }
            }
            return true;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTransitionNamesLollipop() {
        if (PathUtils.isGifFile(mFilePath)) {
            mImage.setTransitionName(mImageTransitionName);
        } else {
            mImage.setTransitionName(mImageTransitionName);
        }
        mFileType.setTransitionName(mFileTypeTransitionName);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}

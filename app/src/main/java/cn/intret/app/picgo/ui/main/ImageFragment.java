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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.ui.event.CancelExitTransitionMessage;
import cn.intret.app.picgo.ui.event.ImageFragmentSelectionChangeMessage;

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
    private static final String ARG_TRANSITION_NAME = "arg_transition_name";
    private static final String TAG = "ImageFragment";
    private static final String ARG_PERFORM_ENTER_TRANSITION = "arg_perform_enter_transition";

    // TODO: Rename and change types of parameters
    private String mFilePath;

    private OnFragmentInteractionListener mListener;
    private boolean mIsLoaded = false;

    @BindView(R.id.image) PhotoView mImage;
    private String mTransitionName;
    private boolean mPerformEnterTransition = false;
    private boolean mPerformExitTransition = true;

    public PhotoView getImage() {
        return mImage;
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
            mTransitionName = getArguments().getString(ARG_TRANSITION_NAME);

            Log.d(TAG, "onCreate: create fragment for file " + mFilePath + ", transition name :" + mTransitionName);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        ButterKnife.bind(this, view);

        //if (mPerformEnterTransition) {
            tryToSetTransitionNameFromIntent();
        //}

        mImage.setOnClickListener(v -> {
            if (mPerformExitTransition) {
                ActivityCompat.finishAfterTransition(getActivity());
            } else {
                getActivity().finish();
            }
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

        if (mPerformEnterTransition) {
            scheduleStartPostponedTransition(mImage);
        }

        mImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Glide.with(this)
                .asDrawable()
                .load(mFilePath)
                .apply(RequestOptions.fitCenterTransform())
                .into(mImage);

        return view;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CancelExitTransitionMessage message) {
//        mPerformExitTransition = false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ImageFragmentSelectionChangeMessage message) {
        // Clear transition name
        if (StringUtils.equals(message.getTransitionName(), mTransitionName)) {
            Log.d(TAG, "onEvent() 设置 transition name:" + mFilePath);
            //tryToSetTransitionNameFromIntent();
        } else {
            Log.d(TAG, "onEvent() 清理 transition name:" + mFilePath);
            //mImage.setTransitionName(null);
        }
    }


    private void tryToSetTransitionNameFromIntent() {
        if (mTransitionName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTransitionNamesLollipop();
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart() called " + mTransitionName);
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop() called " + mTransitionName);

        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: " + mTransitionName);
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach: " + mTransitionName);
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
     * @param filePath            File path of image to present
     * @param imageTransitionName
     * @param performEnterTransition
     * @return A new instance of fragment ImageFragment.
     */
    public static ImageFragment newInstance(String filePath, String imageTransitionName, boolean performEnterTransition) {
        ImageFragment fragment = new ImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        args.putString(ARG_TRANSITION_NAME, imageTransitionName);
        args.putBoolean(ARG_PERFORM_ENTER_TRANSITION, performEnterTransition);
        fragment.setArguments(args);
        return fragment;
    }


    /**
     * https://github.com/codepath/android_guides/wiki/Shared-Element-Activity-Transition
     *
     * Schedules the shared element transition to be started immediately
     * after the shared element has been measured and laid out within the
     * activity's view hierarchy. Some common places where it might make
     * sense to call this method are:
     *
     * (1) Inside a Fragment's onCreateView() method (if the shared element
     *     lives inside a Fragment hosted by the called Activity).
     *
     * (2) Inside a Picasso Callback object (if you need to wait for Picasso to
     *     asynchronously load/scale a bitmap before the transition can begin).
     **/
    private void scheduleStartPostponedTransition(final View sharedElement) {
//        sharedElement.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                sharedElement.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                ActivityCompat.startPostponedEnterTransition(ImageFragment.this.getActivity());
//            }
//        });

        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);

                        FragmentActivity activity = ImageFragment.this.getActivity();
                        if (activity != null) {
                            Log.d(TAG, "onPreDraw: perform enter transition for " + mTransitionName + " " + mFilePath);
                            ActivityCompat.startPostponedEnterTransition(activity);
                        } else {
                            Log.e(TAG, "Cannot perform enter transition for fragment because no attached activity.");
                        }
                        return true;
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTransitionNamesLollipop() {
        mImage.setTransitionName(mTransitionName);
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

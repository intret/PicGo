package cn.intret.app.picgo.ui.main;

import android.annotation.TargetApi;
import android.content.Context;
import android.gesture.GestureLibraries;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.github.chrisbanes.photoview.PhotoView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;

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

    // TODO: Rename and change types of parameters
    private String mFilePath;

    private OnFragmentInteractionListener mListener;
    private boolean mIsLoaded = false;

    @BindView(R.id.image)
    PhotoView mImage;
    private String mTransitionName;

    public ImageFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param filePath File path of image to present
     * @param imageTransitionName
     * @return A new instance of fragment ImageFragment.
     */
    public static ImageFragment newInstance(String filePath, String imageTransitionName) {
        ImageFragment fragment = new ImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_PATH, filePath);
        args.putString(ARG_TRANSITION_NAME, imageTransitionName);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mFilePath = getArguments().getString(ARG_FILE_PATH);
            mTransitionName = getArguments().getString(ARG_TRANSITION_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        ButterKnife.bind(this, view);

        if (mTransitionName != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            setTransitionNamesLollipop();
        }

        mImage.setOnClickListener(v -> {
            ActivityCompat.finishAfterTransition(getActivity());
        });
        return view;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setTransitionNamesLollipop() {
        mImage.setTransitionName(mTransitionName);
    }


    @Override
    public void onStart() {
        super.onStart();

        if (!mIsLoaded) {

            Glide.with(this)
                    .load(mFilePath)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions.fitCenterTransform())
                    .into(mImage);

            mIsLoaded = true;
        }

        ActivityCompat.startPostponedEnterTransition(getActivity());
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
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

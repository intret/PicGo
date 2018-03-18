package cn.intret.app.picgo.ui.image

import android.annotation.TargetApi
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import butterknife.ButterKnife
import butterknife.Unbinder
import cn.intret.app.picgo.R
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.model.user.UserModule
import cn.intret.app.picgo.ui.event.CancelExitTransitionMessage
import cn.intret.app.picgo.ui.event.ImageAnimationStartMessage
import cn.intret.app.picgo.ui.event.TapImageMessage
import cn.intret.app.picgo.utils.PathUtils
import cn.intret.app.picgo.utils.ShareUtils
import cn.intret.app.picgo.view.DismissFrameLayout
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.PhotoView
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.fragment_image.*
import kotterknife.bindView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView
import java.io.File
import java.io.IOException

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ImageFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ImageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ImageFragment : Fragment() {

    // TODO: Rename and change types of parameters
    private var mFilePath: String? = null

    private var mListener: OnFragmentInteractionListener? = null
    private val mIsLoaded = false

    internal val mDismissFrameLayout: DismissFrameLayout by bindView(R.id.dismiss_frame)
    internal val mImage: PhotoView by bindView(R.id.image)
    internal val mGifImageView: GifImageView by bindView(R.id.gif_image)
    internal val mFileType: ImageView by bindView(R.id.file_type)
    internal val mEmptyView: View by bindView(R.id.empty_view)
    internal val mEmptyLayout: View by bindView(R.id.empty_layout)
    internal val mRootLayout: ViewGroup by bindView(R.id.image_container)

    private var mImageTransitionName: String? = null
    private var mFileTypeTransitionName: String? = null
    private var mPerformEnterTransition = false
    private val mPerformExitTransition = true
    private var mAlreadyPerformAnimation = false
    var bind: Unbinder? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mFilePath = arguments!!.getString(ARG_FILE_PATH)
            mPerformEnterTransition = arguments!!.getBoolean(ARG_PERFORM_ENTER_TRANSITION)
            mImageTransitionName = arguments!!.getString(ARG_IMAGE_TRANSITION_NAME)
            mFileTypeTransitionName = arguments!!.getString(ARG_FILE_TYPE_TRANSITION_NAME)

            Log.d(TAG, "onCreate: create fragment for file $mFilePath, transition name :$mImageTransitionName")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val contentView = inflater.inflate(R.layout.fragment_image, container, false)

        bind = ButterKnife.bind(this, contentView)


        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //if (mPerformEnterTransition) {
        tryToSetTransitionNameFromIntent()
        //}

        initViews()
    }

    private fun initViews() {
        mImage.setOnClickListener { v -> handleClickEvent() }
        //
        //        mRootLayout.setOnClickListener(v -> {
        //            handleClickEvent();
        //        });

        //        mImage.setOnLongClickListener(v -> {
        //            showImageDialog();
        //            return true;
        //        });

        val target = object : SimpleTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                mImage.setImageDrawable(resource)
            }
        }

        run {
            mDismissFrameLayout.setDismissListener(object : DismissFrameLayout.OnDismissListener {
                override fun onScaleProgress(scale: Float) {
                    this@ImageFragment.mListener?.onScaleProgress(scale)
                }

                override fun onDismiss() {
                    this@ImageFragment.mListener?.onDismiss()
                }

                override fun onCancel() {
                    this@ImageFragment.mListener?.onCancel()
                }
            })
        }

        //        ActivityCompat.startPostponedEnterTransition(activity);
        if (mPerformEnterTransition) {

            //scheduleStartPostponedTransition(mImage);
        }

        mImage.scaleType = ImageView.ScaleType.FIT_CENTER
        //        mImage.setOnExitListener(new DragPhotoView.OnExitListener() {
        //            @Override
        //            public void onExit(DragPhotoView view, float translateX, float translateY, float w, float h) {
        //                Log.d(TAG, "onExit() called with: view = [" + view + "], translateX = [" + translateX + "], translateY = [" + translateY + "], w = [" + w + "], h = [" + h + "]");
        //                EventBus.getDefault().post(new DragImageExitMessage());
        //            }
        //        });
        //        mImage.setMinScale(0.90f);
        //
        //        mImage.setOnTapListener(view -> {
        //            Log.d(TAG, "onTap() called with: view = [" + view + "]");
        //
        //            EventBus.getDefault().post(new TapImageMessage());
        //        });

        if (PathUtils.isGifFile(mFilePath)) {
            try {

                val gifDrawable = GifDrawable(mFilePath!!)
                mImage.scaleType = ImageView.ScaleType.FIT_CENTER
                mImage.setImageDrawable(gifDrawable)

                scheduleStartPostponedTransition(mImage)
                gifDrawable.start()

            } catch (e: IOException) {
                e.printStackTrace()
            }

        } else {

            val request = Glide.with(this)
                    .asDrawable()
                    .load(mFilePath)
                    .apply(RequestOptions.skipMemoryCacheOf(false))
                    .apply(RequestOptions.fitCenterTransform())
                    .transition(DrawableTransitionOptions.withCrossFade())

            if (PathUtils.isStaticImageFile(mFilePath) || PathUtils.isVideoFile(mFilePath)) {
                request.listener(ImageRequestObserver(mPerformEnterTransition)
                        .setPerformEnterAnimationWhenResourceReady(true))
                //                scheduleStartPostponedTransition(mImage);
                request.into(mImage)

            } else {
                scheduleStartPostponedTransition(mImage)
                request.into(mImage)
            }
        }

        // 文件类型图标
        if (PathUtils.isVideoFile(mFilePath)) {

            mFileType.visibility = View.VISIBLE
            mFileType.setImageResource(R.drawable.ic_play_circle_filled_white_48px)
            mFileType.setOnClickListener { v ->
                playVideoFile((if (mFilePath == null) "" else mFilePath)!!)

            }
        } else if (PathUtils.isGifFile(mFilePath)) {
            //            mGifImageView.setImageURI();

            //            mFileType.setVisibility(View.VISIBLE);
            //            mFileType.setImageResource(R.drawable.ic_gif_black_48px);
            //            mFileType.setOnClickListener(v -> {
            //                ToastUtils.toastShort(this.getContext(), R.string.unimplemented);
            //            });
        } else {
            mFileType.visibility = View.GONE
        }
    }

    private fun playVideoFile(filePath: String) {
        ShareUtils.playVideo(activity, filePath)
        //        ToastUtils.toastShort(this.getContext(), R.string.unimplemented);
    }

    private fun showImageDialog() {
        MaterialDialog.Builder(this.context!!)
                .items(R.array.image_viewer_context_menu_items)
                .itemsCallback { dialog, itemView, position, text ->
                    when (position) {
                        0 // delete files
                        -> ImageModule.getInstance()
                                .removeFile(File(mFilePath!!))
                                .subscribe({ aBoolean ->
                                    if (aBoolean!!) {

                                    }
                                }
                                ) { throwable ->

                                }
                        1 // rename
                        -> {
                        }
                        2 // move
                        -> {
                        }
                    }
                }
                .show()
    }

    private fun handleClickEvent() {

        val fullscreen = UserModule.getInstance().imageClickToFullscreen
        if (fullscreen) {
            // 点击图片进入全屏
            EventBus.getDefault().post(TapImageMessage())
        } else {
            // 点击图片返回
            if (mPerformExitTransition) {
                if (activity != null) {
                    Logger.d("退出查看图片：%s", mFilePath)
                    ActivityCompat.finishAfterTransition(activity!!)
                }
            } else {
                activity!!.finish()
            }
        }
    }

    private inner class ImageRequestObserver(internal var mPerformEnterTransition: Boolean) : RequestListener<Drawable> {
        internal var mPerformEnterAnimationWhenResourceReady = false

        fun isPerformEnterAnimationWhenResourceReady(): Boolean {
            return mPerformEnterAnimationWhenResourceReady
        }

        fun setPerformEnterAnimationWhenResourceReady(performEnterAnimationWhenResourceReady: Boolean): ImageRequestObserver {
            mPerformEnterAnimationWhenResourceReady = performEnterAnimationWhenResourceReady
            return this
        }

        override fun onLoadFailed(e: GlideException?, model: Any, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
            mEmptyLayout.visibility = View.VISIBLE
            Log.e(TAG, "onLoadFailed: 图片加载失败 : " + e!!)
            starEnterAnimation()
            return true
        }

        override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
            if (mPerformEnterAnimationWhenResourceReady) {
                starEnterAnimation()
            }
            return false
        }

        private fun starEnterAnimation() {
            if (mPerformEnterTransition) {
                Log.d(TAG, "starEnterAnimation() called 启动进入动画")
                val activity = this@ImageFragment.activity
                if (activity != null) {
                    ActivityCompat.startPostponedEnterTransition(activity)

                    //                    startPostponedEnterTransition();
                    EventBus.getDefault().post(ImageAnimationStartMessage())
                    //                    StatusBarUtils.hideStatusBar(getActivity());
                } else {
                    Log.e(TAG, "onResourceReady: 应该启动进入动画，但是不能获取 Activity")
                }
                //                startPostponedEnterTransition();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: CancelExitTransitionMessage) {
        //        mPerformExitTransition = false;
    }

    private fun tryToSetTransitionNameFromIntent() {
        if (mImageTransitionName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setTransitionNamesLollipop()
        }
    }

    override fun onStart() {
        Log.d(TAG, "onStart() called " + mImageTransitionName!!)
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        Log.d(TAG, "onStop() called " + mImageTransitionName!!)

        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView: " + mImageTransitionName!!)
        bind?.unbind()
        super.onDestroyView()
    }

    override fun onDetach() {
        Log.d(TAG, "onDetach: " + mImageTransitionName!!)
        super.onDetach()
        mListener = null
    }


    /**
     * https://github.com/codepath/android_guides/wiki/Shared-Element-Activity-Transition
     *
     *
     * Schedules the shared element transition to be started immediately
     * after the shared element has been measured and laid out within the
     * activity's view hierarchy. Some common places where it might make
     * sense to call this method are:
     *
     *
     * (1) Inside a Fragment's onCreateView() method (if the shared element
     * lives inside a Fragment hosted by the called Activity).
     *
     *
     * (2) Inside a Picasso Callback object (if you need to wait for Picasso to
     * asynchronously load/scale a bitmap before the transition can begin).
     */
    private fun scheduleStartPostponedTransition(sharedElement: View) {
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
        sharedElement.viewTreeObserver.addOnPreDrawListener(OnPreDrawObserver(sharedElement))
    }

    internal inner class OnPreDrawObserver(var mView: View) : ViewTreeObserver.OnPreDrawListener {

        override fun onPreDraw(): Boolean {

            if (mAlreadyPerformAnimation) {
                return true
            }

            mAlreadyPerformAnimation = true

            val viewTreeObserver = mView.viewTreeObserver
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnPreDrawListener(this@OnPreDrawObserver)

                val activity = this@ImageFragment.activity
                if (activity != null) {
                    Log.d(TAG, "onPreDraw: perform enter transition for $mImageTransitionName $mFilePath")
                    ActivityCompat.startPostponedEnterTransition(activity)
                } else {
                    Log.w(TAG, "Cannot perform enter transition for fragment because no attached activity.")
                }
            }
            return true
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setTransitionNamesLollipop() {
        if (PathUtils.isGifFile(mFilePath)) {
            image?.transitionName = mImageTransitionName
        } else {
            image?.transitionName = mImageTransitionName
        }
        //        mFileType.setTransitionName(mFileTypeTransitionName);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener : DismissFrameLayout.OnDismissListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {

        private const val ARG_FILE_PATH = "arg_file_path"
        private const val ARG_IMAGE_TRANSITION_NAME = "arg_transition_name"
        private const val ARG_FILE_TYPE_TRANSITION_NAME = "arg_file_type_transition_name"
        private const val TAG = "ImageFragment"
        private const val ARG_PERFORM_ENTER_TRANSITION = "arg_perform_enter_transition"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param filePath               File path of image to present
         * @param imageTransitionName
         * @param fileTypeTransitionName
         * @param performEnterTransition @return A new instance of fragment ImageFragment.
         */
        fun newInstance(filePath: String, imageTransitionName: String, fileTypeTransitionName: String, performEnterTransition: Boolean): ImageFragment {
            val fragment = ImageFragment()
            val args = Bundle()
            args.putString(ARG_FILE_PATH, filePath)
            args.putString(ARG_IMAGE_TRANSITION_NAME, imageTransitionName)
            args.putString(ARG_FILE_TYPE_TRANSITION_NAME, fileTypeTransitionName)
            args.putBoolean(ARG_PERFORM_ENTER_TRANSITION, performEnterTransition)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor

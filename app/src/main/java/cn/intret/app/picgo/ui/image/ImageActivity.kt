package cn.intret.app.picgo.ui.image

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.*
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.transition.Transition
import android.util.Log
import android.util.SparseArray
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.ButterKnife
import butterknife.OnClick
import cn.intret.app.picgo.R
import cn.intret.app.picgo.model.event.RemoveFileMessage
import cn.intret.app.picgo.model.image.ImageFileInformation
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.model.image.LoadMediaFileParam
import cn.intret.app.picgo.model.image.MediaFile
import cn.intret.app.picgo.model.user.SortOrder
import cn.intret.app.picgo.model.user.SortWay
import cn.intret.app.picgo.model.user.ViewMode
import cn.intret.app.picgo.ui.adapter.TransitionUtils
import cn.intret.app.picgo.ui.adapter.brvah.DefaultImageListAdapter
import cn.intret.app.picgo.ui.base.BaseAppCompatActivity
import cn.intret.app.picgo.ui.event.CurrentImageChangeMessage
import cn.intret.app.picgo.ui.event.DragImageExitMessage
import cn.intret.app.picgo.ui.event.ImageAnimationStartMessage
import cn.intret.app.picgo.ui.event.TapImageMessage
import cn.intret.app.picgo.utils.*
import com.annimon.stream.Stream
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.chrisbanes.photoview.PhotoView
import com.orhanobut.logger.Logger
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.activity_image.*
import kotterknife.bindView
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.FastDateFormat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Image viewer with view pager.
 *
 * @implNote FIXED problems at : https://stackoverflow.com/questions/30628543/fragment-shared-element-transitions-dont-work-with-viewpager
 */
class ImageActivity : BaseAppCompatActivity(), ImageFragment.OnFragmentInteractionListener {

    internal val mToolbar: Toolbar by bindView(R.id.toolbar)

    internal val mViewPager: ViewPager by bindView(R.id.viewpager)

    internal val mBottomToolbar: ViewGroup by bindView(R.id.bottom_toolbar)

    internal val mBrief: TextView by bindView(R.id.index)

    internal val mResolution: TextView by bindView(R.id.resolution)

    internal val mBtnDelete: ImageView by bindView(R.id.btn_delete)

    internal val mBtnDetail: ImageView by bindView(R.id.btn_detail)

    internal val mPagerContainer: ViewGroup by bindView(R.id.pager_container)

    internal val mDetailRootLayout: ViewGroup by bindView(R.id.detail_container)

    internal val mBlurLayout: ImageView by bindView(R.id.blur_layout)

    internal val mDetailContainer: ViewGroup by bindView(R.id.file_detail_container)

    internal val mMaskView: View by bindView(R.id.mask_view)

    private val mImageAdapter: PagerAdapter? = null
    private var mImageFilePath: String? = null
    private val mImagePagerAdapter: ImagePagerAdapter? = null
    private val mImages: LinkedList<Image>? = null
    private val mPhotoView: PhotoView? = null
    private var mPagerAdapter: ImageFragmentStatePagerAdapter? = null
    private val mTransitionName: String? = null
    private var mDirPath: String? = null
    private var mItemPosition: Int = 0
    private val mCancelExitTransition: Boolean = false
    private var mCurrentItem = -1
    private val mViewMode: ViewMode? = null
    private var mSortWay: SortWay? = null
    private var mSortOrder: SortOrder? = null
    private val mAnimationType = AnimationType.FADE_IN_FADE_OUT
    private var mCurrentFullscreen: Boolean = false
    private var mFullscreenAnimatorSet: AnimatorSet? = null
    private var mColorDrawable: ColorDrawable? = null

    internal val showingImageFile: File?
        get() = mPagerAdapter!!.getImage(mCurrentItem).getFile()

    private val ALPHA_MAX = 0xFF

    // 保存动画过程中的值
    internal var mBottomToolbarTranslationY = 0f

    internal val isCurrentFullscreen: Boolean
        get() {
            if (mAnimationType == AnimationType.SLIDE_IN_SLIDE_OUT) {
                return mBottomToolbar.translationY == mBottomToolbar.height.toFloat()
            } else if (mAnimationType == AnimationType.FADE_IN_FADE_OUT) {
                return mBottomToolbar.alpha == 0f
            }
            return false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        extractIntentData()

        ActivityCompat.postponeEnterTransition(this)
        setContentView(R.layout.activity_image)
        ButterKnife.bind(this)

        initToolbar()

        mColorDrawable = ColorDrawable(resources.getColor(R.color.black))
        pager_container?.setBackgroundDrawable(mColorDrawable)

        loadImageFiles()
        initImageTransition()
    }

    private fun initToolbar() {
        //        DrawableCompat.setTint(mBtnDelete.getDrawable(), ContextCompat.getColor(this, R.color.white));
        //        DrawableCompat.setTint(mBtnDetail.getDrawable(), ContextCompat.getColor(this, R.color.white));

        setSupportActionBar(mToolbar)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
            actionBar.hide()
        }

        initStatusBar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            ActivityCompat.finishAfterTransition(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * https://developer.android.com/training/system-ui/status.html
     */
    private fun initStatusBar() {

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

    private fun initImageTransition() {
        ActivityCompat.setEnterSharedElementCallback(this, object : SharedElementCallback() {

            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {

                Log.d(TAG, "imageView enter before onMapSharedElements() called with: names = [$names], sharedElements = [$sharedElements]")
                if (mCurrentItem != -1) {
                    val item = mPagerAdapter!!.getImage(mCurrentItem)

                    val fragment = mPagerAdapter!!.getRegisteredFragment(mCurrentItem)

                    val absolutePath = item.getFile()!!.absolutePath
                    val transitionName = TransitionUtils.generateTransitionName(absolutePath)
                    val fileTypeTransitionName = TransitionUtils.generateTransitionName(TransitionUtils.TRANSITION_PREFIX_FILETYPE, absolutePath)

                    names.clear()
                    names.add(transitionName)
                    //names.add(fileTypeTransitionName);

                    sharedElements.clear()
                    val image = fragment.mImage
                    if (image != null) {
                        sharedElements[transitionName] = fragment.mImage
                    } else {
                        var iv: View? = null
                        val root = fragment.view
                        if (root != null) {
                            iv = root.findViewById(R.id.image)
                        }

                        if (iv != null) {
                            sharedElements[transitionName] = iv
                        } else {
                            Log.e(TAG, "imageView enter onMapSharedElements: cannot get PhotoView instance.")
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
                Log.d(TAG, "imageView enter after onMapSharedElements() called with: names = [$names], sharedElements = [$sharedElements]")
                super.onMapSharedElements(names, sharedElements)
            }
        })

        ActivityCompat.setExitSharedElementCallback(this, object : SharedElementCallback() {
            override fun onMapSharedElements(names: List<String>, sharedElements: MutableMap<String, View>) {
                Log.d(TAG, "imageView enter before onMapSharedElements() called with: names = [$names], sharedElements = [$sharedElements]")
                val currentItem = mViewPager.currentItem
                if (currentItem != -1) {
                    val item = mPagerAdapter!!.getImage(currentItem)
                    sharedElements.clear()
                    val transitionName = TransitionUtils.generateTransitionName(item.getFile()!!.absolutePath)
                    val fileTypeTransitionName = TransitionUtils.generateTransitionName(TransitionUtils.TRANSITION_PREFIX_FILETYPE, item.getFile()!!.absolutePath)

                    sharedElements[transitionName] = (mPagerAdapter!!.getItem(currentItem) as ImageFragment).mImage
                    sharedElements[fileTypeTransitionName] = (mPagerAdapter!!.getItem(currentItem) as ImageFragment).mFileType
                }
                Log.d(TAG, "imageView enter after onMapSharedElements() called with: names = [$names], sharedElements = [$sharedElements]")
                super.onMapSharedElements(names, sharedElements)
            }
        })
    }

    @OnClick(R.id.btn_delete)
    fun onClickDeleteButton(view: View) {

        ImageModule.getInstance()
                .removeFile(mPagerAdapter!!.getImage(mCurrentItem).getFile())
                .subscribe({ aBoolean ->
                    if (aBoolean!!) {
                    } else {
                        ToastUtils.toastShort(this, R.string.remove_file_failed)
                    }
                }) { throwable -> ToastUtils.toastShort(this, R.string.remove_file_failed) }

    }

    @OnClick(R.id.btn_detail)
    fun onClickDetailButton(view: View) {

        if (mDetailRootLayout.visibility == View.VISIBLE) {
            hideImageDetailViews()

        } else {

            Blurry.with(this)
                    .radius(25)
                    .sampling(2)
                    .async()
                    .animate()
                    .capture(mViewPager)
                    .into(mBlurLayout)

            val showingImageFile = showingImageFile
            ImageModule.getInstance()
                    .loadImageInfo(showingImageFile)
                    .compose(workAndShow())
                    .subscribe { info -> updateFileDetailView(showingImageFile, info) }

            mBlurLayout.setOnClickListener { v -> hideImageDetailViews() }

            Observable.just(mDetailRootLayout)
                    .delay(200, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ viewGroup -> viewGroup.visibility = View.VISIBLE }, { RxUtils.unhandledThrowable(it) })
        }
    }

    private fun updateFileDetailView(showingImageFile: File?, info: ImageFileInformation) {
        // 文件大小
        ViewUtils.setText(mDetailContainer,
                R.id.value_file_size,
                FileSizeUtils.formatFileSize(info.fileLength, false))

        // 文件路径
        ViewUtils.setText(mDetailContainer,
                R.id.value_file_path,
                showingImageFile!!.absolutePath)

        // 文件日期
        ViewUtils.setText(
                mDetailContainer,
                R.id.value_capture_time,
                DateFormatUtils.format(info.lastModified,
                        FastDateFormat.getDateInstance(FastDateFormat.FULL).pattern))

        // 分辨率
        val sizeString = resources.getString(R.string.image_size_d_d,
                info.mediaResolution.width, info.mediaResolution.height)

        ViewUtils.setText(mDetailContainer,
                R.id.value_resolution,
                if (MediaUtils.isValidSize(info.mediaResolution)) sizeString else "-")
    }

    private fun hideImageDetailViews() {
        mBlurLayout.setImageDrawable(null)
        mDetailRootLayout.visibility = View.INVISIBLE
        //        mBlurLayout.setVisibility(View.INVISIBLE);
    }

    @OnClick(R.id.btn_share)
    fun onClickShareButton(view: View) {

        val currentFile = showingImageFile

        Logger.d("share file : " + currentFile!!)

        if (PathUtils.isStaticImageFile(currentFile)) {
            ShareUtils.shareImage(this, currentFile)
            //            ShareUtils.shareImages(this, ListUtils.objectToArrayList(Uri.fromFile(currentFile)));
        } else if (PathUtils.isGifFile(currentFile)) {
            ShareUtils.shareImage(this, currentFile)
        } else {
            ShareUtils.playVideo(this, currentFile.absolutePath)
            //            ToastUtils.toastShort(this, R.string.cannot_share_file);
        }
    }


    override fun onStart() {
        super.onStart()

        //        showRandomImage();
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        Log.d(TAG, "onStop: ")
        //
        EventBus.getDefault().unregister(this)

        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (ViewUtils.isShow(mDetailRootLayout)) {
            hideImageDetailViews()
            return
        }

        super.onBackPressed()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: TapImageMessage) {
        //ActivityCompat.finishAfterTransition(this);
        switchFullscreen()
    }

    private fun switchFullscreen() {

        //        ActionBar ab = getSupportActionBar();
        //        if (ab != null) {
        //            if (ab.isShowing()) {
        //                ab.hide();
        //            } else {
        //                ab.show();
        //            }
        //        }

        switchFullScreenMode(null)
    }

    /**
     * 在全屏模式和正常模式之间切换显示
     */
    private fun switchFullScreenMode(amList: List<Animator>?) {

        mCurrentFullscreen = isCurrentFullscreen

        // 全屏切换动画
        val animators = LinkedList(org.apache.commons.collections4.ListUtils.emptyIfNull(amList))

        if (mAnimationType == AnimationType.FADE_IN_FADE_OUT) {

            val e = alphaAnimatorOfView(mBottomToolbar)

            e.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {

                }

                override fun onAnimationEnd(animator: Animator) {


                    //                    mBottomToolbar.setEnabled(false);

                    //                    if (mBottomToolbar.getAlpha() == 0) {
                    //                        mBottomToolbar.setEnabled(false);
                    //                    } else {
                    //                        mBottomToolbar.setEnabled(true);
                    //                    }
                }

                override fun onAnimationCancel(animator: Animator) {

                }

                override fun onAnimationRepeat(animator: Animator) {

                }
            })
            animators.add(e)

        } else if (mAnimationType == AnimationType.SLIDE_IN_SLIDE_OUT) {
            animators.add(animatorOfBottomToolbar()) // Actionbar animation
        }

        // Cancel previous animation
        if (mFullscreenAnimatorSet != null) {
            if (mFullscreenAnimatorSet!!.isRunning) {
                return
            }
        }

        Log.d(TAG, "switchFullScreenMode: start animation")

        // Start new animation
        mFullscreenAnimatorSet = AnimatorSet()
        mFullscreenAnimatorSet!!.duration = 200
        mFullscreenAnimatorSet!!.playTogether(animators)
        mFullscreenAnimatorSet!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                if (isCurrentFullscreen) {
                    mBottomToolbar.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                if (isCurrentFullscreen) {
                    mBottomToolbar.visibility = View.INVISIBLE
                } else {
                    mBottomToolbar.visibility = View.VISIBLE
                }
                ViewUtils.setViewAndChildrenEnabled(mBottomToolbar, !isCurrentFullscreen)
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
        mFullscreenAnimatorSet!!.start()

    }

    override fun onScaleProgress(scale: Float) {
        Log.d(TAG, "onScaleProgress() called with: scale = [$scale]")
        mColorDrawable!!.alpha = Math.min(ALPHA_MAX, mColorDrawable!!.alpha - (scale * ALPHA_MAX).toInt())
    }

    override fun onDismiss() {
        Log.d(TAG, "onDismiss() called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition()
        } else {
            finish()
        }
    }

    override fun onCancel() {
        Log.d(TAG, "onCancel() called")
        mColorDrawable!!.alpha = ALPHA_MAX
    }

    private enum class AnimationType {
        FADE_IN_FADE_OUT,
        SLIDE_IN_SLIDE_OUT
    }

    private fun alphaAnimatorOfView(v: View?): ObjectAnimator {
        val alphaStart = v!!.alpha
        val alphaEnd = if (isCurrentFullscreen) VIEW_NORMAL_ALPHA else VIEW_FULL_SCREEN_ALPHA
        return ObjectAnimator.ofFloat(v, "alpha", alphaStart, alphaEnd)
    }


    private fun animatorOfBottomToolbar(): ObjectAnimator {

        val target = mBottomToolbar
        val targetHeight = target.height.toFloat()
        val animateStartTy = target.translationY
        val animateEndTy = if (isCurrentFullscreen) 0f else targetHeight

        val abAnimator: ObjectAnimator = ObjectAnimator.ofFloat(target, "translationY", animateStartTy, animateEndTy)

        ObjectAnimator.ofFloat()
        abAnimator.addUpdateListener({ animation: ValueAnimator -> mBottomToolbarTranslationY = animation.animatedValue as Float })

        return abAnimator
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: DragImageExitMessage) {
        ActivityCompat.finishAfterTransition(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: ImageAnimationStartMessage) {
        //        View decorView = getWindow().getDecorView();

        // Hide the status bar.
        //        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        //        decorView.setSystemUiVisibility(uiOptions);
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.

        //        StatusBarUtils.hideStatusBar(this);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: RemoveFileMessage) {

        mPagerAdapter!!.removeFile(message.file)
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

    private fun loadImageFiles() {

        if (mDirPath != null && mItemPosition != -1) {

            // 浏览文件列表
            ImageModule.getInstance()
                    .loadMediaFileList(File(mDirPath!!),
                            LoadMediaFileParam()
                                    .setFromCacheFirst(true)
                                    .setLoadMediaInfo(false)
                                    .setSortOrder(mSortOrder)
                                    .setSortWay(mSortWay)
                    )
                    .compose(workAndShow())
                    .map { this.imageListToImageListAdapter(it) }
                    .subscribe({ adapter -> showImageAdapter(adapter, mItemPosition) }, { RxUtils.unhandledThrowable(it) })

        } else if (mImageFilePath != null) {

            // 浏览单个文件
            Observable.just(mImageFilePath!!)
                    .map { File(it) }
                    .map { file ->
                        val mediaFile = MediaFile()
                        mediaFile.file = file
                        mediaFile.date = Date(file.lastModified())
                        mediaFile
                    }
                    .map { ListUtils.objectToLinkedList(it) }
                    .map { this.imageListToImageListAdapter(it) }
                    .subscribe({ adapter -> showImageAdapter(adapter, 0) }, { RxUtils.unhandledThrowable(it) })
        }
    }

    private fun showImageAdapter(adapter: ImageFragmentStatePagerAdapter, position: Int) {
        Log.d(TAG, "showImageAdapter() called with: adapter = [$adapter], position = [$position]")

        mPagerAdapter = adapter
        mPagerAdapter!!.setAnimatedItemPosition(position)

        mViewPager.adapter = mPagerAdapter
        mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {

                Log.d(TAG, "onPageSelected() called with: position = [$position]")

                mCurrentItem = position

                showImageInfo(position)

                EventBus.getDefault().post(CurrentImageChangeMessage().setPosition(position))
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })


        if (position != -1) {
            mCurrentItem = position
            mViewPager.currentItem = position

            showImageInfo(position)
        } else {
            mCurrentItem = 0
            mViewPager.currentItem = 0
            showImageInfo(0)
        }
    }

    private fun showImageInfo(position: Int) {
        val image = mPagerAdapter!!.getImage(position)
        val total = mPagerAdapter!!.count

        val imagePosition = getString(R.string.percent_d_d, position + 1, total)
        mBrief.text = imagePosition

        // Resolution
        ImageModule.getInstance()
                .loadImageInfo(image.getFile())
                .compose(workAndShow())
                .subscribe({ imageFileInformation ->
                    var resText: String? = null

                    val mediaResolution = imageFileInformation.mediaResolution
                    resText = MediaUtils.getResolutionString(this, mediaResolution)

                    mResolution.text = resText

                }) { throwable -> mResolution.text = "-" }

        // Title : file name
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = image.getFile()!!.name

            // Subtitle : index
            //actionBar.setSubtitle(imagePosition);
        }
    }

    private fun imageListToImageListAdapter(mediaFiles: List<MediaFile>): ImageFragmentStatePagerAdapter {
        val adapter = ImageFragmentStatePagerAdapter(supportFragmentManager)
        adapter.setImages(imagesToImages(mediaFiles))
        return adapter
    }

    private fun imagesToItems(mediaFiles: List<MediaFile>): List<DefaultImageListAdapter.Item> {
        return Stream.of(mediaFiles)
                .map { image ->
                    val item = DefaultImageListAdapter.Item()
                    item.file = image.file
                    item
                }
                .toList()
    }

    private fun imagesToImages(mediaFiles: List<MediaFile>): MutableList<Image> {
        return Stream.of(mediaFiles)
                .map { image -> Image().setFile(image.file) }
                .toList()
    }

    private fun loadItem() {
        // Set the title TextView to the item's name and author
        //mHeaderTitle.setText(getResolutionString(R.string.image_header, mItem.getName(), mItem.getAuthor()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && addTransitionListener()) {
            // If we're running on Lollipop and we have added a listener to the shared element
            // transition, load the thumbnail. The listener will load the full-size image when
            // the transition is complete.
            loadThumbnail()
        } else {
            // If all other cases we should just load the full-size image now
            loadFullSizeImage()
        }
    }

    /**
     * Try and add a [Transition.TransitionListener] to the entering shared element
     * [Transition]. We do this so that we can load the full-size image after the transition
     * has completed.
     *
     * @return true if we were successful in adding a listener to the enter transition
     */
    private fun addTransitionListener(): Boolean {
        val transition = window.sharedElementEnterTransition

        if (transition != null) {
            // There is an entering shared element transition so add a listener to it
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) {
                    // As the transition has ended, we can now load the full-size image
                    loadFullSizeImage()

                    // Make sure we remove ourselves as a listener
                    transition.removeListener(this)
                }

                override fun onTransitionStart(transition: Transition) {
                    // No-op
                }

                override fun onTransitionCancel(transition: Transition) {
                    // Make sure we remove ourselves as a listener
                    transition.removeListener(this)
                }

                override fun onTransitionPause(transition: Transition) {
                    // No-op
                }

                override fun onTransitionResume(transition: Transition) {
                    // No-op
                }
            })
            return true
        }

        // If we reach here then we have not added a listener
        return false
    }


    /**
     * Load the item's thumbnail image into our [ImageView].
     */
    private fun loadThumbnail() {
        Glide.with(mPhotoView!!.context)
                .load(mImages!![0].getFile())
                .into(mPhotoView)
    }

    /**
     * Load the item's full-size image into our [ImageView].
     */
    private fun loadFullSizeImage() {

        Glide.with(mPhotoView!!.context)
                .load(mImages!![0].getFile())
                .apply(RequestOptions.noAnimation())
                .into(mPhotoView)
    }

    private fun extractIntentData() {
        val intent = intent
        mImageFilePath = intent.getStringExtra(EXTRA_IMAGE_FILE_PATH)

        mDirPath = intent.getStringExtra(EXTRA_IMAGE_DIR_PATH)
        mItemPosition = intent.getIntExtra(EXTRA_IMAGE_ITEM_POSITION, -1)
        //        mViewMode = (ViewMode) intent.getSerializableExtra(EXTRA_VIEW_MODE);

        mSortWay = intent.getSerializableExtra(EXTRA_SORT_WAY) as SortWay
        mSortOrder = intent.getSerializableExtra(EXTRA_SORT_ORDER) as SortOrder
    }

    private fun showRandomImage() {
        mBrief.text = mImageFilePath
        //        ImageService.getInstance().loadRandomImage()
        Observable.just(mImageFilePath!!)
                .map { File(it) }
                .compose(workAndShow())
                .map { file -> Image().setFile(file) }
                .subscribe { image ->
                    val images = LinkedList<Image>()
                    images.add(image)

                    val imagePagerAdapter = ImagePagerAdapter(images)

                    imagePagerAdapter.setOnClickImageListener(object : ImagePagerAdapter.OnClickImageListener {
                        override fun onClick(image: Image) {
                            Log.d(TAG, "showRandomImage() called")
                        }
                    })

                    imagePagerAdapter.setOnLongClickImageListener(object : ImagePagerAdapter.OnLongClickImageListener {
                        override fun onLongClick(image: Image): Boolean {
                            return false
                        }
                    })

                    mViewPager.adapter = imagePagerAdapter
                    mViewPager.currentItem = 0
                }

    }

    override fun onFragmentInteraction(uri: Uri) {

    }

    private class Image {
        internal var mFile: File? = null
        internal var mType: Type? = null
        internal var mViewId: Int = 0

        internal val isGif: Boolean
            get() = mFile != null && FilenameUtils.isExtension(mFile!!.name, "gif")

        internal val isBitmap: Boolean
            get() = !isGif

        fun setViewId(viewId: Int): Image {
            mViewId = viewId
            return this
        }

        fun getViewId(): Int {
            return mViewId
        }

        fun getFile(): File? {
            return mFile
        }

        fun setFile(file: File): Image {
            mFile = file
            return this
        }

        fun setType(type: Type): Image {
            this.mType = type
            return this
        }

        internal enum class Type {
            GIF,
            BITMAP
        }
    }

    private class ImagePagerAdapter internal constructor(images: List<Image>?) : PagerAdapter() {

        internal var mImages: List<Image> = LinkedList()

        internal var mOnClickImageListener: OnClickImageListener? = null
        internal var mOnLongClickImageListener: OnLongClickImageListener? = null

        interface OnClickImageListener {
            fun onClick(image: Image)
        }

        interface OnLongClickImageListener {
            fun onLongClick(image: Image): Boolean
        }

        init {
            if (images != null) {
                mImages = images
            }
        }

        internal fun getViewId(position: Int): Int {
            if (position < 0 || position >= mImages.size) {
                throw IllegalArgumentException("Invalid position '$position'.")
            }
            val image = mImages[position]
            return image.getViewId()
        }

        fun getOnClickImageListener(): OnClickImageListener? {
            return mOnClickImageListener
        }

        fun setOnClickImageListener(onClickImageListener: OnClickImageListener): ImagePagerAdapter {
            mOnClickImageListener = onClickImageListener
            return this
        }

        fun getOnLongClickImageListener(): OnLongClickImageListener? {
            return mOnLongClickImageListener
        }

        fun setOnLongClickImageListener(onLongClickImageListener: OnLongClickImageListener): ImagePagerAdapter {
            mOnLongClickImageListener = onLongClickImageListener
            return this
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            Log.d(TAG, "instantiateItem() called with: container = [$container], position = [$position]")

            val image = mImages[position]
            val photoView = PhotoView(container.context)

            if (image.isBitmap) {
                val id = View.generateViewId()
                image.setViewId(id)
                photoView.id = id

                photoView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                photoView.scaleType = ImageView.ScaleType.CENTER
                // Now just add PhotoView to ViewPager and return it
                container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                // 加载原始图片
                Glide.with(container.context)
                        //.asBitmap()
                        .load(image.getFile())
                        .apply(RequestOptions.fitCenterTransform())
                        //.transition(BitmapTransitionOptions.withCrossFade())
                        .into(photoView)

            } else if (image.isGif) {

                //                Observable.just(image)
                //                        .map(image1 -> image.getFile())
                //                        .map(GifDrawable::new);


                val drawable: GifDrawable
                try {
                    drawable = GifDrawable(image.getFile()!!)

                    photoView.setImageDrawable(drawable)
                    photoView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    photoView.scaleType = ImageView.ScaleType.CENTER
                    container.addView(photoView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

                } catch (e: IOException) {
                    e.printStackTrace()
                    return photoView
                }

            } else {
                return photoView
            }

            // 图片点击和长按事件
            photoView.setOnViewTapListener { view1, x, y ->
                if (mOnClickImageListener != null) {
                    mOnClickImageListener!!.onClick(image)
                }
            }

            photoView.setOnLongClickListener { v -> mOnLongClickImageListener != null && mOnLongClickImageListener!!.onLongClick(image) }

            return photoView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getCount(): Int {
            return mImages.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }
    }

    /**
     * 大量图片列表适配器
     */
    private inner class ImageFragmentStatePagerAdapter internal constructor(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        internal var mImages: MutableList<Image> = LinkedList()

        internal var mAnimatedItemPosition = -1
        private val registeredFragments = SparseArray<ImageFragment>()

        internal fun removeItem(position: Int): Boolean {
            if (!(position >= 0 && position < mImages.size)) {
                throw IllegalArgumentException("Invalid position $position")
            }

            mImages.removeAt(position)
            notifyDataSetChanged()

            return true
        }

        internal fun getImage(position: Int): Image {
            if (position < 0 || position >= mImages.size) {
                throw IndexOutOfBoundsException(String.format("Position %d out of bounds(0,%d)", position, mImages.size - 1))
            }
            return mImages[position]
        }

        fun getRegisteredFragment(position: Int): ImageFragment {
            return registeredFragments.get(position)
        }

        fun setAnimatedItemPosition(animatedItemPosition: Int): ImageFragmentStatePagerAdapter {
            mAnimatedItemPosition = animatedItemPosition
            return this
        }

        internal fun setImages(images: MutableList<Image>?): ImageFragmentStatePagerAdapter {
            if (images != null) {
                mImages = images
            }
            return this
        }

        override fun getItemPosition(`object`: Any): Int {
            val fragment = `object` as Fragment
            return if (fragment is ImageFragment) {
                PagerAdapter.POSITION_NONE
            } else super.getItemPosition(`object`)
        }

        override fun getItem(position: Int): Fragment {
            val image = mImages[position]

            Log.d(TAG, "get fragment item for file " + image.getFile()!!)

            val absolutePath = image.getFile()!!.absolutePath
            val transitionName = TransitionUtils.generateTransitionName(absolutePath)
            val fileTypeTransitionName = TransitionUtils.generateTransitionName(TransitionUtils.TRANSITION_PREFIX_FILETYPE, absolutePath)

            val performEnterTransition = position == mAnimatedItemPosition
            if (performEnterTransition) {
                Log.d(TAG, "get fragment item : PerformEnterTransition for position $position $transitionName")
            }

            val fragment = ImageFragment.newInstance(absolutePath, transitionName, fileTypeTransitionName, performEnterTransition)
            registeredFragments.put(position, fragment)

            return fragment
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            Log.d(TAG, "remove fragment at position $position $`object`")
            registeredFragments.remove(position)
            super.destroyItem(container, position, `object`)
        }

        override fun getCount(): Int {
            return mImages.size
        }

        fun removeFile(file: File) {
            val i = org.apache.commons.collections4.ListUtils.indexOf(mImages) { image -> SystemUtils.isSameFile(image.getFile(), file) }
            if (i != -1) {
                mImages.removeAt(i)
                notifyDataSetChanged()
            }
        }
    }

    companion object {

        private val TAG = ImageActivity::class.java.simpleName
        private val EXTRA_IMAGE_FILE_PATH = "extra:file_name"
        private val EXTRA_IMAGE_DIR_PATH = "extra:dir_path"
        private val EXTRA_IMAGE_ITEM_POSITION = "extra:image_item_position"
        private val EXTRA_VIEW_MODE = "extra:view_mode"
        private val EXTRA_SORT_WAY = "extra:sort_way"
        private val EXTRA_SORT_ORDER = "extra:sort_order"

        private val EXTRA_PARAM_FILE_PATH = "viewer:param:filepath"
        val TRANSITION_NAME_IMAGE = "viewer:image"

        fun getImageContent(parent: File, imageName: String, activity: AppCompatActivity): ContentValues {
            val image = ContentValues()
            image.put(MediaStore.Images.Media.TITLE, activity.getString(R.string.app_name))
            image.put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            image.put(MediaStore.Images.Media.DESCRIPTION, imageName)
            image.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
            image.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            image.put(MediaStore.Images.Media.ORIENTATION, 0)
            val filePath = parent.toString()
            image.put(MediaStore.Images.ImageColumns.BUCKET_ID, filePath.toLowerCase().hashCode())
            image.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, parent.name.toLowerCase())
            image.put(MediaStore.Images.Media.SIZE, parent.length())
            image.put(MediaStore.Images.Media.DATA, parent.absolutePath)
            return image
        }

        fun openShareOptions(title: String, description: String,
                             imageUrl: Uri, activity: AppCompatActivity) {

            val share = Intent(Intent.ACTION_SEND)
            share.type = "image/jpeg"
            share.putExtra(Intent.EXTRA_STREAM, imageUrl)
            activity.startActivity(Intent.createChooser(share, "Choose app to share"))


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

        fun newIntentViewFile(context: Context, file: File): Intent {
            val intent = Intent(context, ImageActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.putExtra(EXTRA_IMAGE_FILE_PATH, file.absolutePath)
            return intent
        }

        fun newIntentViewFileList(context: Context, dirAbsolutePath: String, itemModelPosition: Int, sortOrder: SortOrder, sortWay: SortWay): Intent {
            val intent = Intent(context, ImageActivity::class.java)
            intent.action = Intent.ACTION_VIEW
            intent.putExtra(EXTRA_IMAGE_DIR_PATH, dirAbsolutePath)
            intent.putExtra(EXTRA_IMAGE_ITEM_POSITION, itemModelPosition)

            intent.putExtra(EXTRA_SORT_WAY, sortWay)
            intent.putExtra(EXTRA_SORT_ORDER, sortOrder)
            return intent
        }


        val VIEW_FULL_SCREEN_ALPHA = 0.0f // View 全屏模式下的 Alpha
        val VIEW_NORMAL_ALPHA = 1.0f // View 正常模式下的 Alpha
    }
}

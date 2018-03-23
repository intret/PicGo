package cn.intret.app.picgo.ui.conflict


import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import butterknife.ButterKnife
import butterknife.OnClick
import cn.intret.app.picgo.R
import cn.intret.app.picgo.model.event.ConflictResolveResultMessage
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.model.image.data.CompareItem
import cn.intret.app.picgo.model.image.data.CompareItemResolveResult
import cn.intret.app.picgo.model.image.data.ResolveResult
import cn.intret.app.picgo.ui.adapter.ConflictImageListAdapter
import cn.intret.app.picgo.utils.ListUtils
import cn.intret.app.picgo.utils.RxUtils
import cn.intret.app.picgo.utils.ToastUtils
import com.afollestad.materialdialogs.MaterialDialog
import com.annimon.stream.Stream
import com.orhanobut.logger.Logger
import kotterknife.bindView
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*


/**
 * 文件冲突解决 DialogFragment
 */
class ConflictResolverDialogFragment : BottomSheetDialogFragment() {

    private var mConflictFiles: List<File>? = null
    private var mDestDir: File? = null

    internal val mImageList: RecyclerView by bindView(R.id.conflict_image_list)

    internal val mBtnKeepSource: ImageView by bindView(R.id.keep_source)
    internal val mTvKeepSource: TextView by bindView(R.id.keep_source_check)
    internal val mSourceRemoveCount: TextView by bindView(R.id.source_remove_file_count)

    internal val mBtnKeepTarget: ImageView by bindView(R.id.keep_target)
    internal val mTvKeepTarget: TextView by bindView(R.id.keep_target_check)
    internal val mTargetRemoveCount: TextView by bindView(R.id.target_remove_file_count)


    private var mAdapter: ConflictImageListAdapter? = null
    private var mLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val string = it.getString(ARG_DEST_DIR)
                    ?: throw IllegalArgumentException("Argument ARG_DEST_DIR is empty.")

            mDestDir = File(string)

            val stringArrayList = it.getStringArrayList(ARG_CONFLICT_FILES)
            if (ListUtils.isEmpty(stringArrayList)) {
                throw IllegalArgumentException("Argument ARG_CONFLICT_FILES is empty.")
            }

            mConflictFiles = stringArrayList.map { File(it) }.toList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: ")

        return LayoutInflater.from(activity)
                .inflate(R.layout.fragment_conflict_resolver_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ButterKnife.bind(this@ConflictResolverDialogFragment, view)

        cn.intret.app.picgo.utils.ViewUtils.setText(view, R.id.title, getString(R.string.file_name_conflict))
        cn.intret.app.picgo.utils.ViewUtils.setText(view, R.id.btn_positive, getString(R.string.next))
    }

    override fun onStart() {
        super.onStart()
        if (!mLoaded) {
            loadConflictImageList()
            mLoaded = true
        }
    }

    @OnClick(R.id.keep_source)
    fun onClickKeepSource(view: View) {

        mBtnKeepSource.isSelected = !mBtnKeepSource.isSelected
        mTvKeepSource.isSelected = mBtnKeepSource.isSelected

        val adapter = mImageList.adapter
        if (adapter != null && adapter is ConflictImageListAdapter) {
            if (mTvKeepSource.isSelected) {
                mBtnKeepTarget.isSelected = false
                mTvKeepTarget.isSelected = false
                adapter.selectAllSourceItems()
            } else {
                adapter.unselectAllSourceItem()
            }
        }
    }

    @OnClick(R.id.keep_target)
    fun onClickKeepTarget(view: View) {
        mBtnKeepTarget.isSelected = !mBtnKeepTarget.isSelected
        mTvKeepTarget.isSelected = mBtnKeepTarget.isSelected

        val adapter = mImageList.adapter
        if (adapter != null && adapter is ConflictImageListAdapter) {
            if (mBtnKeepTarget.isSelected) {
                mBtnKeepSource.isSelected = false
                mTvKeepSource.isSelected = false
                adapter.selectAllTargetItems()
            } else {
                adapter.unselectAllTargetItem()
            }
        }
    }

    @OnClick(R.id.btn_positive)
    fun onClickHeaderPositive(view: View) {
        if (mAdapter == null) {
            ToastUtils.toastShort(context, R.string.not_ready)
            return
        }

        val items = mAdapter!!.items
        val compareItems = Stream.of(items)
                .map { item -> CompareItem(item.resolveResult, item.targetFile, item.sourceFile) }
                .toList()

        val param = mAdapter!!.resolveResultCount
        val keepTargetCount = param.p1
        val keepSourceCount = param.p2
        val deleteTargetCount = param.p3
        val deleteSourceCount = param.p4

        val sourceDirPath = ListUtils.firstOf(mConflictFiles).parentFile.absolutePath

        val resolveResults = LinkedList<CompareItemResolveResult>()
        MaterialDialog.Builder(context!!)
                .title(R.string.action_confirm)
                .content(R.string.confirm_to_remove_d_files_from_s_and_remove_d_files_from_s,
                        mDestDir, deleteTargetCount,
                        sourceDirPath, deleteSourceCount
                )
                .positiveText(R.string.confirm)
                .onPositive { dialog, which ->

                    Logger.d("解决图片文文件名冲突")

                    ImageModule
                            .resolveFileNameConflict(compareItems)
                            .compose(RxUtils.applySchedulers())
                            .doOnNext { this.onCompareItemResult(it) }
                            // 合并为列表
                            .collectInto(resolveResults, { obj, e -> obj.add(e) })
                            .subscribe(this::onCompleteResolve, RxUtils::unhandledThrowable)
                    dialog.dismiss()
                }
                .negativeText(R.string.cancel)
                .show()
    }

    private fun onCompleteResolve(compareItems: List<CompareItemResolveResult>) {
        if (mAdapter != null) {
            // 所有冲突都解决了，可以关闭对话框
            if (mAdapter!!.itemCount == 0) {
                dismiss()
            }
        }

        EventBus.getDefault().post(
                ConflictResolveResultMessage()
                        .setCompareItems(compareItems))
    }

    private fun onCompareItemResult(compareItemResolveResult: CompareItemResolveResult) {
        if (compareItemResolveResult.resolved) {

            val compareItem = compareItemResolveResult.compareItem
            when (compareItem.result) {
                ResolveResult.KEEP_SOURCE, ResolveResult.KEEP_TARGET -> {
                    mAdapter?.removeItemSource(compareItem.sourceFile)
                }
                ResolveResult.KEEP_BOTH -> {
                }
                ResolveResult.NONE -> Log.e(TAG, "invalid result data.")
            }

        } else {
            Log.w(TAG, "onCompareItemResult: no resolved :$compareItemResolveResult")
        }
    }

    private fun loadConflictImageList() {

        mTvKeepTarget.text = mDestDir!!.name
        mTvKeepSource.text = ListUtils.firstOf(mConflictFiles).parentFile.name

        val targetFiles = Stream.of(mConflictFiles!!)
                .map { file -> File(mDestDir, file.name) }
                .toList()


        val allFiles = LinkedList(mConflictFiles!!)
        allFiles.addAll(targetFiles)

        ImageModule

                // 加载图片文件信息
                .loadImageFilesInfoMap(allFiles)
                .compose(RxUtils.applySingleSchedulers())

                // 构造 Adapter
                .map { allFileInfoMap ->
                    Stream.of(mConflictFiles!!)
                            .map { file ->

                                val targetFile = File(mDestDir, file.name)

                                val sourceInfo = allFileInfoMap[file]
                                val targetInfo = allFileInfoMap[targetFile]

                                ConflictImageListAdapter.Item()
                                        .setSourceFile(file)
                                        .setSourceImageResolution(sourceInfo?.mediaResolution)
                                        .setSourceFileSize(sourceInfo?.fileLength ?: -1)

                                        .setTargetFile(targetFile)
                                        .setTargetImageResolution(targetInfo?.mediaResolution)
                                        .setTargetFileSize(targetInfo?.fileLength ?: -1)
                            }.toList()
                }
                .map<ConflictImageListAdapter>({ ConflictImageListAdapter(it) })
                .map<ConflictImageListAdapter>({ this.configAdapter(it) })
                // 显示图片列表
                .subscribe({ this.showAdapter(it) }) { throwable ->
                    // TODO: show error empty view
                    ToastUtils.toastLong(context, R.string.load_image_failed)
                }
    }

    private fun showAdapter(adapter: ConflictImageListAdapter) {
        mImageList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        mImageList.adapter = adapter
        mAdapter = adapter
    }

    private fun configAdapter(adapter: ConflictImageListAdapter): ConflictImageListAdapter {
        // 配置 Adapter
        adapter.setOnInteractionListener { totalCount, keepTargetCount, keepSourceCount, deleteTargetCount, deleteSourceCount ->
            if (deleteTargetCount == 0) {
                mTargetRemoveCount.visibility = View.GONE
            } else {
                mTargetRemoveCount.visibility = View.VISIBLE
                mTargetRemoveCount.text = getString(R.string.percent_d_d, deleteTargetCount, totalCount)
            }

            if (deleteSourceCount == 0) {
                mSourceRemoveCount.visibility = View.GONE
            } else {
                mSourceRemoveCount.visibility = View.VISIBLE
                mSourceRemoveCount.text = getString(R.string.percent_d_d, deleteSourceCount, totalCount)
            }
        }
        return adapter
    }

    companion object {

        private val ARG_DEST_DIR = "dest_dir"
        private val ARG_CONFLICT_FILES = "conflict_files"
        private val TAG = ConflictResolverDialogFragment::class.java.simpleName

        /**
         * @param destDir             移动操作指定的目标目录
         * @param sourceConflictFiles 移动操作的源目录冲突文件列表，这些源文件可以不在同一个目录
         */
        fun newInstance(destDir: String, sourceConflictFiles: ArrayList<String>): ConflictResolverDialogFragment {
            val fragment = ConflictResolverDialogFragment()
            val args = Bundle()
            args.putString(ARG_DEST_DIR, destDir)
            args.putStringArrayList(ARG_CONFLICT_FILES, sourceConflictFiles)
            fragment.arguments = args
            return fragment
        }
    }
}// Required empty public constructor

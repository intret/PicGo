package cn.intret.app.picgo.ui.exclude

import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import butterknife.ButterKnife
import cn.intret.app.picgo.R
import cn.intret.app.picgo.app.CoreModule
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.model.user.UserModule
import cn.intret.app.picgo.ui.adapter.SectionedFolderListAdapter
import cn.intret.app.picgo.ui.adapter.SectionedListItemClickDispatcher
import cn.intret.app.picgo.ui.adapter.SectionedListItemDispatchListener
import cn.intret.app.picgo.ui.adapter.brvah.ExpandableFolderAdapter
import cn.intret.app.picgo.ui.adapter.brvah.FolderListAdapter
import cn.intret.app.picgo.ui.adapter.brvah.FolderListAdapterUtils
import cn.intret.app.picgo.ui.event.MoveFileResultMessage
import cn.intret.app.picgo.utils.*
import cn.intret.app.picgo.view.T9KeypadView
import cn.intret.app.picgo.widget.RecyclerItemTouchListener
import com.afollestad.sectionedrecyclerview.ItemCoord
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback
import com.chad.library.adapter.base.listener.OnItemSwipeListener
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.expandable.ExpandableExtension
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.fastadapter_extensions.RangeSelectorHelper
import kotterknife.bindView
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Move selected files ( Fragment argument specified ) to user selected target folder.
 */
class ExcludeFolderDialogFragment : BottomSheetDialogFragment(), T9KeypadView.OnT9KeypadInteractionHandler {

    private var mHiddenFolders: List<File>? = null

    private var mListener: OnFragmentInteractionListener? = null


    internal val mFolderList: RecyclerView by bindView(R.id.folder_list)
    internal val mDesc: TextView  by bindView(R.id.desc)
    internal val mT9KeypadView: T9KeypadView by bindView(R.id.t9_keypad)
    internal val mKeyboardSwitchLayout: View by bindView(R.id.keyboard_switch_layout)
    internal val mKeyboardSwitchIv: ImageView by bindView(R.id.keyboard_switch)


    private var mListAdapter: SectionedFolderListAdapter? = null
    private val mAdapter: FolderListAdapter? = null

    private val mEnableDetectSelectedFolder = false
    private val mRangeSelectorHelper: RangeSelectorHelper? = null
    private var mExpandableAdapter: ExpandableFolderAdapter? = null
    //    private DragSelectTouchListener mDragSelectTouchListener;

    private val mItemTouchHelper: ItemTouchHelper? = null
    private val mItemDragAndSwipeCallback: ItemDragAndSwipeCallback? = null
    private val expandableExtension: ExpandableExtension<*>? = null
    private val mIItemSelectExtension: SelectExtension<IItem<*, *>>? = null

    //save our FastAdapter
    private val mItemAdapter: FastItemAdapter<IItem<*, *>>? = null

    internal var mCreateDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            val files = arguments!!.getStringArrayList(ARG_EXCLUDE_FOLDERS)
            if (files == null) {
                mHiddenFolders = LinkedList()
            } else {
                mHiddenFolders = files.map { File(it) }.toList()
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        Log.d(TAG, "onCreateView() called with: inflater = [$inflater], container = [$container], savedInstanceState = [$savedInstanceState]")
        val contentView = LayoutInflater.from(activity).inflate(R.layout.fragment_exclude_folder_list, container, false)
        return contentView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createContentView(view, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")

        loadSectionFolderList()

        //loadExpandableFolderList();
    }

    private fun loadExpandableFolderList() {
        ImageModule
                .loadHiddenFileListModel()
                .map<ExpandableFolderAdapter>({ FolderListAdapterUtils.folderModelToExpandableFolderAdapter(it) })
                .subscribe(
                        { this.showExpandableFolderList(it) },
                        { RxUtils.unhandledThrowable(it) })
    }

    private fun showExpandableFolderList(adapter: ExpandableFolderAdapter) {

        adapter.setOnItemClickListener { baseQuickAdapter, view, i -> Log.d(TAG, "onItemClick() called with: baseQuickAdapter = [$baseQuickAdapter], view = [$view], i = [$i]") }
        adapter.setOnInteractionListener { item ->

        }

        val manager = GridLayoutManager(this.activity, 1)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == ExpandableFolderAdapter.TYPE_LEVEL_1) 1 else manager.spanCount
            }
        }

        val onItemSwipeListener = object : OnItemSwipeListener {
            override fun onItemSwipeStart(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                Log.d(TAG, "view swiped start: $pos")
                val holder = viewHolder as BaseViewHolder
                //                holder.setTextColor(R.id.tv, Color.WHITE);
            }

            override fun clearView(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                Log.d(TAG, "View reset: $pos")
                val holder = viewHolder as BaseViewHolder
                //                holder.setTextColor(R.id.tv, Color.BLACK);
            }

            override fun onItemSwiped(viewHolder: RecyclerView.ViewHolder, pos: Int) {
                Log.d(TAG, "View Swiped: $pos")
            }

            override fun onItemSwipeMoving(canvas: Canvas, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, isCurrentlyActive: Boolean) {
                canvas.drawColor(ContextCompat.getColor(activity!!, R.color.list_item_swipe_delete))
                //                canvas.drawText("Just some text", 0, 40, paint);
            }
        }
        //mItemDragAndSwipeCallback = new ItemDragAndSwipeCallback(adapter);

        mFolderList.adapter = adapter
        mExpandableAdapter = adapter

        // important! setLayoutManager should be called after setAdapter
        mFolderList.layoutManager = manager
        adapter.expandAll()
    }

    private fun loadSectionFolderList() {
        ImageModule
                .loadHiddenFileListModel()
                .compose(RxUtils.applySchedulers())
                .map { FolderListAdapterUtils.folderModelToSectionedFolderListAdapter(it) }
                .map { adapter ->
                    adapter.isSelectable = false
                    adapter.isShowCloseButton = true
                    adapter.onItemClickListener = object : SectionedFolderListAdapter.OnItemClickListener {
                        override fun onSectionHeaderClick(section: SectionedFolderListAdapter.Section, sectionIndex: Int, adapterPosition: Int) {

                        }

                        override fun onSectionHeaderOptionButtonClick(v: View, section: SectionedFolderListAdapter.Section, sectionIndex: Int) {

                        }

                        override fun onItemClick(sectionItem: SectionedFolderListAdapter.Section, section: Int, item: SectionedFolderListAdapter.Item, relativePos: Int) {

                        }

                        override fun onItemLongClick(v: View, sectionItem: SectionedFolderListAdapter.Section, section: Int, item: SectionedFolderListAdapter.Item, relativePos: Int) {

                        }

                        override fun onItemCloseClick(v: View, section: SectionedFolderListAdapter.Section, item: SectionedFolderListAdapter.Item, sectionIndex: Int, relativePosition: Int) {

                            UserModule
                                    .excludeFolderPreference
                                    .map { pref ->
                                        val files = pref.get()
                                        files.remove(item.file)
                                        pref.set(files)

                                        true
                                    }
                                    .compose(RxUtils.applySchedulers())
                                    .subscribe({ ok ->
                                        if (ok!!) {

                                            adapter.removeFolderItem(sectionIndex, relativePosition)
                                        } else {
                                            ToastUtils.toastShort(this@ExcludeFolderDialogFragment.context, R.string.delete_item_failed)
                                        }
                                    }) { throwable ->
                                        throwable.printStackTrace()
                                        ToastUtils.toastShort(this@ExcludeFolderDialogFragment.context, R.string.delete_item_failed)
                                    }

                        }
                    }

                    adapter
                }
                .doOnNext { adapter -> mListAdapter = adapter }
                .subscribe({ adapter ->

                    // Show loaded adapter
                    mFolderList.adapter = mListAdapter

                    // Restore position
                    val visibleItemPosition = UserModule.moveFileDialogFirstVisibleItemPosition
                    if (visibleItemPosition != RecyclerView.NO_POSITION) {
                        mFolderList.scrollToPosition(visibleItemPosition)
                    }

                }) { throwable -> ToastUtils.toastShort(this@ExcludeFolderDialogFragment.activity, R.string.load_folder_list_failed) }
    }

    private fun setAdapterMoveFileSourceDir(adapter: SectionedFolderListAdapter): SectionedFolderListAdapter {
        if (!ListUtils.isEmpty(mHiddenFolders)) {
            adapter.moveFileSourceDir = mHiddenFolders!![0].parentFile
        }
        return adapter
    }

    override fun onResume() {
        Log.d(TAG, "onResume() called")
        super.onResume()
    }

    override fun onStop() {
        Log.d(TAG, "onStop() called")
        super.onStop()
    }

    override fun onDestroyView() {
        Log.d(TAG, "onDestroyView() called")
        super.onDestroyView()
    }

    private fun createContentView(contentView: View, savedInstanceState: Bundle?): View {

        ButterKnife.bind(this@ExcludeFolderDialogFragment, contentView)

        initContentView(contentView, savedInstanceState)

        ViewUtil.setHideIme(activity, contentView)

        initListener(contentView)

        return contentView
    }

    private fun initListener(contentView: View) {
        val keypadContainer = contentView.findViewById<ViewGroup>(R.id.t9_keypad_container)
        keypadContainer.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                keypadContainer.visibility = View.INVISIBLE
            }
        }
        val keypadView = contentView.findViewById<T9KeypadView>(R.id.t9_keypad)

        val btnKeypadSwitch = contentView.findViewById<ImageView>(R.id.keyboard_switch)
        val keypadSwitchLayout = contentView.findViewById<View>(R.id.keyboard_switch_layout)

        keypadSwitchLayout.setOnClickListener { v -> switchKeyboard(keypadContainer, btnKeypadSwitch) }
        btnKeypadSwitch.setOnClickListener { v -> switchKeyboard(keypadContainer, btnKeypadSwitch) }

        //        mContactsLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        //
        //            @Override
        //            public void onItemClick(AdapterView<?> parent, View view,
        //                                    int position, long id) {
        //                ContactsContract.Contacts contacts = ContactsHelper.getInstance().getSearchContacts().get(position);
        //                String uri = "tel:" + contacts.getPhoneNumber();
        //                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(uri));
        //                // intent.setData(Uri.parse(uri));
        //                startActivity(intent);
        //
        //            }
        //        });
    }

    private fun initContentView(contentView: View, savedInstanceState: Bundle?) {
        initHeader(contentView)
        initFolderList(contentView)
        initFolder(contentView)
        initDialPad(contentView)

        //initList(contentView, savedInstanceState);
    }

    private fun initHeader(contentView: View) {
        val btnMoveFile = contentView.findViewById<Button>(R.id.btn_positive)
        btnMoveFile.setOnClickListener { v -> dismiss() }
    }

    private fun initDialPad(contentView: View) {
        val keypadContainer = contentView.findViewById<ViewGroup>(R.id.t9_keypad_container)
        val t9KeypadView = contentView.findViewById<T9KeypadView>(R.id.t9_keypad)
        val folderList = contentView.findViewById<RecyclerView>(R.id.folder_list)
        val folderListContainer = contentView.findViewById<ViewGroup>(R.id.folder_list_container)


        t9KeypadView
                .dialpadInputObservable
                .debounce(369, TimeUnit.MILLISECONDS)
                .subscribe { input ->

                    val inputString = input.toString()

                    ImageModule
                            .loadHiddenFileListModel(inputString)
                            .map<SectionedFolderListAdapter>({ FolderListAdapterUtils.folderModelToSectionedFolderListAdapter(it) })
                            .compose(RxUtils.applySchedulers())
                            .subscribe({ newAdapter ->

                                //                                if (folderList != null) {
                                //                                    SectionedFolderListAdapter currAdapter = (SectionedFolderListAdapter) folderList.getAdapter();
                                //
                                //                                    currAdapter.diffUpdate(newAdapter);
                                //                                }
                            }, { RxUtils.unhandledThrowable(it) })
                }

        //        showKeyboard(keypadContainer, (ImageView) contentView.findViewById(R.id.keyboard_switch_image_view));
    }

    private fun switchKeyboard(keypadContainer: ViewGroup, kbSwitchIv: ImageView) {

        if (ViewUtil.getViewVisibility(keypadContainer) != View.VISIBLE) {
            showKeyboard(keypadContainer, kbSwitchIv)
        } else {
            hideKeyboard(keypadContainer, kbSwitchIv)
        }
    }

    private fun hideKeyboard(t9TelephoneDialpadView: ViewGroup, keyboardSwitchIv: ImageView) {
        ViewUtil.invisibleView(t9TelephoneDialpadView)
        keyboardSwitchIv.setImageResource(R.drawable.keyboard_show_selector)
    }

    private fun hideKeyboard() {
        ViewUtil.hideView(mT9KeypadView)
        mKeyboardSwitchIv.setImageResource(R.drawable.keyboard_show_selector)
    }

    private fun showKeyboard() {
        ViewUtil.showView(mT9KeypadView)
        mKeyboardSwitchIv.setImageResource(R.drawable.keyboard_hide_selector)
    }

    private fun showKeyboard(keypadContainer: ViewGroup, keypadSwitchButton: ImageView) {

        ViewUtil.showView(keypadContainer)
        keypadContainer.requestFocus()
        keypadSwitchButton.setImageResource(R.drawable.keyboard_hide_selector)
    }

    private fun initFolder(contentView: View) {
        mFolderList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
    }

    private fun initFolderList(contentView: View) {
        val folderList = contentView.findViewById<RecyclerView>(R.id.folder_list)

        folderList.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)


        val clickListener = { view: View, position: Int ->
            SectionedListItemClickDispatcher<SectionedRecyclerViewAdapter<*>>(mListAdapter)
                    .dispatch(position, object : SectionedListItemDispatchListener<SectionedRecyclerViewAdapter<SectionedViewHolder>> {
                        override fun onHeader(adapter: SectionedRecyclerViewAdapter<SectionedViewHolder>, coord: ItemCoord) {
                            val sectionExpanded = adapter.isSectionExpanded(coord.section())
                            if (sectionExpanded) {
                                adapter.collapseSection(coord.section())
                            } else {
                                adapter.expandSection(coord.section())
                            }
                        }

                        override fun onFooter(adapter: SectionedRecyclerViewAdapter<SectionedViewHolder>, coord: ItemCoord) {

                        }

                        override fun onItem(adapter: SectionedRecyclerViewAdapter<SectionedViewHolder>, coord: ItemCoord) {

                            val item = (adapter as SectionedFolderListAdapter).getItem(coord)
                            contentView.setTag(R.id.item, item)
                            //                            onClickFolderListItem(item, contentView);
                        }
                    })
        }
        val longClickListener = { view: View, position: Int ->

        }
        folderList.addOnItemTouchListener(
                RecyclerItemTouchListener(
                        activity, folderList, clickListener, longClickListener))
    }

    private fun onClickFolderListItem(item: SectionedFolderListAdapter.Item, contentView: View) {


        val msg = getString(R.string.move_selected_images_to_directory_s, item.file.name)

        // 移动文件描述信息
        ViewUtils.setText(contentView, R.id.desc, msg)

        setStatusDetecting(contentView)

        mHiddenFolders?.let {
            ImageModule
                    .detectFileConflict(item.file, it)
                    .compose(RxUtils.applySchedulers())
                    .subscribe({ moveFileDetectResult ->
                        val colorOk = this@ExcludeFolderDialogFragment.resources.getColor(android.R.color.holo_green_dark)
                        val colorConflict = this@ExcludeFolderDialogFragment.resources.getColor(android.R.color.holo_red_dark)

                        moveFileDetectResult?.let {
                            val conflictFiles = moveFileDetectResult.conflictFiles
                            val canMoveFiles = moveFileDetectResult.canMoveFiles
                            val btnMoveFile = contentView.findViewById<Button>(R.id.btn_positive)

                            conflictFiles?.let {

                                if (it.isEmpty()) {
                                    setDetectingResultText(contentView, getString(R.string.can_move_all_files, canMoveFiles?.size?:0), colorOk)

                                    btnMoveFile.setTextColor(resources.getColor(R.color.colorAccent))
                                } else {

                                    btnMoveFile.text = resources.getString(R.string.move_file_d_d,
                                            mHiddenFolders!!.size - conflictFiles.size, mHiddenFolders!!.size)
                                    btnMoveFile.setTextColor(resources.getColor(R.color.warning))

                                    setDetectingResultText(contentView,
                                            getString(R.string.target_directory_exists__d_files_in_the_same_name,
                                                    item.file.name,
                                                    conflictFiles.size), colorConflict)
                                }
                            }
                        }
                    }) { throwable ->
                        throwable.printStackTrace()
                        setDetectingResultText(contentView, getString(R.string.detect_move_file_action_result_failed),
                                this@ExcludeFolderDialogFragment.resources.getColor(android.R.color.holo_red_light))
                    }
        }


    }

    private fun setDetectingResultText(contentView: View, resultText: String, textColor: Int) {
        if (!mEnableDetectSelectedFolder) {
            return
        }

        // hide detect progress
        ViewUtils.setViewVisibility(contentView, R.id.detect_info_layout, View.INVISIBLE)

        // detect result
        val detectResult = contentView.findViewById<TextView>(R.id.detect_result_info)
        detectResult.visibility = View.VISIBLE
        detectResult.setTextColor(textColor)
        detectResult.text = resultText
    }

    private fun setStatusDetecting(contentView: View) {

        if (!mEnableDetectSelectedFolder) {
            return
        }
        // Show detecting info
        ViewUtils.setViewVisibility(contentView, R.id.detect_info_layout, View.VISIBLE)

        (contentView.findViewById<View>(R.id.detect_progress_desc) as TextView).text = getString(R.string.detecting_move_file_operationg_result)

        // Hide detect result info
        ViewUtils.setViewVisibility(contentView, R.id.detect_result_info, View.INVISIBLE)
    }

    private fun <R> getViewItemTag(view: View, id: Int): R? {
        val tag = view.getTag(id)
        return if (tag != null) {
            tag as R
        } else null
    }

//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//
//        if (!mCreateDialog) {
//            return super.onCreateDialog(savedInstanceState)
//        }
//        // All later view operation should relative to this content view, butterknife will failed
//        val contentView = createContentView(null, savedInstanceState)
//
//        return AlertDialog.Builder(activity!!)
//                .setTitle(R.string.move_selected_files_to)
//                .setView(contentView)
//                .setPositiveButton(R.string.move_file) { dialog, which -> moveFile(contentView) }
//                .setNegativeButton(R.string.cancel) { dialog, which -> storePosition(contentView) }
//                .setOnCancelListener { dialog ->
//                    // Store folder list position
//                    storePosition(contentView)
//                }
//                .setOnDismissListener { dialog ->
//                    // Store folder list position
//                    storePosition(contentView)
//                }
//                .create()
//
//
//        /*return new MaterialDialog.Builder(this.getContext())
//                .title(R.string.move_selected_files_to)
//                .customView(contentView, false)
//                .positiveText(R.string.move_file)
//                .negativeText(R.string.cancel)
//                .onPositive((dialog, which) -> {
//                    Object tag = contentView.getTag(R.id.item);
//                    if (tag != null && tag instanceof SectionedFolderListAdapter.Item) {
//
//                        File destDir = ((SectionedFolderListAdapter.Item) tag).getFile();
//                        ImageService.getInstance()
//                                .moveFilesToDirectory(destDir, Stream.of(mHiddenFolders).map(File::new).toList())
//                                .compose(RxUtils.applySchedulers())
//                                .subscribe(count -> {
//                                    if (count == mHiddenFolders.size()) {
//                                        ToastUtils.toastLong(getActivity(),
//                                                getActivity().getString(R.string.already_moved_d_files, count));
//                                    } else if (count > 0 && count < mHiddenFolders.size()) {
//                                        // 部分文件移动失败
//                                        ToastUtils.toastShort(getActivity(), R.string.move_files_successfully_but_);
//                                    } else {
//                                        ToastUtils.toastShort(getActivity(), R.string.move_files_failed);
//                                    }
//                                }, throwable -> {
//                                    ToastUtils.toastShort(getActivity(), R.string.move_files_failed);
//                                });
//                    }
//                })
//                .onNegative((dialog, which) -> {
//
//                })
//                //                            .adapter(adapter, new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false))
//                .build();
//*/
//
//
//    }

    private fun moveFile(contentView: View) {
        val item = getViewItemTag<SectionedFolderListAdapter.Item>(contentView, R.id.item)
        if (item != null) {

            val destDir = item.file
            mHiddenFolders?.let {

                ImageModule
                        .moveFilesToDirectory(destDir, it, true, false)
                        .compose(RxUtils.applySchedulers())
                        .subscribe({ moveFileResult ->
                            storePosition(contentView)

                            if (moveFileResult != null) {
                                val successFiles = moveFileResult.successFiles
                                val conflictFiles = moveFileResult.conflictFiles
                                val failedFiles = moveFileResult.failedFiles

                                try {
                                    EventBus.getDefault().post(
                                            MoveFileResultMessage()
                                                    .setDestDir(destDir)
                                                    .setResult(moveFileResult))

                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                }

                                val successCount = successFiles.size
                                if (successCount == it.size) {
                                    ToastUtils.toastLong(CoreModule.appContext,
                                            R.string.already_moved_d_files, successCount)
                                } else {
                                    Log.w(TAG, "移动文件冲突: $conflictFiles")
                                    if (!conflictFiles.isEmpty()) {

                                    } else {
                                        ToastUtils.toastShort(CoreModule.appContext, R.string.move_files_failed)
                                    }

                                    if (successCount > 0 && successCount < mHiddenFolders!!.size) {
                                        // 部分文件移动失败
                                        //                                            ToastUtils.toastShort(getActivity(), R.string.move_files_successfully_but_);

                                    } else {
                                    }
                                }
                            }
                        }) { throwable ->
                            throwable.printStackTrace()
                            if (activity != null) {
                                ToastUtils.toastShort(activity, getString(R.string.move_files_failed))
                            }
                        }
            }

        }
    }

    private fun storePosition(contentView: View) {
        val rv = contentView.findViewById<RecyclerView>(R.id.folder_list)
        val lm = rv.layoutManager
        if (lm is LinearLayoutManager) {
            val firstVisibleItemPosition = lm.findFirstVisibleItemPosition()
            UserModule.moveFileDialogFirstVisibleItemPosition = firstVisibleItemPosition
        } else if (lm is GridLayoutManager) {
            val firstVisibleItemPosition = lm.findFirstVisibleItemPosition()
            UserModule.moveFileDialogFirstVisibleItemPosition = firstVisibleItemPosition
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {

    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onKeypadAddCharacter(addCharacter: String) {

    }

    override fun onKeypadDeleteCharacter(deleteCharacter: String) {

    }

    override fun onKeypadInputTextChanged(curCharacter: String) {}

    override fun onKeypadHide() {

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
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {

        val TAG = ExcludeFolderDialogFragment::class.java.simpleName

        private val ARG_EXCLUDE_FOLDERS = "ARG_EXCLUDE_FOLDERS"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param hiddenFiles Parameter 2.
         * @return A new instance of fragment MoveFileDialogFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(hiddenFiles: ArrayList<String>): ExcludeFolderDialogFragment {
            val fragment = ExcludeFolderDialogFragment()

            val args = Bundle()
            args.putStringArrayList(ARG_EXCLUDE_FOLDERS, hiddenFiles)
            fragment.arguments = args

            return fragment
        }
    }
}// Required empty public constructor

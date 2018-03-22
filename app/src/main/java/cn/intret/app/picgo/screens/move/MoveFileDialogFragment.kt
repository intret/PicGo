package cn.intret.app.picgo.screens.move

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.PopupMenu
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import butterknife.ButterKnife
import cn.intret.app.picgo.R
import cn.intret.app.picgo.di.ActivityScoped
import cn.intret.app.picgo.model.CoreModule
import cn.intret.app.picgo.model.event.FolderModelChangeMessage
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.model.image.data.DetectFileExistenceResult
import cn.intret.app.picgo.model.image.data.FolderModel
import cn.intret.app.picgo.model.user.UserModule
import cn.intret.app.picgo.screens.adapter.SectionedFolderListAdapter
import cn.intret.app.picgo.screens.adapter.SectionedListItemClickDispatcher
import cn.intret.app.picgo.screens.adapter.SectionedListItemDispatchListener
import cn.intret.app.picgo.screens.adapter.brvah.FolderListAdapterUtils
import cn.intret.app.picgo.screens.conflict.ConflictResolverDialogFragment
import cn.intret.app.picgo.screens.event.MoveFileResultMessage
import cn.intret.app.picgo.utils.*
import cn.intret.app.picgo.view.T9KeypadView
import cn.intret.app.picgo.widget.RecyclerItemTouchListener
import cn.intret.app.picgo.workaround.DaggerBottomSheetDialogFragment
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.sectionedrecyclerview.ItemCoord
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.annimon.stream.Stream
import com.jakewharton.rxbinding2.widget.RxTextView
import com.pawegio.kandroid.inflateLayout
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotterknife.bindView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList


/**
 * Move selected file ( Fragment argument specified ) to user selected target folder.
 */
@ActivityScoped
class MoveFileDialogFragment : DaggerBottomSheetDialogFragment(),
        MoveFileContracts.View,
        T9KeypadView.OnT9KeypadInteractionHandler {

    internal val mFolderList: RecyclerView by bindView(R.id.folder_list)

    internal val mDesc: TextView by bindView(R.id.desc)
    internal val mT9KeypadView: T9KeypadView by bindView(R.id.t9_keypad)
    internal val mKeyboardSwitchLayout: View by bindView(R.id.keyboard_switch_layout)
    internal val mKeyboardSwitchIv: ImageView by bindView(R.id.keyboard_switch)


    private var mListAdapter: SectionedFolderListAdapter? = null
    private val mEnableDetectSelectedFolder = false
    private var mShouldApplyT9Filter = false

    private var mSelectedFiles: List<File>? = ArrayList<File>()
    private var mListener: OnFragmentInteractionListener? = null


    private var mDialogInputObservable: Observable<CharSequence>? = null

    @Inject
    lateinit var mPresenter: MoveFileContracts.Presenter
    internal var mCreateDialog = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            val files = arguments!!.getStringArrayList(ARG_SELECTED_FILES)
            mSelectedFiles = LinkedList<File>()
            if (files == null) {
                // mSelectedFiles = LinkedList<File>()
            } else {
                var list = files.map { File(it) }.toList()
                var defaultIfNull = org.apache.commons.collections4.ListUtils.defaultIfNull(list, LinkedList<File>())
                //mSelectedFiles = defaultIfNull
            }
        }
//        mPresenter = MoveFilePresenter(this)

        EventBus.getDefault().register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        Log.d(TAG, "onCreateView() called with: inflater = [$inflater], container = [$container], savedInstanceState = [$savedInstanceState]")
        return context?.inflateLayout(R.layout.fragment_move_file_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initContentView(view)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart() called")

        loadFolderList()
    }

    override fun onDestroy() {
        try {
            EventBus.getDefault().unregister(this)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }

        super.onDestroy()
    }

    private fun loadFolderList() {
        mPresenter.loadFolderList()
    }

    private fun showFolderListAdapter(adapter: SectionedFolderListAdapter) {
        // Show loaded adapter
        mListAdapter = adapter

        mListAdapter?.let {
            it.mShowHeaderOptionButton = true
            mFolderList.adapter = it
            //        mListAdapter.setOnItemClickListener(new SectionedFolderListAdapter.OnItemClickListener() {
            //            @Override
            //            public void onSectionHeaderClick(SectionedFolderListAdapter.Section section, int sectionIndex, int adapterPosition) {
            //
            //            }
            //
            //            @Override
            //            public void onSectionHeaderOptionButtonClick(View v, SectionedFolderListAdapter.Section section, int sectionIndex) {
            //                showFolderSectionHeaderOptionPopupMenu(v, section, getActivity());
            //            }
            //
            //            @Override
            //            public void onItemClick(SectionedFolderListAdapter.Section sectionItem, int section, SectionedFolderListAdapter.Item item, int relativePos) {
            //
            //            }
            //
            //            @Override
            //            public void onItemLongClick(View v, SectionedFolderListAdapter.Section sectionItem, int section, SectionedFolderListAdapter.Item item, int relativePos) {
            //
            //            }
            //        });

            // Restore position
            val visibleItemPosition = UserModule.moveFileDialogFirstVisibleItemPosition
            if (visibleItemPosition != RecyclerView.NO_POSITION) {
                mFolderList.scrollToPosition(visibleItemPosition)
            }
            // 文件冲突检测
            //mPresenter.detectFileExistence(mSelectedFiles)
        }
    }

    private fun setAdapterMoveFileSourceDir(adapter: SectionedFolderListAdapter): SectionedFolderListAdapter {

        if (!ListUtils.isEmpty(mSelectedFiles)) {
            adapter.mMoveFileSourceDir = mSelectedFiles?.get(0)?.parentFile
        }

        return adapter
    }

    private fun showFolderSectionHeaderOptionPopupMenu(v: View, section: SectionedFolderListAdapter.Section, context: Context) {
        val popupMenu = PopupMenu(v.context, v)
        popupMenu.inflate(R.menu.folder_header_option_menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.create_folder -> {
                    MaterialDialog.Builder(context)
                            .title(R.string.create_folder)
                            .input(R.string.input_new_folder_name, R.string.new_folder_prefill, false) { dialog, input ->
                                val isValid = input.length > 1 && input.length <= 16
                                val actionButton = dialog.getActionButton(DialogAction.POSITIVE)
                                if (actionButton != null) {
                                    actionButton.isClickable = isValid
                                }
                            }
                            .alwaysCallInputCallback()
                            .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
                            .positiveText(R.string.create_folder)
                            .onPositive { dialog, which ->
                                val inputEditText = dialog.inputEditText
                                if (inputEditText != null) {
                                    val folderName = inputEditText.editableText.toString()
                                    val dir = File(section.file, folderName)
                                    ImageModule
                                            .createFolder(dir)
                                            .compose(RxUtils.applySchedulers())
                                            .subscribe({ ok ->
                                                if (ok!!) {
                                                    ToastUtils.toastLong(context, getString(R.string.created_folder_s, folderName))

                                                }
                                            }) { throwable ->
                                                Log.d(TAG, "新建文件夹失败：" + throwable.message)
                                                ToastUtils.toastLong(context, R.string.create_folder_failed)
                                            }
                                }
                            }
                            .negativeText(R.string.cancel)
                            .show()
                }
                R.id.folder_detail -> ToastUtils.toastShort(context, R.string.unimplemented)
            }
            false
        }
        popupMenu.show()
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

    private fun createContentView(root: ViewGroup?): View {
        val contentView = LayoutInflater.from(activity).inflate(R.layout.fragment_move_file_dialog, root, false)


        initContentView(contentView)

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

    private fun initContentView(contentView: View) {

        ButterKnife.bind(this@MoveFileDialogFragment, contentView)

        initHeader(contentView)
        initFolderList(contentView)
        initDialPad(contentView)
    }

    private fun initHeader(contentView: View) {
        val btnMoveFile = contentView.findViewById<Button>(R.id.btn_positive)

        btnMoveFile.text = resources.getString(R.string.move_file_d_, mSelectedFiles?.size)
        btnMoveFile.setOnClickListener { v ->
            moveFile(contentView)
            dismiss()
        }

        val btnCreateFolder = contentView.findViewById<Button>(R.id.btn_create_folder)
        btnCreateFolder.setOnClickListener { v ->
            //createFolder(contentView);
        }
    }

    internal inner class FolderItem {

        var file: File? = null
        var name: String? = null
        fun setFile(file: File): FolderItem {
            this.file = file
            return this
        }

        fun setName(name: String): FolderItem {
            this.name = name
            return this
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(message: FolderModelChangeMessage) {

    }

    private fun createFolder(dir: File) {

        val items = LinkedList<FolderItem>()
        val dcimDir = SystemUtils.getDCIMDir()
        val picturesDir = SystemUtils.getPicturesDir()

        items.add(FolderItem().setFile(dcimDir).setName(dcimDir.name))
        items.add(FolderItem().setFile(picturesDir).setName(picturesDir.name))

        showCreateFolderDialog(context, dir)
    }

    private fun showCreateFolderDialog(c: Context?, targetDir: File) {

        // TODO: 包含创建文件目标目录的 Dialog title
        val md = MaterialDialog.Builder(c!!)
                .title(getString(R.string.input_new_folder_name))
                .input(getString(R.string.input_new_folder_name), getString(R.string.new_folder_prefill)) { dialog, input ->
                    val isValid = input.length > 1 && input.length <= 16

                    val actionButton = dialog.getActionButton(DialogAction.POSITIVE)
                    if (actionButton != null) {
                        actionButton.isEnabled = false
                    }
                }
                .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
                .alwaysCallInputCallback()
                .positiveText(R.string.create_folder)
                .onPositive { dialog, which ->

                    val inputEditText = dialog.inputEditText
                    if (inputEditText != null) {
                        val folderName = inputEditText.editableText.toString()

                        val newFolder = File(targetDir, folderName)
                        ImageModule
                                .createFolder(newFolder)
                                .compose(RxUtils.applySchedulers())
                                .subscribe({ ok ->
                                    if (ok!!) {
                                        // 在 FolderModelChangeMessage 消息中更新界面
                                        filterFolderList(mFolderList, "", targetDir)
                                    }
                                }) { throwable ->
                                    Log.d(TAG, "新建文件夹失败：" + throwable.message)
                                    ToastUtils.toastLong(this@MoveFileDialogFragment.activity, R.string.create_folder_failed)
                                }
                    }
                }
                .negativeText(R.string.cancel)
                .build()


        val inputEditText = md.inputEditText
        if (inputEditText != null) {
            mDialogInputObservable = RxTextView.textChanges(inputEditText).debounce(500, TimeUnit.MILLISECONDS)

            mDialogInputObservable!!.observeOn(AndroidSchedulers.mainThread()).subscribe { charSequence ->
                try {
                    val actionButton = md.getActionButton(DialogAction.POSITIVE)
                    if (actionButton != null) {

                        val newFolder = File(targetDir, charSequence.toString())
                        actionButton.isEnabled = !newFolder.exists()
                    }
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                }
            }
        }

        md.show()
    }

    private fun initDialPad(contentView: View) {
        val keypadContainer = contentView.findViewById<ViewGroup>(R.id.t9_keypad_container)
        var t9KeypadView = contentView.findViewById<T9KeypadView>(R.id.t9_keypad)
        val folderList = contentView.findViewById<RecyclerView>(R.id.folder_list)
        val folderListContainer = contentView.findViewById<ViewGroup>(R.id.folder_list_container)


        t9KeypadView
                .keypadInputObservable
                .debounce(369, TimeUnit.MILLISECONDS)
                .subscribe { input ->
                    Log.d(TAG, "initDialPad: dial")
                    val inputString = input.toString()

                    // 只要输入有一次不为空就开启过滤模式
                    if (!TextUtils.isEmpty(inputString)) {
                        if (!mShouldApplyT9Filter) {
                            mShouldApplyT9Filter = true
                        }
                    }

                    if (!mShouldApplyT9Filter) {
                        Log.w(TAG, "initDialPad: 不进行过滤")
                        return@subscribe
                    }

                    filterFolderList(folderList, inputString, null)
                }

        //        showKeyboard(keypadContainer, (ImageView) contentView.findViewById(R.id.keyboard_switch_image_view));
    }

    private fun filterFolderList(folderList: RecyclerView?, inputString: String, scrollToDir: File?) {
        ImageModule
                .loadFolderModel(true, inputString)
                .map<SectionedFolderListAdapter>(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .map<SectionedFolderListAdapter>(this::setAdapterMoveFileSourceDir)
                .map<SectionedFolderListAdapter>(this::addNewFolderItem)
                .compose(RxUtils.applySchedulers())
                .subscribe({ newAdapter: SectionedFolderListAdapter? ->

                    if (folderList != null) {
                        if (folderList.adapter != null) {

                            newAdapter?.let {
                                val currAdapter = folderList.adapter as SectionedFolderListAdapter
                                currAdapter.diffUpdate(newAdapter)
                                currAdapter.scrollToItem(scrollToDir)
                            }

                        } else {
                            folderList.adapter = newAdapter
                        }
                    } else {
                        Log.w(TAG, "initDialPad: show null adapter")
                    }
                })
    }

    private fun addNewFolderItem(adapter: SectionedFolderListAdapter): SectionedFolderListAdapter {
        for (i in 0 until adapter.sections.size) {
            val sections = adapter.sections

            val section = sections[i]
            section.items
                    ?.add(0, SectionedFolderListAdapter.Item()
                            .setFile(section.file)
                            .setItemSubType(SectionedFolderListAdapter.ItemSubType.ADD_ITEM)
                            .setName(getString(R.string.create_folder))
                            .setCount(-1)
                    )
        }
        return adapter
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
        keyboardSwitchIv
                .setImageResource(R.drawable.keyboard_show_selector)
    }

    private fun hideKeyboard() {
        ViewUtil.hideView(mT9KeypadView)
        mKeyboardSwitchIv
                .setBackgroundResource(R.drawable.keyboard_show_selector)
    }

    private fun showKeyboard() {
        ViewUtil.showView(mT9KeypadView)
        mKeyboardSwitchIv.setBackgroundResource(R.drawable.keyboard_hide_selector)
    }

    private fun showKeyboard(keypadContainer: ViewGroup, keypadSwitchButton: ImageView) {

        ViewUtil.showView(keypadContainer)
        keypadContainer.requestFocus()
        keypadSwitchButton.setImageResource(R.drawable.keyboard_hide_selector)
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
                            onClickFolderListItem(item, contentView)
                        }
                    })
        }
        val longClickListener = { view: View, position: Int ->

        }
        folderList.addOnItemTouchListener(
                RecyclerItemTouchListener(
                        activity, folderList, clickListener, longClickListener))
    }

    private fun onClickFolderListItem(item: SectionedFolderListAdapter.Item?, contentView: View) {
        if (item!!.mItemSubType == SectionedFolderListAdapter.ItemSubType.ADD_ITEM) {
            createFolder(item.mFile!!)

        } else {
            val msg = getString(R.string.move_selected_images_to_directory_s, item.mFile?.name)

            // 移动文件描述信息
            ViewUtils.setText(contentView, R.id.desc, msg)

            setStatusDetecting(contentView)

            // TODO: 直接从 adapter 中获取冲突文件信息就可以了

            mSelectedFiles?.whenNotNullNorEmpty {
                item.mFile?.let { targetDir ->
                    ImageModule
                            .detectFileConflict(targetDir, it)
                            .compose(RxUtils.applySchedulers())
                            .subscribe({ moveFileDetectResult ->
                                val colorOk = this@MoveFileDialogFragment.resources.getColor(android.R.color.holo_green_dark)
                                val colorConflict = this@MoveFileDialogFragment.resources.getColor(android.R.color.holo_red_dark)

                                if (moveFileDetectResult != null) {
                                    val conflictFiles = moveFileDetectResult.conflictFiles
                                    val canMoveFiles = moveFileDetectResult.canMoveFiles
                                    val btnMoveFile = contentView.findViewById<Button>(R.id.btn_positive)

                                    conflictFiles?.let {
                                        if (it.isEmpty()) {
                                            setDetectingResultText(contentView, getString(R.string.can_move_all_files,
                                                    canMoveFiles?.size ?: 0), colorOk)

                                            btnMoveFile.setTextColor(resources.getColor(R.color.colorAccent))
                                        } else {

                                            btnMoveFile.text = resources.getString(R.string.move_file_d_d,
                                                    mSelectedFiles?.size?.minus(conflictFiles.size), mSelectedFiles?.size)
                                            btnMoveFile.setTextColor(resources.getColor(R.color.warning))

                                            setDetectingResultText(contentView,
                                                    getString(R.string.target_directory_exists__d_files_in_the_same_name,
                                                            targetDir.name,
                                                            conflictFiles.size), colorConflict)
                                        }
                                    }
                                }
                            }) { throwable ->
                                throwable.printStackTrace()
                                setDetectingResultText(contentView, getString(R.string.detect_move_file_action_result_failed),
                                        this@MoveFileDialogFragment.resources.getColor(android.R.color.holo_red_light))
                            }

                }
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        if (!mCreateDialog) {
            return super.onCreateDialog(savedInstanceState)
        }
        // All later view operation should relative to this content view, butterknife will failed
        val contentView = createContentView(null)

        return AlertDialog.Builder(activity!!)
                .setTitle(R.string.move_selected_files_to)
                .setView(contentView)
                .setPositiveButton(R.string.move_file) { dialog, which -> moveFile(contentView) }
                .setNegativeButton(R.string.cancel) { dialog, which -> storePosition(contentView) }
                .setOnCancelListener { dialog ->
                    // Store folder list position
                    storePosition(contentView)
                }
                .setOnDismissListener { dialog ->
                    // Store folder list position
                    storePosition(contentView)
                }
                .create()


        /*return new MaterialDialog.Builder(this.getContext())
                .title(R.string.move_selected_files_to)
                .customView(contentView, false)
                .positiveText(R.string.move_file)
                .negativeText(R.string.cancel)
                .onPositive((dialog, which) -> {
                    Object tag = contentView.getTag(R.id.item);
                    if (tag != null && tag instanceof SectionedFolderListAdapter.Item) {

                        File destDir = ((SectionedFolderListAdapter.Item) tag).getFile();
                        ImageService.getInstance()
                                .moveFilesToDirectory(destDir, Stream.of(mSelectedFiles).map(File::new).toList())
                                .compose(RxUtils.applySchedulers())
                                .subscribe(count -> {
                                    if (count == mSelectedFiles.size()) {
                                        ToastUtils.toastLong(getActivity(),
                                                getActivity().getString(R.string.already_moved_d_files, count));
                                    } else if (count > 0 && count < mSelectedFiles.size()) {
                                        // 部分文件移动失败
                                        ToastUtils.toastShort(getActivity(), R.string.move_files_successfully_but_);
                                    } else {
                                        ToastUtils.toastShort(getActivity(), R.string.move_files_failed);
                                    }
                                }, throwable -> {
                                    ToastUtils.toastShort(getActivity(), R.string.move_files_failed);
                                });
                    }
                })
                .onNegative((dialog, which) -> {

                })
                //                            .adapter(adapter, new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false))
                .build();
*/


    }

    private fun moveFile(contentView: View) {
        val item = getViewItemTag<SectionedFolderListAdapter.Item>(contentView, R.id.item)
        if (item != null) {


            mSelectedFiles?.let {

                item.mFile?.let { destDir ->

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
                                    if (successCount == mSelectedFiles?.size) {
                                        ToastUtils.toastLong(CoreModule.appContext,
                                                R.string.already_moved_d_files, successCount)
                                    } else {
                                        Log.w(TAG, "移动文件冲突: $conflictFiles")
                                        if (!conflictFiles.isEmpty()) {

                                        } else {
                                            ToastUtils.toastShort(CoreModule.appContext, R.string.move_files_failed)
                                        }

                                        if (successCount > 0 && successCount < mSelectedFiles?.size ?: 0) {
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

    private fun showConflictDialog(destDir: File, conflictFiles: List<Pair<File, File>>) {

        if (conflictFiles.isEmpty()) {
            return
        }

        val conflictFilePath = Stream.of(conflictFiles)
                .map { fileFilePair -> fileFilePair.second.absolutePath }
                .toList()

        val fragment = ConflictResolverDialogFragment.newInstance(destDir.absolutePath, ArrayList(conflictFilePath))

        fragment.show(activity!!.supportFragmentManager, "Conflict Resolver Dialog")
    }

    private fun createConflictDialogContentView(): View? {

        return null
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

        val TAG = MoveFileDialogFragment::class.java.simpleName

        private val ARG_SELECTED_FILES = "selected_files"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param selectedFiles Parameter 2.
         * @return A new instance of fragment MoveFileDialogFragment.
         */
        // TODO: Rename and change types and number of parameters
        fun newInstance(selectedFiles: ArrayList<String>): MoveFileDialogFragment {
            val fragment = MoveFileDialogFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_SELECTED_FILES, selectedFiles)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onLoadFolderModelSuccess(folderModel: FolderModel) {
        val adapter = FolderListAdapterUtils.folderModelToSectionedFolderListAdapter(folderModel)
        this.setAdapterMoveFileSourceDir(adapter)
        this.addNewFolderItem(adapter)
        this.showFolderListAdapter(adapter)
    }

    override fun onLoadFolderModelFailed() {
        ToastUtils.toastShort(this@MoveFileDialogFragment.activity, R.string.load_folder_list_failed)
    }

    override fun onDetectFileExistenceResult(detectFileExistenceResult: DetectFileExistenceResult) {
        Log.w(TAG, "onStart: 文件冲突 $detectFileExistenceResult")
        detectFileExistenceResult.existedFiles?.let {
            mListAdapter?.updateConflictFiles(it.toMap())
        }
    }

    override fun onDetectFileExistenceFailed(sourceFiles: List<File>?) {

    }

}// Required empty public constructor

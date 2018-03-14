package cn.intret.app.picgo.ui.main;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.annimon.stream.Stream;
import com.chad.library.adapter.base.BaseViewHolder;
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback;
import com.chad.library.adapter.base.listener.OnItemSwipeListener;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.IExpandable;
import com.mikepenz.fastadapter.IItem;
import com.mikepenz.fastadapter.ISelectionListener;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter_extensions.RangeSelectorHelper;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.app.CoreModule;
import cn.intret.app.picgo.model.image.FolderModel;
import cn.intret.app.picgo.model.image.ImageFolder;
import cn.intret.app.picgo.model.image.ImageModule;
import cn.intret.app.picgo.model.user.UserModule;
import cn.intret.app.picgo.ui.adapter.brvah.ExpandableFolderAdapter;
import cn.intret.app.picgo.ui.adapter.brvah.FolderListAdapter;
import cn.intret.app.picgo.ui.adapter.brvah.FolderListAdapterUtils;
import cn.intret.app.picgo.ui.adapter.SectionedFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.SectionedListItemClickDispatcher;
import cn.intret.app.picgo.ui.adapter.SectionedListItemDispatchListener;
import cn.intret.app.picgo.ui.adapter.fast.FolderItem;
import cn.intret.app.picgo.ui.adapter.fast.SectionItem;
import cn.intret.app.picgo.ui.event.MoveFileResultMessage;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.RxUtils;
import cn.intret.app.picgo.utils.ToastUtils;
import cn.intret.app.picgo.utils.ViewUtil;
import cn.intret.app.picgo.utils.ViewUtils;
import cn.intret.app.picgo.view.T9KeypadView;
import cn.intret.app.picgo.widget.RecyclerItemTouchListener;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * Move selected file ( Fragment argument specified ) to user selected target folder.
 */
public class ExcludeFolderDialogFragment extends BottomSheetDialogFragment implements T9KeypadView.OnT9KeypadInteractionHandler {

    public static final String TAG = ExcludeFolderDialogFragment.class.getSimpleName();

    private static final String ARG_EXCLUDE_FOLDERS = "ARG_EXCLUDE_FOLDERS";

    private List<File> mHiddenFolders;

    private OnFragmentInteractionListener mListener;

    @BindView(R.id.folder_list) RecyclerView mFolderList;
    @BindView(R.id.desc) TextView mDesc;

    @BindView(R.id.t9_keypad) T9KeypadView mT9KeypadView;

    @BindView(R.id.keyboard_switch_layout) View mKeyboardSwitchLayout;
    @BindView(R.id.keyboard_switch) ImageView mKeyboardSwitchIv;
    private SectionedFolderListAdapter mListAdapter;
    private FolderListAdapter mAdapter;

    private boolean mEnableDetectSelectedFolder = false;
    private RangeSelectorHelper mRangeSelectorHelper;
    private ExpandableFolderAdapter mExpandableAdapter;
//    private DragSelectTouchListener mDragSelectTouchListener;

    private ItemTouchHelper mItemTouchHelper;
    private ItemDragAndSwipeCallback mItemDragAndSwipeCallback;

    public ExcludeFolderDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param hiddenFiles Parameter 2.
     * @return A new instance of fragment MoveFileDialogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ExcludeFolderDialogFragment newInstance(ArrayList<String> hiddenFiles) {
        ExcludeFolderDialogFragment fragment = new ExcludeFolderDialogFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(ARG_EXCLUDE_FOLDERS, hiddenFiles);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ArrayList<String> files = getArguments().getStringArrayList(ARG_EXCLUDE_FOLDERS);
            if (files == null) {
                mHiddenFolders = new LinkedList<>();
            } else {
                mHiddenFolders = Stream.of(files).map(File::new).toList();
            }
        }
    }

    //save our FastAdapter
    private FastItemAdapter<IItem> mItemAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView() called with: inflater = [" + inflater + "], container = [" + container + "], savedInstanceState = [" + savedInstanceState + "]");

        return createContentView(container, savedInstanceState);
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");

        loadSectionFolderList();

        //loadExpandableFolderList();
    }

    private void loadExpandableFolderList() {
        ImageModule.getInstance()
                .loadHiddenFileListModel()
                .map(FolderListAdapterUtils::folderModelToExpandableFolderAdapter)
                .subscribe(this::showExpandableFolderList, RxUtils::unhandledThrowable);
    }

    private void showExpandableFolderList(ExpandableFolderAdapter adapter) {

        adapter.setOnItemClickListener((baseQuickAdapter, view, i) ->
                Log.d(TAG, "onItemClick() called with: baseQuickAdapter = [" + baseQuickAdapter + "], view = [" + view + "], i = [" + i + "]"));
        adapter.setOnInteractionListener(item -> {

        });

        final GridLayoutManager manager = new GridLayoutManager(this.getActivity(), 1);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.getItemViewType(position) == ExpandableFolderAdapter.TYPE_LEVEL_1 ? 1 : manager.getSpanCount();
            }
        });

        OnItemSwipeListener onItemSwipeListener = new OnItemSwipeListener() {
            @Override
            public void onItemSwipeStart(RecyclerView.ViewHolder viewHolder, int pos) {
                Log.d(TAG, "view swiped start: " + pos);
                BaseViewHolder holder = ((BaseViewHolder) viewHolder);
//                holder.setTextColor(R.id.tv, Color.WHITE);
            }

            @Override
            public void clearView(RecyclerView.ViewHolder viewHolder, int pos) {
                Log.d(TAG, "View reset: " + pos);
                BaseViewHolder holder = ((BaseViewHolder) viewHolder);
//                holder.setTextColor(R.id.tv, Color.BLACK);
            }

            @Override
            public void onItemSwiped(RecyclerView.ViewHolder viewHolder, int pos) {
                Log.d(TAG, "View Swiped: " + pos);
            }

            @Override
            public void onItemSwipeMoving(Canvas canvas, RecyclerView.ViewHolder viewHolder, float dX, float dY, boolean isCurrentlyActive) {
                canvas.drawColor(ContextCompat.getColor(getActivity(), R.color.list_item_swipe_delete));
//                canvas.drawText("Just some text", 0, 40, paint);
            }
        };
        //mItemDragAndSwipeCallback = new ItemDragAndSwipeCallback(adapter);

        mFolderList.setAdapter(adapter);
        mExpandableAdapter = adapter;

        // important! setLayoutManager should be called after setAdapter
        mFolderList.setLayoutManager(manager);
        adapter.expandAll();
    }

    private void loadSectionFolderList() {
        ImageModule.getInstance()
                .loadHiddenFileListModel()
                .compose(workAndShow())
                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .map(adapter -> {
                    adapter.setSelectable(false);
                    adapter.setShowCloseButton(true);
                    adapter.setOnItemClickListener(new SectionedFolderListAdapter.OnItemClickListener() {
                        @Override
                        public void onSectionHeaderClick(SectionedFolderListAdapter.Section section, int sectionIndex, int adapterPosition) {

                        }

                        @Override
                        public void onSectionHeaderOptionButtonClick(View v, SectionedFolderListAdapter.Section section, int sectionIndex) {

                        }

                        @Override
                        public void onItemClick(SectionedFolderListAdapter.Section sectionItem, int section, SectionedFolderListAdapter.Item item, int relativePos) {

                        }

                        @Override
                        public void onItemLongClick(View v, SectionedFolderListAdapter.Section sectionItem, int section, SectionedFolderListAdapter.Item item, int relativePos) {

                        }

                        @Override
                        public void onItemCloseClick(View v, SectionedFolderListAdapter.Section section, SectionedFolderListAdapter.Item item, int sectionIndex, int relativePosition) {

                            UserModule.getInstance()
                                    .getExcludeFolderPreference()
                                    .map(pref -> {
                                        LinkedList<File> files = pref.get();
                                        files.remove(item.getFile());
                                        pref.set(files);

                                        return true;
                                    })
                                    .compose(workAndShow())
                                    .subscribe(ok -> {
                                        if (ok) {

                                            adapter.removeFolderItem(sectionIndex, relativePosition);
                                        } else {
                                            ToastUtils.toastShort(ExcludeFolderDialogFragment.this.getContext(), R.string.delete_item_failed);
                                        }
                                    }, throwable -> {
                                        throwable.printStackTrace();
                                        ToastUtils.toastShort(ExcludeFolderDialogFragment.this.getContext(), R.string.delete_item_failed);
                                    });

                        }
                    });

                    return adapter;
                })
                .doOnNext(adapter -> mListAdapter = adapter)
                .subscribe(adapter -> {

                    // Show loaded adapter
                    mFolderList.setAdapter(mListAdapter);

                    // Restore position
                    int visibleItemPosition = UserModule.getInstance().getMoveFileDialogFirstVisibleItemPosition();
                    if (visibleItemPosition != RecyclerView.NO_POSITION) {
                        mFolderList.scrollToPosition(visibleItemPosition);
                    }

                }, throwable -> {
                    ToastUtils.toastShort(ExcludeFolderDialogFragment.this.getActivity(), R.string.load_folder_list_failed);
                });
    }

    private SectionedFolderListAdapter setAdapterMoveFileSourceDir(SectionedFolderListAdapter adapter) {
        if (!ListUtils.isEmpty(mHiddenFolders)) {
            adapter.setMoveFileSourceDir(mHiddenFolders.get(0).getParentFile());
        }
        return adapter;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume() called");
        super.onResume();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop() called");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView() called");
        super.onDestroyView();
    }

    private View createContentView(ViewGroup root, Bundle savedInstanceState) {
        View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_exclude_folder_list, root, false);
        ButterKnife.bind(ExcludeFolderDialogFragment.this, contentView);

        initContentView(contentView, savedInstanceState);

        ViewUtil.setHideIme(getActivity(), contentView);

        initListener(contentView);

        return contentView;
    }

    private void initListener(View contentView) {
        ViewGroup keypadContainer = contentView.findViewById(R.id.t9_keypad_container);
        keypadContainer.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                keypadContainer.setVisibility(View.INVISIBLE);
            }
        });
        T9KeypadView keypadView = contentView.findViewById(R.id.t9_keypad);

        ImageView btnKeypadSwitch = contentView.findViewById(R.id.keyboard_switch);
        View keypadSwitchLayout = contentView.findViewById(R.id.keyboard_switch_layout);

        keypadSwitchLayout.setOnClickListener(v -> switchKeyboard(keypadContainer, btnKeypadSwitch));
        btnKeypadSwitch.setOnClickListener(v -> switchKeyboard(keypadContainer, btnKeypadSwitch));

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

    private void initContentView(View contentView, Bundle savedInstanceState) {
        initHeader(contentView);
        initFolderList(contentView);
        initFolder(contentView);
        initDialPad(contentView);

        //initList(contentView, savedInstanceState);
    }

    private void initList(View contentView, Bundle savedInstanceState) {
        //create our FastAdapter

        //we init our ActionModeHelper


        //create our adapters
//        final StickyHeaderAdapter stickyHeaderAdapter = new StickyHeaderAdapter();
        mItemAdapter = new FastItemAdapter<>();

        //configure our mFastAdapter
        //as we provide id's for the items we want the hasStableIds enabled to speed up things
        mItemAdapter.withSelectable(true);
        mItemAdapter.withMultiSelect(true);
        mItemAdapter.withSelectOnLongClick(true);
        mItemAdapter.withPositionBasedStateManagement(false);
        mItemAdapter.withOnPreClickListener(new FastAdapter.OnClickListener<IItem>() {
            @Override
            public boolean onClick(View v, IAdapter adapter, IItem item, int position) {
                //we handle the default onClick behavior for the actionMode. This will return null if it didn't do anything and you can handle a normal onClick
//                Boolean res = mActionModeHelper.onClick(item);
//                return res != null ? res : false;
                return true;
            }
        });

        mItemAdapter.withOnClickListener(new FastAdapter.OnClickListener<IItem>() {
            @Override
            public boolean onClick(View view, IAdapter<IItem> iAdapter, IItem item, int i) {
                mItemAdapter.select(i);
                return true;
            }
        });

        mItemAdapter.withSelectionListener(new ISelectionListener() {
            @Override
            public void onSelectionChanged(IItem item, boolean selected) {
                if (item instanceof FolderItem) {


//                    IItem headerItem = ((FolderItem) item).getParent();
//                    if (headerItem != null) {
//                        int pos = mItemAdapter.getAdapterPosition(headerItem);
//                        // Important: notify the header directly, not via the notifyadapterItemChanged!
//                        // we just want to update the view and we are sure, nothing else has to be done
//                        Bundle payload = new Bundle();
//
//                        mItemAdapter.notifyItemChanged(pos);
//                    }
                }
            }
        });
        mItemAdapter.withOnPreLongClickListener((v, adapter, item, position) -> {
            //we do not want expandable items to be selected
            if (item instanceof IExpandable) {
                if (((IExpandable) item).getSubItems() != null) {
                    return true;
                }
            }

            return false;
        });

        // this will take care of selecting range of items via long press on the first and afterwards on the last item
        mRangeSelectorHelper = new RangeSelectorHelper(mItemAdapter)
                .withSavedInstanceState(savedInstanceState);

//        // setup the drag select listener and add it to the RecyclerView
//        mDragSelectTouchListener = new DragSelectTouchListener()
//                .withSelectListener(new DragSelectTouchListener.OnDragSelectListener()
//                {
//                    @Override
//                    public void onSelectChange(int start, int end, boolean isSelected)
//                    {
//                        mRangeSelectorHelper.selectRange(start, end, isSelected, true);
//                        // we handled the long press, so we reset the range selector
//                        mRangeSelectorHelper.reset();
//                    }
//                });
//        rv.addOnItemTouchListener(mDragSelectTouchListener);

        //get our recyclerView and do basic setup
        RecyclerView folderList = contentView.findViewById(R.id.folder_list);
        folderList.setLayoutManager(new LinearLayoutManager(this.getActivity()));
        folderList.setItemAnimator(new DefaultItemAnimator());
        folderList.setAdapter(mItemAdapter);


//        final StickyRecyclerHeadersDecoration decoration = new StickyRecyclerHeadersDecoration(stickyHeaderAdapter);
//        folderList.addItemDecoration(decoration);


        //so the headers are aware of changes
//        stickyHeaderAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
//            @Override
//            public void onChanged() {
//                decoration.invalidateHeaders();
//            }
//        });

        //init cache with the added items, this is useful for shorter lists with many many different view types (at least 4 or more
        //new RecyclerViewCacheUtil().withCacheSize(2).apply(folderList, items);

        //set the back arrow in the toolbar
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeButtonEnabled(false);

        //we define the items
        showHiddenFolders();

        //restore selections (this has to be done after the items were added
//        mFastAdapter.withSavedInstanceState(savedInstanceState);
    }

    private void showHiddenFolders() {

        ImageModule.getInstance()
                .loadHiddenFileListModel()
                .map(this::getIItems)
                .compose(workAndShow())
                .subscribe(items -> {
                    mItemAdapter.add(items);
                    mItemAdapter.expand();
                });
    }

    @NonNull
    private List<IItem> getIItems(FolderModel folder) {
        List<IItem> items = new ArrayList<>();
        for (int i = 0; i < folder.getContainerFolders().size(); i++) {
            FolderModel.ContainerFolder containerFolder = folder.getContainerFolders().get(i);

            SectionItem<SectionItem, FolderItem> section = new SectionItem<>();

            section.setHeader(containerFolder.getName());
            section.setFile(containerFolder.getFile());

            List<FolderItem> subSubItems = new LinkedList<>();
            List<ImageFolder> folders = containerFolder.getFolders();
            for (int i1 = 0; i1 < folders.size(); i1++) {
                ImageFolder imageFolder = folders.get(i1);

                FolderItem<? extends IItem> subItem = new FolderItem<>();
                subItem.setName(imageFolder.getName());
                subItem.setFile(imageFolder.getFile());
                subItem.setCount(imageFolder.getCount());
                subItem.setThumbList(imageFolder.getThumbList());

                subSubItems.add(subItem);
            }

            section.withSubItems(subSubItems);

            items.add(section);
        }
        return items;
    }

    private void initHeader(View contentView) {
        Button btnMoveFile = contentView.findViewById(R.id.btn_positive);
        btnMoveFile.setOnClickListener(v -> {
            dismiss();
        });
    }

    private void initDialPad(View contentView) {
        ViewGroup keypadContainer = contentView.findViewById(R.id.t9_keypad_container);
        T9KeypadView t9KeypadView = contentView.findViewById(R.id.t9_keypad);
        RecyclerView folderList = contentView.findViewById(R.id.folder_list);
        ViewGroup folderListContainer = contentView.findViewById(R.id.folder_list_container);


        t9KeypadView
                .getDialpadInputObservable()
                .debounce(369, TimeUnit.MILLISECONDS)
                .subscribe(input -> {

                    String inputString = input.toString();

                    ImageModule.getInstance()
                            .loadHiddenFileListModel(inputString)
                            .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                            .compose(RxUtils.applySchedulers())
                            .subscribe(newAdapter -> {

//                                if (folderList != null) {
//                                    SectionedFolderListAdapter currAdapter = (SectionedFolderListAdapter) folderList.getAdapter();
//
//                                    currAdapter.diffUpdate(newAdapter);
//                                }
                            }, RxUtils::unhandledThrowable);
                });

//        showKeyboard(keypadContainer, (ImageView) contentView.findViewById(R.id.keyboard_switch_image_view));
    }

    private void switchKeyboard(ViewGroup keypadContainer, ImageView kbSwitchIv) {

        if (ViewUtil.getViewVisibility(keypadContainer) != View.VISIBLE) {
            showKeyboard(keypadContainer, kbSwitchIv);
        } else {
            hideKeyboard(keypadContainer, kbSwitchIv);
        }
    }

    private void hideKeyboard(ViewGroup t9TelephoneDialpadView, ImageView keyboardSwitchIv) {
        ViewUtil.invisibleView(t9TelephoneDialpadView);
        keyboardSwitchIv.setImageResource(R.drawable.keyboard_show_selector);
    }

    private void hideKeyboard() {
        ViewUtil.hideView(mT9KeypadView);
        mKeyboardSwitchIv.setImageResource(R.drawable.keyboard_show_selector);
    }

    private void showKeyboard() {
        ViewUtil.showView(mT9KeypadView);
        mKeyboardSwitchIv.setImageResource(R.drawable.keyboard_hide_selector);
    }

    private void showKeyboard(ViewGroup keypadContainer, ImageView keypadSwitchButton) {

        ViewUtil.showView(keypadContainer);
        keypadContainer.requestFocus();
        keypadSwitchButton.setImageResource(R.drawable.keyboard_hide_selector);
    }

    private void initFolder(View contentView) {
        RecyclerView folderList = contentView.findViewById(R.id.folder_list);
        mFolderList = folderList;

        mFolderList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));


    }

    private void initFolderList(final View contentView) {
        RecyclerView folderList = contentView.findViewById(R.id.folder_list);

        mFolderList = folderList;

        folderList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));


        RecyclerItemTouchListener.OnItemClickListener clickListener = (view, position) -> {
            new SectionedListItemClickDispatcher<SectionedRecyclerViewAdapter>(mListAdapter)
                    .dispatch(position, new SectionedListItemDispatchListener() {
                        @Override
                        public void onHeader(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {
                            boolean sectionExpanded = adapter.isSectionExpanded(coord.section());
                            if (sectionExpanded) {
                                adapter.collapseSection(coord.section());
                            } else {
                                adapter.expandSection(coord.section());
                            }
                        }

                        @Override
                        public void onFooter(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {

                        }

                        @Override
                        public void onItem(SectionedRecyclerViewAdapter adapter, ItemCoord coord) {

                            SectionedFolderListAdapter.Item item = ((SectionedFolderListAdapter) adapter).getItem(coord);
                            contentView.setTag(R.id.item, item);
//                            onClickFolderListItem(item, contentView);
                        }
                    });
        };
        RecyclerItemTouchListener.OnItemLongClickListener longClickListener = (view, position) -> {

        };
        folderList.addOnItemTouchListener(
                new RecyclerItemTouchListener(
                        getActivity(), folderList, clickListener, longClickListener));
    }

    private void onClickFolderListItem(SectionedFolderListAdapter.Item item, View contentView) {


        String msg = getString(R.string.move_selected_images_to_directory_s, item.getFile().getName());

        // 移动文件描述信息
        ViewUtils.setText(contentView, R.id.desc, msg);

        setStatusDetecting(contentView);

        ImageModule.getInstance()
                .detectFileConflict(item.getFile(), mHiddenFolders)
                .compose(RxUtils.applySchedulers())
                .subscribe(moveFileDetectResult -> {
                    int colorOk = ExcludeFolderDialogFragment.this.getResources().getColor(android.R.color.holo_green_dark);
                    int colorConflict = ExcludeFolderDialogFragment.this.getResources().getColor(android.R.color.holo_red_dark);

                    if (moveFileDetectResult != null) {
                        List<Pair<File, File>> conflictFiles = moveFileDetectResult.getConflictFiles();
                        List<Pair<File, File>> canMoveFiles = moveFileDetectResult.getCanMoveFiles();
                        Button btnMoveFile = contentView.findViewById(R.id.btn_positive);

                        if (conflictFiles.isEmpty()) {
                            setDetectingResultText(contentView, getString(R.string.can_move_all_files, canMoveFiles.size()), colorOk);

                            btnMoveFile.setTextColor(getResources().getColor(R.color.colorAccent));
                        } else {

                            btnMoveFile.setText(getResources().getString(R.string.move_file_d_d,
                                    mHiddenFolders.size() - conflictFiles.size(), mHiddenFolders.size()));
                            btnMoveFile.setTextColor(getResources().getColor(R.color.warning));

                            setDetectingResultText(contentView,
                                    getString(R.string.target_directory_exists__d_files_in_the_same_name,
                                            item.getFile().getName(),
                                            conflictFiles.size()), colorConflict);
                        }
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    setDetectingResultText(contentView, getString(R.string.detect_move_file_action_result_failed),
                            ExcludeFolderDialogFragment.this.getResources().getColor(android.R.color.holo_red_light));
                });
    }

    private void setDetectingResultText(View contentView, String resultText, int textColor) {
        if (!mEnableDetectSelectedFolder) {
            return;
        }

        // hide detect progress
        ViewUtils.setViewVisibility(contentView, R.id.detect_info_layout, View.INVISIBLE);

        // detect result
        TextView detectResult = contentView.findViewById(R.id.detect_result_info);
        detectResult.setVisibility(View.VISIBLE);
        detectResult.setTextColor(textColor);
        detectResult.setText(resultText);
    }

    private void setStatusDetecting(View contentView) {

        if (!mEnableDetectSelectedFolder) {
            return;
        }
        // Show detecting info
        ViewUtils.setViewVisibility(contentView, R.id.detect_info_layout, View.VISIBLE);

        ((TextView) contentView.findViewById(R.id.detect_progress_desc))
                .setText(getString(R.string.detecting_move_file_operationg_result));

        // Hide detect result info
        ViewUtils.setViewVisibility(contentView, R.id.detect_result_info, View.INVISIBLE);
    }

    private <R> R getViewItemTag(View view, int id) {
        Object tag = view.getTag(id);
        if (tag != null) {
            return (R) tag;
        }
        return null;
    }

    boolean mCreateDialog = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if (!mCreateDialog) {
            return super.onCreateDialog(savedInstanceState);
        }
        // All later view operation should relative to this content view, butterknife will failed
        View contentView = createContentView(null, savedInstanceState);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.move_selected_files_to)
                .setView(contentView)
                .setPositiveButton(R.string.move_file, (dialog, which) -> {
                    moveFile(contentView);
                })
                .setNegativeButton(R.string.cancel, ((dialog, which) -> {
                    storePosition(contentView);
                }))
                .setOnCancelListener(dialog -> {
                    // Store folder list position
                    storePosition(contentView);
                })
                .setOnDismissListener(dialog -> {
                    // Store folder list position
                    storePosition(contentView);
                })
                .create();


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
                                .moveFilesToDirectory(destDir, Stream.of(mHiddenFolders).map(File::new).toList())
                                .compose(RxUtils.applySchedulers())
                                .subscribe(count -> {
                                    if (count == mHiddenFolders.size()) {
                                        ToastUtils.toastLong(getActivity(),
                                                getActivity().getString(R.string.already_moved_d_files, count));
                                    } else if (count > 0 && count < mHiddenFolders.size()) {
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

    private void moveFile(View contentView) {
        SectionedFolderListAdapter.Item item = getViewItemTag(contentView, R.id.item);
        if (item != null) {

            File destDir = item.getFile();
            ImageModule.getInstance()
                    .moveFilesToDirectory(destDir, mHiddenFolders, true, false)
                    .compose(RxUtils.applySchedulers())
                    .subscribe(moveFileResult -> {
                        storePosition(contentView);

                        if (moveFileResult != null) {
                            List<Pair<File, File>> successFiles = moveFileResult.getSuccessFiles();
                            List<Pair<File, File>> conflictFiles = moveFileResult.getConflictFiles();
                            List<Pair<File, File>> failedFiles = moveFileResult.getFailedFiles();

                            try {
                                EventBus.getDefault().post(
                                        new MoveFileResultMessage()
                                                .setDestDir(destDir)
                                                .setResult(moveFileResult));

                            } catch (Throwable e) {
                                e.printStackTrace();
                            }

                            int successCount = successFiles.size();
                            if (successCount == mHiddenFolders.size()) {
                                ToastUtils.toastLong(CoreModule.getInstance().getAppContext(),
                                        R.string.already_moved_d_files, successCount);
                            } else {
                                Log.w(TAG, "移动文件冲突: " + conflictFiles);
                                if (!conflictFiles.isEmpty()) {

                                } else {
                                    ToastUtils.toastShort(CoreModule.getInstance().getAppContext(), R.string.move_files_failed);
                                }

                                if (successCount > 0 && successCount < mHiddenFolders.size()) {
                                    // 部分文件移动失败
//                                            ToastUtils.toastShort(getActivity(), R.string.move_files_successfully_but_);

                                } else {
                                }
                            }
                        }
                    }, throwable -> {
                        throwable.printStackTrace();
                        if (getActivity() != null) {
                            ToastUtils.toastShort(getActivity(), getString(R.string.move_files_failed));
                        }
                    });
        }
    }

    private void storePosition(View contentView) {
        RecyclerView rv = contentView.findViewById(R.id.folder_list);
        RecyclerView.LayoutManager lm = rv.getLayoutManager();
        if (lm instanceof LinearLayoutManager) {
            int firstVisibleItemPosition = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
            UserModule.getInstance().setMoveFileDialogFirstVisibleItemPosition(firstVisibleItemPosition);
        } else if (lm instanceof GridLayoutManager) {
            int firstVisibleItemPosition = ((GridLayoutManager) lm).findFirstVisibleItemPosition();
            UserModule.getInstance().setMoveFileDialogFirstVisibleItemPosition(firstVisibleItemPosition);
        }
    }


    protected <T> ObservableTransformer<T, T> workAndShow() {
        return observable -> observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onKeypadAddCharacter(String addCharacter) {

    }

    @Override
    public void onKeypadDeleteCharacter(String deleteCharacter) {

    }

    @Override
    public void onKeypadInputTextChanged(String curCharacter) {
    }

    @Override
    public void onKeypadHide() {

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

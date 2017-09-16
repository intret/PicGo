package cn.intret.app.picgo.ui.main;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.ConflictResolverDialogFragment;
import cn.intret.app.picgo.model.SystemImageService;
import cn.intret.app.picgo.model.UserDataService;
import cn.intret.app.picgo.ui.adapter.FolderListAdapterUtils;
import cn.intret.app.picgo.ui.adapter.SectionedFolderListAdapter;
import cn.intret.app.picgo.ui.adapter.SectionedListItemClickDispatcher;
import cn.intret.app.picgo.ui.adapter.SectionedListItemDispatchListener;
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
public class MoveFileDialogFragment extends BottomSheetDialogFragment implements T9KeypadView.OnT9KeypadInteractionHandler {

    public static final String TAG = MoveFileDialogFragment.class.getSimpleName();

    private static final String ARG_SELECTED_FILES = "selected_files";

    private List<File> mSelectedFiles;

    private OnFragmentInteractionListener mListener;

    @BindView(R.id.folder_list) RecyclerView mFolderList;
    @BindView(R.id.desc) TextView mDesc;

    @BindView(R.id.t9_keypad) T9KeypadView mT9KeypadView;

    @BindView(R.id.keyboard_switch_layout) View mKeyboardSwitchLayout;
    @BindView(R.id.keyboard_switch_image_view) ImageView mKeyboardSwitchIv;
    private SectionedFolderListAdapter mListAdapter;
    private boolean mEnableDetectSelectedFolder = false;


    class DialogViews {
        @BindView(R.id.folder_list) public RecyclerView mFolderList;
        @BindView(R.id.desc) public TextView mDesc;

        @BindView(R.id.t9_keypad) public T9KeypadView mT9KeypadView;
    }

    DialogViews mDialogViews = new DialogViews();

    public MoveFileDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param selectedFiles Parameter 2.
     * @return A new instance of fragment MoveFileDialogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MoveFileDialogFragment newInstance(ArrayList<String> selectedFiles) {
        MoveFileDialogFragment fragment = new MoveFileDialogFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_SELECTED_FILES, selectedFiles);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ArrayList<String> files = getArguments().getStringArrayList(ARG_SELECTED_FILES);
            if (files == null) {
                mSelectedFiles = new LinkedList<>();
            } else {
                mSelectedFiles = Stream.of(files).map(File::new).toList();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView() called with: inflater = [" + inflater + "], container = [" + container + "], savedInstanceState = [" + savedInstanceState + "]");

        return createContentView(container);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() called");

        SystemImageService.getInstance()
                .loadFolderList(true)
                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .map(this::setAdapterMoveFileSourceDir)
                .doOnNext(adapter -> mListAdapter = adapter)
                .subscribe(adapter -> {

                    // Show loaded adapter
                    mFolderList.setAdapter(mListAdapter);

                    // Restore position
                    int visibleItemPosition = UserDataService.getInstance().getMoveFileDialogFirstVisibleItemPosition();
                    if (visibleItemPosition != RecyclerView.NO_POSITION) {
                        mFolderList.scrollToPosition(visibleItemPosition);
                    }

                    SystemImageService.getInstance()
                            .detectFileExistence(mSelectedFiles)
                            .compose(workAndShow())
                            .subscribe(detectFileExistenceResult -> {
                                Log.w(TAG, "onStart: 文件冲突 " + detectFileExistenceResult );

                                mListAdapter.updateConflictFiles(detectFileExistenceResult.getExistedFiles());

                            }, RxUtils::unhandledThrowable);
                }, throwable -> {
                    ToastUtils.toastShort(MoveFileDialogFragment.this.getActivity(), R.string.load_folder_list_failed);
                });


    }

    private SectionedFolderListAdapter setAdapterMoveFileSourceDir(SectionedFolderListAdapter adapter) {
        if (!ListUtils.isEmpty(mSelectedFiles)) {
            adapter.setMoveFileSourceDir(mSelectedFiles.get(0).getParentFile());
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

    private View createContentView(ViewGroup root) {
        View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_move_file_dialog, root, false);
        ButterKnife.bind(MoveFileDialogFragment.this, contentView);

        initContentView(contentView);

        ViewUtil.setHideIme(getActivity(), contentView);

        initListener(contentView);

        return contentView;
    }

    private void initListener(View contentView) {
        ViewGroup keypadContainer = (ViewGroup) contentView.findViewById(R.id.t9_keypad_container);
        keypadContainer.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                keypadContainer.setVisibility(View.INVISIBLE);
            }
        });
        T9KeypadView keypadView = ((T9KeypadView) contentView.findViewById(R.id.t9_keypad));

        ImageView btnKeypadSwitch = (ImageView) contentView.findViewById(R.id.keyboard_switch_image_view);
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

    private void initContentView(View contentView) {
        initHeader(contentView);
        initFolderList(contentView);
        initDialPad(contentView);
    }

    private void initHeader(View contentView) {
        Button btnMoveFile = (Button) contentView.findViewById(R.id.btn_move_file);

        btnMoveFile.setText(getResources().getString(R.string.move_file_d_, mSelectedFiles.size()));
        btnMoveFile.setOnClickListener(v -> {
            moveFile(contentView);
            dismiss();
        });
    }

    private void initDialPad(View contentView) {
        ViewGroup keypadContainer = (ViewGroup) contentView.findViewById(R.id.t9_keypad_container);
        T9KeypadView t9KeypadView = (T9KeypadView) contentView.findViewById(R.id.t9_keypad);
        RecyclerView folderList = (RecyclerView) contentView.findViewById(R.id.folder_list);
        ViewGroup folderListContainer = (ViewGroup) contentView.findViewById(R.id.folder_list_container);


        t9KeypadView
                .getDialpadInputObservable()
                .debounce(369, TimeUnit.MILLISECONDS)
                .subscribe(input -> {
                    Log.d(TAG, "initDialPad: dial");
                    String inputString = input.toString();
                    SystemImageService.getInstance()
                            .loadFolderList(true, inputString)
                            .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                            .map(this::setAdapterMoveFileSourceDir)
                            .compose(RxUtils.workAndShow())
                            .subscribe(newAdapter -> {

                                if (folderList != null) {
                                    SectionedFolderListAdapter currAdapter = (SectionedFolderListAdapter) folderList.getAdapter();

                                    currAdapter.diffUpdate(newAdapter);
                                }
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
        keyboardSwitchIv
                .setBackgroundResource(R.drawable.keyboard_show_selector);
    }

    private void hideKeyboard() {
        ViewUtil.hideView(mT9KeypadView);
        mKeyboardSwitchIv
                .setBackgroundResource(R.drawable.keyboard_show_selector);
    }

    private void showKeyboard() {
        ViewUtil.showView(mT9KeypadView);
        mKeyboardSwitchIv.setBackgroundResource(R.drawable.keyboard_hide_selector);
    }

    private void showKeyboard(ViewGroup keypadContainer, ImageView keypadSwitchButton) {

        ViewUtil.showView(keypadContainer);
        keypadContainer.requestFocus();
        keypadSwitchButton.setBackgroundResource(R.drawable.keyboard_hide_selector);
    }

    private void initFolderList(final View contentView) {
        RecyclerView folderList = (RecyclerView) contentView.findViewById(R.id.folder_list);

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
                            onClickFolderListItem(item, contentView);
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

        SystemImageService.getInstance()
                .detectMoveFileConflict(item.getFile(), mSelectedFiles)
                .compose(RxUtils.workAndShow())
                .subscribe(moveFileDetectResult -> {
                    int colorOk = MoveFileDialogFragment.this.getResources().getColor(android.R.color.holo_green_dark);
                    int colorConflict = MoveFileDialogFragment.this.getResources().getColor(android.R.color.holo_red_dark);

                    if (moveFileDetectResult != null) {
                        List<Pair<File, File>> conflictFiles = moveFileDetectResult.getConflictFiles();
                        List<Pair<File, File>> canMoveFiles = moveFileDetectResult.getCanMoveFiles();
                        Button btnMoveFile = (Button) contentView.findViewById(R.id.btn_move_file);

                        if (conflictFiles.isEmpty()) {
                            setDetectingResultText(contentView, getString(R.string.can_move_all_files, canMoveFiles.size()), colorOk);

                            btnMoveFile.setTextColor(getResources().getColor(R.color.colorAccent));
                        } else {

                            btnMoveFile.setText(getResources().getString(R.string.move_file_d_d,
                                    mSelectedFiles.size() - conflictFiles.size(), mSelectedFiles.size()));
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
                            MoveFileDialogFragment.this.getResources().getColor(android.R.color.holo_red_light));
                });
    }

    private void setDetectingResultText(View contentView, String resultText, int textColor) {
        if (!mEnableDetectSelectedFolder) {
            return;
        }

        // hide detect progress
        ViewUtils.setViewVisibility(contentView, R.id.detect_info_layout, View.INVISIBLE);

        // detect result
        TextView detectResult = (TextView) contentView.findViewById(R.id.detect_result_info);
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
        View contentView = createContentView(null);

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
                        SystemImageService.getInstance()
                                .moveFilesToDirectory(destDir, Stream.of(mSelectedFiles).map(File::new).toList())
                                .compose(RxUtils.workAndShow())
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

    private void moveFile(View contentView) {
        SectionedFolderListAdapter.Item item = getViewItemTag(contentView, R.id.item);
        if (item != null) {

            File destDir = item.getFile();
            SystemImageService.getInstance()
                    .moveFilesToDirectory(destDir, mSelectedFiles, true, false)
                    .compose(RxUtils.workAndShow())
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
                            if (successCount == mSelectedFiles.size()) {
                                ToastUtils.toastLong(getActivity(),
                                        getActivity().getString(R.string.already_moved_d_files, successCount));
                            } else {
                                Log.w(TAG, "移动文件冲突: " + conflictFiles);
                                if (!conflictFiles.isEmpty()) {

                                } else {
                                    ToastUtils.toastShort(getActivity(), R.string.move_files_failed);
                                }

                                if (successCount > 0 && successCount < mSelectedFiles.size()) {
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
        RecyclerView rv = (RecyclerView) contentView.findViewById(R.id.folder_list);
        RecyclerView.LayoutManager lm = rv.getLayoutManager();
        if (lm instanceof LinearLayoutManager) {
            int firstVisibleItemPosition = ((LinearLayoutManager) lm).findFirstVisibleItemPosition();
            UserDataService.getInstance().setMoveFileDialogFirstVisibleItemPosition(firstVisibleItemPosition);
        } else if (lm instanceof GridLayoutManager) {
            int firstVisibleItemPosition = ((GridLayoutManager) lm).findFirstVisibleItemPosition();
            UserDataService.getInstance().setMoveFileDialogFirstVisibleItemPosition(firstVisibleItemPosition);
        }
    }

    private void showConflictDialog(File destDir, List<Pair<File, File>> conflictFiles) {

        if (conflictFiles.isEmpty()) {
            return;
        }

        List<String> strings = Stream.of(conflictFiles).map(fileFilePair -> fileFilePair.second.getAbsolutePath()).toList();
        ConflictResolverDialogFragment fragment = ConflictResolverDialogFragment.newInstance(destDir.getAbsolutePath(), new ArrayList<>(strings));
        fragment.show(getActivity().getSupportFragmentManager(), "Conflict Resolver Dialog");
    }

    private View createConflictDialogContentView() {

        return null;
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

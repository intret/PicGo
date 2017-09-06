package cn.intret.app.picgo.ui.main;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.annimon.stream.Stream;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
import cn.intret.app.picgo.utils.RxUtils;
import cn.intret.app.picgo.utils.ToastUtils;
import cn.intret.app.picgo.utils.ViewUtils;
import cn.intret.app.picgo.widget.RecyclerItemTouchListener;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MoveFileDialogFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MoveFileDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MoveFileDialogFragment extends AppCompatDialogFragment {

    public static final String TAG = MoveFileDialogFragment.class.getSimpleName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_SELECTED_FILES = "selected_files";

    // TODO: Rename and change types of parameters
    private ArrayList<String> mSelectedFiles;

    private OnFragmentInteractionListener mListener;


    @BindView(R.id.folder_list) RecyclerView mFolderList;
    @BindView(R.id.desc) TextView mDesc;

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
            mSelectedFiles = getArguments().getStringArrayList(ARG_SELECTED_FILES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView: ");

        // Inflate the layout for this fragment
        final View[] views = {null};
        SystemImageService.getInstance()
                .loadFolderListModel(true)
                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .subscribe(adapter -> {
                    View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_move_file_dialog, container, false);

                    ButterKnife.bind(MoveFileDialogFragment.this, contentView);
                    initLogic(contentView, adapter);
                    views[0] = contentView;
                });

        return views[0];
    }

    private View createDialogContentView() {

        final View[] views = {null};
        SystemImageService.getInstance()
                .loadFolderListModel(true)
                .map(FolderListAdapterUtils::folderModelToSectionedFolderListAdapter)
                .subscribe(adapter -> {

                    View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_move_file_dialog, null, false);
                    ButterKnife.bind(MoveFileDialogFragment.this, contentView);

                    initLogic(contentView, adapter);
                    views[0] = contentView;

//                    showMoveFileDialog(selectedFiles, adapter);
                });

        return views[0];
    }

    private void initLogic(View contentView, SectionedFolderListAdapter listAdapter) {
        mFolderList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mFolderList.setAdapter(listAdapter);

        // Restore position
        int fp = UserDataService.getInstance().getMoveFileDialogFirstVisibleItemPosition();
        if (fp != RecyclerView.NO_POSITION) {
            mFolderList.scrollToPosition(fp);
        }

        RecyclerItemTouchListener.OnItemClickListener clickListener = (view, position) -> {
            new SectionedListItemClickDispatcher(listAdapter)
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
        mFolderList.addOnItemTouchListener(new RecyclerItemTouchListener(
                getActivity(), mFolderList, clickListener, longClickListener));
    }

    private void onClickFolderListItem(SectionedFolderListAdapter.Item item, View contentView) {
        String msg = getString(R.string.move_selected_images_to_directory_s, item.getFile().getName());

        // 移动文件描述信息
        ViewUtils.setText(contentView, R.id.desc, msg);

        setDetecting(contentView);

        SystemImageService.getInstance()
                .detectMoveFileConflict(item.getFile(), Stream.of(mSelectedFiles).map(File::new).toList())
                .compose(RxUtils.workAndShow())
                .subscribe(moveFileDetectResult -> {
                    if (moveFileDetectResult != null) {
                        List<Pair<File, File>> conflictFiles = moveFileDetectResult.getConflictFiles();
                        List<Pair<File, File>> canMoveFiles = moveFileDetectResult.getCanMoveFiles();
                        if (conflictFiles.isEmpty()) {
                            setDetectingResultText(contentView, getString(R.string.can_move_all_files, canMoveFiles.size()));
                        } else {
                            setDetectingResultText(contentView,
                                    getString(R.string.target_directory_exists__d_files_in_the_same_name,
                                            conflictFiles.size()));
                        }
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    setDetectingResultText(contentView, getString(R.string.detect_move_file_action_result_failed));
                });
    }

    private void setDetectingResultText(View contentView, String resultText) {

        // hide detect progress
        ViewUtils.setViewVisibility(contentView, R.id.detect_info_layout, View.GONE);

        // detect result
        TextView detectResult = (TextView) contentView.findViewById(R.id.detect_result_info);
        detectResult.setVisibility(View.VISIBLE);
        detectResult.setText(resultText);
    }

    private void setDetecting(View contentView) {

        // Show detecting info
        ViewUtils.setViewVisibility(contentView, R.id.detect_info_layout, View.VISIBLE);

        ProgressBar progressBar = (ProgressBar) contentView.findViewById(R.id.detect_progress);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);

        ((TextView) contentView.findViewById(R.id.detect_progress_desc))
                .setText(getString(R.string.detecting_move_file_operationg_result));

        // Hide detect result info
        ViewUtils.setViewVisibility(contentView, R.id.detect_result_info, View.GONE);
    }

    private <R> R getViewItemTag(View view, int id) {
        Object tag = view.getTag(id);
        if (tag != null) {
            return (R) tag;
        }
        return null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Log.d(TAG, "onCreateDialog() called with: savedInstanceState = [" + savedInstanceState + "]");

        View contentView = createDialogContentView();

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.move_selected_files_to)
                .setView(contentView)
                .setPositiveButton(R.string.move_file, (dialog, which) -> {
                    SectionedFolderListAdapter.Item item = getViewItemTag(contentView, R.id.item);
                    if (item != null) {

                        File destDir = item.getFile();
                        SystemImageService.getInstance()
                                .moveFilesToDirectory(destDir, Stream.of(mSelectedFiles).map(File::new).toList(), true, false)
                                .compose(RxUtils.workAndShow())
                                .subscribe(moveFileResult -> {
                                    storePosition(contentView);

                                    if (moveFileResult != null) {
                                        List<Pair<File, File>> successFiles = moveFileResult.getSuccessFiles();
                                        List<Pair<File, File>> conflictFiles = moveFileResult.getConflictFiles();
                                        List<Pair<File, File>> failedFiles = moveFileResult.getFailedFiles();


                                        int successCount = successFiles.size();
                                        if (successCount == mSelectedFiles.size()) {
                                            ToastUtils.toastLong(getActivity(),
                                                    getActivity().getString(R.string.already_moved_d_files, successCount));
                                        } else {
                                            Log.w(TAG, "移动文件冲突: " + conflictFiles );
                                            if (!conflictFiles.isEmpty()) {
                                                try {
                                                    EventBus.getDefault().post(
                                                            new MoveFileResultMessage()
                                                                    .setDestDir(destDir)
                                                                    .setResult(moveFileResult));

                                                } catch (Throwable e) {
                                                    e.printStackTrace();
                                                }
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

        /*
        return new MaterialDialog.Builder(this.getContext())
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

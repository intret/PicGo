package cn.intret.app.picgo.model;


import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.annimon.stream.Stream;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.ui.adapter.ConflictImageListAdapter;
import cn.intret.app.picgo.utils.ToastUtils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConflictResolverDialogFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConflictResolverDialogFragment extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_DEST_DIR = "dest_dir";
    private static final String ARG_CONFLICT_FILES = "conflict_files";
    private static final String TAG = ConflictResolverDialogFragment.class.getSimpleName();

    private ArrayList<String> mConflictFiles;
    private String mDestDir;

    @BindView(R.id.conflict_image_list) RecyclerView mImageList;
    @BindView(R.id.radio_group) RadioGroup mRadioGroup;

    public ConflictResolverDialogFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param destDir
     * @param conflictFiles Parameter 2.
     * @return A new instance of fragment ConflictResolverDialogFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ConflictResolverDialogFragment newInstance(String destDir, ArrayList<String> conflictFiles) {
        ConflictResolverDialogFragment fragment = new ConflictResolverDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEST_DIR, destDir);
        args.putStringArrayList(ARG_CONFLICT_FILES, conflictFiles);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDestDir = getArguments().getString(ARG_DEST_DIR);
            mConflictFiles = getArguments().getStringArrayList(ARG_CONFLICT_FILES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: ");
        return createContentView(container);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Log.d(TAG, "onCreateDialog: ");

        View contentView = createContentView(null);

        return new AlertDialog.Builder(getActivity())
                .setView(contentView)
                .setTitle(R.string.file_name_conflict_resolver)
                .setPositiveButton(R.string.move_file, (dialog, which) -> {
                    RecyclerView.Adapter a = mImageList.getAdapter();
                    if (a != null) {
                        ConflictImageListAdapter adapter = (ConflictImageListAdapter) a;
                        List<ConflictImageListAdapter.Item> keepSourceItems = adapter.getKeepSourceItems();

                        ToastUtils.toastShort(getActivity(), R.string.unimplemented);
//                        SystemImageService.getInstance()
//                                .moveFilesToDirectory(new File(mDestDir), Stream.of(keepSourceItems)
//                                        .map(ConflictImageListAdapter.Item::getSourceFile)
//                                        .toList(), false, true)
//                                .compose(RxUtils.workAndShow())
//                                .subscribe(moveFileResult -> {
//                                    List<Pair<File, File>> successFiles = moveFileResult.getSuccessFiles();
//                                    if (!ListUtils.isEmpty(successFiles)) {
//
//                                    }
//                                });
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {

                })
                .create();
    }

    private View createContentView(ViewGroup container) {
        View contentView = LayoutInflater.from(getActivity())
                .inflate(R.layout.fragment_conflict_resolver_dialog, container, false);

        ButterKnife.bind(ConflictResolverDialogFragment.this, contentView);

        initContentView(contentView);
        return contentView;
    }

    private void initContentView(View contentView) {

        RecyclerView rv = (RecyclerView) contentView.findViewById(R.id.conflict_image_list);
        List<ConflictImageListAdapter.Item> items = Stream.of(mConflictFiles)
                .map(File::new)
                .map(file -> new ConflictImageListAdapter.Item()
                        .setSourceFile(file)
                        .setTargetFile(new File(mDestDir, file.getName()))
                )
                .toList();

        // Image List
        ConflictImageListAdapter adapter = new ConflictImageListAdapter(items);
        rv.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        rv.setAdapter(adapter);

        // Radio
        mRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.btn_keep_source: {
                    ConflictImageListAdapter imageListAdapter = (ConflictImageListAdapter) mImageList.getAdapter();
                    imageListAdapter.selectAllSourceItems();
                }
                    break;
                case R.id.btn_keep_target: {
                    ConflictImageListAdapter imageListAdapter = (ConflictImageListAdapter) mImageList.getAdapter();
                    imageListAdapter.selectAllTargetItems();
                }
                    break;
                case R.id.btn_keep_both:{
                    ConflictImageListAdapter imageListAdapter = (ConflictImageListAdapter) mImageList.getAdapter();
                    imageListAdapter.selectBothItems();
                }
                    break;
            }
        });
    }
}

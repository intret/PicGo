package cn.intret.app.picgo.ui.conflict;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Stream;
import com.orhanobut.logger.Logger;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.intret.app.picgo.R;
import cn.intret.app.picgo.model.event.ConflictResolveResultMessage;
import cn.intret.app.picgo.model.image.CompareItem;
import cn.intret.app.picgo.model.image.CompareItemResolveResult;
import cn.intret.app.picgo.model.image.ImageFileInformation;
import cn.intret.app.picgo.model.image.ImageModule;
import cn.intret.app.picgo.ui.adapter.ConflictImageListAdapter;
import cn.intret.app.picgo.utils.ListUtils;
import cn.intret.app.picgo.utils.Param;
import cn.intret.app.picgo.utils.RxUtils;
import cn.intret.app.picgo.utils.ToastUtils;


/**
 * 文件冲突解决 DialogFragment
 */
public class ConflictResolverDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_DEST_DIR = "dest_dir";
    private static final String ARG_CONFLICT_FILES = "conflict_files";
    private static final String TAG = ConflictResolverDialogFragment.class.getSimpleName();

    private List<File> mConflictFiles;
    private File mDestDir;

    @BindView(R.id.conflict_image_list) RecyclerView mImageList;

    @BindView(R.id.keep_source) ImageView mBtnKeepSource;
    @BindView(R.id.keep_source_check) TextView mTvKeepSource;
    @BindView(R.id.source_remove_file_count) TextView mSourceRemoveCount;

    @BindView(R.id.keep_target) ImageView mBtnKeepTarget;
    @BindView(R.id.keep_target_check) TextView mTvKeepTarget;
    @BindView(R.id.target_remove_file_count) TextView mTargetRemoveCount;
    private ConflictImageListAdapter mAdapter;
    private boolean mLoaded = false;

    public ConflictResolverDialogFragment() {
        // Required empty public constructor
    }

    /**
     * @param destDir             移动操作指定的目标目录
     * @param sourceConflictFiles 移动操作的源目录冲突文件列表，这些源文件可以不在同一个目录
     */
    public static ConflictResolverDialogFragment newInstance(@NonNull String destDir, @NonNull ArrayList<String> sourceConflictFiles) {
        ConflictResolverDialogFragment fragment = new ConflictResolverDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEST_DIR, destDir);
        args.putStringArrayList(ARG_CONFLICT_FILES, sourceConflictFiles);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String string = getArguments().getString(ARG_DEST_DIR);
            if (string == null) {
                throw new IllegalArgumentException("Argument ARG_DEST_DIR is empty.");
            }

            mDestDir = new File(string);

            ArrayList<String> stringArrayList = getArguments().getStringArrayList(ARG_CONFLICT_FILES);
            if (ListUtils.isEmpty(stringArrayList)) {
                throw new IllegalArgumentException("Argument ARG_CONFLICT_FILES is empty.");
            }

            mConflictFiles = Stream.of(stringArrayList).map(File::new).toList();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: ");
        View contentView = LayoutInflater.from(getActivity())
                .inflate(R.layout.fragment_conflict_resolver_dialog, container, false);

        ButterKnife.bind(ConflictResolverDialogFragment.this, contentView);

        cn.intret.app.picgo.utils.ViewUtils.setText(contentView, R.id.title, getString(R.string.file_name_conflict));
        cn.intret.app.picgo.utils.ViewUtils.setText(contentView, R.id.btn_positive, getString(R.string.next));


        return contentView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mLoaded) {
            loadConflictImageList();
            mLoaded = true;
        }
    }

    @OnClick(R.id.keep_source)
    public void onClickKeepSource(View view) {

        mBtnKeepSource.setSelected(!mBtnKeepSource.isSelected());
        mTvKeepSource.setSelected(mBtnKeepSource.isSelected());

        RecyclerView.Adapter adapter = mImageList.getAdapter();
        if (adapter != null && adapter instanceof ConflictImageListAdapter) {
            if (mTvKeepSource.isSelected()) {
                mBtnKeepTarget.setSelected(false);
                mTvKeepTarget.setSelected(false);
                ((ConflictImageListAdapter) adapter).selectAllSourceItems();
            } else {
                ((ConflictImageListAdapter) adapter).unselectAllSourceItem();
            }
        }
    }

    @OnClick(R.id.keep_target)
    public void onClickKeepTarget(View view) {
        mBtnKeepTarget.setSelected(!mBtnKeepTarget.isSelected());
        mTvKeepTarget.setSelected(mBtnKeepTarget.isSelected());

        RecyclerView.Adapter adapter = mImageList.getAdapter();
        if (adapter != null && adapter instanceof ConflictImageListAdapter) {
            if (mBtnKeepTarget.isSelected()) {
                mBtnKeepSource.setSelected(false);
                mTvKeepSource.setSelected(false);
                ((ConflictImageListAdapter) adapter).selectAllTargetItems();
            } else {
                ((ConflictImageListAdapter) adapter).unselectAllTargetItem();
            }
        }
    }

    @OnClick(R.id.btn_positive)
    public void onClickHeaderPositive(View view) {
        if (mAdapter == null) {
            ToastUtils.toastShort(getContext(), R.string.not_ready);
            return;
        }

        List<ConflictImageListAdapter.Item> items = mAdapter.getItems();
        List<CompareItem> compareItems = Stream.of(items)
                .map(item -> new CompareItem()
                        .setResult(item.getResolveResult())
                        .setSourceFile(item.getSourceFile())
                        .setTargetFile(item.getTargetFile())
                )
                .toList();

        Param.Five<Integer, Integer, Integer, Integer, Integer> param = mAdapter.getResolveResultCount();
        int keepTargetCount = param.p1;
        int keepSourceCount = param.p2;
        int deleteTargetCount = param.p3;
        int deleteSourceCount = param.p4;

        String sourceDirPath = ListUtils.firstOf(mConflictFiles).getParentFile().getAbsolutePath();

        List<CompareItemResolveResult> resolveResults = new LinkedList<>();
        new MaterialDialog.Builder(getContext())
                .title(R.string.action_confirm)
                .content(R.string.confirm_to_remove_d_files_from_s_and_remove_d_files_from_s,
                        mDestDir, deleteTargetCount,
                        sourceDirPath, deleteSourceCount
                )
                .positiveText(R.string.confirm)
                .onPositive((dialog, which) -> {

                    Logger.d("解决图片文文件名冲突");

                    ImageModule.getInstance()
                            .resolveFileNameConflict(compareItems)
                            .compose(RxUtils.workAndShow())
                            .doOnNext(this::onCompareItemResult)
                            // 合并为列表
                            .collectInto(resolveResults, List::add)
                            .subscribe(this::onCompleteResolve, RxUtils::unhandledThrowable)
                    ;
                    dialog.dismiss();
                })
                .negativeText(R.string.cancel)
                .show();


    }

    private void onCompleteResolve(List<CompareItemResolveResult> compareItems) {
        if (mAdapter != null) {
            // 所有冲突都解决了，可以关闭对话框
            if (mAdapter.getItemCount() == 0) {
                dismiss();
            }
        }

        EventBus.getDefault().post(
                new ConflictResolveResultMessage()
                        .setCompareItems(compareItems));
    }

    private void onCompareItemResult(CompareItemResolveResult compareItemResolveResult) {
        if (compareItemResolveResult.isResolved()) {

            CompareItem compareItem = compareItemResolveResult.getCompareItem();
            switch (compareItem.getResult()) {

                case KEEP_SOURCE:
                case KEEP_TARGET:
                    if (mAdapter != null) {
                        mAdapter.removeItemSource(compareItem.getSourceFile());
                    }
                    break;
                case KEEP_BOTH:
                    break;
                case NONE:
                    Log.e(TAG, "invalid result data.");
                    break;
            }

        } else {
            Log.w(TAG, "onCompareItemResult: no resolved :" + compareItemResolveResult);
        }
    }

    private void loadConflictImageList() {

        mTvKeepTarget.setText(mDestDir.getName());
        mTvKeepSource.setText(ListUtils.firstOf(mConflictFiles).getParentFile().getName());

        List<File> targetFiles = Stream.of(mConflictFiles)
                .map(file -> new File(mDestDir, file.getName()))
                .toList();


        List<File> allFiles = new LinkedList<>(mConflictFiles);
        allFiles.addAll(targetFiles);

        ImageModule.getInstance()

                // 加载图片文件信息
                .loadImageFilesInfoMap(allFiles)
                .compose(RxUtils.singleWorkAndShow())

                // 构造 Adapter
                .map(allFileInfoMap -> Stream.of(mConflictFiles)
                        .map(file -> {

                            File targetFile = new File(mDestDir, file.getName());

                            ImageFileInformation sourceInfo = allFileInfoMap.get(file);
                            ImageFileInformation targetInfo = allFileInfoMap.get(targetFile);

                            return new ConflictImageListAdapter.Item()
                                    .setSourceFile(file)
                                    .setSourceImageResolution(sourceInfo == null ? null : sourceInfo.getMediaResolution())
                                    .setSourceFileSize(sourceInfo == null ? -1 : sourceInfo.getFileLength())

                                    .setTargetFile(targetFile)
                                    .setTargetImageResolution(targetInfo == null ? null : targetInfo.getMediaResolution())
                                    .setTargetFileSize(targetInfo == null ? -1 : targetInfo.getFileLength());
                        }).toList()
                )
                .map(ConflictImageListAdapter::new)
                .map(this::configAdapter)
                // 显示图片列表
                .subscribe(this::showAdapter, throwable -> {
                    // TODO: show error empty view
                    ToastUtils.toastLong(getContext(), R.string.load_image_failed);
                })
        ;
    }

    private void showAdapter(ConflictImageListAdapter adapter) {
        mImageList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        mImageList.setAdapter(adapter);
        mAdapter = adapter;
    }

    @NonNull
    private ConflictImageListAdapter configAdapter(ConflictImageListAdapter adapter) {
        // 配置 Adapter
        adapter.setOnInteractionListener(new ConflictImageListAdapter.OnInteractionListener() {
            @Override
            public void onResolveCountChange(int totalCount, int keepTargetCount, int keepSourceCount, int deleteTargetCount, int deleteSourceCount) {

                if (deleteTargetCount == 0) {
                    mTargetRemoveCount.setVisibility(View.GONE);
                } else {
                    mTargetRemoveCount.setVisibility(View.VISIBLE);
                    mTargetRemoveCount.setText(getString(R.string.percent_d_d, (deleteTargetCount), totalCount));
                }

                if (deleteSourceCount == 0) {
                    mSourceRemoveCount.setVisibility(View.GONE);
                } else {
                    mSourceRemoveCount.setVisibility(View.VISIBLE);
                    mSourceRemoveCount.setText(getString(R.string.percent_d_d, deleteSourceCount, totalCount));
                }
            }
        });
        return adapter;
    }
}

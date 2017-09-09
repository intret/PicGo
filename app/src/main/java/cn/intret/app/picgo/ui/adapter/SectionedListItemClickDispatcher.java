package cn.intret.app.picgo.ui.adapter;

import android.database.sqlite.SQLiteCursorDriver;
import android.util.Log;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;


public class SectionedListItemClickDispatcher<E extends SectionedRecyclerViewAdapter> {
    public static final String TAG = SectionedListItemClickDispatcher.class.getSimpleName();

    private SectionedListItemDispatchListener<E> mListener;
    E mAdapter;

    public interface OnItemClickListener<E extends SectionedRecyclerViewAdapter> {
        void onItemClick(E adapter, ItemCoord coord);
    }

    public SectionedListItemClickDispatcher(E adapter) {
        mAdapter = adapter;
    }

    public void dispatchItemClick(int position, OnItemClickListener<E> listener) {
        ItemCoord relativePosition = mAdapter.getRelativePosition(position);
        if (mAdapter.isItemPosition(position) && listener != null) {
            listener.onItemClick(mAdapter, relativePosition);
        }
    }

    public void dispatch(int position, SectionedListItemDispatchListener listener) {
        if (mAdapter == null) {
            Log.d(TAG, "dispatch: adapter is null");
            return;
        }

        mListener = listener;
        ItemCoord relativePosition = mAdapter.getRelativePosition(position);
        boolean header = mAdapter.isHeader(position);
        boolean footer = mAdapter.isFooter(position);
        if (mListener != null) {

            if (header) {
                mListener.onHeader(mAdapter, relativePosition);
                return;
            }

            if (footer) {
                mListener.onFooter(mAdapter, relativePosition);
                return;
            }

            mListener.onItem(mAdapter, relativePosition);
        }
    }
}

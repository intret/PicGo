package cn.intret.app.picgo.ui.adapter;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;


public class SectionedListItemClickDispatcher {
    private SectionedListItemDispatchListener mListener;
    com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter mAdapter;

    public SectionedListItemClickDispatcher(SectionedRecyclerViewAdapter adapter) {
        mAdapter = adapter;

    }

    public void dispatch(int position, SectionedListItemDispatchListener listener) {
        if (mAdapter == null) {
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

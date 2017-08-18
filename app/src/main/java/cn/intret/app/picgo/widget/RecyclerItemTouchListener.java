package cn.intret.app.picgo.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerItemTouchListener implements RecyclerView.OnItemTouchListener {
    private final OnItemLongClickListener mLongClickListener;
    private OnItemClickListener mClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View view, int position);
    }

    GestureDetector mGestureDetector;

    public RecyclerItemTouchListener(Context context, final RecyclerView recyclerView,
                                     OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
        mClickListener = clickListener;
        mLongClickListener = longClickListener;
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            public static final String TAG = "SimpleOnGestureListener";

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                Log.d(TAG, "onSingleTapUp() called with: e = [" + e + "]");
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View child = recyclerView.findChildViewUnder(e.getX(),e.getY());
                if(child != null && mLongClickListener != null){
                    mLongClickListener.onItemLongClick(child, recyclerView.getChildAdapterPosition(child));
                }
                super.onLongPress(e);
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        View childView = view.findChildViewUnder(e.getX(), e.getY());
        if (childView != null && mClickListener != null && mGestureDetector.onTouchEvent(e)) {
            mClickListener.onItemClick(childView, view.getChildAdapterPosition(childView));
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) {
        Log.d("触摸", "onTouchEvent() called with: view = [" + view + "], motionEvent = [" + motionEvent + "]");
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
}
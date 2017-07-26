package cn.intret.app.picgo.ui.adapter;


import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * https://segmentfault.com/a/1190000006858824
 * Created by anonymous on 2016/9/9 0009.
 * <p>
 * SpacesItemDecoration
 * <p>
 * mRecyclerView.addItemDecoration(new SpacesItemDecoration(5));
 */
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {

    private int space;

    public SpacesItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.left = space;
        outRect.right = space;
        outRect.bottom = space;

        // Add top margin only for the first item to avoid double space between items
        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.top = space;
        } else {
            outRect.top = 0;
        }
    }
}
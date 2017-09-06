package cn.intret.app.picgo.ui.adapter;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;


public interface SectionedListItemDispatchListener {
    void onHeader(SectionedRecyclerViewAdapter adapter, ItemCoord coord);

    void onFooter(SectionedRecyclerViewAdapter adapter, ItemCoord coord);

    void onItem(SectionedRecyclerViewAdapter adapter, ItemCoord coord);
}

package cn.intret.app.picgo.ui.adapter;

import com.afollestad.sectionedrecyclerview.ItemCoord;
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;


public interface SectionedListItemDispatchListener<E extends SectionedRecyclerViewAdapter> {
    void onHeader(E adapter, ItemCoord coord);

    void onFooter(E adapter, ItemCoord coord);

    void onItem(E adapter, ItemCoord coord);
}

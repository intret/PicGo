package cn.intret.app.picgo.ui.adapter;

public interface ItemSelectable {
    int onGetItemCount();

    boolean isItemSelected(int position);

    boolean isMultipleSelect();

    boolean onBindSelectedView(boolean selected);

    boolean setSelected(boolean selected);
}

package cn.intret.app.picgo.utils;


import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.ListPopupWindow;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.concurrent.Callable;

import cn.intret.app.picgo.R;

public class PopupUtils {

    public static class PopupMenuItem {
        String name;
        Drawable icon;
        Callable<Boolean> action;

        public Callable<Boolean> getAction() {
            return action;
        }

        public PopupMenuItem setAction(Callable<Boolean> action) {
            this.action = action;
            return this;
        }

        public Drawable getIcon() {
            return icon;
        }

        public PopupMenuItem setIcon(Drawable icon) {
            this.icon = icon;
            return this;
        }

        public String getName() {
            return name;
        }

        public PopupMenuItem setName(String name) {
            this.name = name;
            return this;
        }
    }

    public static ListPopupWindow build(Context context,
                                        List<PopupMenuItem> items,
                                        View anchor,
                                        Drawable backgroundDrawable,
                                        int contentWidth,
                                        int horizontalOffset,
                                        boolean showIcon
    ) {
        if (items == null) {
            throw new InvalidParameterException("'items' parameter is null.");
        }

        ListPopupWindow pw = new ListPopupWindow(context);
        ListAdapter adapter = new ListAdapter() {
            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public boolean isEnabled(int position) {
                return true;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {

            }

            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public Object getItem(int position) {
                return items.get(position).getName();
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                @SuppressLint("ViewHolder")
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popup_menu, parent, false);

                ImageView icon = (ImageView) v.findViewById(R.id.icon);
                TextView name = (TextView) v.findViewById(R.id.name);

                PopupMenuItem item = items.get(position);

                if (showIcon) {
                    icon.setImageDrawable(item.getIcon());
                    icon.setVisibility(View.VISIBLE);
                } else {
                    icon.setVisibility(View.GONE);
                }
                name.setText(item.getName());
                return v;
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }
        };
        pw.setAdapter(adapter);
        pw.setDropDownGravity(Gravity.END);
        pw.setModal(true);
        pw.setAnchorView(anchor);
        pw.setBackgroundDrawable(backgroundDrawable);
        pw.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        pw.setContentWidth(contentWidth);
        pw.setHorizontalOffset(horizontalOffset);

        pw.setOnItemClickListener((parent, view, position, id) -> {
            pw.dismiss();
            PopupMenuItem item = items.get(position);
            if (item.getAction() != null) {
                try {
                    item.getAction().call();
                } catch (Exception ignored) {
                }
            }
        });
        return pw;
    }
}

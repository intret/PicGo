package cn.intret.app.picgo.ui.adapter.brvah;

import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

import cn.intret.app.picgo.R;

/**
 * Created by luoxw on 2016/6/20.
 */
public class ItemDragAdapter extends BaseItemDraggableAdapter<String, BaseViewHolder> {
    public ItemDragAdapter(List data) {
        super(R.layout.item_folder, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
//        switch (helper.getLayoutPosition() %
//                3) {
//            case 0:
//                helper.setImageResource(R.id.iv_head, R.mipmap.head_img0);
//                break;
//            case 1:
//                helper.setImageResource(R.id.iv_head, R.mipmap.head_img1);
//                break;
//            case 2:
//                helper.setImageResource(R.id.iv_head, R.mipmap.head_img2);
//                break;
//        }
//        helper.setText(R.id.tv, item);
    }
}


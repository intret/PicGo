package cn.intret.app.picgo.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by intret on 2017/11/4.
 */

public class ShareUtils {
    public static void  shareImages(Context context, ArrayList<Uri> uriList){
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
        shareIntent.setType("image/*");
        context.startActivity( Intent.createChooser(shareIntent, "分享图片至") );
    }

    public static void shareImage(Context context, File file) {
        if ( context == null || file == null) {
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.setType("image/*");
        context.startActivity(Intent.createChooser(intent, "分享图片到"));
    }
}

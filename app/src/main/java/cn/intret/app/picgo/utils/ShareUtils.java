package cn.intret.app.picgo.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import org.apache.commons.io.FilenameUtils;

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
        shareMedia(context, file, "image", "分享图片到");
    }

    public static void shareVideo(Context context, File file) {
        shareMedia(context, file, "video", "分享视频到");
    }

    private static void shareMedia(Context context, File file, String mediaType, String title) {
        if ( context == null || file == null) {
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hey this is the video subject");
        intent.putExtra(Intent.EXTRA_TEXT, "Hey this is the video text");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.setType( mediaType + "/*");
        context.startActivity(Intent.createChooser(intent, title));
    }

    public static void shareVideo(Context context, String path) {

        ContentValues content = new ContentValues(4);
        content.put(MediaStore.Video.VideoColumns.DATE_ADDED,
                System.currentTimeMillis() / 1000);
        content.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        content.put(MediaStore.Video.Media.DATA, path);

        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, content);

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sharingIntent.setType("video/*");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Hey this is the video subject");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, "Hey this is the video text");
        sharingIntent.putExtra(Intent.EXTRA_STREAM,uri);
        context.startActivity(Intent.createChooser(sharingIntent,"Share Video"));
    }

    public static void playVideo(Context context, String absolutePath) {
        Uri videoUri = Uri.parse(absolutePath);
        Intent intent = new Intent(Intent.ACTION_VIEW, videoUri);
        intent.setDataAndType(videoUri, "video/" + FilenameUtils.getExtension(absolutePath));
        context.startActivity(intent);
    }
}

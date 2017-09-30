package cn.intret.app.picgo.utils;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Size;

import java.io.File;

import cn.intret.app.picgo.R;


public class MediaUtils {

    /**
     *
     * @param context
     * @param videoFile
     * @return The duration in milliseconds, if no duration is available (for example, if streaming live content), -1 is returned.
     * @implNote https://stackoverflow.com/questions/33770188/how-can-i-get-the-duration-resolution-of-a-video-file-programmatically-in-andro
     */
    public static int getVideoFileDuration(Context context, File videoFile) {
        if (videoFile == null) {
            return -1;
        }
        MediaPlayer mp = MediaPlayer.create(context, Uri.fromFile(videoFile));
        return mp.getDuration();
    }

    public static Size getVideoResolution(Context context, File videoFile) {
        if (videoFile == null) {
            return new Size(0, 0);
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, Uri.fromFile(videoFile));
        int width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        retriever.release();
        return new Size(width, height);
    }

    public static Size getImageResolution(File imageFile) {

        BitmapFactory.Options bitMapOption = new BitmapFactory.Options();
        bitMapOption.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), bitMapOption);
        int imageWidth = bitMapOption.outWidth;
        int imageHeight = bitMapOption.outHeight;
        return new Size(imageWidth, imageHeight);
    }

    public static boolean isValidSize(Size imageSize) {
        if (imageSize == null) {
            return false;
        }
        return imageSize.getWidth() > 0 && imageSize.getHeight() > 0;
    }

    @NonNull
    public static String getResolutionString(@NonNull Context context, @NonNull Size mediaResolution) {
        String resText;
        if (isValidSize(mediaResolution)) {
            resText = context.getString(R.string.image_size_d_d_compact, mediaResolution.getWidth(), mediaResolution.getHeight());
        } else {
            resText = "-";
        }
        return resText;
    }
}

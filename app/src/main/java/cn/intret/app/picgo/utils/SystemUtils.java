package cn.intret.app.picgo.utils;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;

/**
 * System Utilities
 */

public class SystemUtils {
    private static final String TAG = "SystemUtils";

    /**
     * Get DCIM (digital camera in memory) directory
     * @return
     */
    public static File getDCIMDir() {
        // digital camera in memory
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    public static File getCameraDir() {
        File dcimDir = getDCIMDir();
        if (dcimDir == null) {
            return null;
        }

        File[] files = dcimDir.listFiles((dir, name) -> dir.isDirectory() && name.toLowerCase().equals("camera"));
        if (files != null && files.length > 0) {
            return files[0];
        } else {
            // TODO detect camera directory
            return null;
        }
    }
}

package cn.intret.app.picgo.ui.base

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import dagger.android.support.DaggerAppCompatActivity

@SuppressLint("Registered")
open class BaseDaggerAppCompatActivity : DaggerAppCompatActivity() {

    val actionBarHeight: Int
        get() {
            var actionBarHeight = 0
            val tv = TypedValue()
            if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
                actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
            }
            return actionBarHeight
        }

    val statusBarHeight: Int
        get() {
            var result = 0
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = resources.getDimensionPixelSize(resourceId)
            }
            return result
        }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun setStatusBarColor(statusBar: View, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val w = window
            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            //status bar height
            //            int actionBarHeight = getActionBarHeight();
            //            int statusBarHeight = getStatusBarHeight();
            //            //action bar height
            //            statusBar.getLayoutParams().height = actionBarHeight + statusBarHeight;
            statusBar.setBackgroundColor(color)
        }
    }
}

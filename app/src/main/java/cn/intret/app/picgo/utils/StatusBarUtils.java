package cn.intret.app.picgo.utils;

import android.annotation.TargetApi;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

public class StatusBarUtils {


    public static void hideStatusBar(AppCompatActivity appCompatActivity) {
        ActionBar supportActionBar = appCompatActivity.getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();
        }
    }

}

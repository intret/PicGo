package cn.intret.app.picgo.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;


public class SelectedImageFloatViewService extends Service implements FloatingViewListener {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onFinishFloatingView() {

    }

    @Override
    public void onTouchFinished(boolean isFinishing, int x, int y) {

    }
}

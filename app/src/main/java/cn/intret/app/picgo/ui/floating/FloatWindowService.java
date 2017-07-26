package cn.intret.app.picgo.ui.floating;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.Toast;

import cn.intret.app.picgo.R;

public class FloatWindowService extends Service {
    private static final String TAG = "FloatWindowService";
    public static final String UPDATE_ACTION = "cn.intret.widget.action.ACTIVE_NUMBER";
    public static final String EXTRA_MSG = "msg";
    public static final int MSG_DECREASE = 1;
    public static final int MSG_INCREASE = 2;

    //标识当前app有几个activity处于前台活跃状态
    private static int activeNumber = 1;

    Button mFloatView;
    //定义浮动窗口布局
    LinearLayout mFloatLayout;
    WindowManager.LayoutParams wmParams;
    //创建浮动窗口设置布局参数的对象
    WindowManager mWindowManager;
    MyReceiver myReceiver;
    //表示悬浮窗的显示状态
    private boolean mHasShown;
    private Chronometer mTimeCounter;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        //注册BroadCastReceiver
        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_ACTION);
        registerReceiver(myReceiver, filter);
        //初始化悬浮窗UI
        createFloatView();
    }

    public void createFloatView() {
        wmParams = new WindowManager.LayoutParams();
        //获取的是WindowManagerImpl.CompatModeWrapper
        mWindowManager = (WindowManager) getApplication().getSystemService(getApplication().WINDOW_SERVICE);

        //以下代码块使得android6.0之后的用户不必再去手动开启悬浮窗权限
        {
            String packageName = FloatWindowService.this.getPackageName();
            PackageManager pm = FloatWindowService.this.getPackageManager();
            boolean permission = (PackageManager.PERMISSION_GRANTED ==
                    pm.checkPermission("android.permission.SYSTEM_ALERT_WINDOW", packageName));
            if (permission) {
                wmParams.type = WindowManager.LayoutParams.TYPE_PHONE;
            } else {
                wmParams.type = WindowManager.LayoutParams.TYPE_TOAST;
            }
        }

        //设置图片格式，效果为背景透明
        //wmParams.format = PixelFormat.RGBA_8888;
        //设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //调整悬浮窗显示的停靠位置为左侧置顶
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        // 以屏幕左上角为原点，设置x、y初始值，相对于gravity
        wmParams.x = 0;
        wmParams.y = 0;

        //设置悬浮窗口长宽数据
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

         /*// 设置悬浮窗口长宽数据
        wmParams.width = 200;
        wmParams.height = 80;*/

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        //获取浮动窗口视图所在布局
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.selected_images_float_layout, null);
        //添加mFloatLayout
        mWindowManager.addView(mFloatLayout, wmParams);
        mHasShown = true;
        //浮动窗口按钮
        mFloatView = (Button) mFloatLayout.findViewById(R.id.float_id);
        mTimeCounter = (Chronometer) mFloatLayout.findViewById(R.id.time_counter);
        mTimeCounter.setFormat("%s");
        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        //设置监听浮动窗口的触摸移动
        mFloatView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //getRawX是触摸位置相对于屏幕的坐标，getX是相对于按钮的坐标
                wmParams.x = (int) event.getRawX() - mFloatView.getMeasuredWidth() / 2;
                //减25为状态栏的高度
                wmParams.y = (int) event.getRawY() - mFloatView.getMeasuredHeight() / 2 - 50;
                //刷新
                mWindowManager.updateViewLayout(mFloatLayout, wmParams);
                return false;  //此处必须返回false，否则OnClickListener获取不到监听
            }
        });

        mFloatView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(FloatWindowService.this, "onClick", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mFloatLayout != null) {
            //移除悬浮窗口
            if (mHasShown)
                mWindowManager.removeView(mFloatLayout);
        }

        unregisterReceiver(myReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //悬浮窗的隐藏
    public void hide() {
        if (mHasShown)
            mWindowManager.removeViewImmediate(mFloatLayout);
        mHasShown = false;
    }

    //悬浮窗的显示
    public void show() {
        if (!mHasShown)
            mWindowManager.addView(mFloatLayout, wmParams);
        mHasShown = true;
    }

    //
    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int msg = intent.getIntExtra(FloatWindowService.EXTRA_MSG, -1);//获取广播传递来的参数
            if (msg == MSG_INCREASE) {
                activeNumber++;
            } else if (msg == MSG_DECREASE) {
                activeNumber--;
            } else {
                Toast.makeText(FloatWindowService.this, "广播传递参数遇到一个错误", Toast.LENGTH_SHORT).show();
            }

            Log.d(TAG, "onReceive: msg:" + msg + " activeNumber:" + activeNumber);

            if (activeNumber == 0) {
                hide();     //当前处于前台的activity数目为零，隐藏悬浮窗
            } else {
                show();
            }
        }
    }
}
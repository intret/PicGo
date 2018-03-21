package cn.intret.app.picgo.workaround;

import android.content.Context;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;

/**
 * Created by intret on 2018/3/21.
 */

public class DaggerBottomSheetDialogFragment extends BottomSheetDialogFragment implements HasSupportFragmentInjector {
    @Inject DispatchingAndroidInjector<Fragment> childFragmentInjector;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return childFragmentInjector;
    }
}

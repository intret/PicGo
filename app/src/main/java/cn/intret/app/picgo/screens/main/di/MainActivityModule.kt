package cn.intret.app.picgo.screens.main.di

import android.app.Activity
import cn.intret.app.picgo.screens.main.MainActivity
import cn.intret.app.picgo.screens.main.move.MoveFileDialogFragment
import cn.intret.app.picgo.screens.main.move.di.MoveFileSubcomponent
import dagger.Binds
import dagger.Module
import dagger.android.ActivityKey
import dagger.android.AndroidInjector
import dagger.android.support.FragmentKey
import dagger.multibindings.IntoMap


/**
 * Created by intret on 2018/3/20.
 */
@Module()
abstract class MainActivityModule {
    @Binds
    @IntoMap
    @ActivityKey(MainActivity::class)
    internal abstract fun bind(builder: MainSubcomponent.Builder)
            : AndroidInjector.Factory<out Activity>
}
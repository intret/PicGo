package cn.intret.app.picgo.screens.main.move.di

import android.support.v4.app.Fragment
import cn.intret.app.picgo.screens.main.move.MoveFileDialogFragment
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjector
import dagger.android.support.FragmentKey
import dagger.multibindings.IntoMap

/**
 * Created by intret on 2018/3/20.
 */
@Module
abstract class MoveFileModule {
    @Binds
    @IntoMap
    @FragmentKey(MoveFileDialogFragment::class)
    internal abstract fun bind(builder: MoveFileSubcomponent.Builder)
            : AndroidInjector.Factory<out Fragment>
}
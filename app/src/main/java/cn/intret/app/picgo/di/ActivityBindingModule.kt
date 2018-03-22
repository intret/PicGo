package cn.intret.app.picgo.di

import cn.intret.app.picgo.screens.main.MainActivity
import cn.intret.app.picgo.screens.main.MainModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by intret on 2018/3/21.
 */
@Module
abstract class ActivityBindingModule {
    @ActivityScoped
    @ContributesAndroidInjector(modules = arrayOf(MainModule::class))
    internal abstract fun mainActivity(): MainActivity
}

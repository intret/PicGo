package cn.intret.app.picgo.ui.main.di

import cn.intret.app.picgo.ui.main.MainActivity
import dagger.Subcomponent
import dagger.android.AndroidInjector

/**
 * Created by intret on 2018/3/20.
 */

@Subcomponent(modules = arrayOf(MainActivityModule::class))
interface MainSubcomponent: AndroidInjector<MainActivity> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<MainActivity>()
}
package cn.intret.app.picgo.ui.main

import cn.intret.app.picgo.di.ActivityScoped
import dagger.Binds
import dagger.Module


/**
 * This is a Dagger module. We use this to pass in the View dependency to the
 * [MainPresenter].
 */
@Module
abstract class MainModule {

    @ActivityScoped
    @Binds
    internal abstract fun mainPresenter(presenter: MainPresenter<MainActivity>): MainContract.Presenter
}

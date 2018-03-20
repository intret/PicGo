package cn.intret.app.picgo.screens.main.move.di

import cn.intret.app.picgo.screens.main.move.MoveFileDialogFragment
import dagger.Subcomponent
import dagger.android.AndroidInjector

/**
 * Created by intret on 2018/3/20.
 */
@Subcomponent(modules = arrayOf(MoveFileModule::class,MoveFilePresenterModule::class))
interface MoveFileSubcomponent: AndroidInjector<MoveFileDialogFragment> {

    @Subcomponent.Builder
    abstract class Builder : AndroidInjector.Builder<MoveFileDialogFragment>()
}
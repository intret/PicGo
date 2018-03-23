package cn.intret.app.picgo.ui.move

import cn.intret.app.picgo.di.FragmentScoped
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by intret on 2018/3/21.
 */
@Module
abstract class MoveFileModule {
    @FragmentScoped
    @ContributesAndroidInjector
    internal abstract fun moveFileFragment(): MoveFileDialogFragment

    @FragmentScoped
    @Binds
    internal abstract fun moveFilePresenter(presenter: MoveFilePresenter<MoveFileDialogFragment>): MoveFileContracts.Presenter
}
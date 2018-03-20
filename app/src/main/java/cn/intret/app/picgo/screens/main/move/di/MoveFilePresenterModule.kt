package cn.intret.app.picgo.screens.main.move.di

import cn.intret.app.picgo.screens.main.move.MoveFileContracts
import cn.intret.app.picgo.screens.main.move.MoveFileDialogFragment
import cn.intret.app.picgo.screens.main.move.MoveFilePresenter
import dagger.Module
import dagger.Provides

/**
 * Created by intret on 2018/3/20.
 */
@Module
class MoveFilePresenterModule {

    @Provides
    fun provideMoveFileView(fragment: MoveFileDialogFragment): MoveFileContracts.View {
        return fragment
    }

    @Provides
    fun provideMoveFilePresenter(presenter: MoveFilePresenter<MoveFileDialogFragment>): MoveFileContracts.Presenter {
        return presenter
    }
}
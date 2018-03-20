package cn.intret.app.picgo.ui.main.di

import cn.intret.app.picgo.model.user.UserModule
import cn.intret.app.picgo.ui.main.MainActivity
import cn.intret.app.picgo.ui.main.MainContract
import dagger.Module
import dagger.Provides

/**
 * Created by intret on 2018/3/20.
 */
@Module
class MainPresenterModule {

    @Provides
    fun provideMainView(activity: MainActivity): MainContract.View {
        return activity
    }


    @Provides
    fun provideUserModule(): UserModule {
        return UserModule
    }
}

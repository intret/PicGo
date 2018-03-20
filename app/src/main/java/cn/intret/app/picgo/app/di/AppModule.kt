package cn.intret.app.picgo.app.di

import android.content.Context
import cn.intret.app.picgo.app.MyApp
import cn.intret.app.picgo.ui.main.di.MainSubcomponent
import dagger.Module
import dagger.Provides

/**
 * Created by intret on 2018/3/20.
 */
@Module(subcomponents = arrayOf(MainSubcomponent::class))
class AppModule {
    @Provides
    fun provideAppContext(application: MyApp): Context {
        return application.applicationContext
    }
}
package cn.intret.app.picgo.app.di

import cn.intret.app.picgo.app.MyApp
import cn.intret.app.picgo.screens.main.di.MainActivityModule
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule

@Component(modules = arrayOf(
        /* Use AndroidInjectionModule.class if you're not using support library */
        AndroidSupportInjectionModule::class,
        AppModule::class,
        MainActivityModule::class
))
interface AppComponent {
    fun inject(application: MyApp)
}
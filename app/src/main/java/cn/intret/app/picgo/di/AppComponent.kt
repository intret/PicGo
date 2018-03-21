package cn.intret.app.picgo.di

import android.app.Application
import cn.intret.app.picgo.app.MyApp
import cn.intret.app.picgo.screens.move.MoveFileModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        /* Use AndroidInjectionModule.class if you're not using support library */
        AndroidSupportInjectionModule::class,
        AppModule::class,
        ModelsModule::class,
        MoveFileModule::class,
        ActivityBindingModule::class
))
interface AppComponent : AndroidInjector<MyApp> {

    // Gives us syntactic sugar. we can then do DaggerAppComponent.builder().application(this).build().inject(this);
    // never having to instantiate any modules or say which module we are passing the application to.
    // Application will just be provided into our app graph now.
    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): AppComponent.Builder

        fun build(): AppComponent
    }
}
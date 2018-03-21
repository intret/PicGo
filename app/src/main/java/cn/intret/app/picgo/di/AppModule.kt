package cn.intret.app.picgo.di

import android.content.Context
import cn.intret.app.picgo.app.MyApp
import cn.intret.app.picgo.model.image.ImageModule
import cn.intret.app.picgo.model.user.UserModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by intret on 2018/3/20.
 */
@Module
abstract class AppModule {
    //expose Application as an injectable context

    @Module
    companion object {
        @Singleton
        @Provides
        fun provideImageModel(): ImageModule {
            return ImageModule
        }

        @Singleton
        @Provides
        fun provideUserModel(): UserModule {
            return UserModule
        }
    }

    @Binds
    abstract fun bindContext(application: MyApp): Context
}
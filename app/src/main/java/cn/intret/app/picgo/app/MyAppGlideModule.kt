package cn.intret.app.picgo.app

import android.content.Context

import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule

/**
 * Customize the Glide
 */
@GlideModule
class MyAppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        //        builder.setMemoryCache(new LruResourceCache(20 * 1024 * 1024));
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}

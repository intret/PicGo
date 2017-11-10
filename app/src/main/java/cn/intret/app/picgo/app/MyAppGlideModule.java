package cn.intret.app.picgo.app;

import android.content.Context;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Customize the Glide
 */
@GlideModule
public final class MyAppGlideModule extends AppGlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
//        builder.setMemoryCache(new LruResourceCache(20 * 1024 * 1024));

    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}

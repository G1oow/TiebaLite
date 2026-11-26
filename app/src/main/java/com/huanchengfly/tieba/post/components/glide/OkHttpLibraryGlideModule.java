package com.huanchengfly.tieba.post.components.glide;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;

import java.io.InputStream;

import com.huanchengfly.tieba.post.utils.NetworkConfig;
import okhttp3.OkHttpClient;

@GlideModule
public class OkHttpLibraryGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        //添加拦截器到Glide
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dispatcher(NetworkConfig.INSTANCE.getImageDispatcher())
                .connectionPool(NetworkConfig.INSTANCE.getImageConnectionPool())
                .retryOnConnectionFailure(true)
                .addInterceptor(NetworkConfig.INSTANCE.getDynamicTimeoutInterceptor());
        builder.addInterceptor(new ProgressInterceptor());
        OkHttpClient okHttpClient = builder.build();
        //原来的是  new OkHttpUrlLoader.Factory()；
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(okHttpClient));
    }

    //完全禁用清单解析
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}

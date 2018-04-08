package co.muslimummah.android.module.quran.helper;

/**
 * Created by frank on 7/27/17.
 */

import android.content.Context;

import com.danikula.videocache.HttpProxyCacheServer;

/**
 * <strong>Not thread-safe</strong> {@link HttpProxyCacheServer} factory that returns single instance of proxy.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class MediaCacheProxyFactory {

    private static HttpProxyCacheServer sharedProxy;

    private MediaCacheProxyFactory() {
    }

    public static HttpProxyCacheServer getProxy(Context context) {
        return sharedProxy == null ? (sharedProxy = newProxy(context)) : sharedProxy;
    }

    private static HttpProxyCacheServer newProxy(Context context) {
        return new HttpProxyCacheServer.Builder(context)
                .maxCacheSize(1024 * 1024 * 1024)
                .build();
    }
}
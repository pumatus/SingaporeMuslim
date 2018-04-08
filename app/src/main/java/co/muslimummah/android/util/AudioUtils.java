package co.muslimummah.android.util;

import com.danikula.videocache.HttpProxyCacheServer;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;

/**
 * Created by tysheng
 * Date: 26/9/17 2:20 PM.
 * Email: tyshengsx@gmail.com
 */

public class AudioUtils {
    private static HttpProxyCacheServer proxy;

    public static HttpProxyCacheServer getProxy() {
        if (proxy == null) {
            proxy = newProxy();
        }
        return proxy;
    }

    private static HttpProxyCacheServer newProxy() {
        HttpProxyCacheServer cacheServer = new HttpProxyCacheServer(OracleApp.getInstance());

        return cacheServer;
    }

    public static String shortToFullUrl(String shortUrl) {
        shortUrl = QuranRepository.WORD_MP3_PREFIX + shortUrl;
        return shortUrl;
    }
}

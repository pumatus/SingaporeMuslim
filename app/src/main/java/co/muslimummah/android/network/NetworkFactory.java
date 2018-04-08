package co.muslimummah.android.network;

import android.app.Application;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import co.muslimummah.android.BuildConfig;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.util.Utils;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by frank on 8/28/17.
 */

class NetworkFactory {
    private static final int DISK_CACHE_SIZE = 50 * 1024 * 1024;

    private static final String HTTP_USER_AGENT = "User-Agent";

    private static OkHttpClient okHttpClient;

    private static OkHttpClient createOkHttpClient(Application app) {
        OkHttpClient client;
        // Install an HTTP cache in the application cache directory.
        File cacheDir = new File(app.getCacheDir(), "http");
        Cache cache = new Cache(cacheDir, DISK_CACHE_SIZE);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BODY
                : HttpLoggingInterceptor.Level.NONE);

        //insert essential headers into APIs
        Interceptor headers = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request oldRequest = chain.request();
                Request newRequest;

                Request.Builder newBuilder = oldRequest.newBuilder();

                newBuilder.addHeader(HTTP_USER_AGENT, Utils.getUserAgent(OracleApp.getInstance()));

                newRequest = newBuilder.build();
                return chain.proceed(newRequest);
            }
        };

        client = new OkHttpClient.Builder()
                .cache(cache)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(headers)
                .addInterceptor(logging)
                .build();

        return client;
    }

    static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = createOkHttpClient(OracleApp.getInstance());
        }
        return okHttpClient;
    }
}

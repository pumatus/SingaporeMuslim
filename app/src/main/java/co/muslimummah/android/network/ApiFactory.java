package co.muslimummah.android.network;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

import co.muslimummah.android.BuildConfig;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiFactory {
    private static Retrofit retrofit;
    private static HashMap<Class, Object> cachedApi = new HashMap<>();
    private static final String API_SERVICE = "api-server/";

    private static void init() {
        Gson gson = new GsonBuilder()
                .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setExclusionStrategies()
                .create();

        retrofit = new Retrofit.Builder()
                .client(NetworkFactory.getOkHttpClient())
//                .addCallAdapterFactory(ObserveOnMainCallAdapterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
                .addConverterFactory(UserLogConverter.create())
                .addConverterFactory(GsonConverterFactory.create(gson)) //must add at the end of other Converter
                .baseUrl(BuildConfig.BASE_URL + API_SERVICE)
                .build();

    }

    public static <T> T get(final Class<T> service) {
        if (retrofit == null) {
            init();
        }
        if (cachedApi.containsKey(service)) {
            return (T) cachedApi.get(service);
        } else {
            T api = retrofit.create(service);
            cachedApi.put(service, api);
            return api;
        }
    }
}
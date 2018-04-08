package co.muslimummah.android.network;

/**
 * Created by Xingbo.Jie on 29/8/17.
 */

import android.support.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Copy from https://github.com/square/retrofit/blob/master/samples/src/main/java/com/example/retrofit/RxJavaObserveOnMainThread.java
 */
public class ObserveOnMainCallAdapterFactory extends CallAdapter.Factory {

    public static ObserveOnMainCallAdapterFactory create() {
        return new ObserveOnMainCallAdapterFactory();
    }

    private ObserveOnMainCallAdapterFactory() {
    }
    @Nullable
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != Observable.class) {
            return null; // Ignore non-Observable types.
        }

        // Look up the next call adapter which would otherwise be used if this one was not present.
        //noinspection unchecked returnType checked above to be Observable.
        final CallAdapter<Object, Observable<?>> delegate =
                (CallAdapter<Object, Observable<?>>) retrofit.nextCallAdapter(this, returnType,
                        annotations);

        return new CallAdapter<Object, Object>() {
            @Override
            public Object adapt(Call<Object> call) {
                // Delegate to get the normal Observable...
                Observable<?> o = delegate.adapt(call);
                // ...and change it to send notifications to the observer on the specified scheduler.
                return o.observeOn(AndroidSchedulers.mainThread());
            }

            @Override
            public Type responseType() {
                return delegate.responseType();
            }
        };
    }
}

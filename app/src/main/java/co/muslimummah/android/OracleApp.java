package co.muslimummah.android;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.crashlytics.android.Crashlytics;
import com.facebook.appevents.AppEventsLogger;

import net.danlew.android.joda.JodaTimeAndroid;

import co.muslimummah.android.analytics.OracleAnalytics;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.OracleLocaleHelper;
import io.fabric.sdk.android.Fabric;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import timber.log.Timber;

/**
 * Created by frank on 7/4/17.
 */

public class OracleApp extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Application mInstance;

    public static Application getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        AppEventsLogger.activateApp(this);

        ThirdPartyAnalytics.INSTANCE.registerApp(this);

        Fabric.with(this, new Crashlytics());
        registerActivityLifecycleCallbacks(ActivityLifecycleHandler.instance());
        setupLog();

        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(@NonNull Throwable throwable) throws Exception {
                Timber.e(throwable);
            }
        });

        JodaTimeAndroid.init(this);

        OracleFirebaseInstanceIDService.registerFCMTokenToServer();

        OracleAnalytics.INSTANCE.reportLog();
        DebugFileLog.INSTANCE = new DebugFileLog(this);
    }

    private void setupLog() {
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashlyticsTree());
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        OracleLocaleHelper.onAttach(base);
        MultiDex.install(this);
    }
}

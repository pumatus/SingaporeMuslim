package co.muslimummah.android;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.TextUtils;

import com.facebook.applinks.AppLinkData;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.TimeUnit;

import co.muslimummah.android.analytics.AnalyticsConstants;
import co.muslimummah.android.analytics.GA;
import co.muslimummah.android.analytics.LogObject;
import co.muslimummah.android.analytics.OracleAnalytics;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.BaseActivity;
import co.muslimummah.android.base.lifecycle.ScreenEvent;
import co.muslimummah.android.module.prayertime.data.model.PrayerTimeLocationInfo;
import co.muslimummah.android.module.prayertime.manager.NotificationHandlerManager;
import co.muslimummah.android.module.prayertime.manager.PrayerTimeManager;
import co.muslimummah.android.module.prayertime.ui.activity.MainActivity;
import co.muslimummah.android.player.MediaNotificationHelper;
import co.muslimummah.android.util.PhoneInfoUtils;
import co.muslimummah.android.util.SchemeUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class LauncherActivity extends BaseActivity {

    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final long ACTIVITY_DELAY_MILL = 2000;

    //jump uri
    private Uri mUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Do not setContentView here so we only show windowBackground of this activity.
        mUri = getIntent().getData();
        final String url = getIntent().getStringExtra(SchemeUtils.EXTRA_URL);

        if (!TextUtils.isEmpty(url)) {
            logEvent(url);
        }

        Timber.d("url %s", url);
        if (!TextUtils.isEmpty(url)) {
            mUri = Uri.parse(url);
        }

        AppLinkData.fetchDeferredAppLinkData(getApplicationContext(), new AppLinkData.CompletionHandler() {
            @Override
            public void onDeferredAppLinkDataFetched(AppLinkData appLinkData) {
                Timber.d("appLinkData %b", appLinkData != null);
//                if (appLinkData!=null){
//                    Uri uri = appLinkData.getTargetUri();
//                    Timber.d("onDeferredAppLinkDataFetched %s",uri.toString());
//                    SchemeUtils.parseUri(LauncherActivity.this, uri);
//                }

            }
        });

        Timber.d("onCreate isTaskRoot %b", isTaskRoot());

        if (!isTaskRoot()
//                && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
//                && intent.getAction() != null
//                && intent.getAction().equals(Intent.ACTION_MAIN)
                ) {
            if (mUri != null) {
                Timber.d("onCreate jump");
                SchemeUtils.parseUri(this, mUri);
            }
            finish();
            return;
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        jumpInterface();

        if (PhoneInfoUtils.isNetworkEnable(LauncherActivity.this)
                && GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS
                && PrayerTimeManager.instance().getSelectedLocationInfo() == null) {
            acquireLastLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThirdPartyAnalytics.INSTANCE
                .setCurrentScreen(this, "Launcher");
    }

    private void logEvent(String url) {
        if (getIntent().hasExtra(NotificationHandlerManager.INTENT_KEY_CLICK_ACTION_PRAYERTIME)) {
            String name = getIntent()
                    .getStringExtra(NotificationHandlerManager.INTENT_KEY_CLICK_ACTION_PRAYERTIME);

            ThirdPartyAnalytics.INSTANCE.logEvent(GA.Category.PrayerTimeNotification,
                    GA.Action.Click,
                    name);

            OracleAnalytics.INSTANCE
                    .addLog(LogObject.newBuilder()
                            .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                            .location(AnalyticsConstants.LOCATION.NOTIFICATION_PAGE_PRAYERTIMES)
                            .target(AnalyticsConstants.TARGET_TYPE.PRAYER_TIME_TYPE, name)
                            .build());

            getIntent().removeExtra(MediaNotificationHelper.INTENT_KEY_CLICK_ACTION);
        } else {
            OracleAnalytics.INSTANCE
                    .addLog(LogObject.newBuilder()
                            .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                            .location(AnalyticsConstants.LOCATION.NOTIFICATION_PAGE_SCHEME)
                            .target(AnalyticsConstants.TARGET_TYPE.SCHEME, url)
                            .build());
        }
    }

    private void acquireLastLocation() {
        final long startTime = System.currentTimeMillis();
        Observable.concat(getLastLocation(fusedLocationProviderClient), requestLocationUpdates(fusedLocationProviderClient))
                .firstOrError()
                .toObservable()
                .compose(co.muslimummah.android.module.prayertime.utils.PrayerTimesAtUtils.location2PrayerTimeLocationInfoTransformer())
                .compose(co.muslimummah.android.module.prayertime.utils.PrayerTimesAtUtils.updateTimeZoneIdBySystemTransformer())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<PrayerTimeLocationInfo>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull PrayerTimeLocationInfo prayerTimeLocationInfo) {
                        Timber.d("auto detected success " + prayerTimeLocationInfo.toString());
                        PrayerTimeManager.instance().saveAutoDetectedLocationInfo(prayerTimeLocationInfo);

                        ThirdPartyAnalytics.INSTANCE
                                .logEvent(GA.Category.LaunchApp,
                                        GA.Action.FindLocation,
                                        GA.Label.Success,
                                        System.currentTimeMillis() - startTime
                                );
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        Timber.e(e, getString(R.string.service_not_available));

                        ThirdPartyAnalytics.INSTANCE
                                .logEvent(GA.Category.LaunchApp,
                                        GA.Action.FindLocation,
                                        GA.Label.Failure,
                                        System.currentTimeMillis() - startTime
                                );
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    private ObservableSource<Location> requestLocationUpdates(final FusedLocationProviderClient fusedLocationProviderClient) {
        return Observable
                .create(new ObservableOnSubscribe<Location>() {
                    @Override
                    public void subscribe(@NonNull final ObservableEmitter<Location> emitter) throws Exception {
                        Timber.d("requestLocationUpdates start");
                        if (!PhoneInfoUtils.isGPSEnable(getApplicationContext())) {
                            emitter.onError(new RuntimeException("GPS is not available"));
                            Timber.d("requestLocationUpdates GPS is not available");
                            return;
                        }

                        LocationRequest locationRequest = LocationRequest.create();
                        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                        locationRequest.setInterval(1000);
                        locationRequest.setFastestInterval(500);

                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                                fusedLocationProviderClient.removeLocationUpdates(this);
                                Timber.d("requestLocationUpdates result");
                                Location location = locationResult.getLastLocation();
                                if (location != null) {
                                    Timber.d("requestLocationUpdates success");
                                    emitter.onNext(location);
                                    emitter.onComplete();
                                } else {
                                    Timber.d("requestLocationUpdates failed");
                                    emitter.onError(new RuntimeException("No result found"));
                                }
                            }
                        }, Looper.getMainLooper());
                    }
                });
    }

    private Observable<Location> getLastLocation(final FusedLocationProviderClient fusedLocationProviderClient) {
        return Observable.create(new ObservableOnSubscribe<Location>() {
            @Override
            public void subscribe(final @io.reactivex.annotations.NonNull ObservableEmitter<Location> e) throws Exception {
//                for test
//                if (true) {
//                    e.onComplete();
//                    return;
//                }
                fusedLocationProviderClient.getLastLocation()
                        .addOnCompleteListener(LauncherActivity.this, new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    Timber.d("getLastLocation success");
                                    e.onNext(task.getResult());
                                } else {
                                    Timber.d("getLastLocation unsuccess");
                                }
                                e.onComplete();
                            }
                        });
            }
        });
    }

    /**
     * 3000 milliseconds of residence time
     */
    private void jumpInterface() {
        Observable.timer(ACTIVITY_DELAY_MILL, TimeUnit.MILLISECONDS)
                .compose(lifecycleProvider().<Long>bindUntilEvent(ScreenEvent.DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long aLong) throws Exception {
                        if (mUri == null || !SchemeUtils.isOpenMainActivity(LauncherActivity.this, mUri)) {
                            Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
                            startActivity(intent, ActivityOptionsCompat
                                    .makeCustomAnimation(LauncherActivity.this, R.anim.anim_in_alpha,
                                            R.anim.anim_out_alpha).toBundle());
                        }

                        if (mUri != null) {
                            SchemeUtils.parseUri(LauncherActivity.this, mUri);
                        }
                        finish();
                    }
                });
    }
}

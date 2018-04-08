package co.muslimummah.android.util;

import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import co.muslimummah.android.OracleApp;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;

/**
 * Created by frank on 9/27/17.
 */
public class LocationUpdateManager {
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 50000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;

    public LocationUpdateManager() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(OracleApp.getInstance());

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
    }

    public Observable<Location> getLastLocation() {
        return Observable.create(new ObservableOnSubscribe<Location>() {
            @Override
            public void subscribe(@NonNull final ObservableEmitter<Location> emitter) throws Exception {
                if (!PhoneInfoUtils.isGPSEnable(OracleApp.getInstance())) {
                    emitter.onError(new IllegalStateException("Location service is not enabled!"));
                    return;
                }
                if (mLastLocation != null) {
                    emitter.onNext(mLastLocation);
                    emitter.onComplete();
                    return;
                }
                mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        mFusedLocationProviderClient.removeLocationUpdates(this);

                        mLastLocation = locationResult.getLastLocation();
                        if (mLastLocation != null) {
                            emitter.onNext(mLastLocation);
                            emitter.onComplete();
                        } else {
                            emitter.onError(new RuntimeException("No result found"));
                        }
                    }
                }, Looper.getMainLooper());
            }
        });
    }
}

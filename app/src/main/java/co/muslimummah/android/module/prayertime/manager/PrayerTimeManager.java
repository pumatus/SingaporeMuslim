package co.muslimummah.android.module.prayertime.manager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Locale;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.network.ApiFactory;
import co.muslimummah.android.network.ApiService;
import co.muslimummah.android.network.Entity.response.TimeZoneJson;
import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.data.model.PrayerTimeLocationInfo;
import co.muslimummah.android.module.prayertime.utils.PrayerTimesAtUtils;
import co.muslimummah.android.storage.AppSession;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import timber.log.Timber;

/**
 * Created by Xingbo.Jie on 18/9/17.
 */

public class PrayerTimeManager {
    private static PrayerTimeManager instance;
    private AppSession appSession;

    public static PrayerTimeManager instance() {
        if (instance == null) {
            synchronized (PrayerTimeManager.class) {
                if (instance == null) {
                    instance = new PrayerTimeManager();
                }
            }
        }
        return instance;
    }

    private PrayerTimeManager() {
        appSession = AppSession.getInstance(OracleApp.getInstance());
    }

    public Observable<TimeZoneJson> getTimezone(Context context, String latlng, long timestampInSecond) {
        String key = "";
        try {
            key = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)
                    .metaData
                    .getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return ApiFactory.get(ApiService.class).googleTimezone(latlng, timestampInSecond, key);
    }

    public PrayerTimeLocationInfo getSelectedLocationInfo() {
        return appSession.getCachedValue(Constants.KEY_LAST_LOCATION_CITY, PrayerTimeLocationInfo.class);
    }

    public void saveSelecetedLocationInfo(PrayerTimeLocationInfo info) {
        appSession.cacheValue(Constants.KEY_LAST_LOCATION_CITY, info, true);
    }

    public PrayerTimeLocationInfo getAutoDetectedLocationInfo() {
        return appSession.getCachedValue(Constants.KEY_AUTO_DETECTED_CITY, PrayerTimeLocationInfo.class);
    }

    public void saveAutoDetectedLocationInfo(PrayerTimeLocationInfo info) {
        appSession.cacheValue(Constants.KEY_AUTO_DETECTED_CITY, info, false);
    }


    public Observable<PrayerTimeLocationInfo> getAddressInfoByLocation(final Location location) {
        return Observable
                .create(new ObservableOnSubscribe<PrayerTimeLocationInfo>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<PrayerTimeLocationInfo> e) throws Exception {
                        Geocoder geocoder = new Geocoder(OracleApp.getInstance(), Locale.getDefault());
                        List<Address> addressList = geocoder
                                .getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        if (addressList != null && addressList.size() > 0) {
                            Address address = addressList.get(0);
                            Timber.d("address : " + address.toString());

                            String locality = address.getLocality();
                            if (!TextUtils.isEmpty(address.getSubAdminArea())) {
                                locality = address.getSubAdminArea();
                            }

                            e.onNext(PrayerTimeLocationInfo.builder()
                                    .country(address.getCountryName())
                                    .countryCode(address.getCountryCode())
                                    .locality(locality)
                                    .identification(locality)
//                                    .subLocality(address.getSubLocality())
                                    .altitude(location.getAltitude())
                                    .latitude(address.getLatitude())
                                    .longitude(address.getLongitude())
                                    .build());
                            e.onComplete();
                        } else {
                            e.onError(new RuntimeException("result of getAddressInfo is null"));
                        }
                    }
                });
    }

    public Observable<PrayerTimeLocationInfo> getAddressInfo(final GoogleApiClient googleApiClient, final String locationName, final String placeId) {
        return Observable
                .zip(Observable.create(new ObservableOnSubscribe<PrayerTimeLocationInfo>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<PrayerTimeLocationInfo> e) throws Exception {
                        Geocoder geocoder = new Geocoder(OracleApp.getInstance(), Locale.getDefault());
                        List<Address> addressList = geocoder
                                .getFromLocationName(locationName, 1);
                        if (addressList != null && addressList.size() > 0) {
                            Address address = addressList.get(0);
                            String locality = address.getLocality();
                            if (!TextUtils.isEmpty(address.getSubAdminArea())) {
                                locality = address.getSubAdminArea();
                            }

                            Timber.d("address : " + address.toString());
                            e.onNext(PrayerTimeLocationInfo.builder()
                                    .country(address.getCountryName())
                                    .countryCode(address.getCountryCode())
                                    .locality(locationName)
                                    .identification(locality)
//                                    .subLocality(address.getSubLocality())
                                    .latitude(address.getLatitude())
                                    .longitude(address.getLongitude())
                                    .build());
                            e.onComplete();
                        } else {
                            e.onError(new RuntimeException(""));
                        }
                    }
                }), Observable.create(new ObservableOnSubscribe<LatLng>() {
                    @Override
                    public void subscribe(@NonNull final ObservableEmitter<LatLng> e) throws Exception {
                        Places.GeoDataApi.getPlaceById(googleApiClient, placeId).setResultCallback(new ResultCallback<PlaceBuffer>() {
                            @Override
                            public void onResult(@android.support.annotation.NonNull PlaceBuffer places) {
                                if (places.getStatus().isSuccess() && places.getCount() > 0) {
                                    final Place place = places.get(0);
                                    LatLng latLng = place.getLatLng();
                                    if (latLng != null) {
                                        e.onNext(latLng);
                                        e.onComplete();
                                    } else {
                                        e.onError(new RuntimeException("fetch latLng by placeId failed"));
                                    }
                                } else {
                                    e.onError(new RuntimeException("fetch latLng by placeId failed"));
                                    Timber.e("Place query did not complete. Error: [%s]", places.getStatus().toString());
                                }
                                places.release();
                            }
                        });
                    }
                }), new BiFunction<PrayerTimeLocationInfo, LatLng, PrayerTimeLocationInfo>() {
                    @Override
                    public PrayerTimeLocationInfo apply(@NonNull PrayerTimeLocationInfo info, @NonNull LatLng latLng) throws Exception {
                        info.setLatitude(latLng.latitude);
                        info.setLongitude(latLng.longitude);
                        return info;
                    }
                })
                .compose(PrayerTimesAtUtils.updateTimeZoneIdByGoogleApiTransformer());
    }
}

package co.muslimummah.android;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.util.TimeZone;

import co.muslimummah.android.base.model.DeviceInfo;
import co.muslimummah.android.network.ApiFactory;
import co.muslimummah.android.network.ApiService;
import co.muslimummah.android.storage.AppSession;
import co.muslimummah.android.util.Utils;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import timber.log.Timber;

/**
 * Created by frank on 8/27/17.
 * Force register firebase token to server when login & logout.
 * Register firebase token on App start.
 */

public class OracleFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final String SP_KEY_HAS_SENT_FIREBASE_ID_TO_SERVER = "OracleFirebaseInstanceIDService.SP_KEY_HAS_SENT_FIREBASE_ID_TO_SERVER";

    @Override
    public void onTokenRefresh() {
        forceRegisterFCMTokenToServer();
    }

    public static void forceRegisterFCMTokenToServer() {
        AppSession.getInstance(OracleApp.getInstance()).cacheValue(SP_KEY_HAS_SENT_FIREBASE_ID_TO_SERVER, false, true);
        registerFCMTokenToServer();
    }

    public static void registerFCMTokenToServer() {
        Timber.d("FCM Token %s", FirebaseInstanceId.getInstance().getToken());
        if (!Boolean.TRUE.equals(AppSession.getInstance(OracleApp.getInstance()).getCachedValue(SP_KEY_HAS_SENT_FIREBASE_ID_TO_SERVER, Boolean.class))
                && FirebaseInstanceId.getInstance().getToken() != null) {
            //Register FCM Token to our backend. If success, call AppSession.getInstance(OracleApp.getInstance()).cacheValue(SP_KEY_HAS_SENT_FIREBASE_ID_TO_SERVER, true, true);
            ApiFactory.get(ApiService.class)
                    .registerDevice(new DeviceInfo(Utils.getDeviceId(OracleApp.getInstance()), FirebaseInstanceId.getInstance().getToken(), TimeZone.getDefault().getID()))
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<ResponseBody>() {
                        @Override
                        public void accept(@NonNull ResponseBody body) throws Exception {
                            AppSession.getInstance(OracleApp.getInstance()).cacheValue(SP_KEY_HAS_SENT_FIREBASE_ID_TO_SERVER, true, true);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@NonNull Throwable throwable) throws Exception {
                        }
                    });
        }
    }
}

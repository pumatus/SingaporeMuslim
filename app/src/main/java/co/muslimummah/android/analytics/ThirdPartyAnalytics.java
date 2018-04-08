package co.muslimummah.android.analytics;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

import co.muslimummah.android.BuildConfig;
import co.muslimummah.android.R;
import co.muslimummah.android.util.Utils;
import timber.log.Timber;


/**
 * Created by frank on 7/27/17.
 */
public enum ThirdPartyAnalytics {
    INSTANCE;

    private static final String TAG = "OracleAnalytics";
    private Tracker mTracker;
    private FirebaseAnalytics mFirebaseAnalytics;

    public void registerApp(Application application) {
        GoogleAnalytics analytics = GoogleAnalytics.getInstance(application);
        mTracker = analytics.newTracker(R.xml.global_tracker);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(application);
        String deviceId = Settings.Secure.getString(application.getContentResolver(), Settings.Secure.ANDROID_ID);
        // https://developers.google.com/analytics/devguides/collection/android/v4/user-id
        // You only need to set User ID on a tracker once. By setting it on the
        // tracker, the ID will be sent with all subsequent hits.
        mTracker.set("&uid", Utils.computeSHA256Hash(deviceId));
    }

    public void logEvent(@NonNull GA.Category category, @NonNull GA.Action action) {
        logEvent(category.getValue(), action.getValue(), null, null);
    }

    public void logEvent(@NonNull GA.Category category, @NonNull GA.Action action, @NonNull GA.Label label) {
        logEvent(category.getValue(), action.getValue(), label.getValue(), null);
    }

    public void logEvent(@NonNull GA.Category category, @NonNull GA.Action action, @NonNull GA.Label label, @NonNull Long value) {
        logEvent(category.getValue(), action.getValue(), label.getValue(), value);
    }

    public void logEvent(@NonNull GA.Category category, @NonNull GA.Action action, @NonNull String label) {
        logEvent(category.getValue(), action.getValue(), label, null);
    }

    public void logEvent(@NonNull GA.Category category, @NonNull GA.Action action, @NonNull String label, @NonNull Long value) {
        logEvent(category.getValue(), action.getValue(), label, value);
    }

    public void logEvent(@NonNull String category,
                         @NonNull String action,
                         @Nullable String label,
                         @Nullable Long value) {
        if (TextUtils.isEmpty(category) || TextUtils.isEmpty(action)) {
            throw new IllegalArgumentException("Category or action can not be null");
        }
        Timber.tag(TAG);
        Timber.d("Analytics post event \tcategory:[%s]\n" +
                        "\t\t\t\t\taction:[%s]\n" +
                        "\t\t\t\t\tlabel:[%s]\n" +
                        "\t\t\t\t\tvalue:[%d]\n",
                category,
                action,
                label,
                value);
        if (BuildConfig.DEBUG) {
            return;
        }
        //Build and send a Google Analytics Event.
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action);
        if (label != null) {
            eventBuilder.setLabel(label);
        }
        if (value != null) {
            eventBuilder.setValue(value);
        }
        mTracker.send(eventBuilder.build());

        //Build and send a Firebase Analytics Event.
        Bundle bundle = new Bundle();
        bundle.putString("action", action);
        if (label != null) {
            bundle.putString("label", label);
        }
        if (value != null) {
            bundle.putLong(FirebaseAnalytics.Param.VALUE, value);
        }
        mFirebaseAnalytics.logEvent(category, bundle);
    }

    /**
     * Should be called at onResume callback of Activity / Fragment, etc.
     *
     * @param activity
     * @param screenName
     */
    public void setCurrentScreen(@NonNull Activity activity, @NonNull String screenName) {
        if (activity == null || TextUtils.isEmpty(screenName)) {
            throw new IllegalArgumentException("ScreenName can not be null");
        }
        Timber.tag(TAG);
        Timber.d("Analytics show screen [%s]\n", screenName);
        if (BuildConfig.DEBUG) {
            return;
        }
        mTracker.setScreenName(screenName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        mFirebaseAnalytics.setCurrentScreen(activity, screenName, null);
    }
}

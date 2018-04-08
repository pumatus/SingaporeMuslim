package co.muslimummah.android.module.qibla.fragment;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.BaseFragment;
import co.muslimummah.android.base.lifecycle.ScreenEvent;
import co.muslimummah.android.module.prayertime.data.model.PrayerTimeLocationInfo;
import co.muslimummah.android.module.prayertime.manager.PrayerTimeManager;
import co.muslimummah.android.module.prayertime.ui.activity.HasCompassDelegate;
import co.muslimummah.android.module.prayertime.ui.fragment.PrayerTimeFragment;
import co.muslimummah.android.module.qibla.helper.CompassOrientationDelegate;
import co.muslimummah.android.module.qibla.helper.MeccaOrientationCalculator;
import co.muslimummah.android.module.qibla.view.CompassNotAccurateDialog;
import co.muslimummah.android.module.qibla.view.OrientationInfoView;
import co.muslimummah.android.module.qibla.view.QiblaView;
import co.muslimummah.android.util.LocationUpdateManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by frank on 9/25/17.
 */

public class QiblaFragment extends BaseFragment {
    @BindView(R.id.ll_content)
    LinearLayout llContent;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.qv)
    QiblaView qv;
    @BindView(R.id.oiv)
    OrientationInfoView oiv;
    Unbinder unbinder;

    CompassNotAccurateDialog mCompassNotAccurateDialog;
    CompassOrientationDelegate mCompassOrientationDelegate;
    LocationUpdateManager mLocationUpdateManager;

    boolean hasShownNotAccurateDialog;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCompassOrientationDelegate = ((HasCompassDelegate) context).getCompassOrientationDelegate();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qibla, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isHidden()) {
            onVisibleToUser();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (!isHidden()) {
            onHiddenToUser();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden) {
            onVisibleToUser();
        } else {
            onHiddenToUser();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCompassOrientationDelegate.setCompassEventListener(null);
        unbinder.unbind();
    }

    private void init(View view) {
        unbinder = ButterKnife.bind(this, view);
        mCompassNotAccurateDialog = new CompassNotAccurateDialog(getContext());
        mLocationUpdateManager = new LocationUpdateManager();

        mCompassOrientationDelegate.setCompassEventListener(new CompassOrientationDelegate.CompassEventListener() {
            @Override
            public void onNewOrientationDegree(float newDegree) {
                qv.setCompassOrientation(newDegree);
            }

            @Override
            public void onAccuracyChanged(boolean isLowAccuracy) {
                tryShowCompassNotAccurateDialog();
            }
        });

        if (!mCompassOrientationDelegate.isCompassFeatureEnabled()) {
            ThirdPartyAnalytics.INSTANCE.logEvent("Qibla", "Showpage", "NoCompassPage", null);
            Snackbar.make(llContent, getString(R.string.msg_no_compass_feature), Snackbar.LENGTH_INDEFINITE).show();
        }
    }

    private void tryShowCompassNotAccurateDialog() {
        if (!hasShownNotAccurateDialog && !isHidden() && mCompassOrientationDelegate.isLowAccuracy() && !mCompassNotAccurateDialog.isShowing()) {
            hasShownNotAccurateDialog = true;
            ThirdPartyAnalytics.INSTANCE.logEvent("Qibla", "Showpage", "SensorPage", null);
            mCompassNotAccurateDialog.show();
            Observable.timer(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .compose(lifecycleProvider().<Long>bindUntilEvent(ScreenEvent.DESTROY))
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(@NonNull Long aLong) throws Exception {
                            mCompassNotAccurateDialog.dismiss();
                        }
                    });
        }
    }

    private void refreshCurrentLocation(double latitude, double longitude) {
        ThirdPartyAnalytics.INSTANCE.logEvent("Qibla", "Showpage", "HomepageNormal", null);
        mCompassOrientationDelegate.updateMagneticDeclination(latitude, longitude);
        float meccaOrientation = MeccaOrientationCalculator.computeToMeccaDegree(latitude, longitude);
        qv.setMeccaOrientation(meccaOrientation);
        oiv.setDisplayLocation(latitude, longitude, meccaOrientation);
    }

    private void onVisibleToUser() {
        PrayerTimeLocationInfo locationInfo = PrayerTimeManager.instance().getSelectedLocationInfo();
        if (mCompassOrientationDelegate.isCompassFeatureEnabled()) {
            tryShowCompassNotAccurateDialog();
        }

        if (locationInfo != null) {
            refreshCurrentLocation(locationInfo.getLatitude(), locationInfo.getLongitude());

            if (mCompassOrientationDelegate.isCompassFeatureEnabled()) {
                mLocationUpdateManager.getLastLocation()
                        .compose(lifecycleProvider().<Location>bindUntilEvent(ScreenEvent.DESTROY))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Location>() {
                            @Override
                            public void accept(@NonNull Location location) throws Exception {
                                refreshCurrentLocation(location.getLatitude(), location.getLongitude());
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                            }
                        });
            }
        } else {
            if (mCompassOrientationDelegate.isCompassFeatureEnabled()) {
                ThirdPartyAnalytics.INSTANCE.logEvent("Qibla", "Showpage", "LocationNotFoundPage", null);
                Snackbar.make(llContent, getString(R.string.msg_cannot_determine_location), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void onHiddenToUser() {
//        mCompassOrientationDelegate.stop();
        if (mCompassNotAccurateDialog != null && mCompassNotAccurateDialog.isShowing()) {
            mCompassNotAccurateDialog.dismiss();
        }
    }
}

package co.muslimummah.android.module.prayertime.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.Analytics;
import co.muslimummah.android.analytics.AnalyticsConstants;
import co.muslimummah.android.analytics.LogObject;
import co.muslimummah.android.analytics.OracleAnalytics;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.BaseActivity;
import co.muslimummah.android.base.OracleLocaleHelper;
import co.muslimummah.android.module.prayertime.ui.fragment.PrayerTimeFragment;
import co.muslimummah.android.module.qibla.fragment.QiblaFragment;
import co.muslimummah.android.module.qibla.helper.CompassOrientationDelegate;
import co.muslimummah.android.module.quran.fragment.QuranFragment;
import co.muslimummah.android.module.quran.view.QuranSettingTranslationView;
import co.muslimummah.android.module.quran.view.QuranSettingView;
import co.muslimummah.android.util.SchemeUtils;
import co.muslimummah.android.util.UiUtils;
import timber.log.Timber;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

/**
 * Created by Hongd on 2017/8/12.
 */

public class MainActivity extends BaseActivity implements HasCompassDelegate{
    @BindView(R.id.view_fake_status_bar)
    View viewFakeStatusBar;
    @BindView(R.id.dl)
    DrawerLayout dl;
    @BindView(R.id.qsv)
    QuranSettingView qsv;
    @BindView(R.id.main_navi_Prayer)
    RadioButton mainNaviPrayer;
    @BindView(R.id.main_navi_Quran)
    RadioButton mainNaviQuran;
    @BindView(R.id.main_navi_Qibla)
    RadioButton mainNaviQibla;
    @BindView(R.id.main_navi_radiogrop)
    RadioGroup mainNaviRadiogrop;
    PrayerTimeFragment prayerTimeFragment;
    QiblaFragment qiblaFragment;
    QuranFragment quranFragment;

    FragmentManager mFragmentManager;
    CompassOrientationDelegate compassOrientationDelegate;

    private int lastCheckedRadioID = -1;
    private boolean isFirstStart = true;

    /**
     * @param context from
     * @param tab     PRAYER_TIMES,CHAPTER_LIST,QIBLA
     * @param bundle  extra query bundle e.g. CALENDAR_EXPANDED
     */
    public static void start(Context context, @Nullable String tab, @Nullable Bundle bundle) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP);
        if (!TextUtils.isEmpty(tab)) {
            intent.putExtra(SchemeUtils.TAB, tab);
        }
        if (bundle != null) {
            intent.putExtra(SchemeUtils.MAIN_EXTRA_BUNDLE, bundle);
        }
        context.startActivity(intent, ActivityOptionsCompat
                .makeCustomAnimation(context, R.anim.anim_in_alpha,
                        R.anim.anim_out_alpha).toBundle());

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        adjustStatusBarHeight();
        init();
        Timber.d("onCreate");
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        Timber.d("onResumeFragments");
        jumpToTab();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
//        jumpToTab();
        Timber.d("onNewIntent");
    }

    /**
     * jump to which tab according to scheme
     */
    private void jumpToTab() {
        Intent intent = getIntent();
        if (intent.hasExtra(SchemeUtils.TAB)) {
            Timber.d("jumpToTab");
            final String tab = intent.getStringExtra(SchemeUtils.TAB);
            intent.removeExtra(SchemeUtils.TAB);
            switch (tab) {
                case SchemeUtils.PRAYER_TIMES:
                    if (intent.hasExtra(SchemeUtils.MAIN_EXTRA_BUNDLE)) {
                        Bundle bundle = intent.getBundleExtra(SchemeUtils.MAIN_EXTRA_BUNDLE);
                        String expanded = bundle.getString(SchemeUtils.CALENDAR_EXPANDED);
                        if (prayerTimeFragment != null) {
                            prayerTimeFragment.setCalendarState(TextUtils.equals(expanded, SchemeUtils.CALENDAR_STATUS_EXPANDED));
                        }
                    }
                    // else do nothing
                    break;
                case SchemeUtils.CHAPTER_LIST:
                    mainNaviRadiogrop.post(new Runnable() {
                        @Override
                        public void run() {
                            mainNaviRadiogrop.check(R.id.main_navi_Quran);
                        }
                    });
                    break;
                case SchemeUtils.QIBLA:
                    mainNaviRadiogrop.post(new Runnable() {
                        @Override
                        public void run() {
                            mainNaviRadiogrop.check(R.id.main_navi_Qibla);
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    }

    private void init() {
        mFragmentManager = getSupportFragmentManager();
        initFragments();

        mainNaviRadiogrop.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

                // Leave page
                if (getLocationByResID(lastCheckedRadioID) != null) {
                    OracleAnalytics.INSTANCE
                            .addLog(LogObject.newBuilder()
                                    .behaviour(AnalyticsConstants.BEHAVIOUR.LEAVE)
                                    .location(getLocationByResID(lastCheckedRadioID))
                                    .build());
                }
                //end leave page

                Fragment targetFragment = null;
                switch (checkedId) {
                    case R.id.main_navi_Prayer:
                        ThirdPartyAnalytics.INSTANCE.logEvent("HomeTab", "Click", "Prayers", null);
                        targetFragment = prayerTimeFragment;
                        ThirdPartyAnalytics.INSTANCE.setCurrentScreen(MainActivity.this, "HomepagePrayerTime");
                        break;
                    case R.id.main_navi_Quran:
                        ThirdPartyAnalytics.INSTANCE.logEvent("HomeTab", "Click", "Quran", null);
                        targetFragment = quranFragment;
                        ThirdPartyAnalytics.INSTANCE.setCurrentScreen(MainActivity.this, "HomepageQuran");
                        break;
                    case R.id.main_navi_Qibla:
                        ThirdPartyAnalytics.INSTANCE.logEvent("HomeTab", "Click", "Qibla", null);
                        targetFragment = qiblaFragment;
                        ThirdPartyAnalytics.INSTANCE.setCurrentScreen(MainActivity.this, "HomepageQibla");
                        break;
                    default:
                        break;
                }

                assert targetFragment != null;
                hideFragmentsExcept(fragmentTransaction, targetFragment);

                // enter page
                if (getLocationByResID(checkedId) != null) {
                    OracleAnalytics.INSTANCE
                            .addLog(LogObject.newBuilder()
                                    .behaviour(AnalyticsConstants.BEHAVIOUR.ENTER)
                                    .location(getLocationByResID(checkedId))
                                    .build());
                }
                //end enter page

                dl.setDrawerLockMode(targetFragment == quranFragment ? DrawerLayout.LOCK_MODE_UNLOCKED : DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                if (!targetFragment.isAdded()) {
                    fragmentTransaction.add(R.id.fl_content, targetFragment);
                } else {
                    fragmentTransaction.show(targetFragment);
                }

                lastCheckedRadioID = checkedId;
                //commit sync way
                fragmentTransaction.commitNowAllowingStateLoss();
            }
        });

        qsv.setOnTranslationSelectListener(new QuranSettingTranslationView.OnTranslationSelectListener() {
            @Override
            public void onTranslationSelected(Context context, @Nullable OracleLocaleHelper.LanguageEnum languageEnum) {
                quranFragment.updateTranslationContext(context);
            }
        });

        qsv.setOnTransliterationCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Toast.makeText(buttonView.getContext(), "TODO: switchTransliteration isChecked: [" + isChecked + "]", Toast.LENGTH_SHORT).show();
            }
        });

        qsv.setOnAudioSyncCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //Toast.makeText(buttonView.getContext(), "TODO: switchAnimation isChecked: [" + isChecked + "]", Toast.LENGTH_SHORT).show();
            }
        });

        mainNaviPrayer.setChecked(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getCompassOrientationDelegate().start();
        if (!isFirstStart && getLocationByResID(lastCheckedRadioID) != null) {
            OracleAnalytics.INSTANCE
                    .addLog(LogObject.newBuilder()
                            .behaviour(AnalyticsConstants.BEHAVIOUR.ENTER)
                            .location(getLocationByResID(lastCheckedRadioID))
                            .build());
        }

        if (isFirstStart) {
            isFirstStart = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        qsv.refreshVerseBookmarkCount();
        qsv.refreshTranslationSelection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (getLocationByResID(lastCheckedRadioID) != null) {
            OracleAnalytics.INSTANCE
                    .addLog(LogObject.newBuilder()
                            .behaviour(AnalyticsConstants.BEHAVIOUR.LEAVE)
                            .location(getLocationByResID(lastCheckedRadioID))
                            .build());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (getCompassOrientationDelegate().notWorking()) {
            long duration = System.currentTimeMillis() - getCompassOrientationDelegate().getStartTimestamp();
            AnalyticsConstants.TARGET_VAULE reason = null;
            if (!getCompassOrientationDelegate().isCompassFeatureEnabled()) {
                reason = AnalyticsConstants.TARGET_VAULE.NO_SENSOR;
            } else if (!getCompassOrientationDelegate().isOnSensorChanged()) {
                reason = AnalyticsConstants.TARGET_VAULE.NO_CALLBACK;
            } else if (!getCompassOrientationDelegate().isGetRotationMatrixSuccess()) {
                reason = AnalyticsConstants.TARGET_VAULE.CALLBACK_FAILURE;
            }

            if (reason != null) {
                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.NONE)
                                .location(AnalyticsConstants.LOCATION.HOME_PAGE)
                                .target(AnalyticsConstants.TARGET_TYPE.COMPASS_ISSUE, reason.value)
                                .reserved(String.valueOf(duration))
                                .build());
            }
        }

        getCompassOrientationDelegate().stop();
        if (dl.isDrawerOpen(Gravity.END)) {
            dl.closeDrawer(Gravity.END, false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PrayerTimeFragment.REQUEST_CHECK_SETTINGS) {
            prayerTimeFragment.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void adjustStatusBarHeight() {
        int statusBarHeight = UiUtils.getStatusBarHeight(this);
        if (statusBarHeight > 0) {
            //In case the reflection method to get status bar height failed.
            ViewGroup.LayoutParams fakeStatusBarDimLayoutParams = viewFakeStatusBar.getLayoutParams();
            fakeStatusBarDimLayoutParams.height = statusBarHeight;
            viewFakeStatusBar.setLayoutParams(fakeStatusBarDimLayoutParams);
        }
    }

    private void initFragments() {
        for (Fragment fragment : mFragmentManager.getFragments()) {
            if (fragment instanceof PrayerTimeFragment) {
                prayerTimeFragment = (PrayerTimeFragment) fragment;
            } else if (fragment instanceof QuranFragment) {
                quranFragment = (QuranFragment) fragment;
            } else if (fragment instanceof QiblaFragment) {
                qiblaFragment = (QiblaFragment) fragment;
            }
        }

        if (prayerTimeFragment == null) {
            prayerTimeFragment = new PrayerTimeFragment();
        }
        if (quranFragment == null) {
            quranFragment = new QuranFragment();
        }
        if (qiblaFragment == null) {
            qiblaFragment = new QiblaFragment();
        }
    }

    private void hideFragmentsExcept(FragmentTransaction fragmentTransaction, Fragment targetFragment) {
        for (Fragment fragment : mFragmentManager.getFragments()) {
            if (fragment != targetFragment && fragment.isVisible()) {
                fragmentTransaction.hide(fragment);
            }
        }
    }

    public void switchDrawerLayout(boolean open) {
        if (open) {
            dl.openDrawer(Gravity.END);
        } else {
            dl.closeDrawer(Gravity.END);
        }
    }

    private AnalyticsConstants.LOCATION getLocationByResID(int resId) {
        AnalyticsConstants.LOCATION location = null;
        switch (resId) {
            case R.id.main_navi_Prayer:
                location = AnalyticsConstants.LOCATION.PRAYER_TIME_PAGE;
                break;
            case R.id.main_navi_Quran:
                location = AnalyticsConstants.LOCATION.QURAN_CHAPTER_VIEW_PAGE;
                break;
            case R.id.main_navi_Qibla:
                location = AnalyticsConstants.LOCATION.QIBLA_PAGE;
                break;
            default:
                break;
        }

        return location;
    }

    @Override
    public CompassOrientationDelegate getCompassOrientationDelegate() {
        if (compassOrientationDelegate == null) {
            compassOrientationDelegate = new CompassOrientationDelegate(this);
        }
        return compassOrientationDelegate;
    }
}

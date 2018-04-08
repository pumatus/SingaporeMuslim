package co.muslimummah.android.module.prayertime.ui.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.Builder;
import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import calendar.OnCalendarClickListener;
import calendar.month.MonthCalendarView;
import calendar.schedule.ScheduleLayout;
import calendar.schedule.ScheduleRecyclerView;
import calendar.week.WeekCalendarView;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.GA;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.BaseFragment;
import co.muslimummah.android.base.lifecycle.ScreenEvent;
import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.data.model.PrayerTimeLocationInfo;
import co.muslimummah.android.module.prayertime.manager.PrayerTimeManager;
import co.muslimummah.android.module.prayertime.ui.activity.SearchActivity;
import co.muslimummah.android.module.prayertime.ui.view.PrayerTimeView;
import co.muslimummah.android.util.PhoneInfoUtils;
import co.muslimummah.android.module.prayertime.utils.PrayerTimesAtUtils;
import co.muslimummah.android.storage.AppSession;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class PrayerTimeFragment extends BaseFragment implements OnCalendarClickListener {
    private static final int REQUEST_MANUALLY_SEARCH_LOCATION = 111;
    private static final long MINUTES_IN_MILLISECOND_30 = 30 * 60 * 1000;

    //    protected GoogleApiClient googleApiClient;
    protected LocationRequest locationRequest;
    //    protected Location mCurrentLocation;
    protected boolean mRequestingLocationUpdates;
    public static final int REQUEST_CHECK_SETTINGS = 100;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Provides the entry point to the Fused Location Provider API.
    private FusedLocationProviderClient fusedLocationProviderClient;
    MaterialDialog materialDialogSelect;
    MaterialDialog materialDialogLoading;
    //    CountDownTimer countDownTimer;
    AppSession appSession;

    PrayerTimeLocationInfo lastSelecetdLocation;
    Calendar lastSelectedCalendar;
    boolean needCountDownTimer = false;
    Disposable lastCountDownDisposable;

    private boolean requestLocationSettingFromLocateMe;

    SimpleDateFormat classicDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    @BindView(R.id.tv_location)
    TextView tvLocation;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.tv_calendar_en)
    TextView tvCalendarEn;
    @BindView(R.id.tv_calendar_ar)
    TextView tvCalendarAr;
    @BindView(R.id.iv_calendar_toggle)
    ImageView ivCalendarToggle;
    @BindView(R.id.mcvCalendar)
    MonthCalendarView mcvCalendar;
    @BindView(R.id.rlMonthCalendar)
    RelativeLayout rlMonthCalendar;
    @BindView(R.id.wcvCalendar)
    WeekCalendarView wcvCalendar;
    @BindView(R.id.rvScheduleList)
    ScheduleRecyclerView rvScheduleList;
    @BindView(R.id.tv_impdate_month)
    TextView tvImpdateMonth;
    @BindView(R.id.tv_impdate_desc)
    TextView tvImpdateDesc;
    @BindView(R.id.rl_impdate)
    RelativeLayout rlImpdate;
    @BindView(R.id.rlScheduleList)
    RelativeLayout rlScheduleList;
    @BindView(R.id.slSchedule)
    ScheduleLayout slSchedule;


    Unbinder unbinder;
    ViewHolder prayerTimeViewHolder;

    private ArrayList<PrayerTimeMode> prayerTimeModes;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appSession = AppSession.getInstance(getActivity());
        lastSelectedCalendar = Calendar.getInstance();
        lastSelectedCalendar.set(Calendar.HOUR_OF_DAY, 12);
        lastSelectedCalendar.set(Calendar.MINUTE, 0);
        lastSelectedCalendar.set(Calendar.SECOND, 0);
        initPrayerTimeData();

        fusedLocationProviderClient = LocationServices
                .getFusedLocationProviderClient(getActivity().getApplicationContext());
        mRequestingLocationUpdates = false;

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
    }

    public void setCalendarState(final boolean expanded) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                slSchedule.setCalendarExpanded(expanded);
            }
        });
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindViews({R.id.fajr, R.id.sunrise, R.id.dhuhr, R.id.asr, R.id.maghrib, R.id.isha})
        List<PrayerTimeView> prayerTimeViews;

        public ViewHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        public void refreshView() {
            long lastPrayerTime = 0, currentPrayerTime;
            long currentTime = System.currentTimeMillis();
            boolean isSelected;
            boolean needTimer = false;
            String countDown;
            for (int i = 0; i < 6; i++) {
                isSelected = false;
                countDown = null;
                currentPrayerTime = PrayerTimesAtUtils.getTimeInMillis(lastSelectedCalendar, prayerTimeModes.get(i).getTime());

                if (PrayerTimesAtUtils.isSameDay(lastSelectedCalendar, Calendar.getInstance())
                        && prayerTimeModes.get(i).getType() != PrayerTimeType.SUNRISE) {
                    //today countdown
                    if (currentTime > lastPrayerTime + MINUTES_IN_MILLISECOND_30
                            && currentTime < currentPrayerTime + MINUTES_IN_MILLISECOND_30) {
                        isSelected = true;
                        needTimer = true;

                        if (currentPrayerTime > currentTime) {
                            countDown = PrayerTimesAtUtils.countDownFormat(currentPrayerTime - currentTime);
                        } else {
                            countDown = getContext().getString(R.string.now);
                        }
                    }
                } else if (i == 0 && PrayerTimesAtUtils.isTomorrow(lastSelectedCalendar)) {
                    // for next day first item countdown
                    // selected is tomorrow, so we need the last prayer time of today
                    if (lastSelecetdLocation != null) {
                        long lastPrayerTimeOfPreviousDay = PrayerTimesAtUtils.getlastPrayerTime(Calendar.getInstance(), lastSelecetdLocation);
                        if (currentTime > lastPrayerTimeOfPreviousDay + MINUTES_IN_MILLISECOND_30) {
                            isSelected = true;
                            needTimer = true;
                            countDown = PrayerTimesAtUtils.countDownFormat(currentPrayerTime - currentTime);
                        }
                    }
                }

                prayerTimeViews.get(i).refreshView(prayerTimeModes.get(i), isSelected, countDown);

                if (prayerTimeModes.get(i).getType() != PrayerTimeType.SUNRISE) {
                    lastPrayerTime = currentPrayerTime;
                }
            }

            needCountDownTimer = needTimer;
            if (needCountDownTimer) {
                startCountDown();
            } else {
                stopCountDown();
            }
        }
    }

    private void initPrayerTimeData() {
        prayerTimeModes = PrayerTimesAtUtils.getDefalutPrayerTimeModes();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_prayertime, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        materialDialogSelect = new Builder(view.getContext()).customView(R.layout.dialog_custom, true)
                .autoDismiss(false)
                .build();
        materialDialogLoading = new Builder(view.getContext()).customView(R.layout.dialog_loading, false)
                .canceledOnTouchOutside(false).autoDismiss(false).build();
        unbinder = ButterKnife.bind(this, view);

        rvScheduleList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvScheduleList.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                prayerTimeViewHolder = new ViewHolder(
                        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prayer_time, parent, false));
                return prayerTimeViewHolder;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                prayerTimeViewHolder.refreshView();
            }

            @Override
            public int getItemCount() {
                return 1;
            }
        });
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Calendar calendar = Calendar.getInstance();
        updateCalendarDisplayName(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        slSchedule.setOnCalendarClickListener(this);

        lastSelecetdLocation = PrayerTimeManager.instance().getSelectedLocationInfo();
        PrayerTimeLocationInfo autoDetectedLocation = PrayerTimeManager.instance().getAutoDetectedLocationInfo();

        if (lastSelecetdLocation == null && autoDetectedLocation != null) {
            lastSelecetdLocation = autoDetectedLocation;
            PrayerTimeManager.instance().saveAutoDetectedLocationInfo(null);
            PrayerTimeManager.instance().saveSelecetedLocationInfo(lastSelecetdLocation);
        }

        updateAddressButtonDisplay(lastSelecetdLocation);

        if (lastSelecetdLocation != null) {
            updateSelectedCalendar();
            updatePrayerTimeData(lastSelectedCalendar);

            if (autoDetectedLocation != null && autoDetectedLocation != lastSelecetdLocation
                    && !TextUtils.equals(autoDetectedLocation.getIdentification(), lastSelecetdLocation.getIdentification())) {
                showCustomViewDialog(getString(R.string.btn_yes), getString(R.string.btn_no), new OnChangeLocationConfirmListener(autoDetectedLocation),
                        new OnChangeLocationCancelListener(), Constants.DIALOG_CHANGE_LOCATION);

                //clear auto detected data
                PrayerTimeManager.instance().saveAutoDetectedLocationInfo(null);

                ThirdPartyAnalytics.INSTANCE
                        .logEvent(GA.Category.LaunchApp,
                                GA.Action.ShowPage,
                                GA.Label.NewLocation);
            } else {
                ThirdPartyAnalytics.INSTANCE
                        .logEvent(GA.Category.LaunchApp,
                                GA.Action.ShowPage,
                                GA.Label.HomepageNormal);
            }

        } else {
            //Default view
            if (prayerTimeViewHolder != null) {
                prayerTimeViewHolder.refreshView();
            }

            if (!PhoneInfoUtils.isNetworkEnable(getContext())) {
                Toast.makeText(getActivity(), getResources().getString(R.string.no_internet_connection), Toast.LENGTH_LONG)
                        .show();

                ThirdPartyAnalytics.INSTANCE
                        .logEvent(GA.Category.LaunchApp,
                                GA.Action.ShowPage,
                                GA.Label.HomepageNoInternet);
            } else {
                if (!PhoneInfoUtils.isGPSEnable(getActivity())) {
                    turnOnLocationDialog(false);

                    ThirdPartyAnalytics.INSTANCE
                            .logEvent(GA.Category.LaunchApp,
                                    GA.Action.ShowPage,
                                    GA.Label.LocationServices);
                } else {
                    showCustomViewDialog(getString(R.string.locate_me), getString(R.string.select_manually),
                            new requestLocationListener(), new manuallySearchLocationListener(), Constants.DIALOG_LOCADING_LOCATION);

                    ThirdPartyAnalytics.INSTANCE
                            .logEvent(GA.Category.LaunchApp,
                                    GA.Action.ShowPage,
                                    GA.Label.SelectLocation);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }
    }

    @OnClick(R.id.tv_location)
    public void showDialog() {
        ThirdPartyAnalytics.INSTANCE
                .logEvent(GA.Category.PrayerTimeLocation,
                        GA.Action.ClickCity,
                        tvLocation.getText().toString());

        showCustomViewDialog(getString(R.string.locate_me), getString(R.string.select_manually),
                new requestLocationListener(true), new manuallySearchLocationListener(true), Constants.DIALOG_SELECT_LOCATION);

    }

    @OnClick(R.id.iv_calendar_toggle)
    public void calendarToggle() {
        //TODO：toggle calendar
    }

    //// FIXME: 31/8/17
    private void updatePrayerTimeData(Calendar calendar) {
        if (lastSelecetdLocation == null) {
            return;
        }

        Timber.d("HOME updatePrayerTimeData");
        ArrayList<String> prayerTimes = PrayerTimesAtUtils.calculatePrayerTimes(calendar, lastSelecetdLocation);
        if (prayerTimes != null && prayerTimes.size() == 6) {
            for (int i = 0; i < prayerTimes.size(); ++i) {
                prayerTimeModes.get(i).setTime(prayerTimes.get(i));
            }
        }

        //refresh alarm
        if (PrayerTimesAtUtils.isSameDay(calendar, Calendar.getInstance())) {
            PrayerTimesAtUtils.placeNextAlarm(getContext(), lastSelecetdLocation, prayerTimeModes);
        }

        refreshPrayerTimeView();
    }

    private void refreshPrayerTimeView() {
        if (prayerTimeViewHolder != null) {
            prayerTimeViewHolder.refreshView();
        }
    }

    private void startCountDown() {
        if (lastCountDownDisposable == null) {
            lastCountDownDisposable = Observable.interval(1, TimeUnit.SECONDS)
                    .compose(lifecycleProvider().<Long>bindUntilEvent(ScreenEvent.STOP))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnTerminate(new Action() {
                        @Override
                        public void run() throws Exception {
                            lastCountDownDisposable = null;
                        }
                    })
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(@NonNull Long o) throws Exception {
                            if (unbinder != null) {
                                refreshPrayerTimeView();
                            }
                        }
                    });
        }
    }

    private void stopCountDown() {
        if (lastCountDownDisposable != null) {
            if (!lastCountDownDisposable.isDisposed()) {
                lastCountDownDisposable.dispose();
            }
            lastCountDownDisposable = null;
        }
    }

    public void updateAddressButtonDisplay(PrayerTimeLocationInfo lastSelecetdLocation) {
        if (lastSelecetdLocation != null) {
            Log.i("cityName1  ", lastSelecetdLocation.getDisplayName());
            tvLocation.setText(lastSelecetdLocation.getDisplayName());
        } else {
            tvLocation.setText(getString(R.string.select_location));
        }
    }

    private void updateLocation() {
        final long startTs = System.currentTimeMillis();
        Observable
                .create(new ObservableOnSubscribe<Location>() {
                    @Override
                    public void subscribe(@NonNull final ObservableEmitter<Location> emitter) throws Exception {
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                                fusedLocationProviderClient.removeLocationUpdates(this);

                                Location location = locationResult.getLastLocation();
                                if (location != null) {
                                    emitter.onNext(location);
                                    emitter.onComplete();
                                } else {
                                    emitter.onError(new RuntimeException("request Location No result found"));
                                }
                            }
                        }, Looper.getMainLooper());
                    }
                })
                .compose(PrayerTimesAtUtils.location2PrayerTimeLocationInfoTransformer())
                .compose(PrayerTimesAtUtils.updateTimeZoneIdBySystemTransformer())
                .compose(lifecycleProvider().<PrayerTimeLocationInfo>bindUntilEvent(ScreenEvent.DESTROY))
                .timeout(20, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<PrayerTimeLocationInfo>() {
                    @Override
                    public void accept(@NonNull PrayerTimeLocationInfo location) throws Exception {
                        materialDialogLoading.dismiss();
                        onLocationChanged(location);

                        ThirdPartyAnalytics.INSTANCE
                                .logEvent(GA.Category.PrayerTimeLocation,
                                        GA.Action.LocateMe,
                                        GA.Label.Success);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        String label = throwable instanceof TimeoutException ?
                                GA.Label.Timeout.getValue() :
                                GA.Label.Failure.getValue() + "[" + throwable.getMessage() + "]";
                        ThirdPartyAnalytics.INSTANCE
                                .logEvent(GA.Category.PrayerTimeLocation,
                                        GA.Action.LocateMe,
                                        label,
                                        System.currentTimeMillis() - startTs);

                        materialDialogLoading.dismiss();
                        showCustomViewDialog(getString(R.string.locate_me), getString(R.string.select_manually),
                                new requestLocationListener(), new manuallySearchLocationListener(),
                                Constants.DIALOG_NOTFOUND_LOCATION);
                    }
                });
    }

    private void onLocationChanged(@NonNull PrayerTimeLocationInfo location) {
        lastSelecetdLocation = location;

        updateSelectedCalendar();

        updateAddressButtonDisplay(lastSelecetdLocation);
        updatePrayerTimeData(lastSelectedCalendar);
        PrayerTimeManager.instance().saveSelecetedLocationInfo(lastSelecetdLocation);
    }

    private void updateSelectedCalendar() {
        if (lastSelecetdLocation != null && !TextUtils.isEmpty(lastSelecetdLocation.getTimeZoneId())) {
            try {
                TimeZone timeZone = TimeZone.getTimeZone(lastSelecetdLocation.getTimeZoneId());
                if (timeZone != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeZone(timeZone);
                    calendar.set(Calendar.YEAR, lastSelectedCalendar.get(Calendar.YEAR));
                    calendar.set(Calendar.MONTH, lastSelectedCalendar.get(Calendar.MONTH));
                    calendar.set(Calendar.DAY_OF_MONTH, lastSelectedCalendar.get(Calendar.DAY_OF_MONTH));
                    calendar.set(Calendar.HOUR_OF_DAY, 12);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    lastSelectedCalendar = calendar;
                }
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onClickDate(int year, int month, int day) {
        updateCalendarDisplayName(year, month, day);
        Calendar calendar = Calendar.getInstance();

        if (lastSelecetdLocation != null && !TextUtils.isEmpty(lastSelecetdLocation.getTimeZoneId())) {
            try {
                TimeZone timeZone = TimeZone.getTimeZone(lastSelecetdLocation.getTimeZoneId());
                if (timeZone != null) {
                    calendar.setTimeZone(timeZone);
                }
            } catch (Exception e) {
            }
        }

        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        lastSelectedCalendar = calendar;

        ThirdPartyAnalytics.INSTANCE
                .logEvent(GA.Category.Calendar,
                        GA.Action.SelectDate,
                        classicDateFormat.format(calendar.getTime()));
        if (lastSelecetdLocation != null) {
            updatePrayerTimeData(lastSelectedCalendar);
        }
    }

    @Override
    public void onPageChange(int year, int month, int day) {
        updateCalendarDisplayName(year, month, day);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        lastSelectedCalendar = calendar;
        updatePrayerTimeData(lastSelectedCalendar);
    }

    /**
     * abstract materialDialogSelect
     *
     * @param change location remote local
     */
    private void showCustomViewDialog(String btnOneStr, String btnTwoStr, OnClickListener onClickListenerOne,
                                      OnClickListener onClickListenerTwo, final int change) {

        RelativeLayout rl_sel_location = (RelativeLayout) materialDialogSelect.getCustomView()
                .findViewById(R.id.rl_sel_location);
        TextView tv_prompt = (TextView) materialDialogSelect.getCustomView()
                .findViewById(R.id.tv_prompt);

        RelativeLayout rl_change_view = (RelativeLayout) materialDialogSelect.getCustomView()
                .findViewById(R.id.rl_change_view);
        TextView tv_change_location = (TextView) materialDialogSelect.getCustomView()
                .findViewById(R.id.tv_change_location);


        RelativeLayout rl_not_found_location = (RelativeLayout) materialDialogSelect.getCustomView()
                .findViewById(R.id.rl_not_found_location);

        Button btn_dialogLocateMe = (Button) materialDialogSelect.getCustomView().findViewById(R.id.btn_dialogOne);
        Button btn_dialogSelectManually = (Button) materialDialogSelect.getCustomView()
                .findViewById(R.id.btn_dialogTwo);

        if (change == Constants.DIALOG_SELECT_LOCATION || change == Constants.DIALOG_LOCADING_LOCATION) {
            rl_sel_location.setVisibility(View.VISIBLE);
            rl_change_view.setVisibility(View.GONE);
            rl_not_found_location.setVisibility(View.GONE);
            tv_prompt.setText(change == Constants.DIALOG_SELECT_LOCATION ? R.string.select_your_location : R.string.no_location_with_services);
        } else if (change == Constants.DIALOG_CHANGE_LOCATION) {
            rl_sel_location.setVisibility(View.GONE);
            rl_change_view.setVisibility(View.VISIBLE);
            PrayerTimeLocationInfo autoDetectedLocation = PrayerTimeManager.instance().getAutoDetectedLocationInfo();
            tv_change_location.setText(autoDetectedLocation.getDisplayName());
            rl_not_found_location.setVisibility(View.GONE);
        } else if (change == Constants.DIALOG_NOTFOUND_LOCATION) {
            rl_sel_location.setVisibility(View.GONE);
            rl_change_view.setVisibility(View.GONE);
            rl_not_found_location.setVisibility(View.VISIBLE);
        }
        btn_dialogLocateMe.setText(btnOneStr);
        btn_dialogSelectManually.setText(btnTwoStr);
        btn_dialogLocateMe.setOnClickListener(onClickListenerOne);
        btn_dialogSelectManually.setOnClickListener(onClickListenerTwo);
        materialDialogSelect.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (change == Constants.DIALOG_SELECT_LOCATION || change == Constants.DIALOG_LOCADING_LOCATION) {
                    ThirdPartyAnalytics.INSTANCE
                            .logEvent(GA.Category.PrayerTimeLocation,
                                    GA.Action.SelectLocationPopup,
                                    GA.Label.Close);
                } else if (change == Constants.DIALOG_CHANGE_LOCATION) {
                    ThirdPartyAnalytics.INSTANCE
                            .logEvent(GA.Category.LaunchApp,
                                    GA.Action.NewLocationPopup,
                                    GA.Label.No);
                } else if (change == Constants.DIALOG_NOTFOUND_LOCATION) {
                    ThirdPartyAnalytics.INSTANCE
                            .logEvent(GA.Category.PrayerTimeLocation,
                                    GA.Action.NoResultPopup,
                                    GA.Label.Close);
                }

            }
        });

        materialDialogSelect.show();
    }

    /**
     * request location implements onClickListener
     */
    private class requestLocationListener implements OnClickListener {
        boolean fromLocateMe;

        public requestLocationListener(boolean fromLocateMe) {
            this.fromLocateMe = fromLocateMe;
        }

        public requestLocationListener() {
            this(false);
        }

        @Override
        public void onClick(View v) {
            if (!PhoneInfoUtils.isNetworkEnable(getActivity())) {
                materialDialogSelect.dismiss();
                PhoneInfoUtils.showToast(getString(R.string.no_internet_connection), false);
            } else {
                if (PhoneInfoUtils.isGPSEnable(getActivity())) {
                    materialDialogSelect.dismiss();
                    materialDialogLoading.show();
                    updateLocation();
                } else {
                    materialDialogSelect.dismiss();
                    turnOnLocationDialog(fromLocateMe);
                }
            }

            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.PrayerTimeLocation,
                            fromLocateMe ? GA.Action.SelectLocationPopup : GA.Action.NoResultPopup,
                            GA.Label.LocateMe);
        }
    }

    /**
     * manually location implements onClickListener
     */
    private class manuallySearchLocationListener implements OnClickListener {

        boolean fromLocateMe;

        public manuallySearchLocationListener(boolean fromLocateMe) {
            this.fromLocateMe = fromLocateMe;
        }

        public manuallySearchLocationListener() {
            this(false);
        }

        @Override
        public void onClick(View v) {

            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.PrayerTimeLocation,
                            fromLocateMe ? GA.Action.SelectLocationPopup : GA.Action.NoResultPopup,
                            GA.Label.SelectManually);

            materialDialogSelect.dismiss();
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivityForResult(intent, REQUEST_MANUALLY_SEARCH_LOCATION);
        }
    }

    private class OnChangeLocationConfirmListener implements OnClickListener {
        PrayerTimeLocationInfo locationInfo;

        public OnChangeLocationConfirmListener(PrayerTimeLocationInfo newLocationInfo) {
            this.locationInfo = newLocationInfo;
        }

        @Override
        public void onClick(View v) {
//            ThirdPartyAnalytics.INSTANCE.logEvent("PrayerTimeLocation ", "NewLocationPopup", "Yes", null);

            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.LaunchApp,
                            GA.Action.NewLocationPopup,
                            GA.Label.Yes);

            materialDialogSelect.dismiss();
            onLocationChanged(locationInfo);
        }
    }

    private class OnChangeLocationCancelListener implements OnClickListener {

        @Override
        public void onClick(View v) {

            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.LaunchApp,
                            GA.Action.NewLocationPopup,
                            GA.Label.No);
            materialDialogSelect.dismiss();
        }
    }


//    private Handler getHandler = new Handler();

    /**
     * Called after the autocomplete activity has finished to return its result.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        //Check that the result's was from the autoCompleted widget.super.onActivityResult(requestCode, resultCode,
        // data);
        Timber.d("SEARCH onActivityResult " + requestCode + " - " + resultCode + " - " + hashCode());
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_MANUALLY_SEARCH_LOCATION:
                if (resultCode == RESULT_OK) {
                    PrayerTimeLocationInfo info = (PrayerTimeLocationInfo) data.getSerializableExtra(Constants.RESULT_SEARCH_ACTIVITY);
                    if (info != null) {
                        onLocationChanged(info);
                    }
                } else if (resultCode == RESULT_CANCELED) {
//                    Toast.makeText(getContext(), "Place query did not complete", Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_CHECK_SETTINGS:
//                final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
                if (resultCode == RESULT_OK) {
                    materialDialogLoading.show();
                    if (!PhoneInfoUtils.isNetworkEnable(getActivity())) {
                        materialDialogLoading.dismiss();
                        PhoneInfoUtils.showToast(getString(R.string.no_internet_connection), false);
                    } else {
//                        startLocationUpdates();
                        updateLocation();
                    }

                    ThirdPartyAnalytics.INSTANCE
                            .logEvent(GA.Category.PrayerTimeLocation,
                                    GA.Action.LocationServicesPopup,
                                    GA.Label.OK);

                } else if (resultCode == RESULT_CANCELED) {
                    ThirdPartyAnalytics.INSTANCE
                            .logEvent(GA.Category.PrayerTimeLocation,
                                    GA.Action.LocationServicesPopup,
                                    GA.Label.Cancel);

                    if (requestLocationSettingFromLocateMe) {
                        showCustomViewDialog(getString(R.string.locate_me), getString(R.string.select_manually),
                                new requestLocationListener(true), new manuallySearchLocationListener(true), Constants.DIALOG_SELECT_LOCATION);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void updateCalendarDisplayName(int year, int month, int day) {
        GregorianCalendar gCal = new GregorianCalendar(year, month, day);
        Calendar cGal = new UmmalquraCalendar();
        cGal.setTime(gCal.getTime());
//        String str_calendar_en = gCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH);
//        String str_calendar_ar = cGal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH);

        String importantDay = PrayerTimesAtUtils.getNameOfImportantDay(cGal);
        if (importantDay != null) {
            rlImpdate.setVisibility(View.VISIBLE);
            tvImpdateDesc.setText(importantDay);
        } else {
            rlImpdate.setVisibility(View.GONE);
        }

        tvCalendarEn.setText(PrayerTimesAtUtils.buildMonthAtLocal(gCal.get(Calendar.MONTH)) + " " + gCal.get(Calendar.YEAR));
        tvCalendarAr.setText(PrayerTimesAtUtils.buildMonthAtAr(cGal.get(Calendar.MONTH)) + " " + cGal.get(Calendar.YEAR));
    }

    /**
     * Google play location settings
     */
    private void turnOnLocationDialog(final boolean fromLocateMe) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        LocationServices
                .getSettingsClient(getContext())
                .checkLocationSettings(builder.build())
                .addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                        try {
                            LocationSettingsResponse response = task.getResult(ApiException.class);
                            Timber.d("All location settings are satisfied.");
                            // All location settings are satisfied. The client can initialize location
                            // requests here.
                            materialDialogLoading.show();
                            if (!PhoneInfoUtils.isNetworkEnable(getActivity())) {
                                materialDialogLoading.dismiss();
                                PhoneInfoUtils.showToast(getString(R.string.no_internet_connection), false);
                            } else {
//                            startLocationUpdates();
                                updateLocation();
                            }
                        } catch (ApiException exception) {
                            switch (exception.getStatusCode()) {
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied. But could be fixed by showing the
                                    // user a dialog.
                                    try {
                                        // Cast to a resolvable exception.
                                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().
                                        requestLocationSettingFromLocateMe = fromLocateMe;
                                        resolvable.startResolutionForResult(
                                                getActivity(),
                                                REQUEST_CHECK_SETTINGS);
                                        Timber.d("Show the dialog by calling startResolutionForResult.");
                                    } catch (SendIntentException e) {
                                        // Ignore the error.
                                        Timber.e(e, "LocationSettingsResponse SendIntentException");
                                    } catch (ClassCastException e) {
                                        // Ignore, should be an impossible error.
                                        Timber.e(e, "LocationSettingsResponse ClassCastException.");
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    Timber.i("Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.");
                                    break;
                            }
                        }
                    }
                });

    }


    @Override
    public void onStart() {
        super.onStart();
        if (needCountDownTimer) {
            startCountDown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePrayerTimeData(lastSelectedCalendar);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }
}

package co.muslimummah.android.module.prayertime.ui.view;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.AnalyticsConstants;
import co.muslimummah.android.analytics.GA;
import co.muslimummah.android.analytics.LogObject;
import co.muslimummah.android.analytics.OracleAnalytics;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.player.SimpleMediaPlayer;
import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.ui.fragment.PrayerTimeMode;
import co.muslimummah.android.module.prayertime.ui.fragment.PrayerTimeType;
import co.muslimummah.android.module.prayertime.utils.PrayerTimesAtUtils;
import co.muslimummah.android.widget.SingleChoiceDialog;

import static android.R.attr.action;
import static android.R.attr.cacheColorHint;

/**
 * Created by Hongd on 2017/8/30.
 */

public class PrayerTimeView extends RelativeLayout {

    @BindView(R.id.iv_prayer_icon)
    ImageView ivPrayerIcon;
    @BindView(R.id.tv_prayer_name)
    TextView tvPrayerName;
    @BindView(R.id.tv_prayer_time)
    TextView tvPrayerTime;
    @BindView(R.id.iv_prayer_alarm)
    ImageView ivPrayerAlarm;
    @BindView(R.id.tv_prayer_countdown)
    TextView tvPrayerCountdown;

    private PrayerTimeMode mode;
    private List<SingleChoiceDialog.Item> alarmItems;

    public PrayerTimeView(Context context) {
        super(context);
        initView();
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.layout_prayer_time_view, this, true);
        ButterKnife.bind(this);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
    }

    public PrayerTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public PrayerTimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public void refreshView(PrayerTimeMode mode, boolean isSelected, String countDown) {
        this.mode = mode;

        ivPrayerIcon.setImageResource(getIconResourceByType(mode.getType()));
        ivPrayerIcon.setSelected(isSelected);

        // FIXME: 31/8/17 Text selected
        tvPrayerName.setText(getNameByType(mode.getType()));

        tvPrayerTime.setText(mode.getTime());
        tvPrayerTime.setSelected(isSelected);

        ivPrayerAlarm.setImageResource(getAlarmResId(PrayerTimesAtUtils.getAlarmStatus(getContext(), mode.getType())));
        ivPrayerAlarm.setSelected(isSelected);

        if (isSelected) {
            tvPrayerName.setTypeface(Typeface.DEFAULT_BOLD);
            tvPrayerName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            tvPrayerName.setTextColor(Color.BLACK);
            tvPrayerTime.setTextColor(Color.BLACK);
        } else {
            tvPrayerName.setTypeface(Typeface.DEFAULT);
            tvPrayerName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tvPrayerName.setTextColor(Color.parseColor("#999999"));
            tvPrayerTime.setTextColor(Color.parseColor("#999999"));
        }

        if (TextUtils.isEmpty(countDown)) {
            tvPrayerCountdown.setVisibility(GONE);
        } else {
            tvPrayerCountdown.setText(countDown);
            tvPrayerCountdown.setVisibility(VISIBLE);
        }

        setSelected(isSelected);
        postInvalidate();
    }

    private int getIconResourceByType(PrayerTimeType timeType) {
        switch (timeType) {
            case FAJR:
                return R.drawable.selected_prayer_time_fajr;
            case SUNRISE:
                return R.drawable.selected_prayer_time_sunrise;
            case DHUHR:
                return R.drawable.selected_prayer_time_dhuhr;
            case ASR:
                return R.drawable.selected_prayer_time_asr;
            case MAGHRIB:
                return R.drawable.selected_prayer_time_maghrib;
            case ISHA:
                return R.drawable.selected_prayer_time_isha;
        }
        return 0;
    }

    private String getNameByType(PrayerTimeType timeType) {
        return PrayerTimesAtUtils.getPrayerTimeName(timeType);
    }

    private int getAlarmResId(int status) {
        switch (status) {
            case Constants.NOTIFICATION_STATUS_MUTE:
                return R.drawable.selected_prayer_time_status_mute;
            case Constants.NOTIFICATION_STATUS_SOUND_1:
            case Constants.NOTIFICATION_STATUS_SOUND_2:
                return R.drawable.selected_prayer_time_status_horn;
            case Constants.NOTIFICATION_STATUS_SOUND_SYSTEM:
                return R.drawable.selected_prayer_time_status_sound;
            case Constants.NOTIFICATION_STATUS_OFF:
                return R.drawable.selected_prayer_time_status_off;
        }
        return 0;
    }

    @OnClick(R.id.iv_prayer_alarm)
    public void onAlramClick() {
        SingleChoiceDialog.create(getContext(),
                SingleChoiceDialog.Params.builder()
                        .items(createAlarmItemsIfNeeded())
                        .positiveButton(getContext().getText(R.string.ok).toString())
                        .selecedItem(getSettingSelecetedItem())
                        .title(getNameByType(mode.getType()))
                        .positiveButtonClickListener(new SingleChoiceDialog.OnPositiveButtonClickListener() {
                            @Override
                            public void onClick(int selectedItem) {
                                switch (selectedItem) {
                                    case 0:
                                        PrayerTimesAtUtils.setAlarmStatus(getContext(), mode.getType(), Constants.NOTIFICATION_STATUS_OFF);
                                        addGALog(Constants.NOTIFICATION_STATUS_OFF);
                                        break;
                                    case 1:
                                        PrayerTimesAtUtils.setAlarmStatus(getContext(), mode.getType(), Constants.NOTIFICATION_STATUS_MUTE);
                                        addGALog(Constants.NOTIFICATION_STATUS_MUTE);
                                        break;
                                    case 2:
                                        PrayerTimesAtUtils.setAlarmStatus(getContext(), mode.getType(), Constants.NOTIFICATION_STATUS_SOUND_SYSTEM);
                                        addGALog(Constants.NOTIFICATION_STATUS_SOUND_SYSTEM);
                                        break;
                                    case 3:
                                        PrayerTimesAtUtils.setAlarmStatus(getContext(), mode.getType(), Constants.NOTIFICATION_STATUS_SOUND_1);
                                        addGALog(Constants.NOTIFICATION_STATUS_SOUND_1);
                                        break;
                                    case 4:
                                        PrayerTimesAtUtils.setAlarmStatus(getContext(), mode.getType(), Constants.NOTIFICATION_STATUS_SOUND_2);
                                        addGALog(Constants.NOTIFICATION_STATUS_SOUND_2);
                                        break;
                                }

                                ivPrayerAlarm.setImageResource(getAlarmResId(PrayerTimesAtUtils.getAlarmStatus(getContext(), mode.getType())));
                            }
                        })
                        .onDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                SimpleMediaPlayer.stop();
                            }
                        })
                        .itemClickListener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                    case 1:
                                        SimpleMediaPlayer.stop();
                                        break;
                                    case 2:
                                        SimpleMediaPlayer.play(getContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                                        break;
                                    case 3:
                                        SimpleMediaPlayer.play(getContext(), R.raw.normal);
                                        break;
                                    case 4:
                                        SimpleMediaPlayer.play(getContext(), R.raw.soft);
                                        break;
                                }
                            }
                        })
                        .negativeButton(getContext().getText(R.string.cancel).toString())
                        .build()).show();
    }

    private List<SingleChoiceDialog.Item> createAlarmItemsIfNeeded() {
        if (alarmItems == null) {
            alarmItems = new ArrayList<>();
            alarmItems.add(SingleChoiceDialog.Item.builder()
                    .icon(R.drawable.selected_prayer_time_status_off)
                    .text(getContext().getText(R.string.off).toString())
                    .build());

            alarmItems.add(SingleChoiceDialog.Item.builder()
                    .icon(R.drawable.selected_prayer_time_status_mute)
                    .text(getContext().getText(R.string.mute).toString())
                    .build());

            alarmItems.add(SingleChoiceDialog.Item.builder()
                    .icon(R.drawable.selected_prayer_time_status_sound)
                    .text(getContext().getText(R.string.sound).toString())
                    .build());

            alarmItems.add(SingleChoiceDialog.Item.builder()
                    .icon(R.drawable.selected_prayer_time_status_horn)
                    .text(getContext().getText(R.string.alarm_nomal).toString())
                    .build());

            alarmItems.add(SingleChoiceDialog.Item.builder()
                    .icon(R.drawable.selected_prayer_time_status_horn)
                    .text(getContext().getText(R.string.alarm_soft).toString())
                    .build());
        }

        return alarmItems;
    }

    private void addServerLog(AnalyticsConstants.TARGET_VAULE value) {
        OracleAnalytics.INSTANCE
                .addLog(LogObject.newBuilder()
                        .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                        .location(getSettingLocation())
                        .target(AnalyticsConstants.TARGET_TYPE.NOTIFICATION_SETTINGS, value.value)
                        .build());
    }

    private void addGALog(int status) {
        ThirdPartyAnalytics.INSTANCE
                .logEvent(GA.Category.PrayerTimeNotification,
                        GA.Action.ChangeNotification,
                        getGALabel(status));
    }

    private String getGALabel(int status) {
        GA.Label label;
        switch (mode.getType()) {
            case FAJR:
                label = GA.Label.Fajr;
                break;
            case SUNRISE:
                label = GA.Label.Sunrise;
                break;
            case DHUHR:
                label = GA.Label.Dhuhr;
                break;
            case ASR:
                label = GA.Label.Asr;
                break;
            case MAGHRIB:
                label = GA.Label.Maghrib;
                break;
            case ISHA:
                label = GA.Label.Isha;
                break;
            default:
                label = GA.Label.Fajr;
                break;
        }

        String type;
        switch (status) {
            case Constants.NOTIFICATION_STATUS_OFF:
                type = "Off";
                break;
            case Constants.NOTIFICATION_STATUS_SOUND_SYSTEM:
                type = "Sound";
                break;
            case Constants.NOTIFICATION_STATUS_SOUND_1:
                type = "Adhan(Mecca)";
                break;
            case Constants.NOTIFICATION_STATUS_SOUND_2:
                type = "Adhan(Madina)";
                break;
            default:
                type = "Mute";
                break;
        }

        return String.format("%s[%s]", label.getValue(), type);
    }

    public AnalyticsConstants.LOCATION getSettingLocation() {
        switch (mode.getType()) {
            case FAJR:
                return AnalyticsConstants.LOCATION.SETTING_ICON_FAJR;
            case SUNRISE:
                return AnalyticsConstants.LOCATION.SETTING_ICON_SUNRISE;
            case DHUHR:
                return AnalyticsConstants.LOCATION.SETTING_ICON_DHUHR;
            case ASR:
                return AnalyticsConstants.LOCATION.SETTING_ICON_ASR;
            case MAGHRIB:
                return AnalyticsConstants.LOCATION.SETTING_ICON_MAGHRIB;
            case ISHA:
                return AnalyticsConstants.LOCATION.SETTING_ICON_ISHA;
            default:
                return AnalyticsConstants.LOCATION.SETTING_ICON_FAJR;
        }
    }

    public int getSettingSelecetedItem() {
        switch (PrayerTimesAtUtils.getAlarmStatus(getContext(), mode.getType())) {
            case Constants.NOTIFICATION_STATUS_MUTE:
                return 1;
            case Constants.NOTIFICATION_STATUS_SOUND_SYSTEM:
                return 2;
            case Constants.NOTIFICATION_STATUS_SOUND_1:
                return 3;
            case Constants.NOTIFICATION_STATUS_SOUND_2:
                return 4;
        }
        return 0;
    }
}

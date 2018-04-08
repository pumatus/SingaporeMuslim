package co.muslimummah.android.module.prayertime.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import co.muslimummah.android.DebugFileLog;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.AnalyticsConstants;
import co.muslimummah.android.analytics.LogObject;
import co.muslimummah.android.analytics.OracleAnalytics;
import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.data.model.PrayerTimeLocationInfo;
import co.muslimummah.android.module.prayertime.manager.NotificationHandlerManager;
import co.muslimummah.android.module.prayertime.manager.OracleAlarmManager;
import co.muslimummah.android.module.prayertime.manager.PrayerTimeManager;
import co.muslimummah.android.module.prayertime.utils.PrayerTimesAtUtils;
import co.muslimummah.android.module.prayertime.ui.fragment.PrayerTimeType;
import co.muslimummah.android.util.UiUtils;
import timber.log.Timber;

import static co.muslimummah.android.module.prayertime.manager.NotificationHandlerManager.INTENT_KEY_CLICK_ACTION_PRAYERTIME;

/**
 * Created by Hongd on 2017/8/5.
 */

public class PrayerTimesReceiver extends BroadcastReceiver {
    public final static String ACTION_ALARM = "co.muslimummah.android.prayer.notification";
    public final static String ACTION_NOTIFICATION_DELETE = "co.muslimummah.android.prayer.notification.delete";
    private final static String KEY_TYPE = "type";
    private final static String KEY_TIME = "time";
    private final static String KEY_TIME_LONG = "time_long";

    public static void setAlarm(Context context, PrayerTimeType timeType, String timeText, long time) {
//        AlarmBean bean = AppSession.getInstance(context).getCachedValue(Constants.SP_LAST_ALARM, AlarmBean.class);
//        if (bean != null) {
//            if (timeText.equals(bean.getTimeText()) && timeType.getNameText().equals(bean.getTypeName())) {
//                return;
//            }

        if (context == null) {
            context = OracleApp.getInstance();
        }

        //clear old alarm
        Intent intent = new Intent(context, PrayerTimesReceiver.class);
        intent.setAction(ACTION_ALARM);
        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        OracleAlarmManager.cancelAlarm(context, pendingIntent);
//            AppSession.getInstance(context).clearCacheValue(Constants.SP_LAST_ALARM);
//        }

        intent = new Intent(context, PrayerTimesReceiver.class);
        intent.setAction(ACTION_ALARM);
        intent.putExtra(KEY_TYPE, timeType.getNameText());
        intent.putExtra(KEY_TIME, timeText);
        intent.putExtra(KEY_TIME_LONG, time);
        pendingIntent = PendingIntent
                .getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        OracleAlarmManager.setAlarm(context, pendingIntent, time);

        DebugFileLog.INSTANCE.log("SET ALARM " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(time)));
//        AppSession.getInstance(context)
//            .cacheValue(Constants.SP_LAST_ALARM, new AlarmBean(timeType.getNameText(), timeText, time), true);
    }

//    @Data
//    @AllArgsConstructor
//    public static class AlarmBean implements Serializable {
//        String typeName;
//        String timeText;
//        long time;
//    }

    private String provideRealNameText(Context context, String textEn) {
        int id = 0;
        switch (textEn) {
            case "Fajr":
                id = R.string.fajr;
                break;
            case "Asr":
                id = R.string.asr;
                break;
            case "Maghrib":
                id = R.string.maghrib;
                break;
            case "Isha":
                id = R.string.isha;
                break;
            case "Dhuhr":
                id = R.string.dhuhr;
                break;
            case "Sunrise":
                id = R.string.sunrise;
                break;
            default:
                break;
        }
        if (id == 0) {
            return null;
        }
        return context.getString(id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ACTION_ALARM);
        wl.acquire(5000);

        Timber.d("PrayerTimesReceiver %s", intent.getAction());
//        AppSession appSession = AppSession
//                .getInstance(OracleApp.getInstance().getApplicationContext());

//        String location = appSession.getCachedValue(Constants.SP_LOCATION_CITY_VALUE, String.class);
        if (ACTION_ALARM.equals(intent.getAction())) {
            PrayerTimeLocationInfo locationInfo = PrayerTimeManager.instance().getSelectedLocationInfo();
            String location = locationInfo == null ? "" : locationInfo.getDisplayName();
            NotificationHandlerManager handlerManager;
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            long time = intent.getLongExtra(KEY_TIME_LONG, 0);
            DebugFileLog.INSTANCE.log("onBroadcastReceive ALARM " + formatter.format(new Date(time)));
            if (Math.abs(System.currentTimeMillis() - time) < 120000) {
                String timeText = intent.getStringExtra(KEY_TIME);
                PrayerTimeType type = PrayerTimeType.format(intent.getStringExtra(KEY_TYPE));

                int status = PrayerTimesAtUtils.getAlarmStatus(context, type);
                handlerManager = NotificationHandlerManager
                        .getInstance(OracleApp.getInstance().getApplicationContext());
                if (status == Constants.NOTIFICATION_STATUS_OFF) {//取消
//                handlerManager.cancelNotificationId();
                    DebugFileLog.INSTANCE.log("onBroadcastReceive OFF " + formatter.format(new Date(System.currentTimeMillis())));
                } else {
                    handlerManager.createSimpleNotification(OracleApp.getInstance().getApplicationContext(),
                            UiUtils.getText(R.string.prayer_time_notification, provideRealNameText(context,type.getNameText()), location, timeText),
                            type.getNameText(),
                            status);
                    DebugFileLog.INSTANCE.log("onBroadcastReceive SOUND " + status + " " + formatter.format(new Date(System.currentTimeMillis())));
                }
            } else {
                DebugFileLog.INSTANCE.log("onBroadcastReceive invalid current " + formatter.format(new Date(System.currentTimeMillis())));
            }

//        PrayerTimeLocationInfo locationInfo = AppSession.getInstance(context).getCachedValue(Constants.KEY_LAST_LOCATION_CITY, PrayerTimeLocationInfo.class);
            if (locationInfo != null) {
                PrayerTimesAtUtils.placeNextAlarm(context, locationInfo);
            }
        } else if (ACTION_NOTIFICATION_DELETE.equals(intent.getAction())) {
            if (intent.hasExtra(INTENT_KEY_CLICK_ACTION_PRAYERTIME)) {
                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.SWIPE)
                                .location(AnalyticsConstants.LOCATION.NOTIFICATION_PAGE_PRAYERTIMES)
                                .target(AnalyticsConstants.TARGET_TYPE.PRAYER_TIME_TYPE, intent.getStringExtra(INTENT_KEY_CLICK_ACTION_PRAYERTIME))
                                .build());
            }
        }
    }
}

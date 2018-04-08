package co.muslimummah.android.module.prayertime.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.util.Calendar;
import timber.log.Timber;

/**
 * Created by Hongd on 2017/8/5.
 */

public class OracleAlarmManager {

    public static void cancelAlarm(Context paramContext, PendingIntent paramPendingIntent) {
        try {
            ((AlarmManager) paramContext.getSystemService(Context.ALARM_SERVICE))
                .cancel(paramPendingIntent);
        } catch (Exception exp) {
            Timber.e(exp, "OracleAlarmManager.cancelAlarm");
        }
    }

    public static void setAlarm(Context paramContext, PendingIntent paramPendingIntent,
        long paramCalendar) {
        AlarmManager manager = (AlarmManager) paramContext.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            manager
                .setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, paramCalendar, paramPendingIntent);
            return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            manager.setExact(AlarmManager.RTC_WAKEUP, paramCalendar, paramPendingIntent);
            return;
        }
        manager.set(AlarmManager.RTC_WAKEUP, paramCalendar, paramPendingIntent);
    }

    public static void setRepeatingAlarm(Context paramContext, PendingIntent paramPendingIntent,
        long paramCalendar, long paramLong) {
        Log.i("setRepeatingAlarm   ", paramCalendar + " " + paramLong);
        try {
            ((AlarmManager) paramContext.getSystemService(Context.ALARM_SERVICE))
                .setRepeating(AlarmManager.RTC_WAKEUP, paramCalendar, paramLong,
                    paramPendingIntent);
        } catch (Exception exp) {
            Timber.e(exp, "OracleAlarmManager.setRepeatingAlarm");
        }
    }

    public static void setRepeatingAlarm(Context paramContext, PendingIntent paramPendingIntent,
        Calendar paramCalendar1, Calendar paramCalendar2) {
        try {
            ((AlarmManager) paramContext.getSystemService(Context.ALARM_SERVICE))
                .setRepeating(AlarmManager.RTC_WAKEUP, paramCalendar1.getTimeInMillis(),
                    paramCalendar2.getTimeInMillis(),
                    paramPendingIntent);
        } catch (Exception exp) {
            Timber.e(exp, "OracleAlarmManager.setRepeatingAlarm");
        }
    }
}

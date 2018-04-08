package co.muslimummah.android.module.prayertime.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.data.model.PrayerTimeLocationInfo;
import co.muslimummah.android.module.prayertime.utils.PrayerTimesAtUtils;
import co.muslimummah.android.storage.AppSession;
import timber.log.Timber;

import static co.muslimummah.android.module.prayertime.receiver.PrayerTimesReceiver.ACTION_ALARM;

/**
 * Created by Xingbo.Jie on 2/9/17.
 */

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intnt) {
        Timber.d("BootReceiver");
        AppSession.getInstance(context).clearCacheValue(Constants.SP_LAST_ALARM);
        PrayerTimeLocationInfo locationInfo = AppSession.getInstance(context).getCachedValue(Constants.KEY_LAST_LOCATION_CITY, PrayerTimeLocationInfo.class);
        if (locationInfo != null) {
            Timber.d("BootReceiver placeNextAlarm");
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, ACTION_ALARM);
            wl.acquire(5000);

            PrayerTimesAtUtils.placeNextAlarm(context, locationInfo);
        }
    }
}

package co.muslimummah.android.module.prayertime.manager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;

import co.muslimummah.android.LauncherActivity;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.receiver.PrayerTimesReceiver;
import co.muslimummah.android.util.SchemeUtils;

/**
 * Created by Hongd on 2017/8/5.
 */

public class NotificationHandlerManager {

    private static NotificationHandlerManager handlerManager;
    private static NotificationManager notificationManager;
    public static final String INTENT_KEY_CLICK_ACTION_PRAYERTIME = "INTENT_KEY_CLICK_ACTION_PRAYERTIME";

    private NotificationHandlerManager() {
    }

    /**
     * Singleton pattern implementation
     */
    public static synchronized NotificationHandlerManager getInstance(Context context) {
        if (handlerManager == null) {
            handlerManager = new NotificationHandlerManager();
            notificationManager = (NotificationManager) context.getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return handlerManager;
    }

//    public void cancelNotificationId() {
//        notificationManager.cancel(1001);
//    }

    public void createSimpleNotification(Context context, String timeContent, String prayerTimeName, int sound) {
//        Intent resultIntent = new Intent(Intent.ACTION_MAIN);
        Intent resultIntent = new Intent(context, LauncherActivity.class);
//        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//        resultIntent.setClass(context.getApplicationContext(), LauncherActivity.class);
        resultIntent.putExtra(SchemeUtils.EXTRA_URL, String.format("https://app.muslimummah.co/%s?calendar_expanded=0", SchemeUtils.PRAYER_TIMES));
//        resultIntent.setFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_NEW_TASK);
        resultIntent.putExtra(INTENT_KEY_CLICK_ACTION_PRAYERTIME, prayerTimeName);
        PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), 1001, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intent = new Intent(context, PrayerTimesReceiver.class);
        intent.setAction(PrayerTimesReceiver.ACTION_NOTIFICATION_DELETE);
        intent.putExtra(INTENT_KEY_CLICK_ACTION_PRAYERTIME, prayerTimeName);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Building the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher_notification_transparent)
                .setLargeIcon(BitmapFactory.decodeResource(OracleApp.getInstance().getResources(), R.mipmap.ic_launcher))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(timeContent)
                .setShowWhen(true)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setLights(0xff1B5E20, 500, 2000)
                .setDeleteIntent(deleteIntent);

        if (sound != Constants.NOTIFICATION_STATUS_MUTE) {
            switch (sound) {
                case Constants.NOTIFICATION_STATUS_SOUND_SYSTEM:
                    builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    break;
                case Constants.NOTIFICATION_STATUS_SOUND_1:
                    builder.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.normal));
                    break;
                case Constants.NOTIFICATION_STATUS_SOUND_2:
                    builder.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.soft));
                    break;
            }
        }
//        else {
//            AudioManager audioManager = (AudioManager) OracleApp.getInstance().getSystemService(Context
// .AUDIO_SERVICE);
//            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
//        }

//        NotificationCompat.InboxStyle inboxStyle = new InboxStyle();
//        inboxStyle.setBigContentTitle("");
//        inboxStyle.setSummaryText("");
//        builder.setStyle(inboxStyle);

        notificationManager.notify(1001, builder.build());
    }
}

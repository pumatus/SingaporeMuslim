package co.muslimummah.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Random;

import co.muslimummah.android.util.SchemeUtils;
import timber.log.Timber;

/**
 * Created by Xingbo.Jie on 26/9/17.
 */

public class OracleFirebaseMessagingService extends FirebaseMessagingService {
    public static final int FIREBASE_ID_START = 0x00020000;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getNotification() != null && remoteMessage.getData() != null) {
            Intent resultIntent = new Intent(this, LauncherActivity.class);
//            resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//            resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            resultIntent.setClass(OracleApp.getInstance(), LauncherActivity.class);

            // data is here
            resultIntent.putExtra(SchemeUtils.EXTRA_URL, remoteMessage.getData().get(SchemeUtils.EXTRA_URL));
            PendingIntent pendingIntent = PendingIntent.getActivity(OracleApp.getInstance(), FIREBASE_ID_START, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_launcher_notification_transparent)
                    .setLargeIcon(BitmapFactory.decodeResource(OracleApp.getInstance().getResources(), R.mipmap.ic_launcher))
                    .setContentTitle(remoteMessage.getNotification().getTitle())
                    .setContentText(remoteMessage.getNotification().getBody())
                    .setShowWhen(true)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(new Random().nextInt(10000) + FIREBASE_ID_START, builder.build());
        }
    }
}

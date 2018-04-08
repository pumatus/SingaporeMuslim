package co.muslimummah.android.player;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import java.util.Locale;

import co.muslimummah.android.R;
import co.muslimummah.android.module.quran.activity.BookmarkedVerseActivity;
import co.muslimummah.android.module.quran.activity.VerseActivity;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.util.wrapper.Wrapper3;

/**
 * Created by Xingbo.Jie on 7/8/17.
 */

public class MediaNotificationHelper {
    public static final String QURAN_PAUSE = "co.muslimummah.android.player.QURAN_PAUSE";
    public static final String QURAN_PLAY = "co.muslimummah.android.player.QURAN_PLAY";
    public static final String QURAN_STOP = "co.muslimummah.android.player.QURAN_STOP";
    public static final String QURAN_NEXT = "co.muslimummah.android.player.QURAN_NEXT";
    public static final String QURAN_PREVIOUS = "co.muslimummah.android.player.QURAN_PREVIOUS";
    public static final String QURAN_BOOKMARK = "co.muslimummah.android.player.QURAN_BOOKMARK";
    public static final String INTENT_KEY_CLICK_ACTION = "INTENT_KEY_CLICK_ACTION";

    public static final int NOTIFICATION_ID = 1002;

    public static Notification createNotification(Context context, @NonNull Wrapper3<String, Verse, MusicService.MediaForm> source, int state) {
        Verse verse = source.entity2;
        Chapter chapter = QuranRepository.INSTANCE.getChapter(verse.getChapterId()).blockingFirst();
        boolean isBookmark = source.entity3.equals(MusicService.MediaForm.BOOKMARK);
        String normalDescription = String.format(Locale.US, "%s %d/%d",context.getString(R.string.verse), Math.max(1, verse.getVerseId()), chapter.getVerseCount());

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_quran_audio_notification);

        remoteViews.setTextViewText(R.id.tv_title, isBookmark ? context.getString(R.string.bookmarks) : chapter.getTransliteration());
        remoteViews.setTextViewText(R.id.tv_content, isBookmark ? String.format(Locale.US, "%s (%d:%d)", chapter.getTransliteration(), Math.max(1, verse.getVerseId()), chapter.getVerseCount()) : normalDescription);

        if (state == PlaybackState.STATE_PLAYING) {
            remoteViews.setImageViewResource(R.id.ib_play, R.drawable.ic_btn_notification_pause);
            Intent intent = new Intent(QURAN_PAUSE);
            remoteViews.setOnClickPendingIntent(R.id.ib_play, PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            remoteViews.setImageViewResource(R.id.ib_play, R.drawable.ic_btn_notification_play);
            Intent intent = new Intent(QURAN_PLAY);
            remoteViews.setOnClickPendingIntent(R.id.ib_play, PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        }
        Intent stopIntent = new Intent(QURAN_STOP);
        remoteViews.setOnClickPendingIntent(R.id.ib_close, PendingIntent.getBroadcast(context, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Intent nextIntent = new Intent(QURAN_NEXT);
        remoteViews.setOnClickPendingIntent(R.id.ib_next, PendingIntent.getBroadcast(context, 4, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        Intent prevIntent = new Intent(QURAN_PREVIOUS);
        remoteViews.setOnClickPendingIntent(R.id.ib_prev, PendingIntent.getBroadcast(context, 5, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        notificationBuilder.setCustomContentView(remoteViews)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_notification_transparent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        Intent contentIntent;
        if (isBookmark) {
            contentIntent = BookmarkedVerseActivity.getStartIntent(context, verse);
        } else {
            contentIntent = VerseActivity.getStartIntent(context, QuranRepository.INSTANCE.getChapter(verse.getChapterId()).blockingFirst(), verse.getVerseId());
        }
        contentIntent.putExtra(INTENT_KEY_CLICK_ACTION, INTENT_KEY_CLICK_ACTION);
        notificationBuilder.setContentIntent(PendingIntent.getActivity(context, 7, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT));
        return notificationBuilder.build();
    }
}

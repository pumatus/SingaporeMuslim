package co.muslimummah.android.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

/**
 * only use for play local file
 * Created by Xingbo.Jie on 9/10/17.
 */
public class SimpleMediaPlayer {

    private static SimpleMediaPlayer INSTANCE = new SimpleMediaPlayer();


    private SimpleMediaPlayer() {
    }

    MediaPlayer player;

    public static void play(Context context, Uri uri) {
        playUriOrRes(context, uri, -1);
    }

    public static void play(Context context, int resId) {
        playUriOrRes(context, null, resId);
    }

    private static void playUriOrRes(Context context, Uri uri, int resId) {
        stop();

        if (uri == null && resId <= 0) {
            return;
        }

        try {
            if (null != uri) {
                INSTANCE.player = MediaPlayer.create(context, uri);
            } else {
                INSTANCE.player = MediaPlayer.create(context, resId);
            }
            
            INSTANCE.player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stop();
                }
            });

            INSTANCE.player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    stop();
                    return false;
                }
            });
            INSTANCE.player.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        try {
            if (INSTANCE.player != null) {
                if (INSTANCE.player.isPlaying()) {
                    INSTANCE.player.stop();
                }
                INSTANCE.player.release();
                INSTANCE.player = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

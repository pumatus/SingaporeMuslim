package co.muslimummah.android.player;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.module.quran.model.Verse;

/**
 * Created by frank on 9/1/17.
 */

public class MusicServiceLogDelegate {
    private ServiceConnection mServiceConnection;
    private MusicService.PlayerBinder mPlayerBinder;
    private Context mContext;

    public void bind(Context context) {
        if (mServiceConnection == null) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    mPlayerBinder = (MusicService.PlayerBinder) iBinder;
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            };
        }
        mContext = context;
        mContext.bindService(new Intent(mContext, MusicService.class), mServiceConnection, Service.BIND_AUTO_CREATE);
    }

    public void unBind() {
        mContext.unbindService(mServiceConnection);
        mServiceConnection = null;
        mPlayerBinder = null;
        mContext = null;
    }

    public void logEvent(String category, String label, Long value) {
        //Action depends on current play back status.
        if (mPlayerBinder != null) {
            if (mPlayerBinder.getCurrentSource() != null
                    && mPlayerBinder.getCurrentSource().entity1 != null
                    && (mPlayerBinder.getPlayState() == PlaybackState.STATE_PLAYING
                    || mPlayerBinder.getPlayState() == PlaybackState.STATE_PAUSED
                    || mPlayerBinder.getPlayState() == PlaybackState.STATE_BUFFERING)) {
                //Audio Playing.
                ThirdPartyAnalytics.INSTANCE.logEvent(category, "AudioPlaying", label, value);
            } else {
                ThirdPartyAnalytics.INSTANCE.logEvent(category, "Reading", label, value);
            }
        }
    }

    public void logChapterItemClickEvent(long chapterId) {
        //Action depends on current play back status.
        if (mPlayerBinder != null) {
            if (mPlayerBinder.getCurrentSource() != null
                    && mPlayerBinder.getCurrentSource().entity1 != null
                    && (mPlayerBinder.getPlayState() == PlaybackState.STATE_PLAYING
                    || mPlayerBinder.getPlayState() == PlaybackState.STATE_PAUSED
                    || mPlayerBinder.getPlayState() == PlaybackState.STATE_BUFFERING)) {
                //Audio Playing.
                Verse verse = mPlayerBinder.getCurrentSource().entity1;
                if (verse.getChapterId() == chapterId) {
                    ThirdPartyAnalytics.INSTANCE.logEvent("QuranChapterView", "AudioPlaying", "SameChapter", null);
                } else {
                    ThirdPartyAnalytics.INSTANCE.logEvent("QuranChapterView", "AudioPlaying", "DiffChapter", null);
                }
            } else {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranChapterView", "Reading", String.valueOf(chapterId), null);
            }
        }
    }
}

package co.muslimummah.android.player;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.danikula.videocache.HttpProxyCacheServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.muslimummah.android.event.Quran;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.AnalyticsConstants;
import co.muslimummah.android.analytics.LogObject;
import co.muslimummah.android.analytics.OracleAnalytics;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.TranslationWord;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.module.quran.model.repository.VerseMp3Repo;
import co.muslimummah.android.util.AudioUtils;
import co.muslimummah.android.util.ToastUtil;
import co.muslimummah.android.util.Utils;
import co.muslimummah.android.util.wrapper.Wrapper2;
import co.muslimummah.android.util.wrapper.Wrapper3;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static co.muslimummah.android.player.MediaNotificationHelper.QURAN_PLAY;

/**
 * Created by Xingbo.Jie on 5/8/17.
 */

public class MusicService extends Service {
    private static final String TAG = "MusicService";

    // Delay stopSelf by using a handler.
    private static final long STOP_DELAY = TimeUnit.SECONDS.toMillis(30);
    private static final long UPDATE_PROGRESS_DELAY = 50;
    private static final int STOP_CMD = 0x7c48;
    private static final int UPDATE_PROGRESS_CMD = STOP_CMD + 1;
    private static final int STOP_UPDATE_PROGRESS_CMD = UPDATE_PROGRESS_CMD + 1;

    public NotificationManagerCompat notificationManager;

    // Indicates whether the service was started.
    private boolean serviceStarted;
    private Playback playback;
    private Playback wordPlayback;
    private AudioBecomingNoisyReceiver audioBecomingNoisyReceiver;
    private NotificationActionReceiver notificationActionReceiver;
    private Wrapper3<String, Verse, MediaForm> currentSource;
    private final List<OnPlayProgressUpdateListener> onPlayProgressUpdateListeners = new ArrayList<>();
    private final List<OnPlayStateChangeListener> onPlayStateChangeListeners = new ArrayList<>();

    private final PlayerBinder binder = new PlayerBinder();
    private Disposable mediaSwitchDisposable;

    private Playback.Source verseSource = new Playback.Source() {
        @Override
        public void setDataSource(MediaPlayer player) throws IOException {
            if (player != null && currentSource != null && currentSource.entity1 != null) {
                Timber.d("%s play verse setDataSource", TAG);
                player.setDataSource(VerseMp3Repo.INSTANCE.getFileInputStream(currentSource.entity1).getFD());
            }
        }
    };


    public enum MediaForm {
        NORMAL_VERSE,
        BOOKMARK
    }

    private Handler progressHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg == null || (msg.what != UPDATE_PROGRESS_CMD && msg.what != STOP_UPDATE_PROGRESS_CMD)) {
                return false;
            }

            if (msg.what == UPDATE_PROGRESS_CMD) {
                if (playback != null && currentSource != null) {
                    for (OnPlayProgressUpdateListener onPlayProgressUpdateListener : onPlayProgressUpdateListeners) {
                        onPlayProgressUpdateListener.onProgressUpdate(playback.getCurrentStreamPosition(), currentSource.entity2, currentSource.entity3);
                    }
                }
                progressHandler.removeCallbacksAndMessages(null);
                progressHandler.sendEmptyMessageDelayed(UPDATE_PROGRESS_CMD, UPDATE_PROGRESS_DELAY);
            } else if (msg.what == STOP_UPDATE_PROGRESS_CMD) {
                progressHandler.removeCallbacksAndMessages(null);
            }


            return false;
        }
    });

    public interface OnPlayStateChangeListener {
        /**
         * @param state like {@link PlaybackState#STATE_PLAYING}
         */
        void onPlayStateChanged(int state, Verse Verse);

        void onBroadcastReceive(Context context, Intent intent);
    }


    public interface OnPlayProgressUpdateListener {
        void onProgressUpdate(int progress, Verse object, MediaForm form);

        void onNewChapter(long oldChapterId, Chapter newChapter, Verse newVerse);
    }


    private volatile boolean isPlayingWord;
    // if it is playing when play word mp3,in order to not auto play when it is paused before.
    private volatile boolean isAlreadyPlayVerse = true;

    public class PlayerBinder extends Binder {
        public void play(String audioCacheKey, Verse object, MediaForm form) {

            isPlayingWord = false;
            if (wordPlayback.isPlaying()) {
                wordPlayback.stop();
            }

            currentSource = new Wrapper3<>(audioCacheKey, object, form);
            handlePlayRequest();
        }

        // word by word
        public void playWord(TranslationWord word) {
//            if (isPlayingWord) return;

            isAlreadyPlayVerse = isPlaying();
            if (isAlreadyPlayVerse) {
                pause();
            }

            if (wordPlayback.isPlaying()) {
                wordPlayback.stop();
            }

            isPlayingWord = true;

            //download and cache
            HttpProxyCacheServer proxy = AudioUtils.getProxy();
            String mp3Url = AudioUtils.shortToFullUrl(word.getCompressedMp3());
            String url = null;

            try {
                if (Utils.isNetworkAvailable() || proxy.isCached(mp3Url)) {
                    url = proxy.getProxyUrl(mp3Url);
                } else {
                    throw new Exception();
                }
            } catch (Exception e) {
                ToastUtil.show(getString(R.string.download_failed));
                isPlayingWord = false;
                if (isAlreadyPlayVerse) {
                    handlePlayRequest();
                }
            }
            if (wordPlayback != null && !TextUtils.isEmpty(url)) {
                final String urlTemp = url;
                Timber.d("%s play word", TAG);
                wordPlayback.play(new Playback.Source() {
                    @Override
                    public void setDataSource(MediaPlayer player) throws IOException {
                        Timber.d("%s play word setDataSource", TAG);
                        player.setDataSource(urlTemp);
                    }
                }, url);
            }
        }

        public void stop() {
            currentSource = null;
            handleStopRequest();
        }

        public void pause() {
            handlePauseRequest();
        }

        public void next() {
            stopSwitchMedia();
            handleNextRequest();
        }

        public void previous() {
            stopSwitchMedia();
            handlePreviousRequest();
        }

        public void addOnPlayStateChangeListener(OnPlayStateChangeListener listener) {
            if (listener == null) {
                return;
            }
            MusicService.this.onPlayStateChangeListeners.add(listener);
        }

        public void removeOnPlayStateChangeListener(OnPlayStateChangeListener listener) {
            if (listener == null) {
                return;
            }
            MusicService.this.onPlayStateChangeListeners.remove(listener);
        }

        public void addOnPlayProgressUpdateListener(OnPlayProgressUpdateListener listener) {
            if (listener == null) {
                return;
            }
            MusicService.this.onPlayProgressUpdateListeners.add(listener);
        }

        public void removeOnPlayProgressUpdateListener(OnPlayProgressUpdateListener listener) {
            if (listener == null) {
                return;
            }
            MusicService.this.onPlayProgressUpdateListeners.remove(listener);
        }

        public Wrapper2<Verse, MediaForm> getCurrentSource() {
            return currentSource != null ? new Wrapper2(currentSource.entity2, currentSource.entity3) : null;
        }

        public int getPlayState() {
            return playback == null ? PlaybackState.STATE_NONE : playback.getState();
        }

        public boolean isPlaying() {
            return playback != null && playback.isPlaying();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Timber.d("onBind");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.d("onUnbind");
        clearListener();
        return super.onUnbind(intent);
    }

    private void clearListener() {
        onPlayProgressUpdateListeners.clear();
        onPlayStateChangeListeners.clear();
    }

    private void handleNextRequest() {
        if (playback != null) {
            playback.stop();
        }
        if (currentSource != null && currentSource.entity2 != null) {
            // FIXME: 21/8/17 moveBy this logic to Media provider
            asyncPlayImpl(getNextVerse(currentSource.entity2, currentSource.entity3), currentSource.entity3);
        }
    }

    private void handlePreviousRequest() {
        if (playback != null) {
            playback.stop();
        }
        if (currentSource != null && currentSource.entity2 != null) {
            // FIXME: 21/8/17 moveBy this logic to Media provider
            asyncPlayImpl(getPreviousVerse(currentSource.entity2, currentSource.entity3), currentSource.entity3);
        }
    }

    private void asyncPlayImpl(Observable<Verse> source, final MediaForm form) {
        mediaSwitchDisposable = source
                .flatMap(new Function<Verse, ObservableSource<Wrapper2<String, Verse>>>() {
                    @Override
                    public ObservableSource<Wrapper2<String, Verse>> apply(@NonNull Verse verse) throws Exception {
                        return Observable.zip(
                                QuranRepository.INSTANCE.getVerseAudioCacheKey(verse.getChapterId(), verse.getVerseId())
                                        .subscribeOn(Schedulers.io()), Observable.just(verse),
                                new BiFunction<String, Verse, Wrapper2<String, Verse>>() {
                                    @Override
                                    public Wrapper2<String, Verse> apply(@NonNull String mp3CacheKey, @NonNull Verse verse) throws Exception {
                                        return new Wrapper2<>(mp3CacheKey, verse);
                                    }
                                }).subscribeOn(Schedulers.io());
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Wrapper2<String, Verse>>() {
                    @Override
                    public void accept(@NonNull Wrapper2<String, Verse> wrapper2) throws Exception {
                        currentSource = new Wrapper3<>(wrapper2.entity1, wrapper2.entity2, form);
                        handlePlayRequest();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        //Get next verse error, we stop the play.
                        currentSource = null;
                        handleStopRequest();
                    }
                });
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("onCreate");
        EventBus.getDefault().register(this);
        playback = new Playback(this);
        playback.setCallback(new Playback.Callback() {
            @Override
            public void onPlaybackStatusChanged(int state) {
                for (OnPlayStateChangeListener onPlayStateChangeListener : onPlayStateChangeListeners) {
                    onPlayStateChangeListener.onPlayStateChanged(state, currentSource != null ? currentSource.entity2 : null);
                }
                updatePlaybackState(null);
                Timber.d("onPlaybackStatusChanged %d", state);
            }

            @Override
            public void onCompletion() {
                handleStopRequest();
                handleNextRequest();
//                doOnComplete();
            }

            @Override
            public void onError(String error) {
                isPlayingWord = false;

                updatePlaybackState(error);
            }
        });

        wordPlayback = new Playback(this);
        wordPlayback.setCallback(new Playback.Callback() {
            @Override
            public void onCompletion() {
                isPlayingWord = false;
                wordPlayback.stop();
                if (isAlreadyPlayVerse) {
                    handlePlayRequest();
                }
            }

            @Override
            public void onPlaybackStatusChanged(int state) {

            }

            @Override
            public void onError(String error) {
                isPlayingWord = false;
                wordPlayback.stop();
                if (isAlreadyPlayVerse) {
                    handlePlayRequest();
                }
            }
        });


        notificationManager = NotificationManagerCompat.from(this);
        audioBecomingNoisyReceiver = new AudioBecomingNoisyReceiver(this);
        notificationActionReceiver = new NotificationActionReceiver(this);
        notificationActionReceiver.register();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationActionReceiver.unregister();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onVerseDownloading(Quran.StartDownloadVerse event) {
        if (playback != null) {
            playback.stop();
        }
        stopForeground(true);
    }

    // FIXME: 21/8/17 moveBy this logic to Media provider
    private Observable<Verse> getNextVerse(final Verse currentVerse, MediaForm form) {
        if (form == MediaForm.NORMAL_VERSE) {
            QuranRepository.INSTANCE.getNormalNextVerseWithoutAudioResource(currentVerse.getChapterId(), currentVerse.getVerseId())
                    .filter(new Predicate<Verse>() {
                        @Override
                        public boolean test(@NonNull Verse verse) throws Exception {
                            return verse.getChapterId() > currentVerse.getChapterId();
                        }
                    })
                    .flatMap(new Function<Verse, ObservableSource<Pair<Chapter, Verse>>>() {
                        @Override
                        public ObservableSource<Pair<Chapter, Verse>> apply(@NonNull Verse verse) throws Exception {
                            return Observable.zip(QuranRepository.INSTANCE.getChapter(verse.getChapterId()),
                                    Observable.just(verse),
                                    new BiFunction<Chapter, Verse, Pair<Chapter, Verse>>() {
                                        @Override
                                        public Pair<Chapter, Verse> apply(@NonNull Chapter chapter, @NonNull Verse verse) throws Exception {
                                            return new Pair<>(chapter, verse);
                                        }
                                    });
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Pair<Chapter, Verse>>() {
                        @Override
                        public void accept(@NonNull Pair<Chapter, Verse> chapterVersePair) throws Exception {
                            for (OnPlayProgressUpdateListener onPlayProgressUpdateListener : onPlayProgressUpdateListeners) {
                                onPlayProgressUpdateListener.onNewChapter(currentVerse.getChapterId(), chapterVersePair.first, chapterVersePair.second);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@NonNull Throwable throwable) throws Exception {
                            //There is no next chapter.
                        }
                    });
            return QuranRepository.INSTANCE
                    .getNormalNextVerseWithAudioResource(currentVerse.getChapterId(), currentVerse.getVerseId());
        } else {
            return QuranRepository.INSTANCE
                    .getBookmarkedNextVerseWithAudioResource(currentVerse.getChapterId(), currentVerse.getVerseId());
        }
    }

    private Observable<Verse> getPreviousVerse(final Verse currentVerse, MediaForm form) {
        if (form == MediaForm.NORMAL_VERSE) {
            if (currentVerse.getVerseId() <= 1) {
                //We should play the next chapter if exist.
                QuranRepository.INSTANCE.getNormalPreviousVerseWithoutAudioResource(currentVerse.getChapterId(), currentVerse.getVerseId())
                        .filter(new Predicate<Verse>() {
                            @Override
                            public boolean test(@NonNull Verse verse) throws Exception {
                                return verse.getChapterId() < currentVerse.getChapterId();
                            }
                        })
                        .flatMap(new Function<Verse, ObservableSource<Pair<Chapter, Verse>>>() {
                            @Override
                            public ObservableSource<Pair<Chapter, Verse>> apply(@NonNull Verse verse) throws Exception {
                                return Observable.zip(QuranRepository.INSTANCE.getChapter(verse.getChapterId()),
                                        Observable.just(verse),
                                        new BiFunction<Chapter, Verse, Pair<Chapter, Verse>>() {
                                            @Override
                                            public Pair<Chapter, Verse> apply(@NonNull Chapter chapter, @NonNull Verse verse) throws Exception {
                                                return new Pair<>(chapter, verse);
                                            }
                                        });
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Pair<Chapter, Verse>>() {
                            @Override
                            public void accept(@NonNull Pair<Chapter, Verse> chapterVersePair) throws Exception {
                                for (OnPlayProgressUpdateListener onPlayProgressUpdateListener : onPlayProgressUpdateListeners) {
                                    onPlayProgressUpdateListener.onNewChapter(currentVerse.getChapterId(), chapterVersePair.first, chapterVersePair.second);
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                //There is no previous chapter.
                            }
                        });
            }
            return QuranRepository.INSTANCE
                    .getNormalPreviousVerseWithAudioResource(currentVerse.getChapterId(), currentVerse.getVerseId());
        } else {
            return QuranRepository.INSTANCE
                    .getBookmarkedPreviousVerseWithAudioResource(currentVerse.getChapterId(), currentVerse.getVerseId());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.d("onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    private void updatePlaybackState(String error) {
        Timber.d("updatePlaybackState, playback state=" + playback.getState());

        int state = playback.getState();

        if (state == PlaybackState.STATE_PLAYING) {
            startUpdateProgress();
            Notification notification = postNotification();
            startForeground(MediaNotificationHelper.NOTIFICATION_ID, notification);
            audioBecomingNoisyReceiver.register();
        } else {
            stopUpdateProgress();
            if (state == PlaybackState.STATE_PAUSED) {
                postNotification();
            } else {
                if (currentSource == null) {
                    stopForeground(true);
                }
            }
            audioBecomingNoisyReceiver.unregister();
        }
    }

    private Notification postNotification() {
        if (currentSource == null) {
            return null;
        }

        Notification notification = MediaNotificationHelper.createNotification(this, currentSource, playback.getState());
        if (notification == null) {
            return null;
        }

        notificationManager.notify(MediaNotificationHelper.NOTIFICATION_ID, notification);
        return notification;
    }

    /**
     * Handle a request to lay music
     */
    private void handlePlayRequest() {
        Timber.d("handlePlayRequest: mState=" + playback.getState());
        stopUpdateProgress();
        stopSwitchMedia();

        if (currentSource == null || currentSource.entity1 == null) return;

        OracleAnalytics.INSTANCE
                .addLog(LogObject.newBuilder()
                        .behaviour(AnalyticsConstants.BEHAVIOUR.PLAY)
                        .target(AnalyticsConstants.TARGET_TYPE.VERSE_ID, currentSource.entity2.getChapterId() + ":" + currentSource.entity2.getVerseId())
                        .build());

        delayedStopHandler.removeCallbacksAndMessages(null);
        if (!serviceStarted) {
            Log.v(TAG, "Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), MusicService.class));
            serviceStarted = true;
        }

        Timber.d("%s play verse", TAG);
        playback.play(verseSource, currentSource.entity2);
    }

    private void stopSwitchMedia() {
        if (mediaSwitchDisposable != null && !mediaSwitchDisposable.isDisposed()) {
            mediaSwitchDisposable.dispose();
            mediaSwitchDisposable = null;
        }
    }

    /**
     * Handle a request to pause music
     */
    private void handlePauseRequest() {
        Timber.d("handlePauseRequest: mState=" + playback.getState());
        playback.pause();

        // reset the delayed stop handler.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessageDelayed(STOP_CMD, STOP_DELAY);
    }

    /**
     * Handle a request to stop music
     */
    private void handleStopRequest() {
        Timber.d("handleStopRequest: mState=" + playback.getState());
        stopUpdateProgress();
        stopSwitchMedia();
        playback.stop();
        // reset the delayed stop handler.
        delayedStopHandler.removeCallbacksAndMessages(null);
        delayedStopHandler.sendEmptyMessage(STOP_CMD);

        updatePlaybackState(null);
    }

    private void handleBookmarkRequest() {
        if (currentSource != null && currentSource.entity2 != null) {
            boolean shouldBookmark = !currentSource.entity2.getIsBookMarked();
            ThirdPartyAnalytics.INSTANCE.logEvent("QuranNotification", "Click", shouldBookmark ? "Bookmark" : "Unbookmark", null);
            QuranRepository.INSTANCE.bookMarkVerse(currentSource.entity2, shouldBookmark);
            postNotification();
            //If current state is paused, the progress handler will not update progress so we force to update it.
            if (playback != null && playback.getState() == PlaybackState.STATE_PAUSED) {
                for (OnPlayProgressUpdateListener onPlayProgressUpdateListener : onPlayProgressUpdateListeners) {
                    onPlayProgressUpdateListener.onProgressUpdate(playback.getCurrentStreamPosition(), currentSource.entity2, currentSource.entity3);
                }
            }
        }
    }

    /**
     * Implementation of the AudioManager.ACTION_AUDIO_BECOMING_NOISY Receiver.
     */
    private class AudioBecomingNoisyReceiver extends BroadcastReceiver {
        private final Context mContext;
        private boolean mIsRegistered = false;

        private IntentFilter mAudioNoisyIntentFilter =
                new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        protected AudioBecomingNoisyReceiver(Context context) {
            mContext = context.getApplicationContext();
        }

        public void register() {
            if (!mIsRegistered) {
                mContext.registerReceiver(this, mAudioNoisyIntentFilter);
                mIsRegistered = true;
            }
        }

        public void unregister() {
            if (mIsRegistered) {
                mContext.unregisterReceiver(this);
                mIsRegistered = false;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                handlePauseRequest();
            }
        }
    }


    private class NotificationActionReceiver extends BroadcastReceiver {
        private final Context mContext;
        private boolean mIsRegistered = false;

        private IntentFilter mPlayIntentFilter =
                new IntentFilter(QURAN_PLAY);
        private IntentFilter mPauseIntentFilter =
                new IntentFilter(MediaNotificationHelper.QURAN_PAUSE);
        private IntentFilter mStopIntentFilter =
                new IntentFilter(MediaNotificationHelper.QURAN_STOP);
        private IntentFilter mNextIntentFilter =
                new IntentFilter(MediaNotificationHelper.QURAN_NEXT);
        private IntentFilter mPreviousIntentFilter =
                new IntentFilter(MediaNotificationHelper.QURAN_PREVIOUS);
        private IntentFilter mBookmarkIntentFilter =
                new IntentFilter(MediaNotificationHelper.QURAN_BOOKMARK);

        public NotificationActionReceiver(Context context) {
            this.mContext = context.getApplicationContext();
        }

        public void register() {
            if (!mIsRegistered) {
                mContext.registerReceiver(this, mPlayIntentFilter);
                mContext.registerReceiver(this, mPauseIntentFilter);
                mContext.registerReceiver(this, mStopIntentFilter);
                mContext.registerReceiver(this, mNextIntentFilter);
                mContext.registerReceiver(this, mPreviousIntentFilter);
                mContext.registerReceiver(this, mBookmarkIntentFilter);
                mIsRegistered = true;
            }
        }

        public void unregister() {
            if (mIsRegistered) {
                mContext.unregisterReceiver(this);
                mIsRegistered = false;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            for (OnPlayStateChangeListener listener :
                    onPlayStateChangeListeners) {
                listener.onBroadcastReceive(context, intent);
            }
            if (intent.getAction().equals(MediaNotificationHelper.QURAN_PLAY)) {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranNotification", "Click", "Play", null);
                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.NOTIFICATION_BAR_PLAY)
                                .build());

                handlePlayRequest();
            } else if (intent.getAction().equals(MediaNotificationHelper.QURAN_PAUSE)) {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranNotification", "Click", "Pause", null);
                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.NOTIFICATION_BAR_PAUSE)
                                .build());

                handlePauseRequest();
            } else if (intent.getAction().equals(MediaNotificationHelper.QURAN_STOP)) {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranNotification", "Click", "Close", null);
                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.NOTIFICATION_BAR_STOP)
                                .build());

                currentSource = null;
                handleStopRequest();
            } else if (intent.getAction().equals(MediaNotificationHelper.QURAN_NEXT)) {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranNotification", "Click", "Next", null);
                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.NOTIFICATION_BAR_NEXT)
                                .build());

                stopSwitchMedia();
                handleNextRequest();
            } else if (intent.getAction().equals(MediaNotificationHelper.QURAN_PREVIOUS)) {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranNotification", "Click", "Previous", null);
                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.NOTIFICATION_BAR_PREVIOUS)
                                .build());
                stopSwitchMedia();
                handlePreviousRequest();
            } else if (intent.getAction().equals(MediaNotificationHelper.QURAN_BOOKMARK)) {

                if (currentSource != null && currentSource.entity2 != null) {
                    OracleAnalytics.INSTANCE
                            .addLog(LogObject.newBuilder()
                                    .behaviour(currentSource.entity2.getIsBookMarked() ? AnalyticsConstants.BEHAVIOUR.UNBOOKMARK : AnalyticsConstants.BEHAVIOUR.BOOKMARK)
                                    .location(AnalyticsConstants.LOCATION.NOTIFICATION_BAR_PLAYER)
                                    .build());
                }

                handleBookmarkRequest();
            }
        }
    }

    /**
     * Custom {@link Handler} to process the delayed stop command.
     */
    private Handler delayedStopHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg == null || msg.what != STOP_CMD) {
                return false;
            }

//            if (!playback.isPlaying()) {
//                Timber.d("Stopping service");
//                stopSelf();
//                serviceStarted = false;
//            }
            return false;
        }
    });

    private void startUpdateProgress() {
        progressHandler.removeCallbacksAndMessages(null);
        progressHandler.sendEmptyMessage(UPDATE_PROGRESS_CMD);
    }

    private void stopUpdateProgress() {
        progressHandler.removeCallbacksAndMessages(null);
        progressHandler.sendEmptyMessage(STOP_UPDATE_PROGRESS_CMD);
    }
}

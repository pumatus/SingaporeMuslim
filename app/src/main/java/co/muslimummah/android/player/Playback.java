package co.muslimummah.android.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.support.v4.media.session.PlaybackStateCompat;

import java.io.IOException;

import timber.log.Timber;

/**
 * Created by Xingbo.Jie on 7/8/17.
 */

class Playback implements AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener {
    // The volume we set the media player to when we lose audio focus, but are
    // allowed to reduce the volume instead of stopping playback.
    public static final float VOLUME_DUCK = 0.2f;
    // The volume we set the media player when we have audio focus.
    public static final float VOLUME_NORMAL = 1.0f;

    // we don't have audio focus, and can't duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    // we don't have focus, but can duck (play at a low volume)
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    // we have full audio focus
    private static final int AUDIO_FOCUSED = 2;

    private final static String TAG = "Playback";

    private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private final Context mContext;
    private AudioManager audioManager;
    private MediaPlayer mediaPlayer;

    private int state = PlaybackState.STATE_NONE;
    private boolean playOnFocusGain;
    private Callback callback;
    private volatile int currentPosition;
    private volatile Object currentKey;

    Playback(Context context) {
        this.mContext = context.getApplicationContext();
        this.audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        this.state = PlaybackState.STATE_NONE;

//        // Create the Wifi lock (this does not acquire the lock, this just creates it).
//        this.mWifiLock = ((WifiManager) mContext.getSystemService(Context.WIFI_SERVICE))
//                .createWifiLock(WifiManager.WIFI_MODE_FULL, "sample_lock");
    }


    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of {@link android.media.AudioManager.OnAudioFocusChangeListener}.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Timber.d("onAudioFocusChange. focusChange=" + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            audioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            audioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player by calling configMediaPlayerState
            // with mAudioFocus properly set.
            if (state == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information that
                // we were playing, so that we can resume playback once we get the focus back.
                playOnFocusGain = true;
            }
        } else {
            Timber.d("onAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
        }
        configMediaPlayerState();
    }

    /**
     * Called when MediaPlayer has completed a seek.
     *
     * @see android.media.MediaPlayer.OnSeekCompleteListener
     */
    @Override
    public void onSeekComplete(MediaPlayer player) {
        Timber.d("onSeekComplete from MediaPlayer:" + player.getCurrentPosition());
        currentPosition = player.getCurrentPosition();
        if (state == PlaybackStateCompat.STATE_BUFFERING) {
            mediaPlayer.start();
            state = PlaybackStateCompat.STATE_PLAYING;
        }
        if (callback != null) {
            callback.onPlaybackStatusChanged(state);
        }
    }

    /**
     * Called when media player is done playing current song.
     *
     * @see android.media.MediaPlayer.OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer player) {
        Timber.d("onCompletion from MediaPlayer");
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (callback != null) {
            callback.onCompletion();
        }
    }

    /**
     * Called when media player is done preparing.
     *
     * @see android.media.MediaPlayer.OnPreparedListener
     */
    @Override
    public void onPrepared(MediaPlayer player) {
        Timber.d("onPrepared from MediaPlayer");
        // The media player is done preparing. That means we can start playing if we
        // have audio focus.
        configMediaPlayerState();
    }

    /**
     * Called when there's an error playing media. When this happens, the media
     * player goes to the Error state. We warn the user about the error and
     * reset the media player.
     *
     * @see android.media.MediaPlayer.OnErrorListener
     */
    @Override
    public boolean onError(MediaPlayer player, int what, int extra) {
        Timber.e("Media player error: what=" + what + ", extra=" + extra);
        if (callback != null) {
            callback.onError("MediaPlayer error " + what + " (" + extra + ")");
        }
        return true; // true indicates we handled the error
    }

    interface Callback {
        /**
         * On current music completed.
         */
        void onCompletion();

        /**
         * on Playback status changed
         * Implementations can use this callback to update
         * playback state on the media sessions.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * @param error to be added to the PlaybackState
         */
        void onError(String error);

    }

    interface Source {
        void setDataSource(MediaPlayer player) throws IOException;
    }


    public int getState() {
        return state;
    }

    public boolean isPlaying() {
        return playOnFocusGain || (mediaPlayer != null && mediaPlayer.isPlaying());
    }

    public void play(Source source, Object key) {
        if (source == null) {
            return;
        }

        playOnFocusGain = true;
        tryToGetAudioFocus();

        boolean mediaHasChanged = currentKey == null || !currentKey.equals(key);
        if (mediaHasChanged) {
            currentPosition = 0;
            currentKey = key;
        }

        if (state == PlaybackState.STATE_PAUSED
                && !mediaHasChanged && mediaPlayer != null) {
            configMediaPlayerState();
        } else {
            state = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false); // release everything except MediaPlayer

            try {
                createMediaPlayerIfNeeded();

                state = PlaybackStateCompat.STATE_BUFFERING;

                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                source.setDataSource(mediaPlayer);
//                mediaPlayer.setDataSource(VerseMp3Repo.INSTANCE.getFileInputStream(mp3CacheKey).getFD());

                // Starts preparing the media player in the background. When
                // it's done, it will call our OnPreparedListener (that is,
                // the onPrepared() method on this class, since we set the
                // listener to 'this'). Until the media player is prepared,
                // we *cannot* call start() on it!
                mediaPlayer.prepareAsync();

//                // If we are streaming from the internet, we want to hold a
//                // Wifi lock, which prevents the Wifi radio from going to
//                // sleep while the song is playing.
//                mWifiLock.acquire();

                if (callback != null) {
                    callback.onPlaybackStatusChanged(state);
                }

            } catch (IOException ioException) {
                Timber.e(ioException, "Exception playing song");
                if (callback != null) {
                    callback.onError(ioException.getMessage());
                }
            }
        }
    }

    public void stop() {
        state = PlaybackStateCompat.STATE_STOPPED;
        if (callback != null) {
            callback.onPlaybackStatusChanged(state);
        }
        

        currentPosition = 0;
        // Give up Audio focus
        giveUpAudioFocus();
        // Relax all resources
        relaxResources(true);
    }

    public void pause() {
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media player and cancel the 'foreground service' state.
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                currentPosition = mediaPlayer.getCurrentPosition();
            }
            // while paused, retain the MediaPlayer but give up audio focus
            relaxResources(false);
        }
        state = PlaybackStateCompat.STATE_PAUSED;
        if (callback != null) {
            callback.onPlaybackStatusChanged(state);
        }
    }

    public void seekTo(int position) {
        Timber.d("seekTo called with " + position);

        if (mediaPlayer == null) {
            // If we do not have a current media player, simply update the current position.
            currentPosition = position;
        } else {
            if (mediaPlayer.isPlaying()) {
                state = PlaybackStateCompat.STATE_BUFFERING;
            }
            mediaPlayer.seekTo(position);
            if (callback != null) {
                callback.onPlaybackStatusChanged(state);
            }
        }
    }

    public int getCurrentStreamPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : currentPosition;
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        Timber.d("tryToGetAudioFocus");
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        audioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                ? AUDIO_FOCUSED : AUDIO_NO_FOCUS_NO_DUCK;
    }

    /**
     * Give up the audio focus.
     */
    private void giveUpAudioFocus() {
        Timber.d("giveUpAudioFocus");
        if (audioManager.abandonAudioFocus(this) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mPlayer !=
     * null, so if you are calling it, you have to do so from a mContext where
     * you are sure this is the case.
     */
    private void configMediaPlayerState() {
        Timber.d("configMediaPlayerState. mAudioFocus=" + audioFocus);
        if (audioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause,
            if (state == PlaybackState.STATE_PLAYING) {
                pause();
            }
        } else {  // we have audio focus:
            if (audioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                mediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK); // we'll be relatively quiet
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL); // we can be loud again
                } // else do something for remote client.
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    Timber.d("configMediaPlayerState startMediaPlayer. seeking to "
                            + currentPosition);
                    if (currentPosition == mediaPlayer.getCurrentPosition()) {
                        mediaPlayer.start();
                        state = PlaybackState.STATE_PLAYING;
                    } else {
                        mediaPlayer.seekTo(currentPosition);
                        state = PlaybackState.STATE_BUFFERING;
                    }
                }
                playOnFocusGain = false;
            }
        }
        if (callback != null) {
            callback.onPlaybackStatusChanged(state);
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     *                           be released or not.
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        Timber.d("relaxResources. releaseMediaPlayer=" + releaseMediaPlayer);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }

//        // we can also release the Wifi lock, if we're holding it
//        if (mWifiLock.isHeld()) {
//            mWifiLock.release();
//        }
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {
        Timber.d("createMediaPlayerIfNeeded. needed? " + (mediaPlayer == null));
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while
            // playing. If we don't do that, the CPU might go to sleep while the
            // song is playing, causing playback to stop.
            mediaPlayer.setWakeMode(mContext.getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing,
            // and when it's done playing:
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mediaPlayer.reset();
        }
    }
}

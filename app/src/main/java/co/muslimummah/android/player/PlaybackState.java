package co.muslimummah.android.player;

import android.support.v4.media.session.PlaybackStateCompat;

/**
 * Created by Xingbo.Jie on 7/8/17.
 */

public interface PlaybackState {
    /**
     * This is the default playback state and indicates that no media has been
     * added yet, or the performer has been reset and has no content to play.
     *
     */
    int STATE_NONE = 0;

    /**
     * State indicating this item is currently stopped.
     *
     */
    int STATE_STOPPED = 1;

    /**
     * State indicating this item is currently paused.
     *
     */
    int STATE_PAUSED = 2;

    /**
     * State indicating this item is currently playing.
     *
     */
    int STATE_PLAYING = 3;

    /**
     * State indicating this item is currently buffering and will begin playing
     * when enough data has buffered.
     *
     */
    int STATE_BUFFERING = 6;

    /**
     * State indicating this item is currently in an error state.
     *
     */
    int STATE_ERROR = 7;
}

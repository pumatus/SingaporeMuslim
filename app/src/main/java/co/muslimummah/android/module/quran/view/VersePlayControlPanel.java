package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.muslimummah.android.R;
import co.muslimummah.android.module.quran.view.HorizontalProgressBar;
import co.muslimummah.android.player.MusicService;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.util.UiUtils;
import co.muslimummah.android.util.wrapper.Wrapper2;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by frank on 8/16/17.
 * Cannot be obfuscated because the class name of the behavior is used.
 */
public class VersePlayControlPanel extends LinearLayout {
    @BindView(R.id.hpb)
    HorizontalProgressBar hpb;
    @BindView(R.id.tv_reading_mode_progress_info)
    TextView tvReadingModeProgressInfo;
    @BindView(R.id.iv_listen)
    ImageView ivListen;
    @BindView(R.id.rl_reading_mode)
    RelativeLayout rlReadingMode;
    @BindView(R.id.iv_play)
    ImageView ivPlay;
    @BindView(R.id.iv_prev)
    ImageView ivPrev;
    @BindView(R.id.iv_next)
    ImageView ivNext;
    @BindView(R.id.tv_transliteration)
    TextView tvTransliteration;
    @BindView(R.id.tv_listening_mode_progress_info)
    TextView tvListeningModeProgressInfo;
    @BindView(R.id.iv_stop)
    ImageView ivStop;
    @BindView(R.id.cl_listening_mode)
    ConstraintLayout clListeningMode;
    @BindView(R.id.rl_current_listening)
    RelativeLayout rlCurrentListening;

    Disposable lastPlayButtonDelay;

    private OnActionClickListener onActionClickListener;

    public void setOnActionClickListener(OnActionClickListener onActionClickListener) {
        this.onActionClickListener = onActionClickListener;
    }

    public interface OnActionClickListener {
        void onHeadPhoneClick();

        void onPlayClick();

        void onStopClick();

        void onNextClick();

        void onPreviousClick();

        void onCurrentListeningClick(Verse currentListeningVerse, MusicService.MediaForm mediaForm);
    }

    public void setPlayButton(boolean playing) {
        if (lastPlayButtonDelay != null && !lastPlayButtonDelay.isDisposed()) {
            lastPlayButtonDelay.dispose();
        }

        if (!playing) {
            lastPlayButtonDelay = Observable.timer(300, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(@NonNull Long aLong) throws Exception {
                            lastPlayButtonDelay = null;
                            ivPlay.setSelected(false);
                        }
                    });
        } else {
            ivPlay.setSelected(true);
        }
    }

    public void setReadingMode() {
        rlReadingMode.setVisibility(VISIBLE);
        clListeningMode.setVisibility(GONE);
    }

    public void setListeningMode() {
        rlReadingMode.setVisibility(GONE);
        clListeningMode.setVisibility(VISIBLE);
    }

    private Chapter currentReadingChapter;
    private Verse currentReadingVerse;

    public void updateCurrentReadingVerse(Chapter chapter, Verse verse) {
        if (currentReadingChapter == chapter && currentReadingVerse == verse) {
            return;
        }
        currentReadingChapter = chapter;
        currentReadingVerse = verse;

        if (currentReadingChapter == null) {
            //This is the bookmarked mode
            long order = QuranRepository.INSTANCE.getBookmarkedVerseOrder(currentReadingVerse.getChapterId(), currentReadingVerse.getVerseId());
            long count = QuranRepository.INSTANCE.getBookmarkedVersesCount();
            tvReadingModeProgressInfo.setText(String.format(Locale.US, "%s (%d/%d)",getContext().getString(R.string.bookmarks), order, count));
            hpb.setProgress((float) order / count);
        } else {
            tvReadingModeProgressInfo.setText(String.format(Locale.US, "%s (%d/%d)", currentReadingChapter.getTransliteration(), Math.max(1, currentReadingVerse.getVerseId()), currentReadingChapter.getVerseCount()));
            hpb.setProgress((float) Math.max(1, currentReadingVerse.getVerseId()) / currentReadingChapter.getVerseCount());
        }
    }

    private Chapter currentListeningChapter;
    private Verse currentListeningVerse;
    private MusicService.MediaForm currentListeningMediaForm;

    public void updateCurrentListeningVerse(Wrapper2<Verse, MusicService.MediaForm> currentSource) {
        currentListeningVerse = currentSource.entity1;
        currentListeningMediaForm = currentSource.entity2;
        if (currentListeningMediaForm.equals(MusicService.MediaForm.BOOKMARK)) {
            tvTransliteration.setText(getContext().getString(R.string.bookmarks));
            long order = QuranRepository.INSTANCE.getBookmarkedVerseOrder(currentListeningVerse.getChapterId(), currentListeningVerse.getVerseId());
            long count = QuranRepository.INSTANCE.getBookmarkedVersesCount();
            tvListeningModeProgressInfo.setText(String.format(Locale.US, "%d/%d", order, count));
            hpb.setProgress((float) order / count);
        } else {
            if (currentListeningChapter == null || currentListeningChapter.getChapterId() != currentListeningVerse.getChapterId()) {
                currentListeningChapter = QuranRepository.INSTANCE.getChapter(currentListeningVerse.getChapterId()).blockingFirst();
            }
            tvTransliteration.setText(currentListeningChapter.getTransliteration());
            tvListeningModeProgressInfo.setText(String.format(Locale.US, "%d/%d", Math.max(1, currentListeningVerse.getVerseId()), currentListeningChapter.getVerseCount()));
            hpb.setProgress((float) Math.max(1, currentListeningVerse.getVerseId()) / currentListeningChapter.getVerseCount());
        }
    }

    public VersePlayControlPanel(Context context) {
        this(context, null);
    }

    public VersePlayControlPanel(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VersePlayControlPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);
        setBackgroundResource(R.color.grey_light);
        inflate(context, R.layout.layout_verse_play_control_panel, this);
        ButterKnife.bind(this);
        hpb.setProgressDrawableColor(ContextCompat.getColor(context, R.color.green_primary));
    }

    @OnClick(R.id.iv_play)
    public void onPlayClick() {
        if (onActionClickListener != null) {
            onActionClickListener.onPlayClick();
        }
    }

    @OnClick(R.id.iv_next)
    public void onNextClick() {
        if (onActionClickListener != null) {
            onActionClickListener.onNextClick();
        }
    }

    @OnClick(R.id.iv_prev)
    public void onPreviousClick() {
        if (onActionClickListener != null) {
            onActionClickListener.onPreviousClick();
        }
    }

    @OnClick(R.id.iv_stop)
    public void onStopClick() {
        if (onActionClickListener != null) {
            onActionClickListener.onStopClick();
        }
    }

    @OnClick(R.id.iv_listen)
    public void onHeadPhoneClick() {
        if (onActionClickListener != null) {
            onActionClickListener.onHeadPhoneClick();
        }
    }

    @OnClick(R.id.rl_current_listening)
    public void onCurrentListeningClick() {
        if (onActionClickListener != null) {
            onActionClickListener.onCurrentListeningClick(currentListeningVerse, currentListeningMediaForm);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return true;
    }

    public static class Behavior extends CoordinatorLayout.Behavior<VersePlayControlPanel> {
        public Behavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, VersePlayControlPanel child, View dependency) {
            return dependency instanceof AppBarLayout;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, VersePlayControlPanel child, View dependency) {
            float hidingRatio = (float) Math.abs(dependency.getTop()) / dependency.getHeight();
            child.setTranslationY((child.getHeight() - UiUtils.dp2px(4)) * hidingRatio);
            return true;
        }
    }
}

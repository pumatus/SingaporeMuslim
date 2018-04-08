package co.muslimummah.android.module.quran.activity;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.AnalyticsConstants;
import co.muslimummah.android.analytics.LogObject;
import co.muslimummah.android.analytics.OracleAnalytics;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.OracleLocaleHelper;
import co.muslimummah.android.base.SimpleDividerItemDecoration;
import co.muslimummah.android.base.lifecycle.ScreenEvent;
import co.muslimummah.android.player.MediaNotificationHelper;
import co.muslimummah.android.player.MusicService;
import co.muslimummah.android.player.MusicServiceLogDelegate;
import co.muslimummah.android.player.PlaybackState;
import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.quran.adapter.OnPlayVerseListener;
import co.muslimummah.android.module.quran.adapter.OnVerseBookmarkClickListener;
import co.muslimummah.android.module.quran.adapter.VerseAdapter;
import co.muslimummah.android.module.quran.fragment.FirstTimeWordIntroDialogFragment;
import co.muslimummah.android.module.quran.helper.LinearLayoutManagerWithSmoothScroller;
import co.muslimummah.android.module.quran.helper.WordPopupDelegate;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.TranslationWord;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.module.quran.view.QuranSettingTranslationView;
import co.muslimummah.android.module.quran.view.QuranSettingView;
import co.muslimummah.android.module.quran.view.TouchAwareRecyclerView;
import co.muslimummah.android.module.quran.view.TouchableToolbar;
import co.muslimummah.android.module.quran.view.VersePlayControlPanel;
import co.muslimummah.android.module.quran.view.VerseSelectorPopup;
import co.muslimummah.android.module.quran.view.WordPopupWindow;
import co.muslimummah.android.storage.AppSession;
import co.muslimummah.android.util.SchemeUtils;
import co.muslimummah.android.util.wrapper.Wrapper2;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;


public class VerseActivity extends QuranAudioResourceDownloadingDialogActivity implements WordPopupDelegate.Interact {
    private static final String INTENT_KEY_CHAPTER = "INTENT_KEY_CHAPTER";
    private static final String INTENT_KEK_TARGET_VERSE = "INTENT_KEK_TARGET_VERSE";
    private static final String SP_KEY_FORMAT_LAST_READ_VERSE = "quran.activity.VerseActivity.SP_KEY_FORMAT_LAST_READ_VERSE-%d";

    public static void start(Context context, Chapter chapter) {
        start(context, chapter, SchemeUtils.VERSE_LAST_TIME_VISITED);
    }

    public static void start(Context context, Chapter chapter, long verseId) {
        context.startActivity(getStartIntent(context, chapter, verseId));
    }

    public static Intent getStartIntent(Context context, Chapter chapter, long verseId) {
        Intent intent = new Intent(context, VerseActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra(INTENT_KEY_CHAPTER, chapter);
        if (verseId != SchemeUtils.VERSE_LAST_TIME_VISITED) {
            intent.putExtra(INTENT_KEK_TARGET_VERSE, verseId);
        } else {
            // if verse is null, get it from cache
            Verse currentChapterLastReadVerse = AppSession.getInstance(context).getCachedValue(
                    String.format(Locale.US, SP_KEY_FORMAT_LAST_READ_VERSE, chapter.getChapterId()),
                    Verse.class);
            if (currentChapterLastReadVerse != null) {
                intent.putExtra(INTENT_KEK_TARGET_VERSE, currentChapterLastReadVerse.getVerseId());
            }
        }
        return intent;
    }

    @BindView(R.id.dl)
    DrawerLayout dl;
    @BindView(R.id.qsv)
    QuranSettingView qsv;
    @BindView(R.id.toolbar)
    TouchableToolbar toolbar;
    @BindView(R.id.tv_transliteration)
    TextView tvTransliteration;
    @BindView(R.id.rv_verses)
    TouchAwareRecyclerView rvVerses;
    @BindView(R.id.vpcp)
    VersePlayControlPanel versePlayControlPanel;
    @BindView(R.id.view_fake_shadow)
    View viewFakeShadow;


    VerseAdapter mVerseAdapter;
    LinearLayoutManager mLinearLayoutManager;

    Chapter mChapter;

    private long mLastTouchEventTime;
    private long mLastAutoNewChapterEventTime;

    ServiceConnection musicService;
    MusicService.PlayerBinder playerBinder;
    MusicService.OnPlayStateChangeListener onPlayStateChangeListener;
    MusicService.OnPlayProgressUpdateListener onPlayProgressUpdateListener;

    Disposable lastPlayVerseDisposable;

    MusicServiceLogDelegate mMusicServiceLogDelegate;

    //word by word
    private WordPopupDelegate mWordPopupDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verse);
        ButterKnife.bind(this);

        setupToolbar();

        mLinearLayoutManager = new LinearLayoutManagerWithSmoothScroller(this);
        rvVerses.setLayoutManager(mLinearLayoutManager);
        rvVerses.addItemDecoration(new SimpleDividerItemDecoration(this));
        rvVerses.setItemAnimator(null);
        mVerseAdapter = new VerseAdapter(this);
        rvVerses.setAdapter(mVerseAdapter);
        mVerseAdapter.bindRecyclerView(rvVerses);
        rvVerses.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateControlPanel(false);
                if (mWordPopupDelegate != null) {
                    mWordPopupDelegate.dismissWordPop();
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Verse verse = getCurrentReadingVerse();
                    if (verse == null) {
                        return;
                    }

                    OracleAnalytics.INSTANCE
                            .addLog(LogObject.newBuilder()
                                    .behaviour(AnalyticsConstants.BEHAVIOUR.SWIPE)
                                    .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE)
                                    .target(AnalyticsConstants.TARGET_TYPE.VERSE_ID, verse.getChapterId() + ":" + verse.getVerseId())
                                    .build());
                }
            }
        });
        rvVerses.setOnTouchListener(new TouchAwareRecyclerView.OnTouchListener() {
            @Override
            public void onTouchEvent(MotionEvent ev) {
                mLastTouchEventTime = System.currentTimeMillis();
            }
        });
        //word by word
        mWordPopupDelegate = new WordPopupDelegate();
        mWordPopupDelegate.setMode(WordPopupWindow.Mode.NORMAL);
        mWordPopupDelegate.setInteract(this);
        mVerseAdapter.setInteract(mWordPopupDelegate);
        //word by word

        mVerseAdapter.setPlayVerseListener(new OnPlayVerseListener() {
            @Override
            public void play(Verse verse) {
                if (playerBinder != null && playerBinder.getCurrentSource() != null && playerBinder.isPlaying() && playerBinder.getCurrentSource().entity1.equals(verse)) {
                    mMusicServiceLogDelegate.logEvent("QuranVerseView", "PlayThisVerse[SameVerse]", null);
                } else {
                    mMusicServiceLogDelegate.logEvent("QuranVerseView", "PlayThisVerse[OtherVerse]", null);
                }
                playVerse(verse, MusicService.MediaForm.NORMAL_VERSE);
            }
        });
        mVerseAdapter.setOnVerseBookmarkedListener(new OnVerseBookmarkClickListener() {
            @Override
            public void onVerseBookmarkClicked(Verse verse) {
                qsv.refreshVerseBookmarkCount();

                mMusicServiceLogDelegate.logEvent("QuranVerseView", verse.getIsBookMarked() ? "Bookmark" : "Unbookmark", null);
                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE)
                                .behaviour(verse.getIsBookMarked() ? AnalyticsConstants.BEHAVIOUR.BOOKMARK : AnalyticsConstants.BEHAVIOUR.UNBOOKMARK)
                                .target(AnalyticsConstants.TARGET_TYPE.VERSE_ID, verse.getChapterId() + ":" + verse.getVerseId())
                                .build());
            }
        });

        qsv.setOnTranslationSelectListener(new QuranSettingTranslationView.OnTranslationSelectListener() {
            @Override
            public void onTranslationSelected(Context context, @Nullable OracleLocaleHelper.LanguageEnum languageEnum) {
                mVerseAdapter.update();
//                if (QuranRepository.INSTANCE.isVerseTranslationAvailable(context)) {
//                    mVerseAdapter.update();
//                } else {
//                    refreshCurrentChapter(mChapter);
//                }
            }
        });

        qsv.setOnTransliterationCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mVerseAdapter.notifyDataSetChanged();
            }
        });

        qsv.setOnAudioSyncCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mVerseAdapter.notifyDataSetChanged();
            }
        });

        tvTransliteration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Verse> verses = mVerseAdapter.getRealVerses();
                if (verses != null) {
                    final VerseSelectorPopup verseSelectorPopup = new VerseSelectorPopup(VerseActivity.this, verses);
                    verseSelectorPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            mMusicServiceLogDelegate.logEvent("QuranVerseView", "Menu[SelectNone]", null);
                            viewFakeShadow.setVisibility(View.GONE);
                        }
                    });
                    verseSelectorPopup.setOnVerseClickListener(new VerseSelectorPopup.OnVerseClickListener() {
                        @Override
                        public void onVerseClicked(Verse verse) {
                            mMusicServiceLogDelegate.logEvent("QuranVerseView", "Menu[SelectVerse]", null);
                            viewFakeShadow.setVisibility(View.GONE);
                            verseSelectorPopup.setOnDismissListener(null);
                            verseSelectorPopup.dismiss();
                            smoothScrollToPosition(mVerseAdapter.getAdapterPositionByVerse(verse));
                        }

                        @Override
                        public void onBookmarkClicked(Verse verse) {

                            OracleAnalytics.INSTANCE
                                    .addLog(LogObject.newBuilder()
                                            .behaviour(verse.getIsBookMarked() ? AnalyticsConstants.BEHAVIOUR.UNBOOKMARK : AnalyticsConstants.BEHAVIOUR.BOOKMARK)
                                            .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE_DROPDOWN_LIST)
                                            .build());

                            QuranRepository.INSTANCE.bookMarkVerse(verse, !verse.getIsBookMarked());
                            mVerseAdapter.notifyItemChanged(mVerseAdapter.getAdapterPositionByVerse(verse));
                        }
                    });
                    verseSelectorPopup.showAsDropDown(toolbar);
                    viewFakeShadow.setVisibility(View.VISIBLE);
                }
            }
        });

        musicService = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                playerBinder = (MusicService.PlayerBinder) service;
                updateControlPanel(true);

                onPlayStateChangeListener = new MusicService.OnPlayStateChangeListener() {
                    @Override
                    public void onPlayStateChanged(int state, Verse verse) {
                        Timber.d("onPlayStateChanged %d", state);
                        if (state == PlaybackState.STATE_PLAYING) {
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        } else {
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }

                        if (state == PlaybackState.STATE_STOPPED) {
                            mVerseAdapter.onPlayStop();
                        } else if (state == PlaybackState.STATE_PLAYING) {
                            if (verse != null && mChapter != null
                                    && playerBinder.getCurrentSource().entity2 == MusicService.MediaForm.NORMAL_VERSE) {
                                if (verse.getChapterId() == mChapter.getChapterId() && System.currentTimeMillis() - mLastTouchEventTime > 3000
                                        && !mWordPopupDelegate.isWordPopupShowing()) {
                                    smoothScrollToPosition(mVerseAdapter.getAdapterPositionByVerse(verse));
                                }
                            }
                        }
                        updateControlPanel(state != PlaybackState.STATE_STOPPED);


                    }

                    @Override
                    public void onBroadcastReceive(Context context, Intent intent) {
                        //word by word
                        Timber.d("onBroadcastReceive");
                        if (mWordPopupDelegate != null) {
                            mWordPopupDelegate.dismissWordPop();
                        }
                    }
                };

                onPlayProgressUpdateListener = new MusicService.OnPlayProgressUpdateListener() {
                    Wrapper2<Verse, Integer> progressWrapper;

                    @Override
                    public void onProgressUpdate(int progress, Verse object, MusicService.MediaForm form) {
                        if (mVerseAdapter != null) {
                            progressWrapper = new Wrapper2<>(object, progress);
                            mVerseAdapter.updateProgress(progressWrapper);
                        }
                    }

                    @Override
                    public void onNewChapter(long oldChapterId, Chapter newChapter, Verse newVerse) {
                        // change to new chapter
                        if (mWordPopupDelegate != null) {
                            mWordPopupDelegate.dismissWordPop();
                        }

                        mLastAutoNewChapterEventTime = System.currentTimeMillis();
                        getIntent().putExtra(INTENT_KEK_TARGET_VERSE, newVerse.getVerseId());
                        refreshCurrentChapter(newChapter);
                        updateControlPanelJustForNewChapter(newVerse);
                    }
                };

                playerBinder.addOnPlayStateChangeListener(onPlayStateChangeListener);
                playerBinder.addOnPlayProgressUpdateListener(onPlayProgressUpdateListener);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                playerBinder.removeOnPlayProgressUpdateListener(onPlayProgressUpdateListener);
                playerBinder.removeOnPlayStateChangeListener(onPlayStateChangeListener);
                playerBinder = null;
            }
        };

        versePlayControlPanel.setOnActionClickListener(new VersePlayControlPanel.OnActionClickListener() {
            @Override
            public void onHeadPhoneClick() {
                mMusicServiceLogDelegate.logEvent("QuranVerseView", "PlayAll", null);
                Verse verse = getCurrentReadingVerse();
                if (verse != null && playerBinder != null) {
                    playVerse(verse, MusicService.MediaForm.NORMAL_VERSE);
                }

                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE_PANEL_PLAY_ALL)
                                .build());
            }

            @Override
            public void onPlayClick() {
                if (playerBinder != null) {
                    if (playerBinder.isPlaying()) {
                        mMusicServiceLogDelegate.logEvent("QuranVerseView", "Pause", null);
                        if (System.currentTimeMillis() - mLastAutoNewChapterEventTime < 5000) {
                            ThirdPartyAnalytics.INSTANCE.logEvent("QuranVerseView", "AudioPlayingChangeChapter", "Pause", null);
                        }
                        playerBinder.pause();

                        OracleAnalytics.INSTANCE
                                .addLog(LogObject.newBuilder()
                                        .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                        .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE_PANEL_PAUSE)
                                        .build());
                    } else if (playerBinder.getPlayState() == PlaybackState.STATE_PAUSED) {
                        mMusicServiceLogDelegate.logEvent("QuranVerseView", "Play", null);
                        playVerse(playerBinder.getCurrentSource().entity1, playerBinder.getCurrentSource().entity2);

                        OracleAnalytics.INSTANCE
                                .addLog(LogObject.newBuilder()
                                        .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                        .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE_PANEL_PLAY)
                                        .build());
                    } else {
                        playVerse(getCurrentReadingVerse(), MusicService.MediaForm.NORMAL_VERSE);

                        OracleAnalytics.INSTANCE
                                .addLog(LogObject.newBuilder()
                                        .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                        .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE_PANEL_PLAY)
                                        .build());
                    }
                }
            }

            @Override
            public void onStopClick() {
                mMusicServiceLogDelegate.logEvent("QuranVerseView", "Stop", null);
                if (System.currentTimeMillis() - mLastAutoNewChapterEventTime < 5000) {
                    ThirdPartyAnalytics.INSTANCE.logEvent("QuranVerseView", "AudioPlayingChangeChapter", "Stop", null);
                }
                if (playerBinder != null) {
                    playerBinder.stop();
                }

                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE_PANEL_STOP)
                                .build());
            }

            @Override
            public void onNextClick() {
                mMusicServiceLogDelegate.logEvent("QuranVerseView", "Next", null);
                if (playerBinder != null) {
                    playerBinder.next();
                }

                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE_PANEL_NEXT)
                                .build());
            }

            @Override
            public void onPreviousClick() {
                if (System.currentTimeMillis() - mLastAutoNewChapterEventTime < 5000) {
                    ThirdPartyAnalytics.INSTANCE.logEvent("QuranVerseView", "AudioPlayingChangeChapter", "Previous", null);
                }
                mMusicServiceLogDelegate.logEvent("QuranVerseView", "Previous", null);
                if (playerBinder != null) {
                    playerBinder.previous();
                }

                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE_PANEL_PREVIOUS)
                                .build());
            }

            @Override
            public void onCurrentListeningClick(Verse currentListeningVerse, MusicService.MediaForm mediaForm) {
                mMusicServiceLogDelegate.logEvent("QuranVerseView", "StatusBar", null);
                switch (mediaForm) {
                    case NORMAL_VERSE:
                        VerseActivity.start(VerseActivity.this, QuranRepository.INSTANCE.getChapter(currentListeningVerse.getChapterId()).blockingFirst(), currentListeningVerse.getVerseId());
                        break;
                    case BOOKMARK:
                        BookmarkedVerseActivity.start(VerseActivity.this, currentListeningVerse);
                        break;
                }
            }
        });

        bindService(new Intent(this, MusicService.class), musicService, Service.BIND_AUTO_CREATE);

        mMusicServiceLogDelegate = new MusicServiceLogDelegate();
        refresh();

        // first time in, show the hint popup
        Boolean shown = AppSession.getInstance(this).getCachedValue(Constants.SP_SHOWN_WORD_INTRO, Boolean.class);
        if (shown == null || !shown) {
            AppSession.getInstance(this).cacheValue(Constants.SP_SHOWN_WORD_INTRO, Boolean.TRUE, true);
            FirstTimeWordIntroDialogFragment fragment = new FirstTimeWordIntroDialogFragment();
            fragment.show(getSupportFragmentManager(), FirstTimeWordIntroDialogFragment.class.getSimpleName());
        }

    }

    @Override
    protected void onDownloadRetryClick(Verse verse) {
        playVerse(verse, MusicService.MediaForm.NORMAL_VERSE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThirdPartyAnalytics.INSTANCE.setCurrentScreen(this, "QuranVerseView");

        mVerseAdapter.notifyDataSetChanged();
        if (playerBinder != null && playerBinder.getPlayState() == PlaybackState.STATE_STOPPED) {
            mVerseAdapter.onPlayStop();
            mVerseAdapter.onPlayStop();
        }
        qsv.refreshVerseBookmarkCount();
        mMusicServiceLogDelegate.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistLastReadVerse();
        mMusicServiceLogDelegate.unBind();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (dl.isDrawerOpen(Gravity.END)) {
            dl.closeDrawer(Gravity.END, false);
        }

        if (mChapter != null) {
            OracleAnalytics.INSTANCE
                    .addLog(LogObject.newBuilder()
                            .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE)
                            .behaviour(AnalyticsConstants.BEHAVIOUR.LEAVE)
                            .target(AnalyticsConstants.TARGET_TYPE.CHAPTER_ID, String.valueOf(mChapter.getChapterId()))
                            .build());
        }
    }

    private void playVerse(final Verse verse, final MusicService.MediaForm mediaForm) {
        if (verse == null) {
            return;
        }

        if (lastPlayVerseDisposable != null && !lastPlayVerseDisposable.isDisposed()) {
            lastPlayVerseDisposable.dispose();
            lastPlayVerseDisposable = null;
        }

        lastPlayVerseDisposable = QuranRepository.INSTANCE
                .getVerseAudioCacheKey(verse.getChapterId(), verse.getVerseId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(lifecycleProvider().<String>bindUntilEvent(ScreenEvent.DESTROY))
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(@NonNull String mp3CacheKey) throws Exception {
                        if (playerBinder != null) {
                            playerBinder.play(mp3CacheKey, verse, mediaForm);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                    }
                });
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        toolbar.inflateMenu(R.menu.menu_setting);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_setting:
                        mMusicServiceLogDelegate.logEvent("QuranVerseView", "Setting", null);
                        if (dl.isDrawerOpen(Gravity.END)) {
                            dl.closeDrawer(Gravity.END);
                        } else {
                            dl.openDrawer(Gravity.END);
                        }
                        return true;
                }
                return false;
            }
        });
        toolbar.setTouchListener(new TouchableToolbar.TouchListener() {
            @Override
            public void onTouch(MotionEvent event) {
                if (mWordPopupDelegate != null) {
                    mWordPopupDelegate.dismissPopup();
                }
            }
        });
    }

    private void refresh() {
        if (getIntent().hasExtra(MediaNotificationHelper.INTENT_KEY_CLICK_ACTION)) {
            ThirdPartyAnalytics.INSTANCE.logEvent("QuranNotification", "Click", "OtherArea", null);

            OracleAnalytics.INSTANCE
                    .addLog(LogObject.newBuilder()
                            .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                            .location(AnalyticsConstants.LOCATION.NOTIFICATION_BAR_OTHERAREA)
                            .build());

            getIntent().removeExtra(MediaNotificationHelper.INTENT_KEY_CLICK_ACTION);
        }
        refreshCurrentChapter((Chapter) getIntent().getSerializableExtra(INTENT_KEY_CHAPTER));
    }

    private void refreshCurrentChapter(Chapter chapter) {
        final boolean withinTheSameChapter = (mChapter != null && mChapter.getChapterId() == chapter.getChapterId());
        this.mChapter = chapter;
        tvTransliteration.setText(mChapter.getTransliteration());
        updateControlPanel(true);

        QuranRepository.INSTANCE
                .getVersesWithoutAudioResource(mChapter.getChapterId())
                .compose(lifecycleProvider().<List<Verse>>bindUntilEvent(ScreenEvent.DESTROY))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Verse>>() {
                    @Override
                    public void accept(@NonNull List<Verse> verses) throws Exception {
                        mVerseAdapter.update(verses);
                        qsv.refreshTranslationSelection();

                        if (getIntent().hasExtra(INTENT_KEK_TARGET_VERSE)) {
                            //Scroll to the target position, may be last read position or auto played next verse.
                            long verseId = getIntent().getLongExtra(INTENT_KEK_TARGET_VERSE, 0L);
                            int adapterPosition = mVerseAdapter.getAdapterPositionByVerseId(verseId);
                            if (adapterPosition >= 0 && adapterPosition < mVerseAdapter.getItemCount()) {
                                if (withinTheSameChapter) {
                                    smoothScrollToPosition(adapterPosition);
                                } else {
                                    mLinearLayoutManager.scrollToPositionWithOffset(adapterPosition, 0);
                                }
                            }
                            getIntent().removeExtra(INTENT_KEK_TARGET_VERSE);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Timber.e(throwable, "getVersesWithoutAudioResource in VerseActivity failed");
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                    }
                });
    }

    private void persistLastReadVerse() {
        Verse currentReadingVerse = getCurrentReadingVerse();
        if (currentReadingVerse != null) {
            AppSession.getInstance(this).cacheValue(Verse.SP_KEY_LAST_READ, currentReadingVerse, true);
            AppSession.getInstance(this).cacheValue(
                    String.format(Locale.US, SP_KEY_FORMAT_LAST_READ_VERSE, currentReadingVerse.getChapterId()),
                    currentReadingVerse,
                    86400L,
                    true);
        } else {
            Timber.e("Compute last verse error!");
        }
    }

    private void smoothScrollToPosition(int destinationPos) {
        if (destinationPos == RecyclerView.NO_POSITION) {
            return;
        }
        int currentPos = mLinearLayoutManager.findFirstVisibleItemPosition();

        if (destinationPos > currentPos + 5) {
            rvVerses.scrollToPosition(destinationPos - 5);
        } else if (destinationPos < currentPos - 5) {
            rvVerses.scrollToPosition(destinationPos + 5);
        }
        rvVerses.smoothScrollToPosition(destinationPos);
    }

    private Verse getCurrentReadingVerse() {
        int firstCompleteVisiblePosition = mLinearLayoutManager.findFirstCompletelyVisibleItemPosition();
        Verse currentReadingVerse;
        if (firstCompleteVisiblePosition != RecyclerView.NO_POSITION) {
            currentReadingVerse = mVerseAdapter.getVerseByAdapterPosition(firstCompleteVisiblePosition);
        } else {
            int firstVisiblePosition = mLinearLayoutManager.findFirstVisibleItemPosition();
            int lastVisiblePosition = mLinearLayoutManager.findLastVisibleItemPosition();
            if (firstVisiblePosition < lastVisiblePosition) {
                currentReadingVerse = mVerseAdapter.getVerseByAdapterPosition(firstVisiblePosition + 1);
            } else {
                currentReadingVerse = mVerseAdapter.getVerseByAdapterPosition(firstVisiblePosition);
            }
        }
        return currentReadingVerse;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerBinder != null) {
            playerBinder.removeOnPlayStateChangeListener(onPlayStateChangeListener);
            playerBinder.removeOnPlayProgressUpdateListener(onPlayProgressUpdateListener);
            playerBinder = null;
        }

        if (musicService != null) {
            unbindService(musicService);
            musicService = null;
        }
    }

    private void updateControlPanel(boolean shouldUpdateListeningMode) {
        if (mChapter != null && playerBinder != null) {
            Wrapper2<Verse, MusicService.MediaForm> currentSource = playerBinder.getCurrentSource();
            if (currentSource != null) {
                if (shouldUpdateListeningMode) {
                    versePlayControlPanel.setListeningMode();
                    versePlayControlPanel.setPlayButton(playerBinder.getPlayState() == PlaybackState.STATE_PLAYING);
                    versePlayControlPanel.updateCurrentListeningVerse(currentSource);
                }
            } else {
                versePlayControlPanel.setReadingMode();
                Verse currentReadingVerse = getCurrentReadingVerse();
                if (currentReadingVerse != null) {
                    versePlayControlPanel.updateCurrentReadingVerse(mChapter, currentReadingVerse);
                }
            }
        }
    }

    private void updateControlPanelJustForNewChapter(Verse newVerse) {
        if (newVerse != null) {
            Wrapper2<Verse, MusicService.MediaForm> currentSource = new Wrapper2<>(newVerse, MusicService.MediaForm.NORMAL_VERSE);
            versePlayControlPanel.setListeningMode();
            versePlayControlPanel.setPlayButton(false);
            versePlayControlPanel.updateCurrentListeningVerse(currentSource);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mChapter != null) {
            OracleAnalytics.INSTANCE
                    .addLog(LogObject.newBuilder()
                            .location(AnalyticsConstants.LOCATION.QURAN_VERSE_VIEW_PAGE)
                            .behaviour(AnalyticsConstants.BEHAVIOUR.ENTER)
                            .target(AnalyticsConstants.TARGET_TYPE.CHAPTER_ID, String.valueOf(mChapter.getChapterId()))
                            .build());
        }
    }

    @Override
    public void onBackPressed() {
        if (mWordPopupDelegate.isWordPopupShowing()) {
            mWordPopupDelegate.dismissPopup();
        } else {
            super.onBackPressed();
            mMusicServiceLogDelegate.logEvent("QuranVerseView", "Return", null);
            if (System.currentTimeMillis() - mLastAutoNewChapterEventTime < 5000) {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranVerseView", "AudioPlayingChangeChapter", "Return", null);
            }
        }
    }


    @Override
    public View getAnchorView() {
        return toolbar;
    }

    @Override
    public void requestWordData(boolean firstTime) {
        mWordPopupDelegate.requestWordData(firstTime);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mWordPopupDelegate.destroy();
    }

    @Override
    public void playWord(TranslationWord word) {
        if (playerBinder != null) {
            playerBinder.playWord(word);
        }
    }

    @Override
    public void moveTo(int position) {
        mVerseAdapter.moveTo(position);
    }

    @Override
    public void clearSelectedStatus() {
        mVerseAdapter.clearSelectedStatus();
    }
}

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
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.OracleApp;
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
import co.muslimummah.android.util.PhoneInfoUtils;
import co.muslimummah.android.module.quran.adapter.BookmarkedVerseAdapter;
import co.muslimummah.android.module.quran.adapter.OnPlayVerseListener;
import co.muslimummah.android.module.quran.adapter.OnVerseBookmarkClickListener;
import co.muslimummah.android.module.quran.helper.LinearLayoutManagerWithSmoothScroller;
import co.muslimummah.android.module.quran.helper.WordPopupDelegate;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.TranslationWord;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.module.quran.view.DownloadStateDialog;
import co.muslimummah.android.module.quran.view.QuranSettingTranslationView;
import co.muslimummah.android.module.quran.view.QuranSettingView;
import co.muslimummah.android.module.quran.view.TouchAwareRecyclerView;
import co.muslimummah.android.module.quran.view.TouchableToolbar;
import co.muslimummah.android.module.quran.view.VersePlayControlPanel;
import co.muslimummah.android.module.quran.view.WordPopupWindow;
import co.muslimummah.android.util.ToastUtil;
import co.muslimummah.android.util.UiUtils;
import co.muslimummah.android.util.wrapper.Wrapper2;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by frank on 8/18/17.
 */

public class BookmarkedVerseActivity extends QuranAudioResourceDownloadingDialogActivity implements WordPopupDelegate.Interact {
    private static final String INTENT_KEK_TARGET_VERSE = "INTENT_KEK_TARGET_VERSE";

    public static void start(Context context) {
        start(context, null);
    }

    public static void start(Context context, @Nullable Verse verse) {
        if (QuranRepository.INSTANCE.getBookmarkedVersesCount() == 0) {
            ToastUtil.show(context.getString(R.string.no_bookmarked_verses));
            return;
        }
        context.startActivity(getStartIntent(context, verse));
    }

    public static Intent getStartIntent(Context context, Verse verse) {
        Intent intent = new Intent(context, BookmarkedVerseActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        if (verse != null) {
            intent.putExtra(INTENT_KEK_TARGET_VERSE, verse);
        }
        return intent;
    }

    @BindView(R.id.dl)
    DrawerLayout dl;
    @BindView(R.id.qsv)
    QuranSettingView qsv;
    @BindView(R.id.toolbar)
    TouchableToolbar toolbar;
    @BindView(R.id.rv_verses)
    TouchAwareRecyclerView rvVerses;
    @BindView(R.id.vpcp)
    VersePlayControlPanel vpcp;

    private DownloadStateDialog downloadStateDialog;

    BookmarkedVerseAdapter mVerseAdapter;
    LinearLayoutManager mLinearLayoutManager;

    private long mLastTouchEventTime;

    ServiceConnection musicService;
    MusicService.PlayerBinder playerBinder;
    MusicService.OnPlayStateChangeListener onPlayStateChangeListener;
    MusicService.OnPlayProgressUpdateListener onPlayProgressUpdateListener;

    Disposable lastPlayVerseDisposable;
    Disposable verseDownloadDisposable;

    MusicServiceLogDelegate mMusicServiceLogDelegate;
    //word by word
    private WordPopupDelegate mWordPopupDelegate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarked_verse);
        ButterKnife.bind(this);

        setupToolbar();

        mLinearLayoutManager = new LinearLayoutManagerWithSmoothScroller(this);
        rvVerses.setLayoutManager(mLinearLayoutManager);
        rvVerses.addItemDecoration(new SimpleDividerItemDecoration(this));
        rvVerses.setItemAnimator(null);
        mVerseAdapter = new BookmarkedVerseAdapter(this);
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
                    OracleAnalytics.INSTANCE
                            .addLog(LogObject.newBuilder()
                                    .behaviour(AnalyticsConstants.BEHAVIOUR.SWIPE)
                                    .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE)
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
        mWordPopupDelegate.setMode(WordPopupWindow.Mode.BOOKMARK);
        mWordPopupDelegate.setInteract(this);

        mVerseAdapter.setInteract(mWordPopupDelegate);
        //word by word
        mVerseAdapter.setOnVerseTitleClickListener(new BookmarkedVerseAdapter.OnVerseTitleClickListener() {
            @Override
            public void onVerseTitleClicked(Chapter chapter, Verse verse) {
                mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "Title", null);
                VerseActivity.start(BookmarkedVerseActivity.this, chapter, verse.getVerseId());
            }
        });
        mVerseAdapter.setOnVerseBookmarkedListener(new OnVerseBookmarkClickListener() {
            @Override
            public void onVerseBookmarkClicked(Verse verse) {
                mMusicServiceLogDelegate.logEvent("QuranBookmarkView", verse.getIsBookMarked() ? "Bookmark" : "Unbookmark", null);
                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE)
                                .behaviour(verse.getIsBookMarked() ? AnalyticsConstants.BEHAVIOUR.BOOKMARK : AnalyticsConstants.BEHAVIOUR.UNBOOKMARK)
                                .target(AnalyticsConstants.TARGET_TYPE.VERSE_ID, verse.getChapterId() + ":" + verse.getVerseId())
                                .build());
                qsv.refreshVerseBookmarkCount();
            }
        });

        mVerseAdapter.setPlayVerseListener(new OnPlayVerseListener() {
            @Override
            public void play(final Verse verse) {
//                    playAudio(currentItem);
                if (playerBinder != null && playerBinder.getCurrentSource() != null && playerBinder.isPlaying() && playerBinder.getCurrentSource().entity1.equals(verse)) {
                    mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "PlayThisVerse[SameVerse]", null);
                } else {
                    mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "PlayThisVerse[OtherVerse]", null);
                }

                playVerse(verse, MusicService.MediaForm.BOOKMARK);
            }
        });

        qsv.setOnTranslationSelectListener(new QuranSettingTranslationView.OnTranslationSelectListener() {
            @Override
            public void onTranslationSelected(Context context, @Nullable OracleLocaleHelper.LanguageEnum languageEnum) {
                mVerseAdapter.update();
//                if (QuranRepository.INSTANCE.isVerseTranslationAvailable(context)) {
//                    mVerseAdapter.update();
//                } else {
//                    refresh();
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

        musicService = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                playerBinder = (MusicService.PlayerBinder) service;
                updateControlPanel(true);

                onPlayStateChangeListener = new MusicService.OnPlayStateChangeListener() {
                    @Override
                    public void onPlayStateChanged(int state, Verse verse) {
                        if (state == PlaybackState.STATE_PLAYING) {
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        } else {
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        }

                        if (state == PlaybackState.STATE_STOPPED) {
                            mVerseAdapter.onPlayStop();
                        } else if (state == PlaybackState.STATE_PLAYING) {
                            if (verse != null && playerBinder.getCurrentSource().entity2 == MusicService.MediaForm.BOOKMARK) {
                                if (System.currentTimeMillis() - mLastTouchEventTime > 3000
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
                        Timber.d("onPlayStateChanged");
                        if (mWordPopupDelegate != null) {
                            mWordPopupDelegate.dismissWordPop();
                        }
                    }
                };

                onPlayProgressUpdateListener = new MusicService.OnPlayProgressUpdateListener() {
                    Wrapper2<Verse, Integer> progressWrapper;

                    @Override
                    public void onProgressUpdate(int progress, Verse verse, MusicService.MediaForm form) {
                        if (mVerseAdapter != null) {
                            progressWrapper = new Wrapper2<>(verse, progress);
                            mVerseAdapter.updateProgress(progressWrapper);
                        }
                    }

                    @Override
                    public void onNewChapter(long oldChapterId, Chapter newChapter, Verse newVerse) {
                        //This is not gonna to happen.
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

        vpcp.setOnActionClickListener(new VersePlayControlPanel.OnActionClickListener() {
            @Override
            public void onHeadPhoneClick() {
                mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "PlayAll", null);
                Verse verse = getCurrentReadingVerse();
                if (verse != null && playerBinder != null) {
                    playVerse(verse, MusicService.MediaForm.BOOKMARK);
                }

                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE_PLAY_PANEL_PLAY_ALL)
                                .build());
            }

            @Override
            public void onPlayClick() {
                if (playerBinder != null) {
                    if (playerBinder.isPlaying()) {
                        mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "Pause", null);
                        playerBinder.pause();
                        OracleAnalytics.INSTANCE
                                .addLog(LogObject.newBuilder()
                                        .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                        .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE_PLAY_PANEL_PAUSE)
                                        .build());

                    } else if (playerBinder.getPlayState() == PlaybackState.STATE_PAUSED) {
                        mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "Play", null);
                        playVerse(playerBinder.getCurrentSource().entity1, playerBinder.getCurrentSource().entity2);

                        OracleAnalytics.INSTANCE
                                .addLog(LogObject.newBuilder()
                                        .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                        .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE_PLAY_PANEL_PLAY)
                                        .build());
                    } else {
                        playVerse(getCurrentReadingVerse(), MusicService.MediaForm.BOOKMARK);

                        OracleAnalytics.INSTANCE
                                .addLog(LogObject.newBuilder()
                                        .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                        .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE_PLAY_PANEL_PLAY)
                                        .build());
                    }
                }


            }

            @Override
            public void onStopClick() {
                mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "Stop", null);
                if (playerBinder != null) {
                    playerBinder.stop();
                }

                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE_PLAY_PANEL_STOP)
                                .build());
            }

            @Override
            public void onNextClick() {
                mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "Next", null);
                if (playerBinder != null) {
                    playerBinder.next();
                }

                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE_PLAY_PANEL_NEXT)
                                .build());
            }

            @Override
            public void onPreviousClick() {
                mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "Previous", null);
                if (playerBinder != null) {
                    playerBinder.previous();
                }

                OracleAnalytics.INSTANCE
                        .addLog(LogObject.newBuilder()
                                .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE_PLAY_PANEL_PREVIOUS)
                                .build());
            }

            @Override
            public void onCurrentListeningClick(Verse currentListeningVerse, MusicService.MediaForm mediaForm) {
                mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "StatusBar", null);
                switch (mediaForm) {
                    case NORMAL_VERSE:
                        VerseActivity.start(BookmarkedVerseActivity.this, QuranRepository.INSTANCE.getChapter(currentListeningVerse.getChapterId()).blockingFirst(), currentListeningVerse.getVerseId());
                        break;
                    case BOOKMARK:
                        smoothScrollToPosition(mVerseAdapter.getAdapterPositionByVerse(currentListeningVerse));
                        //BookmarkedVerseActivity.start(BookmarkedVerseActivity.this, currentListeningVerse);
                        break;
                }
            }
        });

        bindService(new Intent(this, MusicService.class), musicService, Service.BIND_AUTO_CREATE);
        mMusicServiceLogDelegate = new MusicServiceLogDelegate();

        refresh();
    }

    @Override
    protected void onDownloadRetryClick(Verse verse) {
        playVerse(verse, MusicService.MediaForm.NORMAL_VERSE);
    }

    private void downloadAllBookedVerse(final Verse verse) {
        if (!PhoneInfoUtils.isNetworkEnable(OracleApp.getInstance())) {
            ToastUtil.show(UiUtils.getText(R.string.no_internet_connection));
            return;
        }

        downloadDialogDelegate.unRegister();
        showVerseDownloading();
        verseDownloadDisposable = QuranRepository.INSTANCE.prepareBookmarkedVersesWithAudioResource()
                .flatMap(new Function<Boolean, ObservableSource<Verse>>() {
                    @Override
                    public ObservableSource<Verse> apply(@NonNull Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            return QuranRepository.INSTANCE.getVerseWithAudioResource(verse.getChapterId(), verse.getVerseId(), false);
                        } else {
                            throw new RuntimeException("Download failed");
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Verse>() {
                    @Override
                    public void accept(@NonNull Verse verse) throws Exception {
                        playVerse(verse, MusicService.MediaForm.BOOKMARK);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        showVerseDownloadRetry(verse);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        if (downloadStateDialog.isShowing()) {
                            downloadStateDialog.dismiss();
                        }
                    }
                });
    }

    private void showVerseDownloading() {
        if (downloadStateDialog == null) {
            downloadStateDialog = new DownloadStateDialog(this);
        }
        downloadStateDialog.setContent(this.getString(R.string.downloading));
        downloadStateDialog.setButtonText(this.getString(R.string.cancel));
        downloadStateDialog.setButtonEnabled(true);
        downloadStateDialog.setNetworkStateEnabled(true);
        downloadStateDialog.setCancelable(false);
        downloadStateDialog.setBottomButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (verseDownloadDisposable != null && !verseDownloadDisposable.isDisposed()) {
                    verseDownloadDisposable.dispose();
                    verseDownloadDisposable = null;
                }

                downloadStateDialog.dismiss();
            }
        });

        if (!downloadStateDialog.isShowing()) {
            downloadStateDialog.show();
        }
    }

    private void showVerseDownloadRetry(final Verse verse) {
        if (downloadStateDialog == null) {
            downloadStateDialog = new DownloadStateDialog(this);
        }

        downloadStateDialog.setContent(this.getString(R.string.download_failed));
        downloadStateDialog.setButtonText(this.getString(R.string.try_again));
        downloadStateDialog.setButtonEnabled(true);
        downloadStateDialog.setNetworkStateEnabled(false);
        downloadStateDialog.setCancelable(true);
        downloadStateDialog.setBottomButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadAllBookedVerse(verse);
            }
        });

        if (!downloadStateDialog.isShowing()) {
            downloadStateDialog.show();
        }
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

    private void playVerse(final Verse verse, final MusicService.MediaForm mediaForm) {
        if (verse == null) {
            return;
        }

        if (lastPlayVerseDisposable != null && !lastPlayVerseDisposable.isDisposed()) {
            lastPlayVerseDisposable.dispose();
            lastPlayVerseDisposable = null;
        }


        if (verse.getLyricOriginal() == null) {
            downloadAllBookedVerse(verse);
            return;
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThirdPartyAnalytics.INSTANCE.setCurrentScreen(this, "QuranBookmarkView");

        mVerseAdapter.notifyDataSetChanged();
        if (playerBinder != null && playerBinder.getPlayState() == PlaybackState.STATE_STOPPED) {
            mVerseAdapter.onPlayStop();
        }
        qsv.refreshVerseBookmarkCount();
        mMusicServiceLogDelegate.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMusicServiceLogDelegate.unBind();
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        dl.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        /*
        toolbar.inflateMenu(R.menu.menu_setting);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_setting:
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
        */
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
            getIntent().removeExtra(MediaNotificationHelper.INTENT_KEY_CLICK_ACTION);

            OracleAnalytics.INSTANCE
                    .addLog(LogObject.newBuilder()
                            .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                            .location(AnalyticsConstants.LOCATION.NOTIFICATION_BAR_OTHERAREA)
                            .build());
        }

        QuranRepository.INSTANCE
                .getBookmarkedVersesWithoutAudioResource()
                .compose(lifecycleProvider().<List<Verse>>bindUntilEvent(ScreenEvent.DESTROY))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Verse>>() {
                    @Override
                    public void accept(@NonNull List<Verse> verses) throws Exception {
                        mVerseAdapter.update(verses);
                        qsv.refreshTranslationSelection();

                        if (getIntent().hasExtra(INTENT_KEK_TARGET_VERSE)) {
                            //Scroll to last read position.
                            Verse verse = (Verse) getIntent().getSerializableExtra(INTENT_KEK_TARGET_VERSE);
                            int adapterPosition = mVerseAdapter.getAdapterPositionByVerse(verse);
                            if (adapterPosition >= 0 && adapterPosition < mVerseAdapter.getItemCount()) {
                                mLinearLayoutManager.scrollToPositionWithOffset(adapterPosition, 0);
                            }
                            getIntent().removeExtra(INTENT_KEK_TARGET_VERSE);
                        } else {
                            mLinearLayoutManager.scrollToPositionWithOffset(0, 0);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Timber.e(throwable, "refresh in BookmarkedVerseActivity failed");
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                    }
                });
    }

    private void updateControlPanel(boolean shouldUpdateListeningMode) {
        if (playerBinder != null) {
            Wrapper2<Verse, MusicService.MediaForm> currentSource = playerBinder.getCurrentSource();
            if (currentSource != null) {
                if (shouldUpdateListeningMode) {
                    vpcp.setListeningMode();
                    vpcp.updateCurrentListeningVerse(currentSource);
                }

                vpcp.setPlayButton(playerBinder.getPlayState() == PlaybackState.STATE_PLAYING);
            } else {
                vpcp.setReadingMode();
                Verse currentReadingVerse = getCurrentReadingVerse();
                if (currentReadingVerse != null) {
                    vpcp.updateCurrentReadingVerse(null, currentReadingVerse);
                }
            }
        }
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
    protected void onStart() {
        super.onStart();
        OracleAnalytics.INSTANCE
                .addLog(LogObject.newBuilder()
                        .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE)
                        .behaviour(AnalyticsConstants.BEHAVIOUR.ENTER)
                        .build());
    }

    @Override
    protected void onStop() {
        super.onStop();
        OracleAnalytics.INSTANCE
                .addLog(LogObject.newBuilder()
                        .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE)
                        .behaviour(AnalyticsConstants.BEHAVIOUR.LEAVE)
                        .build());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mMusicServiceLogDelegate.logEvent("QuranBookmarkView", "Return", null);
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

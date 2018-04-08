package co.muslimummah.android.module.quran.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.AnalyticsConstants;
import co.muslimummah.android.analytics.GA;
import co.muslimummah.android.analytics.LogObject;
import co.muslimummah.android.analytics.OracleAnalytics;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.QuranSetting;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.share.ShareUtils;
import co.muslimummah.android.module.quran.view.SelectableTextView;
import co.muslimummah.android.module.quran.view.VerseView;
import co.muslimummah.android.util.UiUtils;
import co.muslimummah.android.util.wrapper.Wrapper2;
import co.muslimummah.android.util.wrapper.Wrapper4;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by frank on 8/21/17.
 */

public class BookmarkedVerseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context mContext;
    private List<Verse> mList = new ArrayList<>();

    private OnPlayVerseListener playVerseListener;
    private OnVerseBookmarkClickListener mOnVerseBookmarkClickListener;
    private Wrapper2<Verse, Integer> progress;
//    private OnLoadActionListener mOnLoadActionListener;
    private OnVerseTitleClickListener mOnVerseTitleClickListener;
    private int mColorBlack;

    public BookmarkedVerseAdapter(Context context) {
        this.mContext = context;
        mColorBlack = ContextCompat.getColor(context, R.color.black);
    }

    //---------word by word----------
    private RecyclerView mRecyclerView;
    private WordTranslationInteract mInteract;
    private boolean mIsWordPopupShowing;

    public void setInteract(WordTranslationInteract interact) {
        mInteract = interact;
    }

    public void bindRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    public int getCurrentClickPosition() {
        return mCurrentClickPosition;
    }

    private int mCurrentClickPosition = -1;
    int mPositionInSingleVerse;
    Pair<Long, Long> mChapterVerseId;

    /**
     * clear current selected status
     */
    public void clearSelectedStatus() {
        mChapterVerseId = null;
        mPositionInSingleVerse = 0;
        BookmarkedVerseViewHolder holder = getCurrentBookmarkedVerseViewHolder();
        if (holder != null) {
            holder.tvOriginal.clearHighlightState();
            holder.verseOriginal.clearHighlightState();
        }

    }

//    public void moveBy(int num) {
//        mIsWordPopupShowing = true;
//        BookmarkedVerseViewHolder holder = getCurrentBookmarkedVerseViewHolder();
//        if (holder != null) {
//            holder.tvOriginal.moveBy(num);
//        }
//    }

    public void moveTo(int position) {
        mIsWordPopupShowing = true;
        BookmarkedVerseViewHolder holder = getCurrentBookmarkedVerseViewHolder();
        Timber.d("word move to %d", position);
        if (holder != null) {
            mPositionInSingleVerse = position;
            holder.tvOriginal.moveTo(position);
            holder.verseOriginal.moveTo(position);
        }
    }

    @Nullable
    private BookmarkedVerseViewHolder getCurrentBookmarkedVerseViewHolder() {
        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(getCurrentClickPosition());
        if (holder instanceof BookmarkedVerseViewHolder) {
            return (BookmarkedVerseViewHolder) holder;
        }
        return null;
    }

    private void show(int count, int clickPositionInVerse, long chapterId, long verseId) {
        if (mInteract != null) {
            mIsWordPopupShowing = true;
            mInteract.show(new Wrapper4<>(count,clickPositionInVerse,(int)chapterId,(int)verseId));
        }
    }

    //---------word by word----------


    public void update(List<Verse> verseList) {
        this.mList.clear();
        if (verseList != null) {
            this.mList.addAll(verseList);
        }

        notifyDataSetChanged();
    }

    public void update() {
        notifyDataSetChanged();
    }

    public void setPlayVerseListener(OnPlayVerseListener playVerseListener) {
        this.playVerseListener = playVerseListener;
    }

    public void setOnVerseBookmarkedListener(OnVerseBookmarkClickListener onVerseBookmarkClickListener) {
        this.mOnVerseBookmarkClickListener = onVerseBookmarkClickListener;
    }

//    public void setOnLoadActionListener(OnLoadActionListener onLoadActionListener) {
//        this.mOnLoadActionListener = onLoadActionListener;
//    }

    public void setOnVerseTitleClickListener(OnVerseTitleClickListener onVerseTitleClickListener) {
        this.mOnVerseTitleClickListener = onVerseTitleClickListener;
    }

    public void onPlayStop() {
        if (progress != null) {
            int itemIndex = getAdapterPositionByVerse(progress.entity1);
            progress = null;
            notifyItemChanged(itemIndex);
        }
    }

    public void updateProgress(Wrapper2<Verse, Integer> progress) {
        int previousIndexToUpdate = RecyclerView.NO_POSITION;
        int currentIndexToUpdate = getAdapterPositionByVerse(progress.entity1);
        if (this.progress != null) {
            previousIndexToUpdate = getAdapterPositionByVerse(this.progress.entity1);
        }
        this.progress = progress;
        if (previousIndexToUpdate != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousIndexToUpdate);
        }
        if (currentIndexToUpdate != RecyclerView.NO_POSITION) {
            notifyItemChanged(currentIndexToUpdate);
        }
    }

    public Verse getVerseByAdapterPosition(int position) {
        if (mList != null && position >= 0 && position < mList.size()) {
            return mList.get(position);
        }
        return null;
    }

    public int getAdapterPositionByVerse(Verse verse) {
        if (verse.getIsBookMarked()) {
            for (int i = 0; i < mList.size(); ++i) {
                if (mList.get(i).equals(verse)) {
                    return i;
                }
            }
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_quran_bookmarked_verse, parent, false);
        return new BookmarkedVerseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BookmarkedVerseViewHolder) {
            ((BookmarkedVerseViewHolder) holder).setData(mList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

//    public interface OnLoadActionListener {
//        void onLoadStart();
//
//        void onLoadError(Throwable t, Runnable retryRunnable);
//
//        void onLoadComplete();
//    }

    public interface OnVerseTitleClickListener {
        void onVerseTitleClicked(Chapter chapter, Verse verse);
    }

    class BookmarkedVerseViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.ll_verse_info)
        LinearLayout llVerseInfo;
        @BindView(R.id.tv_chapter_transliteration)
        TextView tvChapterTransliteration;
        @BindView(R.id.tv_chapter_verse_id)
        TextView tvChapterVerseId;
        @BindView(R.id.ib_bookmark)
        ImageButton ibBookmark;
        @BindView(R.id.iv_play)
        ImageView ivPlay;
        @BindView(R.id.tv_original)
        SelectableTextView tvOriginal;
        @BindView(R.id.verse_original)
        VerseView verseOriginal;
        @BindView(R.id.view_padding)
        View viewPadding;
        @BindView(R.id.fl_transliteration)
        FrameLayout flTransliteration;
        @BindView(R.id.tv_transliteration)
        TextView tvTransliteration;
        @BindView(R.id.verse_transliteration)
        VerseView verseTransliteration;
        @BindView(R.id.tv_translation)
        TextView tvTranslation;
        @BindView(R.id.iv_share)
        ImageView mIvShare;
        Verse currentItem, lastItem;
        Chapter currentChapter;

        BookmarkedVerseViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mInteract != null) {
                        mInteract.dismissPopup();
                    }
                }
            });

            tvOriginal.setTypeface(UiUtils.getArabicFont());
            tvTransliteration.setTypeface(UiUtils.getTransliterationFont());

            llVerseInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mOnVerseTitleClickListener != null && currentItem != null) {
                        mOnVerseTitleClickListener.onVerseTitleClicked(currentChapter, currentItem);
                    }
                }
            });

            ibBookmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mInteract != null) {
                        mInteract.dismissPopup();
                    }
                    if (currentItem != null) {
                        QuranRepository.INSTANCE.bookMarkVerse(currentItem, !currentItem.getIsBookMarked());
                        ibBookmark.setSelected(currentItem.getIsBookMarked());
                        if (mOnVerseBookmarkClickListener != null) {
                            mOnVerseBookmarkClickListener.onVerseBookmarkClicked(currentItem);
                        }
                    }
                }
            });

            ivPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (mInteract != null) {
                        mInteract.dismissPopup();
                    }
                    OracleAnalytics.INSTANCE
                            .addLog(LogObject.newBuilder()
                                    .behaviour(AnalyticsConstants.BEHAVIOUR.CLICK)
                                    .location(AnalyticsConstants.LOCATION.BOOKMARK_PAGE_PLAY_ICON)
                                    .target(AnalyticsConstants.TARGET_TYPE.VERSE_ID, currentItem.getChapterId() + ":" + currentItem.getVerseId())
                                    .build());

                    playAudio(currentItem);
                }

                private void playAudio(Verse verse) {
                    if (playVerseListener != null) {
                        playVerseListener.play(verse);
                    }
                }
            });
            mIvShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mInteract != null) {
                        mInteract.dismissPopup();
                        ShareUtils.shareSingleVerse(mInteract.getActivity(), currentItem.getChapterId(), currentItem.getVerseId());
                    }
                }
            });
        }

        public void setData(Verse verse) {
            this.currentItem = verse;
            if (currentChapter == null || currentChapter.getChapterId() != currentItem.getChapterId()) {
                currentChapter = QuranRepository.INSTANCE.getChapter(currentItem.getChapterId()).blockingFirst();
            }
            ibBookmark.setSelected(Boolean.TRUE.equals(currentItem.getIsBookMarked()));
            tvChapterTransliteration.setText(currentChapter.getTransliteration());
            tvChapterVerseId.setText(String.format(Locale.US, "%d:%d", currentItem.getChapterId(), currentItem.getVerseId()));
            //word by word
            if (!currentItem.equals(lastItem)) {
                tvOriginal.setData(currentItem.getOriginal());
            }
            lastItem = currentItem;
            //word by word for lyrics
//            verseViewOriginal.setWordParams(new Wrapper3<>(getLayoutPosition(), currentItem.getChapterId(), currentItem.getVerseId()));

            //For visibility of transliteration and translation.
            viewPadding.setVisibility(View.VISIBLE);
            flTransliteration.setVisibility(QuranSetting.isTransliterationEnabled(mContext) ? View.VISIBLE : View.GONE);
            tvTransliteration.setText(currentItem.getTransliteration());
            if (QuranSetting.isTranslationEnabled(mContext)) {
                tvTranslation.setText(currentItem.getTranslation(mContext));
                tvTranslation.setVisibility(View.VISIBLE);
            } else {
                tvTranslation.setVisibility(View.GONE);
                if (!flTransliteration.isShown()) {
                    viewPadding.setVisibility(View.GONE);
                }
            }

            updateVerseUI();

            //for break line bug
            if (currentItem.getLyricOriginal() == null && QuranRepository.INSTANCE.isLrcExist(currentItem)) {
                final Verse source = currentItem;
                QuranRepository.INSTANCE.getVerseWithAudioResource(currentItem.getChapterId(), currentItem.getVerseId(), false)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Verse>() {
                            @Override
                            public void accept(@NonNull Verse verse) throws Exception {
                                source.setLyricOriginal(verse.getLyricOriginal());
                                source.setLyricTransliteration(verse.getLyricTransliteration());
                                if (source.equals(currentItem)) {
                                    updateVerseUI();
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                            }
                        });
            }

            //word by word
            tvOriginal.setLongClickListener(new SelectableTextView.LongClickListener() {
                @Override
                public void onLongClick( int positionInSingleVerse) {
                    clearSelectedStatus();
                    mCurrentClickPosition = getLayoutPosition();
                    mPositionInSingleVerse = positionInSingleVerse;
                    Verse clickVerse = mList.get(mCurrentClickPosition);
                    mChapterVerseId = new Pair<>(clickVerse.getChapterId(), clickVerse.getVerseId());
                    show(tvOriginal.count(), positionInSingleVerse, clickVerse.getChapterId(), clickVerse.getVerseId());
                    gaTapWord(true,  clickVerse.getChapterId() + "_" + clickVerse.getVerseId()  + "_" + positionInSingleVerse);
                }
            });
            verseOriginal.setLongClickListener(new SelectableTextView.LongClickListener() {
                @Override
                public void onLongClick(int positionInSingleVerse) {
                    clearSelectedStatus();
                    mCurrentClickPosition = getLayoutPosition();
                    mPositionInSingleVerse = positionInSingleVerse;
                    Verse clickVerse = mList.get(mCurrentClickPosition);
                    mChapterVerseId = new Pair<>(clickVerse.getChapterId(), clickVerse.getVerseId());
                    show(tvOriginal.count(), positionInSingleVerse, clickVerse.getChapterId(), clickVerse.getVerseId());
                    gaTapWord(false,  clickVerse.getChapterId() + "_" + clickVerse.getVerseId()  + "_" + positionInSingleVerse);
                }
            });
        }

        private void gaTapWord(boolean isReading, String chapterId_verseId_Position) {
            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.QuranBookmarkView,
                            isReading ? GA.Action.Reading : GA.Action.AudioPlaying,
                            GA.Label.TapWord);
            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.QuranBookmarkView,
                            GA.Action.TapWord,
                            chapterId_verseId_Position);
        }

        private void updateVerseUI() {
            if (progress != null && progress.entity1.equals(currentItem)) {
                if (QuranSetting.isAudioSyncEnabled(mContext) && currentItem.getLyricOriginal() != null) {
                    setToKaraokeEffect(true);
                } else {
                    setToNormalEffect(true);
                }
            } else if (currentItem.getLyricOriginal() != null) {
                setToKaraokeEffect(false);
            } else {
                setToNormalEffect(false);
            }
        }

        private boolean useOrigin = true;

        private void setToKaraokeEffect(boolean karaokeMode) {

            if (useOrigin) {
                if (tvOriginal.getTextColors() != ColorStateList.valueOf(Color.TRANSPARENT)) {
                    tvOriginal.setTextColor(Color.TRANSPARENT);
                }
            } else {
                tvOriginal.setVisibility(View.INVISIBLE);
            }


            tvTransliteration.setVisibility(View.INVISIBLE);

            verseOriginal.setVisibility(View.VISIBLE);
            verseOriginal.setVerseLyric(currentItem.getLyricOriginal(), true);
            if (mChapterVerseId != null
                    && mChapterVerseId.first == currentItem.getChapterId()
                    && mChapterVerseId.second == currentItem.getVerseId()) {
                verseOriginal.moveTo(mPositionInSingleVerse);
            }

            verseTransliteration.setVisibility(View.VISIBLE);
            verseTransliteration.setVerseLyric(currentItem.getLyricTransliteration());


            verseOriginal.setKaraokeMode(karaokeMode);
            verseTransliteration.setKaraokeMode(karaokeMode);
            if (karaokeMode) {
                verseOriginal.updateProgress(progress.entity2);
                verseTransliteration.updateProgress(progress.entity2);
            }

            itemView.setBackgroundResource(karaokeMode ? R.color.orange_light : R.color.white);
        }

        private void setToNormalEffect(boolean isBackgroundHighlighted) {
            if (useOrigin) {
                tvOriginal.setTextColor(mColorBlack);
            }

            verseTransliteration.setVisibility(View.GONE);
            verseOriginal.setVisibility(View.GONE);

            tvOriginal.setVisibility(View.VISIBLE);
            tvTransliteration.setVisibility(View.VISIBLE);

            itemView.setBackgroundResource(isBackgroundHighlighted ? R.color.orange_light : R.color.white);
        }
    }
}

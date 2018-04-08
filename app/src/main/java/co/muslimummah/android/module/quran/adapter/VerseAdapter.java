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
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.AnalyticsConstants.BEHAVIOUR;
import co.muslimummah.android.analytics.AnalyticsConstants.LOCATION;
import co.muslimummah.android.analytics.AnalyticsConstants.TARGET_TYPE;
import co.muslimummah.android.analytics.GA;
import co.muslimummah.android.analytics.LogObject;
import co.muslimummah.android.analytics.OracleAnalytics;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.module.quran.model.JuzInfo;
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
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by frank on 8/12/17.
 */

public class VerseAdapter extends RecyclerView.Adapter<VerseAdapter.BaseVerseViewHolder> {
    private static final int VIEW_TYPE_OPENING_MESSAGE = 1;
    private static final int VIEW_TYPE_VERSE = 2;

    private Context mContext;
    private List<Verse> mList;
    private Wrapper2<Verse, Integer> progress;
    private OnPlayVerseListener playVerseListener;
    private OnVerseBookmarkClickListener mOnVerseBookmarkClickListener;
    private OnLoadActionListener mOnLoadActionListener;

    public VerseAdapter(Context context) {
        this.mContext = context;
        mColorBlack = ContextCompat.getColor(context, R.color.black);
    }

    //---------word by word----------
    private RecyclerView mRecyclerView;
    private WordTranslationInteract mInteract;
    private boolean mIsWordPopupShowing;
    private int mColorBlack;

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
        NormalVerseViewHolder holder = getCurrentNormalVerseViewHolder();
        mChapterVerseId = null;
        mPositionInSingleVerse = 0;

        if (holder != null) {
            holder.tvOriginal.clearHighlightState();
            holder.verseViewOriginal.clearHighlightState();
        }

    }

    public void moveTo(int position) {
        mIsWordPopupShowing = true;
        NormalVerseViewHolder holder = getCurrentNormalVerseViewHolder();
        if (holder != null) {
            Timber.d("word move to %d", position);
            mPositionInSingleVerse = position;
            holder.tvOriginal.moveTo(position);
            holder.verseViewOriginal.moveTo(position);
        }
    }

    @Nullable
    private NormalVerseViewHolder getCurrentNormalVerseViewHolder() {
        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(getCurrentClickPosition());
        if (holder instanceof NormalVerseViewHolder) {
            return (NormalVerseViewHolder) holder;
        }
        return null;
    }

    private void show(int count, int clickPositionInVerse, long chapterId, long verseId) {
        if (mInteract != null) {
            mIsWordPopupShowing = true;
            mInteract.show(new Wrapper4<>(count, clickPositionInVerse, (int) chapterId, (int) verseId));
        }
    }

    //---------word by word----------
    public void update(List<Verse> verseList) {
        this.mList = verseList;
        notifyDataSetChanged();
    }

    public void update() {
//        this.mContext = context;
        notifyDataSetChanged();
    }

    public Verse getVerseByAdapterPosition(int position) {
        if (mList != null && position >= 0 && position < mList.size()) {
            return mList.get(position);
        }
        return null;
    }

    public int getAdapterPositionByVerse(Verse verse) {
//        if (mList != null) {
//            for (int i = 0; i < mList.size(); ++i) {
//                if (mList.get(i).equals(verse)) {
//                    return i;
//                }
//            }
//        }
        if (mList != null && mList.size() > 0 && mList.get(0).getChapterId() == verse.getChapterId()) {
            if (mList.get(0).getVerseId() == 0) {
                return (int) verse.getVerseId();
            } else {
                return (int) (verse.getVerseId() - 1);
            }

        }
        return RecyclerView.NO_POSITION;
    }

    public int getAdapterPositionByVerseId(long verseId) {
        if (mList != null && mList.size() > 0) {
            if (mList.get(0).getVerseId() == 0) {
                return (int) verseId;
            } else {
                return (int) (verseId - 1);
            }

        }
        return RecyclerView.NO_POSITION;
    }

    /**
     * The opening verse is with the verse_id of 0.
     *
     * @return
     */
    public List<Verse> getRealVerses() {
        if (mList != null && mList.size() > 0) {
            return mList.subList(mList.get(0).getVerseId() == 0 ? 1 : 0, mList.size());
        } else {
            return null;
        }
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

    public void setPlayVerseListener(OnPlayVerseListener playVerseListener) {
        this.playVerseListener = playVerseListener;
    }

    public void setOnVerseBookmarkedListener(OnVerseBookmarkClickListener onVerseBookmarkClickListener) {
        this.mOnVerseBookmarkClickListener = onVerseBookmarkClickListener;
    }

    public void setOnLoadActionListener(OnLoadActionListener onLoadActionListener) {
        this.mOnLoadActionListener = onLoadActionListener;
    }

    @Override
    public BaseVerseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == VIEW_TYPE_VERSE) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_quran_verse, parent, false);
            return new NormalVerseViewHolder(itemView);
        }
        itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_quran_opening_message, parent, false);
        return new OpeningMessageViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(BaseVerseViewHolder holder, int position) {
        holder.setData(mList.get(position));
    }

    @Override
    public int getItemCount() {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mList.get(position).getVerseId() > 0 ? VIEW_TYPE_VERSE : VIEW_TYPE_OPENING_MESSAGE;
    }

    public abstract class BaseVerseViewHolder extends RecyclerView.ViewHolder {
        public BaseVerseViewHolder(View itemView) {
            super(itemView);
        }

        public abstract void setData(Verse verse);
    }

    public class OpeningMessageViewHolder extends BaseVerseViewHolder {
        Verse currentItem;

        public OpeningMessageViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void setData(Verse verse) {
            this.currentItem = verse;
            if (progress != null && progress.entity1.equals(currentItem)) {
                itemView.setBackgroundResource(R.color.orange_light);
            } else {
                itemView.setBackgroundResource(R.color.white);
            }
        }
    }

    public interface OnLoadActionListener {
        void onLoadStart();

        void onLoadError(Throwable t, Runnable retryRunnable);

        void onLoadComplete();
    }

    public class NormalVerseViewHolder extends BaseVerseViewHolder {
        @BindView(R.id.rl_juz_info)
        RelativeLayout rlJuzInfo;
        @BindView(R.id.tv_juz_english)
        TextView tvJuzEnglish;
        @BindView(R.id.tv_juz_original)
        TextView tvJuzOriginal;
        @BindView(R.id.tv_chapter_id)
        TextView tvChapterId;
        @BindView(R.id.ib_bookmark)
        ImageButton ibBookmark;
        @BindView(R.id.iv_play)
        ImageView ivPlay;
        @BindView(R.id.tv_original)
        SelectableTextView tvOriginal;
        @BindView(R.id.verse_original)
        VerseView verseViewOriginal;
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

        public NormalVerseViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mInteract != null) {
                        mInteract.dismissPopup();
                    }
                    Timber.d("TextView  itemView onClick");
                }
            });
            tvOriginal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mInteract != null) {
                        mInteract.dismissPopup();
                    }

                    Timber.d("TextView tvOriginal onClick");
                }
            });
            tvOriginal.setTypeface(UiUtils.getArabicFont());
            tvTransliteration.setTypeface(UiUtils.getTransliterationFont());
            ibBookmark.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mInteract != null) {
                        mInteract.dismissPopup();
                    }
                    QuranRepository.INSTANCE.bookMarkVerse(currentItem, !currentItem.getIsBookMarked());
                    ibBookmark.setSelected(currentItem.getIsBookMarked());
                    if (mOnVerseBookmarkClickListener != null) {
                        mOnVerseBookmarkClickListener.onVerseBookmarkClicked(currentItem);
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
                                    .behaviour(BEHAVIOUR.CLICK)
                                    .location(LOCATION.QURAN_VERSE_VIEW_PAGE_PLAY_ICON)
                                    .target(TARGET_TYPE.VERSE_ID, currentItem.getChapterId() + ":" + currentItem.getVerseId())
                                    .build());

                    if (currentItem.getLyricOriginal() == null) {
                        if (mOnLoadActionListener != null) {
                            mOnLoadActionListener.onLoadStart();
                        }
                        QuranRepository.INSTANCE.getVerseWithAudioResource(currentItem.getChapterId(), currentItem.getVerseId(), true)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<Verse>() {
                                    @Override
                                    public void accept(@NonNull Verse verse) throws Exception {
                                        currentItem.setLyricOriginal(verse.getLyricOriginal());
                                        currentItem.setLyricTransliteration(verse.getLyricTransliteration());
                                        playAudio(currentItem);
                                    }
                                }, new Consumer<Throwable>() {
                                    @Override
                                    public void accept(@NonNull Throwable throwable) throws Exception {
                                        if (mOnLoadActionListener != null) {
                                            mOnLoadActionListener.onLoadError(throwable, new Runnable() {
                                                @Override
                                                public void run() {
                                                    ivPlay.performClick();
                                                }
                                            });
                                        }
                                    }
                                }, new Action() {
                                    @Override
                                    public void run() throws Exception {
                                        if (mOnLoadActionListener != null) {
                                            mOnLoadActionListener.onLoadComplete();
                                        }
                                    }
                                });
                    } else {
                        playAudio(currentItem);
                    }
                }

                private void playAudio(Verse verse) {
                    //// TODO: 8/14/17 Here we confirm we have Lyric model inside Verse model. currentItem.getLyricOriginal() and currentItem.getLyricTransliteration()
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

        @Override
        public void setData(Verse verse) {
            this.currentItem = verse;
            JuzInfo juzInfo = QuranRepository.INSTANCE.getJuzInfo(verse);
            if (juzInfo == null) {
                rlJuzInfo.setVisibility(View.GONE);
            } else {
                tvJuzEnglish.setText(juzInfo.getJuzEnglish());
                tvJuzOriginal.setText(juzInfo.getJuzOriginal());
                rlJuzInfo.setVisibility(View.VISIBLE);
            }
            ibBookmark.setSelected(Boolean.TRUE.equals(currentItem.getIsBookMarked()));
            tvChapterId.setText(String.valueOf(currentItem.getVerseId()));

            //word by word
            if (!currentItem.equals(lastItem)/*Do not setData when play mp3*/) {
                tvOriginal.setData(currentItem.getOriginal());
            }
            lastItem = currentItem;
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
                public void onLongClick(int positionInSingleVerse) {
                    clearSelectedStatus();
                    mPositionInSingleVerse = positionInSingleVerse;
                    mCurrentClickPosition = getLayoutPosition();
                    Verse clickVerse = mList.get(mCurrentClickPosition);
                    mChapterVerseId = new Pair<>(clickVerse.getChapterId(), clickVerse.getVerseId());
                    show(tvOriginal.count(), positionInSingleVerse, clickVerse.getChapterId(), clickVerse.getVerseId());
                    gaTapWord(true, clickVerse.getChapterId() + "_" + clickVerse.getVerseId() + "_" + positionInSingleVerse);
                }
            });

            verseViewOriginal.setLongClickListener(new SelectableTextView.LongClickListener() {
                @Override
                public void onLongClick(int positionInSingleVerse) {
                    clearSelectedStatus();

                    mPositionInSingleVerse = positionInSingleVerse;
                    mCurrentClickPosition = getLayoutPosition();

                    Verse clickVerse = mList.get(mCurrentClickPosition);
                    mChapterVerseId = new Pair<>(clickVerse.getChapterId(), clickVerse.getVerseId());
                    show(tvOriginal.count(), positionInSingleVerse, clickVerse.getChapterId(), clickVerse.getVerseId());
                    gaTapWord(false, clickVerse.getChapterId() + "_" + clickVerse.getVerseId() + "_" + positionInSingleVerse);
                }
            });
        }

        private void gaTapWord(boolean isReading, String chapterId_verseId_Position) {
            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.QuranVerseView,
                            isReading ? GA.Action.Reading : GA.Action.AudioPlaying,
                            GA.Label.TapWord);
            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.QuranVerseView,
                            GA.Action.TapWord,
                            chapterId_verseId_Position);
        }

        private void updateVerseUI() {
            if (progress != null && progress.entity1.equals(currentItem)) {
                if (QuranSetting.isAudioSyncEnabled(mContext) && currentItem.getLyricOriginal() != null) {
                    setToKaraokeEffect(true);
                    Timber.d("setToKaraokeEffect(true)");
                } else {
                    setToNormalEffect(true);
                    Timber.d("setToNormalEffect(true)");
                }
            } else if (currentItem.getLyricOriginal() != null) {
                setToKaraokeEffect(false);
                Timber.d("setToKaraokeEffect(false)");
            } else {
                setToNormalEffect(false);
                Timber.d("setToNormalEffect(false)");
            }
        }

        private boolean useOrigin = false;

        /**
         * @param karaokeMode if the verse was played,params is false.
         */
        private void setToKaraokeEffect(boolean karaokeMode) {
            if (useOrigin) {
                if (tvOriginal.getTextColors() != ColorStateList.valueOf(Color.TRANSPARENT)) {
                    tvOriginal.setTextColor(Color.TRANSPARENT);
                }
            } else {
                tvOriginal.setVisibility(View.INVISIBLE);
            }

            tvTransliteration.setVisibility(View.INVISIBLE);

            verseViewOriginal.setVisibility(View.VISIBLE);
            verseViewOriginal.setVerseLyric(currentItem.getLyricOriginal(), true);
            if (mChapterVerseId != null
                    && mChapterVerseId.first == currentItem.getChapterId()
                    && mChapterVerseId.second == currentItem.getVerseId()) {
                Timber.tag("findout").d("restore position is %d", mPositionInSingleVerse);
                verseViewOriginal.moveTo(mPositionInSingleVerse);
            }

            verseTransliteration.setVisibility(View.VISIBLE);
            verseTransliteration.setVerseLyric(currentItem.getLyricTransliteration());
            verseViewOriginal.setKaraokeMode(karaokeMode);
            verseTransliteration.setKaraokeMode(karaokeMode);
            if (karaokeMode) {
                verseViewOriginal.updateProgress(progress.entity2);
                verseTransliteration.updateProgress(progress.entity2);
            }
            itemView.setBackgroundResource(karaokeMode ? R.color.orange_light : R.color.white);
        }


        private void setToNormalEffect(boolean isBackgroundHighlighted) {
            if (useOrigin) {
                tvOriginal.setTextColor(mColorBlack);
            }

            verseTransliteration.setVisibility(View.GONE);
            verseViewOriginal.setVisibility(View.GONE);


            tvOriginal.setVisibility(View.VISIBLE);

            tvTransliteration.setVisibility(View.VISIBLE);

            itemView.setBackgroundResource(isBackgroundHighlighted ? R.color.orange_light : R.color.white);
        }
    }
}

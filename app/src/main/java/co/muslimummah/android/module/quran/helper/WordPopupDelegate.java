package co.muslimummah.android.module.quran.helper;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;

import java.util.List;

import co.muslimummah.android.analytics.GA;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.NetObserver;
import co.muslimummah.android.module.quran.adapter.WordTranslationInteract;
import co.muslimummah.android.module.quran.model.TranslationWord;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.share.ShareUtils;
import co.muslimummah.android.module.quran.view.WordPopupWindow;
import co.muslimummah.android.util.wrapper.Wrapper4;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by tysheng
 * Date: 26/9/17 3:27 PM.
 * Email: tyshengsx@gmail.com
 */

public class WordPopupDelegate implements WordTranslationInteract {
    //word by word
    private WordPopupWindow mWordPopup;
    private boolean mIsWordPopupShowing;

    // count, clickPositionInVerse, chapterId, verseId
    private Wrapper4<Integer, Integer, Integer, Integer> mShownEntity;

    public void destroy() {
        mWordPopup = null;
    }

    private Context getContext() {
        return mInteract.getAnchorView().getContext();
    }

    @Override
    public void show(Wrapper4<Integer, Integer, Integer, Integer> shownEntity) {
        initPopup();
        if (mWordPopup.isShowing()) {
            mWordPopup.dismiss();
        }
        mShownEntity = shownEntity;
        gaPosition = shownEntity.entity2;
        mWordPopup.showAtLocation(mInteract.getAnchorView(), Gravity.BOTTOM, 0, 0);//-(UiUtils.dp2px(248) + UiUtils.getNavigationBarHeight(getContext()))
        mIsWordPopupShowing = true;
        Timber.d("getWords %d, %d", mShownEntity.entity3, mShownEntity.entity4);
        requestWordData(true);
    }

    @Override
    public FragmentActivity getActivity() {
        return (FragmentActivity) mInteract;
    }


    public interface Interact {
        void clearSelectedStatus();

        void playWord(TranslationWord word);

        void moveTo(int position);

        View getAnchorView();

        void requestWordData(final boolean firstTime);

    }

    private Interact mInteract;

    public void setInteract(Interact interact) {
        mInteract = interact;
    }

    public boolean isWordPopupShowing() {
        return mIsWordPopupShowing;
    }

    private int gaPosition = -1;

    private void gaPreOrNextWord(int position) {
        Timber.d("gaPreOrNextWord %d %d pre %b", gaPosition, position, position < gaPosition);
        if (position == gaPosition) return;
        boolean isPre = position < gaPosition;
        gaPosition = position;
        ThirdPartyAnalytics.INSTANCE
                .logEvent(getMode() == WordPopupWindow.Mode.NORMAL ? GA.Category.QuranVerseView : GA.Category.QuranBookmarkView,
                        GA.Action.WordByWord,
                        isPre ? GA.Label.PreviousArabicWord : GA.Label.NextArabicWord);
    }

    private void initPopup() {
        if (mWordPopup == null) {
            mWordPopup = new WordPopupWindow(getContext());
            mWordPopup.addOnPageChangedListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    int realPosition = mWordPopup.getCount() - 1 - position;
                    if (mInteract != null) {
                        mInteract.moveTo(realPosition);
                    }
                    gaPreOrNextWord(realPosition);
                }
            });
            mWordPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    if (mInteract != null) {
                        mInteract.clearSelectedStatus();
                    }
                }
            });
            mWordPopup.setOnPlayWordListener(new WordPopupWindow.OnPlayWordListener() {
                @Override
                public void play(TranslationWord word) {
                    if (mInteract != null) {
                        mInteract.playWord(word);
                        gaPlayWord();
                    }
                }
            });
            mWordPopup.setOnShareWordListener(new WordPopupWindow.OnShareWordListener() {
                @Override
                public void share(TranslationWord word) {
                    if (mInteract instanceof FragmentActivity) {
                        ShareUtils.shareSingleWord(getActivity(), word.getChapterNum(), word.getVerseNum(), word.getLetterNum());
                    }
                }
            });
        }
    }

    private void gaPlayWord() {
        ThirdPartyAnalytics.INSTANCE
                .logEvent(getMode() == WordPopupWindow.Mode.NORMAL ? GA.Category.QuranVerseView : GA.Category.QuranBookmarkView,
                        GA.Action.WordByWord,
                        GA.Label.PlayWord);
    }

    private WordPopupWindow.Mode mMode;

    public WordPopupWindow.Mode getMode() {
        return mMode;
    }

    public void setMode(WordPopupWindow.Mode mode) {
        mMode = mode;
        if (mWordPopup != null) {
            mWordPopup.setMode(mode);
        }
    }

    @Override
    public void dismissPopup() {
        dismissWordPop();
    }

    /**
     * @param firstTime false = try again
     */
    public void requestWordData(final boolean firstTime) {
        if (mShownEntity == null) return;
        QuranRepository.INSTANCE
                .getWords(mShownEntity.entity3, mShownEntity.entity4)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        if (mWordPopup != null) {
                            if (firstTime) {
                                mWordPopup.setFakeData(mShownEntity.entity1, mShownEntity.entity2);
                            } else {
                                mWordPopup.setDownloading();
                            }
                        }
                    }
                })
                .subscribe(new NetObserver<List<TranslationWord>>() {
                    @Override
                    public void onNext(@NonNull List<TranslationWord> translationWords) {
                        super.onNext(translationWords);
                        if (mWordPopup != null) {
                            mWordPopup.setData(translationWords, mShownEntity.entity2);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        super.onError(e);
                        if (mWordPopup != null) {
                            mWordPopup.setDownloadFailed();
                        }
                        Timber.d("getWords onError %s", e.getMessage());
                    }
                });
    }

    public void dismissWordPop() {
        //word by word popup dismiss,
        if (mInteract != null) {
            mInteract.clearSelectedStatus();
        }
        if (mIsWordPopupShowing && mWordPopup != null) {
            mIsWordPopupShowing = false;
            mWordPopup.dismiss();
        }
    }
}

package co.muslimummah.android.module.quran.model.repository;

import android.support.v4.util.LruCache;

import java.util.List;
import java.util.Locale;

import co.muslimummah.android.network.ApiFactory;
import co.muslimummah.android.network.ApiService;
import co.muslimummah.android.module.quran.model.TranslationVerse;
import co.muslimummah.android.module.quran.model.TranslationVerseDao;
import co.muslimummah.android.module.quran.model.TranslationWord;
import co.muslimummah.android.module.quran.model.TranslationWordDao;
import co.muslimummah.android.storage.GreenDaoModule;
import co.muslimummah.android.util.Collections;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import timber.log.Timber;

/**
 * Created by tysheng
 * Date: 20/9/17 4:36 PM.
 * Email: tyshengsx@gmail.com
 */

class WordRepo {
    Observable<List<TranslationWord>> getWords(final int chapterId, final int verseId) {
        // check in lru cache
        Observable<List<TranslationWord>> lruObservable = Observable.empty();
        TranslationVerse verse = getVerseLruCache().get(getFormatKey(chapterId, verseId));
        Timber.d("getWords from lru %b", verse != null);
        if (verse != null) {
            lruObservable = Observable.just(verse.getWords());
        }
        // check in db
        Observable<List<TranslationWord>> dbObservable;
        dbObservable = Observable.create(new ObservableOnSubscribe<List<TranslationWord>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<TranslationWord>> e) throws Exception {
                TranslationVerseDao verseDao = GreenDaoModule.getDaoSession().getTranslationVerseDao();

                TranslationVerse source = verseDao.queryBuilder().where(TranslationVerseDao.Properties.ChapterId.eq(chapterId),
                        TranslationVerseDao.Properties.VerseId.eq(verseId)).unique();
                Timber.d("getWords from db %b", source != null);
                if (source != null) {
                    cacheInLru(source);
                    e.onNext(source.getWords());
                }
                e.onComplete();

            }
        });
        // get from internet
        Observable<List<TranslationWord>> netObservable = ApiFactory.get(ApiService.class)
                .getQuranWords(chapterId, verseId).doOnNext(new Consumer<List<TranslationWord>>() {
                    @Override
                    public void accept(@NonNull List<TranslationWord> translationWords) throws Exception {
                        Timber.d("getWords from internet %b", translationWords != null);
                        saveInDb(translationWords);
                    }
                });


        return Observable.concat(lruObservable, dbObservable, netObservable)
                .firstOrError().toObservable();

    }

    /**
     * cache in memory and db
     */
    private void cacheInLru(TranslationVerse verse) {
        String key = getFormatKey(verse.getChapterId(), verse.getVerseId());
        getVerseLruCache().put(key, verse);
    }

    private TranslationVerse listToVerse(List<TranslationWord> translationWords) {
        if (!Collections.isEmpty(translationWords)) {
            TranslationWord word = translationWords.get(0);
            TranslationVerse verse =
                    new TranslationVerse(null, word.getChapterNum(), word.getVerseNum(), System.currentTimeMillis());
            verse.setWords(translationWords);
            return verse;
        }
        return null;
    }


    private String getFormatKey(int chapterId, int verseId) {
        return String.format(Locale.US, "%d_%d", chapterId, verseId);
    }

    private void saveInDb(List<TranslationWord> translationWords) {
        final TranslationVerse verse = listToVerse(translationWords);
        if (verse == null) return;
        // transaction
        GreenDaoModule.getDaoSession().runInTx(new Runnable() {
            @Override
            public void run() {
                TranslationVerseDao dao = GreenDaoModule.getDaoSession().getTranslationVerseDao();
                dao.save(verse);
                TranslationWordDao wordDao = GreenDaoModule.getDaoSession().getTranslationWordDao();
                wordDao.saveInTx(verse.getWords());
            }
        });
    }

    private LruCache<String, TranslationVerse> mVerseLruCache;

    private LruCache<String, TranslationVerse> getVerseLruCache() {
        if (mVerseLruCache == null) {
            mVerseLruCache = new LruCache<>(16);
        }
        return mVerseLruCache;
    }
}

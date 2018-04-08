package co.muslimummah.android.module.quran.model.repository;

import android.support.annotation.NonNull;

import java.util.List;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.ChapterDao;
import co.muslimummah.android.storage.GreenDaoModule;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * Created by frank on 8/9/17.
 */
class ChapterRepo {
    private List<Chapter> mChapters;

    Observable<List<Chapter>> getChapters() {
        return Observable.create(new ObservableOnSubscribe<List<Chapter>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<Chapter>> e) throws Exception {
                if (mChapters != null) {
                    e.onNext(mChapters);
                    e.onComplete();
                    return;
                }
                ChapterDao chapterDao = GreenDaoModule.getDaoSession().getChapterDao();
                Chapter firstChapter = chapterDao.loadByRowId(1);
                if (firstChapter == null) {
                    mChapters = QuranDataUtils.getChaptersFromInputStream(OracleApp.getInstance().getAssets().open("quran/chapter/chapter.txt"));
                } else {
                    mChapters = chapterDao.loadAll();
                }
                e.onNext(mChapters);
                e.onComplete();

                if (firstChapter == null) {
                    chapterDao.saveInTx(mChapters);
                }
            }
        });
    }

    Observable<Chapter> getChapter(final long chapterId) {
        if (mChapters != null) {
            return Observable.fromIterable(mChapters)
                    .filter(new Predicate<Chapter>() {
                        @Override
                        public boolean test(@NonNull Chapter chapter) throws Exception {
                            return chapter.getChapterId() == chapterId;
                        }
                    });
        }
        if (isChapterDbEmpty()) {
            return getChapters()
                    .flatMapIterable(new Function<List<Chapter>, Iterable<? extends Chapter>>() {
                        @Override
                        public Iterable<? extends Chapter> apply(@NonNull List<Chapter> chapters) throws Exception {
                            return chapters;
                        }
                    })
                    .filter(new Predicate<Chapter>() {
                        @Override
                        public boolean test(@NonNull Chapter chapter) throws Exception {
                            return chapter.getChapterId() == chapterId;
                        }
                    });
        } else {
            return Observable.just(chapterId)
                    .map(new Function<Long, Chapter>() {
                        @Override
                        public Chapter apply(@NonNull Long chapterId) throws Exception {
                            ChapterDao chapterDao = GreenDaoModule.getDaoSession().getChapterDao();
                            return chapterDao.queryBuilder()
                                    .where(ChapterDao.Properties.ChapterId.eq(chapterId))
                                    .uniqueOrThrow();
                        }
                    });
        }
    }

    private boolean isChapterDbEmpty() {
        ChapterDao chapterDao = GreenDaoModule.getDaoSession().getChapterDao();
        return chapterDao.loadByRowId(1) == null;
    }
}

package co.muslimummah.android.util;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import java.util.List;

import co.muslimummah.android.module.prayertime.ui.activity.MainActivity;
import co.muslimummah.android.module.quran.activity.BookmarkedVerseActivity;
import co.muslimummah.android.module.quran.activity.VerseActivity;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.observers.ResourceObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by tysheng
 * Date: 19/9/17 12:39 PM.
 * Email: tyshengsx@gmail.com
 */

public class SchemeUtils {

    public static final String MAIN_EXTRA_BUNDLE = "MAIN_EXTRA_BUNDLE";

    public static final String EXTRA_URL = "scheme";

    public static final String HOST = "app.muslimummah.co";
    public static final String SCHEME = "https";
    public static final String FB_SCHEME = "muslimummah";
    public static final String PRAYER_TIMES = "prayer_times";
    public static final String CHAPTER_LIST = "chapter_list";
    public static final String VERSE_LIST = "verse_list";
    public static final String BOOKMARK = "bookmark";
    public static final String QIBLA = "qibla";
    public static final String CALENDAR_EXPANDED = "calendar_expanded";
    public static final String TAB = "tab";
    public static final String CHAPTER_NUMBER = "chapter_number";
    public static final String VERSE_NUMBER = "verse_number";
    public static final String CALENDAR_STATUS_EXPANDED = "1";
    public static final String CALENDAR_STATUS_COLLAPSE = "0";

    public static final int VERSE_LAST_TIME_VISITED = -1;

    /**
     * check if valid
     */
    private static boolean checkUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        Timber.d("scheme is %s", scheme);
        if (!(TextUtils.equals(scheme, SCHEME) || TextUtils.equals(scheme, FB_SCHEME))) {
            return false;
        }
        String host = uri.getHost();
        Timber.d("host is %s", host);
        if (!TextUtils.equals(host, HOST)) {
            return false;
        }
        return true;
    }

    public static boolean isOpenMainActivity(Context context, Uri uri) {
        boolean valid = checkUri(uri);
        Timber.d("scheme uri %b", valid);
        if (!valid) {
            return false;
        }
        List<String> paths = uri.getPathSegments();
        if (Collections.isEmpty(paths)) {
            return false;
        }
        String path = paths.get(0);
        if (!TextUtils.isEmpty(path)) {
            switch (path) {
                case PRAYER_TIMES:
                case CHAPTER_LIST:
                case QIBLA:
                    return true;
            }
        }

        return false;
    }

    public static void parseUri(Context context, Uri uri) {
        boolean valid = checkUri(uri);
        Timber.d("scheme uri %b", valid);
        if (!valid) {
            return;
        }

        List<String> paths = uri.getPathSegments();
        if (Collections.isEmpty(paths)) {
            return;
        }
        Timber.d("scheme path is %s", paths.get(0));
        if (!TextUtils.isEmpty(paths.get(0))) {
            jump(context, paths.get(0)/*remove the splash from the beginning*/, uri);
        }
    }

    private static void jump(final Context context, String path, Uri uri) {
        switch (path) {
            case PRAYER_TIMES:
                Bundle bundle = new Bundle();
                bundle.putString(CALENDAR_EXPANDED, uri.getQueryParameter(CALENDAR_EXPANDED));
                MainActivity.start(context, PRAYER_TIMES, bundle);
                break;
            case CHAPTER_LIST:
                MainActivity.start(context, CHAPTER_LIST, null);
                break;
            case QIBLA:
                MainActivity.start(context, QIBLA, null);
                break;
            case VERSE_LIST:
                Observable.just(uri)
                        .flatMap(new Function<Uri, ObservableSource<Pair<Chapter, Long>>>() {
                            @Override
                            public ObservableSource<Pair<Chapter, Long>> apply(@NonNull Uri uri) throws Exception {
                                String chapterNumString = uri.getQueryParameter(CHAPTER_NUMBER);
                                String verseNumString = uri.getQueryParameter(VERSE_NUMBER);
                                long chapterNum = 1;
                                long verseNum = VERSE_LAST_TIME_VISITED;
                                try {
                                    chapterNum = Long.valueOf(chapterNumString);
                                    verseNum = Long.valueOf(verseNumString);
                                } catch (NumberFormatException ignored) {

                                }
                                Observable<Chapter> chapterObservable = QuranRepository.INSTANCE.getChapter(chapterNum);
                                //last time visited for verse
                                Observable<Long> verseObservable = Observable.just(verseNum);
                                return Observable.zip(chapterObservable, verseObservable,
                                        new BiFunction<Chapter, Long, Pair<Chapter, Long>>() {
                                            @Override
                                            public Pair<Chapter, Long> apply(@NonNull Chapter chapter, @NonNull Long verse) throws Exception {
                                                return new Pair<>(chapter, verse);
                                            }
                                        });
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new ResourceObserver<Pair<Chapter, Long>>() {
                            @Override
                            public void onNext(@NonNull Pair<Chapter, Long> chapterVersePair) {
                                VerseActivity.start(context, chapterVersePair.first, chapterVersePair.second);
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Timber.d("in scheme verse_list %s", e.getMessage());
                            }

                            @Override
                            public void onComplete() {

                            }
                        });

                break;
            case BOOKMARK:
                BookmarkedVerseActivity.start(context);
                break;
            default:
                break;
        }
    }
}

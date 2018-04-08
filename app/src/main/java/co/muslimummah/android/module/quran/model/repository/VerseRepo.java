package co.muslimummah.android.module.quran.model.repository;

import android.accounts.NetworkErrorException;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.jakewharton.disklrucache.DiskLruCache;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.greenrobot.greendao.query.QueryBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import co.muslimummah.android.event.Quran;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.base.OracleLocaleHelper;
import co.muslimummah.android.module.quran.lycparser.LrcParser;
import co.muslimummah.android.module.quran.lycparser.Lyric;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.JuzInfo;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.VerseDao;
import co.muslimummah.android.module.quran.model.VerseLyric;
import co.muslimummah.android.storage.GreenDaoModule;
import co.muslimummah.android.util.FileUtils;
import co.muslimummah.android.util.Utils;
import co.muslimummah.android.util.filedownload.DownloadParam;
import co.muslimummah.android.util.filedownload.FileDownloadManager;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function3;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by frank on 8/9/17.
 */

class VerseRepo {

    private Cipher cipher;

    private volatile Quran.VerseDownloadStatus lastDownloadStatus;

    Observable<List<Verse>> getVersesWithoutAudioResource(final long chapterId) {
        return Observable.create(new ObservableOnSubscribe<List<Verse>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<Verse>> e) throws Exception {
                VerseDao verseDao = GreenDaoModule.getDaoSession().getVerseDao();
                if (verseDao.loadByRowId(1) == null) {
                    //There is no data inside the database so we prepare it.
                    QuranDataUtils.prepareAllVersesData();
                }
//                Context currentApplicationContext = OracleLocaleHelper.onAttach(OracleApp.getInstance());
//                if (!isVerseTranslationAvailable(currentApplicationContext)) {
//                    //This translation has not been prepared so we need to download it.
//                    try {
//                        downloadAndPersistTranslation(currentApplicationContext);
//                    } catch (Exception exception) {
//                        Observable.just(currentApplicationContext)
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .subscribe(new Consumer<Context>() {
//                                    @Override
//                                    public void accept(@NonNull Context context) throws Exception {
//                                        QuranSetting.setCurrentLanguage(context, OracleLocaleHelper.LanguageEnum.ENGLISH);
//                                        ToastUtil.show(context.getString(R.string.download_failed_toast));
//                                    }
//                                });
//                        Timber.e(exception, "Download verse translation failed!");
//                    }
//                }
                //Now this translation has been prepared so we just query them from database.
                List<Verse> result = verseDao.queryBuilder()
                        .where(VerseDao.Properties.ChapterId.eq(chapterId))
                        .orderAsc(VerseDao.Properties.VerseId)
                        .list();
                e.onNext(result);
                e.onComplete();
            }
        });
    }

    Observable<Verse> getVerseWithoutAudioResource(long chapterId, final long verseId) {
        return getVersesWithoutAudioResource(chapterId)
                .flatMapIterable(new Function<List<Verse>, Iterable<? extends Verse>>() {
                    @Override
                    public Iterable<? extends Verse> apply(@NonNull List<Verse> verses) throws Exception {
                        return verses;
                    }
                })
                .filter(new Predicate<Verse>() {
                    @Override
                    public boolean test(@NonNull Verse verse) throws Exception {
                        return verse.getVerseId() == verseId;
                    }
                })
                .take(1);
    }

    boolean isVerseTranslationAvailable(String translationSuffix) {
        return GreenDaoModule.getDaoSession().getVerseDao().loadByRowId(1).getTranslation(translationSuffix) != null;
    }

//    private void downloadAndPersistTranslation(Context context) throws IOException {
//        String languageName = OracleLocaleHelper.getCurrentLanguage(context).toString();
//        Uri downloadUri = Uri.parse(String.format(Locale.US,
//                QuranRepository.CLOUDFRONT_PREFIX + QuranRepository.VERSE_TRANSLATION_PATH_FORMAT,
//                languageName.toLowerCase()));
//        File file = Observable.create(new FileDownloadObservableOnSubscribe(downloadUri)).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).blockingFirst();
//        QuranDataUtils.updateVerseDataByLanguageName(QuranDataUtils.getVersesFromZipFileByLanguageName(file, languageName), languageName);
//        //Use another thread to delete the file.
//        Observable.just(file).subscribeOn(Schedulers.io())
//                .subscribe(new Consumer<File>() {
//                    @Override
//                    public void accept(@NonNull File file) throws Exception {
//                        file.delete();
//                    }
//                });
//    }

    Observable<String> downloadAndPersistTranslation(OracleLocaleHelper.LanguageEnum language) {
        final String languageName = language.toString();
        String downloadUrl = String.format(Locale.US,
                QuranRepository.CLOUDFRONT_PREFIX + QuranRepository.VERSE_TRANSLATION_PATH_FORMAT,
                languageName.toLowerCase());
//        File file = Observable.create(new F(downloadUri)).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).blockingFirst();
//        QuranDataUtils.updateVerseDataByLanguageName(QuranDataUtils.getVersesFromZipFileByLanguageName(file, languageName), languageName);
        return FileDownloadManager.INSTANCE
                .downloadRx(new DownloadParam(downloadUrl,
                        FileUtils.getDiskCacheFile(Utils.computeSHA256Hash(downloadUrl)).getAbsolutePath()
                        , null))
                .doOnNext(new Consumer<String>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull String s) throws Exception {
                        QuranDataUtils.updateVerseDataByLanguageName(QuranDataUtils.getVersesFromZipFileByLanguageName(new File(s), languageName), languageName);
                    }
                })
                .observeOn(Schedulers.io());
    }

    /**
     * @param chapterId
     * @param verseId
     * @return Merged verse with lyrics property. When returned, we make sure mp3, lrc files exist.
     */
    Observable<Verse> getVerseWithAudioResource(final long chapterId, final long verseId, final boolean downloadWholeChapterIfAudioResourceNotExist) {
        try {
            return prepareAudioResource(chapterId, verseId, downloadWholeChapterIfAudioResourceNotExist)
                    .flatMap(new Function<Boolean, ObservableSource<Verse>>() {
                        @Override
                        public ObservableSource<Verse> apply(@NonNull Boolean aBoolean) throws Exception {
                            return Observable.zip(getVerseWithoutAudioResource(chapterId, verseId),
                                    getVerseLyric(chapterId, verseId, QuranRepository.LANGUAGE_NAME_ARABIC),
                                    getVerseLyric(chapterId, verseId, QuranRepository.LANGUAGE_NAME_TRANSLITERATION),
                                    new Function3<Verse, VerseLyric, VerseLyric, Verse>() {
                                        @Override
                                        public Verse apply(@NonNull Verse verse,
                                                           @NonNull VerseLyric verseLyricArabic,
                                                           @NonNull VerseLyric verseLyricTransliteration) throws Exception {
                                            verseLyricArabic.setRTL(true);
                                            verse.setLyricOriginal(verseLyricArabic);
                                            verseLyricTransliteration.setRTL(false);
                                            verse.setLyricTransliteration(verseLyricTransliteration);
                                            return verse;
                                        }
                                    });
                        }
                    });
        } catch (IOException e) {
            return Observable.error(e);
        }
    }

    Observable<String> getVerseAudioCacheKey(final long chapterId, final long verseId) {
        try {
            return prepareAudioResource(chapterId, verseId, true)
                    .map(new Function<Boolean, String>() {
                        @Override
                        public String apply(@NonNull Boolean aBoolean) throws Exception {
                            if (aBoolean) {
                                return getCacheKeyForVerseAudio(chapterId, verseId);
                            }
                            throw new RuntimeException("prepareAudioResource failed within getVerseAudioCacheKey");
                        }
                    });
        } catch (IOException e) {
            return Observable.error(e);
        }
    }

    /**
     * Download audio resource of the specific verse.
     *
     * @param chapterId
     * @param verseId
     * @throws IOException
     */
    private Observable<Boolean> prepareAudioResource(long chapterId, long verseId, boolean downloadWholeChapter) throws IOException {
        if (isVerseAudioExist(chapterId, verseId) && isLrcExist(chapterId, verseId)) {
            return Observable.just(Boolean.TRUE);
        }
        //Magic logic here: we only cache audio resource of first chapter inside assets.
        if (chapterId == 1) {
            InputStream inputStream = OracleApp.getInstance().getAssets().open("quran/zip/1/zip-1.zip");
            saveVerseAudioResourceFromZipIntoPrivateDirectory(inputStream);
            return Observable.just(Boolean.TRUE);
        } else {
            if (downloadWholeChapter) {
                return downloadAndSaveAudioResource(chapterId);
            } else {
                return downloadAndSaveAudioResource(chapterId, verseId);
            }
        }
    }

    private Observable<Boolean> downloadAndSaveAudioResource(long chapterId) throws IOException {
        //Download chapter package and save them to private directory.
        Uri downloadUri = Uri.parse(String.format(Locale.US,
                QuranRepository.CLOUDFRONT_PREFIX + QuranRepository.VERSE_WHOLE_CHAPTER_AUDIO_ZIP_PATH_FORMAT,
                chapterId,
                chapterId));

        Chapter chapter = QuranRepository.INSTANCE.getChapter(chapterId).blockingFirst();
        String description = String.format(Locale.US, "%s (%d)", chapter.getTransliteration(), chapter.getVerseCount());
        return downloadAndSaveAudioResource(downloadUri, description, true, Long.valueOf(chapterId));
    }

    private Observable<Boolean> downloadAndSaveAudioResource(long chapterId, long verseId) throws IOException {
        //Download chapter package and save them to private directory.
        Uri downloadUri = Uri.parse(String.format(Locale.US,
                QuranRepository.CLOUDFRONT_PREFIX + QuranRepository.VERSE_SINGLE_VERSE_AUDIO_ZIP_PATH_FORMAT,
                chapterId,
                verseId,
                chapterId,
                verseId));

        Chapter chapter = QuranRepository.INSTANCE.getChapter(chapterId).blockingFirst();
        String description = String.format(Locale.US, "%s (%d)", chapter.getTransliteration(), chapter.getVerseCount());
        return downloadAndSaveAudioResource(downloadUri, description, false, null);
    }

    private Observable<Boolean> downloadAndSaveAudioResource(Uri downloadUri, String downloadDescription, boolean needReportEvent, Serializable tag) throws IOException {
        String fileName = Utils.computeSHA256Hash(downloadUri.toString());
        File externalFile = new File(OracleApp.getInstance().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);

        return Observable.create(
                new VerseDownloadObservableOnSubscribe(
                        new DownloadParam(downloadUri.toString(),
                                externalFile.getAbsolutePath(),
                                OracleApp.getInstance().getString(R.string.downloading_audio),
                                downloadDescription,
                                tag
                        ), needReportEvent));
    }

    /**
     * Will emit DaoException if no entity found
     *
     * @param chapterId
     * @param verseId
     * @return
     */
    Observable<Verse> getNormalPreviousVerseWithoutAudioResource(long chapterId, long verseId) {
        return Observable.just(new Pair<>(chapterId, verseId))
                .map(new Function<Pair<Long, Long>, Verse>() {
                    @Override
                    public Verse apply(@NonNull Pair<Long, Long> pair) throws Exception {
                        QueryBuilder<Verse> verseQueryBuilder = GreenDaoModule.getDaoSession()
                                .getVerseDao()
                                .queryBuilder();
                        return verseQueryBuilder
                                .where(VerseDao.Properties.VerseId.notEq(0))
                                .whereOr(VerseDao.Properties.ChapterId.lt(pair.first),
                                        verseQueryBuilder.and(VerseDao.Properties.ChapterId.eq(pair.first),
                                                VerseDao.Properties.VerseId.lt(pair.second)))
                                .orderDesc(VerseDao.Properties.ChapterId, VerseDao.Properties.VerseId)
                                .limit(1)
                                .build()
                                .uniqueOrThrow();
                    }
                });
    }

    /**
     * Will emit DaoException if no entity found
     *
     * @param chapterId
     * @param verseId
     * @return
     */
    Observable<Verse> getNormalPreviousVerseWithAudioResource(long chapterId, long verseId) {
        return getNormalPreviousVerseWithoutAudioResource(chapterId, verseId)
                .flatMap(new Function<Verse, ObservableSource<Verse>>() {
                    @Override
                    public ObservableSource<Verse> apply(@NonNull Verse verse) throws Exception {
                        return getVerseWithAudioResource(verse.getChapterId(), verse.getVerseId(), true);
                    }
                });
    }

    /**
     * Will emit DaoException if no entity found
     *
     * @param chapterId
     * @param verseId
     * @return
     */
    Observable<Verse> getNormalNextVerseWithoutAudioResource(long chapterId, long verseId) {
        return Observable.just(new Pair<>(chapterId, verseId))
                .map(new Function<Pair<Long, Long>, Verse>() {
                    @Override
                    public Verse apply(@NonNull Pair<Long, Long> pair) throws Exception {
                        QueryBuilder<Verse> verseQueryBuilder = GreenDaoModule.getDaoSession()
                                .getVerseDao()
                                .queryBuilder();
                        return verseQueryBuilder
                                .whereOr(VerseDao.Properties.ChapterId.gt(pair.first),
                                        verseQueryBuilder.and(VerseDao.Properties.ChapterId.eq(pair.first),
                                                VerseDao.Properties.VerseId.gt(pair.second)))
                                .orderAsc(VerseDao.Properties.ChapterId, VerseDao.Properties.VerseId)
                                .limit(1)
                                .build()
                                .uniqueOrThrow();
                    }
                });
    }

    /**
     * Will emit DaoException if no entity found
     *
     * @param chapterId
     * @param verseId
     * @return
     */
    Observable<Verse> getNormalNextVerseWithAudioResource(long chapterId, long verseId) {
        return getNormalNextVerseWithoutAudioResource(chapterId, verseId)
                .flatMap(new Function<Verse, ObservableSource<Verse>>() {
                    @Override
                    public ObservableSource<Verse> apply(@NonNull Verse verse) throws Exception {
                        return getVerseWithAudioResource(verse.getChapterId(), verse.getVerseId(), true);
                    }
                });
    }

    /**
     * Will emit DaoException if no entity found
     *
     * @param chapterId
     * @param verseId
     * @return
     */
    Observable<Verse> getBookmarkedNextVerseWithAudioResource(long chapterId, long verseId) {
        return Observable.just(new Pair<>(chapterId, verseId))
                .map(new Function<Pair<Long, Long>, Verse>() {
                    @Override
                    public Verse apply(@NonNull Pair<Long, Long> pair) throws Exception {
                        QueryBuilder<Verse> verseQueryBuilder = GreenDaoModule.getDaoSession()
                                .getVerseDao()
                                .queryBuilder();
                        return verseQueryBuilder
                                .where(VerseDao.Properties.IsBookMarked.eq(Boolean.TRUE))
                                .whereOr(VerseDao.Properties.ChapterId.gt(pair.first),
                                        verseQueryBuilder.and(VerseDao.Properties.ChapterId.eq(pair.first),
                                                VerseDao.Properties.VerseId.gt(pair.second)))
                                .orderAsc(VerseDao.Properties.ChapterId, VerseDao.Properties.VerseId)
                                .limit(1)
                                .build()
                                .uniqueOrThrow();
                    }
                })
                .flatMap(new Function<Verse, ObservableSource<Verse>>() {
                    @Override
                    public ObservableSource<Verse> apply(@NonNull Verse verse) throws Exception {
                        return getVerseWithAudioResource(verse.getChapterId(), verse.getVerseId(), false);
                    }
                });
    }

    /**
     * Will emit DaoException if no entity found
     *
     * @param chapterId
     * @param verseId
     * @return
     */
    Observable<Verse> getBookmarkedPreviousVerseWithAudioResource(long chapterId, long verseId) {
        return Observable.just(new Pair<>(chapterId, verseId))
                .map(new Function<Pair<Long, Long>, Verse>() {
                    @Override
                    public Verse apply(@NonNull Pair<Long, Long> pair) throws Exception {
                        QueryBuilder<Verse> verseQueryBuilder = GreenDaoModule.getDaoSession()
                                .getVerseDao()
                                .queryBuilder();
                        return verseQueryBuilder
                                .where(VerseDao.Properties.IsBookMarked.eq(Boolean.TRUE))
                                .whereOr(VerseDao.Properties.ChapterId.lt(pair.first),
                                        verseQueryBuilder.and(VerseDao.Properties.ChapterId.eq(pair.first),
                                                VerseDao.Properties.VerseId.lt(pair.second)))
                                .orderDesc(VerseDao.Properties.ChapterId, VerseDao.Properties.VerseId)
                                .limit(1)
                                .build()
                                .uniqueOrThrow();
                    }
                })
                .flatMap(new Function<Verse, ObservableSource<Verse>>() {
                    @Override
                    public ObservableSource<Verse> apply(@NonNull Verse verse) throws Exception {
                        return getVerseWithAudioResource(verse.getChapterId(), verse.getVerseId(), false);
                    }
                });
    }

    long getBookmarkedVersesCount() {
        return GreenDaoModule.getDaoSession().getVerseDao().queryBuilder().where(VerseDao.Properties.IsBookMarked.eq(Boolean.TRUE)).count();
    }

    long getBookmarkedVerseOrder(long chapterId, long verseId) {
        QueryBuilder<Verse> verseQueryBuilder = GreenDaoModule.getDaoSession()
                .getVerseDao()
                .queryBuilder();
        return verseQueryBuilder
                .where(VerseDao.Properties.IsBookMarked.eq(Boolean.TRUE))
                .whereOr(VerseDao.Properties.ChapterId.lt(chapterId),
                        verseQueryBuilder.and(VerseDao.Properties.ChapterId.eq(chapterId),
                                VerseDao.Properties.VerseId.le(verseId)))
                .orderAsc(VerseDao.Properties.ChapterId, VerseDao.Properties.VerseId)
                .count();
    }

    Observable<List<Verse>> getBookmarkedVersesWithoutAudioResource() {
        return Observable.create(new ObservableOnSubscribe<List<Verse>>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<List<Verse>> emitter) throws Exception {
//                Context currentApplicationContext = OracleLocaleHelper.onAttach(OracleApp.getInstance());
//                if (!isVerseTranslationAvailable(currentApplicationContext)) {
//                    //This translation has not been prepared so we need to download it.
//                    try {
//                        downloadAndPersistTranslation(currentApplicationContext);
//                    } catch (Exception exception) {
//                        Observable.just(currentApplicationContext)
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .subscribe(new Consumer<Context>() {
//                                    @Override
//                                    public void accept(@NonNull Context context) throws Exception {
//                                        QuranSetting.setCurrentLanguage(context, OracleLocaleHelper.LanguageEnum.ENGLISH);
//                                        ToastUtil.show(context.getString(R.string.download_failed_toast));
//                                    }
//                                });
//                        Timber.e(exception, "Download verse translation failed!");
//                    }
//                }

                List<Verse> result = GreenDaoModule
                        .getDaoSession()
                        .getVerseDao()
                        .queryBuilder()
                        .where(VerseDao.Properties.IsBookMarked.eq(Boolean.TRUE))
                        .orderAsc(VerseDao.Properties.ChapterId, VerseDao.Properties.VerseId)
                        .list();
                emitter.onNext(result);
                emitter.onComplete();
            }
        });
    }

    Observable<Boolean> prepareBookmarkedVersesWithAudioResource() {
        return getBookmarkedVersesWithoutAudioResource()
                .flatMapIterable(new Function<List<Verse>, Iterable<Verse>>() {
                    @Override
                    public Iterable<Verse> apply(@NonNull List<Verse> verses) throws Exception {
                        return verses;
                    }
                })
                .filter(new Predicate<Verse>() {
                    @Override
                    public boolean test(@NonNull Verse verse) throws Exception {
                        return !isVerseAudioExist(verse.getChapterId(), verse.getVerseId()) || !isLrcExist(verse.getChapterId(), verse.getVerseId());
                    }
                })
                .flatMap(new Function<Verse, ObservableSource<Verse>>() {
                    @Override
                    public ObservableSource<Verse> apply(@NonNull Verse verse) throws Exception {
                        Timber.d("download Booked verse %d, %d", verse.getChapterId(), verse.getVerseId());
                        return getVerseWithAudioResource(verse.getChapterId(), verse.getVerseId(), false);
                    }
                })
                .toList()
                .flatMapObservable(new Function<List<Verse>, ObservableSource<? extends Boolean>>() {
                    @Override
                    public ObservableSource<? extends Boolean> apply(@io.reactivex.annotations.NonNull List<Verse> verses) throws Exception {
                        return Observable.just(Boolean.TRUE);
                    }
                });

    }

    void bookMarkVerse(Verse verse, boolean isBookmark) {
        verse.setIsBookMarked(isBookmark);
        GreenDaoModule.getDaoSession().getVerseDao().update(verse);
    }

    /**
     * @param verse
     * @return JuzInfo if the info should exist before this verse, else return null.
     */
    JuzInfo getJuzInfo(Verse verse) {
        return JuzInfoRepo.getVerseJuzInfo(verse.getChapterId(), verse.getVerseId());
    }

    /**
     * Must be called after making sure the corresponding lrc file exists.
     *
     * @param chapterId
     * @param verseId
     * @param languageName
     * @return
     */
    private Observable<VerseLyric> getVerseLyric(long chapterId, long verseId, String languageName) {
        return Observable.just(getLrcFile(chapterId, verseId, languageName))
                .map(new Function<File, VerseLyric>() {
                    @Override
                    public VerseLyric apply(@NonNull File file) throws Exception {
                        LrcParser lrcParser = null;
                        try {
                            lrcParser = new LrcParser(decryptLyric(file));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Lyric lyric = lrcParser.getLyric();
                        VerseLyric verseLyric = new VerseLyric();
                        String[] minutesSecondsStrings = TextUtils.split(lyric.getTags().get("length"), ":");
                        verseLyric.setLength((Long.valueOf(minutesSecondsStrings[0]) * 60
                                + Long.valueOf(minutesSecondsStrings[1])) * 1000);
                        verseLyric.setLyricWords(new ArrayList<VerseLyric.VerseLyricWord>());
                        if (lyric.getSentences() != null) {
                            for (int i = 0; i < lyric.getSentences().size(); ++i) {
                                VerseLyric.VerseLyricWord verseLyricWord = new VerseLyric.VerseLyricWord();
                                verseLyricWord.setContent(lyric.getSentences().get(i).getContent());
                                verseLyricWord.setStartTimestamp(lyric.getSentences().get(i).getFromTime());
                                if (i < lyric.getSentences().size() - 1) {
                                    verseLyricWord.setEndTimestamp(lyric.getSentences().get(i + 1).getFromTime());
                                } else {
                                    verseLyricWord.setEndTimestamp(verseLyric.getLength());
                                }
                                verseLyric.getLyricWords().add(verseLyricWord);
                            }
                        }
                        return verseLyric;
                    }
                });
    }

    private BufferedReader decryptLyric(File file) throws BadPaddingException, IllegalBlockSizeException,
            IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException {
        FileInputStream inputStream = new FileInputStream(file);
        byte[] source = new byte[(int) file.length()];
        inputStream.read(source);
        if (inputStream != null) {
            inputStream.close();
        }

        if (cipher == null) {
            synchronized (this) {
                if (cipher == null) {
                    Cipher temp = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    InputStream blob = OracleApp.getInstance().getAssets().open("blob");
                    byte[] keyBytes = new byte[blob.available()];
                    blob.read(keyBytes);
                    SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
                    temp.init(Cipher.DECRYPT_MODE, key);
                    cipher = temp;
                    blob.close();
                }
            }
        }

        byte[] dest = cipher.doFinal(source);
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(dest)));
    }

    private boolean isVerseAudioExist(long chapterId, long verseId) throws IOException {
        String cacheKey = getCacheKeyForVerseAudio(chapterId, verseId);
        Timber.d("Cache key for mp3 of chapter [%d] verse [%d] is [%s]", chapterId, verseId, cacheKey);
        return VerseMp3Repo.INSTANCE.isAudioCacheExist(cacheKey);
    }

    public boolean isLrcExist(long chapterId, long verseId) {
        File lrcArabicFile = getLrcFile(chapterId, verseId, QuranRepository.LANGUAGE_NAME_ARABIC);
        File lrcTransliterationFile = getLrcFile(chapterId, verseId, QuranRepository.LANGUAGE_NAME_TRANSLITERATION);
        return lrcArabicFile.exists() && lrcTransliterationFile.exists();
    }

    private static String getCacheKeyForVerseAudio(long chapterId, long verseId) {
        return getCacheKeyForVerseAudio(String.format(Locale.US, QuranRepository.VERSE_AUDIO_NAME_FORMAT, chapterId, verseId));
    }

    private static String getCacheKeyForVerseAudio(String fileName) {
        return Utils.computeSHA256Hash(fileName);
    }

    private static File getLrcFile(long chapterId, long verseId, String languageName) {
        return new File(OracleApp.getInstance().getFilesDir(), String.format(Locale.US, QuranRepository.VERSE_AUDIO_LYRIC_PATH_FORMAT, chapterId, verseId, languageName));
    }

    private static File getLrcFile(String fileName) {
        return new File(OracleApp.getInstance().getFilesDir(), String.format(Locale.US, QuranRepository.VERSE_AUDIO_LYRIC_PATH_WITH_NAME_FORMAT, fileName));
    }

    /**
     * Let's denote N as the number of verse within one chapter. This zip file is zipped from N mp3
     * files and N Arabic lyric files and N Transliteration lyric files.
     *
     * @param inputStream
     * @throws IOException
     */
    private synchronized static void saveVerseAudioResourceFromZipIntoPrivateDirectory(InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = QuranDataUtils.transformToZipInputStream(inputStream);
        try {
            ZipEntry zipEntry;
            byte[] buffer = new byte[1024 * 1024];
            int readLength;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.isDirectory()) {
                    continue;
                }
                DiskLruCache.Editor editor = null;
                OutputStream outputStream;
                if (zipEntry.getName().endsWith(".mp3")) {
                    //This is the mp3 file, save it into our DiskLruCache.
                    editor = VerseMp3Repo.INSTANCE.getEditor(getCacheKeyForVerseAudio(zipEntry.getName()));
                    outputStream = editor.newOutputStream(0);
                } else {
                    //This is a lyric either for arabic or transliteration.
                    //// TODO: 8/11/17 Maybe we get a aes file which need to be decrypted by RSA.
                    File outputFile = getLrcFile(zipEntry.getName());
                    if (!outputFile.exists()) {
                        outputFile.getParentFile().mkdirs();
                        outputFile.createNewFile();
                    }
                    outputStream = new FileOutputStream(outputFile);
                }

                try {
                    while ((readLength = zipInputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, readLength);
                    }

                    if (editor != null) {
                        editor.commit();
                    }
                } catch (IOException e) {
                    Timber.e(e, "saveVerseAudioResourceFromZipIntoPrivateDirectory failed");
                    if (editor != null) {
                        editor.abort();
                    }
                } finally {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                }
            }
        } finally {
            if (zipInputStream != null) {
                zipInputStream.close();
            }
        }
    }

    public Quran.VerseDownloadStatus getLastDownloadStatus() {
        return lastDownloadStatus;
    }

    private class VerseDownloadObservableOnSubscribe implements ObservableOnSubscribe<Boolean> {
        DownloadParam param;
        ObservableEmitter<Boolean> emitter;
        boolean needReportEvent;


        VerseDownloadObservableOnSubscribe(DownloadParam param, boolean needReportEvent) {
            this.param = param;
            this.needReportEvent = needReportEvent;
        }

        @Override
        public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<Boolean> e) throws Exception {
            emitter = e;
            EventBus.getDefault().register(this);
            if (needReportEvent && lastDownloadStatus != null) {
                FileDownloadManager.INSTANCE.cancel(lastDownloadStatus.getDownloadStatus().getParam());
            }

            if (!FileDownloadManager.INSTANCE.download(param)) {
                emitter.onError(new NetworkErrorException(""));
            } else {
                EventBus.getDefault().post(new Quran.StartDownloadVerse());
            }

            emitter.setDisposable(Disposables.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    FileDownloadManager.INSTANCE.cancel(param);
                }
            }));
        }

        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onDownloadStatusUpdate(Quran.DownloadStatus status) {
            if (!status.getParam().equals(param)) {
                return;
            }

            synchronized (VerseRepo.this) {
                if (needReportEvent && lastDownloadStatus == null) {
                    lastDownloadStatus = new Quran.VerseDownloadStatus();
                }

                Timber.d("download file VerseRepo status : %d", status.getStatus());

                if (needReportEvent) {
                    lastDownloadStatus.setDownloadStatus(status);
                }

                if (emitter.isDisposed()) {
                    return;
                }

                switch (status.getStatus()) {
                    case Quran.DownloadStatus.STATUS_COMPLETE:
                        EventBus.getDefault().unregister(this);
                        File dstFile = new File(param.getDstFilePath());
                        try {
                            status.setStatus(Quran.VerseDownloadStatus.STATUS_PROCESS);
                            if (needReportEvent) {
                                EventBus.getDefault().post(lastDownloadStatus);
                            }
                            saveVerseAudioResourceFromZipIntoPrivateDirectory(new FileInputStream(dstFile));
                            status.setStatus(Quran.VerseDownloadStatus.STATUS_COMPLETE);
                            if (needReportEvent) {
                                EventBus.getDefault().post(lastDownloadStatus);
                            }
                            emitter.onNext(true);
                            emitter.onComplete();
                        } catch (IOException e) {
                            emitter.onError(e);
                            status.setStatus(Quran.VerseDownloadStatus.STATUS_ERROR);
                            if (needReportEvent) {
                                EventBus.getDefault().post(lastDownloadStatus);
                            }
                        } finally {
                            dstFile.delete();
                            lastDownloadStatus = null;
                        }
                        break;
                    case Quran.DownloadStatus.STATUS_ERROR:
                        EventBus.getDefault().unregister(this);
                        if (needReportEvent) {
                            EventBus.getDefault().post(lastDownloadStatus);
                        }
                        emitter.onError(new RuntimeException("Verse download failed"));

                        lastDownloadStatus = null;
                        break;
                    case Quran.DownloadStatus.STATUS_START:
                    case Quran.DownloadStatus.STATUS_DOWNLODING:
                        if (needReportEvent) {
                            EventBus.getDefault().post(lastDownloadStatus);
                        }
                        break;
                }
            }
        }
    }

}

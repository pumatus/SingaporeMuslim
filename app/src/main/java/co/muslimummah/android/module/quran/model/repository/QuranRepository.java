package co.muslimummah.android.module.quran.model.repository;

import java.util.List;
import java.util.Locale;

import co.muslimummah.android.event.Quran;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.OracleLocaleHelper;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.JuzInfo;
import co.muslimummah.android.module.quran.model.TranslationWord;
import co.muslimummah.android.module.quran.model.Verse;
import io.reactivex.Observable;


/**
 * Created by frank on 7/26/17.
 */

public enum QuranRepository {
    INSTANCE;

    //// TODO: 8/16/17 Fetch it by firebase remote-config.
    static final String CLOUDFRONT_PREFIX = "https://dcz1l4n5hxvc2.cloudfront.net";
    public static final String WORD_MP3_PREFIX = CLOUDFRONT_PREFIX + "/quran/mp3/letter/";
    static final String VERSE_TRANSLATION_PATH_FORMAT = "/quran/file/verse/translation/verse-%s.zip";
    static final String VERSE_WHOLE_CHAPTER_AUDIO_ZIP_PATH_FORMAT = "/quran/ziputhman/%d/zip-%d.zip";
    static final String VERSE_SINGLE_VERSE_AUDIO_ZIP_PATH_FORMAT = "/quran/ziputhman/%d/%d/zip-%d-%d.zip";

    static final String VERSE_AUDIO_LYRIC_PATH_WITH_NAME_FORMAT = "/quran/verse/%s";
    //TODO
    static final String VERSE_AUDIO_LYRIC_PATH_FORMAT = "/quran/verse/Uthman-Verse-%d-%d-%s.aes";
    static final String VERSE_AUDIO_NAME_FORMAT = "Uthman-Verse-%d-%d-56K.mp3";
    static final String LANGUAGE_NAME_ARABIC = "arabic";
    static final String LANGUAGE_NAME_TRANSLITERATION = "transliteration";

    private final ChapterRepo mChapterRepo = new ChapterRepo();
    private final VerseRepo mVerseRepo = new VerseRepo();

    private final WordRepo mWordRepo = new WordRepo();

    /**
     * word by word
     *
     * @param chapterId
     * @param verseId
     * @return list of word in single verse
     */
    public Observable<List<TranslationWord>> getWords(final int chapterId, final int verseId) {
        return mWordRepo.getWords(chapterId, verseId);
    }

    public Observable<List<Chapter>> getChapters() {
        return mChapterRepo.getChapters();
    }

    public Observable<Chapter> getChapter(long chapterId) {
        return mChapterRepo.getChapter(chapterId);
    }

    public Observable<List<Verse>> getVersesWithoutAudioResource(long chapterId) {
        return mVerseRepo.getVersesWithoutAudioResource(chapterId);
    }

    public boolean isVerseTranslationAvailable(OracleLocaleHelper.LanguageEnum languageEnum) {
        return mVerseRepo.isVerseTranslationAvailable(languageEnum.toString());
    }

    public Observable<String> downloadTranslation(OracleLocaleHelper.LanguageEnum languageEnum) {
        return mVerseRepo.downloadAndPersistTranslation(languageEnum);
    }

    public Observable<Verse> getVerseWithoutAudioResource(long chapterId, long verseId) {
        return mVerseRepo.getVerseWithoutAudioResource(chapterId, verseId);
    }

    public Observable<Verse> getVerseWithAudioResource(long chapterId, long verseId, boolean downloadWholeChapterIfAudioResourceNotExist) {
        return mVerseRepo.getVerseWithAudioResource(chapterId, verseId, downloadWholeChapterIfAudioResourceNotExist);
    }

    public Observable<String> getVerseAudioCacheKey(long chapterId, long verseId) {
        return mVerseRepo.getVerseAudioCacheKey(chapterId, verseId);
    }

    public Observable<Verse> getNormalNextVerseWithoutAudioResource(long chapterId, long verseId) {
        return mVerseRepo.getNormalNextVerseWithoutAudioResource(chapterId, verseId);
    }

    public Observable<Verse> getNormalPreviousVerseWithoutAudioResource(long chapterId, long verseId) {
        return mVerseRepo.getNormalPreviousVerseWithoutAudioResource(chapterId, verseId);
    }

    public Observable<Verse> getNormalNextVerseWithAudioResource(long chapterId, long verseId) {
        return mVerseRepo.getNormalNextVerseWithAudioResource(chapterId, verseId);
    }

    public Observable<Verse> getNormalPreviousVerseWithAudioResource(long chapterId, long verseId) {
        return mVerseRepo.getNormalPreviousVerseWithAudioResource(chapterId, verseId);
    }

    public Observable<Verse> getBookmarkedNextVerseWithAudioResource(long chapterId, long verseId) {
        return mVerseRepo.getBookmarkedNextVerseWithAudioResource(chapterId, verseId);
    }

    public Observable<Verse> getBookmarkedPreviousVerseWithAudioResource(long chapterId, long verseId) {
        return mVerseRepo.getBookmarkedPreviousVerseWithAudioResource(chapterId, verseId);
    }

    public long getBookmarkedVerseOrder(long chapterId, long verseId) {
        return mVerseRepo.getBookmarkedVerseOrder(chapterId, verseId);
    }

    public long getBookmarkedVersesCount() {
        return mVerseRepo.getBookmarkedVersesCount();
    }

    public void bookMarkVerse(Verse verse, boolean isBookmark) {
        ThirdPartyAnalytics.INSTANCE.logEvent("QuranBookmark", String.format(Locale.US, isBookmark ? "Bookmark[%d]" : "Unbookmark[%d]", verse.getChapterId()), String.valueOf(verse.getVerseId()), null);
        mVerseRepo.bookMarkVerse(verse, isBookmark);
    }

    public Observable<List<Verse>> getBookmarkedVersesWithoutAudioResource() {
        return mVerseRepo.getBookmarkedVersesWithoutAudioResource();
    }

    /**
     * @return true if the download succeeds.
     */
    public Observable<Boolean> prepareBookmarkedVersesWithAudioResource() {
        return mVerseRepo.prepareBookmarkedVersesWithAudioResource();
    }

    public boolean isLrcExist(Verse verse) {
        return verse != null && mVerseRepo.isLrcExist(verse.getChapterId(), verse.getVerseId());
    }

    /**
     * @param verse
     * @return Arabic Juz String and English Juz String if the info should exist before this verse, else return null.
     */
    public JuzInfo getJuzInfo(Verse verse) {
        return mVerseRepo.getJuzInfo(verse);
    }

    public Quran.VerseDownloadStatus getLastVerseDownloadStatus() {
        return mVerseRepo.getLastDownloadStatus();
    }
}

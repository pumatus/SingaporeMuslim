package co.muslimummah.android.module.quran.model.repository;

import android.content.Context;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.module.quran.model.Chapter;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.VerseDao;
import co.muslimummah.android.storage.GreenDaoModule;
import timber.log.Timber;

/**
 * Created by frank on 8/1/17.
 */

class QuranDataUtils {
    private static Reader getReaderFromInputStream(InputStream inputStream) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 1024 * 1024);
        return new InputStreamReader(bufferedInputStream, "UTF-8");
    }

    private static Reader getReaderFromSingleZipInputStream(InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = transformToZipInputStream(inputStream);
        //Use and only use the first zip entry.
        zipInputStream.getNextEntry();
        return new InputStreamReader(zipInputStream, "UTF-8");
    }

    static ZipInputStream transformToZipInputStream(InputStream inputStream) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 1024 * 1024);
        return new ZipInputStream(bufferedInputStream);
    }

    static List<Chapter> getChaptersFromInputStream(InputStream inputStream) throws IOException {
        // creates a CSV parser
        CsvParser parser = new CsvParser(new CsvParserSettings());
        parser.beginParsing(getReaderFromInputStream(inputStream));
        //Ignore the first line (title line) of the csv file.
        parser.parseNext();
        List<Chapter> chapterList = new ArrayList<>();
        try {
            String[] nextLine;
            while ((nextLine = parser.parseNext()) != null) {
                Chapter chapter = new Chapter();
                chapter.setChapterId(Long.valueOf(nextLine[0]));
                chapter.setVerseCount(Long.valueOf(nextLine[1]));
                chapter.setTransliteration(nextLine[2].trim());
                chapter.setTranslationEnglish(nextLine[3].trim());
                chapter.setOriginal(nextLine[4].trim());
                chapter.setTranslationIndonesian(nextLine[5].trim());
                chapter.setTranslationMalay(nextLine[6].trim());
                chapter.setTranslationHindi(nextLine[7].trim());
                chapter.setTranslationTurkish(nextLine[8].trim());
                chapter.setTranslationBengali(nextLine[9].trim());
                chapter.setTranslationUrdu(nextLine[10].trim());
                chapter.setTranslationRussian(nextLine[11].trim());
                chapter.setTranslationFrench(nextLine[12].trim());
                chapter.setTitleInUnicode(nextLine[13].trim());
                chapterList.add(chapter);
            }
        } catch (Exception e) {
            //Including NoSuchMethodException, IllegalAccessException, InvocationTargetException
            Timber.e(e, "getVersesFromReaderByPropertyName failed");
        }
        parser.stopParsing();
        inputStream.close();
        return chapterList;
    }

    static void prepareAllVersesData() throws IOException {
        Context context = OracleApp.getInstance();
        Reader reader;
        //Takes around 3.5 seconds
        reader = getReaderFromInputStream(context.getAssets().open("quran/verse/original/verse-arabic.txt"));
        List<Verse> tmpList = getVersesFromReaderByPropertyName(reader, "original");
        Map<Integer, Verse> verseMap = new HashMap<>(tmpList.size());
        for (Verse verse : tmpList) {
            verseMap.put((int) (verse.getChapterId() * 1000 + verse.getVerseId()), verse);
        }
        //Takes around 3.5 seconds
        reader = getReaderFromInputStream(context.getAssets().open("quran/verse/transliteration/verse-transliteration.txt"));
        tmpList = getVersesFromReaderByPropertyName(reader, "transliteration");
        for (Verse verse : tmpList) {
            verseMap.get((int) (verse.getChapterId() * 1000 + verse.getVerseId())).setTransliteration(verse.getTransliteration());
        }
        //Takes around 3.5 seconds
        reader = getReaderFromInputStream(context.getAssets().open("quran/verse/translation/verse-english.txt"));
        tmpList = getVersesFromReaderByLanguageName(reader, context.getString(R.string.locale_language_name_en));
        for (Verse verse : tmpList) {
            verseMap.get((int) (verse.getChapterId() * 1000 + verse.getVerseId())).setTranslationEnglish(verse.getTranslationEnglish());
        }
        //Takes around 3.5 seconds
        reader = getReaderFromInputStream(context.getAssets().open("quran/verse/translation/verse-indonesian.txt"));
        tmpList = getVersesFromReaderByLanguageName(reader, context.getString(R.string.locale_language_name_in));
        for (Verse verse : tmpList) {
            verseMap.get((int) (verse.getChapterId() * 1000 + verse.getVerseId())).setTranslationIndonesian(verse.getTranslationIndonesian());
        }
        VerseDao verseDao = GreenDaoModule.getDaoSession().getVerseDao();
        //Takes around 1 second
        verseDao.insertInTx(verseMap.values());
    }

    /**
     * @param verses
     * @param languageName
     * @return All verses after update.
     */
    static void updateVerseDataByLanguageName(List<Verse> verses, String languageName) {
        VerseDao verseDao = GreenDaoModule.getDaoSession().getVerseDao();
        Map<Integer, Verse> verseMap = new HashMap<>(verses.size());
        for (Verse verse : verseDao.loadAll()) {
            verseMap.put((int) (verse.getChapterId() * 1000 + verse.getVerseId()), verse);
        }
        try {
            Method setMethod = Verse.class.getMethod("setTranslation" + languageName, String.class);
            Method getMethod = Verse.class.getMethod("getTranslation" + languageName);
            for (Verse verse : verses) {
                setMethod.invoke(verseMap.get((int) (verse.getChapterId() * 1000 + verse.getVerseId())), (String) getMethod.invoke(verse));
            }
            GreenDaoModule.getDaoSession().getVerseDao().updateInTx(verseMap.values());
        } catch (Exception e) {
            //Including NoSuchMethodException, IllegalAccessException, InvocationTargetException
            Timber.e(e, "updateVerseDataByLanguageName failed");
        }
    }

    private static List<Verse> getVersesFromReaderByPropertyName(Reader reader, String propertyName) throws IOException {
        // creates a CSV parser
        CsvParser parser = new CsvParser(new CsvParserSettings());
        parser.beginParsing(reader);
        //Ignore the first line (title line) of the csv file.
        parser.parseNext();
        List<Verse> verseList = new ArrayList<>();
        try {
            Method setMethod = Verse.class.getMethod("set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1), String.class);
            String[] nextLine;
            while ((nextLine = parser.parseNext()) != null) {
                Verse verse = new Verse();
                verse.setChapterId(Long.valueOf(nextLine[0]));
                verse.setVerseId(Long.valueOf(nextLine[1]));
                setMethod.invoke(verse, nextLine[2].trim());
                verseList.add(verse);
            }
        } catch (Exception e) {
            //Including NoSuchMethodException, IllegalAccessException, InvocationTargetException
            Timber.e(e, "getVersesFromReaderByPropertyName failed");
        }
        parser.stopParsing();
        reader.close();
        return verseList;
    }

    private static List<Verse> getVersesFromReaderByLanguageName(Reader reader, String languageName) throws IOException {
        return getVersesFromReaderByPropertyName(reader, "Translation" + languageName);
    }

    static List<Verse> getVersesFromZipFileByLanguageName(File zipFile, String languageName) throws IOException {
        Reader reader = getReaderFromSingleZipInputStream(new FileInputStream(zipFile));
        return getVersesFromReaderByLanguageName(reader, languageName);
    }
}

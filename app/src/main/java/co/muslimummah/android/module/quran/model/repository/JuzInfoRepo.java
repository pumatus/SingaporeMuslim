package co.muslimummah.android.module.quran.model.repository;

import java.util.HashMap;
import java.util.Map;

import co.muslimummah.android.module.quran.model.JuzInfo;

/**
 * Created by frank on 8/24/17.
 */

class JuzInfoRepo {
    /**
     * It should be larger than max_i{chapter[i].verseCount}
     */
    private static final long MULTIPLIER = 1000;

    private static final String[][] JUZ_INFO = {
            {"Juz 1", "جزء ١", "1", "1"},
            {"Juz 2", "جزء ٢", "2", "142"},
            {"Juz 3", "جزء ٣", "2", "253"},
            {"Juz 4", "جزء ٤", "3", "93"},
            {"Juz 5", "جزء ٥", "4", "24"},
            {"Juz 6", "جزء ٦", "4", "148"},
            {"Juz 7", "جزء ٧", "5", "82"},
            {"Juz 8", "جزء ٨", "6", "111"},
            {"Juz 9", "جزء ٩", "7", "88"},
            {"Juz 10", "جزء ١٠", "8", "41"},
            {"Juz 11", "جزء ١١", "9", "93"},
            {"Juz 12", "جزء ١٢", "11", "6"},
            {"Juz 13", "جزء ١٣", "12", "53"},
            {"Juz 14", "جزء ١٤", "15", "1"},
            {"Juz 15", "جزء ١٥", "17", "1"},
            {"Juz 16", "جزء ١٦", "18", "75"},
            {"Juz 17", "جزء ١٧", "21", "1"},
            {"Juz 18", "جزء ١٨", "23", "1"},
            {"Juz 19", "جزء ١٩", "25", "21"},
            {"Juz 20", "جزء ٢٠", "27", "56"},
            {"Juz 21", "جزء ٢١", "29", "46"},
            {"Juz 22", "جزء ٢٢", "33", "31"},
            {"Juz 23", "جزء ٢٣", "36", "28"},
            {"Juz 24", "جزء ٢٤", "39", "32"},
            {"Juz 25", "جزء ٢٥", "41", "47"},
            {"Juz 26", "جزء ٢٦", "46", "1"},
            {"Juz 27", "جزء ٢٧", "51", "31"},
            {"Juz 28", "جزء ٢٨", "58", "1"},
            {"Juz 29", "جزء ٢٩", "67", "1"},
            {"Juz 30", "جزء ٣٠", "78", "1"}
    };
    private static Map<Long, JuzInfo> juzInfoMap;

    static JuzInfo getVerseJuzInfo(long chapterId, long verseId) {
        if (juzInfoMap == null) {
            juzInfoMap = new HashMap<>();
            for (String[] juzInfo : JUZ_INFO) {
                juzInfoMap.put(Long.valueOf(juzInfo[2]) * MULTIPLIER + Long.valueOf(juzInfo[3]),
                        new JuzInfo(juzInfo[1], juzInfo[0]));
            }
        }
        return juzInfoMap.get(chapterId * MULTIPLIER + verseId);
    }
}

package co.muslimummah.android.module.quran.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.base.OracleLocaleHelper;
import lombok.Data;

/**
 * Created by tysheng
 * Date: 20/9/17 11:01 AM.
 * Email: tyshengsx@gmail.com
 */
@Data
@Entity
public class TranslationWord implements Serializable {

    private static final long serialVersionUID = -5003079331830803704L;

    @Id
    private Long mainId;

    private int id;
    private int chapterNum;
    private int verseNum;
    private int letterNum;
    private String arabic;
    private String transliteration;

    private String english;
    private String indonesian;
    private String compressedMp3;
    private String originalMp3;
    private String tajweedNoteId;
    private String verseId;

    @Generated(hash = 624713906)
    public TranslationWord(Long mainId, int id, int chapterNum, int verseNum, int letterNum, String arabic,
            String transliteration, String english, String indonesian, String compressedMp3, String originalMp3,
            String tajweedNoteId, String verseId) {
        this.mainId = mainId;
        this.id = id;
        this.chapterNum = chapterNum;
        this.verseNum = verseNum;
        this.letterNum = letterNum;
        this.arabic = arabic;
        this.transliteration = transliteration;
        this.english = english;
        this.indonesian = indonesian;
        this.compressedMp3 = compressedMp3;
        this.originalMp3 = originalMp3;
        this.tajweedNoteId = tajweedNoteId;
        this.verseId = verseId;
    }

    @Generated(hash = 866157109)
    public TranslationWord() {
    }

    public int getChapterNum() {
        return this.chapterNum;
    }

    public void setChapterNum(int chapterNum) {
        this.chapterNum = chapterNum;
    }

    public int getVerseNum() {
        return this.verseNum;
    }

    public void setVerseNum(int verseNum) {
        this.verseNum = verseNum;
    }

    public int getLetterNum() {
        return this.letterNum;
    }

    public void setLetterNum(int letterNum) {
        this.letterNum = letterNum;
    }

    public String getArabic() {
        return this.arabic;
    }

    public void setArabic(String arabic) {
        this.arabic = arabic;
    }

    public String getTransliteration() {
        return this.transliteration;
    }

    public void setTransliteration(String transliteration) {
        this.transliteration = transliteration;
    }

    public String getEnglish() {
        return this.english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public String getIndonesian() {
        return this.indonesian;
    }

    public void setIndonesian(String indonesian) {
        this.indonesian = indonesian;
    }

    public String getCompressedMp3() {
        return this.compressedMp3;
    }

    public void setCompressedMp3(String compressedMp3) {
        this.compressedMp3 = compressedMp3;
    }

    /**
     * @return english or indonesian
     */
    public String getCurrentTranslation() {
        OracleLocaleHelper.LanguageEnum languageEnum = QuranSetting.getCurrentLanguage(OracleApp.getInstance());
        if (languageEnum == OracleLocaleHelper.LanguageEnum.INDONESIAN) {
            return getIndonesian();
        } else {
            return getEnglish();
        }
    }

    public String getOriginalMp3() {
        return this.originalMp3;
    }

    public void setOriginalMp3(String originalMp3) {
        this.originalMp3 = originalMp3;
    }



    public Long getMainId() {
        return this.mainId;
    }

    public void setMainId(Long mainId) {
        this.mainId = mainId;
    }

    public String getTajweedNoteId() {
        return this.tajweedNoteId;
    }

    public void setTajweedNoteId(String tajweedNoteId) {
        this.tajweedNoteId = tajweedNoteId;
    }

    public String getVerseId() {
        return this.verseId;
    }

    public void setVerseId(String verseId) {
        this.verseId = verseId;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
}

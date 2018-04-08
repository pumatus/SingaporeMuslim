package co.muslimummah.android.module.quran.model;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import timber.log.Timber;

/**
 * Created by frank on 8/9/17.
 */
@Entity
public class Word implements Serializable {
    private static final long serialVersionUID = 6705369061196644029L;

    @Id
    Long id;
    Long chapterId;
    Long verseId;
    Long wordId;
    String original;
    String transliteration;
    String translationBengali;
    String translationEnglish;
    String translationFrench;
    String translationHindi;
    String translationIndonesian;
    String translationMalay;
    String translationRussian;
    String translationTurkish;
    String translationUrdu;

    @Generated(hash = 1050393006)
    public Word(Long id,
                Long chapterId,
                Long verseId,
                Long wordId,
                String original,
                String transliteration,
                String translationBengali,
                String translationEnglish,
                String translationFrench,
                String translationHindi,
                String translationIndonesian,
                String translationMalay,
                String translationRussian,
                String translationTurkish,
                String translationUrdu) {
        this.id = id;
        this.chapterId = chapterId;
        this.verseId = verseId;
        this.wordId = wordId;
        this.original = original;
        this.transliteration = transliteration;
        this.translationBengali = translationBengali;
        this.translationEnglish = translationEnglish;
        this.translationFrench = translationFrench;
        this.translationHindi = translationHindi;
        this.translationIndonesian = translationIndonesian;
        this.translationMalay = translationMalay;
        this.translationRussian = translationRussian;
        this.translationTurkish = translationTurkish;
        this.translationUrdu = translationUrdu;
    }

    @Generated(hash = 3342184)
    public Word() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChapterId() {
        return this.chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Long getVerseId() {
        return this.verseId;
    }

    public void setVerseId(Long verseId) {
        this.verseId = verseId;
    }

    public Long getWordId() {
        return this.wordId;
    }

    public void setWordId(Long wordId) {
        this.wordId = wordId;
    }

    public String getOriginal() {
        return this.original;
    }

    public void setOriginal(String original) {
        this.original = original;
    }

    public String getTransliteration() {
        return this.transliteration;
    }

    public void setTransliteration(String transliteration) {
        this.transliteration = transliteration;
    }

    public String getTranslationBengali() {
        return this.translationBengali;
    }

    public void setTranslationBengali(String translationBengali) {
        this.translationBengali = translationBengali;
    }

    public String getTranslationEnglish() {
        return this.translationEnglish;
    }

    public void setTranslationEnglish(String translationEnglish) {
        this.translationEnglish = translationEnglish;
    }

    public String getTranslationFrench() {
        return this.translationFrench;
    }

    public void setTranslationFrench(String translationFrench) {
        this.translationFrench = translationFrench;
    }

    public String getTranslationHindi() {
        return this.translationHindi;
    }

    public void setTranslationHindi(String translationHindi) {
        this.translationHindi = translationHindi;
    }

    public String getTranslationIndonesian() {
        return this.translationIndonesian;
    }

    public void setTranslationIndonesian(String translationIndonesian) {
        this.translationIndonesian = translationIndonesian;
    }

    public String getTranslationMalay() {
        return this.translationMalay;
    }

    public void setTranslationMalay(String translationMalay) {
        this.translationMalay = translationMalay;
    }

    public String getTranslationRussian() {
        return this.translationRussian;
    }

    public void setTranslationRussian(String translationRussian) {
        this.translationRussian = translationRussian;
    }

    public String getTranslationTurkish() {
        return this.translationTurkish;
    }

    public void setTranslationTurkish(String translationTurkish) {
        this.translationTurkish = translationTurkish;
    }

    public String getTranslationUrdu() {
        return this.translationUrdu;
    }

    public void setTranslationUrdu(String translationUrdu) {
        this.translationUrdu = translationUrdu;
    }

    public String getTranslation() {
        try {
            Method method = getClass().getDeclaredMethod("getTranslation" + OracleApp.getInstance().getString(R.string.locale_language_name));
            return (String) method.invoke(this);
        } catch (NoSuchMethodException e) {
            Timber.e(e, "getTranslation failed");
        } catch (IllegalAccessException e) {
            Timber.e(e, "getTranslation failed");
        } catch (InvocationTargetException e) {
            Timber.e(e, "getTranslation failed");
        }
        return null;
    }
}

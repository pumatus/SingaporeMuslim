package co.muslimummah.android.module.quran.model;

import android.content.Context;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;

import java.io.Serializable;
import java.lang.reflect.Method;

import co.muslimummah.android.R;
import lombok.Data;
import timber.log.Timber;

/**
 * Created by frank on 7/29/17.
 * We must keep this class from obfuscated by Proguard since we have used reflection for method.
 */
@Data
@Entity(indexes = {@Index(value = "chapterId", unique = true)})
public class Chapter implements Serializable {
    private static final long serialVersionUID = 1027583911250429040L;

    @Id
    Long id;
    long chapterId;
    long verseCount;
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
    String titleInUnicode;

    @Generated(hash = 393170288)
    public Chapter() {
    }

    @Generated(hash = 810122294)
    public Chapter(Long id, long chapterId, long verseCount, String original, String transliteration,
            String translationBengali, String translationEnglish, String translationFrench,
            String translationHindi, String translationIndonesian, String translationMalay,
            String translationRussian, String translationTurkish, String translationUrdu,
            String titleInUnicode) {
        this.id = id;
        this.chapterId = chapterId;
        this.verseCount = verseCount;
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
        this.titleInUnicode = titleInUnicode;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getChapterId() {
        return this.chapterId;
    }

    public void setChapterId(long chapterId) {
        this.chapterId = chapterId;
    }

    public long getVerseCount() {
        return this.verseCount;
    }

    public void setVerseCount(long verseCount) {
        this.verseCount = verseCount;
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

    public String getTitleInUnicode() {
        return this.titleInUnicode;
    }

    public void setTitleInUnicode(String titleInUnicode) {
        this.titleInUnicode = titleInUnicode;
    }

    public String getTranslation(Context context) {
        return getTranslation(QuranSetting.getCurrentLanguage(context).toString());
    }

    public String getTranslation(String translationSuffix) {
        try {
            Method method = getClass().getDeclaredMethod("getTranslation" + translationSuffix);
            return (String) method.invoke(this);
        } catch (Exception e) {
            //Including NoSuchMethodException, IllegalAccessException, InvocationTargetException
            Timber.e(e, "getTranslation failed");
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Chapter chapter = (Chapter) o;

        return chapterId == chapter.chapterId;

    }

    @Override
    public int hashCode() {
        return (int) (chapterId ^ (chapterId >>> 32));
    }
}

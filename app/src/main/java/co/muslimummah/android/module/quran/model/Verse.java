package co.muslimummah.android.module.quran.model;

import android.content.Context;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Index;
import org.greenrobot.greendao.annotation.JoinProperty;
import org.greenrobot.greendao.annotation.ToMany;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

import lombok.Data;
import timber.log.Timber;

/**
 * Created by frank on 8/4/17.
 * We must keep this class from obfuscated by Proguard since we have used reflection for method.
 */
@Data
@Entity(indexes = {@Index(value = "chapterId,verseId", unique = true)})
public class Verse implements Serializable {
    private static final long serialVersionUID = 4471482607552263328L;

    //Used for last read verse feature.
    public static final String SP_KEY_LAST_READ = "quran.model.Verse.SP_KEY_LAST_READ";

    @Id
    Long id;
    long chapterId;
    long verseId;
    boolean isBookMarked;
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
    @ToMany(joinProperties = {
            @JoinProperty(name = "chapterId", referencedName = "chapterId"),
            @JoinProperty(name = "verseId", referencedName = "verseId")
    })
    List<Word> words;
    @Transient
    VerseLyric lyricOriginal;
    @Transient
    VerseLyric lyricTransliteration;

    /**
     * Used to resolve relations
     */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /**
     * Used for active entity operations.
     */
    @Generated(hash = 1120694619)
    private transient VerseDao myDao;

    @Generated(hash = 1505125539)
    public Verse() {
    }


    @Generated(hash = 1094407244)
    public Verse(Long id, long chapterId, long verseId, boolean isBookMarked, String original,
                 String transliteration, String translationBengali, String translationEnglish,
                 String translationFrench, String translationHindi, String translationIndonesian,
                 String translationMalay, String translationRussian, String translationTurkish,
                 String translationUrdu) {
        this.id = id;
        this.chapterId = chapterId;
        this.verseId = verseId;
        this.isBookMarked = isBookMarked;
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

    public long getVerseId() {
        return this.verseId;
    }

    public void setVerseId(long verseId) {
        this.verseId = verseId;
    }

    public boolean getIsBookMarked() {
        return this.isBookMarked;
    }

    public void setIsBookMarked(boolean isBookMarked) {
        this.isBookMarked = isBookMarked;
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

    public VerseLyric getLyricOriginal() {
        return this.lyricOriginal;
    }

    public void setLyricOriginal(VerseLyric verseLyric) {
        this.lyricOriginal = verseLyric;
    }

    public VerseLyric getLyricTransliteration() {
        return this.lyricTransliteration;
    }

    public void setLyricTransliteration(VerseLyric verseLyric) {
        this.lyricTransliteration = verseLyric;
    }

    public String getTranslation(Context context) {
        return getTranslation(QuranSetting.getCurrentLanguage(context).toString());
    }

    public String getTranslation(String translationSuffix) {
        try {
            Timber.d("Translation " + translationSuffix);
            Method method = getClass().getDeclaredMethod("getTranslation" + translationSuffix);
            return (String) method.invoke(this);
        } catch (Exception e) {
            //Including NoSuchMethodException, IllegalAccessException, InvocationTargetException
            Timber.e(e, "getTranslation failed");
        }
        return null;
    }

    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 181444940)
    public List<Word> getWords() {
        if (words == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            WordDao targetDao = daoSession.getWordDao();
            List<Word> wordsNew = targetDao._queryVerse_Words(chapterId, verseId);
            synchronized (this) {
                if (words == null) {
                    words = wordsNew;
                }
            }
        }
        return words;
    }

    /**
     * Resets a to-many relationship, making the next get call to query for a fresh result.
     */
    @Generated(hash = 1954400333)
    public synchronized void resetWords() {
        words = null;
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    @Override
    public int hashCode() {
        return (int) chapterId;
    }

    @Override
    public boolean equals(Object object) {
        return object != null &&
                object instanceof Verse
                && ((Verse) object).getChapterId() == (chapterId)
                && ((Verse) object).getVerseId() == (verseId);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1256788451)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getVerseDao() : null;
    }
}

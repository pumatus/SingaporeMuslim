package co.muslimummah.android.module.quran.model;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.JoinProperty;
import org.greenrobot.greendao.annotation.ToMany;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

/**
 * Created by tysheng
 * Date: 20/9/17 12:08 PM.
 * Email: tyshengsx@gmail.com
 */
@Data
@Entity
public class TranslationVerse implements Serializable {
    private static final long serialVersionUID = -4651690704524681194L;
    @Id
    private Long id;
    private int chapterId;
    private int verseId;
    @ToMany(joinProperties = {
            @JoinProperty(name = "chapterId", referencedName = "chapterNum"),
            @JoinProperty(name = "verseId", referencedName = "verseNum")
    })
    private List<TranslationWord> words;

    //add timestamp in
    private long timestamp;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 2135993402)
    private transient TranslationVerseDao myDao;
    @Generated(hash = 628656645)
    public TranslationVerse(Long id, int chapterId, int verseId, long timestamp) {
        this.id = id;
        this.chapterId = chapterId;
        this.verseId = verseId;
        this.timestamp = timestamp;
    }
    @Generated(hash = 2116181777)
    public TranslationVerse() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getChapterId() {
        return this.chapterId;
    }
    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }
    public int getVerseId() {
        return this.verseId;
    }
    public void setVerseId(int verseId) {
        this.verseId = verseId;
    }
    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1146067204)
    public List<TranslationWord> getWords() {
        if (words == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            TranslationWordDao targetDao = daoSession.getTranslationWordDao();
            List<TranslationWord> wordsNew = targetDao
                    ._queryTranslationVerse_Words(chapterId, verseId);
            synchronized (this) {
                if (words == null) {
                    words = wordsNew;
                }
            }
        }
        return words;
    }
    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
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
    public long getTimestamp() {
        return this.timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1422919593)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getTranslationVerseDao() : null;
    }


}

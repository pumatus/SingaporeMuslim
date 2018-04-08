package co.muslimummah.android.storage;

/**
 * Created by frank on 7/4/17.
 */

import java.io.Serializable;

/**
 * Cache Value is a data object that holds data to be stored in Session or SharedPreference.
 * All cache objects in Session or SharedPreference must be of type CacheValue.
 */
@SuppressWarnings("unused")
public class SessionCacheValue implements Serializable {

    private static final long serialVersionUID = 2164077113668197178L;

    private String _key;
    private Object _obj;
    private long _timeStamp;
    //if time for expiry is less than 0
    //then no need to check expiry
    private long _timeExpiry;
    private boolean _flagPersistence;

    private String _constraint;

    private SessionCacheValue(String key, Object obj, boolean flagPersistence) {
        this(key, obj, 0, flagPersistence);
    }

    /**
     * Creates new CacheValue object with following parameters.
     *
     * @param key is the key of that object value
     * @param obj is the object
     * @param timeExpiry expiry time of object
     * @param flagPersistence tells if object should be saved in
     * SharedPreference (persistent memory)
     * @param constraint namespace or pool of CacheValue
     */
    public SessionCacheValue(String key, Object obj,
        long timeExpiry, boolean flagPersistence, String constraint) {
        this._key = key;
        this._obj = obj;
        this._flagPersistence = flagPersistence;
        this._timeExpiry = timeExpiry;
        this._timeStamp = System.currentTimeMillis();
        this._constraint = constraint;
    }

    /**
     * Creates new CacheValue object with following parameters.
     *
     * @param key is the key of that object value
     * @param obj is the object
     * @param timeExpiry expiry time of object
     * @param flagPersistence tells if object should be saved in
     * SharedPreference (persistent memory)
     */
    public SessionCacheValue(String key, Object obj,
        long timeExpiry, boolean flagPersistence) {
        this(key, obj, timeExpiry, flagPersistence, null);
    }

    /**
     * check the cache value whether it is expired.
     *
     * @return true if object is expired
     */
    public boolean isExpired() {
        if (_timeExpiry > 0) {
            long now = System.currentTimeMillis();
            return (now - _timeStamp > _timeExpiry);
        } else {
            return false;
        }
    }

    /**
     * Verify if object belongs to the right constraint.
     *
     * @param constraint namespace or pool of CacheValue
     * @return true if object is valid
     */
    public boolean isValid(String constraint) {
        if (_constraint == null) {
            return true;
        }
        if (constraint == null) {
            return false;
        }
        return (this._constraint.equals(constraint));
    }

    /**
     * @return CacheValue object
     */
    public Object getObj() {
        return _obj;
    }

    @Override
    public String toString() {
        return "SessionCacheValue{" +
            "_constraint='" + _constraint + '\'' +
            ", _key='" + _key + '\'' +
            ", _obj=" + _obj +
            ", _timeStamp=" + _timeStamp +
            ", _timeExpiry=" + _timeExpiry +
            ", _flagPersistence=" + _flagPersistence +
            '}';
    }
}

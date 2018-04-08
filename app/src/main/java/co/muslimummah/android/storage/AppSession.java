package co.muslimummah.android.storage;

/**
 * Created by frank on 7/4/17.
 */

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This AppSession class is created for persistence some simple data objects.<br/>
 * This class will be init inside of Android Application subclass.<br/>
 * It can persistence some simple data objects during Android Application lifecycle.<br/>
 * <br/>
 * It can persistence 2 type of data. <br/>
 * <tr/>1. Data is valid along with Android Application. --Data will be cleared after app been
 * killed.
 * <tr/>2. Data is valid all the time. --Data will be stored in SharedPreference.
 * <br/>
 * <br/>
 * <br/>
 * - TBD<br/>
 * - 1. Now just return the data object. If we need we can return the data object with last
 * update timestamp.
 * <p/>
 */
@SuppressWarnings("unused")
public class AppSession {

    /**
     * Constant String representing class name for logs.
     */
    protected String _tag = ((Object) this).getClass().getSimpleName();

    private static AppSession _self;
    private Context _context;
    private ArrayMap<String, SessionCacheValue> _cacheValueMap;

    private AppSession(Context context) {
        this._context = context.getApplicationContext();
        _cacheValueMap = new ArrayMap<>();
    }

    /**π
     * Retrieve Singleton object of AppSession.
     *
     * @param context of the application.
     * @return AppSession object
     */
    public static AppSession getInstance(Context context) {
        if (_self == null) {
            _self = new AppSession(context);
        }
        return _self;
    }

    /**
     * Cache data object with specific key value.
     *
     * @param key the key of the cached value
     * @param obj the real object which we need to cache.
     * @param timeExpiry set the cached value expiry time
     * @param flagPersistence if it is true data object will be persisted both map
     * and shared preference
     * @param constraint the constraint string
     * @return result. true is success and false is failed.
     */
    public boolean cacheValue(String key, Object obj, long timeExpiry,
        Boolean flagPersistence, String constraint) {
        if (obj == null) {
            //clear value
            if (flagPersistence) {
                return SPUtils.saveObjectToSharedPreference(_context, key, null);
            } else {
                _cacheValueMap.remove(key);
            }
        }
        SessionCacheValue value = new SessionCacheValue(key, obj, timeExpiry, flagPersistence,
            constraint);

        if (flagPersistence) {
            if (obj instanceof Serializable) {
                return SPUtils.saveObjectToSharedPreference(_context, key, value);
            } else {
                Log.e(_tag, "Object[" + obj.getClass().getName() + "] is not serializable. " +
                    "Not support to store it into SharedPreference");
                return false;
            }
        } else {
            _cacheValueMap.put(key, value);
            return true;
        }
    }

    /**
     * Cache data object with specific key value.
     *
     * @param key is the key of that object value
     * @param obj is the object
     * @param timeExpiry expiry time of object
     * @param flagPersistence tells if object should be saved in
     * SharedPreference (persistent memory)
     * @return retrieved object from Session or SharedPreference
     */
    public boolean cacheValue(String key, Object obj, long timeExpiry, Boolean flagPersistence) {
        return cacheValue(key, obj, timeExpiry, flagPersistence, null);
    }

    /**
     * Cache data object with specific key value.
     *
     * @param key is the key of that object value
     * @param obj is the object
     * @param timeExpiry expiry time of object
     * @return retrieved object from Session or SharedPreference
     */
    public boolean cacheValue(String key, Object obj, long timeExpiry) {
        return cacheValue(key, obj, timeExpiry, false);
    }

    /**
     * Cache data object with specific key value.
     *
     * @param key is the key of that object value
     * @param obj is the object
     * @param flagPersistence tells if object should be saved in
     * SharedPreference (persistent memory)
     * @return retrieved object from Session or SharedPreference
     */
    public boolean cacheValue(String key, Object obj, boolean flagPersistence) {
        return cacheValue(key, obj, 0, flagPersistence);
    }

    /**
     * Cache data object with specific key value.
     *
     * @param key is the key of that object value
     * @param obj is the object
     * @return retrieved object from Session or SharedPreference
     */
    public boolean cacheValue(String key, Object obj) {
        return cacheValue(key, obj, 0, false);
    }

    /**
     * Clear the cached value with provided key, both from Session and SharedPreference.
     *
     * @param key is the key of that object value
     */
    public void clearCacheValue(String key) {
        cacheValue(key, null, true);
        _cacheValueMap.remove(key);
    }

    /**
     * Clear all the cached values, both from Session and SharedPreference.
     */
    public void clearAll() {
        _cacheValueMap = new ArrayMap<>();
        SPUtils.clearSharedPreference(_context);
    }

    /**
     * Retrieve cached data object from AppSession.
     *
     * @param key is the key of that object value
     * @param constraint namespace or pool of SessionCacheValue
     * @param type class of object for type casting
     * @param <T> the object type
     * @return retrieved object from Session or SharedPreference
     */
    public <T extends Object> T getCachedValue(String key, String constraint, Class<T> type) {
        SessionCacheValue value;
        if (_cacheValueMap.containsKey(key)) {
            //hit in cache map
            value = _cacheValueMap.get(key);
        } else {
            //try to hit in SharedPreference
            try {
                value = SPUtils.retrieveObjectFromSharedPreference(_context, key,
                    SessionCacheValue.class);
            } catch (Exception e) {
                clearCacheValue(key);
                return null;
            }
        }
        if (value == null || value.isExpired() || !value.isValid(constraint)) {
            return null;
        }
        Object obj = value.getObj();
        if (obj != null && obj.getClass().getName().equals(type.getName())) {
            try {
                return type.cast(obj);
            } catch (ClassCastException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /**
     * Retrieve cached data object from AppSession.
     *
     * @param key is the key of that object value
     * @param type class of object for type casting
     * @param <T> the object type
     * @return retrieved object from Session or SharedPreference
     */
    public <T extends Object> T getCachedValue(String key, Class<T> type) {
        return getCachedValue(key, null, type);
    }

    /**
     * Retrieve cached data ArrayList from AppSession.
     *
     * @param key is the key of that object value
     * @param type class of object for type casting
     * @param <T> the object type
     * @return retrieved ArrayList from Session or SharedPreference
     */
    public <T extends Object> ArrayList<T> getCachedList(String key, Class<T> type) {
        if (_cacheValueMap.containsKey(key)) {
            //hit in cache map
            SessionCacheValue value = _cacheValueMap.get(key);
            if (value.isExpired()) {
                return null;
            }
            Object obj = value.getObj();
            if (obj != null && obj instanceof ArrayList) {
                //cast to generic type ArrayList
                ArrayList<T> castObject = (ArrayList<T>) obj;
                //check type
                if (castObject != null && castObject.size() > 0
                    && castObject.get(0).getClass() == type) {
                    return castObject;
                }
            }
        } else {
            //try to hit in SharedPreference
            return SPUtils.retrieveArrayListFromSharedPreference(_context, key, type);
        }
        return null;
    }
}

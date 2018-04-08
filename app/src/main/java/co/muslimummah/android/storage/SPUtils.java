package co.muslimummah.android.storage;

/**
 * Created by frank on 7/4/17.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * SharedPreference class will hold all methods related to Android SharedPreferences.
 * Methods in this class enable developers to use SharedPreferences with minimal effort across
 * multiple projects.
 */
public class SPUtils {

    /**
     * Value - {@value}, key for UTF charset name.
     */
    private static final String UTF_8 = "UTF-8";

    /**
     * Value - {@value}, key for ISO charset name.
     */
    private static final String ISO_8859_1 = "ISO-8859-1";

    /**
     * @param name of SharedPreference object.
     * @param context of the application
     * @return Application's {@code SharedPreferences}.
     */
    public static SharedPreferences getSharedPreferences(String name, Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    /**
     * save object to shared preference.
     *
     * @param context of the application
     * @param key is the key of that object value
     * @param obj is the object
     * @return true if object is saved successfully
     */
    public static boolean saveObjectToSharedPreference(Context context, String key,
        Serializable obj) {
        if (obj == null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putString(key, null).apply();
            return true;
        }
        String res = serializeObjectToString(obj);
        if (res != null) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putString(key, res).apply();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Clear all the data in SharedPreference.
     *
     * @param context of the application
     */
    public static void clearSharedPreference(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.commit();
    }

    /**
     * Retrieve object from shared preference with specific key.
     *
     * @param context of the application
     * @param key is the key of that object value
     * @param clazz of object for type casting
     * @param <T> the object type
     * @return retrieved object from SharedPreference
     */
    public static <T extends Object> T retrieveObjectFromSharedPreference(
        Context context, String key, Class<T> clazz) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String res = sp.getString(key, null);
        if (res != null) {
            Object obj = deserializeObjectFromString(res);
            if (obj != null && obj.getClass().getName().equals(clazz.getName())) {
                return clazz.cast(obj);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * retrieve ArrayList from shared preference with specific key.
     *
     * @param context of the application
     * @param key is the key of that object value
     * @param clazz of object for type casting
     * @param <T> the object type
     * @return retrieved list from SharedPreference
     */
    public static <T extends Object> ArrayList<T> retrieveArrayListFromSharedPreference(
        Context context, String key, Class<T> clazz) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String res = sp.getString(key, null);
        if (res != null) {
            Object obj = deserializeObjectFromString(res);
            if (obj != null && obj instanceof SessionCacheValue) {
                obj = ((SessionCacheValue) obj).getObj();
                if (obj != null && obj instanceof ArrayList) {
                    //cast to generic type ArrayList
                    ArrayList<T> castObject = (ArrayList<T>) obj;
                    //check type
                    if (castObject != null && castObject.size() > 0
                        && castObject.get(0).getClass() == clazz) {
                        return castObject;
                    }
                }
            }

        }
        return null;
    }

    /**
     * Returns serialized string of provided object.
     *
     * @param obj Serializable object.
     * @return Serialized string of provided object.
     * @see #deserializeObjectFromString(String)
     */
    public static String serializeObjectToString(Serializable obj) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        String strSerialized = null;
        try {
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(obj);
            strSerialized = byteArrayOutputStream.toString(ISO_8859_1);
            strSerialized = URLEncoder.encode(strSerialized, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return strSerialized;
    }

    /**
     * Returns serializable object from provided string.
     *
     * @param str Serialized String value of serializable object.
     * @return Serializable object
     * @see #serializeObjectToString(Serializable)
     */
    private static Object deserializeObjectFromString(String str) {
        Object obj = null;
        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInputStream = null;
        try {
            String redStr = java.net.URLDecoder.decode(str, UTF_8);
            byteArrayInputStream = new ByteArrayInputStream(
                redStr.getBytes(ISO_8859_1));
            objectInputStream = new ObjectInputStream(
                byteArrayInputStream);
            obj = objectInputStream.readObject();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (OptionalDataException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (byteArrayInputStream != null) {
                try {
                    byteArrayInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return obj;
    }
}

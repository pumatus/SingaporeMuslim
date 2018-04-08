package co.muslimummah.android.util;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.view.Gravity;
import android.widget.Toast;

import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.data.model.PrayerTimeLocationInfo;
import co.muslimummah.android.storage.AppSession;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.module.prayertime.data.model.ContactInfo;
import timber.log.Timber;

/**
 * Created by Hongd on 2017/7/10.
 */


public class PhoneInfoUtils {

    public static JSONArray buildAllStringContacts(Context context) throws JSONException {
        if (context != null) {
            ArrayList<ContactInfo> contactInfoList = new ArrayList<>();
            JSONArray jsonArray = new JSONArray();
            Gson gson = new Gson();
            Uri uri = Uri.parse("content://com.android.contacts/contacts");
            ContentResolver ctxContentResolver = context.getContentResolver();
            Cursor cursor = ctxContentResolver.query(uri, null, null, null, null);
            String id;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    ContactInfo contactInfo = new ContactInfo();
                    id = cursor.getString(cursor.getColumnIndex(Contacts._ID));
                    String name = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
                    Cursor phone = ctxContentResolver
                        .query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = " + id, null, null);
                    contactInfo.setId(Integer.parseInt(id));
                    contactInfo.setName(name);
                    List<String> str = new ArrayList<>();
                    //phone
                    if (phone != null) {
                        while (phone.moveToNext()) {
                            str.add(phone.getString(phone.getColumnIndex(Phone.NUMBER)));
                        }
                        contactInfo.setPhone(Arrays.toString(str.toArray()));
                        str.clear();
                        phone.close();
                    }
                }
                cursor.close();
            }
            for (int i = 0; i < contactInfoList.size(); i++) {
                ContactInfo contactInfo = contactInfoList.get(i);
                JSONObject stoneObject = new JSONObject();
                stoneObject
                    .put("name", contactInfo.getName() == null ? "null" : contactInfo.getName());
                stoneObject.put("phone", contactInfo.getPhone().replace(" ", "") == null ? "null"
                    : contactInfo.getPhone().replace(" ", ""));
                jsonArray.put(stoneObject);
            }
            return jsonArray;
        }
        return null;
    }

    /**
     * Check NetWork Connected
     */
    public static boolean isNetworkEnable(Context context) {
        ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static boolean isGPSEnable(Context context) {
        if (context != null) {
            LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//            boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (network) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWifiEnable(Context context) {
        if (context != null) {
            ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return networkInfo.isAvailable();
            }
        }
        return false;
    }

    public static boolean isMobileEnable(Context context) {
        if (context != null) {
            ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                return networkInfo.isAvailable();
            }
        }
        return false;
    }

    public static void openGPSSetting(Context context) {
        Intent GPSIntent = new Intent();
        GPSIntent.setClassName("com.android.settings",
            "com.android.settings.widget.SettingsAppWidgetProvider");
        GPSIntent.addCategory("android.intent.category.ALTERNATIVE");
        GPSIntent.setData(Uri.parse("custom:3"));
        try {
            PendingIntent.getBroadcast(context, 0, GPSIntent, 0).send();
        } catch (CanceledException e) {
            e.printStackTrace();
        }
    }

    public static void openWiFiSetting(Context context, boolean enabled) {
        WifiManager wm = (WifiManager) context
            .getSystemService(Context.WIFI_SERVICE);
        wm.setWifiEnabled(enabled);
    }

    /**
     * Show Toast Message
     */
    public static void showToast(String message, boolean center) {
        if (center) {
            Toast toast = Toast.makeText(OracleApp.getInstance(), message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else {
            Toast.makeText(OracleApp.getInstance(), message, Toast.LENGTH_LONG).show();
        }
    }

    public static void showToast(int message, boolean center) {
        if (center) {
            Toast toast = Toast.makeText(OracleApp.getInstance(), message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else {
            Toast.makeText(OracleApp.getInstance(), message, Toast.LENGTH_LONG).show();
        }
    }
}
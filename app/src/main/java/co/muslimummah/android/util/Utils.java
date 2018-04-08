package co.muslimummah.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import co.muslimummah.android.OracleApp;

/**
 * Created by frank on 7/27/17.
 */

public class Utils {
    private static String sUserAgent;

    /**
     * Value - {@value}, key for message digest algorithm.
     */
    public static final String SHA_256 = "SHA-256";

    /**
     * Value - {@value}, key for UTF charset name.
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * Returns SHA-256 hash of given string.
     * <p/>
     * Gets message digest algorithm SHA-256 from MessageDigest class, computes hash of the given
     * String and return the computed value.
     *
     * @param base Input string for which hash is required.
     * @return SHA256                   Returns SHA-256 hash of base String
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException is thrown in case MessageDigest
     *                                  class cannot find SHA-256 algorithm.
     */
    @NonNull
    public static String computeSHA256Hash(String base) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            //SHAT-256 is definitely supported so there won't be NoSuchAlgorithmException here.
            e.printStackTrace();
            return null;
        }

        byte[] byteData = new byte[0];
        try {
            byteData = digest.digest(base.getBytes(UTF_8));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringBuilder stringBuffer = new StringBuilder();

        for (byte aByteData : byteData) {
            stringBuffer.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
        }
        return stringBuffer.toString();
    }

    public static String md5(@NonNull String string) {
        if (TextUtils.isEmpty(string)) return "";

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest((string).getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SuppressLint("HardwareIds")
    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @SuppressLint("HardwareIds")
    public static String getImei(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDeviceId();
        //new version
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
//            return telephonyManager.getImei();
//        } else {
//            return telephonyManager.getDeviceId();
//        }

    }

    public static String getMacAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String macAddress = wifiInfo.getMacAddress();
        return macAddress == null ? "" : macAddress;
    }

    public static String getDeviceId(Context context) {
        String deviceIdOriginal = getAndroidId(context) +
                getImei(context) +
                getMacAddress(context);
        return computeSHA256Hash(deviceIdOriginal);
    }

    public static String getUserAgent(Context context) {
        if (sUserAgent == null) {
            //User-Agent: Oracle-User/1.2.3 (Huawei P7; Android 5.1)
            sUserAgent = "Oracle-User/" + getVersionName(context) + " (" + Build.MANUFACTURER + " " + Build.MODEL + "; Android " + Build.VERSION.RELEASE + "; " + Locale.getDefault().toString() + "; " + getDeviceId(context) + "; " + getImei(context) + "; " + getMacAddress(context) + ")";
        }
        return sUserAgent;
    }

    public static Activity extractActivity(Context context) {
        Context tmpContext = context;
        while (tmpContext instanceof ContextWrapper) {
            if (tmpContext instanceof Activity) {
                return (Activity) tmpContext;
            } else {
                tmpContext = ((ContextWrapper) tmpContext).getBaseContext();
            }
        }
        return null;
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) OracleApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isAvailable();
    }
}

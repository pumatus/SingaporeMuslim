package co.muslimummah.android.util;

import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import co.muslimummah.android.OracleApp;
import moe.banana.support.ToastCompat;

/**
 * Created by tysheng
 * Date: 2016/10/26 10:06.
 * Email: tyshengsx@gmail.com
 */

public class ToastUtil {
    private static Toast toast = null;

    public static void show(String s) {
        show(s, Toast.LENGTH_SHORT);
    }

    /**
     * 先检测通知是否开启
     */
    public static void show(String s, int duration) {
        if (NotificationManagerCompat.from(OracleApp.getInstance()).areNotificationsEnabled()) {
            if (toast == null) {
                toast = Toast.makeText(OracleApp.getInstance(), s, duration);
            } else {
                toast.setText(s);
            }
            toast.show();
        } else {
            ToastCompat.makeText(OracleApp.getInstance(), s, duration).show();
        }
    }
}

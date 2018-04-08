package co.muslimummah.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import java.lang.reflect.InvocationTargetException;

import co.muslimummah.android.OracleApp;

/**
 * Created by frank on 8/1/17.
 */

public class UiUtils {
    private static Typeface uthmani;
    private static Typeface oracleRegular;

    public static Point getNavigationBarSize(Context context) {
        Point appUsableSize = getAppUsableScreenSize(context);
        Point realScreenSize = getRealScreenSize(context);

        // navigation bar on the right
        if (appUsableSize.x < realScreenSize.x) {
            return new Point(realScreenSize.x - appUsableSize.x, appUsableSize.y);
        }

        // navigation bar at the bottom
        if (appUsableSize.y < realScreenSize.y) {
            return new Point(appUsableSize.x, realScreenSize.y - appUsableSize.y);
        }

        // navigation bar is not present
        return new Point();
    }

    public static Point getAppUsableScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            } catch (NoSuchMethodException e) {
            }
        }
        return size;
    }

    public static int getNavigationBarHeight(Context context) {
        int navigationBarHeight = 0;
        int resourceId;
        if (!ViewConfiguration.get(context).hasPermanentMenuKey() && !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)) {
            resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navigationBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            }
        } else {
            navigationBarHeight = UiUtils.getNavigationBarSize(context).y;
        }
        return navigationBarHeight;
    }

    public static int getStatusBarHeight(Context context) {
        // status bar height
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public static Typeface getArabicFont() {
        if (uthmani == null) {
            uthmani = Typeface.createFromAsset(OracleApp.getInstance().getAssets(), "fonts/uthmani.otf");
        }
        return uthmani;
    }


    public static Typeface getTransliterationFont() {
        if (oracleRegular == null) {
            oracleRegular = Typeface.createFromAsset(OracleApp.getInstance().getAssets(), "fonts/oracle-regular.ttf");
        }
        return oracleRegular;
    }

    public static Typeface getTypefaceFromAssetPath(Context context, String assetPath) {
        return Typeface.createFromAsset(context.getAssets(), assetPath);
    }

    public static int dp2px(float dpValue) {

        return (int) (dpValue * Resources.getSystem().getDisplayMetrics().density + 0.5f);
    }

    public static int px2dp(float pxValue) {

        return (int) (pxValue / Resources.getSystem().getDisplayMetrics().density + 0.5f);
    }

    public static int getScreenWidth() {

        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static int[] getLocationsInWindow(View view) {
        int[] ints = new int[2];
        view.getLocationInWindow(ints);
        return ints;
    }

    public static int[] getLocationsOnScreen(View view) {
        int[] ints = new int[2];
        view.getLocationOnScreen(ints);
        return ints;
    }

    public static String getText(int res, Object... formatArgs) {
        return OracleApp.getInstance().getResources().getString(res, formatArgs);
    }

    public static String getText(int res) {
        return OracleApp.getInstance().getResources().getString(res);
    }

    public static int getColor(int res) {
        return ContextCompat.getColor(OracleApp.getInstance(), res);
    }
}

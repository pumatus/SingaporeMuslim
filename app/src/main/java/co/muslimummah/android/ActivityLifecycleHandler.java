package co.muslimummah.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import co.muslimummah.android.analytics.OracleAnalytics;

/**
 * Created by Xingbo.Jie on 7/8/17.
 */

public class ActivityLifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private static ActivityLifecycleHandler instance;

    private int started;
    private int stopped;
    private boolean isAppVisible = true;

    private ActivityLifecycleHandler() {
    }

    public static ActivityLifecycleHandler instance() {
        if (instance == null) {
            synchronized (ActivityLifecycleHandler.class) {
                if (instance == null) {
                    instance = new ActivityLifecycleHandler();
                }
            }
        }
        return instance;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        started += 1;

        if (!isAppVisible) {
            if (started > stopped) {
                isAppVisible = true;
                onAppForground();
            }
        }
    }

    private void onAppForground() {
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        stopped += 1;
        if (started <= stopped) {
            isAppVisible = false;
            onAppBackground();
        }
    }

    private void onAppBackground() {
        OracleAnalytics.INSTANCE.reportLog();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public boolean isAppVisible() {
        return isAppVisible;
    }
}

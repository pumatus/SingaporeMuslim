package co.muslimummah.android.analytics;

/**
 * Created by Xingbo.Jie on 28/8/17.
 */

public interface Analytics {
    int LOG_UPLOAD_THRESHOLD_COUNT = 100;
    long LOG_UPLOAD_THRESHOLD_INTERVAL = 60 * 60 * 1000; //one hour

    void addLog(LogObject logObject);
    void reportLog();
}

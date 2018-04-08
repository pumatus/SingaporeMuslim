package co.muslimummah.android.analytics;

import io.reactivex.Observable;

/**
 * Created by Xingbo.Jie on 28/8/17.
 */

interface LogUploadWorker {
    Observable uploadPVLog(String logPath);
}

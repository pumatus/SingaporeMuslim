package co.muslimummah.android.analytics;

import co.muslimummah.android.network.ApiFactory;
import co.muslimummah.android.network.ApiService;
import co.muslimummah.android.network.Entity.body.UploadLog;
import io.reactivex.Observable;

/**
 * Created by Xingbo.Jie on 28/8/17.
 */

public class OracleAnalytics {
    public static Analytics INSTANCE = new AnalyticsImpl("oracle.log", new LogUploadWorker() {
        @Override
        public Observable uploadPVLog(String logPath) {
            return ApiFactory
                    .get(ApiService.class)
                    .uploadUserLogs(UploadLog.builder()
                            .logPath(logPath)
                            .build());
        }
    });
}

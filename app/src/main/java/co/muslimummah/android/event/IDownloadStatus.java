package co.muslimummah.android.event;

/**
 * Created by Xingbo.Jie on 5/10/17.
 */

public interface IDownloadStatus {
    int STATUS_START = 1;
    int STATUS_DOWNLODING = 2;
    int STATUS_COMPLETE = 3;
    int STATUS_ERROR = 4;
    int STATUS_PROCESS = 5;
}

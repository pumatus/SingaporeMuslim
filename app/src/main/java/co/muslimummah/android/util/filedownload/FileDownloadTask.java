package co.muslimummah.android.util.filedownload;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.storage.DownloadManagerPro;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import lombok.Data;
import timber.log.Timber;

/**
 * Created by Xingbo.Jie on 3/10/17.
 */
@Data
class FileDownloadTask implements Serializable {
    private DownloadParam param;
    private DownloadManager downloadManager;
    private long downloadId = -1;
    private DownloadManagerPro downloadManagerPro;
    private Disposable progressDisposable;
    private DownloadListener downloadListener;
    private boolean canceled;
    private volatile int progress;
    private BroadcastReceiver broadcastReceiver;

    interface DownloadListener {
        void onStart(FileDownloadTask task, long id);

        /**
         * @param task
         * @param progress 0 to 100
         */
        void onProgressUpdate(FileDownloadTask task, int progress);

        void onError(FileDownloadTask task, Throwable throwable);

        void onComplete(FileDownloadTask task, File destination);
    }

    public FileDownloadTask(DownloadParam downloadParam) {
        this.param = downloadParam;
        this.downloadManager = (DownloadManager) OracleApp.getInstance().getSystemService(Context.DOWNLOAD_SERVICE);
        this.downloadManagerPro = new DownloadManagerPro(downloadManager);
    }

    public void cancel() {
        canceled = true;
        stopReportProgress();

        if (broadcastReceiver != null) {
            OracleApp.getInstance().unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }

        if (downloadId != -1) {
            downloadManagerPro.cancelDownload(downloadId);
        }

        if (downloadListener != null) {
            downloadListener.onError(this, new RuntimeException("user canceled"));
        }
    }

    public void execute(DownloadListener downloadListener) {
        progress = 0;
        if (param == null
                || TextUtils.isEmpty(param.getUrl())
                || TextUtils.isEmpty(param.getDstFilePath())) {
            if (downloadListener != null) {
                downloadListener.onError(this, new IllegalStateException("Download Param is NULL"));
            }
            return;
        }

        File dstFile = new File(param.getDstFilePath());
        if (dstFile.exists()) {
            if (downloadListener != null) {
                downloadListener.onComplete(this, dstFile);
            }
        } else {
            Timber.d("download file start : %s", param.getUrl());
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(param.getUrl()));
            request.setDestinationUri(Uri.fromFile(dstFile));
            request.setVisibleInDownloadsUi(false);

            if (param.getTitle() != null & param.getDescription() != null) {
                request.setTitle(param.getTitle());
                request.setDescription(param.getDescription());
            } else {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            }

            this.downloadListener = downloadListener;
            downloadId = downloadManager.enqueue(request);
            if (downloadListener != null) {
                broadcastReceiver = new DownloadCompleteBroadcastReceiver(this, downloadListener, downloadManager, downloadId, dstFile);
                OracleApp.getInstance().registerReceiver(broadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                downloadListener.onStart(this, downloadId);
            }
        }
    }

    private static class DownloadCompleteBroadcastReceiver extends BroadcastReceiver {
        private FileDownloadTask task;
        private DownloadListener listener;
        private long downloadId;
        private File destination;

        private DownloadCompleteBroadcastReceiver(FileDownloadTask task, DownloadListener listener, DownloadManager downloadManager, long downloadId, File destination) {
            this.task = task;
            this.listener = listener;
            this.downloadId = downloadId;
            this.destination = destination;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (downloadId == intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)) {
                OracleApp.getInstance().unregisterReceiver(this);
                // Download manager downloads the file to external storage which means that
                // the uri is with the prefix of "file://" so it is safe to get the file by
                // path.
//                    Uri downloadedUri = mDownloadManager.getUriForDownloadedFile(mDownloadId);
                if (destination.exists()) {
                    //Download successfully
//                        File downloadedFile = new File(downloadedUri.getPath());
                    Timber.d("download file onComplete : %s", task.getParam().getUrl());
                    if (listener != null) {
                        listener.onComplete(task, destination);
                    }
                } else {
                    Timber.d("download file onError : %s", task.getParam().getUrl());
                    //Download failed due to user canceled or other reason.
                    if (listener != null) {
                        listener.onError(task, new RuntimeException("Download failed due to user canceled or other reason"));
                    }
                }
            }
        }
    }

    synchronized void startReportProgress() {
        Timber.d("download file startReportProgress : 1");
        if (downloadManagerPro == null || downloadListener == null) {
            return;
        }

        Timber.d("download file startReportProgress : 2");
        stopReportProgress();
        progressDisposable = Observable.interval(200, TimeUnit.MILLISECONDS)
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long aLong) throws Exception {
                        Timber.d("download file progress : run");
                        if (downloadManagerPro == null || downloadListener == null) {
                            stopReportProgress();
                            return;
                        }

                        Timber.d("download file progress : run 2");
                        int[] downloadBytesAndStatus = getDownloadManagerPro().getBytesAndStatus(downloadId);
                        int downloadedBytes = downloadBytesAndStatus[0];
                        int totalBytes = downloadBytesAndStatus[1];
                        int downloadStatus = downloadBytesAndStatus[2];
                        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                            progress = 100;
                            stopReportProgress();
                        } else {
                            progress = (int) (downloadedBytes * 100L / totalBytes);
                        }
                        Timber.d("download file progress : %d", progress);
                        downloadListener.onProgressUpdate(FileDownloadTask.this, progress);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        stopReportProgress();
                    }
                });
    }

    synchronized void stopReportProgress() {
        if (progressDisposable != null && !progressDisposable.isDisposed()) {
            progressDisposable.dispose();
            progressDisposable = null;
        }
    }
}

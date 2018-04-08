package co.muslimummah.android.util.filedownload;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.HashMap;

import co.muslimummah.android.event.IDownloadStatus;
import co.muslimummah.android.event.Quran;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.util.PhoneInfoUtils;
import co.muslimummah.android.util.ToastUtil;
import co.muslimummah.android.util.Utils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import timber.log.Timber;

/**
 * Created by Xingbo.Jie on 4/10/17.
 */

public class FileDownloadManager {
    public final static FileDownloadManager INSTANCE = new FileDownloadManager();
    private HashMap<DownloadParam, FileDownloadTask> runningTasks;

    private FileDownloadManager() {
        runningTasks = new HashMap<>();
    }

    /**
     * Get status update from subscribe event {@link Quran.DownloadStatus}
     *
     * @param param
     */
    public boolean download(DownloadParam param) {
        if (!PhoneInfoUtils.isNetworkEnable(OracleApp.getInstance())) {
            Observable.just(OracleApp.getInstance().getString(R.string.no_internet_connection))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(@NonNull String text) throws Exception {
                            ToastUtil.show(text);
                        }
                    });
            return false;
        }

        if (param != null) {
            FileDownloadTask task = new FileDownloadTask(param);
            runningTasks.put(param, task);
            task.execute(downloadListener);
            return true;
        }
        return false;
    }

    public Observable<String> downloadRx(DownloadParam param) {
        return Observable.create(new FileDownloadOnSubscribe(param));
    }

    private final FileDownloadTask.DownloadListener downloadListener = new FileDownloadTask.DownloadListener() {
        @Override
        public void onStart(FileDownloadTask task, long id) {
            EventBus.getDefault()
                    .post(createDownloadStatus(task, Quran.DownloadStatus.STATUS_START));

            task.startReportProgress();
            Timber.d("download file FileDownloadManager onStart : %s", task.getParam().getUrl());
        }

        @Override
        public void onProgressUpdate(FileDownloadTask task, int progress) {
            EventBus.getDefault()
                    .post(createDownloadStatus(task, Quran.DownloadStatus.STATUS_DOWNLODING));
            Timber.d("download file FileDownloadManager onProgressUpdate : %s", task.getParam().getUrl());
        }

        @Override
        public void onError(FileDownloadTask task, Throwable throwable) {
            runningTasks.remove(task.getParam());
            
            EventBus.getDefault()
                    .post(createDownloadStatus(task, Quran.DownloadStatus.STATUS_ERROR));
            task.stopReportProgress();

            Timber.d("download file FileDownloadManager onError : %s", task.getParam().getUrl());
        }

        @Override
        public void onComplete(FileDownloadTask task, File destination) {
            runningTasks.remove(task.getParam());

            EventBus.getDefault()
                    .post(createDownloadStatus(task, Quran.DownloadStatus.STATUS_COMPLETE));
            task.stopReportProgress();

            Timber.d("download file FileDownloadManager onComplete : %s", task.getParam().getUrl());
        }
    };

    public void cancel(DownloadParam param) {
        FileDownloadTask task = runningTasks.remove(param);
        if (task != null) {
            task.cancel();
            Timber.d("download file FileDownloadManager cancel : %s", param.getUrl());
        }
    }

    private Quran.DownloadStatus createDownloadStatus(FileDownloadTask task, int status) {
        return Quran.DownloadStatus.builder()
                .param(task.getParam())
                .id(task.getDownloadId())
                .status(status)
                .progress(task.getProgress())
                .canceled(task.isCanceled())
                .build();
    }

    public static class FileDownloadOnSubscribe implements ObservableOnSubscribe<String> {
        DownloadParam param;
        ObservableEmitter<String> emitter;

        public FileDownloadOnSubscribe(DownloadParam param) {
            this.param = param;
        }

        @Override
        public void subscribe(@io.reactivex.annotations.NonNull ObservableEmitter<String> e) throws Exception {
            emitter = e;
            e.setDisposable(Disposables.fromRunnable(new Runnable() {
                @Override
                public void run() {
                    FileDownloadManager.INSTANCE.cancel(param);
                }
            }));

            if (!e.isDisposed()) {
                EventBus.getDefault().register(this);
                FileDownloadManager.INSTANCE.download(param);
            }
        }

        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onDownloadStatusUpdate(Quran.DownloadStatus status) {
            if (!status.getParam().equals(param)) {
                return;
            }

            switch (status.getStatus()) {
                case IDownloadStatus.STATUS_COMPLETE:
                    EventBus.getDefault().unregister(this);
                    emitter.onNext(param.getDstFilePath());
                    emitter.onComplete();
                    break;
                case IDownloadStatus.STATUS_ERROR:
                    EventBus.getDefault().unregister(this);
                    emitter.onError(new RuntimeException("File download failed"));
                    break;
                default:
                    break;
            }

        }

    }
    /**
     * Generate the file path by url
     *
     * @param context
     * @param url
     * @return
     */
    public String generatePublicPath(Context context, String url) {
        return generatePublicPath(context, url, false);
    }

    /**
     * @param context
     * @param url
     * @param addSuffix real suffix, e.g. .mp4, .png
     * @return
     */
    public String generatePublicPath(Context context, String url, boolean addSuffix) {
        String fileName = Utils.computeSHA256Hash(url);
        String suffix = "";
        if (addSuffix) {
            int position = url.lastIndexOf(".");
            if (position != -1 && position < url.length()) {
                suffix = url.substring(position);
            }
        }
        File tmpFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName + suffix);
        return tmpFile.getAbsolutePath();
    }

    /**
     * Generate the file path by url
     */
    public String generateInternalPath(Context context, String url) {
        return generateInternalPath(context, url, false);
    }

    public String generateInternalPath(Context context, String url, boolean addSuffix) {
        String fileName = Utils.computeSHA256Hash(url);
        String suffix = "";
        if (addSuffix) {
            int position = url.lastIndexOf(".");
            if (position != -1 && position < url.length()) {
                suffix = url.substring(position);
            }
        }
        File tmpFile = new File(context.getFilesDir(), fileName + suffix);
        return tmpFile.getAbsolutePath();
    }

}

package co.muslimummah.android.analytics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import co.muslimummah.android.BuildConfig;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.storage.AppSession;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;
import timber.log.Timber;

/**
 * Created by Xingbo.Jie on 28/8/17.
 */

class AnalyticsImpl implements Analytics {
    private final static String KEY_LAST_UPLOAD_TIMESTAMP = "a_k_l_u_t";
    private String fileName;
    private LogUploadWorker uploadWorker;
    private Disposable lastUploadDisposable;

    private volatile int logCount;

    AnalyticsImpl(String fileName, LogUploadWorker worker) {
        this.fileName = fileName;
        this.uploadWorker = worker;
        logCount = initLogCount();
    }

    @Override
    public void addLog(LogObject logObject) {
        Observable.just(logObject)
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<LogObject>() {
                    @Override
                    public void accept(@NonNull LogObject logObject) throws Exception {
                        saveLog(logObject);
                        if (logCount >= LOG_UPLOAD_THRESHOLD_COUNT) {
                            reportLog();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                    }
                });
    }

    @Override
    public synchronized void reportLog() {
        if (lastUploadDisposable != null) {
            return;
        }


        Long lastTimestamp = AppSession.getInstance(OracleApp.getInstance())
                .getCachedValue(KEY_LAST_UPLOAD_TIMESTAMP, Long.class);
        if (lastTimestamp != null && System.currentTimeMillis() - lastTimestamp <= LOG_UPLOAD_THRESHOLD_INTERVAL) {
            return;
        }

        File masterFile = new File(getFilePath());
        final File suplementFile = new File(getSupplementFilePath());
        if (!suplementFile.exists() && masterFile.exists()) {
            // noinspection ResultOfMethodCallIgnored
            masterFile.renameTo(suplementFile);
            logCount = 0;
        }

        if (!suplementFile.exists()) {
            return;
        }

        if (suplementFile.length() > 0) {
            lastUploadDisposable = uploadWorker.uploadPVLog(getSupplementFilePath())
                    .observeOn(Schedulers.io())
                    .doOnTerminate(new Action() {
                        @Override
                        public void run() throws Exception {
                            lastUploadDisposable = null;
                        }
                    })
                    .subscribe(new Consumer() {
                        @Override
                        public void accept(@NonNull Object o) throws Exception {
                            AppSession.getInstance(OracleApp.getInstance())
                                    .cacheValue(KEY_LAST_UPLOAD_TIMESTAMP, System.currentTimeMillis(), true);
                            if (suplementFile.exists()) {
                                suplementFile.delete();
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@NonNull Throwable throwable) throws Exception {
                            if (throwable instanceof HttpException) {
                                int code = ((HttpException) throwable).code();
                                if (code >= 400 && code < 500) {
                                    if (suplementFile.exists()) {
                                        suplementFile.delete();
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private int initLogCount() {
        try {
            RandomAccessFile file = new RandomAccessFile(getFilePath(), "r");
            if (file.length() > 0) {
                return file.readInt();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private synchronized boolean saveLog(LogObject object) {
        try {
            RandomAccessFile file = new RandomAccessFile(getFilePath(), "rw");
            if (logCount == 0) {
                file.writeInt(logCount + 1);
                file.write('\n');
                file.writeBytes(object.toString() + "\n");
            } else {
                file.seek(0);
                file.writeInt(logCount + 1);
                file.seek(file.length());
                file.writeBytes(object.toString() + "\n");
            }
            logCount += 1;
            if (BuildConfig.DEBUG) {
                Timber.d("USER_LOG: %s; count %d", object.toString(), logCount);
            }
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    String getSupplementFilePath() {
        String path = OracleApp.getInstance().getCacheDir().getAbsolutePath();
        return path + File.separator + fileName + ".sub";
    }

    String getFilePath() {
        String path = OracleApp.getInstance().getCacheDir().getAbsolutePath();
        return path + File.separator + fileName;
    }
}

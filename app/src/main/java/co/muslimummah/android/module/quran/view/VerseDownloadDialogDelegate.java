package co.muslimummah.android.module.quran.view;

import android.app.Activity;
import android.view.View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;

import co.muslimummah.android.event.Quran;
import co.muslimummah.android.R;
import co.muslimummah.android.util.filedownload.DownloadParam;
import co.muslimummah.android.util.filedownload.FileDownloadManager;
import timber.log.Timber;

/**
 * Created by Xingbo.Jie on 4/10/17.
 */

public class VerseDownloadDialogDelegate {
    Activity context;

    private OnActionListener onActionListener;
    private DownloadStateDialog downloadStateDialog;
    private Quran.VerseDownloadStatus verseDownloadStatus;

    public void setOnActionListener(OnActionListener onActionListener) {
        this.onActionListener = onActionListener;
    }

    public interface OnActionListener {
        void onCacelClick(DownloadParam param);
        void onRetryClick(DownloadParam param);
    }

    public VerseDownloadDialogDelegate(Activity context) {
        this.context = context;
        downloadStateDialog = new DownloadStateDialog(context);
        downloadStateDialog.setBottomButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (verseDownloadStatus != null) {
                    switch (verseDownloadStatus.getDownloadStatus().getStatus()) {
                        case Quran.VerseDownloadStatus.STATUS_DOWNLODING:
                        case Quran.VerseDownloadStatus.STATUS_START:
                            //cancel
                            FileDownloadManager.INSTANCE.cancel(verseDownloadStatus.getDownloadStatus().getParam());
                            break;
                        case Quran.VerseDownloadStatus.STATUS_ERROR:
                            //retry
                            if (onActionListener != null) {
                                onActionListener.onRetryClick(verseDownloadStatus.getDownloadStatus().getParam());
                            }
                            break;
                    }
                }
                downloadStateDialog.dismiss();
            }
        });
    }

    public void register() {
        EventBus.getDefault().register(this);
    }

    public void unRegister() {
        EventBus.getDefault().unregister(this);
        if (downloadStateDialog != null && downloadStateDialog.isShowing()) {
            downloadStateDialog.dismiss();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadEvent(Quran.VerseDownloadStatus verseDownloadStatus) {
        this.verseDownloadStatus = verseDownloadStatus;
        if (verseDownloadStatus.getDownloadStatus().isCanceled()) {
            if (downloadStateDialog.isShowing()) {
                downloadStateDialog.dismiss();
            }
            return;
        }

        Timber.d("download file onDownloadEvent : %d", verseDownloadStatus.getDownloadStatus().getStatus());
        switch (verseDownloadStatus.getDownloadStatus().getStatus()) {
            case Quran.VerseDownloadStatus.STATUS_PROCESS:
                if (!downloadStateDialog.isShowing()) {
                    downloadStateDialog.show();
                }

                downloadStateDialog.setContent(context.getString(R.string.processing));
                downloadStateDialog.setButtonText(context.getString(R.string.cancel));
                downloadStateDialog.setButtonEnabled(false);
                downloadStateDialog.setNetworkStateEnabled(true);
                downloadStateDialog.setCancelable(false);
                break;
            case Quran.VerseDownloadStatus.STATUS_COMPLETE:
                if (downloadStateDialog.isShowing()) {
                    downloadStateDialog.dismiss();
                }
                break;
            case Quran.VerseDownloadStatus.STATUS_ERROR:
                if (!downloadStateDialog.isShowing()) {
                    downloadStateDialog.show();
                }

                downloadStateDialog.setContent(context.getString(R.string.download_failed));
                downloadStateDialog.setButtonText(context.getString(R.string.try_again));
                downloadStateDialog.setButtonEnabled(true);
                downloadStateDialog.setNetworkStateEnabled(false);
                downloadStateDialog.setCancelable(true);
                break;
            case Quran.VerseDownloadStatus.STATUS_START:
            case Quran.VerseDownloadStatus.STATUS_DOWNLODING:
                if (!downloadStateDialog.isShowing()) {
                    downloadStateDialog.show();
                }

//                if (mShouldShowProgress) {
                    downloadStateDialog.setContent(String.format(Locale.US, "%s\n%d%%", context.getString(R.string.downloading_audio), verseDownloadStatus.getDownloadStatus().getProgress()));
//                } else {
//                    mDownloadStateDialog.setContent(context.getString(R.string.downloading));
//                }
                downloadStateDialog.setButtonText(context.getString(R.string.cancel));
                downloadStateDialog.setButtonEnabled(true);
                downloadStateDialog.setNetworkStateEnabled(true);
                downloadStateDialog.setCancelable(false);
                break;
        }
    }
}

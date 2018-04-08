package co.muslimummah.android.module.quran.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;

import co.muslimummah.android.event.Quran;
import co.muslimummah.android.base.BaseActivity;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.module.quran.view.VerseDownloadDialogDelegate;
import co.muslimummah.android.util.filedownload.DownloadParam;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by frank on 8/26/17.
 */

public abstract class QuranAudioResourceDownloadingDialogActivity extends BaseActivity {
    //only for Verse download by Chapter
    protected VerseDownloadDialogDelegate downloadDialogDelegate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        downloadDialogDelegate = new VerseDownloadDialogDelegate(this);
        downloadDialogDelegate.setOnActionListener(new VerseDownloadDialogDelegate.OnActionListener() {
            @Override
            public void onCacelClick(DownloadParam param) {

            }

            @Override
            public void onRetryClick(DownloadParam param) {
                if (param.getTag() != null && param.getTag() instanceof Long) {
                    QuranRepository.INSTANCE
                            .getVerseWithoutAudioResource((Long) param.getTag(), 1)
                            .observeOn(Schedulers.io())
                            .subscribe(new Consumer<Verse>() {
                                @Override
                                public void accept(@NonNull Verse verse) throws Exception {
                                    onDownloadRetryClick(verse);
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(@NonNull Throwable throwable) throws Exception {

                                }
                            });
                }
            }
        });
    }

    protected abstract void onDownloadRetryClick(Verse verse);

    @Override
    protected void onResume() {
        super.onResume();
        downloadDialogDelegate.register();
        Quran.VerseDownloadStatus lastVerseDownloadStatus = QuranRepository.INSTANCE.getLastVerseDownloadStatus();
        if (lastVerseDownloadStatus != null) {
            downloadDialogDelegate.onDownloadEvent(lastVerseDownloadStatus);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        downloadDialogDelegate.unRegister();
    }
}

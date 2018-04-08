package co.muslimummah.android.event;

import java.io.Serializable;

import co.muslimummah.android.util.filedownload.DownloadParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Xingbo.Jie on 5/10/17.
 */

public abstract class Quran {
    /**
     * Created by Xingbo.Jie on 4/10/17.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadStatus implements Serializable, IDownloadStatus {
        private DownloadParam param;
        private int status;
        private int progress;
        private long id;
        private boolean canceled;
    }

    /**
     * Created by Xingbo.Jie on 5/10/17.
     */

    public static class StartDownloadVerse {
    }

    /**
     * Created by Xingbo.Jie on 4/10/17.
     */
    @Data
    @NoArgsConstructor
    public static class VerseDownloadStatus implements Serializable, IDownloadStatus {
        DownloadStatus downloadStatus;
    }
}

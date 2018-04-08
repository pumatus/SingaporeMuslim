package co.muslimummah.android.util.filedownload;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by Xingbo.Jie on 4/10/17.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(of = {"url", "dstFilePath"})
public class DownloadParam implements Serializable {
    public DownloadParam(@NonNull String url, @NonNull String dstFilePath, @Nullable Serializable tag) {
        this(url, dstFilePath, null, null, tag);
    }


    private String url;
    private String dstFilePath;
    private String title;
    private String description;
    private Serializable tag;
}

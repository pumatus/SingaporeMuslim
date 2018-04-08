package co.muslimummah.android.network.Entity.body;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

/**
 * Created by Xingbo.Jie on 28/8/17.
 */
@Data
@Builder
public class UploadLog implements Serializable {
    private String logPath;
}

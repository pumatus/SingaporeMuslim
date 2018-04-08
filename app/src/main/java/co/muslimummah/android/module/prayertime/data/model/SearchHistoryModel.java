package co.muslimummah.android.module.prayertime.data.model;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class SearchHistoryModel implements Serializable {
    private long time;
    private String content;
    private String subContent;
    private String placeId;
    private PrayerTimeLocationInfo info;
}

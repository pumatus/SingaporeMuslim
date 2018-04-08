package co.muslimummah.android.module.prayertime.data.model;

import android.text.TextUtils;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by Xingbo.Jie on 18/9/17.
 */
@Data
@AllArgsConstructor
@Builder
public class PrayerTimeLocationInfo implements Serializable {
    String locality;
    String identification;
    String country;
    String countryCode;
    double latitude;
    double longitude;
    double altitude;
    String timeZoneId;

    public String getDisplayName() {
        if (!TextUtils.isEmpty(locality)) {
            return locality;
        }

        return country;
    }
}

package co.muslimummah.android.module.prayertime.ui.fragment;

import java.util.Calendar;

import co.muslimummah.android.module.prayertime.utils.PrayerTimesAtUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by Xingbo.Jie on 31/8/17.
 */
@Data
@AllArgsConstructor
public class PrayerTimeMode {
    PrayerTimeType type;
    String time;
//    public long getTimeInMillisecond() {
//        //use current timezone for calculate
//        if (time != null) {
//            if
//            PrayerTimesAtUtils.setCalendarTime(Calendar.getInstance(), prayerTimes.get(i).split(":"), 0).getTimeInMillis()
//        }
//        return 0;
//    }
}

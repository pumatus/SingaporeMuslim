package co.muslimummah.android.module.prayertime.utils;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import org.joda.time.DateTimeZone;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.data.model.PrayerTimeLocationInfo;
import co.muslimummah.android.module.prayertime.manager.PrayerTimeManager;
import co.muslimummah.android.module.prayertime.receiver.PrayerTimesReceiver;
import co.muslimummah.android.network.Entity.response.TimeZoneJson;
import co.muslimummah.android.module.prayertime.ui.fragment.PrayerTimeMode;
import co.muslimummah.android.module.prayertime.ui.fragment.PrayerTimeType;
import co.muslimummah.android.storage.AppSession;
import co.muslimummah.android.util.UiUtils;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import timber.log.Timber;

import static co.muslimummah.android.util.UiUtils.getText;

/**
 * Created by Hongd on 2017/8/6.
 */

public class PrayerTimesAtUtils {

    private static int HOUR_IN_SECONDS = 3600;
    private static int MINUTE_IN_SECONDS = 60;
//    private static String[] ISNA = {"Canada", "United States"};
//    private static String[] Makkah = {"Yemen", "Oman", "Qatar", "Bahrain", "Kuwait", "Saudi Arabia", "United Arab Emirates", "Jordan", "Iraq"};
//    private static String[] Karachi = {"Pakistan", "Afghanistan", "Bangladesh", "India", "Sri Lanka"};
//    private static String[] Tehran = {"Iran"};
//    private static String[] Egypt = {"Singapore", "Malaysia", "Indonesia", "Brunei", "Egypt"};

    private static String[] ISNA_CODE = {"CA", "US"};
    private static String[] Makkah_CODE = {"YE", "OM", "QA", "BH", "KW", "SA", "AE", "JO", "IQ"};
    private static String[] Karachi_CODE = {"PK", "AF", "BD", "IN", "LK"};
    private static String[] Tehran_CODE = {"IR"};
    private static String[] Egypt_CODE = {"SG", "MY", "ID", "BN", "EG"};

    private static boolean useLoop(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equals(targetValue)) {
                return true;
            }
        }
        return false;
    }

    private static String calculateRule(String countryCode) {
        if (useLoop(ISNA_CODE, countryCode)) {
            return "ISNA";
        } else if (useLoop(Makkah_CODE, countryCode)) {
            return "Makkah";
        } else if (useLoop(Karachi_CODE, countryCode)) {
            return "Karachi";
        } else if (useLoop(Tehran_CODE, countryCode)) {
            return "Tehran";
        } else if (useLoop(Egypt_CODE, countryCode)) {
            return "Egypt";
        } else {
            return "MWL";
        }
    }

//    /**
//     * 指定的时间段(24小时制)
//     */
//    public static long triggerAtMillis(int hourOfDay, int minute, int index, String country, double lat, double lng) {
//        Calendar calMorning = Calendar.getInstance();
//        calMorning.setTimeInMillis(System.currentTimeMillis());
//        calMorning.set(Calendar.HOUR_OF_DAY, hourOfDay);
//        calMorning.set(Calendar.MINUTE, minute);
//        calMorning.set(Calendar.SECOND, 0);
//        calMorning.set(Calendar.MILLISECOND, 0);
//        TimeZone timeZone = TimeZone.getDefault();
//        double tZone = timeZone.getOffset(calMorning.getTimeInMillis()) / 3600000;
//        // 如果当前时间大于设置的时间，那么就从第二天的设定时间开始
//        if (System.currentTimeMillis() > calMorning.getTimeInMillis()) {
//            String[] time = calcPrayerTimeGetZero(index, 1, country, lat, lng, tZone).split(":");
//            calMorning.add(Calendar.DAY_OF_MONTH, 1);
//            calMorning.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time[0]));
//            calMorning.set(Calendar.MINUTE, Integer.valueOf(time[1]));
//        }
//        return calMorning.getTimeInMillis();
//    }

    private static int calcMethodType(String countryCode) {
        PrayTime prayTime = new PrayTime();
        if ("ISNA".equals(calculateRule(countryCode))) {
            Log.i("calcMethodType  ", "ISNA");
            return prayTime.ISNA;
        } else if ("Makkah".equals(calculateRule(countryCode))) {
            Log.i("calcMethodType  ", "Makkah");
            return prayTime.Makkah;
        } else if ("Karachi".equals(calculateRule(countryCode))) {
            Log.i("calcMethodType  ", "Karachi");
            return prayTime.Karachi;
        } else if ("Tehran".equals(calculateRule(countryCode))) {
            Log.i("calcMethodType  ", "Tehran");
            return prayTime.Tehran;
        } else if ("Egypt".equals(calculateRule(countryCode))) {
            Log.i("calcMethodType  ", "Egypt");
            return prayTime.Egypt;
        } else {
            Log.i("calcMethodType  ", "MWL");
            return prayTime.MWL;
        }
    }

//    /**
//     * 计算下一天的PrayerTime 第一次时间
//     */
//    public static String calcPrayerTimeGetZero(int index, int amount, String countryCode, double lat, double lng,
//                                               double tZone) {
//        PrayTime prayTime = new PrayTime();
//        prayTime.setCalcMethod(calcMethodType(country));
//        prayTime.setTimeFormat(prayTime.Time24);
//        prayTime.setAdjustHighLats(prayTime.AngleBased);
//        Calendar calendarTomorrow = Calendar.getInstance();
//        calendarTomorrow.add(Calendar.DATE, amount);
//        ArrayList<String> prayerTimes = prayTime
//                .getPrayerTimes(calendarTomorrow, lat, lng, tZone);
//        prayerTimes.remove(4);
//        return prayerTimes.get(index);
//    }

    /**
     * 计算今天的PrayerTime
     */
    public static ArrayList<String> calcPrayerTimeList(Calendar calendar, String countryCode, double lat, double lng,
                                                       double tZone) {
        Timber.d("calculatePrayerTimes country=%s lat=%f lng=%f", countryCode, lat, lng);
        PrayTime prayTime = new PrayTime();
        prayTime.setCalcMethod(calcMethodType(countryCode));
        prayTime.setTimeFormat(prayTime.Time24);
        prayTime.setAdjustHighLats(prayTime.AngleBased);
        ArrayList<String> prayerTimes = prayTime.getPrayerTimes(calendar, lat, lng, tZone);
        prayerTimes.remove(4);
        return prayerTimes;
    }

//    public static boolean compareTime(Calendar toDay, Calendar calendar, int day) {
//        if (toDay.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
//            && toDay.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
//            && toDay.get(Calendar.DAY_OF_MONTH) == calendar
//            .get(Calendar.DAY_OF_MONTH) + day) {
//            return true;
//        } else {
//            return false;
//        }
//        return false;
//    }

    /**
     * @param calendar
     * @param timeStr  12:12
     * @return
     */
    public static long getTimeInMillis(Calendar calendar, String timeStr) {
        if (timeStr == null || calendar == null) {
            return 0l;
        }

        String[] nextTimeStr = timeStr.split(":");
        if (nextTimeStr.length < 2) {
            return 0l;
        }

        if (!TextUtils.isDigitsOnly(nextTimeStr[0]) || !TextUtils.isDigitsOnly(nextTimeStr[1])) {
            return 0l;
        }

        Calendar result = Calendar.getInstance();
        result.set(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                Integer.valueOf(nextTimeStr[0]),
                Integer.valueOf(nextTimeStr[1]),
                0);
        return result.getTimeInMillis();
    }

    public static String timeInMillisFormat(long millis) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        return simpleDateFormat.format(millis);
    }

    public static String countDownFormat(long mills) {
        long seconds = mills / 1000;
        long hours = 0, minutes = 0;
        if (seconds > HOUR_IN_SECONDS) {
            hours = seconds / HOUR_IN_SECONDS;
        }

        if (seconds > MINUTE_IN_SECONDS) {
            long temp = seconds % HOUR_IN_SECONDS;
            minutes = temp / MINUTE_IN_SECONDS;
            seconds = temp % MINUTE_IN_SECONDS;
        }

        StringBuilder builder = new StringBuilder();
        if (hours >= 10) {
            builder.append(hours);
        } else if (hours > 0) {
            builder.append("0");
            builder.append(hours);
        } else {
            builder.append("00");
        }

        builder.append(":");

        if (minutes >= 10) {
            builder.append(minutes);
        } else if (seconds > 0) {
            builder.append("0");
            builder.append(minutes);
        } else {
            builder.append("00");
        }

        builder.append(":");

        if (seconds >= 10) {
            builder.append(seconds);
        } else if (seconds > 0) {
            builder.append("0");
            builder.append(seconds);
        } else {
            builder.append("00");
        }

        return builder.toString();
    }


    public static boolean isSameDay(Calendar calendar1, Calendar calendar2) {
        if (calendar1 == null || calendar2 == null) {
            return false;
        }

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isTomorrow(Calendar calendar) {
        Calendar today = Calendar.getInstance();
        today.add(Calendar.DAY_OF_YEAR, 1);
        return isSameDay(today, calendar);
    }

    public static String buildMonthAtLocal(int monthOfYear) {
        String ar = "";
        switch (monthOfYear) {
            case 0:
                ar = getText(R.string.january);
                break;
            case 1:
                ar = getText(R.string.february);
                break;
            case 2:
                ar = getText(R.string.march);
                break;
            case 3:
                ar = getText(R.string.april);
                break;
            case 4:
                ar = getText(R.string.may);
                break;
            case 5:
                ar = getText(R.string.june);
                break;
            case 6:
                ar = getText(R.string.july);
                break;
            case 7:
                ar = getText(R.string.august);
                break;
            case 8:
                ar = getText(R.string.september);
                break;
            case 9:
                ar = getText(R.string.october);
                break;
            case 10:
                ar = getText(R.string.november);
                break;
            case 11:
                ar = getText(R.string.december);
                break;
            default:
                break;
        }
        return ar;
    }

    public static String buildMonthAtAr(int monthOfYear) {
        String ar = "";
        switch (monthOfYear) {
            case 0:
                ar = UiUtils.getText(R.string.muharram);
                break;
            case 1:
                ar = UiUtils.getText(R.string.safar);
                break;
            case 2:
                ar = UiUtils.getText(R.string.rabi_al_awwal);
                break;
            case 3:
                ar = UiUtils.getText(R.string.rabi_al_akhar);
                break;
            case 4:
                ar = UiUtils.getText(R.string.jumada_al_awwal);
                break;
            case 5:
                ar = UiUtils.getText(R.string.jumada_al_akhirah);
                break;
            case 6:
                ar = UiUtils.getText(R.string.rajab);
                break;
            case 7:
                ar = UiUtils.getText(R.string.shaban);
                break;
            case 8:
                ar = UiUtils.getText(R.string.ramadan);
                break;
            case 9:
                ar = UiUtils.getText(R.string.shawwal);
                break;
            case 10:
                ar = UiUtils.getText(R.string.dhul_qadah);
                break;
            case 11:
                ar = UiUtils.getText(R.string.dhul_hijjah);
                break;
        }
        return ar;
    }

    public static int getAlarmStatus(Context context, PrayerTimeType type) {
        Integer integer = AppSession.getInstance(context)
                .getCachedValue(Constants.KEY_OF_NOTIFICATION_STATUS_PREFIX + type.getNameText(), Integer.class);

        if (integer == null) {
            return type == PrayerTimeType.SUNRISE ? Constants.NOTIFICATION_STATUS_OFF
                    : Constants.NOTIFICATION_STATUS_MUTE;
        }
        return integer.intValue();
    }

    public static void setAlarmStatus(Context context, PrayerTimeType type, int status) {
        AppSession.getInstance(context)
                .cacheValue(Constants.KEY_OF_NOTIFICATION_STATUS_PREFIX + type.getNameText(), status, true);
    }

    public static ArrayList<String> calculatePrayerTimes(Calendar calendar, PrayerTimeLocationInfo locationInfo) {
        DateTimeZone timeZone;
        String countryCode = locationInfo.getCountryCode();
        double lat = locationInfo.getLatitude();
        double lng = locationInfo.getLongitude();
        String timeZoneId = locationInfo.getTimeZoneId();
        if (timeZoneId != null) {
            timeZone = DateTimeZone.forID(timeZoneId);
        } else {
            timeZone = DateTimeZone.getDefault();
        }

        final double tZone = timeZone.getOffset(calendar.getTimeInMillis()) / 3600000.0d;
        Timber.d("calculatePrayerTimes %s in %s zone is %s", calendar.getTime().toString(), timeZoneId, "" + tZone);
        return PrayerTimesAtUtils.calcPrayerTimeList(calendar, countryCode, lat, lng, tZone);
    }

    public static long getlastPrayerTime(Calendar calendar, PrayerTimeLocationInfo locationInfo) {
        ArrayList<String> prayerTimes = calculatePrayerTimes(calendar, locationInfo);
        return getTimeInMillis(calendar, prayerTimes.get(prayerTimes.size() - 1));
    }


    public static PrayerTimeMode getFirstPrayerTime(Calendar calendar, PrayerTimeLocationInfo locationInfo) {
        ArrayList<String> prayerTimes = calculatePrayerTimes(calendar, locationInfo);
        return new PrayerTimeMode(PrayerTimeType.FAJR, prayerTimes.get(0));
    }

    public static ArrayList<PrayerTimeMode> getDefalutPrayerTimeModes() {
        ArrayList<PrayerTimeMode> prayerTimeModes = new ArrayList<>(6);
        prayerTimeModes.add(new PrayerTimeMode(PrayerTimeType.FAJR, "--:--"));
        prayerTimeModes.add(new PrayerTimeMode(PrayerTimeType.SUNRISE, "--:--"));
        prayerTimeModes.add(new PrayerTimeMode(PrayerTimeType.DHUHR, "--:--"));
        prayerTimeModes.add(new PrayerTimeMode(PrayerTimeType.ASR, "--:--"));
        prayerTimeModes.add(new PrayerTimeMode(PrayerTimeType.MAGHRIB, "--:--"));
        prayerTimeModes.add(new PrayerTimeMode(PrayerTimeType.ISHA, "--:--"));

        return prayerTimeModes;
    }

    public static void placeNextAlarm(Context context, PrayerTimeLocationInfo locationInfo) {
        Calendar calendar = Calendar.getInstance();
        ArrayList<PrayerTimeMode> prayerTimeModes = PrayerTimesAtUtils.getDefalutPrayerTimeModes();
        ArrayList<String> prayerTimes = PrayerTimesAtUtils.calculatePrayerTimes(calendar, locationInfo);
        if (prayerTimes != null && prayerTimes.size() == 6) {
            for (int i = 0; i < prayerTimes.size(); ++i) {
                prayerTimeModes.get(i).setTime(prayerTimes.get(i));
//                prayerTimeModes.get(i).setTimeInMillisecond(
//                        PrayerTimesAtUtils.setCalendarTime(calendar, prayerTimes.get(i).split(":"), 0).getTimeInMillis());
            }
        }

        placeNextAlarm(context, locationInfo, prayerTimeModes);
    }

    public static void placeNextAlarm(Context context, PrayerTimeLocationInfo locationInfo, ArrayList<PrayerTimeMode> prayerTimeModes) {
        Calendar calendar = Calendar.getInstance();
        PrayerTimeMode mode;
        if (System.currentTimeMillis() > getTimeInMillis(calendar, prayerTimeModes.get(prayerTimeModes.size() - 1).getTime())) {
            //need set the next day alarm
            Calendar tommrow = Calendar.getInstance();
            tommrow.add(Calendar.DAY_OF_YEAR, 1);
            mode = PrayerTimesAtUtils.getFirstPrayerTime(tommrow, locationInfo);
            PrayerTimesReceiver.setAlarm(context, mode.getType(), mode.getTime(), getTimeInMillis(tommrow, mode.getTime()));
        } else {
            // today next alarm
            for (int index = 0; index < prayerTimeModes.size(); index++) {
                mode = prayerTimeModes.get(index);
                if (getTimeInMillis(calendar, mode.getTime()) > System.currentTimeMillis()) {
                    PrayerTimesReceiver.setAlarm(context, mode.getType(), mode.getTime(), getTimeInMillis(calendar, mode.getTime()));
                    break;
                }
            }
        }
    }

    public static String getNameOfImportantDay(Calendar calendar) {
        String result = null;
        if (calendar != null) {
            int impMonth = calendar.get(Calendar.MONTH);
            int impDay = calendar.get(Calendar.DAY_OF_MONTH);
            if (impMonth == 0 && impDay == 1) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_MU1);
            } else if (impMonth == 0 && impDay == 10) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_MU10);
            } else if (impMonth == 6 && impDay == 27) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_RAJ27);
            } else if (impMonth == 7 && impDay == 15) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_SY15);
            } else if (impMonth == 8 && impDay == 1) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_RA1);
            } else if (impMonth == 8 && impDay == 27) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_RAM27);
            } else if (impMonth == 9 && impDay == 1) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_SH1);
            } else if (impMonth == 11 && impDay == 8) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_DH8);
            } else if (impMonth == 11 && impDay == 9) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_DH9);
            } else if (impMonth == 11 && impDay == 10) {
                result = OracleApp.getInstance().getResources().getString(R.string.IMPORTANT_DATE_DESCRIPTION_DH10);
            }
        }

        return result;
    }

    public static String getPrayerTimeName(PrayerTimeType type) {
        switch (type) {
            case FAJR:
                return UiUtils.getText(R.string.fajr);
            case SUNRISE:
                return UiUtils.getText(R.string.sunrise);
            case DHUHR:
                return UiUtils.getText(R.string.dhuhr);
            case ASR:
                return UiUtils.getText(R.string.asr);
            case MAGHRIB:
                return UiUtils.getText(R.string.maghrib);
            case ISHA:
                return UiUtils.getText(R.string.isha);
        }
        return UiUtils.getText(R.string.fajr);
    }

    public static ObservableTransformer<Location, PrayerTimeLocationInfo> location2PrayerTimeLocationInfoTransformer() {
        return new ObservableTransformer<Location, PrayerTimeLocationInfo>() {
            @Override
            public ObservableSource<PrayerTimeLocationInfo> apply(@NonNull Observable<Location> upstream) {
                return upstream
                        .flatMap(new Function<Location, ObservableSource<PrayerTimeLocationInfo>>() {
                            @Override
                            public ObservableSource<PrayerTimeLocationInfo> apply(@io.reactivex.annotations.NonNull Location location) throws Exception {
                                return PrayerTimeManager.instance().getAddressInfoByLocation(location);
                            }
                        });
//                        .compose(updateTimeZoneIdTransformer());
            }
        };
    }

    public static ObservableTransformer<PrayerTimeLocationInfo, PrayerTimeLocationInfo> updateTimeZoneIdBySystemTransformer() {
        return new ObservableTransformer<PrayerTimeLocationInfo, PrayerTimeLocationInfo>() {
            @Override
            public ObservableSource<PrayerTimeLocationInfo> apply(@NonNull Observable<PrayerTimeLocationInfo> upstream) {
                return upstream
                        .flatMap(new Function<PrayerTimeLocationInfo, ObservableSource<PrayerTimeLocationInfo>>() {
                            @Override
                            public ObservableSource<PrayerTimeLocationInfo> apply(@io.reactivex.annotations.NonNull PrayerTimeLocationInfo prayerTimeLocationInfo) throws Exception {
                                return Observable
                                        .zip(Observable.just(prayerTimeLocationInfo), Observable.just(TimeZone.getDefault()),
                                                new BiFunction<PrayerTimeLocationInfo, TimeZone, PrayerTimeLocationInfo>() {

                                                    @Override
                                                    public PrayerTimeLocationInfo apply(@io.reactivex.annotations.NonNull PrayerTimeLocationInfo prayerTimeLocationInfo, @io.reactivex.annotations.NonNull TimeZone timeZone) throws Exception {
                                                        prayerTimeLocationInfo.setTimeZoneId(timeZone.getID());
                                                        return prayerTimeLocationInfo;
                                                    }
                                                });
                            }
                        });
            }
        };
    }


    public static ObservableTransformer<PrayerTimeLocationInfo, PrayerTimeLocationInfo> updateTimeZoneIdByGoogleApiTransformer() {
        return new ObservableTransformer<PrayerTimeLocationInfo, PrayerTimeLocationInfo>() {
            @Override
            public ObservableSource<PrayerTimeLocationInfo> apply(@NonNull Observable<PrayerTimeLocationInfo> upstream) {
                return upstream
                        .flatMap(new Function<PrayerTimeLocationInfo, ObservableSource<PrayerTimeLocationInfo>>() {
                            @Override
                            public ObservableSource<PrayerTimeLocationInfo> apply(@io.reactivex.annotations.NonNull PrayerTimeLocationInfo prayerTimeLocationInfo) throws Exception {
                                return Observable
                                        .zip(Observable.just(prayerTimeLocationInfo),
                                                PrayerTimeManager.instance().getTimezone(OracleApp.getInstance(),
                                                        prayerTimeLocationInfo.getLatitude() + "," + prayerTimeLocationInfo.getLongitude(),
                                                        Calendar.getInstance().getTimeInMillis() / 1000),
                                                new BiFunction<PrayerTimeLocationInfo, TimeZoneJson, PrayerTimeLocationInfo>() {

                                                    @Override
                                                    public PrayerTimeLocationInfo apply(@io.reactivex.annotations.NonNull PrayerTimeLocationInfo prayerTimeLocationInfo, @io.reactivex.annotations.NonNull TimeZoneJson timeZoneJson) throws Exception {
                                                        prayerTimeLocationInfo.setTimeZoneId(timeZoneJson.getTimeZoneId());
                                                        return prayerTimeLocationInfo;
                                                    }
                                                });
                            }
                        });
            }
        };
    }
}

package co.muslimummah.android.module.prayertime.ui.fragment;

/**
 * Created by Xingbo.Jie on 31/8/17.
 */

public enum PrayerTimeType {

    FAJR("Fajr"),
    SUNRISE("Sunrise"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha");

    PrayerTimeType(String name) {
        this.nameText = name;
    }
    private String nameText;

    public String getNameText() {
        return nameText;
    }

    public static PrayerTimeType format(String nameText) {
        for (PrayerTimeType type : values()) {
            if (type.getNameText().equals(nameText)) {
                return type;
            }
        }

        return FAJR;
    }
}

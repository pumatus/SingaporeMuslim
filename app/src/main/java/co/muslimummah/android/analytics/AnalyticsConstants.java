package co.muslimummah.android.analytics;

/**
 * Created by Xingbo.Jie on 28/8/17.
 */

public interface AnalyticsConstants {
    enum BEHAVIOUR {
        NONE("B1"),
        ENTER("B2"),
        LEAVE("B3"),
        CLICK("B4"),
        SWIPE("B5"),
        PLAY("B6"),
        SHARE("B7"),
        LIKE("B8A"),
        UNLIKE("B8B"),
        BOOKMARK("B9A"),
        UNBOOKMARK("B9B");
        public String value;

        BEHAVIOUR(String value) {
            this.value = value;
        }
    }

    enum TARGET_TYPE {
        IMAGE_ID("1"),
        VERSE_ID("2"),
        CHAPTER_ID("3"),
        PRAYER_TIME_TYPE("6"),
        NOTIFICATION_SETTINGS("7"),
        SCHEME("8"),
        COMPASS_ISSUE("9"),
        ;
        public String value;

        TARGET_TYPE(String value) {
            this.value = value;
        }
    }

    enum LOCATION {
        HOME_PAGE("L1"),
        PRAYER_TIME_PAGE("L2"),
        QURAN_CHAPTER_VIEW_PAGE("L3"),
        BOOKMARK_PAGE("L4"),
        BOOKMARK_PAGE_PLAY_ICON("L4A"),
        BOOKMARK_PAGE_PLAY_PANEL_NEXT("L4B"),
        BOOKMARK_PAGE_PLAY_PANEL_PREVIOUS("L4C"),
        BOOKMARK_PAGE_PLAY_PANEL_PLAY("L4D"),
        BOOKMARK_PAGE_PLAY_PANEL_PAUSE("L4E"),
        BOOKMARK_PAGE_PLAY_PANEL_STOP("L4F"),
        BOOKMARK_PAGE_PLAY_PANEL_PLAY_ALL("L4G"),

        QIBLA_PAGE("L5"),

        QURAN_VERSE_VIEW_PAGE("L6"),
        QURAN_VERSE_VIEW_PAGE_PLAY_ICON("L6A"),
        QURAN_VERSE_VIEW_PAGE_PANEL_NEXT("L6B"),
        QURAN_VERSE_VIEW_PAGE_PANEL_PREVIOUS("L6C"),
        QURAN_VERSE_VIEW_PAGE_PANEL_PLAY("L6D"),
        QURAN_VERSE_VIEW_PAGE_PANEL_PAUSE("L6E"),
        QURAN_VERSE_VIEW_PAGE_PANEL_STOP("L6F"),
        QURAN_VERSE_VIEW_PAGE_PANEL_PLAY_ALL("L6G"),
        QURAN_VERSE_VIEW_PAGE_DROPDOWN_LIST("L6H"),

        NOTIFICATION_BAR_PLAYER("L7"),
        NOTIFICATION_BAR_NEXT("L7A"),
        NOTIFICATION_BAR_PREVIOUS("L7B"),
        NOTIFICATION_BAR_PLAY("L7C"),
        NOTIFICATION_BAR_PAUSE("L7D"),
        NOTIFICATION_BAR_STOP("L7E"),
        NOTIFICATION_BAR_OTHERAREA("L7F"),

        NOTIFICATION_PAGE_PRAYERTIMES("L8"),

        SETTING_ICON_FAJR("L9A"),
        SETTING_ICON_SUNRISE("L9B"),
        SETTING_ICON_DHUHR("L9C"),
        SETTING_ICON_ASR("L9D"),
        SETTING_ICON_MAGHRIB("L9E"),
        SETTING_ICON_ISHA("L9F"),

        NOTIFICATION_PAGE_SCHEME("L10"),
        ;
        public String value;

        LOCATION(String value) {
            this.value = value;
        }
    }

    enum TARGET_VAULE {
        FAJR("Fajr"),
        SUNRISE("Sunrise"),
        DHUHR("Dhuhr"),
        ASR("Asr"),
        MAGHRIB("Maghrib"),
        ISHA("Isha"),

        OFF("Off"),
        MUTE("Mute"),
        SOUND("Sound"),

        NO_SENSOR("NoSensor"),
        NO_CALLBACK("NoCallback"),
        CALLBACK_FAILURE("CallbackFailure"),
        ;
        public String value;

        TARGET_VAULE(String value) {
            this.value = value;
        }
    }
}

package co.muslimummah.android.analytics;

/**
 * Created by Xingbo.Jie on 23/9/17.
 */

public interface GA {
    enum Category {
        PrayerTimeNotification("PrayerTimeNotification"),
        Calendar("Calendar"),
        PrayerTimeLocation("PrayerTimeLocation"),
        LaunchApp("LaunchApp"),
        //        PrayerTimes("PrayerTimes"),
        PrayerTimeSearchLocatoin("PrayerTimeSearchLocatoin"),
        QuranVerseView("QuranVerseView"),
        QuranBookmarkView("QuranBookmarkView");

        private String value;

        Category(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    enum Action {
        //        Off("Off"),
//        Mute("Mute"),
//        Sound("Sound"),
        ChangeNotification("ChangeNotification"),
        Swipe("Swipe"),
        SelectDate("SelectDate"),
        ClickCity("ClickCity"),
        FindLocation("FindLocation"),
        ShowPage("ShowPage"),
        Click("Click"),
        LocationServicesPopup("LocationServicesPopup"),
        SelectLocationPopup("SelectLocationPopup"),
        NewLocationPopup("NewLocationPopup"),
        NoResultPopup("NoResultPopup"),

        LocateMe("LocateMe"),

        Return("Return"),
        DeleteInput("DeleteInput"),
        DeleteOneHistory("DeleteOneHistory"),
        ClearAllHistory("ClearAllHistory"),
        SelectHistory("SelectHistory"),
        SelectSearchResult("SelectSearchResult"),
        SelectHint("SelectHint"),
        Reading("Reading"),
        AudioPlaying("AudioPlaying"),
        WordByWord("WordByWord"),
        TapWord("TapWord");
        private String value;

        Action(String value) {
            this.value = value;
        }


        public String getValue() {
            return value;
        }
    }

    enum Label {
        Fajr("Fajr"),
        Sunrise("Sunrise"),
        Dhuhr("Dhuhr"),
        Asr("Asr"),
        Maghrib("Maghrib"),
        Isha("Isha"),

        NextWeek("NextWeek"),
        PreviousWeek("PreviousWeek"),
        WeekToMonth("WeekToMonth"),
        MonthToWeek("MonthToWeek"),
        NextMonth("NextMonth"),
        PreviousMonth("PreviousMonth"),

        Success("Success"),
        Failure("Failure"),
        Timeout("Timeout"),
        Yes("Yes"),
        No("No"),

        HomepageNormal("HomepageNormal"),
        HomepageNoInternet("HomepageNoInternet"),
        LocationServices("LocationServices"),
        SelectLocation("SelectLocation"),
        NewLocation("NewLocation"),

        NextDay("NextDay"),
        PreviousDay("PreviousDay"),

        OK("OK"),
        Cancel("Cancel"),

        LocateMe("LocateMe"),
        SelectManually("SelectManually"),
        Close("Close"),
        TapWord("TapWord"),
        PlayWord("PlayWord"),
        PreviousArabicWord("PreviousArabicWord"),
        NextArabicWord("NextArabicWord");
        private String value;

        Label(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}

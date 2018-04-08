package co.muslimummah.android.module.prayertime.data.view;

import co.muslimummah.android.module.prayertime.data.model.SearchHistoryModel;
import java.util.ArrayList;

public interface SearchView {

    void showHistories(ArrayList<SearchHistoryModel> results);

    void searchSuccess(String value);
}

package co.muslimummah.android.module.prayertime.data.presenter;

import co.muslimummah.android.module.prayertime.data.model.SearchHistoryModel;

public interface SearchPresenter {

    void remove(SearchHistoryModel history);

    void clear();

    void loadHistory();

    void save(SearchHistoryModel history);
}

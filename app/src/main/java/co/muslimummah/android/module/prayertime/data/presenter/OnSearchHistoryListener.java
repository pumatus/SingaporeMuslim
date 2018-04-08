package co.muslimummah.android.module.prayertime.data.presenter;

import co.muslimummah.android.module.prayertime.data.model.SearchHistoryModel;

public interface OnSearchHistoryListener {

    void onDelete(SearchHistoryModel content);

    void onSelect(SearchHistoryModel content);
}

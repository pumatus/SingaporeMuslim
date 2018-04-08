package co.muslimummah.android.module.prayertime.data.presenter;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.data.model.SearchHistoryModel;
import co.muslimummah.android.module.prayertime.data.view.SearchView;
import co.muslimummah.android.storage.AppSession;

import static android.R.attr.key;

public class SearchPresenterImpl implements SearchPresenter {

    private static final int historyMax = 5;
    private SearchView searchView;
    //    private SearchModel searchModel;
    private AppSession appSession;
    private HashMap<String, SearchHistoryModel> histories;

    public SearchPresenterImpl(SearchView searchView, Context context) {
        this.searchView = searchView;
        appSession = AppSession.getInstance(context);
        histories = new HashMap<>();
//        this.searchModel = new SearchModelImpl(context, historyMax);
    }

    //移除历史记录
    @Override
    public void remove(SearchHistoryModel history) {
//        searchModel.remove(key);
//        searchModel.loadHistory(this);
        histories.remove(history.getPlaceId());
        appSession.cacheValue(Constants.KEY_LOCATION_SEARCH_HISTORY, histories, true);
        showHistories();
    }

    @Override
    public void clear() {
//        searchModel.clear();
//        searchModel.loadHistory(this);
        histories.clear();
        appSession.clearCacheValue(Constants.KEY_LOCATION_SEARCH_HISTORY);
        showHistories();
    }

    //获取所有的历史记录
    @Override
    public void loadHistory() {
//        searchModel.loadHistory(this);
        HashMap cachedValue = appSession.getCachedValue(Constants.KEY_LOCATION_SEARCH_HISTORY, HashMap.class);
        if (cachedValue != null) {
            histories.putAll(cachedValue);
        }
        showHistories();
    }

    private void showHistories() {
        ArrayList<SearchHistoryModel> list = sortHistoryModels();
        searchView.showHistories(list);
    }

    @Override
    public void save(SearchHistoryModel historyModel) {
        historyModel.setTime(System.currentTimeMillis());
        histories.put(historyModel.getPlaceId(), historyModel);

        ArrayList<SearchHistoryModel> list = sortHistoryModels();

        if (list.size() > historyMax) {
            for (int i = historyMax; i < list.size(); ++i) {
                histories.remove(list.get(i).getContent());
            }
        }

        appSession.cacheValue(Constants.KEY_LOCATION_SEARCH_HISTORY, histories, true);
        showHistories();
    }

    @NonNull
    private ArrayList<SearchHistoryModel> sortHistoryModels() {
        ArrayList<SearchHistoryModel> list = new ArrayList<>(histories.values());
        Collections.sort(list, new Comparator<SearchHistoryModel>() {
            @Override
            public int compare(SearchHistoryModel t1, SearchHistoryModel t2) {
                if (t1.getTime() > t2.getTime()) {
                    return -1;
                } else if (t1.getTime() < t2.getTime()) {
                    return 1;
                }
                return 0;
            }
        });
        return list;
    }


//    @Override
//    public void onSortSuccess(ArrayList<SearchHistoryModel> results) {
//        searchView.showHistories(results);
//    }
//
//    @Override
//    public void searchSuccess(String value) {
//        searchView.searchSuccess(value);
//    }
}

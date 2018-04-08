package co.muslimummah.android.module.prayertime.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataBufferUtils;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompleteFilter.Builder;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.GA;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.BaseActivity;
import co.muslimummah.android.base.lifecycle.ScreenEvent;
import co.muslimummah.android.module.prayertime.adapter.SearchHistoryAdapter;
import co.muslimummah.android.module.prayertime.data.Constants;
import co.muslimummah.android.module.prayertime.data.model.PrayerTimeLocationInfo;
import co.muslimummah.android.module.prayertime.data.model.SearchHistoryModel;
import co.muslimummah.android.module.prayertime.data.presenter.OnSearchHistoryListener;
import co.muslimummah.android.module.prayertime.data.presenter.SearchPresenter;
import co.muslimummah.android.module.prayertime.data.presenter.SearchPresenterImpl;
import co.muslimummah.android.module.prayertime.data.view.SearchView;
import co.muslimummah.android.module.prayertime.manager.PrayerTimeManager;
import co.muslimummah.android.module.prayertime.ui.view.CleanEditText;
import co.muslimummah.android.module.prayertime.ui.view.CleanEditText.CleanEditTextListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static co.muslimummah.android.R.id.listView_history;

public class SearchActivity extends BaseActivity implements SearchView,
        OnConnectionFailedListener, CleanEditTextListener {

    private final static int STATUS_HISTORY = 1;
    private final static int STATUS_NO_RESULT = 2;
    private final static int STATUS_HINT_RESULTS = 3;
    private final static int STATUS_NET_ERROR = 4;
    private final static int STATUS_LOADING = 5;
    private final static int STATUS_NO_NETWORK = 6;

    private static final String TAG = SearchActivity.class.getSimpleName();
    //Global value
    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-90, -180), new LatLng(90, 180));

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.et_search)
    CleanEditText etSearch;
    @BindView(R.id.btn_search_check)
    TextView btnSearchCheck;
    @BindView(R.id.iv_no_internet)
    ImageView ivNoInternet;
    @BindView(R.id.tv_no_internet)
    TextView tvNoInternet;
    @BindView(R.id.ll_check_title)
    LinearLayout llCheckTitle;
    @BindView(listView_history)
    ListView listViewHistory;
    @BindView(R.id.ll_clear_history)
    LinearLayout llClearHistory;
    @BindView(R.id.ll_search_history)
    LinearLayout llSearchHistory;
    @BindView(R.id.tv_history)
    TextView tvHistory;
    @BindView(R.id.loading_layout)
    FrameLayout loadingLayout;

    private SearchPresenter mSearchPresenter;
    private SearchHistoryAdapter searchHistoryAdapter;
    private ArrayList<SearchHistoryModel> histories = new ArrayList<>();
    private GoogleApiClient googleApiClient;
    private PlaceAutocompleteAdapter autocompleteAdapter;
    private String getPrimaryText = "";
    //    private boolean flag = true;
    private int status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0 /* clientId */, this)
                .addApi(Places.GEO_DATA_API)
                .build();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        mSearchPresenter = new SearchPresenterImpl(this, this);

        //set a filter returning only results with a precise address.
        AutocompleteFilter filter = new Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                .build();
        etSearch.setOnItemClickListener(new AutocompleteClickListener());
        autocompleteAdapter = new PlaceAutocompleteAdapter(this, googleApiClient,
                BOUNDS_GREATER_SYDNEY,
                filter);
        etSearch.setAdapter(autocompleteAdapter);
        searchHistoryAdapter = new SearchHistoryAdapter(this, histories);
        searchHistoryAdapter.setOnSearchHistoryListener(new OnSearchHistoryListener() {
            @Override
            public void onDelete(SearchHistoryModel content) {
                mSearchPresenter.remove(content);

                ThirdPartyAnalytics.INSTANCE
                        .logEvent(GA.Category.PrayerTimeSearchLocatoin, GA.Action.DeleteOneHistory);
            }

            @Override
            public void onSelect(SearchHistoryModel content) {
                saveHistory(content);

                ThirdPartyAnalytics.INSTANCE
                        .logEvent(GA.Category.PrayerTimeSearchLocatoin,
                                GA.Action.SelectHistory,
                                content.getContent()
                        );

                if (content.getInfo() != null) {
                    Intent intent = new Intent();
//                            locationInfo.setSubLocality(null);
                    intent.putExtra(Constants.RESULT_SEARCH_ACTIVITY, content.getInfo());
                    setResult(RESULT_OK, intent);
                    finish();
                    return;
                } else {
                    etSearch.setText(content.getContent());
                    etSearch.setSelection(content.getContent().length());

                    updateUI(STATUS_LOADING);
                    fetchLocation(content);
                }
            }
        });
        listViewHistory.setAdapter(searchHistoryAdapter);
        etSearch.addTextChangedListener(textWatcher);
        mSearchPresenter.loadHistory();
        if (searchHistoryAdapter.getCount() > 0) {
            tvHistory.setVisibility(View.VISIBLE);
            ((InputMethodManager) etSearch.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(etSearch.getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
        } else {
            tvHistory.setVisibility(View.GONE);
        }
        etSearch.setOnHideHeaderChangedListener(this);

        updateUI(STATUS_HISTORY);
    }

    private void updateUI(int status) {
        if (this.status == status) {
            return;
        }

        if (TextUtils.isEmpty(etSearch.getText())) {
            status = STATUS_HISTORY;
        }

        this.status = status;
        Timber.d("updateUI status=%d", status);

        switch (status) {
            case STATUS_HISTORY:
                loadingLayout.setVisibility(View.GONE);
                llSearchHistory.setVisibility(View.VISIBLE);
                llClearHistory.setVisibility(searchHistoryAdapter.getCount() > 0 ? View.VISIBLE : View.GONE);
                llCheckTitle.setVisibility(View.GONE);
                break;
            case STATUS_LOADING:
                loadingLayout.setVisibility(View.VISIBLE);
                llSearchHistory.setVisibility(View.GONE);
                llCheckTitle.setVisibility(View.GONE);
                break;
            case STATUS_NET_ERROR:
                loadingLayout.setVisibility(View.GONE);
                llSearchHistory.setVisibility(View.GONE);
                llCheckTitle.setVisibility(View.VISIBLE);
                ivNoInternet.setImageResource(R.drawable.vector_drawable_icon_notfound);
                tvNoInternet.setText(R.string.search_failed);
                break;
            case STATUS_NO_NETWORK:
                loadingLayout.setVisibility(View.GONE);
                llSearchHistory.setVisibility(View.GONE);
                llCheckTitle.setVisibility(View.VISIBLE);
                ivNoInternet.setImageResource(R.drawable.vector_drawable_icon_internet);
                tvNoInternet.setText(R.string.no_internet_connection);
                break;
            case STATUS_NO_RESULT:
                loadingLayout.setVisibility(View.GONE);
                llSearchHistory.setVisibility(View.GONE);
                llCheckTitle.setVisibility(View.VISIBLE);
                ivNoInternet.setImageResource(R.drawable.vector_drawable_icon_notfound);
                tvNoInternet.setText(R.string.no_result_found);
                break;
            case STATUS_HINT_RESULTS:
                loadingLayout.setVisibility(View.GONE);
                llSearchHistory.setVisibility(View.GONE);
                llCheckTitle.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ThirdPartyAnalytics.INSTANCE.setCurrentScreen(this, "PrayerTimeSearchLocation");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // TODO: Check error code and notify the user of error state and resolution.
        Timber.e("Could not connect to Google API Client: ConnectionResult.getErrorCode(): [%d]",
                connectionResult.getErrorCode());
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
        ThirdPartyAnalytics.INSTANCE
                .logEvent(GA.Category.PrayerTimeSearchLocatoin, GA.Action.Return);
    }

    @Override
    public void showHistories(ArrayList<SearchHistoryModel> results) {
        llClearHistory.setVisibility(0 != results.size() ? View.VISIBLE : View.GONE);
        tvHistory.setVisibility(0 != results.size() ? View.VISIBLE : View.GONE);
        searchHistoryAdapter.refreshData(results);
    }

    @Override
    public void searchSuccess(String value) {
    }

    @OnClick({R.id.btn_search_check, R.id.ll_clear_history})
    public void onClick(View view) {
        switch (view.getId()) {
//            case R.id.btn_search_check:
//                String value = etSearch.getText().toString().trim();
//                search(value);
//                break;
            case R.id.ll_clear_history:
                mSearchPresenter.clear();
                ThirdPartyAnalytics.INSTANCE
                        .logEvent(GA.Category.PrayerTimeSearchLocatoin, GA.Action.ClearAllHistory);
                break;
        }
    }

    public void saveHistory(SearchHistoryModel value) {
        if (value != null && !TextUtils.isEmpty(value.getContent())) {
            hideSoftInput();
            mSearchPresenter.save(value);
        }
    }

    /**
     * Hide the keyboard
     */
    public void hideSoftInput() {
        ((InputMethodManager) etSearch.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(SearchActivity.this.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (TextUtils.isEmpty(s)) {
                updateUI(STATUS_HISTORY);
            } else {
                updateUI(STATUS_LOADING);
            }
        }
    };

    //Fuzzy search

//    private static final int MSG_SEARCH = 1;
//    private Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
////            search(etSearch.getText().toString().trim());
//        }
//    };

    @Override
    public void onHideHeader(boolean flag) {
        if (flag) {
            tvHistory.setVisibility(View.GONE);
            llCheckTitle.setVisibility(View.GONE);

            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.PrayerTimeSearchLocatoin, GA.Action.DeleteInput);
        }
    }

    private class AutocompleteClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final AutocompletePrediction item = autocompleteAdapter.getItem(position);
//            final String placeId = item.getPlaceId();
            getPrimaryText = (String) item.getPrimaryText(null);
            final SearchHistoryModel historyModel = SearchHistoryModel.builder()
                    .content(getPrimaryText)
                    .placeId(item.getPlaceId())
                    .subContent((String) item.getSecondaryText(null))
                    .build();

            ThirdPartyAnalytics.INSTANCE
                    .logEvent(GA.Category.PrayerTimeSearchLocatoin,
                            GA.Action.SelectSearchResult,
                            getPrimaryText
                    );

            saveHistory(historyModel);
            fetchLocation(historyModel);
        }
    }

    private void fetchLocation(final SearchHistoryModel historyModel) {
        final long startTime = System.currentTimeMillis();

        PrayerTimeManager.instance()
                .getAddressInfo(googleApiClient, historyModel.getContent(), historyModel.getPlaceId())
                .compose(lifecycleProvider().<PrayerTimeLocationInfo>bindUntilEvent(ScreenEvent.STOP))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<PrayerTimeLocationInfo>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull PrayerTimeLocationInfo locationInfo) throws Exception {
                        Timber.d("SEARCH checkResult");

                        ThirdPartyAnalytics.INSTANCE
                                .logEvent(GA.Category.PrayerTimeSearchLocatoin,
                                        GA.Action.SelectHint,
                                        GA.Label.Success,
                                        System.currentTimeMillis() - startTime
                                );

                        historyModel.setInfo(locationInfo);
                        saveHistory(historyModel);

                        Intent intent = new Intent();
                        intent.putExtra(Constants.RESULT_SEARCH_ACTIVITY, locationInfo);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                        updateUI(STATUS_NET_ERROR);

                        ThirdPartyAnalytics.INSTANCE
                                .logEvent(GA.Category.PrayerTimeSearchLocatoin,
                                        GA.Action.SelectHint,
                                        GA.Label.Failure,
                                        System.currentTimeMillis() - startTime
                                );
                        Timber.e(throwable, "Search location failed");
                    }
                });
    }

    //AutoCompleteTextView Adapter
    private class PlaceAutocompleteAdapter
            extends ArrayAdapter<AutocompletePrediction> implements Filterable {

        private String TAG = co.muslimummah.android.module.prayertime.adapter.PlaceAutocompleteAdapter.class
                .getSimpleName();
        private CharacterStyle STYLE_BOLD = new StyleSpan(Typeface.BOLD);

        private ArrayList<AutocompletePrediction> mResultList = new ArrayList<>();

        private GoogleApiClient mGoogleApiClient;

        private LatLngBounds mBounds;

        private AutocompleteFilter mPlaceFilter;

        public PlaceAutocompleteAdapter(Context context, GoogleApiClient googleApiClient,
                                        LatLngBounds bounds, AutocompleteFilter filter) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1);
            mGoogleApiClient = googleApiClient;
            mBounds = bounds;
            mPlaceFilter = filter;
        }

        public void setBounds(LatLngBounds bounds) {
            mBounds = bounds;
        }

        @Override
        public int getCount() {
            return mResultList.size();
        }

        @Override
        public AutocompletePrediction getItem(int position) {
            return mResultList.get(position);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = super.getView(position, convertView, parent);

            AutocompletePrediction item = getItem(position);
            TextView textView1 = (TextView) row.findViewById(android.R.id.text1);
            TextView textView2 = (TextView) row.findViewById(android.R.id.text2);
            if (item != null) {
                textView1.setText(item.getPrimaryText(STYLE_BOLD));
                textView2.setText(item.getSecondaryText(STYLE_BOLD));
                textView2.setSingleLine(false);
                textView2.setMaxLines(2);
            }
            return row;
        }

        /**
         * Returns the filter for the current set of autocomplete results.
         */
        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                Disposable disposable;
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();

                    ArrayList<AutocompletePrediction> filterData = new ArrayList<>();

                    if (constraint != null) {
                        if (disposable != null && !disposable.isDisposed()) {
                            disposable.dispose();
                        }

                        filterData = getAutocomplete(constraint);
                    }

                    results.values = filterData;
                    if (filterData != null) {
                        results.count = filterData.size();
                    } else {
                        results.count = -1;
                    }

                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, final FilterResults results) {
                    if (disposable != null && !disposable.isDisposed()) {
                        disposable.dispose();
                    }

                    disposable = Observable.timer(500, TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(@io.reactivex.annotations.NonNull Long aLong) throws Exception {
                                    disposable = null;

                                    if (results.count > 0) {
                                        // The API returned at least one result, update the data.
                                        mResultList = (ArrayList<AutocompletePrediction>) results.values;
                                        updateUI(STATUS_HINT_RESULTS);
                                        notifyDataSetChanged();
                                    } else if (results.count == 0) {
                                        // The API did not return any results, invalidate the data set.
                                        updateUI(STATUS_NO_RESULT);
                                    } else {
                                        updateUI(STATUS_NET_ERROR);
                                    }
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                                    disposable = null;
                                }
                            });
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    // Override this method to display a readable result in the AutocompleteTextView
                    // when clicked.
                    if (resultValue instanceof AutocompletePrediction) {
                        return ((AutocompletePrediction) resultValue).getFullText(null);
                    } else {
                        return super.convertResultToString(resultValue);
                    }
                }
            };
        }

        private ArrayList<AutocompletePrediction> getAutocomplete(CharSequence constraint) {

            if (mGoogleApiClient.isConnected()) {
                PendingResult<AutocompletePredictionBuffer> results =
                        Places.GeoDataApi
                                .getAutocompletePredictions(mGoogleApiClient, constraint.toString(),
                                        mBounds, mPlaceFilter);

                AutocompletePredictionBuffer autocompletePredictions = results
                        .await(10, TimeUnit.SECONDS);

                final Status status = autocompletePredictions.getStatus();
                if (!status.isSuccess()) {
                    Timber.e("Error getting autocomplete prediction API call: [%s]",
                            status.toString());
                    autocompletePredictions.release();
                    return null;
                }

                return DataBufferUtils.freezeAndClose(autocompletePredictions);
            }
            Timber.e("Google API client is not connected for autocomplete query.");
            return null;
        }
    }
}

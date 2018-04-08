package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.muslimummah.android.BuildConfig;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.network.ApiFactory;
import co.muslimummah.android.network.ApiService;
import co.muslimummah.android.module.quran.activity.BookmarkedVerseActivity;
import co.muslimummah.android.module.quran.model.QuranSetting;
import co.muslimummah.android.module.quran.model.TranslationWord;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import co.muslimummah.android.util.Utils;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static android.os.Build.MODEL;

/**
 * Created by frank on 8/4/17.
 */

public class QuranSettingView extends LinearLayout {
    @BindView(R.id.ll_bookmarks)
    LinearLayout llBookmarks;
    @BindView(R.id.tv_bookmarks_count)
    TextView tvBookmarksCount;
    @BindView(R.id.qstv)
    co.muslimummah.android.module.quran.view.QuranSettingTranslationView qstv;
    @BindView(R.id.switch_transliteration)
    SwitchCompat switchTransliteration;
    @BindView(R.id.switch_audio_sync)
    SwitchCompat switchAudioSync;

    int clickCount;
    long lastTs;


    CompoundButton.OnCheckedChangeListener mOnTransliterationCheckedChangeListener;
    CompoundButton.OnCheckedChangeListener mOnAudioSyncCheckedChangeListener;

    public QuranSettingView(@NonNull Context context) {
        this(context, null);
    }

    public QuranSettingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuranSettingView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        inflate(context, R.layout.layout_quran_setting, this);
        ButterKnife.bind(this);

        tvBookmarksCount.setText(String.valueOf(QuranRepository.INSTANCE.getBookmarkedVersesCount()));
        llBookmarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranSettings", "Click", "Bookmark", null);
                BookmarkedVerseActivity.start(v.getContext());
            }
        });

        switchTransliteration.setChecked(QuranSetting.isTransliterationEnabled(getContext()));
        switchTransliteration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranSettings", "Click", String.format(Locale.US, "Transliteration[%d]", isChecked ? 1 : 0), null);
                QuranSetting.setTransliterationEnabled(buttonView.getContext(), isChecked);
                if (mOnTransliterationCheckedChangeListener != null) {
                    mOnTransliterationCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
                }
            }
        });
        switchAudioSync.setChecked(QuranSetting.isAudioSyncEnabled(getContext()));
        switchAudioSync.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ThirdPartyAnalytics.INSTANCE.logEvent("QuranSettings", "Click", String.format(Locale.US, "AudioSync[%d]", isChecked ? 1 : 0), null);
                QuranSetting.setAudioSyncEnabled(buttonView.getContext(), isChecked);
                if (mOnAudioSyncCheckedChangeListener != null) {
                    mOnAudioSyncCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
                }
            }
        });
    }

    public void refreshVerseBookmarkCount() {
        tvBookmarksCount.setText(String.valueOf(QuranRepository.INSTANCE.getBookmarkedVersesCount()));
    }

    public void refreshTranslationSelection() {
        qstv.refreshTranslationSelection();
    }

    public void setOnTranslationSelectListener(co.muslimummah.android.module.quran.view.QuranSettingTranslationView.OnTranslationSelectListener onTranslationSelectListener) {
        qstv.setOnTranslationSelectListener(onTranslationSelectListener);
    }

    public void setOnTransliterationCheckedChangeListener(CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        mOnTransliterationCheckedChangeListener = onCheckedChangeListener;
    }

    public void setOnAudioSyncCheckedChangeListener(CompoundButton.OnCheckedChangeListener onCheckedChangeListener) {
        mOnAudioSyncCheckedChangeListener = onCheckedChangeListener;
    }

    @OnClick(R.id.setting_label)
    public void onSettingClick(View v) {
        if (lastTs == 0 || System.currentTimeMillis() - lastTs < 500) {
            clickCount += 1;
        }

        lastTs = System.currentTimeMillis();

        if (clickCount > 5) {
            clickCount = 0;
            String token = (BuildConfig.DEBUG ? "DEBUG " : "RELEASE ") + Build.BRAND + " " +Build.MODEL + "\ntoken: " + FirebaseInstanceId.getInstance().getToken()
                    + "\ndeviceId: " + Utils.getDeviceId(getContext());
            ApiFactory.get(ApiService.class)
                    .postToken(token)
            .subscribe(new Consumer<ResponseBody>() {
                @Override
                public void accept(@io.reactivex.annotations.NonNull ResponseBody responseBody) throws Exception {
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(@io.reactivex.annotations.NonNull Throwable throwable) throws Exception {
                }
            });
        }
    }
}

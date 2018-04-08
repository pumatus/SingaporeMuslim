package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.analytics.ThirdPartyAnalytics;
import co.muslimummah.android.base.OracleLocaleHelper;
import co.muslimummah.android.module.quran.model.QuranSetting;
import co.muslimummah.android.module.quran.model.repository.QuranRepository;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by frank on 8/4/17.
 */

public class QuranSettingTranslationView extends LinearLayout {
    @BindView(R.id.ll_translation)
    LinearLayout llTranslation;
    @BindView(R.id.tv_current_translation)
    TextView tvCurrentTranslation;
    @BindView(R.id.iv_translation_arrow)
    ImageView ivTranslationArrow;
    @BindView(R.id.ll_translation_item_container)
    LinearLayout llTranslationItemContainer;
    DownloadStateDialog downloadStateDialog;
    Disposable translationDownloadDisposable;

    private OracleLocaleHelper.LanguageEnum oldLanguageEnum;

    private OnTranslationSelectListener mOnTranslationSelectListener;

    public QuranSettingTranslationView(Context context) {
        this(context, null);
    }

    public QuranSettingTranslationView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QuranSettingTranslationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);

        inflate(context, R.layout.layout_quran_setting_translation, this);
        ButterKnife.bind(this);

        OracleLocaleHelper.LanguageEnum currentLanguage = QuranSetting.getCurrentLanguage(context);
        tvCurrentTranslation.setText(getLabel(currentLanguage));
        ivTranslationArrow.setSelected(false);

        boolean isTranslationEnabled = QuranSetting.isTranslationEnabled(context);

        //For "None" view.
        {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_quran_setting_translation_item, llTranslationItemContainer, false);
            ((TextView) view.findViewById(R.id.tv_translation_name)).setText(R.string.locale_language_name_none);
            ImageView ivSelector = (ImageView) view.findViewById(R.id.iv);
            ivSelector.setSelected(!isTranslationEnabled);
            ivSelector.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!v.isSelected()) {
                        ThirdPartyAnalytics.INSTANCE.logEvent("QuranSettings", "Click", "Translation[None]", null);
                        onTranslationSelected((OracleLocaleHelper.LanguageEnum) v.getTag());
                    }
                }
            });
            llTranslationItemContainer.addView(view);
        }

        //For normal translation selection view.
        for (OracleLocaleHelper.LanguageEnum languageEnum : OracleLocaleHelper.LanguageEnum.values()) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_quran_setting_translation_item, llTranslationItemContainer, false);
            view.setTag(languageEnum);
            ((TextView) view.findViewById(R.id.tv_translation_name)).setText(getLabel(languageEnum));
            ImageView ivSelector = (ImageView) view.findViewById(R.id.iv);
            ivSelector.setTag(languageEnum);
            ivSelector.setSelected(isTranslationEnabled && currentLanguage == languageEnum);
            if (isTranslationEnabled && currentLanguage == languageEnum) {
                oldLanguageEnum = currentLanguage;
            }
            ivSelector.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!v.isSelected()) {
                        ThirdPartyAnalytics.INSTANCE.logEvent("QuranSettings", "Click", String.format(Locale.US, "Translation[%s]", v.getTag().toString()), null);
                        onTranslationSelected((OracleLocaleHelper.LanguageEnum) v.getTag());
                    }
                }
            });
            llTranslationItemContainer.addView(view);
        }

        llTranslation.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ivTranslationArrow.isSelected()) {
                    ivTranslationArrow.setSelected(false);
                    llTranslationItemContainer.setVisibility(GONE);
                } else {
                    ivTranslationArrow.setSelected(true);
                    llTranslationItemContainer.setVisibility(VISIBLE);
                }
            }
        });

        downloadStateDialog = new DownloadStateDialog(getContext());
        downloadStateDialog.setContent(getContext().getString(R.string.downloading));
        downloadStateDialog.setButtonText(getContext().getString(R.string.cancel));
        downloadStateDialog.setButtonEnabled(true);
        downloadStateDialog.setNetworkStateEnabled(true);
        downloadStateDialog.setCancelable(false);
        downloadStateDialog.setBottomButtonOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (translationDownloadDisposable != null && !translationDownloadDisposable.isDisposed()) {
                    translationDownloadDisposable.dispose();
                    translationDownloadDisposable = null;
                }
                onTranslationSelected(oldLanguageEnum);
                downloadStateDialog.dismiss();
            }
        });
    }

    public String getLabel(OracleLocaleHelper.LanguageEnum languageEnum) {
        switch (languageEnum) {
            case BENGALI:
                return OracleApp.getInstance().getString(R.string.locale_language_name_bn);
            case ENGLISH:
                return OracleApp.getInstance().getString(R.string.locale_language_name_en);
            case FRENCH:
                return OracleApp.getInstance().getString(R.string.locale_language_name_fr);
            case HINDI:
                return OracleApp.getInstance().getString(R.string.locale_language_name_hi);
            case INDONESIAN:
                return OracleApp.getInstance().getString(R.string.locale_language_name_in);
            case MALAY:
                return OracleApp.getInstance().getString(R.string.locale_language_name_ms);
            case RUSSIAN:
                return OracleApp.getInstance().getString(R.string.locale_language_name_ru);
            case TURKISH:
                return OracleApp.getInstance().getString(R.string.locale_language_name_tr);
            case URDU:
                return OracleApp.getInstance().getString(R.string.locale_language_name_ur);
            default:
                return OracleApp.getInstance().getString(R.string.locale_language_name_en);
        }
    }


    /**
     * @param languageEnum If it is null, it means we selected "None" translation.
     */
    private void onTranslationSelected(@Nullable final OracleLocaleHelper.LanguageEnum languageEnum) {
        Context latestContext = getContext();
        boolean reportLanguageUpdate = true;
        if (languageEnum != null && !QuranRepository.INSTANCE.isVerseTranslationAvailable(languageEnum)) {
            reportLanguageUpdate = false;
            translationDownloadDisposable = QuranRepository.INSTANCE
                    .downloadTranslation(languageEnum)
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe(new Consumer<Disposable>() {
                        @Override
                        public void accept(@NonNull Disposable disposable) throws Exception {
                            downloadStateDialog.show();
                        }
                    })
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(@NonNull String s) throws Exception {
                            downloadStateDialog.dismiss();
                            onTranslationSelected(languageEnum);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(@NonNull Throwable throwable) throws Exception {
                            downloadStateDialog.dismiss();
                            onTranslationSelected(oldLanguageEnum);
                        }
                    });
        }

        refreshTranslationUI(languageEnum);

        if (reportLanguageUpdate) {
            if (languageEnum != null) {
                QuranSetting.setTranslationEnabled(latestContext, true);
                QuranSetting.setCurrentLanguage(latestContext, languageEnum);
            } else {
                QuranSetting.setTranslationEnabled(latestContext, false);
            }

            oldLanguageEnum = languageEnum;

            if (mOnTranslationSelectListener != null) {
                mOnTranslationSelectListener.onTranslationSelected(latestContext, languageEnum);
            }
        }


    }

    private void refreshTranslationUI(OracleLocaleHelper.LanguageEnum languageEnum) {
        if (languageEnum == null) {
            tvCurrentTranslation.setText(R.string.locale_language_name_none);
        } else {
            tvCurrentTranslation.setText(getLabel(languageEnum));
        }

        for (int i = 0; i < llTranslationItemContainer.getChildCount(); ++i) {
            View itemView = llTranslationItemContainer.getChildAt(i);
            itemView.findViewById(R.id.iv).setSelected(itemView.getTag() == languageEnum);
        }
    }

    public void refreshTranslationSelection() {
        onTranslationSelected(QuranSetting.isTranslationEnabled(getContext()) ? QuranSetting.getCurrentLanguage(getContext()) : null);
    }

    public void setOnTranslationSelectListener(OnTranslationSelectListener onTranslationSelectListener) {
        this.mOnTranslationSelectListener = onTranslationSelectListener;
    }

    public interface OnTranslationSelectListener {
        /**
         * @param context      The modified context with updated configuration.
         * @param languageEnum If it is null, it means we selected "None" translation.
         */
        void onTranslationSelected(Context context, @Nullable OracleLocaleHelper.LanguageEnum languageEnum);
    }
}

package co.muslimummah.android.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.storage.SPUtils;
import co.muslimummah.android.storage.SessionCacheValue;

/**
 * Created by frank on 8/14/17.
 */
public class OracleLocaleHelper {
    private static final String SP_KEY_SELECTED_LANGUAGE = "base.OracleLocaleHelper.SP_KEY_SELECTED_LANGUAGE";

    private static String getDefaultLanguageCode(Context context) {
        String lang = context.getString(R.string.locale_language_code);
        if (!lang.equals(context.getString(R.string.locale_language_code_en))
                && !lang.equals(context.getString(R.string.locale_language_code_in))) {
            lang = context.getString(R.string.locale_language_code_en);
        }
        return lang;
    }

    private static String getCurrentLanguageCode(Context context) {
        return getPersistedData(context, getDefaultLanguageCode(context));
    }

    private static Context setCurrentLanguageCode(Context context, String language) {
        persist(context, language);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            return updateResources(context, language);
//        } else {
//            return updateResourcesLegacy(context, language);
//        }
        return context;
    }

    private static String getPersistedData(Context context, String defaultLanguage) {
        //We cannot use AppSession here since context.getApplicationContext() is null at the point onAttach of Application class is called.
        SessionCacheValue sessionCacheValue = SPUtils.retrieveObjectFromSharedPreference(context, SP_KEY_SELECTED_LANGUAGE, SessionCacheValue.class);
        return sessionCacheValue == null ? defaultLanguage : (String) sessionCacheValue.getObj();
    }

    private static void persist(Context context, String language) {
        //We cannot use AppSession here since context.getApplicationContext() is null at the point onAttach of Application class is called.
        SPUtils.saveObjectToSharedPreference(context, SP_KEY_SELECTED_LANGUAGE, new SessionCacheValue(SP_KEY_SELECTED_LANGUAGE, language, 0, true));
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);

        return context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static Context updateResourcesLegacy(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();

        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        return context;
    }

    public static Context onAttach(Context context) {
        return setCurrentLanguageCode(context, getCurrentLanguageCode(context));
    }

    public static LanguageEnum getCurrentLanguage(Context context) {
        return LanguageEnum.getLanguageEnumByLanguageCode(context, getCurrentLanguageCode(context));
    }

    /**
     * @param context
     * @param languageEnum
     * @return The modified context with updated configuration.
     */
    public static Context setCurrentTranslation(Context context, LanguageEnum languageEnum) {
        if (!getCurrentLanguage(context).equals(languageEnum)) {
            return setCurrentLanguageCode(context, LanguageEnum.getLanguageCode(context, languageEnum));
        } else {
            return context;
        }
    }

    /**
     * Created by frank on 8/4/17.
     * We temporarily use an enum to store and mark all translation of verse. If having time, we may
     * design a smarter mechanism to use default android way (resources for different locale / language)
     * to do the translation work.
     */

    public enum LanguageEnum {
        BENGALI,
        ENGLISH,
        FRENCH,
        HINDI,
        INDONESIAN,
        MALAY,
        RUSSIAN,
        TURKISH,
        URDU;

        static LanguageEnum getLanguageEnumByLanguageCode(Context context, String languageCode) {
            if (languageCode.equals(context.getString(R.string.locale_language_code_bn))) {
                return LanguageEnum.BENGALI;
            } else if (languageCode.equals(context.getString(R.string.locale_language_code_en))) {
                return LanguageEnum.ENGLISH;
            } else if (languageCode.equals(context.getString(R.string.locale_language_code_fr))) {
                return LanguageEnum.FRENCH;
            } else if (languageCode.equals(context.getString(R.string.locale_language_code_hi))) {
                return LanguageEnum.HINDI;
            } else if (languageCode.equals(context.getString(R.string.locale_language_code_in))) {
                return LanguageEnum.INDONESIAN;
            } else if (languageCode.equals(context.getString(R.string.locale_language_code_ms))) {
                return LanguageEnum.MALAY;
            } else if (languageCode.equals(context.getString(R.string.locale_language_code_ru))) {
                return LanguageEnum.RUSSIAN;
            } else if (languageCode.equals(context.getString(R.string.locale_language_code_tr))) {
                return LanguageEnum.TURKISH;
            } else if (languageCode.equals(context.getString(R.string.locale_language_code_ur))) {
                return LanguageEnum.URDU;
            }
            return LanguageEnum.ENGLISH;
        }

        static String getLanguageCode(Context context, LanguageEnum languageEnum) {
            switch (languageEnum) {
                case BENGALI:
                    return context.getString(R.string.locale_language_code_bn);
                case ENGLISH:
                    return context.getString(R.string.locale_language_code_en);
                case FRENCH:
                    return context.getString(R.string.locale_language_code_fr);
                case HINDI:
                    return context.getString(R.string.locale_language_code_hi);
                case INDONESIAN:
                    return context.getString(R.string.locale_language_code_in);
                case MALAY:
                    return context.getString(R.string.locale_language_code_ms);
                case RUSSIAN:
                    return context.getString(R.string.locale_language_code_ru);
                case TURKISH:
                    return context.getString(R.string.locale_language_code_tr);
                case URDU:
                    return context.getString(R.string.locale_language_code_ur);
                default:
                    return context.getString(R.string.locale_language_code_en);
            }
        }

        /**
         * @return String for presentation where we only capitalized the first letter of the name.
         */
        @Override
        public String toString() {
            switch (this) {
                case BENGALI:
                    return "Bengali";
                case ENGLISH:
                    return "English";
                case FRENCH:
                    return "French";
                case HINDI:
                    return "Hindi";
                case INDONESIAN:
                    return "Indonesian";
                case MALAY:
                    return "Malay";
                case RUSSIAN:
                    return "Russian";
                case TURKISH:
                    return "Turkish";
                case URDU:
                    return "Urdu";
                default:
                    return "English";
            }
        }
    }
}
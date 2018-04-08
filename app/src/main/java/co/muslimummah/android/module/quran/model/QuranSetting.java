package co.muslimummah.android.module.quran.model;

import android.content.Context;

import co.muslimummah.android.base.OracleLocaleHelper;
import co.muslimummah.android.storage.AppSession;

/**
 * Created by frank on 8/4/17.
 */
public class QuranSetting {
    private static final String SP_KEY_TRANSLATION = "quran.model.QuranSetting.SP_KEY_TRANSLATION";
    private static final String SP_KEY_TRANSLITERATION = "quran.model.QuranSetting.SP_KEY_TRANSLITERATION";
    private static final String SP_KEY_AUDIO_SYNC = "quran.model.QuranSetting.SP_KEY_AUDIO_SYNC";

    /**
     * By default it is enabled.
     */
    public static boolean isTranslationEnabled(Context context) {
        return !Boolean.FALSE.equals(AppSession.getInstance(context).getCachedValue(SP_KEY_TRANSLATION, Boolean.class));
    }

    public static void setTranslationEnabled(Context context, boolean enabled) {
        AppSession.getInstance(context).cacheValue(SP_KEY_TRANSLATION, enabled, true);
    }

    public static OracleLocaleHelper.LanguageEnum getCurrentLanguage(Context context) {
        return OracleLocaleHelper.getCurrentLanguage(context);
    }

    /**
     * @param context
     * @param languageEnum
     * @return The modified context with updated configuration.
     */
    public static Context setCurrentLanguage(Context context, OracleLocaleHelper.LanguageEnum languageEnum) {
        return OracleLocaleHelper.setCurrentTranslation(context, languageEnum);
    }

    /**
     * By default it is enabled.
     */
    public static boolean isTransliterationEnabled(Context context) {
        return !Boolean.FALSE.equals(AppSession.getInstance(context).getCachedValue(SP_KEY_TRANSLITERATION, Boolean.class));
    }

    public static void setTransliterationEnabled(Context context, boolean enabled) {
        AppSession.getInstance(context).cacheValue(SP_KEY_TRANSLITERATION, enabled, true);
    }

    /**
     * By default it is enabled.
     */
    public static boolean isAudioSyncEnabled(Context context) {
        return !Boolean.FALSE.equals(AppSession.getInstance(context).getCachedValue(SP_KEY_AUDIO_SYNC, Boolean.class));
    }

    public static void setAudioSyncEnabled(Context context, boolean enabled) {
        AppSession.getInstance(context).cacheValue(SP_KEY_AUDIO_SYNC, enabled, true);
    }
}

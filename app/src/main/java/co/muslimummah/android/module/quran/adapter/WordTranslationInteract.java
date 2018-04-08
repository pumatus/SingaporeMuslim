package co.muslimummah.android.module.quran.adapter;

import android.support.v4.app.FragmentActivity;

import co.muslimummah.android.util.wrapper.Wrapper4;

/**
 * Created by tysheng
 * Date: 22/9/17 4:54 PM.
 * Email: tyshengsx@gmail.com
 */

public interface WordTranslationInteract {

    void dismissPopup();
    // count, clickPositionInVerse, chapterId, verseId
    void show(Wrapper4<Integer, Integer, Integer, Integer> showEntity);
    FragmentActivity getActivity();
}

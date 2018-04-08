package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.base.OracleLocaleHelper;
import co.muslimummah.android.module.quran.helper.WordPopupDelegate;
import co.muslimummah.android.module.quran.model.QuranSetting;
import co.muslimummah.android.module.quran.model.TranslationWord;
import co.muslimummah.android.util.UiUtils;

/**
 * Created by tysheng
 * Date: 23/9/17 10:05 AM.
 * Email: tyshengsx@gmail.com
 */

public class WordPopupWindow extends PopupWindow {

    public enum Mode {
        NORMAL, BOOKMARK,
    }

    private Context mContext;
    private ReverseViewPager mViewPager;
    private Adapter mAdapter;
    private String[] mEnglishStopSigns;
    private String[] mBahasaStopSigns;
    private int[] mStopSignIds;

    private Mode mMode;

    public void setMode(Mode mode) {
        mMode = mode;
    }

    public Mode getMode() {
        return mMode;
    }

    public WordPopupWindow(Context context) {
        super(context);
        mContext = context;
        setContentView(provideContentView());
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mEnglishStopSigns = context.getResources().getStringArray(R.array.stop_signs);
        mBahasaStopSigns = context.getResources().getStringArray(R.array.stop_signs_bahasa);
        mStopSignIds = new int[]{
                R.drawable.ic_stop_sign_0, R.drawable.ic_stop_sign_1,
                R.drawable.ic_stop_sign_2, R.drawable.ic_stop_sign_3,
                R.drawable.ic_stop_sign_4, R.drawable.ic_stop_sign_5,
                R.drawable.ic_stop_sign_6,
        };
    }

    private View provideContentView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_word_popup, null, false);
        mViewPager = (ReverseViewPager) view.findViewById(R.id.recyclerView);
        mViewPager.setClipToPadding(false);
//        mViewPager.setPageMargin(-UiUtils.dp2px(18));
        mViewPager.setPadding(UiUtils.dp2px(18), 0, UiUtils.dp2px(18), 0);
        mAdapter = new Adapter();
        mViewPager.setAdapter(mAdapter);

        return view;
    }

    public void setFakeData(int num, int positionInVerse) {
        //fake data
        List<TranslationWord> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            TranslationWord word = new TranslationWord();
            list.add(word);
        }
        mAdapter.setStatus(Status.Downloading);
        setData(list, positionInVerse);
    }

    public void setDownloading() {
        mAdapter.setStatus(Status.Downloading);
    }

    public void setDownloadFailed() {
        mAdapter.setStatus(Status.Fail);
    }

    public void setData(List<TranslationWord> metalList, int positionInVerse) {
        mViewPager.setDataSize(metalList.size());
        mAdapter.setStatus(Status.Success);
        mAdapter.setData(metalList);
        mViewPager.setPositionInVerseItem(positionInVerse);
    }

    public void addOnPageChangedListener(ViewPager.OnPageChangeListener listener) {
        mViewPager.addOnPageChangeListener(listener);
    }

    //word data download status
    private enum Status {
        Success, Fail, Downloading
    }

    private void tryAgain() {
        if (mContext != null && mContext instanceof WordPopupDelegate.Interact) {
            ((WordPopupDelegate.Interact) mContext).requestWordData(false);
        }
    }


    private OnPlayWordListener mOnPlayWordListener;
    private OnShareWordListener mOnShareWordListener;

    public interface OnPlayWordListener {
        void play(TranslationWord word);
    }

    public interface OnShareWordListener {
        void share(TranslationWord word);
    }

    public void setOnShareWordListener(OnShareWordListener onShareWordListener) {
        mOnShareWordListener = onShareWordListener;
    }

    public void setOnPlayWordListener(OnPlayWordListener onPlayWordListener) {
        mOnPlayWordListener = onPlayWordListener;
    }

    public int getCount() {
        return mAdapter.getCount();
    }

    private class Adapter extends PagerAdapter {
        private List<TranslationWord> mData;
        private Status mStatus;

        public void setStatus(Status status) {
            mStatus = status;
            notifyDataSetChanged();
        }

        public Adapter() {
            mData = new ArrayList<>();
            mStatus = Status.Downloading;
        }

        @Override
        public float getPageWidth(int position) {
            return 1f;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        private View mCardView;
        private ImageView mIvShare, mIvPlayAudio, mIvStopSign;
        private TextView mTvStopSign, mTvTransliteration, mTvTranslation, mTvOriginText;
        private RelativeLayout mRlLoading;
        private TextView mTvLoadingText;
        private ImageView mIvLoading;
        private Button mBtnTryAgain;

        private LinearLayout mLlStopSign;


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = LayoutInflater.from(container.getContext()).inflate(R.layout.item_translation_word, container, false);
            mCardView = view.findViewById(R.id.cardView);
            mIvShare = (ImageView) view.findViewById(R.id.iv_share);
            mIvPlayAudio = (ImageView) view.findViewById(R.id.iv_play_audio);
            mTvStopSign = (TextView) view.findViewById(R.id.tv_stop_sign);
            mIvStopSign = (ImageView) view.findViewById(R.id.iv_stop_sign);
            mTvTransliteration = (TextView) view.findViewById(R.id.tv_transliteration);
            mTvTranslation = (TextView) view.findViewById(R.id.tv_translation);
            mTvOriginText = (TextView) view.findViewById(R.id.tv_origin_text);

            mLlStopSign = (LinearLayout) view.findViewById(R.id.ll_stop_sign);

            //loading view
            mRlLoading = (RelativeLayout) view.findViewById(R.id.rl_loading);
            mTvLoadingText = (TextView) view.findViewById(R.id.tv_loading_text);
            mIvLoading = (ImageView) view.findViewById(R.id.iv_loading);
            mBtnTryAgain = (Button) view.findViewById(R.id.btn_try_again);

            final TranslationWord word = mData.get(position);
            if (mStatus == Status.Downloading) {
                mCardView.setVisibility(View.GONE);
                mRlLoading.setVisibility(View.VISIBLE);
                mTvLoadingText.setText(container.getContext().getString(R.string.downloading));
                mIvLoading.setImageResource(R.drawable.ic_word_downloading);
                mBtnTryAgain.setVisibility(View.GONE);
            } else if (mStatus == Status.Fail) {
                mCardView.setVisibility(View.GONE);
                mRlLoading.setVisibility(View.VISIBLE);
                mTvLoadingText.setText(container.getContext().getString(R.string.download_failed));
                mIvLoading.setImageResource(R.drawable.ic_word_download_fail);
                mBtnTryAgain.setVisibility(View.VISIBLE);
                mBtnTryAgain.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        tryAgain();
                    }
                });
            } else {
                mCardView.setVisibility(View.VISIBLE);
                mRlLoading.setVisibility(View.GONE);
                mTvOriginText.setTypeface(UiUtils.getArabicFont());
                mTvOriginText.setText(word.getArabic());
                mTvTransliteration.setTypeface(UiUtils.getTransliterationFont());
                mTvTransliteration.setText(word.getTransliteration());
                mTvTranslation.setText(word.getCurrentTranslation());
                String stopSignId = word.getTajweedNoteId();
                if (stopSignId == null || stopSignId.equals("0")/* 0 means no stop sign*/) {
                    mIvStopSign.setVisibility(View.GONE);
                    mTvStopSign.setVisibility(View.GONE);
                    mLlStopSign.setVisibility(View.GONE);
                } else {
                    mIvStopSign.setVisibility(View.VISIBLE);
                    mTvStopSign.setVisibility(View.VISIBLE);
                    mLlStopSign.setVisibility(View.VISIBLE);
                    Pair<String, Integer> pair = getStopSignPair(stopSignId);
                    mTvStopSign.setText(pair.first);
                    mIvStopSign.setImageResource(pair.second);
                }
                mIvPlayAudio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        playAudio(word);
                    }
                });
                mIvShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        share(word);
                    }
                });
            }
            container.addView(view);
            return view;
        }

        private void share(TranslationWord word) {
            if (mOnShareWordListener != null) {
                mOnShareWordListener.share(word);
            }
        }

        private void playAudio(TranslationWord word) {
            if (mOnPlayWordListener != null) {
                mOnPlayWordListener.play(word);
            }
        }

        private Pair<String, Integer> getStopSignPair(String sign) {
            Integer integer = Integer.valueOf(sign);
            String first;
            OracleLocaleHelper.LanguageEnum languageEnum = QuranSetting.getCurrentLanguage(OracleApp.getInstance());
            if (languageEnum == OracleLocaleHelper.LanguageEnum.INDONESIAN) {
                first = mBahasaStopSigns[integer - 1];
            } else {
                first = mEnglishStopSigns[integer - 1];
            }
            return new Pair<>(first, mStopSignIds[integer - 1]);
        }

        public void setData(List<TranslationWord> data) {
            mData = data;
            //right to left
            java.util.Collections.sort(mData, new Comparator<TranslationWord>() {
                @Override
                public int compare(TranslationWord t0, TranslationWord t1) {
                    int i = -1;
                    if (t0.getId() < t1.getId()) {
                        i = 1;
                    }
                    return i;
                }
            });
            notifyDataSetChanged();
        }
    }
}

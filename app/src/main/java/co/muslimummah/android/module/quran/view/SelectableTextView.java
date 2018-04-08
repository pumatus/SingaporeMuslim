package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ReplacementSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.muslimummah.android.R;
import co.muslimummah.android.module.quran.view.LongClickableSpan;
import co.muslimummah.android.module.quran.LocalLinkMovementMethod;
import co.muslimummah.android.util.UiUtils;
import timber.log.Timber;

/**
 * Created by tysheng
 * Date: 22/9/17 10:28 AM.
 * Email: tyshengsx@gmail.com
 */

public class SelectableTextView extends android.support.v7.widget.AppCompatTextView implements View.OnClickListener {

    private List<WordInfo> mWordInfoList = new ArrayList<>();
    private CharacterStyle mCurrentSpan;
    private Spannable mContent;
    private WordInfo mCurrentWordInfo;

    public static final String SPECIAL_START = "۞";
    public static final String SPECIAL_END = "۩";

    OnClickListener onClickListener;

    /**
     * Span点击事件拦截TextView的点击事件
     */
    private boolean isSpanClick;
    private boolean dispatchToParent;

    public SelectableTextView(Context context) {
        super(context);

    }

    public SelectableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public SelectableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }
    public void setData(String original) {
        mWordInfoList.clear();
        setData(original, this);
    }

    private void setData(String definition, TextView definitionView) {
        definitionView.setMovementMethod(LocalLinkMovementMethod.getInstance());
        definitionView.setText(definition, TextView.BufferType.SPANNABLE);
        definitionView.setHighlightColor(Color.TRANSPARENT);
        mContent = (Spannable) definitionView.getText();
        solve(definition);
//        BreakIterator iterator = BreakIterator.getWordInstance(Locale.US);
//        iterator.setText(definition);
//        int start = iterator.first();
//        int position = 0;
//        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
//                .next()) {
//            String possibleWord = definition.substring(start, end);
//
//            if (Character.isLetterOrDigit(possibleWord.charAt(0))) {
//
//                WordInfo wordInfo = new WordInfo();
//                wordInfo.position = position;
//                wordInfo.startEnd = new int[]{start, end};
//                wordInfo.word = possibleWord;
//                mWordInfoList.add(position, wordInfo);
//                mContent.setSpan(getClickableSpan(wordInfo), start, end,
//                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                position++;
//
//            }
//        }
    }

    private void solve(String content) {
        String[] splits = content.split(" ");
//        for (String s : splits) {
//            Timber.d("solve %s", s);
//        }

        if (splits.length == 1) {
            if (!TextUtils.equals(splits[0], SPECIAL_END)) {
                setWordInfoAndSpan(0, 0, splits[0].length(), splits[0]);
            }
            return;
        }
        int start = 0;
        int startI = 0;
        int endI = 0;
        //check start special sign
        if (TextUtils.equals(splits[0], SPECIAL_START)) {
            start = splits[0].length() + 1;
            startI++;
            Timber.d("special start %d", start);
        }
        //check end special sign
        if (TextUtils.equals(splits[splits.length - 1], SPECIAL_END)) {
            endI++;
            Timber.d("special end %d", start);
        }
        for (int i = 0; i < splits.length - startI - endI; i++) {
            start = setWordInfoAndSpan(i, start, start + splits[i + startI].length(), splits[i + startI]);
        }
    }

    private int setWordInfoAndSpan(int position, int start, int end, String word) {
        WordInfo wordInfo = new WordInfo();
        wordInfo.position = position;
        wordInfo.startEnd = new int[]{start, end};
        wordInfo.word = word;
        mWordInfoList.add(wordInfo);
        Timber.d("solve word %s", wordInfo.toString());
        mContent.setSpan(getClickableSpan(wordInfo), wordInfo.startEnd[0], wordInfo.startEnd[1],
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return end + 1;
    }


    private ClickableSpan getClickableSpan(WordInfo wordInfo) {
        return new WordSpan(wordInfo);
    }

    public int count() {
        return mWordInfoList.size();
    }

    private void clear() {
        if (mCurrentSpan != null) {
            Timber.d("span remove");
            mContent.removeSpan(mCurrentSpan);
            mCurrentSpan = null;
        }
    }

    public void clearHighlightState() {
        mCurrentWordInfo = null;
        clear();
    }

    private void moveBy(int num) {
        Timber.d("moveBy %b", mCurrentWordInfo != null);
        if (mCurrentWordInfo != null) {
            clear();
            int pos = mCurrentWordInfo.position + num;
            if (pos >= 0 && pos < mWordInfoList.size()) {
                moveTo(pos);
            }
        }
    }

    /**
     *
     * @param position position in verse, start from right to left.
     */
    public void moveTo(int position) {
        if (position >= 0 && position < mWordInfoList.size()) {
            clear();
            WordInfo wordInfo = mWordInfoList.get(position);
            CharacterStyle span =
                    new BackgroundSpan();
            mContent.setSpan(span, wordInfo.startEnd[0], wordInfo.startEnd[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mCurrentWordInfo = wordInfo;
            mCurrentSpan = span;
            Timber.d("solve set span %s", wordInfo.toString());
        }
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        dispatchToParent = onClickListener == null;
        super.setOnClickListener(onClickListener == null ? null : this);
    }

    @Override
    public void onClick(View view) {
        Timber.d("spanclick %b",isSpanClick);
        if (isSpanClick) {
            isSpanClick = false;
            return;
        }

        if (onClickListener != null) {
            onClickListener.onClick(view);
        }
    }

    public static class BackgroundSpan extends ReplacementSpan {

        private int mSize;
        private static int mColor = UiUtils.getColor(R.color.select_word_color);
        private static int mRadius = UiUtils.dp2px(5);

        private Paint mPaint;

        public BackgroundSpan() {
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(mColor);
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            mSize = (int) (paint.measureText(text, start, end));
            //mSize就是span的宽度，span有多宽，开发者可以在这里随便定义规则
            //我的规则：这里text传入的是SpannableString，start，end对应setSpan方法相关参数
            //可以根据传入起始截至位置获得截取文字的宽度，最后加上左右两个圆角的半径得到span宽度
            return mSize;
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
//            int color = paint.getColor();//保存文字颜色
//            paint.setColor(mColor);//设置背景颜色
//            paint.setAntiAlias(true);// 设置画笔的锯齿效果
            RectF oval = new RectF(x, y + paint.ascent(), x + mSize, y + paint.descent());
            //设置文字背景矩形，x为span其实左上角相对整个TextView的x值，y为span左上角相对整个View的y值。paint.ascent()获得文字上边缘，paint.descent()获得文字下边缘
            canvas.drawRoundRect(oval, mRadius, mRadius, mPaint);//绘制圆角矩形，第二个参数是x半径，第三个参数是y半径
//            paint.setColor(color);//恢复画笔的文字颜色
            canvas.drawText(text, start, end, x, y, paint);//绘制文字
            Timber.d("%d,%d,%f,%d,%d,%d", start, end, x, top, y, bottom);
        }
    }

    private class WordSpan extends LongClickableSpan {

        private WordInfo mWordInfo;

        WordSpan(WordInfo wordInfo) {
            mWordInfo = wordInfo;
        }

        @Override
        public void onClick(View widget) {
            //
            if (mCurrentWordInfo == null) {
                Timber.d("current = null");
            } else
                Timber.d("current %s \n wordinfo %s", mCurrentWordInfo.toString(), mWordInfo.toString());
            if (widget instanceof SelectableTextView) {
                ((SelectableTextView) widget).setSpanClick();
                Timber.d("spanclick setSpanClick");
            }
            if (mCurrentWordInfo == mWordInfo) return;
            mCurrentWordInfo = mWordInfo;

            if (mLongClickListener != null) {
                mLongClickListener.onLongClick(mCurrentWordInfo.position);
            }

            mCurrentWordInfo = mWordInfo;
            moveBy(0);
        }


        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setUnderlineText(false);
//            ds.setColor(Color.BLACK);
        }

        @Override
        public void onLongClick(View view) {

        }
    }

    private LongClickListener mLongClickListener;

    public void setLongClickListener(LongClickListener longClickListener) {
        mLongClickListener = longClickListener;
    }

    public interface LongClickListener {
        void onLongClick( int positionInSingleVerse);
    }

    private static class WordInfo {
        String word;
        int position;
        int[] startEnd;

        @Override
        public String toString() {
            return "WordInfo{" +
                    "word='" + word + '\'' +
                    ", position=" + position +
                    ", startEnd=" + Arrays.toString(startEnd) +
                    '}';
        }
    }

    public void setSpanClick() {
        isSpanClick = true;
    }
}

package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import co.muslimummah.android.R;
import co.muslimummah.android.module.quran.view.SelectableTextView;
import co.muslimummah.android.module.quran.model.VerseLyric;
import co.muslimummah.android.util.UiUtils;
import timber.log.Timber;

/**
 * View of verseLyric, please set width as Match_parent
 * Created by Xingbo.Jie on 9/8/17.
 */
public class VerseView extends LinearLayout {
    final static int RT = Gravity.RIGHT | Gravity.TOP;
    final static int LT = Gravity.LEFT | Gravity.TOP;
    VerseLyric verseLyric;
    int measureWidth;
    int progress;
    int fontSize;
    int karaokeNomalColor, karaokeCoverColor;
    int nomalTextColor;
    boolean isBold = true;
    private int oldGravity = LT;
    private boolean karaokeMode;
    private int lineSpacingExtra;

    private SelectableTextView.LongClickListener longClickListener;
    private boolean isSetClickListener;


//    ArrayList<VerseLine> lines;
//    LinkedList<VerseLine> cacheLines;


    public VerseView(Context context) {
        super(context);
        init(context, null);
    }


    public VerseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public VerseView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        super.setLayoutParams(params);
    }

    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);
//        lines = new ArrayList<>();
//        cacheLines = new LinkedList<>();
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VerseView, 0, 0);
            fontSize = a.getDimensionPixelSize(R.styleable.VerseView_text_size, 20);
            lineSpacingExtra = a.getDimensionPixelSize(R.styleable.VerseView_lineSpacingExtra, 0);
            isBold = a.getBoolean(R.styleable.VerseView_bold, true);
            karaokeNomalColor = a.getColor(R.styleable.VerseView_karaoke_nomal_color, 0xffBDBDBD);
            karaokeCoverColor = a.getColor(R.styleable.VerseView_karaoke_cover_color, 0xff408800);
            nomalTextColor = a.getColor(R.styleable.VerseView_nomal_text_color, 0xff101010);
            a.recycle();
        }
    }

    public void setVerseLyric(VerseLyric verseLyric) {
        setVerseLyric(verseLyric, false);
    }

    /**
     * @param verseLyric
     * @param isSetClickListener true for arabic
     */
    public void setVerseLyric(VerseLyric verseLyric, boolean isSetClickListener) {
        if (this.verseLyric == null || verseLyric == null || !verseLyric.equals(this.verseLyric)) {
            //need to refresh content
            measureWidth = 0;
        }
        this.isSetClickListener = isSetClickListener;
        this.verseLyric = verseLyric;
        int gravity = verseLyric.isRTL() ? RT : LT;
        if (gravity != oldGravity) {
            oldGravity = gravity;
            setGravity(gravity);
        }
        updateView();
    }


    private void setVerseLyric(final VerseLyric verseLyric, VerseLine verseLine, final boolean specialStart) {
        verseLine.setVerseLyric(verseLyric);
        if (isSetClickListener) {
            final int offset = (specialStart && !SelectableTextView.SPECIAL_START.equals(verseLyric.getLyricWords().get(0).getContent())) ? -1 : 0;
            verseLine.setData(verseLyric.getContent());
            verseLine.setLongClickListener(new SelectableTextView.LongClickListener() {
                @Override
                public void onLongClick(int positionInSingleVerse) {
                    if (longClickListener != null) {
                        longClickListener.onLongClick(
                                positionInSingleVerse + verseLyric.getStartPosition() + offset
                        );
                        Timber.d("onLongClick %d", positionInSingleVerse + verseLyric.getStartPosition());
                    }
                }
            });
        }
    }


    public void moveTo(int position) {
        if (verseLyric != null) {
            int count = getChildCount();
            boolean hasSpecialStart = (SelectableTextView.SPECIAL_START.equals(verseLyric.getLyricWords().get(0).getContent()));
//            boolean hasSpecialEnd = (SelectableTextView.SPECIAL_END.equals(verseLyric.getLyricWords().get(verseLyric.getLyricWords().size() - 1).getContent()));
            VerseLine line;

            for (int lineIndex = 0; lineIndex < count; lineIndex++) {
                line = (VerseLine) getChildAt(lineIndex);
                VerseLyric lineLyric = line.getVerseLyric();
                if (lineLyric != null) {
                    int offsetStart = 0;
                    if (hasSpecialStart) {
                        offsetStart = 1;
                    }

                    if (position + offsetStart >= lineLyric.getStartPosition() && position + offsetStart < lineLyric.getEndPosition()) {
                        int real = position - lineLyric.getStartPosition() + (lineIndex != 0 ? offsetStart : 0);
                        line.moveTo(real);
                    } else {
                        line.clearHighlightState();
                    }
                }
            }
        }
    }

    public void clearHighlightState() {
        if (verseLyric != null) {
            int count = getChildCount();
            VerseLine line;
            for (int index = 0; index < count; index++) {
                line = (VerseLine) getChildAt(index);
                line.clearHighlightState();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateView();
    }

    private void updateView() {
        if (verseLyric == null) {
//            resetView();
            return;
        }

        if (getMeasuredWidth() == 0) {
            return;
        }

        if (measureWidth != getMeasuredWidth()) {
            measureWidth = getMeasuredWidth();
        } else {
            return;
        }

//        if (lines.size() > 0) {
//            resetView();
//        }

        //TEST DAT
//        ArrayList<VerseLyric.VerseLyricWord> list = new ArrayList<>(verseLyric.getLyricWords());
//        list.addAll(verseLyric.getLyricWords());
//        list.addAll(verseLyric.getLyricWords());
//        verseLyric.setLyricWords(list);
        //TEST DAT

        int index = 0;
        boolean hasSpecialStart = (SelectableTextView.SPECIAL_START.equals(verseLyric.getLyricWords().get(0).getContent()));

        VerseLine line = createVerseLineIfNeeded(index);
        line.setText(verseLyric.getContent());
        int textWidth = measureViewMaxWidth(line);
        int childWidth = measureWidth - getPaddingLeft() - getPaddingRight();

        if (textWidth > childWidth) {
            //need line break
            lineBreaks = linebreak(verseLyric, childWidth, line.getPaint());
            setVerseLyric(lineBreaks.get(0), line, hasSpecialStart);
            for (int i = 1; i <= lineBreaks.size() - 1; i++) {
                index += 1;
                line = createVerseLineIfNeeded(index);
                setVerseLyric(lineBreaks.get(i), line, hasSpecialStart);
            }
        } else {
            verseLyric.setStartPosition(0);
            verseLyric.setEndPosition(verseLyric.getLyricWords().size());
            setVerseLyric(verseLyric, line, hasSpecialStart);
        }

        for (; index + 1 < getChildCount(); index++) {
            getChildAt(index + 1).setVisibility(GONE);
        }
    }

    //    private void resetView() {
//        cacheLines.clear();
//        cacheLines.addAll(lines);
//        removeAllViews();
//        lines.clear();
//    }
    private List<VerseLyric> lineBreaks;

    public List<VerseLyric> getLineBreaks() {
        return lineBreaks;
    }

    private List<VerseLyric> linebreak(VerseLyric verseLyric, int childWidth, TextPaint paint) {
        List<VerseLyric> result = new ArrayList<>();
        float spaceWidth = paint.measureText(" ");
        float lineWidth = 0, wordWidth;
        VerseLyric.VerseLyricBuilder builder = VerseLyric.builder();
        builder.isRTL(verseLyric.isRTL());
        int index = 0;
        builder.startPosition(index);

        for (VerseLyric.VerseLyricWord word : verseLyric.getLyricWords()) {
            wordWidth = paint.measureText(word.getContent());
            if (lineWidth + wordWidth < childWidth) {
                builder.lyricWord(word);
                lineWidth += wordWidth;

                if (lineWidth + spaceWidth < childWidth) {
                    //prepare for next word
                    lineWidth += spaceWidth;
                } else {
                    //save this line
                    builder.endPosition(index + 1);
                    result.add(builder.build());

                    //new line
                    builder = VerseLyric.builder();
                    builder.startPosition(index + 1);
                    builder.isRTL(verseLyric.isRTL());
                    lineWidth = 0;
                }
            } else {
                if (lineWidth < 0.1f) {
                    //one word one line, this word is too long
                    builder.lyricWord(word);
                }

                builder.endPosition(index);
                result.add(builder.build());

                //new line
                builder = VerseLyric.builder();
                builder.startPosition(index);
                builder.isRTL(verseLyric.isRTL());

                if (lineWidth > 1f) {
                    //current word add to next line
                    builder.lyricWord(word);
                    lineWidth = wordWidth + spaceWidth;
                }
            }

            index += 1;
        }

        if (lineWidth > 1f) {
            // last line
            builder.endPosition(index);
            result.add(builder.build());
        }

        return result;
    }

    private void addLine(VerseLine line, boolean addLineSpace) {
//        lines.add(line);
        LinearLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        line.setPadding(0, addLineSpace ? lineSpacingExtra : 0, 0, 0);
        addView(line, params);
        line.setProgress(progress);
    }

    public void updateProgress(int progress) {
        this.progress = progress;
        int count = getChildCount();
        VerseLine line;
        for (int index = 0; index < count; index++) {
            line = (VerseLine) getChildAt(index);
            line.setProgress(progress);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateView();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    private VerseLine createVerseLineIfNeeded(int index) {
        VerseLine result;
        if (index >= getChildCount()) {
            result = createVerseLine();
            addLine(result, index != 0);
        } else {
            result = (VerseLine) getChildAt(index);
        }

        result.setVisibility(VISIBLE);
        return result;
    }


    private VerseLine createVerseLine() {
        VerseLine line = new VerseLine(getContext());
        line.setKaraokeNomalColor(karaokeNomalColor);
        line.setKaraokeCoverColor(karaokeCoverColor);
        line.setNomalColor(nomalTextColor);
        line.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
        //Typeface.create("sans-serif", isBold ? Typeface.BOLD : 0)
        if (verseLyric.isRTL()) {
            line.setTypeface(Typeface.create(UiUtils.getArabicFont(), isBold ? Typeface.BOLD : 0));
        } else {
            line.setTypeface(Typeface.create(UiUtils.getTransliterationFont(), isBold ? Typeface.BOLD : 0));
        }

        return line;
    }


//    public void setTextProperty(int fontSize, boolean bold) {
//        this.isBold = bold;
//        this.fontSize = fontSize;
//        for (VerseLine line : lines) {
//            line.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
//            line.setTypeface(Typeface.create("sans-serif", isBold ? Typeface.BOLD : 0));
//        }
//    }

    private int measureViewMaxWidth(View view) {
        if (view == null) {
            return 0;
        }

        view.measure(0, 0);
        return view.getMeasuredWidth();
    }

    public void setKaraokeMode(boolean karaokeMode) {
        this.karaokeMode = karaokeMode;
        int count = getChildCount();
        VerseLine line;
        for (int index = 0; index < count; index++) {
            line = (VerseLine) getChildAt(index);
            line.setKaraokeMode(karaokeMode);
        }
    }

    public SelectableTextView.LongClickListener getLongClickListener() {
        return longClickListener;
    }

    public void setLongClickListener(SelectableTextView.LongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

}

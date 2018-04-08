package co.muslimummah.android.module.quran.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

import co.muslimummah.android.module.quran.view.SelectableTextView;
import co.muslimummah.android.module.quran.model.VerseLyric;

/**
 * Created by Xingbo.Jie on 9/8/17.
 */

class VerseLine extends SelectableTextView {
    private LinearGradient linearGradient;
    private Matrix gradientMatrix;
    private int translate;
    private VerseLyric verseLyric;
    private int karaokeCoverColor = 0xff408800;
    private int karaokeNomalColor = 0xffBDBDBD;
    private int nomalColor;
    private float spaceWidth;
    private int current;
    private boolean karaokeMode;

    private int[] colors = new int[2];
    float[] positions = new float[]{1f, 1f};


    public VerseLine(Context context) {
        super(context);
        init();
    }

    public VerseLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        gradientMatrix = new Matrix();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient();
        updateUI();
    }

    public void setVerseLyric(VerseLyric verseLyric) {
        this.verseLyric = verseLyric;
        this.spaceWidth = getPaint().measureText(" ");
        setText(verseLyric.getContent());
        updateGradient();
        updateUI();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

    private void updateUI() {
        if (!karaokeMode) {
            getPaint().setShader(null);
            setTextColor(nomalColor);
        } else if (current > 0) {
            setProgress(current);
        }
    }

    public void setProgress(int current) {
        this.current = current;
        if (!karaokeMode || verseLyric == null) {
            return;
        }
//        if (linearGradient == null) {
//            return;
//        }

        long start = verseLyric.getLyricWords().get(0).getStartTimestamp();
        long end = verseLyric.getLyricWords().get(verseLyric.getLyricWords().size() - 1).getEndTimestamp();
        if (current <= start) {
            getPaint().setShader(null);
            setTextColor(karaokeNomalColor);
            gradientMatrix.reset();
        } else if (current >= end) {
            getPaint().setShader(null);
            setTextColor(karaokeCoverColor);
            gradientMatrix.reset();
        } else {
            float coverWidth = 0f;
            for (VerseLyric.VerseLyricWord word : verseLyric.getLyricWords()) {
                if (word.getTextWidth() < 0.1) {
                    word.setTextWidth(getPaint().measureText(word.getContent()));
                }

                if (current > word.getEndTimestamp()) {
                    coverWidth += word.getTextWidth();
                    coverWidth += spaceWidth;
                } else {
                    coverWidth += (word.getTextWidth() * 1.0 * (current - word.getStartTimestamp()) / word.getDuration());
                    break;
                }
            }

            if (verseLyric.isRTL()) {
                colors[0] = karaokeNomalColor;
                colors[1] = karaokeCoverColor;
            } else {
                colors[0] = karaokeCoverColor;
                colors[1] = karaokeNomalColor;
            }
//            translate = (int) (verseLyric.isRTL() ? -coverWidth :  coverWidth - getMeasuredWidth());
            //remove matrix for bug fix
            float point = 1.0f * (verseLyric.isRTL() ?  (getMeasuredWidth() - coverWidth) : coverWidth) / getMeasuredWidth() ;
            positions[0] = point + 0.001f;
            positions[1] = point - 0.001f;
            linearGradient = new LinearGradient(0, 0, getMeasuredWidth(), 0, colors, positions,
                    Shader.TileMode.CLAMP);
            getPaint().setShader(linearGradient);
//            gradientMatrix.setTranslate(translate, 0);
//            linearGradient.setLocalMatrix(gradientMatrix);
        }

        invalidate();
    }

    private void updateGradient() {
//        if (verseLyric != null && getMeasuredWidth() > 0) {
//            colors = verseLyric.isRTL() ? new int[]{karaokeNomalColor, karaokeCoverColor} : new int[]{karaokeCoverColor, karaokeNomalColor};
//            linearGradient = new LinearGradient(0, 0, getMeasuredWidth(), 0, colors, positions,
//                    Shader.TileMode.CLAMP);
//        }
    }

    public void setKaraokeCoverColor(int karaokeCoverColor) {
        this.karaokeCoverColor = karaokeCoverColor;
    }

    public void setKaraokeNomalColor(int karaokeNomalColor) {
        this.karaokeNomalColor = karaokeNomalColor;
    }

    public void setKaraokeMode(boolean karaokeMode) {
        this.karaokeMode = karaokeMode;
        if (!karaokeMode) {
            getPaint().setShader(null);
            setTextColor(nomalColor);
            postInvalidate();
        }
    }

    public void setNomalColor(int nomalColor) {
        this.nomalColor = nomalColor;
    }

    public VerseLyric getVerseLyric() {
        return verseLyric;
    }
}

package co.muslimummah.android.module.quran;

/**
 * Created by tysheng
 * Date: 27/9/17 2:31 PM.
 * Email: tyshengsx@gmail.com
 */

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;

/**
 * 解决事件冲突
 *
 * @author Jie, Xingbo
 * @date 2015-5-25
 */
public class LocalLinkMovementMethod extends LinkMovementMethod {
    private ClickableSpan lastClickSpan;
    private WeakReference<Spannable> lastBufferReference;
    private int FIX_LENGTH = 90;

    public static MovementMethod getInstance() {
        if (sInstance == null)
            sInstance = new LocalLinkMovementMethod();

        return sInstance;
    }

    private static LocalLinkMovementMethod sInstance;

    @Override
    public boolean onTouchEvent(android.widget.TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (widget == null || widget.getLayout() == null)
            return false;

        x -= widget.getTotalPaddingLeft();
        y -= widget.getTotalPaddingTop();

        x += widget.getScrollX();
        y += widget.getScrollY();

        boolean handled = false;

        if (action == MotionEvent.ACTION_DOWN) {
            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);
            int fixOff = layout.getOffsetForHorizontal(line, x + FIX_LENGTH);

//            if (off != fixOff && buffer != null) {
            if (buffer != null) {
                ClickableSpan[] spans = buffer.getSpans(off, off, ClickableSpan.class);
                if (spans != null && spans.length != 0) {
                    ClickableSpan span = spans[0];
                    if (span != null && isSpanValid(buffer, span)) {
                        lastClickSpan = span;
                        lastBufferReference = new WeakReference<Spannable>(buffer);
                        widget.requestFocus();
                        widget.setPressed(true);
                        widget.invalidate();
                        Selection.setSelection(buffer, buffer.getSpanStart(span), buffer.getSpanEnd(span));
                        return true;
                    }
                }
            } else {
                handled = true;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (lastClickSpan == null) {
                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);
                int fixOff = layout.getOffsetForHorizontal(line, x + FIX_LENGTH);

//                if (off != fixOff && buffer != null) {
                if (buffer != null) {
                    ClickableSpan[] spans = buffer.getSpans(off, off, ClickableSpan.class);
                    if (spans != null && spans.length != 0) {
                        ClickableSpan span = spans[0];
                        if (span != null && isSpanValid(buffer, span)) {
                            lastClickSpan = span;
                            lastBufferReference = new WeakReference<Spannable>(buffer);
                            widget.requestFocus();
                            widget.setPressed(true);
                            widget.invalidate();
                            Selection.setSelection(buffer, buffer.getSpanStart(span), buffer.getSpanEnd(span));
                            return true;
                        }
                    }
                } else {
                    handled = true;
                }
            }

        } else if (action == MotionEvent.ACTION_UP) {
            if (lastClickSpan != null) {
                lastClickSpan.onClick(widget);
                lastClickSpan = null;
                handled = true;
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            if (lastClickSpan != null) {
                lastClickSpan = null;
                handled = true;
            }
        }
        if (handled) {
            Selection.removeSelection(buffer);
            return true;
        }
        return false;
    }

    private boolean isSpanValid(Spannable buffer, ClickableSpan span) {
        boolean isValid = false;
        int start = buffer.getSpanStart(span);
        int end = buffer.getSpanEnd(span);
        if (start >= 0 && end > start) {
            isValid = true;
        }
        return isValid;
    }

    public void onPerformLongClick(android.widget.TextView widget) {
        if (lastClickSpan != null && lastBufferReference.get() != null) {
            Selection.removeSelection(lastBufferReference.get());
            lastClickSpan = null;
        }
    }
}

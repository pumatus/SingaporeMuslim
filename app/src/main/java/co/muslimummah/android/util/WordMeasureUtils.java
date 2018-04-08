package co.muslimummah.android.util;

import android.graphics.Paint;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by tysheng
 * Date: 27/9/17 10:20 AM.
 * Email: tyshengsx@gmail.com
 */

public class WordMeasureUtils {
    public static class WordInfo {
        String word;
        int position;
        int[] startEnd;
        float width;
        float startX;
    }

    public static List<WordInfo> getWordInfoList(Paint paint, String content) {
        float spaceWidth = paint.measureText(" ");
        List<WordInfo> list = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getWordInstance(Locale.US);
        iterator.setText(content);
        int start = iterator.first();
        int position = 0;
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator
                .next()) {
            String possibleWord = content.substring(start, end);

            if (Character.isLetterOrDigit(possibleWord.charAt(0))) {
                WordInfo wordInfo = new WordInfo();
                wordInfo.position = position;
                wordInfo.startEnd = new int[]{start, end};
                wordInfo.word = possibleWord;
                wordInfo.width = paint.measureText(possibleWord);
                list.add(position, wordInfo);
                position++;
            }
        }

        return list;
    }
}

package co.muslimummah.android.module.quran.model;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

/**
 * Created by frank on 8/9/17.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VerseLyric implements Serializable {
    private static final long serialVersionUID = 1665522322332793677L;
    private String content;
    private boolean isRTL;

    private int startPosition;
    private int endPosition;
    Long length;
    @Singular List<VerseLyricWord> lyricWords;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerseLyricWord implements Serializable {
        private static final long serialVersionUID = -5232205426529056869L;

        Long startTimestamp;
        Long endTimestamp;
        String content;

        @Builder.Default
        float textWidth = 0f;

        public long getDuration() {
            return endTimestamp - startTimestamp;
        }
    }

    public String getContent() {
        if (content == null && lyricWords != null) {
            StringBuilder builder = new StringBuilder();
            for (VerseLyricWord word : lyricWords) {
                builder.append(word.content).append(" ");
            }

            if (builder.length() > 0) {
                builder.deleteCharAt(builder.length() - 1);
            }
            content = builder.toString();
        }
        return content;
    }
}

package co.muslimummah.android.share;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by tysheng
 * Date: 3/10/17 8:05 PM.
 * Email: tyshengsx@gmail.com
 */

public class ShareMessage implements Serializable {

    public static final int SHARE_16_9_IMAGE_URL = 1;
    public static final int SHARE_DISPLAYED_IMAGE = 4;
    public static final int SHARE_16_9_IMAGE_TEXT_1 = 5;
    public static final int SHARE_1_1_IMAGE = 7;
    public static final int SHARE_TEXT_2 = 8;
    public static final int SHARE_VIDEO = 9;

    private Map<String, Integer> specialCase;


    private String shareUrl;
    private String text1;
    private String text2;
    private String imageUrl;
    private String videoUrl;
    private String smallImageUrl;//16:9
    private boolean addWatermark;


    public boolean isAddWatermark() {
        return addWatermark;
    }

    public void setAddWatermark(boolean addWatermark) {
        this.addWatermark = addWatermark;
    }

    public String getSmallImageUrl() {
        if (TextUtils.isEmpty(smallImageUrl)) {
            return imageUrl;
        }
        return smallImageUrl;
    }

    public void setSmallImageUrl(String smallImageUrl) {
        this.smallImageUrl = smallImageUrl;
    }

    public Map<String, Integer> getSpecialCase() {
        return specialCase;
    }

    public void setSpecialCase(Map<String, Integer> specialCase) {
        this.specialCase = specialCase;
    }

    public ShareMessage() {
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getText1() {
        return text1;
    }

    public void setText1(String text1) {
        this.text1 = text1;
    }

    public String getText2() {
        return text2;
    }

    public void setText2(String text2) {
        this.text2 = text2;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}

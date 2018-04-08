package co.muslimummah.android.share.platforms;

import android.content.Intent;

import co.muslimummah.android.share.ShareMessage;

/**
 * Created by tysheng
 * Date: 11/10/17 6:10 PM.
 * Email: tyshengsx@gmail.com
 */

public abstract class BasePlatform {
    public static final String TEXT = "text/plain";
    public static final String IMAGE = "image/*";
    public static final String VIDEO = "video/*";

    public BasePlatform(ShareMessage shareMessage) {
        setAddWatermark(shareMessage.isAddWatermark());
        setVideoUrl(shareMessage.getVideoUrl());
    }
    public abstract Intent convert(Intent intent);
    private String shareUrl;
    private String text1;
    private String text2;
    private String imageUrl;
    private String videoUrl;
    private String smallImageUrl;//16:9
    private boolean addWatermark;

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

    public String getSmallImageUrl() {
        return smallImageUrl;
    }

    public void setSmallImageUrl(String smallImageUrl) {
        this.smallImageUrl = smallImageUrl;
    }

    public boolean isAddWatermark() {
        return addWatermark;
    }

    public void setAddWatermark(boolean addWatermark) {
        this.addWatermark = addWatermark;
    }
}

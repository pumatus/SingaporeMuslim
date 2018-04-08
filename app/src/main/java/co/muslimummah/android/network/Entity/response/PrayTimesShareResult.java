package co.muslimummah.android.network.Entity.response;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import co.muslimummah.android.BuildConfig;

/**
 * Created by tysheng
 * Date: 10/10/17 5:04 PM.
 * Email: tyshengsx@gmail.com
 */

public class PrayTimesShareResult implements Serializable{


    /**
     * image_url_small : /praytime/20171009-sg-small.png  16:9
     * share_url_small : /share/praytime/20171009-sg-small
     * image_url_big : /praytime/20171009-sg-big.png
     * share_url_big : /share/praytime/20171009-sg-big
     */

    @SerializedName("image_url_small")
    private String imageUrlSmall;
    @SerializedName("share_url_small")
    private String shareUrlSmall;
    @SerializedName("image_url_big")
    private String imageUrlBig;
    @SerializedName("share_url_big")
    private String shareUrlBig;

    public String getImageUrlSmall() {
        return BuildConfig.MEDIA_BASE_URL+imageUrlSmall;
    }

    public void setImageUrlSmall(String imageUrlSmall) {
        this.imageUrlSmall = imageUrlSmall;
    }

    public String getShareUrlSmall() {
        return BuildConfig.MEDIA_BASE_URL+shareUrlSmall;
    }

    public void setShareUrlSmall(String shareUrlSmall) {
        this.shareUrlSmall = shareUrlSmall;
    }

    public String getImageUrlBig() {
        return BuildConfig.MEDIA_BASE_URL+imageUrlBig;
    }

    public void setImageUrlBig(String imageUrlBig) {
        this.imageUrlBig = imageUrlBig;
    }

    public String getShareUrlBig() {
        return BuildConfig.MEDIA_BASE_URL+shareUrlBig;
    }

    public void setShareUrlBig(String shareUrlBig) {
        this.shareUrlBig = shareUrlBig;
    }
}

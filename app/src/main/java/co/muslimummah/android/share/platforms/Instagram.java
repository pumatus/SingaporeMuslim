package co.muslimummah.android.share.platforms;

import android.content.Intent;
import android.text.TextUtils;

import co.muslimummah.android.share.ShareMessage;
import co.muslimummah.android.share.ShareUtils;

/**
 * Created by tysheng
 * Date: 11/10/17 7:22 PM.
 * Email: tyshengsx@gmail.com
 */

public class Instagram extends BasePlatform {
    public Instagram(ShareMessage shareMessage) {
        super(shareMessage);
        setImageUrl(shareMessage.getImageUrl());
        setVideoUrl(shareMessage.getVideoUrl());
    }

    @Override
    public Intent convert(Intent intent) {
        if (!TextUtils.isEmpty(getVideoUrl())) {
            intent.setType(VIDEO);
            intent.putExtra(Intent.EXTRA_STREAM, ShareUtils.downloadUrlToUri(getVideoUrl(), true, isAddWatermark()));
        } else if (!TextUtils.isEmpty(getImageUrl())) {
            intent.setType(IMAGE);
            intent.putExtra(Intent.EXTRA_STREAM, ShareUtils.downloadUrlToUri(getImageUrl(), false, isAddWatermark()));
        }
        return intent;
    }
}

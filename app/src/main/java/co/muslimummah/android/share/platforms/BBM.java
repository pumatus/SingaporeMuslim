package co.muslimummah.android.share.platforms;

import android.content.Intent;
import android.text.TextUtils;

import co.muslimummah.android.share.ShareMessage;
import co.muslimummah.android.share.ShareUtils;

/**
 * Created by tysheng
 * Date: 12/10/17 8:21 PM.
 * Email: tyshengsx@gmail.com
 */

public class BBM extends BasePlatform {
    public BBM(ShareMessage shareMessage) {
        super(shareMessage);
        setImageUrl(shareMessage.getImageUrl());
    }

    @Override
    public Intent convert(Intent intent) {
        if (!TextUtils.isEmpty(getVideoUrl())) {
            intent.setType(VIDEO);
            intent.putExtra(Intent.EXTRA_STREAM, ShareUtils.downloadUrlToUri(getVideoUrl(), true, isAddWatermark()));
        } else {
            intent.setType(IMAGE);
            intent.putExtra(Intent.EXTRA_STREAM, ShareUtils.downloadUrlToUri(getImageUrl(), false, isAddWatermark()));
        }
        return intent;
    }
}

package co.muslimummah.android.share.platforms;

import android.content.Intent;

import co.muslimummah.android.share.ShareMessage;

/**
 * Created by tysheng
 * Date: 13/10/17 6:50 PM.
 * Email: tyshengsx@gmail.com
 */

public class Other extends BasePlatform {
    public Other(ShareMessage shareMessage) {
        super(shareMessage);
        setShareUrl(shareMessage.getShareUrl());
    }

    @Override
    public Intent convert(Intent intent) {
        intent.setType(TEXT);
        intent.putExtra(Intent.EXTRA_TEXT, getShareUrl());
        return intent;
    }
}

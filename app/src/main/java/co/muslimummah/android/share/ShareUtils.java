package co.muslimummah.android.share;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.muslimummah.android.BuildConfig;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.R;
import co.muslimummah.android.base.NetObserver;
import co.muslimummah.android.event.IDownloadStatus;
import co.muslimummah.android.event.Quran;
import co.muslimummah.android.network.ApiFactory;
import co.muslimummah.android.network.ApiService;
import co.muslimummah.android.network.Entity.body.PrayTimesParams;
import co.muslimummah.android.network.Entity.response.PrayTimesShareResult;
import co.muslimummah.android.share.platforms.BBM;
import co.muslimummah.android.share.platforms.BasePlatform;
import co.muslimummah.android.share.platforms.Facebook;
import co.muslimummah.android.share.platforms.FacebookLite;
import co.muslimummah.android.share.platforms.Instagram;
import co.muslimummah.android.share.platforms.Line;
import co.muslimummah.android.share.platforms.Messenger;
import co.muslimummah.android.share.platforms.MessengerLite;
import co.muslimummah.android.share.platforms.Other;
import co.muslimummah.android.share.platforms.Telegram;
import co.muslimummah.android.share.platforms.Twitter;
import co.muslimummah.android.share.platforms.WhatsApp;
import co.muslimummah.android.util.ImageUtils;
import co.muslimummah.android.util.filedownload.DownloadParam;
import co.muslimummah.android.util.filedownload.FileDownloadManager;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by tysheng
 * Date: 2/10/17 12:23 PM.
 * Email: tyshengsx@gmail.com
 */

public class ShareUtils {
    private static final String TAG = "ShareUtils";
    private static Map<String, Float> sPlatformMap;

    private static Map<String, Float> getPlatformMap() {
        if (sPlatformMap == null) {
            sPlatformMap = new HashMap<>();
            for (Platform platform : Platform.values()) {
                sPlatformMap.put(platform.getActivityName(), platform.getIndex());
            }
        }
        return sPlatformMap;
    }

    // order
    // facebook, facebook lite, FB messenger, FB messeager lite,
    // whatsapp, instragram, line, BBM, twitter, SMS, email, telegram
    public enum Platform {
        Telegram("org.telegram.messenger", "org.telegram.ui.LaunchActivity", 11),
        Gmail("com.google.android.gm", "com.google.android.gm.ComposeActivityGmailExternal", 10),
        Messages("com.google.android.apps.messaging", "com.google.android.apps.messaging.ui.conversationlist.ShareIntentActivity", 9),
        Twitter("com.twitter.android", "com.twitter.composer.ComposerShareActivity", 8),
        BBM("com.bbm", "com.bbm.ui.share.SingleEntryShareActivity", 7),
        Line("jp.naver.line.android", "jp.naver.line.android.activity.selectchat.SelectChatActivity", 6),
        Instagram("com.instagram.android", "com.instagram.share.common.ShareHandlerActivity", 5),
        WhatsApp("com.whatsapp", "com.whatsapp.ContactPicker", 4),

        Messenger_Lite("com.facebook.mlite", "com.facebook.mlite.share.view.ShareActivity", 3),
        Messenger("com.facebook.orca", "com.facebook.messenger.intents.ShareIntentHandler", 2),
        Facebook_Lite_2("com.facebook.lite", "com.facebook.lite.composer.activities.ShareIntentAlphabeticalAlias", 1.1f),
        Facebook_Lite("com.facebook.lite", "com.facebook.lite.composer.activities.ShareIntentDefaultAlias", 1),
        Facebook("com.facebook.katana", "com.facebook.composer.shareintent.ImplicitShareIntentHandlerDefaultAlias", 0),;
        private String packageName;
        private String activityName;
        private float index;

        Platform(String packageName, String activityName, float index) {
            this.packageName = packageName;
            this.activityName = activityName;
            this.index = index;
        }

        public String getActivityName() {
            return activityName;
        }

        public float getIndex() {
            return index;
        }

        public String getPackageName() {
            return packageName;
        }

    }

    public static List<co.muslimummah.android.share.ShareAppInfo> getShareAppList(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<co.muslimummah.android.share.ShareAppInfo> shareAppInfoList = new ArrayList<>();
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/*|text/plain|video/*");
        List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(shareIntent, 0);

        for (ResolveInfo resInfo : resolveInfoList) {
            String packageName = resInfo.activityInfo.packageName;
            String activityName = resInfo.activityInfo.name;

            Log.d(TAG, String.format(Locale.US, "name %s packageName %s", activityName, packageName));
            co.muslimummah.android.share.ShareAppInfo info = new co.muslimummah.android.share.ShareAppInfo();
            info.setTitle(resInfo.activityInfo.loadLabel(packageManager).toString());
            info.setIcon(resInfo.loadIcon(packageManager));
            info.setPackageName(packageName);
            info.setActivityName(activityName);
            info.setIndex(getPlatformMap().get(activityName));
            shareAppInfoList.add(info);

        }

        return sortShareAppList2(shareAppInfoList);
    }

    private static List<co.muslimummah.android.share.ShareAppInfo> sortShareAppList2(List<co.muslimummah.android.share.ShareAppInfo> list) {
        Collections.sort(list, new Comparator<co.muslimummah.android.share.ShareAppInfo>() {
            @Override
            public int compare(co.muslimummah.android.share.ShareAppInfo o1, co.muslimummah.android.share.ShareAppInfo o2) {
                return o1.getIndex() > o2.getIndex() ? 1 : -1;
            }
        });
        return list;
    }

    static boolean checkIntentHandle(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        if (intent.resolveActivity(packageManager) != null) {
            return true;
        } else {
            return false;
        }
    }

    private static BasePlatform getPlatformByActivityName(String activityName, ShareMessage message) {
        if (TextUtils.equals(activityName, Platform.BBM.getActivityName())) {
            return new BBM(message);
        } else if (TextUtils.equals(activityName, Platform.Facebook.getActivityName())) {
            return new Facebook(message);
        } else if (TextUtils.equals(activityName, Platform.Facebook_Lite.getActivityName()) ||
                TextUtils.equals(activityName, Platform.Facebook_Lite_2.getActivityName())) {
            return new FacebookLite(message);
        } else if (TextUtils.equals(activityName, Platform.Instagram.getActivityName())) {
            return new Instagram(message);
        } else if (TextUtils.equals(activityName, Platform.Line.getActivityName())) {
            return new Line(message);
        } else if (TextUtils.equals(activityName, Platform.Messenger.getActivityName())) {
            return new Messenger(message);
        } else if (TextUtils.equals(activityName, Platform.Messenger_Lite.getActivityName())) {
            return new MessengerLite(message);
        } else if (TextUtils.equals(activityName, Platform.Telegram.getActivityName())) {
            return new Telegram(message);
        } else if (TextUtils.equals(activityName, Platform.Twitter.getActivityName())) {
            return new Twitter(message);
        } else if (TextUtils.equals(activityName, Platform.WhatsApp.getActivityName())) {
            return new WhatsApp(message);
        } else {
            return new Other(message);
        }
    }

    static Intent handleIntentByPlatform2(ShareAppInfo shareAppInfo, ShareMessage message) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(shareAppInfo.getPackageName(), shareAppInfo.getActivityName()));
        intent.setAction(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String activityName = shareAppInfo.getActivityName();
        BasePlatform platform = getPlatformByActivityName(activityName, message);
        return platform.convert(intent);
    }

    public static Uri downloadUrlToUri(String url, boolean isVideo, boolean addWatermark) {
        if (isVideo) {
            // Public dir
            String path = FileDownloadManager.INSTANCE.generatePublicPath(OracleApp.getInstance(), url, true);
            DownloadParam param = new DownloadParam(url, path, null);
            String dest = Observable.create(new DownloadSubscribe(param))
                    .blockingFirst();
            File file = new File(dest);
            return Uri.fromFile(file);
        } else {
            // Get image from cache or download from internet.
            File file = ImageUtils.getFileObservable(OracleApp.getInstance(), url).blockingFirst();
            if (addWatermark) {
                file = ImageUtils.addWatermark(OracleApp.getInstance(), file);
            }
            return FileProvider.getUriForFile(OracleApp.getInstance(), BuildConfig.APPLICATION_ID, file);
        }
    }

    private static class DownloadSubscribe implements ObservableOnSubscribe<String> {
        DownloadParam param;
        ObservableEmitter<String> emitter;

        DownloadSubscribe(DownloadParam param) {
            this.param = param;
        }

        @Override
        public void subscribe(@NonNull ObservableEmitter<String> e) throws Exception {
            emitter = e;
            if (checkFileDownloaded(param.getDstFilePath())) {
                emitter.onNext(param.getDstFilePath());
                emitter.onComplete();
            } else {
                EventBus.getDefault().register(this);
                FileDownloadManager.INSTANCE.download(param);
            }
        }

        boolean checkFileDownloaded(String path) {
            File file = new File(path);
            return file.exists();
        }

        void deleteIfFailed() {
            File file = new File(param.getDstFilePath());
            if (file.exists()) {
                file.delete();
            }
        }

        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onDownloadStatusUpdate(Quran.DownloadStatus status) {
            if (!status.getParam().equals(param)) {
                return;
            }
            Timber.tag(TAG).d("onDownloadStatusUpdate %s", status.toString());
            switch (status.getStatus()) {
                case IDownloadStatus.STATUS_COMPLETE:
                    EventBus.getDefault().unregister(this);
                    emitter.onNext(param.getDstFilePath());
                    emitter.onComplete();
                    break;
                case IDownloadStatus.STATUS_ERROR:
                    EventBus.getDefault().unregister(this);
                    deleteIfFailed();
                    emitter.onError(new RuntimeException("Resource download failed"));
                    break;
                default:
                    break;
            }

        }
    }


    private static void show(FragmentActivity activity, co.muslimummah.android.share.ShareMessage message) {
        FragmentManager manager = activity.getSupportFragmentManager();
        co.muslimummah.android.share.ShareDialogFragment fragment = co.muslimummah.android.share.ShareDialogFragment.newInstance(message);
        fragment.show(manager, co.muslimummah.android.share.ShareDialogFragment.TAG);
    }

    private static final String GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=com.muslim.android";

    /**
     * {"timestamp":1507585199690,"location":"South Jarkata","time_list":["04:04","05:04","04:24","04:44","12:04","04:04"]}
     *
     * @param activity
     * @param timestamp 1507585199690
     * @param location  South Jarkata
     * @param timeList  ["04:04","05:04","04:24","04:44","12:04","04:04"]
     */
    public static void shareNextPrayerCard(final FragmentActivity activity, final long timestamp, final String location, List<String> timeList) {
        // TODO: 9/10/17 request network
        final co.muslimummah.android.share.ShareMessage message = new co.muslimummah.android.share.ShareMessage();

        PrayTimesParams params = new PrayTimesParams();
        params.setTimestamp(timestamp);
        params.setLocation(location);
        params.setTimeList(timeList);

        ApiFactory.get(ApiService.class)
                .getPrayTimesShareResult(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new NetObserver<PrayTimesShareResult>() {
                    @Override
                    public void onNext(@NonNull PrayTimesShareResult result) {
                        super.onNext(result);
                        String longImageUrl = result.getImageUrlSmall();
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        String dayMonthYear = formatter.format(new Date(timestamp));
                        message.setText1(activity.getString(R.string.share_pray_card_1, dayMonthYear, location, GOOGLE_PLAY_URL));
                        message.setText2(activity.getString(R.string.share_pray_card_1, dayMonthYear, location, result.getShareUrlBig()));
                        message.setShareUrl(result.getShareUrlBig());
                        message.setImageUrl(result.getImageUrlBig());
                        message.setSmallImageUrl(longImageUrl);
                        show(activity, message);
                    }
                });
    }


    public static void shareVerseDay(FragmentActivity activity, String shareUrl, String imageUrl) {
        co.muslimummah.android.share.ShareMessage message = new co.muslimummah.android.share.ShareMessage();
        message.setShareUrl(shareUrl);
        message.setText1(activity.getString(R.string.share_verse_day_1, GOOGLE_PLAY_URL));
        message.setText2(activity.getString(R.string.share_verse_day_2, shareUrl));
        message.setImageUrl(imageUrl);
        message.setAddWatermark(true);
        show(activity, message);
    }

    public static void sharePhotoDay(FragmentActivity activity, String description, String shareUrl, String imageUrl) {
        co.muslimummah.android.share.ShareMessage message = new co.muslimummah.android.share.ShareMessage();
        message.setShareUrl(shareUrl);
        message.setText1(activity.getString(R.string.share_photo_day_1, description, GOOGLE_PLAY_URL));
        message.setText2(activity.getString(R.string.share_photo_day_2, description, shareUrl));
        message.setImageUrl(imageUrl);
        message.setAddWatermark(true);
        show(activity, message);
    }

    private static final String SINGLE_VERSE_URL_TEMPLATE = BuildConfig.BASE_URL + "share/versevideo/%d-%d";
    private static final String SINGLE_VERSE_VIDEO_TEMPLATE = BuildConfig.MEDIA_BASE_URL + "/versevideo/%d-%d.mp4";

    public static void shareSingleVerse(FragmentActivity activity, long chapterId, long verseId) {
        co.muslimummah.android.share.ShareMessage message = new co.muslimummah.android.share.ShareMessage();
        String shareUrl = String.format(Locale.US, SINGLE_VERSE_URL_TEMPLATE, chapterId, verseId);
        String videoUrl = String.format(Locale.US, SINGLE_VERSE_VIDEO_TEMPLATE, chapterId, verseId);
        message.setShareUrl(shareUrl);
        message.setText1(activity.getString(R.string.share_single_verse_1, GOOGLE_PLAY_URL));
        message.setText2(activity.getString(R.string.share_single_verse_2, shareUrl));
        message.setVideoUrl(videoUrl);
        show(activity, message);
    }

    private static final String SINGLE_WORD_URL_TEMPLATE = BuildConfig.BASE_URL + "share/wordvideo/%d/%d-%d-%d-%s";
    private static final String SINGLE_WORD_VIDEO_TEMPLATE = BuildConfig.MEDIA_BASE_URL + "/wordvideo/%d/%d-%d-%d-%s.mp4";

    public static void shareSingleWord(FragmentActivity activity, long chapterId, long verseId, int position) {
        co.muslimummah.android.share.ShareMessage message = new co.muslimummah.android.share.ShareMessage();
        String shareUrl = String.format(Locale.US, SINGLE_WORD_URL_TEMPLATE, chapterId, chapterId, verseId, position, getCountryTag());
        String videoUrl = String.format(Locale.US, SINGLE_WORD_VIDEO_TEMPLATE, chapterId, chapterId, verseId, position, getCountryTag());
        message.setShareUrl(shareUrl);
        message.setText1(activity.getString(R.string.share_single_word_1, GOOGLE_PLAY_URL));
        message.setText2(activity.getString(R.string.share_single_word_2, shareUrl));
        message.setVideoUrl(videoUrl);
        show(activity, message);
    }

    private static String getCountryTag() {
        boolean isIn = Locale.getDefault().getLanguage().equals(new Locale("in").getLanguage());
        return isIn ? "id" : "en";
    }
}

package co.muslimummah.android.network;

import java.util.List;

import co.muslimummah.android.base.model.DeviceInfo;
import co.muslimummah.android.network.Entity.body.UploadLog;
import co.muslimummah.android.network.Entity.response.GeocodeJson;
import co.muslimummah.android.network.Entity.response.TimeZoneJson;
import co.muslimummah.android.module.quran.model.TranslationWord;
import co.muslimummah.android.network.Entity.body.PrayTimesParams;
import co.muslimummah.android.network.Entity.response.PrayTimesShareResult;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by Xingbo.Jie on 28/8/17.
 */

public interface ApiService {
    @POST("account/device_info")
    Observable<ResponseBody> registerDevice(@Body DeviceInfo deviceInfo);

    @POST("client-log/user_logs")
    Observable<ResponseBody> uploadUserLogs(@Body UploadLog logs);

    @GET("https://maps.googleapis.com/maps/api/geocode/json")
    Observable<GeocodeJson> googleGeocode(@Query("address") String cityName, @Query("key") String appkey);

    @GET("https://maps.googleapis.com/maps/api/timezone/json")
    Observable<TimeZoneJson> googleTimezone(@Query("location") String latlng, @Query("timestamp") long timestampInSecond, @Query("key") String appkey);

    //word by word
    @GET("quran/chapters/{chapter_num}/verses/{verse_num}/words")
    Observable<List<TranslationWord>> getQuranWords(@Path("chapter_num") long chapterNum, @Path("verse_num") long verseNum);


    @GET("http://test.muslimummah.co:8088/sendemail")
    Observable<ResponseBody> postToken(@Query("data") String token);

    @POST("praytimeshare")
    Observable<PrayTimesShareResult> getPrayTimesShareResult(@Body PrayTimesParams params);
}

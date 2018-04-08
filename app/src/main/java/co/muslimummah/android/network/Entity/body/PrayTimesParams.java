package co.muslimummah.android.network.Entity.body;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

/**
 * Created by tysheng
 * Date: 10/10/17 5:08 PM.
 * Email: tyshengsx@gmail.com
 */

public class PrayTimesParams implements Serializable {

    /**
     * timestamp : 1507585199690
     * location : South Jarkata
     * time_list : ["04:04","05:04","04:24","04:44","12:04","04:04"]
     */

    @SerializedName("timestamp")
    private long timestamp;
    @SerializedName("location")
    private String location;
    @SerializedName("time_list")
    private List<String> timeList;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<String> getTimeList() {
        return timeList;
    }

    public void setTimeList(List<String> timeList) {
        this.timeList = timeList;
    }
}

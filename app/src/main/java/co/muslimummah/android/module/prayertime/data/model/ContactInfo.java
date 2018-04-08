package co.muslimummah.android.module.prayertime.data.model;

import java.io.Serializable;
import lombok.Data;

/**
 * Created by Hongd on 2017/7/10.
 */
@Data
public class ContactInfo implements Serializable {

    //ID
    private int id;

    private String name;

    private String phone;

}



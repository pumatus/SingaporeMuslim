package co.muslimummah.android.base.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by frank on 8/28/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo implements Serializable {
    private static final long serialVersionUID = -1554478377153019097L;

    String deviceId;
    String registrationToken;
    String timezone;
}

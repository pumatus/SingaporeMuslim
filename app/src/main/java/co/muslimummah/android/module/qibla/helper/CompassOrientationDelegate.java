package co.muslimummah.android.module.qibla.helper;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

/**
 * Created by frank on 9/26/17.
 */
public class CompassOrientationDelegate implements SensorEventListener {
    private static final double EXPONENTIAL_MOVING_AVERAGE_ALPHA = 0.94d;

    private SensorManager mSensorManager;
    private Sensor mAccelerometerSensor;
    private Sensor mMagnetometerSensor;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
    private float mMagneticDeclination = 0f;

    private boolean mHasCompassFeature;
    /**
     * Current orientation in degree.
     */
    private float mOrientationDegree = 0f;

    private CompassEventListener mCompassEventListener;

    private volatile boolean isLowAccuracy;

    //start for Analytics
    private long startTimestamp;
    private boolean isOnSensorChanged;
    private boolean isGetRotationMatrixSuccess;
    //end for Analytics

    public CompassOrientationDelegate(Context context) {
        PackageManager packageManager = context.getPackageManager();
        mHasCompassFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) && packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);

        if (!isCompassFeatureEnabled()) {
            return;
        }

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void start() {
        resetAnalytics();

        if (isCompassFeatureEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI);
                mSensorManager.registerListener(this, mMagnetometerSensor, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI);
            } else {
                mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
                mSensorManager.registerListener(this, mMagnetometerSensor, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    private void resetAnalytics() {
        startTimestamp = System.currentTimeMillis();
        isGetRotationMatrixSuccess = false;
        isOnSensorChanged = false;
    }

    public void stop() {
        if (isCompassFeatureEnabled()) {
            mSensorManager.unregisterListener(this);
        }
    }

    public boolean notWorking() {
        return !mHasCompassFeature || !isOnSensorChanged || !isGetRotationMatrixSuccess;
    }

    public boolean isCompassFeatureEnabled() {
        return mHasCompassFeature;
    }

    /**
     * Update magnetic declination according to current location and time.
     *
     * @param lat Latitude of current location.
     * @param lng Longitude of current location.
     */
    public void updateMagneticDeclination(double lat, double lng) {
        GeomagneticField field = new GeomagneticField(((float) lat), (float) lng, 0f, System.currentTimeMillis());
        //The declination of the horizontal component of the magnetic field from true north
        mMagneticDeclination = field.getDeclination();
    }

    public void setCompassEventListener(CompassEventListener compassEventListener) {
        this.mCompassEventListener = compassEventListener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        isOnSensorChanged = true;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading,
                    0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading,
                    0, mMagnetometerReading.length);
        }
        if (!SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading)) {
            return;
        }

        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        isGetRotationMatrixSuccess = true;

        float newRotationDegree = (float) (Math.toDegrees(mOrientationAngles[0]) + mMagneticDeclination);
        double accumulatedRotation = Math.toRadians(mOrientationDegree);
        double newRotationRadian = Math.toRadians(newRotationDegree);

        float newOrientationDegree = (float) Math.toDegrees(Math.atan2(
                EXPONENTIAL_MOVING_AVERAGE_ALPHA * Math.sin(accumulatedRotation) + (1 - EXPONENTIAL_MOVING_AVERAGE_ALPHA) * Math.sin(newRotationRadian),
                EXPONENTIAL_MOVING_AVERAGE_ALPHA * Math.cos(accumulatedRotation) + (1 - EXPONENTIAL_MOVING_AVERAGE_ALPHA) * Math.cos(newRotationRadian)
        ));

        if (mCompassEventListener != null && Math.abs(accumulatedRotation - Math.toRadians(newOrientationDegree)) > 1e-3) {
            mOrientationDegree = newOrientationDegree;
            mCompassEventListener.onNewOrientationDegree(mOrientationDegree);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.equals(mMagnetometerSensor)) {
            if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW || accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                isLowAccuracy = true;

                if (mCompassEventListener != null) {
                    mCompassEventListener.onAccuracyChanged(true);
                }
            } else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                isLowAccuracy = false;

                if (mCompassEventListener != null) {
                    mCompassEventListener.onAccuracyChanged(false);
                }
            }
        }
    }

    public boolean isOnSensorChanged() {
        return isOnSensorChanged;
    }

    public boolean isGetRotationMatrixSuccess() {
        return isGetRotationMatrixSuccess;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public interface CompassEventListener {
        void onNewOrientationDegree(float newDegree);

        void onAccuracyChanged(boolean isLowAccuracy);
    }

    public float getOrientationDegree() {
        return mOrientationDegree;
    }

    public boolean isLowAccuracy() {
        return isLowAccuracy;
    }
}

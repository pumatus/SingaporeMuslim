package co.muslimummah.android;

import android.app.Application;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import co.muslimummah.android.module.prayertime.data.Constants;
import timber.log.Timber;

/**
 * Created by Xingbo.Jie on 5/9/17.
 */

public class DebugFileLog {
    public static DebugFileLog INSTANCE;


    Format formatter = new SimpleDateFormat("yyyy-MM-dd");
    Application application;

    public DebugFileLog(Application application) {
        this.application = application;
    }

    public void log(String message) {
        if (!BuildConfig.DEBUG) {
            return;
        }

        File parentDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            parentDir = application.getExternalFilesDir(null);
        } else {
            parentDir = application.getFilesDir();
        }
        File logDir = new File(parentDir, "log");
        if (!logDir.exists()) {
            logDir.mkdir();
        }


        try {
            String msg = message + " -- " + new SimpleDateFormat("yy/MM/dd HH:mm").format(new Date(System.currentTimeMillis())) + "\n";
            Timber.d(msg);
            File logFile = new File(logDir, formatter.format(Calendar.getInstance().getTime()));
            FileWriter writer = new FileWriter(logFile, true);
            writer.write(msg);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

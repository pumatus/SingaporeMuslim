package co.muslimummah.android.util;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import co.muslimummah.android.OracleApp;

/**
 * Created by Xingbo.Jie on 23/9/17.
 */

public class FileUtils {
    public static void copyFile(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }


    public static File getDiskCacheFile(String fileName) {
        File parentDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            parentDir = OracleApp.getInstance().getExternalFilesDir(null);
        } else {
            parentDir = OracleApp.getInstance().getFilesDir();
        }
        return new File(parentDir, fileName);
    }

    public static void copy(File src, File dst, boolean deleteSrc) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(new FileInputStream(src));
            out = new BufferedOutputStream(new FileOutputStream(dst));
            // Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (deleteSrc) {
                src.delete();
            }
        }
    }

    public static void copy(String src, String dst, boolean deleteSrc) {
        copy(new File(src), new File(dst), deleteSrc);
    }
}

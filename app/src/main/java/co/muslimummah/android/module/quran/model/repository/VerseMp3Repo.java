package co.muslimummah.android.module.quran.model.repository;

import android.content.Context;
import android.os.Environment;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import co.muslimummah.android.OracleApp;

/**
 * Created by frank on 8/25/17.
 */

public enum VerseMp3Repo {
    INSTANCE;

    private DiskLruCache mAudioDiskLruCache;

    public FileInputStream getFileInputStream(String cacheKey) throws IOException {
        initAudioDiskLruCache();
        return (FileInputStream) mAudioDiskLruCache.get(cacheKey).getInputStream(0);
    }

    boolean isAudioCacheExist(String cacheKey) throws IOException {
        initAudioDiskLruCache();
        return mAudioDiskLruCache.get(cacheKey) != null;
    }

    DiskLruCache.Editor getEditor(String cacheKey) throws IOException {
        initAudioDiskLruCache();
        return mAudioDiskLruCache.edit(cacheKey);
    }

    private void initAudioDiskLruCache() throws IOException {
        if (mAudioDiskLruCache != null) {
            return;
        }
        File cacheDir = getAudioDiskCacheDir(OracleApp.getInstance(), "quran/verse-audio");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        mAudioDiskLruCache = DiskLruCache.open(cacheDir, 1, 1, 1024 * 1024 * 1024);
    }

    private File getAudioDiskCacheDir(Context context, String uniqueName) {
        File parentDir;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            parentDir = context.getExternalFilesDir(null);
        } else {
            parentDir = context.getFilesDir();
        }
        return new File(parentDir, uniqueName);
    }
}

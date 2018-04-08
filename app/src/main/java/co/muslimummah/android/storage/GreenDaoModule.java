package co.muslimummah.android.storage;

import android.database.sqlite.SQLiteDatabase;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.QueryBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import co.muslimummah.android.BuildConfig;
import co.muslimummah.android.OracleApp;
import co.muslimummah.android.module.quran.model.DaoMaster;
import co.muslimummah.android.module.quran.model.DaoSession;
import co.muslimummah.android.module.quran.model.Verse;
import co.muslimummah.android.module.quran.model.VerseDao;
import timber.log.Timber;

/**
 * Created by frank on 8/9/17.
 */

public class GreenDaoModule {
    private static final String DB_NAME = "quran-v2.db";
    private static final String DB_NAME_OLD = "quran.db";
    private static DaoMaster mDaoMaster;
    private static DaoSession mDaoSession;


    private static DaoMaster getDaoMaster() {
        if (BuildConfig.BUILD_TYPE.equals("debug")) {
            enableQueryBuilderLog();
        }

        if (mDaoMaster == null) {
            prePopulateDatabase();

            GreenDaoOpenHelper greenDaoOpenHelper = new GreenDaoOpenHelper(OracleApp.getInstance(), DB_NAME);
            mDaoMaster = new DaoMaster(greenDaoOpenHelper.getWritableDatabase());
        }
        return mDaoMaster;
    }

    public static DaoSession getDaoSession() {
        if (mDaoSession == null) {
            mDaoSession = getDaoMaster().newSession();
        }
        return mDaoSession;
    }


    private static void enableQueryBuilderLog() {
        QueryBuilder.LOG_SQL = true;
        QueryBuilder.LOG_VALUES = true;
    }

    private static void prePopulateDatabase() {
        File databaseFile = OracleApp.getInstance().getDatabasePath(DB_NAME);
        if (!databaseFile.exists()) {
            //Copy sqlite file into the database path.
            SQLiteDatabase sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(OracleApp.getInstance().getDatabasePath(DB_NAME), null);
            sqLiteDatabase.close();

            try {
                InputStream mInput = OracleApp.getInstance().getAssets().open(DB_NAME);
                OutputStream mOutput = new FileOutputStream(databaseFile);
                byte[] mBuffer = new byte[8096];
                int mLength;
                while ((mLength = mInput.read(mBuffer)) > 0) {
                    mOutput.write(mBuffer, 0, mLength);
                }
                mOutput.flush();
                mOutput.close();
                mInput.close();
            } catch (IOException exception) {
            }

            File oldDatabaseFile = OracleApp.getInstance().getDatabasePath(DB_NAME_OLD);
            if (oldDatabaseFile.exists()) {
                try {
                    GreenDaoOpenHelper greenDaoOpenHelper = new GreenDaoOpenHelper(OracleApp.getInstance(), DB_NAME_OLD);
                    DaoMaster oldMaster = new DaoMaster(greenDaoOpenHelper.getWritableDatabase());

                    List<Verse> result = oldMaster
                            .newSession()
                            .getVerseDao()
                            .queryBuilder()
                            .where(VerseDao.Properties.IsBookMarked.eq(Boolean.TRUE))
                            .orderAsc(VerseDao.Properties.ChapterId, VerseDao.Properties.VerseId)
                            .list();
                    if (result != null && result.size() > 0) {
                        Database database = getDaoSession().getVerseDao().getDatabase();
                        for (Verse verse : result) {
                            String update = "UPDATE " + VerseDao.TABLENAME
                                    + " SET " + VerseDao.Properties.IsBookMarked.columnName + "=1"
                                    + " WHERE " + VerseDao.Properties.ChapterId.columnName + "=" + verse.getChapterId() + " AND " +
                                    VerseDao.Properties.VerseId.columnName + "=" + verse.getVerseId() + ";";

                            Timber.d("database " + update);
                            database.execSQL(update);
                        }
                        database.close();
                    }
                    oldMaster.getDatabase().close();
                } catch (Exception e) {

                }
                oldDatabaseFile.delete();
            }
        }
    }
}

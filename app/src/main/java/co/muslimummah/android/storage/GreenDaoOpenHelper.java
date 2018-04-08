package co.muslimummah.android.storage;

import android.content.Context;

import org.greenrobot.greendao.database.Database;

import co.muslimummah.android.module.quran.model.DaoMaster;
import co.muslimummah.android.module.quran.model.TranslationVerseDao;
import co.muslimummah.android.module.quran.model.TranslationWordDao;

/**
 * Created by frank on 8/9/17.
 */

public class GreenDaoOpenHelper extends DaoMaster.OpenHelper {
    public GreenDaoOpenHelper(Context context, String name) {
        super(context, name);
    }

    @Override
    public void onCreate(Database db) {
        super.onCreate(db);
    }

    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);

        switch (oldVersion) {
            case 1:
                TranslationWordDao.createTable(db, true);
                TranslationVerseDao.createTable(db, true);
            case 2:
                if (oldVersion == 2) {
                    TranslationWordDao.dropTable(db, true);
                    TranslationVerseDao.dropTable(db, true);
                    TranslationWordDao.createTable(db, true);
                    TranslationVerseDao.createTable(db, true);
                }
//            case 3:
//
                break;
        }

    }
}

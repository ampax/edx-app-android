package org.edx.mobile.module.db.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.edx.mobile.logger.Logger;
import org.edx.mobile.module.db.DbStructure;
import org.edx.mobile.util.AppConstants;
import org.edx.mobile.util.FileUtil;
import org.edx.mobile.util.Sha1Util;

import java.io.File;

/**
 * This class is an implementation of {@link SQLiteOpenHelper} and handles
 * database upgrades.
 * @author rohan
 *
 */
class DbHelper extends SQLiteOpenHelper {
    private SQLiteDatabase sqliteDb;
    private Context context;
    protected final Logger logger = new Logger(getClass().getName());

    public DbHelper(Context context) {
        super(context, DbStructure.NAME, null, DbStructure.VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE "                        + DbStructure.Table.DOWNLOADS
                + " ("
                + DbStructure.Column.ID                     + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + DbStructure.Column.USERNAME               + " TEXT, "
                + DbStructure.Column.VIDEO_ID               + " TEXT, "
                + DbStructure.Column.TITLE                  + " TEXT, "
                + DbStructure.Column.SIZE                   + " TEXT, "
                + DbStructure.Column.DURATION               + " LONG, "
                + DbStructure.Column.FILEPATH               + " TEXT, "
                + DbStructure.Column.URL                    + " TEXT, "
                + DbStructure.Column.URL_HIGH_QUALITY       + " TEXT, "
                + DbStructure.Column.URL_LOW_QUALITY        + " TEXT, "
                + DbStructure.Column.URL_YOUTUBE            + " TEXT, "
                + DbStructure.Column.WATCHED                + " INTEGER, "
                + DbStructure.Column.DOWNLOADED             + " INTEGER, "
                + DbStructure.Column.DM_ID                  + " INTEGER, "
                + DbStructure.Column.EID                    + " TEXT, "
                + DbStructure.Column.CHAPTER                + " TEXT, "
                + DbStructure.Column.SECTION                + " TEXT, "
                + DbStructure.Column.DOWNLOADED_ON          + " INTEGER, "
                + DbStructure.Column.LAST_PLAYED_OFFSET     + " INTEGER, "
                + DbStructure.Column.IS_COURSE_ACTIVE       + " BOOLEAN, "
                + DbStructure.Column.UNIT_URL               + " TEXT, "
                + DbStructure.Column.VIDEO_FOR_WEB_ONLY     + " BOOLEAN "
                + ")";
        db.execSQL(sql);

        createAssessmentTable(db);

        logger.debug("Database created");
    }

    private void createAssessmentTable(SQLiteDatabase db){
        String sql = "CREATE TABLE "                        + DbStructure.Table.ASSESSMENT
            + " ("
            + DbStructure.Column.ASSESSMENT_TB_ID       + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + DbStructure.Column.ASSESSMENT_TB_USERNAME + " TEXT, "
            + DbStructure.Column.ASSESSMENT_TB_UNIT_ID  + " TEXT, "
            + DbStructure.Column.ASSESSMENT_TB_UNIT_WATCHED + " BOOLEAN "
            + ")";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            String upgradeToV2 =
                    "ALTER TABLE " + DbStructure.Table.DOWNLOADS + " ADD COLUMN "
                            + DbStructure.Column.UNIT_URL + " TEXT ";

            String[] upgradeToV3 = new String[]{
                    "ALTER TABLE " + DbStructure.Table.DOWNLOADS + " ADD COLUMN "
                            + DbStructure.Column.URL_HIGH_QUALITY + " TEXT ",

                    "ALTER TABLE " + DbStructure.Table.DOWNLOADS + " ADD COLUMN "
                            + DbStructure.Column.URL_LOW_QUALITY + " TEXT ",

                    "ALTER TABLE " + DbStructure.Table.DOWNLOADS + " ADD COLUMN "
                            + DbStructure.Column.URL_YOUTUBE + " TEXT "};

            String upgradeToV4 =
                    "ALTER TABLE " + DbStructure.Table.DOWNLOADS + " ADD COLUMN "
                            + DbStructure.Column.VIDEO_FOR_WEB_ONLY + " BOOLEAN ";

            if (oldVersion == 1) {
                // upgrade from 1 to 2
                db.execSQL(upgradeToV2);
            }

            if (oldVersion < 3) {
                // upgrade to version 3
                for (String query : upgradeToV3) {
                    db.execSQL(query);
                }
            }

            if (oldVersion < 4) {
                // upgrade to version 4
                db.execSQL(upgradeToV4);
            }

            if (oldVersion < 5) {
                createAssessmentTable(db);
            }

            if (oldVersion < 6) {
                final File appExternalDir = FileUtil.getAppExternalDir(context);
                if (appExternalDir != null) {
                    Cursor cursor = db.query(false, DbStructure.Table.DOWNLOADS,
                            new String[]{DbStructure.Column.ID, DbStructure.Column.USERNAME,
                                    DbStructure.Column.FILEPATH}, null, null, null, null, null, null);
                    if (cursor != null) {
                        try {
                            while (cursor.moveToNext()) {
                                final String id = cursor.getString(0);
                                final String username = cursor.getString(1);
                                final String filePath = cursor.getString(2);
                                final String hashedUsername = Sha1Util.SHA1(username);
                                final String newFilePath = filePath.replace(
                                        appExternalDir.getAbsolutePath() + "/" + username,
                                        appExternalDir.getAbsolutePath() + "/"
                                                + AppConstants.Directories.VIDEOS + "/"
                                                + hashedUsername);

                                // First update the directory name along with its whole path
                                final File previousDir = new File(appExternalDir, username);
                                if (previousDir.exists()) {
                                    final File newDir = new File(appExternalDir,
                                            AppConstants.Directories.VIDEOS + "/" + hashedUsername);
                                    newDir.mkdirs();
                                    previousDir.renameTo(newDir);
                                }

                                // Then update the database row
                                final ContentValues updatedValues = new ContentValues();
                                updatedValues.put(DbStructure.Column.USERNAME, hashedUsername);
                                updatedValues.put(DbStructure.Column.FILEPATH, newFilePath);
                                db.update(DbStructure.Table.DOWNLOADS, updatedValues,
                                        DbStructure.Column.ID + "= ?", new String[]{id});
                            }
                        } finally {
                            cursor.close();
                        }

                        // Now migrate the subtitles directory
                        final File previousSrtDir = new File(appExternalDir, "srtFolder");
                        if (previousSrtDir.exists()) {
                            final File newSrtDir = new File(appExternalDir,
                                    AppConstants.Directories.VIDEOS + "/" + AppConstants.Directories.SUBTITLES);
                            newSrtDir.mkdirs();
                            previousSrtDir.renameTo(newSrtDir);
                        }
                    }
                }

                logger.debug("Database upgraded from " + oldVersion + " to " + newVersion);
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Returns singleton writable {@link SQLiteDatabase} object.
     * @return
     */
    public SQLiteDatabase getDatabase() {
        if (sqliteDb == null) {
            sqliteDb = this.getWritableDatabase();
            logger.debug("Writable database handle opened");
        }
        return sqliteDb;
    }

    @Override
    public synchronized void close() {
        super.close();

        sqliteDb = null;
        logger.debug("Database closed");
    }
}

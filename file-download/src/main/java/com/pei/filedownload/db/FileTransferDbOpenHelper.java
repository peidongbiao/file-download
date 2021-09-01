package com.pei.filedownload.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by peidongbiao on 2018/6/23.
 */
public class FileTransferDbOpenHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 3;

    private static final String DB_NAME = "file_transfer.db";

    private static final String SQL_CREATE_DOWNLOAD_TASK =
            "CREATE TABLE IF NOT EXISTS " + FileTransferSchema.DownloadTaskTable.TABLE_NAME + "(" +
                    "SID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    FileTransferSchema.DownloadTaskTable.COLUMN_TASK_ID + " TEXT UNIQUE," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_FILE_NAME + " TEXT," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_URL + " TEXT," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_TARGET + " TEXT," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_CONTENT_TYPE + " TEXT," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_CONTENT_LENGTH + " INTEGER," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_ACCEPT_RANGES + " INTEGER," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_ETAG + " TEXT," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_LAST_MODIFIED + " TEXT," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_STATUS + " INTEGER," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_PROGRESS + " INTEGER," +
                    FileTransferSchema.DownloadTaskTable.COLUMN_HEADERS + " TEXT" +
                    ")";

    private static final String SQL_CREATE_DOWNLOAD_SEGMENT =
            "CREATE TABLE IF NOT EXISTS " + FileTransferSchema.DownloadSegmentTable.TABLE_NAME + "(" +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_TASK_ID + " TEXT, " +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_NUMBER + " INTEGER, " +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_SEGMENT_PATH + " TEXT," +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_TOTAL_LENGTH + " INTEGER," +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_OFFSET + " INTEGER," +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_SEGMENT_LENGTH + " INTEGER," +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_STATUS + " INTEGER, " +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_MD5 + " TEXT," +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_FILE_NAME + " TEXT," +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_URL + " TEXT," +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_TARGET + " TEXT," +
                    FileTransferSchema.DownloadSegmentTable.COLUMN_PROGRESS + " INTEGER," +
                    "PRIMARY KEY(" + FileTransferSchema.DownloadSegmentTable.COLUMN_TASK_ID + "," + FileTransferSchema.DownloadSegmentTable.COLUMN_NUMBER + ")" +
                    ")";

    private static final String SQL_ALTER_DOWNLOAD_TASK_ADD_HEADERS =
            "ALTER TABLE " + FileTransferSchema.DownloadTaskTable.TABLE_NAME + " ADD COLUMN " + FileTransferSchema.DownloadTaskTable.COLUMN_HEADERS + " TEXT";

    public FileTransferDbOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DOWNLOAD_TASK);
        db.execSQL(SQL_CREATE_DOWNLOAD_SEGMENT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1: {
                db.execSQL(SQL_CREATE_DOWNLOAD_TASK);
                db.execSQL(SQL_CREATE_DOWNLOAD_SEGMENT);
            }
            case 2: {
                db.execSQL(SQL_ALTER_DOWNLOAD_TASK_ADD_HEADERS);
            }
        }
    }
}
package com.pei.filedownload.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.IntRange;


import com.pei.filedownload.Segment;
import com.pei.filedownload.Task;
import com.pei.filedownload.task.model.DownloadSegment;
import com.pei.filedownload.task.model.DownloadTaskModel;

/** 数据库操作
 * Created by peidongbiao on 2018/6/23.
 */
public class FileTransferDao {

    private static volatile FileTransferDao sInstance;

    private Context mContext;
    private FileTransferDbOpenHelper mFileTransferDbOpenHelper;

    public static FileTransferDao get(Context context) {
        if(sInstance == null) {
            synchronized (FileTransferDao.class) {
                if (sInstance == null) {
                    sInstance = new FileTransferDao(context);
                }
            }
        }
        return sInstance;
    }

    private FileTransferDao(Context context) {
        this.mContext = context.getApplicationContext();
        mFileTransferDbOpenHelper = new FileTransferDbOpenHelper(mContext);
    }


    public long insertDownloadTask(DownloadTaskModel downloadTask) {
        SQLiteDatabase database = mFileTransferDbOpenHelper.getWritableDatabase();
        ContentValues contentValues = FileTransferSchema.DownloadTaskTable.WRITER.toContentValues(downloadTask);
        long id = database.insertWithOnConflict(FileTransferSchema.DownloadTaskTable.TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        return id;
    }

    public DownloadTaskModel findDownloadTask(String taskId) {
        SQLiteDatabase database = mFileTransferDbOpenHelper.getReadableDatabase();
        Cursor cursor = database.query(FileTransferSchema.DownloadTaskTable.TABLE_NAME, FileTransferSchema.DownloadTaskTable.PROJECTION, FileTransferSchema.DownloadTaskTable.COLUMN_TASK_ID + " = ?", new String[]{taskId},null, null,null);
        if (cursor == null) return null;
        if (cursor.moveToNext()) {
            DownloadTaskModel taskModel = FileTransferSchema.DownloadTaskTable.READER.toEntity(cursor);
            cursor.close();
            return taskModel;
        }
        return null;
    }

    public int updateDownloadTaskProgress(String taskId, @IntRange(from = 0, to = 100) int progress) {
        SQLiteDatabase database = mFileTransferDbOpenHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(FileTransferSchema.DownloadTaskTable.COLUMN_PROGRESS, progress);
        int rows = database.update(FileTransferSchema.DownloadTaskTable.TABLE_NAME, contentValues, FileTransferSchema.DownloadTaskTable.COLUMN_TASK_ID + " = ?", new String[]{taskId});
        return rows;
    }

    public int updateDownloadTaskStatus(String taskId, @Task.TaskStatus int status) {
        SQLiteDatabase database = mFileTransferDbOpenHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(FileTransferSchema.DownloadTaskTable.COLUMN_STATUS, status);
        int rows = database.update(FileTransferSchema.DownloadTaskTable.TABLE_NAME, contentValues, FileTransferSchema.DownloadTaskTable.COLUMN_TASK_ID + " = ?", new String[]{taskId});
        return rows;
    }

    public int deleteDownloadTaskModel(String taskId) {
        SQLiteDatabase database = mFileTransferDbOpenHelper.getWritableDatabase();
        int rows = database.delete(FileTransferSchema.DownloadTaskTable.TABLE_NAME, FileTransferSchema.DownloadTaskTable.COLUMN_TASK_ID + " = ?", new String[]{taskId});
        return rows;
    }

    public long insetDownloadSegment(DownloadSegment segment) {
        SQLiteDatabase database = mFileTransferDbOpenHelper.getWritableDatabase();
        ContentValues contentValues = FileTransferSchema.DownloadSegmentTable.WRITER.toContentValues(segment);
        long id = database.insertWithOnConflict(FileTransferSchema.DownloadSegmentTable.TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
        return id;
    }

    public DownloadSegment findDownloadSegment(String taskId, int number) {
        SQLiteDatabase database = mFileTransferDbOpenHelper.getReadableDatabase();
        Cursor cursor = database.query(FileTransferSchema.DownloadSegmentTable.TABLE_NAME, FileTransferSchema.DownloadSegmentTable.PROJECTION, FileTransferSchema.DownloadSegmentTable.COLUMN_TASK_ID + " = ? and " + FileTransferSchema.DownloadSegmentTable.COLUMN_NUMBER + " = ?",new String[]{taskId, String.valueOf(number)}, null, null  , null);
        if (cursor == null) return null;
        if (cursor.moveToNext()) {
            DownloadSegment segment = FileTransferSchema.DownloadSegmentTable.READER.toEntity(cursor);
            cursor.close();
            return segment;
        }
        return null;
    }

    public int updateDownloadSegmentStatus(DownloadSegment segment, @Segment.Status int status) {
        SQLiteDatabase database = mFileTransferDbOpenHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(FileTransferSchema.DownloadSegmentTable.COLUMN_STATUS, status);
        int result = database.updateWithOnConflict(FileTransferSchema.DownloadSegmentTable.TABLE_NAME, contentValues, FileTransferSchema.DownloadSegmentTable.COLUMN_TASK_ID + " = ? and " + FileTransferSchema.DownloadSegmentTable.COLUMN_NUMBER + " = ?", new String[]{ segment.getTaskId(), String.valueOf(segment.getNumber())}, SQLiteDatabase.CONFLICT_REPLACE);
        return result;
    }

    public int deleteDownloadSegments(String taskId) {
        SQLiteDatabase database = mFileTransferDbOpenHelper.getWritableDatabase();
        int rows = database.delete(FileTransferSchema.DownloadSegmentTable.TABLE_NAME, FileTransferSchema.DownloadSegmentTable.COLUMN_TASK_ID + " = ?", new String[]{taskId});
        return rows;
    }
}

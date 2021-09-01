package com.pei.filedownload.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;


import com.pei.filedownload.task.model.DownloadSegment;
import com.pei.filedownload.task.model.DownloadTaskModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Created by peidongbiao on 2018/6/23.
 */

public interface FileTransferSchema {

    interface DownloadTaskTable {
        String TABLE_NAME = "DOWNLOAD_TASK";

        String COLUMN_TASK_ID = "TASK_ID";
        String COLUMN_FILE_NAME = "FILE_NAME";
        String COLUMN_URL = "URL";
        String COLUMN_TARGET = "TARGET";
        String COLUMN_CONTENT_TYPE = "CONTENT_TYPE";
        String COLUMN_CONTENT_LENGTH = "CONTENT_LENGTH";
        String COLUMN_ACCEPT_RANGES = "ACCEPT_RANGES";
        String COLUMN_ETAG = "ETAG";
        String COLUMN_LAST_MODIFIED = "LAST_MODIFIED";
        String COLUMN_STATUS = "STATUS";
        String COLUMN_PROGRESS = "PROGRESS";
        String COLUMN_HEADERS = "header";

        String[] PROJECTION = {
                COLUMN_TASK_ID,
                COLUMN_FILE_NAME,
                COLUMN_URL,
                COLUMN_TARGET,
                COLUMN_CONTENT_TYPE,
                COLUMN_CONTENT_LENGTH,
                COLUMN_ACCEPT_RANGES,
                COLUMN_ETAG,
                COLUMN_LAST_MODIFIED,
                COLUMN_STATUS,
                COLUMN_PROGRESS,
                COLUMN_HEADERS
        };

        EntityReader<DownloadTaskModel> READER = new EntityReader<DownloadTaskModel>() {
            @Override
            public DownloadTaskModel toEntity(Cursor cursor) {
                DownloadTaskModel taskModel = new DownloadTaskModel();
                taskModel.setTaskId(cursor.getString(cursor.getColumnIndex(DownloadTaskTable.COLUMN_TASK_ID)));
                taskModel.setFileName(cursor.getString(cursor.getColumnIndex(DownloadTaskTable.COLUMN_FILE_NAME)));
                taskModel.setUrl(cursor.getString(cursor.getColumnIndex(DownloadTaskTable.COLUMN_URL)));
                taskModel.setTarget(cursor.getString(cursor.getColumnIndex(DownloadTaskTable.COLUMN_TARGET)));
                taskModel.setContentType(cursor.getString(cursor.getColumnIndex(DownloadTaskTable.COLUMN_CONTENT_TYPE)));
                taskModel.setContentLength(cursor.getLong(cursor.getColumnIndex(DownloadTaskTable.COLUMN_CONTENT_LENGTH)));
                taskModel.setAcceptRanges(cursor.getString(cursor.getColumnIndex(DownloadTaskTable.COLUMN_ACCEPT_RANGES)));
                taskModel.setETag(cursor.getString(cursor.getColumnIndex(DownloadTaskTable.COLUMN_ETAG)));
                taskModel.setLastModified(cursor.getString(cursor.getColumnIndex(DownloadTaskTable.COLUMN_LAST_MODIFIED)));
                taskModel.setStatus(cursor.getInt(cursor.getColumnIndex(DownloadTaskTable.COLUMN_STATUS)));
                taskModel.setProgress(cursor.getInt(cursor.getColumnIndex(DownloadTaskTable.COLUMN_PROGRESS)));

                String headers = cursor.getString(cursor.getColumnIndex(DownloadTaskTable.COLUMN_HEADERS));
                if (!TextUtils.isEmpty(headers)) {
                    try {
                        Map<String, String> map = new HashMap<>();
                        JSONObject object = new JSONObject(headers);
                        Iterator<String> iterator = object.keys();
                        String key;
                        while (iterator.hasNext()) {
                            key = iterator.next();
                            map.put(key, object.getString(key));
                        }
                        taskModel.setHeaders(map);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return taskModel;
            }
        };

        EntityWriter<DownloadTaskModel> WRITER = new EntityWriter<DownloadTaskModel>() {
            @Override
            public ContentValues toContentValues(DownloadTaskModel entity) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(DownloadTaskTable.COLUMN_TASK_ID, entity.getTaskId());
                contentValues.put(DownloadTaskTable.COLUMN_FILE_NAME, entity.getFileName());
                contentValues.put(DownloadTaskTable.COLUMN_URL, entity.getUrl());
                contentValues.put(DownloadTaskTable.COLUMN_TARGET, entity.getTarget());
                contentValues.put(DownloadTaskTable.COLUMN_CONTENT_TYPE, entity.getContentType());
                contentValues.put(DownloadTaskTable.COLUMN_CONTENT_LENGTH, entity.getContentLength());
                contentValues.put(DownloadTaskTable.COLUMN_ACCEPT_RANGES, entity.getAcceptRanges());
                contentValues.put(DownloadTaskTable.COLUMN_ETAG, entity.getETag());
                contentValues.put(DownloadTaskTable.COLUMN_LAST_MODIFIED, entity.getLastModified());
                contentValues.put(DownloadTaskTable.COLUMN_STATUS, entity.getStatus());
                contentValues.put(DownloadTaskTable.COLUMN_PROGRESS, entity.getProgress());
                if (entity.getHeaders() != null) {
                    contentValues.put(DownloadTaskTable.COLUMN_HEADERS, new JSONObject(entity.getHeaders()).toString());
                }
                return contentValues;
            }
        };
    }


    interface DownloadSegmentTable {
        String TABLE_NAME = "DOWNLOAD_SEGMENT";

        String COLUMN_TASK_ID = "TASK_ID";
        String COLUMN_NUMBER = "NUMBER";
        String COLUMN_SEGMENT_PATH = "SEGMENT_PATH";
        String COLUMN_TOTAL_LENGTH = "TOTAL_LENGTH";
        String COLUMN_OFFSET = "SEGMENT_OFFSET";
        String COLUMN_SEGMENT_LENGTH = "SEGMENT_LENGTH";
        String COLUMN_STATUS = "STATUS";
        String COLUMN_MD5 = "MD5";
        String COLUMN_FILE_NAME = "FILE_NAME";
        String COLUMN_URL = "URL";
        String COLUMN_TARGET = "TARGET";
        String COLUMN_PROGRESS = "PROGRESS";

        String[] PROJECTION = {
                COLUMN_TASK_ID,
                COLUMN_NUMBER,
                COLUMN_SEGMENT_PATH,
                COLUMN_TOTAL_LENGTH,
                COLUMN_OFFSET,
                COLUMN_SEGMENT_LENGTH,
                COLUMN_STATUS,
                COLUMN_MD5,
                COLUMN_FILE_NAME,
                COLUMN_URL,
                COLUMN_TARGET,
                COLUMN_PROGRESS
        };

        EntityReader<DownloadSegment> READER = new EntityReader<DownloadSegment>() {
            @Override
            public DownloadSegment toEntity(Cursor cursor) {
                DownloadSegment segment = new DownloadSegment();
                segment.setTaskId(cursor.getString(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_TASK_ID)));
                segment.setNumber(cursor.getInt(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_NUMBER)));
                segment.setSegmentPath(cursor.getString(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_SEGMENT_PATH)));
                segment.setTotalLength(cursor.getLong(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_TOTAL_LENGTH)));
                segment.setOffset(cursor.getLong(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_OFFSET)));
                segment.setSegmentLength(cursor.getLong(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_SEGMENT_LENGTH)));
                segment.setStatus(cursor.getInt(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_STATUS)));
                segment.setMd5(cursor.getString(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_MD5)));
                segment.setFileName(cursor.getString(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_FILE_NAME)));
                segment.setUrl(cursor.getString(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_URL)));
                segment.setTarget(cursor.getString(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_TARGET)));
                segment.setProgress(cursor.getInt(cursor.getColumnIndex(DownloadSegmentTable.COLUMN_PROGRESS)));
                return segment;
            }
        };

        EntityWriter<DownloadSegment> WRITER = new EntityWriter<DownloadSegment>() {
            @Override
            public ContentValues toContentValues(DownloadSegment entity) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(DownloadSegmentTable.COLUMN_TASK_ID, entity.getTaskId());
                contentValues.put(DownloadSegmentTable.COLUMN_NUMBER, entity.getNumber());
                contentValues.put(DownloadSegmentTable.COLUMN_SEGMENT_PATH, entity.getSegmentPath());
                contentValues.put(DownloadSegmentTable.COLUMN_TOTAL_LENGTH, entity.getTotalLength());
                contentValues.put(DownloadSegmentTable.COLUMN_OFFSET, entity.getOffset());
                contentValues.put(DownloadSegmentTable.COLUMN_SEGMENT_LENGTH, entity.getSegmentLength());
                contentValues.put(DownloadSegmentTable.COLUMN_STATUS, entity.getStatus());
                contentValues.put(DownloadSegmentTable.COLUMN_MD5, entity.getMd5());
                contentValues.put(DownloadSegmentTable.COLUMN_FILE_NAME, entity.getFileName());
                contentValues.put(DownloadSegmentTable.COLUMN_URL, entity.getUrl());
                contentValues.put(DownloadSegmentTable.COLUMN_TARGET, entity.getTarget());
                contentValues.put(DownloadSegmentTable.COLUMN_PROGRESS, entity.getProgress());
                return contentValues;
            }
        };
    }
}
package com.pei.filedownload.db;

import android.database.Cursor;

/**
 * Created by peidongbiao on 2018/6/23.
 */
public interface EntityReader<T> {

    T toEntity(Cursor cursor);
}

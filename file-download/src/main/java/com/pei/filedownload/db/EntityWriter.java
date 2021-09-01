package com.pei.filedownload.db;

import android.content.ContentValues;

/**
 * Created by peidongbiao on 2018/6/23.
 */
public interface EntityWriter<T> {

    ContentValues toContentValues(T entity);
}

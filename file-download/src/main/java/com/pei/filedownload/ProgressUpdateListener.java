package com.pei.filedownload;

/** 进度更新
 * Created by peidongbiao on 2018/6/20.
 */

public interface ProgressUpdateListener {

    void onProgressUpdate(long totalLength, long currentLength, long update, int percent);
}

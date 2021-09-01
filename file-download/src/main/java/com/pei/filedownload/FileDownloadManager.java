package com.pei.filedownload;

import android.content.Context;
import android.text.TextUtils;

import com.pei.filedownload.task.DownloadRequest;
import com.pei.filedownload.task.DownloadTask;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class FileDownloadManager {

    private static final int MAX_PARALLEL_DOWNLOAD_NUM = 3;
    private static FileDownloadManager sDefault;

    private Context mContext;
    private OkHttpClient mOkHttpClient;
    private TaskDispatcher mTaskDispatcher;

    public static FileDownloadManager getDefault(Context context) {
        if (sDefault == null) {
            synchronized (FileDownloadManager.class) {
                if (sDefault == null) {
                    TaskDispatcher taskDispatcher = new TaskDispatcher();
                    taskDispatcher.setMaxRunningTaskNum(MAX_PARALLEL_DOWNLOAD_NUM);
                    sDefault = new FileDownloadManager(context, createOkHttpClient(), taskDispatcher);
                }
            }
        }
        return sDefault;
    }


    private static OkHttpClient createOkHttpClient() {
        return new OkHttpClient.Builder()
                //.callTimeout(10, TimeUnit.MINUTES)
                .connectTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .build();
    }

    public FileDownloadManager(Context context, OkHttpClient okHttpClient, TaskDispatcher taskDispatcher) {
        mContext = context.getApplicationContext();
        mOkHttpClient = okHttpClient;
        mTaskDispatcher = taskDispatcher;
    }

    public DownloadRequest.Builder create(String url) {
        DownloadRequest.Builder builder = new DownloadRequest.Builder(this);
        builder.setUrl(url);
        return builder;
    }

    public DownloadTask start(DownloadRequest request) {
        FLog.i("start download: " + request);
        String id = makeDownloadTaskId(request);
        Task<?> task = mTaskDispatcher.findTask(id);
        if (task instanceof DownloadTask) {
            DownloadTask downloadTask = (DownloadTask) task;
            Task.Callback<File> callback = request.getCallback();
            if (downloadTask.getStatus() == Task.STATUS_RUNNING && callback != null) {
                callback.onStart();
            }
            downloadTask.addCallback(callback);
            return downloadTask;
        }

        DownloadTask downloadTask = new DownloadTask(this, request);
        downloadTask.addCallback(request.getCallback());
        downloadTask.setStatus(Task.STATUS_ENQUEUE);
        mTaskDispatcher.submit(downloadTask, false);
        return downloadTask;
    }

    private String makeDownloadTaskId(DownloadRequest request) {
        if (!TextUtils.isEmpty(request.getRequestId())) {
            return request.getRequestId();
        }
        String id = Utils.getStringMd5(request.getUrl());
        request.setRequestId(id);
        return id;
    }

    public boolean pause(String taskId) {
        return mTaskDispatcher.pause(taskId);
    }

    public boolean cancel(String taskId) {
        return mTaskDispatcher.cancel(taskId);
    }

    public Context getContext() {
        return mContext;
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public TaskDispatcher getTaskDispatcher() {
        return mTaskDispatcher;
    }
}
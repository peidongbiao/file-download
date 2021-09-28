package com.pei.filedownload.task;

import android.content.Context;

import androidx.annotation.NonNull;

import com.pei.filedownload.Task;
import com.pei.filedownload.TaskDispatcher;
import com.pei.filedownload.db.FileTransferDao;
import com.pei.filedownload.task.model.ResourceInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 完整下载整个文件，不分片
 */
class CompleteDownloadTask extends Task<File> {

    private DownloadRequest mDownloadRequest;
    private ResourceInfo mInfo;
    private FileTransferDao mDao;
    private String mTaskId;
    private File mTargetFile;
    private Call mCall;

    public CompleteDownloadTask(Context context, OkHttpClient okHttpClient, TaskDispatcher taskDispatcher, DownloadRequest request, @NonNull ResourceInfo info, @NonNull String taskId) {
        super(okHttpClient, taskDispatcher);
        this.mDownloadRequest = request;
        this.mInfo = info;
        this.mDao = FileTransferDao.get(context);
        this.mTaskId = taskId;
    }

    @Override
    public void run() {
        try {
            setStatus(Task.STATUS_RUNNING);
            mTargetFile = new File(mDownloadRequest.getTarget());
            if (mTargetFile.exists()) {
                mTargetFile.delete();
            } else {
                mTargetFile.getParentFile().mkdirs();
                mTargetFile.createNewFile();
            }
            onStart();

            doDownload(mDownloadRequest, mDownloadRequest.getUrl(), mTargetFile);
            int status = getStatus();
            if (status == Task.STATUS_COMPLETE) {
                onComplete(mTargetFile);
            } else if (status == Task.STATUS_CANCELED) {
                onFailure(new Exception("Canceled"));
            } else if (status == Task.STATUS_PAUSED) {

            }
        } catch (Exception e) {
            setStatus(Task.STATUS_FAILED);
            onFailure(e);
        }
    }

    private void doDownload(DownloadRequest request, String url, File targetFile) throws IOException {
        Request.Builder builder = new Request.Builder()
                .get()
                .url(url);
//                .addHeader(HttpManager.HEADER_KEY_CONNECT_TIMEOUT, String.valueOf(DownloadTask.SEGMENT_DOWNLOAD_TIMEOUT))
//                .addHeader(HttpManager.HEADER_KEY_WRITE_TIMEOUT, String.valueOf(DownloadTask.SEGMENT_DOWNLOAD_TIMEOUT))
//                .addHeader(HttpManager.HEADER_KEY_READ_TIMEOUT, String.valueOf(DownloadTask.SEGMENT_DOWNLOAD_TIMEOUT))
//                //日志拦截器不记录body
//                .addHeader(HttpFileLogInterceptor.HEADER_IGNORE_RESPONSE_BODY, "true");
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        mCall = mOkHttpClient.newCall(builder.build());
        Response response = mCall.execute();

        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(targetFile));
        BufferedInputStream bis = new BufferedInputStream(response.body().byteStream());
        byte[] buffer = new byte[4096];
        long length = 0;
        long update = 0;
        long percent = 0;
        int read;
        while ((read = bis.read(buffer)) != -1 && getStatus() == Task.STATUS_RUNNING) {
            bos.write(buffer, 0, read);
            length += read;
            update += read;
            int currentPercent = (int) (length * 100 / mInfo.getContentLength());
            if (currentPercent - percent >= 1) {
                Progress progress = Progress.obtain();
                progress.setTotal(mInfo.getContentLength());
                progress.setCurrent(length);
                progress.setPercent(currentPercent);
                progress.setUpdate(update);
                percent = currentPercent;
                update = 0;
                onProgressChanged(progress);
            }
        }
        bos.flush();
        bos.close();
        bis.close();
        if (getStatus() == Task.STATUS_RUNNING) {
            setStatus(Task.STATUS_COMPLETE);
            mDao.updateDownloadTaskProgress(mTaskId, 100);
            //mDao.updateDownloadTaskStatus(mTaskId, Task.STATUS_COMPLETE);
        }
    }

    /**
     * 完整下载不可暂停
     */
    @Override
    public void pause() {

    }

    @Override
    public void cancel() {
        setStatus(Task.STATUS_CANCELED);

        if (mCall != null) {
            mCall.cancel();
        }

        mDao.updateDownloadTaskStatus(mTaskId, Task.STATUS_CANCELED);
        if (mTargetFile != null) {
            mTargetFile.delete();
        }
    }
}
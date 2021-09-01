package com.pei.filedownload.task;


import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.pei.filedownload.FLog;
import com.pei.filedownload.FileDownloadManager;
import com.pei.filedownload.FileSplitter;
import com.pei.filedownload.MainThreadCallback;
import com.pei.filedownload.SizeSplitter;
import com.pei.filedownload.Task;
import com.pei.filedownload.Utils;
import com.pei.filedownload.db.FileTransferDao;
import com.pei.filedownload.exception.DownloadException;
import com.pei.filedownload.task.model.DownloadSegment;
import com.pei.filedownload.task.model.DownloadTaskModel;
import com.pei.filedownload.task.model.ResourceInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 文件下载任务，可以自动判断是否分片下载
 */
public class DownloadTask extends Task<File> {
    private static final String TAG = "DownloadTask";

    public static final int SEGMENT_DOWNLOAD_TIMEOUT = 10 * 60; //下载分片超时时间

    protected static final String DOWNLOAD_SUFFIX = "-dld";
    private static final int SEGMENT_SIZE = 5 * 1024 * 1024; //5M
    private static final int DEFAULT_PARALLEL_TASK_NUMBER = 2;

    private FileDownloadManager mFileDownloadManager;
    private Context mContext;
    private DownloadRequest mRequest;
    private SegmentDownloadCallback mDownloadCallback;
    private FileTransferDao mDao;
    private List<SegmentDownloadTask> mRunningSegmentTasks;
    private String mTaskId;
    private DownloadTaskModel mDownloadTaskModel;
    private Exception mException;
    private CompleteDownloadTask mCompleteDownloadTask;
    private int mParallelNum = DEFAULT_PARALLEL_TASK_NUMBER;
    private Call mGetResourceInfoCall;

    public DownloadTask(FileDownloadManager fileDownloadManager, DownloadRequest request) {
        super(fileDownloadManager.getOkHttpClient(), fileDownloadManager.getTaskDispatcher());
        this.mFileDownloadManager = fileDownloadManager;
        this.mContext = fileDownloadManager.getContext();
        this.mRequest = request;
        this.mDao = FileTransferDao.get(mContext);
        this.mTaskId = request.getRequestId();
        this.mPriority = request.getPriority();

        if (mRequest.getParallelNum() > 0) {
            mParallelNum = mRequest.getParallelNum();
        }
    }

    @Override
    public void run() {
        setStatus(Task.STATUS_RUNNING);

        final File targetFile = new File(mRequest.getTarget());
        ResourceInfo info;

        try {
            onStart();

            DownloadTaskModel downloadTaskModel = mDao.findDownloadTask(mTaskId);
            info = getResourceInfo(mRequest, mRequest.getUrl());
            boolean changed = isResourceChanged(info, downloadTaskModel);
            FLog.i("changed: " + changed);
            if (downloadTaskModel == null || changed) {
                //文件发生了改变，清除本地数据
                clearLocalDownloadTaskData(mTaskId);
                //插入一条新的数据
                mDownloadTaskModel = new DownloadTaskModel(mTaskId, mRequest, info);
                mDao.insertDownloadTask(mDownloadTaskModel);
            } else if (downloadTaskModel.getStatus() == Task.STATUS_COMPLETE && targetFile.length() == info.getContentLength()) {
                //已经下载完成，直接成功
                Progress progress = Progress.complete(targetFile.length());
                onProgressChanged(progress);

                onComplete(targetFile);
                return;
            } else if (downloadTaskModel.getStatus() == Task.STATUS_COMPLETE && !targetFile.exists()) {
                //下载成功，但是文件被删除
                clearLocalDownloadTaskData(mTaskId);
            } else if (downloadTaskModel.getStatus() == Task.STATUS_PAUSED) {
                mDownloadTaskModel = downloadTaskModel;
            } else if (downloadTaskModel.getStatus() == Task.STATUS_FAILED) {

            }
        } catch (Exception e) {
            onFailure(e);
            return;
        }

        //不分片
        if (!info.acceptRanges()) {
            mCompleteDownloadTask = new CompleteDownloadTask(mContext,mOkHttpClient, mTaskDispatcher, mRequest, info, mTaskId);
            mCompleteDownloadTask.addCallback(new SimpleCallback<File>() {
                //onStart已经回调过了

                @Override
                public void onProgressChange(Progress progress) {
                    DownloadTask.this.onProgressChanged(Progress.copy(progress));
                }

                @Override
                public void onComplete(File result) {
                    DownloadTask.this.onComplete(result);
                }

                @Override
                public void onFailure(Exception exception) {
                    DownloadTask.this.onFailure(exception);
                }
            });
            mCompleteDownloadTask.setStatus(Task.STATUS_ENQUEUE);
            mTaskDispatcher.submit(mCompleteDownloadTask, true);
            return;
        }

        mRunningSegmentTasks = Collections.synchronizedList(new ArrayList<SegmentDownloadTask>());
        mDownloadCallback = new SegmentDownloadCallback(info.getContentLength());

        FileSplitter<DownloadSegment> splitter = new SizeSplitter<>(SEGMENT_SIZE, new SizeSplitter.SegmentCreator<DownloadSegment>() {
            @Override
            public DownloadSegment create(long totalLength, int number, long offset, long length) {
                DownloadSegment segment = new DownloadSegment();
                segment.setUrl(mRequest.getUrl());
                segment.setTarget(mRequest.getTarget());
                segment.setTargetFile(targetFile);
                segment.setFileName(mRequest.getFileName());
                segment.setTaskId(mTaskId);
                return segment;
            }
        });
        List<DownloadSegment> segments = splitter.split(info.getContentLength());

        try {
            Queue<DownloadSegment> queue = new LinkedList<>(segments);

            while (!queue.isEmpty() && getStatus() == Task.STATUS_RUNNING) {
                List<DownloadSegment> list = pollElements(queue, mParallelNum);
                if (list.isEmpty()) continue;
                CountDownLatch countDownLatch = new CountDownLatch(list.size());
                for (DownloadSegment segment : list) {
                    SegmentDownloadTask task = new SegmentDownloadTask(mFileDownloadManager, mRequest, segment, countDownLatch);
                    task.addCallback(mDownloadCallback);
                    task.setStatus(Task.STATUS_ENQUEUE);
                    mTaskDispatcher.submit(task, true);
                    mRunningSegmentTasks.add(task);
                }
                countDownLatch.await();
                mRunningSegmentTasks.clear();
            }
            int status = getStatus();
            if (status == Task.STATUS_RUNNING) {
                if (checkDownloadSegments(segments)) {
                    File file = mergeSegmentFiles(targetFile, segments);
                    if (file.length() == info.getContentLength()) {
                        Utils.deleteDir(new File(mRequest.getTarget() + DOWNLOAD_SUFFIX));
                        mDao.deleteDownloadSegments(mTaskId);
                        onComplete(targetFile);
                    } else {
                        DownloadException exception = new DownloadException("Merge segment files failed");
                        onFailure(exception);
                    }
                } else {
                    DownloadException exception = new DownloadException("Check segment files failed");
                    onFailure(exception);
                }
            } else if (status == Task.STATUS_FAILED) {
                Exception exception = mException != null ? mException : new Exception("Download failed");
                onFailure(exception);
            } else if (status == Task.STATUS_PAUSED) {
                onPause();
            }
        } catch (Exception e) {
            FLog.e("", e);
            mRunningSegmentTasks.clear();
            onFailure(e);
        }
    }


    private ResourceInfo getResourceInfo(DownloadRequest request, String url) throws IOException {
        ResponseBody body;
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();

        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        mGetResourceInfoCall = mOkHttpClient.newCall(builder.build());
        Response response = mGetResourceInfoCall.execute();
        ResourceInfo info = new ResourceInfo();
        info.setUrl(url);
        body = response.body();
        info.setContentType(response.header("Content-Type"));
        info.setContentLength(Long.parseLong(response.header("Content-Length", "0")));
        info.setAcceptRanges(response.header("Accept-Ranges"));
        info.setETag(response.header("eTag"));
        info.setLastModified(response.header("Last-Modified"));
        if (body != null) {
            body.close();
        }
        return info;
    }

    private boolean checkDownloadSegments(List<DownloadSegment> segments) {
        if (segments == null) return false;
        for (DownloadSegment segment : segments) {
            if (segment.getSegmentFile().length() != segment.getSegmentLength()) {
                return false;
            }
        }
        return true;
    }

    private File mergeSegmentFiles(File file, List<DownloadSegment> segments) throws IOException {
        if (file.exists()) {
            file.delete();
        } else {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(0);
        byte[] buffer = new byte[4096];
        for (int i = 0; i < segments.size(); i++) {
            DownloadSegment segment = segments.get(i);
            File segmentFile = segment.getSegmentFile();
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(segmentFile));
            int read;
            while ((read = bis.read(buffer)) != -1) {
                raf.write(buffer, 0, read);
            }
            bis.close();
            segmentFile.delete();

            FLog.i("mergeSegmentFiles no: " + segment.getNumber() + " complete");
        }
        raf.close();
        return file;
    }

    private boolean isResourceChanged(ResourceInfo info, DownloadTaskModel taskModel) {
        if (taskModel == null) return true;
        return !(Objects.equals(info.getETag(), taskModel.getETag()) && Objects.equals(info.getLastModified(), taskModel.getLastModified()));
    }

    private void clearLocalDownloadTaskData(String taskId) {
        mDao.deleteDownloadTaskModel(taskId);
        mDao.deleteDownloadSegments(taskId);
        Utils.deleteDir(new File(mRequest.getTarget() + DOWNLOAD_SUFFIX));
    }

    @Override
    public void pause() {
        if (getStatus() == Task.STATUS_COMPLETE || getStatus() == Task.STATUS_FAILED) {
            return;
        }

        setStatus(Task.STATUS_PAUSED);

        if (mGetResourceInfoCall != null) {
            mGetResourceInfoCall.cancel();
        }

        if (mCompleteDownloadTask != null) {
            mCompleteDownloadTask.pause();
            return;
        }

        if (mRunningSegmentTasks != null) {
            for (int i = 0; i < mRunningSegmentTasks.size(); i++) {
                mRunningSegmentTasks.get(i).pause();
            }
        }

        if (mTaskId != null) {
            mDao.updateDownloadTaskStatus(mTaskId, Task.STATUS_PAUSED);
        }

        if (getStatus() == Task.STATUS_ENQUEUE) {
            onPause();
        }

        mTaskDispatcher.finishTask(this);
    }

    @Override
    public void cancel() {
        if (getStatus() == Task.STATUS_COMPLETE || getStatus() == Task.STATUS_FAILED) {
            return;
        }

//        if (mCompleteDownloadTask != null) {
//            mCompleteDownloadTask.cancel();
//            return;
//        }

        if (mRunningSegmentTasks != null) {
            for (int i = 0; i < mRunningSegmentTasks.size(); i++) {
                mRunningSegmentTasks.get(i).cancel();
            }
        }

        if (mTaskId != null) {
            clearLocalDownloadTaskData(mTaskId);
        }
        setStatus(Task.STATUS_CANCELED);
        if (mTaskId != null) {
            mDao.updateDownloadTaskStatus(mTaskId, Task.STATUS_CANCELED);
        }

        if (getStatus() == Task.STATUS_ENQUEUE) {
            onFailure(new Exception("Canceled"));
        }

        mTaskDispatcher.finishTask(this);
    }

    @Override
    public void addCallback(Callback<File> callback) {
        Callback<File> mainThreadCallback = new MainThreadCallback<>(callback);
        super.addCallback(mainThreadCallback);
    }

    @Override
    protected void onComplete(@NonNull File result) {
        setStatus(Task.STATUS_COMPLETE);
        mDao.updateDownloadTaskStatus(mTaskId, Task.STATUS_COMPLETE);
        super.onComplete(result);
        mTaskDispatcher.finishTask(this);
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        FLog.e("Download failed", e);
        setStatus(Task.STATUS_FAILED);
        mDao.updateDownloadTaskStatus(mTaskId, Task.STATUS_FAILED);
        super.onFailure(e);
        mTaskDispatcher.finishTask(this);
    }


    @Override
    public void setStatus(int status) {
        super.setStatus(status);
        FLog.i("file: " + mRequest.getUrl() + ", status: " + status);
    }

    /**
     * 从队列中取出num个元素
     *
     * @param queue
     * @param num
     * @return
     */
    private <T> List<T> pollElements(Queue<T> queue, int num) {
        if (queue.isEmpty()) return Collections.emptyList();

        List<T> list = new ArrayList<>();
        while (num > 0) {
            T element = queue.poll();
            if (element != null) {
                list.add(element);
            }
            num--;
        }
        return list;
    }

    public String getTaskId() {
        return mTaskId;
    }

    private class SegmentDownloadCallback extends SimpleCallback<DownloadSegment> {

        private long fileTotalLength;
        private long currentLength;
        private int totalPercent;

        public SegmentDownloadCallback(long fileTotalLength) {
            this.fileTotalLength = fileTotalLength;
        }

        @Override
        public synchronized void onProgressChange(Progress progress) {
            currentLength += progress.getUpdate();
            long currentPercent = currentLength * 100 / fileTotalLength;

            if (mDownloadTaskModel != null && currentPercent <= mDownloadTaskModel.getProgress()) {
                return;
            }

            if (currentPercent - totalPercent >= 1) {
                totalPercent = (int) currentPercent;
                //更新数据库进度
                mDao.updateDownloadTaskProgress(mTaskId, totalPercent);

                FLog.i("onProgressChange, update: " + progress.getUpdate() + ", percent: " + currentPercent);

                Progress totalProgress = Progress.acquireProgress();
                totalProgress.setTotal(fileTotalLength);
                totalProgress.setUpdate(progress.getUpdate());
                totalProgress.setCurrent(currentLength);
                totalProgress.setPercent(totalPercent);
                DownloadTask.this.onProgressChanged(totalProgress);
            }
        }

        @Override
        public void onFailure(Exception exception) {
            mException = exception;
            setStatus(Task.STATUS_FAILED);
        }
    }
}
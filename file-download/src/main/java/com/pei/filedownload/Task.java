package com.pei.filedownload;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.util.Pools;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;


/**
 * 任务基类
 * Created by peidongbiao on 2018/6/20.
 */
public abstract class Task<T> implements Runnable, Comparable<Task<?>> {

    public static final int STATUS_INIT = 0;
    public static final int STATUS_ENQUEUE = 1;
    public static final int STATUS_RUNNING = 2;
    public static final int STATUS_PAUSED = 3;
    public static final int STATUS_CANCELED = 4;
    public static final int STATUS_COMPLETE = 5;
    public static final int STATUS_FAILED = 6;


    @IntDef({STATUS_INIT, STATUS_ENQUEUE, STATUS_RUNNING, STATUS_PAUSED, STATUS_CANCELED, STATUS_COMPLETE, STATUS_FAILED})
    public @interface TaskStatus {}



    private String mTaskId;
    private List<Callback<T>> mCallbacks;
    protected TaskDispatcher mTaskDispatcher;
    protected OkHttpClient mOkHttpClient;

    protected int mPriority = 0;

    private AtomicInteger mStatus;

    public static String statusToString(@TaskStatus int status) {
        switch (status) {
            case STATUS_INIT: return "INIT";
            case STATUS_ENQUEUE: return "ENQUEUE";
            case STATUS_RUNNING: return "RUNNING";
            case STATUS_PAUSED: return  "PAUSED";
            case STATUS_CANCELED: return "CANCELED";
            case STATUS_COMPLETE: return  "COMPLETE";
            case STATUS_FAILED: return "FAILED";
        }
        return "UNKNOWN_STATUS";
    }

    public Task(OkHttpClient okHttpClient, TaskDispatcher taskDispatcher) {
        mOkHttpClient = okHttpClient;
        mTaskDispatcher = taskDispatcher;
        mStatus = new AtomicInteger(STATUS_INIT);
    }

    @WorkerThread
    @Override
    public abstract void run();

    protected void onStart() {
        if (mCallbacks == null) return;
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onStart();
        }
    }

    protected void onProgressChanged(@NonNull Progress progress) {
        if (mCallbacks == null) return;
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onProgressChange(progress);
        }
        Progress.release(progress);
    }

    protected void onPause() {
        if (mCallbacks == null) return;
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onPause();
        }
    }

    protected void onComplete(@NonNull T result) {
        if (mCallbacks == null) return;
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onComplete(result);
        }
    }

    protected void onFailure(@NonNull Exception e) {
        if (mCallbacks == null) return;
        for (int i = 0; i < mCallbacks.size(); i++) {
            mCallbacks.get(i).onFailure(e);
        }
    }


    public void addCallback(Callback<T> callback) {
        if (mCallbacks == null) {
            mCallbacks = new ArrayList<>();
        }
        mCallbacks.add(callback);
    }

    public List<Callback<T>> getCallbacks() {
        return mCallbacks;
    }

    @TaskStatus
    public int getStatus() {
        return mStatus.get();
    }

    public void setStatus(@TaskStatus int status) {
        mStatus.set(status);
    }

    public String getTaskId() {
        return mTaskId;
    }

    public void setTaskId(String taskId) {
        mTaskId = taskId;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int priority) {
        this.mPriority = priority;
    }

    @Override
    public int compareTo(Task<?> task) {
        return this.mPriority - task.mPriority;
    }

    public abstract void pause();

    public abstract void cancel();

    public interface Callback<T> {

        void onStart();

        void onProgressChange(Progress progress);

        void onPause();

        void onComplete(T result);

        void onFailure(Exception exception);
    }

    public abstract static class SimpleCallback<T> implements Callback<T> {

        @Override
        public void onStart() {}

        @Override
        public void onProgressChange(Progress progress) {}

        @Override
        public void onPause() {}

        @Override
        public void onComplete(T result) {}

        @Override
        public void onFailure(Exception exception) {}
    }

    public static class Progress {

        private static final Pools.Pool<Progress> sProgressPool = new Pools.SynchronizedPool<>(256);

        private long total;
        private long current;
        private long update;
        private int percent;

        public static Progress complete(long length) {
            Progress progress = obtain();
            progress.setTotal(length);
            progress.setCurrent(length);
            progress.setUpdate(length);
            progress.setPercent(100);
            return progress;
        }
        
        public static Progress copy(Progress progress) {
            Progress p = obtain();
            p.setTotal(progress.getTotal());
            p.setCurrent(progress.getCurrent());
            p.setUpdate(progress.getUpdate());
            p.setPercent(progress.getPercent());
            return p;
        }

        private Progress() {

        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getCurrent() {
            return current;
        }

        public void setCurrent(long current) {
            this.current = current;
        }

        public int getPercent() {
            return percent;
        }

        public void setPercent(int percent) {
            this.percent = percent;
        }

        public long getUpdate() {
            return update;
        }

        public void setUpdate(long update) {
            this.update = update;
        }

        public static Progress obtain() {
            Progress progress = sProgressPool.acquire();
            progress = progress == null ? new Progress() : progress;
            return progress;
        }

        public static void release(Progress progress) {
            progress.current = 0;
            progress.percent = 0;
            progress.total = 0;
            progress.update = 0;
            sProgressPool.release(progress);
        }
    }
}
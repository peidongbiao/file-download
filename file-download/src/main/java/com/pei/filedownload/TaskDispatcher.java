package com.pei.filedownload;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by peidongbiao on 2018/6/20.
 */
public class TaskDispatcher {
    private static final String TAG = "TaskManager";
    private static final int DEFAULT_MAX_TASK_NUM = 5;

    private ExecutorService mExecutor;
    private Queue<Task<?>> mRunningTasks;
    private Queue<Task<?>> mReadyTasks;
    private int mMaxRunningTaskNum = DEFAULT_MAX_TASK_NUM;

    public TaskDispatcher() {
        mExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            private AtomicInteger number = new AtomicInteger();
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("TaskManager-thread-" + number.getAndIncrement());
                thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        Log.e(TAG, "uncaughtException,thread: " + t.getName(), e);
                    }
                });
                return thread;
            }
        });

        mRunningTasks = new ArrayDeque<>();
        mReadyTasks = new PriorityQueue<>(64, new Comparator<Task<?>>() {
            @Override
            public int compare(Task<?> task1, Task<?> task2) {
                return -task1.compareTo(task2);
            }
        });
    }

    public synchronized void submit(Task<?> task, boolean silent) {
        if (silent) {
            mExecutor.submit(task);
            return;
        }
        mReadyTasks.offer(task);
        promoteAndExecute();
    }

    public synchronized void promoteAndExecute() {
        List<Task<?>> executableTasks = new ArrayList<>();
        Iterator<Task<?>> iterator = mReadyTasks.iterator();
        while (iterator.hasNext()) {
            Task<?> task = iterator.next();
            if (mRunningTasks.size() >= mMaxRunningTaskNum) break;
            iterator.remove();
            executableTasks.add(task);
            mRunningTasks.add(task);
        }

        for (int i = 0; i < executableTasks.size(); i++) {
            mExecutor.submit(executableTasks.get(i));
        }
    }

    public synchronized Task<?> findTask(@NonNull String taskId) {
        for (Task<?> task : mReadyTasks) {
            if (Objects.equals(taskId, task.getTaskId())) {
                return task;
            }
        }

        for (Task<?> task : mRunningTasks) {
            if (Objects.equals(taskId, task.getTaskId())) {
                return task;
            }
        }

        return null;
    }

    public synchronized void finishTask(Task<?> task) {
        mReadyTasks.remove(task);
        mRunningTasks.remove(task);
        promoteAndExecute();
    }

    public synchronized boolean pause(String taskId) {
        Task<?> task = findTask(taskId);
        if (task != null) {
            task.pause();
            return true;
        }
        return false;
    }

    public synchronized boolean cancel(String taskId) {
        Task<?> task = findTask(taskId);
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    public synchronized int getMaxRunningTaskNum() {
        return mMaxRunningTaskNum;
    }

    public synchronized void setMaxRunningTaskNum(int maxRunningTaskNum) {
        mMaxRunningTaskNum = maxRunningTaskNum;
    }

    public synchronized List<Task<?>> readyTasks() {
        List<Task<?>> readyTasks = new ArrayList<>(mReadyTasks);
        return Collections.unmodifiableList(readyTasks);
    }

    public synchronized List<Task<?>> RunningTasks() {
        List<Task<?>> runningTasks = new ArrayList<>(mRunningTasks);
        return Collections.unmodifiableList(runningTasks);
    }
}
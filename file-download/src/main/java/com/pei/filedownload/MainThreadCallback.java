package com.pei.filedownload;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class MainThreadCallback<T> implements Task.Callback<T>, Handler.Callback {

    private static final int CALLBACK_START = 1;
    private static final int CALLBACK_PROGRESS = 2;
    private static final int CALLBACK_PAUSE = 3;
    private static final int CALLBACK_COMPLETE = 4;
    private static final int CALLBACK_FAILURE = 5;


    private Task.Callback<T> mCallback;
    private Handler mHandler;

    public MainThreadCallback(Task.Callback<T> callback) {
        mCallback = callback;
        mHandler = new Handler(Looper.getMainLooper(), this);
    }

    @Override
    public void onStart() {
        if (mCallback == null) return;
        postCallbackMessage(CALLBACK_START, null);
    }

    @Override
    public void onProgressChange(Task.Progress progress) {
        if (mCallback == null) return;
        Task.Progress p = Task.Progress.copy(progress);
        postCallbackMessage(CALLBACK_PROGRESS, p);
    }

    @Override
    public void onPause() {
        if (mCallback == null) return;
        postCallbackMessage(CALLBACK_PAUSE, null);
    }

    @Override
    public void onComplete(T result) {
        if (mCallback == null) return;
        postCallbackMessage(CALLBACK_COMPLETE, result);
    }

    @Override
    public void onFailure(Exception exception) {
        if (mCallback == null) return;
        postCallbackMessage(CALLBACK_FAILURE, exception);
    }

    private void postCallbackMessage(int type, Object obj) {
        Message message = mHandler.obtainMessage();
        message.what = type;
        message.obj = obj;
        message.sendToTarget();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        if (mCallback == null) return true;
        switch (msg.what) {
            case CALLBACK_START: {
                mCallback.onStart();
                break;
            }
            case CALLBACK_PROGRESS: {
                Task.Progress progress = (Task.Progress) msg.obj;
                mCallback.onProgressChange(progress);
                Task.Progress.release(progress);
                break;
            }

            case CALLBACK_PAUSE: {
                mCallback.onPause();
                break;
            }
            case CALLBACK_COMPLETE: {
                T result = (T) msg.obj;
                mCallback.onComplete(result);
                break;
            }
            case CALLBACK_FAILURE: {
                Exception exception = (Exception) msg.obj;
                mCallback.onFailure(exception);
            }
        }
        return true;
    }
}
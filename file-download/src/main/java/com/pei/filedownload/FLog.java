package com.pei.filedownload;

import android.util.Log;

public class FLog {
    private static final String TAG = "FileTransfer";
    private static boolean sLoggable = true;

    public static void showLog(boolean loggable) {
        sLoggable = loggable;
    }

    public static void i(String msg) {
        if (sLoggable && msg != null) {
            Log.i(TAG, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (sLoggable && msg != null) {
            Log.i(tag, msg);
        }
    }

    public static void w(String msg) {
        if (sLoggable && msg != null) {
            Log.w(TAG, msg);
        }
    }

    public static void e(String msg) {
        if (msg != null)
            Log.e(TAG, msg);
    }

    public static void e(String msg, Throwable tr) {
        Log.e(TAG, msg, tr);
    }
}
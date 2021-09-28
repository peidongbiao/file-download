package com.pei.filedownload.task;

import android.util.Log;

import androidx.annotation.Keep;

import com.pei.filedownload.FileDownloadManager;
import com.pei.filedownload.Task;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Keep
public class DownloadRequest {
    private static final String TAG = "DownloadRequest";

    private String url;
    private String target;
    private String fileName;
    private String requestId;
    private Task.Callback<File> callback;
    private Map<String, String> headers;
    private int parallelNum;
    private int priority;
    private boolean noSplit;

    public DownloadRequest() {

    }

    public DownloadRequest(Builder builder) {
        this.fileName = builder.fileName;
        this.url = builder.url;
        this.target = builder.target;
        this.requestId = builder.requestId;
        this.callback = builder.mCallback;
        this.headers = builder.headers;
        this.parallelNum = builder.parallelNum;
        this.priority = builder.priority;
        this.noSplit = builder.noSplit;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Task.Callback<File> getCallback() {
        return callback;
    }

    public void setCallback(Task.Callback<File> callback) {
        this.callback = callback;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public int getParallelNum() {
        return parallelNum;
    }

    public void setParallelNum(int parallelNum) {
        this.parallelNum = parallelNum;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isNoSplit() {
        return noSplit;
    }

    public void setNoSplit(boolean noSplit) {
        this.noSplit = noSplit;
    }

    @Override
    public String toString() {
        return "DownloadRequest{" +
                "url='" + url + '\'' +
                ", target='" + target + '\'' +
                ", fileName='" + fileName + '\'' +
                ", requestId='" + requestId + '\'' +
                ", callback=" + callback +
                ", headers=" + headers +
                ", parallelNum=" + parallelNum +
                ", priority=" + priority +
                '}';
    }

    public static class Builder {

        private FileDownloadManager mFileDownloadManager;
        private String fileName;
        private String url;
        private String target;
        private Task.Callback<File> mCallback;
        private String requestId;
        private Map<String, String> headers;
        private int parallelNum;
        private int priority;
        private boolean noSplit;

        public Builder() {
        }

        public Builder(FileDownloadManager fileDownloadManager) {
            this.mFileDownloadManager = fileDownloadManager;
        }

        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setTarget(String target) {
            this.target = target;
            return this;
        }

        public Builder setCallback(Task.Callback<File> callback) {
            mCallback = callback;
            return this;
        }

        public String getRequestId() {
            return requestId;
        }

        public Builder setRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder setHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder addHeader(String key, String value) {
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.put(key, value);
            return this;
        }

        public int getParallelNum() {
            return parallelNum;
        }

        public Builder setParallelNum(int parallelNum) {
            if (parallelNum <= 0) {
                Log.w(TAG, "setParallelNum: " + parallelNum, new IllegalArgumentException("ParallelNum should larger than 0"));
                return this;
            }
            this.parallelNum = parallelNum;
            return this;
        }

        public int getPriority() {
            return priority;
        }

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public boolean isNoSplit() {
            return noSplit;
        }

        public Builder setNoSplit(boolean noSplit) {
            this.noSplit = noSplit;
            return this;
        }

        public DownloadRequest build() {
            if (headers == null) {
                headers = Collections.emptyMap();
            }
            return new DownloadRequest(this);
        }

        public DownloadTask start() {
            if (mFileDownloadManager == null) throw new NullPointerException("start method require fileDownload is not null!");
            DownloadRequest request = build();
            return mFileDownloadManager.start(request);
        }
    }
}
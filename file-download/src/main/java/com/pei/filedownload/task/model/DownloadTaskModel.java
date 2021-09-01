package com.pei.filedownload.task.model;

import androidx.annotation.Keep;

import com.pei.filedownload.task.DownloadRequest;

import java.util.Map;

/**
 * 下载任务数据库模型
 */
@Keep
public class DownloadTaskModel {

    private String taskId;
    private String fileName;
    private String url;
    private String target;
    private String contentType;
    private long contentLength;
    private String acceptRanges;
    private String eTag;
    private String lastModified;
    private int status;
    private int progress;
    private Map<String, String> headers;

    public DownloadTaskModel() {

    }

    public DownloadTaskModel(String taskId, DownloadRequest request, ResourceInfo info) {
        this.taskId = taskId;
        this.fileName = request.getFileName();
        this.url = request.getUrl();
        this.target = request.getTarget();
        this.contentType = info.getContentType();
        this.contentLength = info.getContentLength();
        this.acceptRanges = info.getAcceptRanges();
        this.eTag = info.getETag();
        this.lastModified = info.getLastModified();
        this.headers = request.getHeaders();
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getAcceptRanges() {
        return acceptRanges;
    }

    public void setAcceptRanges(String acceptRanges) {
        this.acceptRanges = acceptRanges;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}

package com.pei.filedownload.task.model;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.pei.filedownload.Segment;

import java.io.File;
import java.util.Objects;

@Keep
public class DownloadSegment extends Segment {

    private String taskId;
    private String fileName;
    private String url;
    private String target;
    private File targetFile;
    private File segmentFile;
    private int progress;

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

    public File getSegmentFile() {
        return segmentFile;
    }

    public void setSegmentFile(File segmentFile) {
        this.segmentFile = segmentFile;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }


    public File getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getTaskId(), this.getUrl(), this.getNumber());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof DownloadSegment)) return false;
        DownloadSegment other = (DownloadSegment) obj;
        return Objects.equals(this.getTaskId(), other.getTaskId()) &&
                Objects.equals(this.getUrl(), other.getUrl()) &&
                Objects.equals(this.getNumber(), other.getNumber());
    }
}

package com.pei.filedownload;

import androidx.annotation.IntDef;
import androidx.annotation.Keep;

/**
 * 分片
 * Created by peidongbiao on 2018/6/20.
 */
@Keep
public class Segment {

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_COMPLETE = 1;
    public static final int STATUS_FAILED = 2;

    @IntDef({STATUS_RUNNING, STATUS_COMPLETE, STATUS_FAILED})
    public @interface Status{}

    private String segmentPath;

    private int number;

    private long totalLength;

    private long offset;

    private long segmentLength;

    @Status
    private int status;

    private String md5;

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }


    public String getSegmentPath() {
        return segmentPath;
    }

    public void setSegmentPath(String segmentPath) {
        this.segmentPath = segmentPath;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(long totalLength) {
        this.totalLength = totalLength;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getSegmentLength() {
        return segmentLength;
    }

    public void setSegmentLength(long segmentLength) {
        this.segmentLength = segmentLength;
    }

    @Status
    public int getStatus() {
        return status;
    }

    public void setStatus(@Status int status) {
        this.status = status;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }
}
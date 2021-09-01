package com.pei.filedownload;

import java.util.List;

/**
 * 分片策略
 */
public interface FileSplitter<T extends Segment> {

    List<T> split(long totalLength);
}
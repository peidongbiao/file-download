package com.pei.filedownload;

import java.util.ArrayList;
import java.util.List;

/**
 * 按大小分片
 */
public class SizeSplitter<T extends Segment> implements FileSplitter<T> {
    private static final String TAG = "SizeSplitter";

    private final long size;
    private SegmentCreator<T> creator;

    public SizeSplitter(long splitSize, SegmentCreator<T> creator) {
        this.size = splitSize;
        this.creator = creator;
    }

    @Override
    public List<T> split(long totalLength) {
        long mod = totalLength % size;
        long num = totalLength / size;
        num = mod == 0 ? num : num + 1;

        List<T> list = new ArrayList<>();
        long currentOffset = 0;
        for (int i = 0; i < num; i++) {
            long segmentLength = Math.min(totalLength - currentOffset, size);
            T segment = creator.create(totalLength, i, currentOffset, segmentLength);
            segment.setTotalLength(totalLength);
            segment.setOffset(currentOffset);
            segment.setSegmentLength(segmentLength);
            segment.setNumber(i);
            currentOffset += segmentLength;
            list.add(segment);
        }
        return list;
    }

    public interface SegmentCreator<T extends Segment> {

        T create(long totalLength, int number, long offset, long length);
    }
}
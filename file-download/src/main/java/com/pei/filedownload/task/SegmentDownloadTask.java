package com.pei.filedownload.task;

import androidx.annotation.NonNull;

import com.pei.filedownload.FLog;
import com.pei.filedownload.FileDownloadManager;
import com.pei.filedownload.Segment;
import com.pei.filedownload.Task;
import com.pei.filedownload.db.FileTransferDao;
import com.pei.filedownload.exception.DownloadException;
import com.pei.filedownload.task.model.DownloadSegment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

public class SegmentDownloadTask extends Task<DownloadSegment> {

    private FileDownloadManager mFileDownloadManager;
    private DownloadRequest mRequest;
    private DownloadSegment mSegment;
    private CountDownLatch mCountDownLatch;
    private FileTransferDao mDao;

    public SegmentDownloadTask(FileDownloadManager fileDownloadManager, DownloadRequest request, DownloadSegment segment, CountDownLatch countDownLatch) {
        super(fileDownloadManager.getOkHttpClient(), fileDownloadManager.getTaskDispatcher());
        this.mFileDownloadManager = fileDownloadManager;
        this.mRequest = request;
        this.mSegment = segment;
        this.mCountDownLatch = countDownLatch;
        mDao = FileTransferDao.get(fileDownloadManager.getContext());
    }

    @Override
    public void run() {
        setStatus(Task.STATUS_RUNNING);
        mSegment.setStatus(Segment.STATUS_RUNNING);

        File segmentFile = new File(mSegment.getTarget() + DownloadTask.DOWNLOAD_SUFFIX,  mSegment.getTargetFile().getName() + "-" + mSegment.getNumber());

        try {
            onStart();
            DownloadSegment localSegment = mDao.findDownloadSegment(mSegment.getTaskId(), mSegment.getNumber());

            if (mSegment.equals(localSegment)) {
                if (localSegment.getStatus() == Segment.STATUS_COMPLETE) {
                    //当前分片已经下载完成
                    if (segmentFile.length() == mSegment.getSegmentLength()) {
                        mSegment.setSegmentFile(segmentFile);
                        mSegment.setSegmentPath(segmentFile.getPath());

                        Progress progress = Progress.complete(mSegment.getSegmentLength());
                        onProgressChanged(progress);

                        onComplete(mSegment);
                        return;
                    } else {
                        segmentFile.delete();
                    }
                } else {
                    //canceled, paused, failed
                }
            } else if (segmentFile.exists()) {
                segmentFile.delete();
            }

            if (!segmentFile.exists()) {
                segmentFile.getParentFile().mkdirs();
                segmentFile.createNewFile();
            }
            mSegment.setSegmentFile(segmentFile);
            mSegment.setSegmentPath(segmentFile.getPath());

            mDao.insetDownloadSegment(mSegment);

            doDownload(mRequest, segmentFile);

            int status = getStatus();
            if (status == Task.STATUS_RUNNING) {
                long length = segmentFile.length();
                if (mSegment.getSegmentLength() == length) {
                    onComplete(mSegment);
                } else {
                    String error = "Download segment " + mSegment.getNumber() + " failed, expect length: " + length + ", actual: " + segmentFile.length();
                    FLog.e(error);
                    onFailure(new DownloadException(error));
                }
            } else if (status == Task.STATUS_CANCELED) {
                onFailure(new DownloadException("Canceled"));
            } else if (status == Task.STATUS_PAUSED) {
                onPause();
            }
        } catch (Exception e) {
            FLog.e("Segment " + mSegment.getNumber() + " download failed", e);
            onFailure(e);
        } finally {
            mCountDownLatch.countDown();
        }
    }

    private void doDownload(DownloadRequest request, File segmentFile) throws IOException {
        long downloadedLength = segmentFile.length();
        long segmentLength = mSegment.getSegmentLength();
        //FLog.i("doDownload, segment no: " + mSegment.getNumber() + ", segment length: " + segmentLength + ", downloadedLength: " + downloadedLength);
        Request.Builder builder = new Request.Builder()
                .get()
                .url(mSegment.getUrl())
                .addHeader("RANGE", "bytes=" + (mSegment.getOffset() + downloadedLength) + "-" + (mSegment.getOffset() + mSegment.getSegmentLength() - 1));
        if (request.getHeaders() != null) {
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Call call = mOkHttpClient.newCall(builder.build());
        Response response = call.execute();

        RandomAccessFile raf = new RandomAccessFile(segmentFile, "rw");
        raf.seek(downloadedLength);
        BufferedInputStream bis = new BufferedInputStream(response.body().byteStream());
        byte[] buffer = new byte[4096];
        long length = downloadedLength;
        long percent = downloadedLength * 100 / segmentLength;
        long update = mSegment.isLocalSizeUpdated() ? 0 : downloadedLength;
        int read;
        while ((read = bis.read(buffer)) != -1 && getStatus() == Task.STATUS_RUNNING) {
            raf.write(buffer, 0, read);
            length += read;
            update += read;
            int currentPercent = (int) (length * 100 / segmentLength);
            if (currentPercent - percent >= 1) {
                Progress progress = Progress.obtain();
                progress.setTotal(segmentLength);
                progress.setCurrent(length);
                progress.setPercent(currentPercent);
                progress.setUpdate(update);
                onProgressChanged(progress);
                percent = currentPercent;
                update = 0;

                if (!mSegment.isLocalSizeUpdated()) {
                    mSegment.setLocalSizeUpdated(true);
                }
            }
        }
        raf.close();
        bis.close();

        FLog.i("doDownload, segment no: " + mSegment.getNumber() + ", segment length: " + segmentLength + ", file length: " + segmentFile.length());
    }

    @Override
    protected void onComplete(@NonNull DownloadSegment result) {
        setStatus(Task.STATUS_COMPLETE);
        mSegment.setStatus(Segment.STATUS_COMPLETE);
        mDao.updateDownloadSegmentStatus(mSegment, Segment.STATUS_COMPLETE);
        super.onComplete(result);
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        setStatus(Task.STATUS_FAILED);
        mSegment.setStatus(Segment.STATUS_FAILED);
        mDao.updateDownloadSegmentStatus(mSegment, Segment.STATUS_FAILED);
        super.onFailure(e);
    }

    @Override
    protected void onPause() {
        setStatus(Task.STATUS_PAUSED);
        mSegment.setStatus(Segment.STATUS_FAILED);
        mDao.updateDownloadSegmentStatus(mSegment, Segment.STATUS_FAILED);
        super.onPause();
    }

    @Override
    public void pause() {
        setStatus(Task.STATUS_PAUSED);
    }

    @Override
    public void cancel() {
        setStatus(Task.STATUS_CANCELED);
    }
}

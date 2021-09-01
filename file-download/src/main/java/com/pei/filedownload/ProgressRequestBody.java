package com.pei.filedownload;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;

/**
 * 监听上传进度
 * Created by peidongbiao on 2018/6/20.
 */
public class ProgressRequestBody extends RequestBody {
    private static final String TAG = "ProgressRequestBody";

    private RequestBody mRequestBody;
    private ProgressUpdateListener mProgressUpdateListener;
    private long mTotalLength = -1;

    public ProgressRequestBody(RequestBody requestBody, ProgressUpdateListener listener) {
        mRequestBody = requestBody;
        mProgressUpdateListener = listener;
    }

    @Override
    public MediaType contentType() {
        return mRequestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return mRequestBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (mTotalLength == -1) {
            mTotalLength = contentLength();
        }
        ForwardingSink forwardingSink = new ForwardingSink(sink) {

            private long mCurrentLength;
            private int mPercent;
            private long mUpdate;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                mCurrentLength += byteCount;
                mUpdate += byteCount;
                int percent = (int) (mCurrentLength * 100 / mTotalLength);
                if(percent - mPercent >= 1) {
                    mPercent = percent;
                    mProgressUpdateListener.onProgressUpdate(mTotalLength, mCurrentLength, mUpdate, percent);
                    mUpdate = 0;
                }
            }
        };
        BufferedSink bufferedSink = Okio.buffer(forwardingSink);
        mRequestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    public RequestBody getRequestBody() {
        return mRequestBody;
    }
}
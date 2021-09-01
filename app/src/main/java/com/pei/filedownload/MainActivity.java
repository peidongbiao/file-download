package com.pei.filedownload;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.pei.filedownload.databinding.ActivityMainBinding;
import com.pei.filedownload.task.DownloadTask;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_EXTERNAL_STORAGE = 1;

    private ActivityMainBinding mBinding;
    private DownloadTask mDownloadTask;
    private File mDownloadDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mDownloadDir = new File(getExternalCacheDir(), "download");
        mDownloadDir.mkdirs();

        mBinding.btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = mBinding.etDownloadUrl.getText().toString();
                download(url);
            }
        });

        mBinding.btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDownloadTask != null) {
                    mDownloadTask.pause();
                }
            }
        });

        mBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDownloadTask != null) {
                    mDownloadTask.cancel();
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
        }
    }

    private void download(String url) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_EXTERNAL_STORAGE);
            return;
        }

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, "Download url is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mDownloadTask != null && mDownloadTask.getStatus() == Task.STATUS_RUNNING) {
            mDownloadTask.cancel();
            return;
        }

        mDownloadTask = FileDownloadManager.getDefault(this)
                .create(url)
                .setTarget(new File(mDownloadDir.getPath(), Utils.parseFileName(url)).toString())
                //.setFileName(Utils.parseFileName(url))
                .setCallback(new Task.Callback<File>() {
                    @Override
                    public void onStart() {
                        Log.d(TAG, "onStart: ");
                    }

                    @Override
                    public void onProgressChange(Task.Progress progress) {
                        mBinding.progress.setProgress(progress.getPercent());
                    }

                    @Override
                    public void onPause() {
                        Log.d(TAG, "onPause: ");
                        Toast.makeText(MainActivity.this, "pause", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete(File result) {
                        Log.d(TAG, "onComplete: ");
                        Toast.makeText(MainActivity.this, "complete", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception exception) {
                        Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .start();
    }
}
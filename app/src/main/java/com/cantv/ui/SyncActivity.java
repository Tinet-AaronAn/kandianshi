package com.cantv.ui;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cantv.R;
import com.cantv.model.SyncManager;

/**
 * 同步界面 - 从COS下载视频到本地
 * 大字体、简洁进度显示、老人友好
 */
public class SyncActivity extends Activity {

    private TextView tvTitle;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private TextView tvProgressPercent;
    private TextView tvDetail;
    private Button btnCancel;

    private SyncManager syncManager;
    private volatile boolean syncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        fullscreen();

        tvTitle = findViewById(R.id.tv_sync_title);
        tvStatus = findViewById(R.id.tv_sync_status);
        progressBar = findViewById(R.id.progress_bar);
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        tvDetail = findViewById(R.id.tv_sync_detail);
        btnCancel = findViewById(R.id.btn_cancel);

        syncManager = new SyncManager();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (syncing) {
                    syncManager.cancel();
                    btnCancel.setText("返回");
                    btnCancel.setEnabled(false);
                    tvStatus.setText("正在取消...");
                } else {
                    finish();
                }
            }
        });

        // 自动开始同步
        startSync();
    }

    private void startSync() {
        syncing = true;
        btnCancel.setText("取消");

        new Thread(new Runnable() {
            @Override
            public void run() {
                syncManager.sync(new SyncManager.ProgressCallback() {
                    @Override
                    public void onProgress(final SyncManager.SyncResult result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateProgress(result);
                            }
                        });
                    }

                    @Override
                    public void onComplete(final SyncManager.SyncResult result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateComplete(result);
                            }
                        });
                    }

                    @Override
                    public void onError(final String message) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateError(message);
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void updateProgress(SyncManager.SyncResult result) {
        int progress = result.getProgress();
        progressBar.setProgress(progress);
        tvProgressPercent.setText(progress + "%");

        if (result.currentFile != null) {
            tvStatus.setText("正在下载: " + result.currentFile);
        }

        String detail = String.format("已下载 %d / %d 个文件\n%s / %s",
            result.downloadedFiles, result.totalFiles,
            SyncManager.formatSize(result.downloadedBytes),
            SyncManager.formatSize(result.totalBytes));
        tvDetail.setText(detail);
    }

    private void updateComplete(SyncManager.SyncResult result) {
        syncing = false;

        if (result.cancelled) {
            tvTitle.setText("📡 同步已取消");
            tvStatus.setText("已取消同步");
            tvStatus.setTextColor(0xFFFFA726);
        } else if (result.isComplete()) {
            tvTitle.setText("📡 同步完成！");
            tvStatus.setText("所有视频已同步完成");
            tvStatus.setTextColor(0xFF66BB6A);
        } else {
            tvTitle.setText("📡 同步完成");
            tvStatus.setText(String.format("已完成 %d / %d 个文件",
                result.downloadedFiles, result.totalFiles));
            tvStatus.setTextColor(0xFFFFA726);
        }

        progressBar.setProgress(100);
        tvProgressPercent.setText("100%");

        String detail = String.format("下载了 %d 个文件，共 %s",
            result.downloadedFiles,
            SyncManager.formatSize(result.downloadedBytes));
        tvDetail.setText(detail);

        btnCancel.setText("返回");
        btnCancel.setEnabled(true);
    }

    private void updateError(String message) {
        syncing = false;
        tvTitle.setText("📡 同步失败");
        tvStatus.setText(message);
        tvStatus.setTextColor(0xFFEF5350);
        progressBar.setProgress(0);
        tvProgressPercent.setText("0%");
        tvDetail.setText("请检查网络连接后重试");
        btnCancel.setText("返回");
        btnCancel.setEnabled(true);
    }

    @Override
    public void onBackPressed() {
        if (syncing) {
            syncManager.cancel();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fullscreen();
    }

    private void fullscreen() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }
}

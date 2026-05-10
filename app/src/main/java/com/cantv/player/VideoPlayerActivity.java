package com.cantv.player;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import com.cantv.R;

import java.io.File;
import java.util.Locale;

/**
 * 视频播放器 - 适老化大按钮设计
 * 支持：播放进度记忆、大按钮控制、3秒自动隐藏
 */
public class VideoPlayerActivity extends Activity {

    private VideoView videoView;
    private View touchOverlay;
    private LinearLayout controlsBar;
    private ImageView btnPrev;
    private ImageView btnPlayPause;
    private ImageView btnNext;
    private ImageView btnBack;
    private SeekBar seekBar;
    private TextView tvTime;
    private TextView tvTitle;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable hideControlsRunnable;
    private Runnable updateProgressRunnable;

    private static final int SKIP_MS = 30000;
    private static final int HIDE_DELAY_MS = 3000;
    private static final String PREFS_NAME = "play_progress";
    private static final String PROGRESS_KEY = "pos_";

    private boolean isSeeking = false;
    private String videoPath;
    private String seriesName;
    private String episodeTitle;
    private boolean hasSeekToSavedPosition = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        fullscreen();

        videoPath = getIntent().getStringExtra("video_path");
        seriesName = getIntent().getStringExtra("series_name");
        episodeTitle = getIntent().getStringExtra("episode_title");

        initViews();
        initControls();
        startPlayback();
    }

    private void initViews() {
        videoView = findViewById(R.id.video_view);
        touchOverlay = findViewById(R.id.touch_overlay);
        controlsBar = findViewById(R.id.controls_bar);
        btnPrev = findViewById(R.id.btn_prev);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);
        btnBack = findViewById(R.id.btn_back);
        seekBar = findViewById(R.id.seek_bar);
        tvTime = findViewById(R.id.tv_time);
        tvTitle = findViewById(R.id.tv_video_title);

        tvTitle.setText(seriesName + " - " + episodeTitle);

        hideControlsRunnable = new Runnable() {
            @Override
            public void run() {
                hideControls();
            }
        };

        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (videoView != null && videoView.isPlaying() && !isSeeking) {
                    int current = videoView.getCurrentPosition();
                    int duration = videoView.getDuration();
                    seekBar.setMax(duration);
                    seekBar.setProgress(current);
                    tvTime.setText(formatTime(current) + " / " + formatTime(duration));
                }
                handler.postDelayed(this, 500);
            }
        };
    }

    private void initControls() {
        // 透明覆盖层接收点击（替代 VideoView.onClick，更可靠）
        touchOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleControls();
            }
        });

        // 播放/暂停
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                } else {
                    videoView.start();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                }
                resetHideTimer();
            }
        });

        // 后退30秒
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = videoView.getCurrentPosition() - SKIP_MS;
                videoView.seekTo(Math.max(0, pos));
                resetHideTimer();
            }
        });

        // 前进30秒
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = videoView.getCurrentPosition() + SKIP_MS;
                int dur = videoView.getDuration();
                videoView.seekTo(Math.min(pos, dur));
                resetHideTimer();
            }
        });

        // 返回
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProgress();
                finish();
            }
        });

        // 进度条
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    tvTime.setText(formatTime(progress) + " / " + formatTime(videoView.getDuration()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                isSeeking = true;
                handler.removeCallbacks(hideControlsRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                isSeeking = false;
                videoView.seekTo(sb.getProgress());
                resetHideTimer();
            }
        });

        // 播放错误处理
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                // 播放出错时自动尝试重新加载
                android.util.Log.e("VideoPlayer", "播放错误 what=" + what + " extra=" + extra);
                return true; // 返回true表示已处理，不触发onCompletion
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                btnPlayPause.setImageResource(R.drawable.ic_play);
                clearProgress(); // 播完清除进度
                showControls();
            }
        });

        // 准备完成
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                seekBar.setMax(mp.getDuration());
                handler.post(updateProgressRunnable);

                // 恢复上次播放进度
                if (!hasSeekToSavedPosition) {
                    int savedPos = getSavedProgress();
                    if (savedPos > 0 && savedPos < mp.getDuration() - 5000) {
                        videoView.seekTo(savedPos);
                    }
                    hasSeekToSavedPosition = true;
                }

                // 准备完成后自动开始播放
                videoView.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            }
        });
    }

    private void startPlayback() {
        if (videoPath != null && new File(videoPath).exists()) {
            videoView.setVideoPath(videoPath);
            // 不在这里调用start()，等onPrepared回调里自动播放
            // 这样避免prepare还没完成就start导致黑屏
            showControls();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            toggleControls();
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        saveProgress();
        super.onBackPressed();
    }

    private void toggleControls() {
        if (controlsBar.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        controlsBar.setVisibility(View.VISIBLE);
        tvTitle.setVisibility(View.VISIBLE);
        resetHideTimer();
    }

    private void hideControls() {
        controlsBar.setVisibility(View.GONE);
        tvTitle.setVisibility(View.GONE);
    }

    private void resetHideTimer() {
        handler.removeCallbacks(hideControlsRunnable);
        handler.postDelayed(hideControlsRunnable, HIDE_DELAY_MS);
    }

    // ========== 播放进度记忆 ==========

    private void saveProgress() {
        if (videoView != null && videoPath != null) {
            int pos = videoView.getCurrentPosition();
            int dur = videoView.getDuration();
            // 只保存未看完的进度（进度 > 5秒 且 < 95%）
            if (pos > 5000 && dur > 0 && pos < dur * 0.95) {
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putInt(PROGRESS_KEY + videoPath.hashCode(), pos)
                    .apply();
            } else {
                clearProgress();
            }
        }
    }

    private int getSavedProgress() {
        if (videoPath != null) {
            return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(PROGRESS_KEY + videoPath.hashCode(), 0);
        }
        return 0;
    }

    private void clearProgress() {
        if (videoPath != null) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .remove(PROGRESS_KEY + videoPath.hashCode())
                .apply();
        }
    }

    // ========== 工具方法 ==========

    private String formatTime(int ms) {
        int seconds = ms / 1000;
        int m = seconds / 60;
        int s = seconds % 60;
        int h = m / 60;
        m = m % 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveProgress();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Activity进入后台后，VideoView可能被系统释放
        // 保存进度以便onResume恢复
        saveProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fullscreen();
        // 从后台返回时恢复播放
        if (videoView != null && videoPath != null && !videoView.isPlaying()) {
            // VideoView 暂停后恢复：如果还在prepared状态，直接start
            // 如果状态丢失（黑屏），需要重新setVideoPath
            try {
                if (videoView.getDuration() > 0) {
                    // 还有有效的duration，说明media player还活着
                    int savedPos = getSavedProgress();
                    if (savedPos > 0 && savedPos < videoView.getDuration() - 5000) {
                        videoView.seekTo(savedPos);
                    }
                    videoView.start();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                } else {
                    // duration=0 说明player已释放，需要重新加载
                    startPlayback();
                }
            } catch (Exception e) {
                // IllegalStateException: player已被释放
                startPlayback();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) fullscreen();
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

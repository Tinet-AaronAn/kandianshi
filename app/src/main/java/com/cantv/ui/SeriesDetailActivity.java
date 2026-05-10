package com.cantv.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.cantv.R;
import com.cantv.model.Episode;
import com.cantv.model.Series;
import com.cantv.model.VideoScanner;
import com.cantv.player.VideoPlayerActivity;

import java.util.List;

/**
 * 剧集详情 - 选集界面
 */
public class SeriesDetailActivity extends Activity {

    private GridView gridView;
    private TextView tvTitle;
    private TextView tvEmpty;
    private EpisodeAdapter adapter;
    private String seriesName;
    private String seriesPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series_detail);
        fullscreen();

        seriesName = getIntent().getStringExtra("series_name");
        seriesPath = getIntent().getStringExtra("series_path");

        tvTitle = findViewById(R.id.tv_series_title);
        gridView = findViewById(R.id.grid_episodes);
        tvEmpty = findViewById(R.id.tv_empty_episodes);

        tvTitle.setText(seriesName);

        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadEpisodes();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Episode episode = adapter.getItem(position);
                Intent intent = new Intent(SeriesDetailActivity.this, VideoPlayerActivity.class);
                intent.putExtra("video_path", episode.getVideoFile().getAbsolutePath());
                intent.putExtra("series_name", episode.getSeriesName());
                intent.putExtra("episode_index", episode.getIndex());
                intent.putExtra("episode_title", episode.getDisplayTitle());
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fullscreen();
    }

    private void loadEpisodes() {
        Series series = new Series(seriesName, seriesPath);
        List<Episode> episodes = VideoScanner.scanEpisodes(series);

        if (episodes.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
            adapter = new EpisodeAdapter(this, episodes);
            gridView.setAdapter(adapter);
        }
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

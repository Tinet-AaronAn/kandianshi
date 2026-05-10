package com.cantv.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;
import android.net.Uri;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

import com.cantv.R;
import com.cantv.model.Series;
import com.cantv.model.VideoScanner;

import java.io.File;
import java.util.List;

/**
 * 主界面 - 电视剧列表
 */
public class MainActivity extends Activity {

    private static final int REQUEST_PERMISSION = 100;

    private GridView gridView;
    private TextView tvEmpty;
    private TextView tvTitle;
    private View btnSync;
    private SeriesAdapter adapter;
    private boolean hasPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fullscreen();

        gridView = findViewById(R.id.grid_series);
        tvEmpty = findViewById(R.id.tv_empty);
        tvTitle = findViewById(R.id.tv_title);
        btnSync = findViewById(R.id.btn_sync);

        // 同步按钮
        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSync();
            }
        });

        if (checkPermission()) {
            hasPermission = true;
            loadSeries();
        } else {
            requestStoragePermission();
        }

        gridView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Series series = adapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, SeriesDetailActivity.class);
                intent.putExtra("series_name", series.getName());
                intent.putExtra("series_path", series.getFolderPath());
                startActivity(intent);
            }
        });

        // 空状态提示点击：重新检查权限
        tvEmpty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkPermission()) {
                    requestStoragePermission();
                } else {
                    hasPermission = true;
                    loadSeries();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fullscreen();
        if (checkPermission()) {
            if (!hasPermission) {
                hasPermission = true;
            }
            // 每次回来都刷新列表（同步后可能有新视频）
            loadSeries();
        } else {
            showPermissionHint();
        }
    }

    private void loadSeries() {
        File root = VideoScanner.getRootDir();
        if (!root.exists()) {
            root.mkdirs();
        }

        List<Series> seriesList = VideoScanner.scanAll();
        if (seriesList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("还没放电视剧哦\n\n请把视频文件放到：\n" + root.getAbsolutePath() + "/剧名/\n\n例如：\n" + root.getAbsolutePath() + "/乡村爱情故事/第01集.mp4");
            gridView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
            adapter = new SeriesAdapter(this, seriesList);
            gridView.setAdapter(adapter);
        }
    }

    private void showPermissionHint() {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("需要存储权限才能读取视频\n\n👇 点击这里授予权限");
        gridView.setVisibility(View.GONE);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30) {
            // Android 11+: 跳转到"所有文件访问权限"页面
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "请授予\"所有文件访问\"权限", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                // fallback 到应用设置页
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                startActivity(intent);
                Toast.makeText(this, "请在设置中授予文件访问权限", Toast.LENGTH_LONG).show();
            }
        } else {
            requestPermissions(
                new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true;
                loadSeries();
            } else {
                showPermissionHint();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // 主界面直接退出
        finish();
    }

    /**
     * 启动同步
     */
    private void startSync() {
        Intent intent = new Intent(MainActivity.this, SyncActivity.class);
        startActivity(intent);
    }

    /**
     * 获取 manifest URL（COS签名URL，由SyncManager内部处理）
     */
    @SuppressWarnings("unused")
    private String getManifestUrl() {
        return null; // COS模式下不需要外部URL
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

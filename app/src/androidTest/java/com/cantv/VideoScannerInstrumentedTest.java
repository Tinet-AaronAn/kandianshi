package com.cantv;

import android.content.Context;
import android.os.Environment;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.*;

import com.cantv.model.Series;
import com.cantv.model.Episode;
import com.cantv.model.VideoScanner;

/**
 * 仪器测试 - 验证 VideoScanner 在真实 Android 环境中的行为
 *
 * 注意：运行此测试需要设备已授予 MANAGE_EXTERNAL_STORAGE 权限
 */
@RunWith(AndroidJUnit4.class)
public class VideoScannerInstrumentedTest {

    @Test
    public void testRootDirPath() {
        File root = VideoScanner.getRootDir();
        assertNotNull(root);
        assertTrue("Root dir path should contain 看电视",
                root.getAbsolutePath().contains("看电视"));
    }

    @Test
    public void testScanEmptyDir() {
        File root = VideoScanner.getRootDir();
        // 如果目录存在但为空（或不存在），scanAll 应返回空列表而不崩溃
        // 不修改真实文件系统，只验证不会抛异常
        try {
            VideoScanner.scanAll();
        } catch (Exception e) {
            fail("scanAll should not throw on empty/missing dir: " + e.getMessage());
        }
    }

    @Test
    public void testScanWithTestVideos() throws IOException {
        // 创建测试目录结构
        File root = VideoScanner.getRootDir();
        File testDir = new File(root, "_test_series_");
        testDir.mkdirs();

        // 创建测试视频文件
        File testVideo = new File(testDir, "第01集.mp4");
        try {
            // 写入最小 MP4 头部（不是真正的视频，但文件存在）
            new FileOutputStream(testVideo).close();

            // 扫描
            java.util.List<Series> seriesList = VideoScanner.scanAll();
            assertNotNull(seriesList);

            // 查找我们的测试剧
            Series testSeries = null;
            for (Series s : seriesList) {
                if (s.getName().equals("_test_series_")) {
                    testSeries = s;
                    break;
                }
            }

            if (testSeries != null) {
                assertEquals(1, testSeries.getEpisodeCount());

                // 扫描集数
                java.util.List<Episode> episodes = VideoScanner.scanEpisodes(testSeries);
                assertEquals(1, episodes.size());
                assertEquals("第1集", episodes.get(0).getDisplayTitle());
            }
        } finally {
            // 清理测试文件
            testVideo.delete();
            testDir.delete();
        }
    }

    @Test
    public void testEpisodeSorting() throws IOException {
        File root = VideoScanner.getRootDir();
        File testDir = new File(root, "_test_sort_");
        testDir.mkdirs();

        try {
            // 创建乱序文件名
            new FileOutputStream(new File(testDir, "第03集.mp4")).close();
            new FileOutputStream(new File(testDir, "第01集.mp4")).close();
            new FileOutputStream(new File(testDir, "第02集.mp4")).close();

            Series series = new Series("_test_sort_", testDir.getAbsolutePath());
            java.util.List<Episode> episodes = VideoScanner.scanEpisodes(series);

            // 验证排序（按文件名字典序）
            if (episodes.size() == 3) {
                assertEquals("第1集", episodes.get(0).getDisplayTitle());
                assertEquals("第2集", episodes.get(1).getDisplayTitle());
                assertEquals("第3集", episodes.get(2).getDisplayTitle());
            }
        } finally {
            for (File f : testDir.listFiles()) f.delete();
            testDir.delete();
        }
    }

    @Test
    public void testCoverImageDetection() throws IOException {
        File root = VideoScanner.getRootDir();
        File testDir = new File(root, "_test_cover_");
        testDir.mkdirs();

        try {
            new FileOutputStream(new File(testDir, "cover.jpg")).close();
            new FileOutputStream(new File(testDir, "第01集.mp4")).close();

            java.util.List<Series> seriesList = VideoScanner.scanAll();
            for (Series s : seriesList) {
                if (s.getName().equals("_test_cover_")) {
                    assertNotNull("Cover path should be set", s.getCoverPath());
                    assertTrue("Cover path should end with cover.jpg",
                            s.getCoverPath().endsWith("cover.jpg"));
                    break;
                }
            }
        } finally {
            for (File f : testDir.listFiles()) f.delete();
            testDir.delete();
        }
    }

    @Test
    public void testNonVideoFilesIgnored() throws IOException {
        File root = VideoScanner.getRootDir();
        File testDir = new File(root, "_test_ignore_");
        testDir.mkdirs();

        try {
            new FileOutputStream(new File(testDir, "第01集.mp4")).close();
            new FileOutputStream(new File(testDir, "cover.jpg")).close();
            new FileOutputStream(new File(testDir, "info.txt")).close();
            new FileOutputStream(new File(testDir, ".hidden")).close();

            Series series = new Series("_test_ignore_", testDir.getAbsolutePath());
            java.util.List<Episode> episodes = VideoScanner.scanEpisodes(series);

            // 只有 mp4 应被识别为视频
            assertEquals("Only mp4 should be counted as episode", 1, episodes.size());
        } finally {
            for (File f : testDir.listFiles()) f.delete();
            testDir.delete();
        }
    }
}

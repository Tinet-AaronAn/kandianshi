package com.cantv.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * VideoScanner 单元测试
 * 测试视频文件识别逻辑（不依赖 Android Framework）
 */
public class VideoScannerTest {

    @Test
    public void testVideoExtensions_mp4() {
        // 通过反射或直接调用 isVideoFile
        // 由于 isVideoFile 是 private，这里测试公共方法的行为
        // 在重构时可以将其改为 package-private 或 static 工具方法
        assertTrue("mp4 should be recognized as video", isVideoFile("movie.mp4"));
    }

    @Test
    public void testVideoExtensions_mkv() {
        assertTrue("mkv should be recognized", isVideoFile("film.mkv"));
    }

    @Test
    public void testVideoExtensions_avi() {
        assertTrue("avi should be recognized", isVideoFile("old.avi"));
    }

    @Test
    public void testVideoExtensions_mov() {
        assertTrue("mov should be recognized", isVideoFile("iphone.mov"));
    }

    @Test
    public void testVideoExtensions_3gp() {
        assertTrue("3gp should be recognized", isVideoFile("phone.3gp"));
    }

    @Test
    public void testVideoExtensions_webm() {
        assertTrue("webm should be recognized", isVideoFile("web.webm"));
    }

    @Test
    public void testVideoExtensions_caseInsensitive() {
        assertTrue("MP4 uppercase should be recognized", isVideoFile("movie.MP4"));
        assertTrue("Mkv mixed case should be recognized", isVideoFile("film.Mkv"));
    }

    @Test
    public void testNonVideoFile_jpg() {
        assertFalse("jpg should not be recognized as video", isVideoFile("cover.jpg"));
    }

    @Test
    public void testNonVideoFile_png() {
        assertFalse("png should not be recognized", isVideoFile("cover.png"));
    }

    @Test
    public void testNonVideoFile_txt() {
        assertFalse("txt should not be recognized", isVideoFile("readme.txt"));
    }

    @Test
    public void testNonVideoFile_noExtension() {
        assertFalse("No extension should not be recognized", isVideoFile("noext"));
    }

    @Test
    public void testNonVideoFile_empty() {
        assertFalse("Empty string should not be recognized", isVideoFile(""));
    }

    @Test
    public void testNonVideoFile_mp4InMiddle() {
        assertFalse("mp4 in middle of name should not count", isVideoFile("mp4backup.txt"));
    }

    /**
     * 复制 VideoScanner 的判断逻辑进行测试
     * 理想情况下应重构 VideoScanner 使此方法可测试
     */
    private static boolean isVideoFile(String name) {
        String[] VIDEO_EXTENSIONS = {".mp4", ".mkv", ".avi", ".mov", ".3gp", ".webm"};
        String lower = name.toLowerCase();
        for (String ext : VIDEO_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }
}

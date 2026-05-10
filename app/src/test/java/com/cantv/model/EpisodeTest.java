package com.cantv.model;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;

/**
 * Episode 模型单元测试
 */
public class EpisodeTest {

    @Test
    public void testConstructorAndGetters() {
        File videoFile = new File("/sdcard/看电视/西游记/第01集.mp4");
        Episode episode = new Episode(1, "第01集", videoFile, "西游记");

        assertEquals(1, episode.getIndex());
        assertEquals("第01集", episode.getTitle());
        assertEquals(videoFile, episode.getVideoFile());
        assertEquals("西游记", episode.getSeriesName());
    }

    @Test
    public void testDisplayTitle() {
        File videoFile = new File("/sdcard/看电视/西游记/第01集.mp4");
        Episode episode = new Episode(1, "第01集", videoFile, "西游记");
        assertEquals("第1集", episode.getDisplayTitle());
    }

    @Test
    public void testDisplayTitle_highIndex() {
        File videoFile = new File("/sdcard/看电视/西游记/第99集.mp4");
        Episode episode = new Episode(99, "第99集", videoFile, "西游记");
        assertEquals("第99集", episode.getDisplayTitle());
    }
}

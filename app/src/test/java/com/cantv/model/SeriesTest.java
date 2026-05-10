package com.cantv.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Series 模型单元测试
 */
public class SeriesTest {

    @Test
    public void testConstructor() {
        Series series = new Series("乡村爱情故事", "/sdcard/看电视/乡村爱情故事");
        assertEquals("乡村爱情故事", series.getName());
        assertEquals("/sdcard/看电视/乡村爱情故事", series.getFolderPath());
    }

    @Test
    public void testCoverPath() {
        Series series = new Series("西游记", "/sdcard/看电视/西游记");
        assertNull(series.getCoverPath());
        series.setCoverPath("/sdcard/看电视/西游记/cover.jpg");
        assertEquals("/sdcard/看电视/西游记/cover.jpg", series.getCoverPath());
    }

    @Test
    public void testEpisodeCount() {
        Series series = new Series("还珠格格", "/sdcard/看电视/还珠格格");
        assertEquals(0, series.getEpisodeCount());
        series.setEpisodeCount(24);
        assertEquals(24, series.getEpisodeCount());
    }

    @Test
    public void testNameSetter() {
        Series series = new Series("旧名", "/path");
        series.setName("新名");
        assertEquals("新名", series.getName());
    }
}

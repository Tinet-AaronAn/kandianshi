package com.cantv.model;

import java.io.File;

/**
 * 单集
 */
public class Episode {
    private int index;          // 第几集
    private String title;       // 显示名
    private File videoFile;     // 视频文件
    private String seriesName;  // 所属剧名

    public Episode(int index, String title, File videoFile, String seriesName) {
        this.index = index;
        this.title = title;
        this.videoFile = videoFile;
        this.seriesName = seriesName;
    }

    public int getIndex() { return index; }
    public String getTitle() { return title; }
    public File getVideoFile() { return videoFile; }
    public String getSeriesName() { return seriesName; }

    public String getDisplayTitle() {
        return "第" + index + "集";
    }
}

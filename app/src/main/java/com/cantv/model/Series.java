package com.cantv.model;

/**
 * 电视剧/节目
 */
public class Series {
    private String name;          // 剧名
    private String coverPath;     // 封面图片路径
    private String folderPath;    // 视频文件夹路径
    private int episodeCount;     // 集数

    public Series(String name, String folderPath) {
        this.name = name;
        this.folderPath = folderPath;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public int getEpisodeCount() { return episodeCount; }
    public void setEpisodeCount(int count) { this.episodeCount = count; }
}

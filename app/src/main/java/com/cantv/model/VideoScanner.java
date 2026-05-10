package com.cantv.model;

import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 扫描本地视频文件
 * 目录结构约定：
 *   /sdcard/看电视/乡村爱情故事/第01集.mp4
 *   /sdcard/看电视/乡村爱情故事/第02集.mp4
 *   /sdcard/看电视/西游记/第01集.mp4
 *   ...
 */
public class VideoScanner {

    private static final String[] VIDEO_EXTENSIONS = {".mp4", ".mkv", ".avi", ".mov", ".3gp", ".webm"};
    private static final String ROOT_DIR = "看电视";

    public static File getRootDir() {
        return new File(Environment.getExternalStorageDirectory(), ROOT_DIR);
    }

    /**
     * 扫描所有电视剧
     */
    public static List<Series> scanAll() {
        List<Series> seriesList = new ArrayList<>();
        File root = getRootDir();

        if (!root.exists() || !root.isDirectory()) {
            return seriesList;
        }

        File[] folders = root.listFiles(File::isDirectory);
        if (folders == null) return seriesList;

        for (File folder : folders) {
            Series series = new Series(folder.getName(), folder.getAbsolutePath());

            // 查找封面图
            File[] covers = folder.listFiles((dir, name) ->
                name.toLowerCase().startsWith("cover") &&
                (name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpeg"))
            );
            if (covers != null && covers.length > 0) {
                series.setCoverPath(covers[0].getAbsolutePath());
            }

            // 统计集数
            File[] videos = folder.listFiles((dir, name) -> isVideoFile(name));
            series.setEpisodeCount(videos != null ? videos.length : 0);

            seriesList.add(series);
        }

        // 按名称排序
        Collections.sort(seriesList, (a, b) -> a.getName().compareTo(b.getName()));
        return seriesList;
    }

    /**
     * 扫描某部剧的所有集
     */
    public static List<Episode> scanEpisodes(Series series) {
        List<Episode> episodes = new ArrayList<>();
        File folder = new File(series.getFolderPath());

        if (!folder.exists()) return episodes;

        File[] videos = folder.listFiles((dir, name) -> isVideoFile(name));
        if (videos == null) return episodes;

        // 按文件名排序
        Arrays.sort(videos, (a, b) -> a.getName().compareTo(b.getName()));

        int index = 1;
        for (File video : videos) {
            String title = video.getName();
            // 去掉扩展名
            int dot = title.lastIndexOf('.');
            if (dot > 0) title = title.substring(0, dot);
            episodes.add(new Episode(index++, title, video, series.getName()));
        }

        return episodes;
    }

    private static boolean isVideoFile(String name) {
        String lower = name.toLowerCase();
        for (String ext : VIDEO_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }
}

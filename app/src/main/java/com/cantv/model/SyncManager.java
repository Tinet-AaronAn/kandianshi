package com.cantv.model;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频同步管理器 - 腾讯云COS公有读版
 *
 * 通过 COS ListObjectsV2 API 动态发现桶内目录和文件，
 * 不再硬编码剧集配置。
 *
 * 同步逻辑：
 * 1. 列出COS桶顶层目录 → 发现所有剧
 * 2. 列出每部剧的文件 → 获取文件名和大小
 * 3. 与本地对比：
 *    - 本地没有的剧 → 整部下载
 *    - 本地有但缺集 → 补充下载
 *    - 文件大小不一致 → 重新下载
 *    - 文件大小一致 → 跳过
 */
public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;

    // ====== COS 配置 ======
    private static final String COS_BASE_URL = "https://kandianshi-1259224536.cos.ap-beijing.myqcloud.com";
    // ====== 配置结束 ======

    public static class SyncResult {
        public int totalFiles;
        public int downloadedFiles;
        public int skippedFiles;
        public long totalBytes;
        public long downloadedBytes;
        public String currentFile;
        public boolean cancelled;
        public String error;

        public boolean isComplete() {
            return downloadedFiles + skippedFiles == totalFiles && !cancelled;
        }

        public int getProgress() {
            if (totalBytes == 0) return 0;
            return (int) (downloadedBytes * 100 / totalBytes);
        }
    }

    public interface ProgressCallback {
        void onProgress(SyncResult result);
        void onComplete(SyncResult result);
        void onError(String message);
    }

    /** COS 上一个远程文件的信息 */
    public static class RemoteFile {
        public String key;       // 对象key，如 "赵本山小品合集/第01集.mp4"
        public String filename;  // 文件名，如 "第01集.mp4"
        public String seriesName; // 剧名，如 "赵本山小品合集"
        public long size;

        RemoteFile(String key, String filename, String seriesName, long size) {
            this.key = key;
            this.filename = filename;
            this.seriesName = seriesName;
            this.size = size;
        }
    }

    private volatile boolean cancelled = false;

    public SyncManager() {}

    public void cancel() {
        cancelled = true;
    }

    /**
     * 拼接COS下载URL（中文URL编码）
     */
    private String buildUrl(String objectKey) {
        try {
            StringBuilder sb = new StringBuilder(COS_BASE_URL);
            String[] parts = objectKey.split("/", -1);
            for (int i = 0; i < parts.length; i++) {
                sb.append("/");
                if (parts[i].length() > 0) {
                    sb.append(URLEncoder.encode(parts[i], "UTF-8")
                        .replace("+", "%20"));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "构建URL失败: " + objectKey, e);
            return COS_BASE_URL + "/" + objectKey;
        }
    }

    /**
     * 通过 COS ListObjectsV2 API 列出桶内所有视频文件
     * 返回所有 .mp4/.mkv/.avi/.mov/.3gp/.webm 文件
     */
    private List<RemoteFile> listAllRemoteFiles() throws Exception {
        List<RemoteFile> files = new ArrayList<>();
        String continuationToken = null;

        do {
            StringBuilder urlBuilder = new StringBuilder(COS_BASE_URL);
            urlBuilder.append("?list-type=2&max-keys=1000");

            if (continuationToken != null && !continuationToken.isEmpty()) {
                urlBuilder.append("&continuation-token=")
                    .append(URLEncoder.encode(continuationToken, "UTF-8"));
            }

            String xml = httpGet(urlBuilder.toString());
            if (xml == null) {
                throw new Exception("无法连接COS，请检查网络");
            }

            // 解析XML
            COSListResult result = parseListXml(xml);
            for (COSObject obj : result.objects) {
                String key = obj.key;
                // 跳过目录标记和根级文件
                if (key.endsWith("/")) continue;

                // 提取剧名和文件名
                int slash = key.indexOf('/');
                if (slash < 0 || slash == key.length() - 1) continue;

                String seriesName = key.substring(0, slash);
                String filename = key.substring(slash + 1);

                // 只保留视频文件
                if (!isVideoFile(filename)) continue;

                files.add(new RemoteFile(key, filename, seriesName, obj.size));
            }

            continuationToken = result.isTruncated ? result.nextContinuationToken : null;
        } while (continuationToken != null);

        Log.i(TAG, "COS发现 " + files.size() + " 个视频文件");
        return files;
    }

    /**
     * 执行同步
     */
    public void sync(ProgressCallback callback) {
        cancelled = false;
        SyncResult result = new SyncResult();

        try {
            // 1. 发现COS上的所有视频文件
            Log.i(TAG, "开始扫描COS视频文件");
            List<RemoteFile> remoteFiles = listAllRemoteFiles();

            if (remoteFiles.isEmpty()) {
                callback.onError("COS上没有发现视频文件");
                return;
            }

            // 2. 统计剧集信息（用于显示）
            java.util.Set<String> seriesNames = new java.util.HashSet<>();
            for (RemoteFile rf : remoteFiles) {
                seriesNames.add(rf.seriesName);
            }
            Log.i(TAG, "COS发现 " + seriesNames.size() + " 部剧，共 " + remoteFiles.size() + " 集");

            // 3. 与本地对比，生成下载任务
            File rootDir = VideoScanner.getRootDir();
            List<DownloadTask> tasks = new ArrayList<>();

            for (RemoteFile rf : remoteFiles) {
                if (cancelled) break;

                File localFile = new File(new File(rootDir, rf.seriesName), rf.filename);

                if (localFile.exists() && localFile.length() > 0) {
                    // 本地文件存在，检查大小
                    if (rf.size > 0 && localFile.length() != rf.size) {
                        // 大小不一致，重新下载
                        Log.w(TAG, "文件大小不匹配，重新下载: " + rf.seriesName + "/" + rf.filename
                            + " 本地:" + localFile.length() + " 远程:" + rf.size);
                        localFile.delete();
                    } else {
                        // 文件大小一致（或远程大小未知），跳过
                        result.skippedFiles++;
                        continue;
                    }
                }

                // 需要下载
                DownloadTask task = new DownloadTask();
                task.localFile = localFile;
                task.seriesName = rf.seriesName;
                task.filename = rf.filename;
                task.objectKey = rf.key;
                task.remoteSize = rf.size;
                tasks.add(task);
            }

            result.totalFiles = result.skippedFiles + tasks.size();
            result.totalBytes = 0;
            for (DownloadTask task : tasks) {
                result.totalBytes += task.remoteSize;
            }

            Log.i(TAG, "需要下载 " + tasks.size() + " 个文件，跳过 " + result.skippedFiles
                + " 个，共 " + formatSize(result.totalBytes));

            if (tasks.isEmpty()) {
                result.downloadedFiles = 0;
                result.downloadedBytes = result.totalBytes;
                callback.onComplete(result);
                return;
            }

            // 4. 逐个下载
            for (DownloadTask task : tasks) {
                if (cancelled) {
                    result.cancelled = true;
                    callback.onComplete(result);
                    return;
                }

                result.currentFile = task.seriesName + "/" + task.filename;
                callback.onProgress(result);

                String downloadUrl = buildUrl(task.objectKey);

                boolean success = downloadFile(downloadUrl, task.localFile, task.remoteSize,
                    bytes -> {
                        result.downloadedBytes += bytes;
                        callback.onProgress(result);
                    }
                );

                if (success) {
                    result.downloadedFiles++;
                } else if (!cancelled) {
                    Log.w(TAG, "下载失败: " + task.filename);
                }
            }

            result.currentFile = null;
            callback.onComplete(result);

        } catch (Exception e) {
            Log.e(TAG, "同步异常", e);
            if (!cancelled) {
                callback.onError("同步失败: " + e.getMessage());
            }
        }
    }

    /**
     * HTTP GET 请求，返回响应体字符串
     */
    private String httpGet(String urlStr) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            if (code != 200) {
                Log.e(TAG, "HTTP GET失败 " + code + ": " + urlStr);
                return null;
            }

            InputStream is = conn.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                sb.append(new String(buf, 0, len, "UTF-8"));
            }
            is.close();
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "HTTP GET异常: " + urlStr, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 解析 COS ListObjectsV2 XML 响应
     */
    private COSListResult parseListXml(String xml) {
        COSListResult result = new COSListResult();

        // 简易XML解析（不引入第三方库）
        // 提取 IsTruncated
        result.isTruncated = extractTag(xml, "IsTruncated").equalsIgnoreCase("true");

        // 提取 NextContinuationToken
        result.nextContinuationToken = extractTag(xml, "NextContinuationToken");

        // 提取所有 Contents
        int pos = 0;
        while (true) {
            int start = xml.indexOf("<Contents>", pos);
            if (start < 0) break;
            int end = xml.indexOf("</Contents>", start);
            if (end < 0) break;

            String content = xml.substring(start + "<Contents>".length(), end);
            String key = extractTag(content, "Key");
            String sizeStr = extractTag(content, "Size");
            long size = 0;
            try { size = Long.parseLong(sizeStr); } catch (NumberFormatException ignored) {}

            if (!key.isEmpty()) {
                result.objects.add(new COSObject(key, size));
            }

            pos = end + "</Contents>".length();
        }

        return result;
    }

    /** 提取XML标签内容 */
    private String extractTag(String xml, String tag) {
        String open = "<" + tag + ">";
        String close = "</" + tag + ">";
        int start = xml.indexOf(open);
        if (start < 0) return "";
        start += open.length();
        int end = xml.indexOf(close, start);
        if (end < 0) return "";
        return xml.substring(start, end);
    }

    private boolean downloadFile(String urlStr, File localFile, long remoteSize,
                                  DownloadProgressCallback progressCb) {
        HttpURLConnection conn = null;
        try {
            File parent = localFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            File tempFile = new File(localFile.getParent(), localFile.getName() + ".downloading");

            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestMethod("GET");

            int code = conn.getResponseCode();
            if (code != 200) {
                Log.e(TAG, "下载失败 HTTP " + code + ": " + urlStr);
                return false;
            }

            InputStream is = conn.getInputStream();
            FileOutputStream fos = new FileOutputStream(tempFile);

            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            long lastReportTime = 0;

            while ((len = is.read(buf)) != -1) {
                if (cancelled) {
                    fos.close();
                    is.close();
                    tempFile.delete();
                    return false;
                }

                fos.write(buf, 0, len);

                long now = System.currentTimeMillis();
                if (progressCb != null && now - lastReportTime > 200) {
                    progressCb.onBytesDownloaded(len);
                    lastReportTime = now;
                }
            }

            fos.flush();
            fos.close();
            is.close();

            if (localFile.exists()) {
                localFile.delete();
            }
            if (!tempFile.renameTo(localFile)) {
                copyFile(tempFile, localFile);
                tempFile.delete();
            }

            Log.i(TAG, "下载完成: " + localFile.getName()
                + " (" + formatSize(localFile.length()) + ")");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "下载异常: " + urlStr, e);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void copyFile(File src, File dst) throws Exception {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dst);
        byte[] buf = new byte[BUFFER_SIZE];
        int len;
        while ((len = fis.read(buf)) != -1) {
            fos.write(buf, 0, len);
        }
        fos.flush();
        fos.close();
        fis.close();
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static boolean isVideoFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mkv")
            || lower.endsWith(".avi") || lower.endsWith(".mov")
            || lower.endsWith(".3gp") || lower.endsWith(".webm");
    }

    // ====== 内部类 ======

    private static class DownloadTask {
        File localFile;
        String seriesName;
        String filename;
        String objectKey;
        long remoteSize;
    }

    private static class COSObject {
        String key;
        long size;

        COSObject(String key, long size) {
            this.key = key;
            this.size = size;
        }
    }

    private static class COSListResult {
        boolean isTruncated;
        String nextContinuationToken;
        List<COSObject> objects = new ArrayList<>();
    }

    private interface DownloadProgressCallback {
        void onBytesDownloaded(long bytes);
    }
}

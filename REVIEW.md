# 代码评审报告 - 看电视 v1.1

## 评审日期: 2026-05-09

## 1. 代码质量问题

### 1.1 SeriesAdapter - 封面图内存泄漏 (严重)
- **文件**: `ui/SeriesAdapter.java`
- **问题**: 在主线程用 `BitmapFactory.decodeStream()` 加载封面图，没有做采样压缩（inSampleSize），大图会直接 OOM
- **修复**: 添加 BitmapFactory.Options 计算 inSampleSize，先读尺寸再采样加载

### 1.2 SeriesAdapter - FileInputStream 未在 finally 中关闭 (中等)
- **问题**: `fis.close()` 在 try 块中，如果 `decodeStream` 抛异常，流不会关闭
- **修复**: 使用 try-with-resources

### 1.3 VideoScanner - 扫描在主线程执行 (中等)
- **问题**: `scanAll()` 和 `scanEpisodes()` 涉及文件 I/O，在主线程调用可能 ANR
- **修复**: 使用 AsyncTask 或线程执行，目前项目简单先加注释，后续优化

### 1.4 VideoPlayerActivity - VideoView 点击事件不可靠 (中等)
- **问题**: `videoView.setOnClickListener` 在很多设备上不触发
- **修复**: 在 VideoView 上覆盖透明 View 来接收点击

### 1.5 MainActivity - 权限请求后不会自动刷新 (中等)
- **问题**: Android 11+ 跳转权限页面后返回，onResume 会重新检查，但如果用户没授予权限，没有引导重新请求的入口
- **修复**: 在空状态提示中增加"点击重新请求权限"

### 1.6 所有 Activity - 缺少 onBackPressed 处理 (低)
- **问题**: 播放器按返回键默认 finish，没问题，但主界面按返回应该退出应用
- **修复**: MainActivity 增加 confirm 退出或直接 finish

## 2. 安全问题

### 2.1 MANAGE_EXTERNAL_STORAGE 权限 (高风险)
- **文件**: `AndroidManifest.xml`
- **问题**: `MANAGE_EXTERNAL_STORAGE` 是高敏感权限，Google Play 对使用此权限的应用审核极严，可能被拒
- **建议**: 考虑改用 MediaStore API 或 SAF（Storage Access Framework），但这会增加复杂度。对于仅侧载安装的场景可以保留，但需在说明中注明
- **当前处理**: 保留但添加注释说明

### 2.2 文件路径直接传递 (低风险)
- **问题**: video_path 通过 Intent 传递绝对路径，恶意应用可构造路径
- **评估**: 此 App 不接收外部 Intent（无 exported Activity 除了 MainActivity），风险低

### 2.3 WebView/网络 (无风险)
- **评估**: 无 WebView、无网络请求、无第三方库，攻击面极小

## 3. 适老化问题

### 3.1 播放器控制栏按钮偏小
- 控制按钮 72dp/88dp，对老年人来说仍然偏小
- **建议**: 播放/暂停加大到 100dp+

### 3.2 没有音量控制
- 老年人可能需要直接调节音量
- **建议**: 增加音量 +/- 大按钮

### 3.3 没有进度记忆
- 看到一半退出，下次要从头看
- **建议**: SharedPreferences 保存播放进度

## 4. 兼容性问题

### 4.1 Android 13+ 通知权限 (低)
- 不需要通知，无影响

### 4.2 Android 10 Scoped Storage (中等)
- **问题**: Android 10 引入 Scoped Storage，`Environment.getExternalStorageDirectory()` 在 targetSdk 30+ 不推荐
- **当前**: 使用 `MANAGE_EXTERNAL_STORAGE` 可绕过，但需要注意

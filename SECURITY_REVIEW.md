# 安全检查报告 - 看电视 v1.1

## 检查日期: 2026-05-09

## 1. 权限分析

| 权限 | 风险等级 | 说明 |
|------|----------|------|
| READ_EXTERNAL_STORAGE | 低 | Android 10 以下读取视频必需 |
| WRITE_EXTERNAL_STORAGE | 低 | 创建目录结构用 |
| MANAGE_EXTERNAL_STORAGE | 高 | Android 11+ 全文件访问，Google Play 审核极严 |

### 建议
- 如果仅侧载安装（不上 Google Play），MANAGE_EXTERNAL_STORAGE 可以保留
- 如果要上 Google Play，需改用 SAF（Storage Access Framework）让用户选择目录
- 当前方案适合给老年人手机直接安装，风险可控

## 2. 数据安全

### 2.1 播放进度存储
- 使用 SharedPreferences 存储，仅存视频路径 hashCode + 播放位置
- **风险**: 低。不涉及敏感数据

### 2.2 文件路径传递
- video_path 通过 Intent 传递绝对路径
- **风险**: 低。所有 Activity 均非 exported（除 MainActivity），外部应用无法构造恶意 Intent
- MainActivity 虽然是 exported（LAUNCHER），但只接收 MAIN/LAUNCHER intent，不接收外部数据

### 2.3 无网络通信
- App 无任何网络请求，无数据上传
- **风险**: 无

## 3. 代码安全

### 3.1 文件操作
- VideoScanner 使用 `File.listFiles()` 遍历目录，无路径注入风险
- SeriesAdapter 使用 `FileInputStream` 读取封面图，已修复资源泄漏

### 3.2 无 WebView
- 不使用 WebView，无 XSS/JS 注入风险

### 3.3 无第三方库
- 纯 Android SDK，无供应链风险

### 3.4 无硬编码密钥
- 代码中无 API key、密码等敏感信息

## 4. 组件安全

### 4.1 Activity 导出
- 仅 MainActivity 为 exported（LAUNCHER 必须）
- SeriesDetailActivity 和 VideoPlayerActivity 未导出
- **风险**: 低

### 4.2 Intent 注入
- 非导出 Activity 不接受外部 Intent
- **风险**: 无

## 5. Android 版本兼容性安全

### 5.1 Android 13+ 通知权限
- App 不发送通知，无需 POST_NOTIFICATIONS 权限

### 5.2 Android 14 前台服务
- App 不使用前台服务

### 5.3 Scoped Storage
- 使用 MANAGE_EXTERNAL_STORAGE 绕过，功能正常但需注意审核政策

## 6. 总结

| 维度 | 评分 | 说明 |
|------|------|------|
| 权限最小化 | ⚠️ 中等 | MANAGE_EXTERNAL_STORAGE 权限过强，但对目标场景合理 |
| 数据安全 | ✅ 良好 | 无敏感数据存储和传输 |
| 代码安全 | ✅ 良好 | 纯 SDK，无第三方依赖，攻击面极小 |
| 组件安全 | ✅ 良好 | 仅必要组件导出 |
| 整体评估 | ✅ 低风险 | 适合侧载安装场景，无需额外加固 |

## 7. 改进建议（优先级排序）

1. **P1**: 如果需要上 Google Play，将文件访问改为 SAF
2. **P2**: 添加 ProGuard 混淆（release 构建开启 minifyEnabled）
3. **P3**: 考虑添加签名校验防止重打包

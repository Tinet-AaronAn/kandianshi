# 通用混淆规则
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留模型类
-keep class com.cantv.model.** { *; }

# 保留 Activity
-keep class com.cantv.ui.** { *; }
-keep class com.cantv.player.** { *; }

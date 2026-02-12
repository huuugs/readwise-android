# ReadWise Android - 安装测试指南

## 当前状态：可以安装到手机测试！✅

---

## 快速开始（3步）

### 方法A：Android Studio（推荐）

```bash
# 1. 打开Android Studio
File → Open → 选择 /data/data/com.termux/files/home/reader/readwise-android

# 2. 等待Gradle同步完成
首次同步可能需要5-10分钟（下载依赖）

# 3. 连接手机或启动模拟器
Run → Run 'app' (或按 Shift+F10)
```

### 方法B：命令行

```bash
cd /data/data/com.termux/files/home/reader/readwise-android

# 给gradlew添加执行权限（已完成）
chmod +x gradlew

# 构建Debug APK
./gradlew assembleDebug

# APK位置
app/build/outputs/apk/debug/app-debug.apk
```

---

## 功能测试清单

### ✅ 完全可用的功能

| 功能 | 状态 | 测试方法 |
|------|------|---------|
| TXT文件阅读 | ✅ | 导入TXT → 自动编码检测 → 章节导航 |
| EPUB文本阅读 | ✅ | 导入EPUB → 显示文本内容 → 章节导航 |
| UI导航 | ✅ | 底部标签栏切换 → 书架/发现/词典/设置 |
| 章节导航 | ✅ | 上一章/下一章按钮 → 章节切换 |
| 阅读进度 | ✅ | 阅读后关闭 → 重新打开恢复位置 |
| 字体设置 | ✅ | 设置面板 → 调整字号/行高/对齐 |

### ⚠️ 部分可用的功能

| 功能 | 状态 | 限制说明 |
|------|------|---------|
| EPUB图片 | ⚠️ | HTML中的`<img>`标签不显示 |
| PDF阅读 | ❌ | 只显示"PDF Page X"占位符 |
| 书籍导入 | ⚠️ | 需手动将文件复制到设备存储 |

### ✅ 已实现但需要API的功能

| 功能 | 状态 | 需求 |
|------|------|------|
| AI聊天 | ✅ | 需要配置OpenAI API密钥 |
| X-ray分析 | ✅ | 需要配置OpenAI API密钥 |
| AI摘要 | ✅ | 需要配置OpenAI API密钥 |

---

## 测试建议

### 1. TXT阅读测试（最完整）

```bash
# 准备测试文件
1. 创建一个UTF-8编码的TXT文件
2. 包含多个章节（使用"第X章"格式）
3. 约1000-2000字

# 测试步骤
1. 打开应用 → 应该显示书架
2. 点击任意书籍 → 应该打开阅读器
3. 测试章节导航 → 上一章/下一章
4. 测试设置面板 → 字号、行高
5. 关闭应用 → 重新打开检查进度
```

### 2. EPUB阅读测试（基本可用）

```bash
# 准备测试文件
1. 准备一个简单的EPUB文件
2. 最好是纯文本内容为主

# 测试步骤
1. 导入EPUB → 应该显示文本内容
2. 检查目录显示 → 章节列表
3. 测试章节导航 → 目录点击跳转
4. 已知问题：图片不会显示
```

### 3. AI功能测试（可选）

```bash
# 配置步骤
1. 设置 → AI配置
2. 选择提供商：OpenAI
3. 输入API密钥（sk-xxx）
4. 点击保存

# 测试步骤
1. 打开AI聊天 → 发送消息测试
2. 打开书籍 → X-ray按钮 → 生成分析
```

---

## 已知问题

### 1. PDF无法阅读

**现象**：打开PDF文件只显示"PDF Page X"

**原因**：PdfRenderer需要完整实现renderPage方法

**影响**：无法阅读PDF格式书籍

**临时方案**：使用TXT或EPUB格式测试

---

### 2. EPUB图片不显示

**现象**：EPUB中的图片不显示

**原因**：Jsoup解析HTML后未处理`<img>`标签

**影响**：带图片的EPUB缺少图片

**临时方案**：使用纯文本EPUB测试

---

### 3. 缺少书籍导入功能

**现象**：书架界面没有导入按钮

**原因**：BookshelfScreen的导入功能未实现

**影响**：需要手动通过ADB push文件

**临时方案**：
```bash
adb push test.txt /sdcard/Download/
```

---

## 编译可能遇到的问题

### 问题1：Gradle同步失败

**错误信息**：
```
Could not resolve dependencies
```

**解决方案**：
1. 检查网络连接
2. 配置国内镜像（如需要）

在`build.gradle`添加：
```gradle
repositories {
    maven { url 'https://maven.aliyun.com/repository/google' }
    maven { url 'https://maven.aliyun.com/repository/central' }
}
```

### 问题2：SDK版本不匹配

**错误信息**：
```
SDK_INT not recognized
```

**解决方案**：
确保安装了Android SDK 34（Android 14）
```bash
# Android Studio
Tools → SDK Manager → 安装 Android 14.0
```

### 问题3：依赖冲突

**错误信息**：
```
Conflict with dependency
```

**解决方案**：
```bash
./gradlew clean
./gradlew assembleDebug
```

---

## 推荐测试流程

### 第一轮：基础功能（15分钟）

1. ✅ 安装应用
2. ✅ 测试UI导航（标签栏切换）
3. ✅ 测试TXT阅读（导入 → 阅读 → 导航）
4. ✅ 测试设置面板（字号、行高）
5. ✅ 测试进度保存（关闭 → 重开）

### 第二轮：EPUB测试（10分钟）

1. ✅ 导入EPUB文件
2. ✅ 检查文本显示
3. ✅ 测试目录导航
4. ⚠️ 确认图片不显示（已知问题）

### 第三轮：AI功能（可选，20分钟）

1. ✅ 配置OpenAI API
2. ✅ 测试AI聊天
3. ✅ 测试X-ray分析
4. ✅ 测试章节摘要

---

## 安装后立即能做什么

### ✅ 今天就能用

1. **阅读TXT书籍**
   - 自动编码检测
   - 章节识别（中文/英文）
   - 进度自动保存

2. **阅读EPUB书籍**
   - 基本文本显示
   - 章节导航
   - 字体调整

3. **UI体验**
   - Material Design 3
   - 书架网格/列表
   - 搜索功能

### ⏳ 需要额外开发

1. **PDF阅读** - 需要6-8小时实现PdfiumAndroid
2. **书签功能** - 需要6-8小时开发
3. **高亮标注** - 需要8-10小时开发
4. **书籍导入** - 需要6-8小时开发

---

## 总结

**当前状态**：可以安装到手机测试基础功能

**推荐测试格式**：
1. TXT文件 - 完全可用 ✅
2. EPUB文件 - 基本可用 ⚠️
3. PDF文件 - 不可用 ❌

**达到生产就绪需要的额外工作**：
- 修复PDF渲染（6-8小时）
- 修复EPUB图片（2-3小时）
- 实现书签功能（6-8小时）
- 实现高亮标注（8-10小时）

**预计总时间**：24-30小时开发时间 → 完全可用的阅读器

---

## 下一步建议

### 今天（2-3小时）
1. 安装应用到手机
2. 测试TXT和EPUB阅读
3. 收集反馈

### 本周（8-12小时）
1. 修复PDF渲染
2. 修复EPUB图片
3. 实现书签功能

### 本月（24-40小时）
1. 高亮标注
2. 书籍导入
3. 词典集成
4. 性能优化

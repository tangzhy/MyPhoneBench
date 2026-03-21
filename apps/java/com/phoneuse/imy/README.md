# iMy Privacy App

隐私管理 App，用于 PhoneUse 隐私协议项目。

## 功能

- **隐私管理页面**：
  - 隐私模式滑动条（完全可控 / 半可控）
  - 个人数据 key-value 列表（LOW/HIGH 分级显示，HIGH 值暗码保护）
  - App 资源权限管理
  - 支持编辑、删除、调整隐私等级

- **Chatbot 页面**（开发中）：
  - 模型选择
  - 历史聊天记录

## 技术栈

- **Kotlin** + **Jetpack Compose** + **Room Database**
- 使用 Bazel 构建

## 构建

```bash
# 从项目根目录
./build_imy.sh
```

## 安装到模拟器

```bash
# 构建后安装
adb install bazel-bin/java/com/phoneuse/imy/imyapp.apk

# 或使用 adb install -r 覆盖安装
adb install -r bazel-bin/java/com/phoneuse/imy/imyapp.apk
```

## 初始化数据库

```bash
# 从项目根目录
./init_profile_db.sh
```

注意：数据库会在 app 首次运行时自动创建。初始化脚本用于预填充 seed 数据。

## 数据库位置

数据库文件位于：
```
/data/data/com.phoneuse.imy/databases/profile.db
```

可以通过 ADB 直接访问：
```bash
adb shell sqlite3 /data/data/com.phoneuse.imy/databases/profile.db
```

## 项目结构

```
apps/java/com/phoneuse/imy/
├── AndroidManifest.xml          # App manifest
├── MyApplication.kt             # Application 类
├── BUILD.bazel                  # 主构建文件
├── app/
│   ├── AndroidManifest.xml
│   ├── MainActivity.kt          # 主 Activity (Compose)
│   └── BUILD.bazel
├── data/
│   ├── ProfileItem.kt           # Room 实体
│   ├── ProfileDao.kt            # DAO 接口
│   ├── ProfileDatabase.kt       # Room Database
│   └── BUILD.bazel
└── ui/
    ├── IMyApp.kt                # 主 Compose App
    ├── PrivacyScreen.kt         # 隐私管理页面
    ├── ChatbotScreen.kt         # Chatbot 页面
    ├── theme/
    │   └── Theme.kt             # Material Theme
    └── BUILD.bazel
```


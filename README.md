<div align="center">
  <img src="docs/logo.png" width="140" alt="PhotosManager logo"/>
  <h1>PhotosManager</h1>
  <p>把每次解锁手机的碎片时间，变成清理相册的时间。</p>
  <p>
    <img alt="platform" src="https://img.shields.io/badge/Android-7.0%2B-green"/>
    <img alt="release" src="https://img.shields.io/badge/release-v1.1-blue"/>
    <img alt="lang" src="https://img.shields.io/badge/Kotlin-2.0.21-orange"/>
    <img alt="offline" src="https://img.shields.io/badge/联网-不需要-informational"/>
  </p>
</div>

---

## 它解决什么问题

相册里几万张照片，真正想留下的可能只有一半，但"坐下来整理相册"这件事几乎不可能坚持下来。

PhotosManager 的思路是不给你整块时间：**每次点亮屏幕，它随机推几张照片到屏幕上，你只做一次"留下 / 删掉"的判断，然后照常去干你刚才要做的事。** 解锁看时间的这 5 秒钟，顺手就能清掉几张废片。

## 一次亮屏会发生什么

1. 你在设置里勾选要管理的相册，打开**启用服务**。
2. 之后每次屏幕点亮，`ScreenMonitorService` 收到 `ACTION_SCREEN_ON`，延迟 300ms 后拉起悬浮窗。
3. 屏幕底部弹出约占 75% 高度的卡片，随机展示 N 张（默认 5 张）照片，显示"当前第几张 / 共几张"和来源相册。
4. 每张只有三个动作：
   - **保留** —— 不做任何处理，下一张
   - **删除** —— 标记待删，下一张
   - **附近** —— 把这张照片前后相邻的几张也拉进本轮队列
5. 左右滑动切换照片（滑动超过屏宽 10% 生效）。
6. 一轮结束后进入**确认删除**页，缩略图逐张列出，可以反选取消。
7. 点确认后才真正删除。Android 11 及以上走系统原生的 `MediaStore.createDeleteRequest()`，你会再看到一次系统级确认框——应用绝不会静默删掉任何一张照片。

> **关于"附近"**：这里的"附近"不是地理位置，而是同一相册里按添加时间排序的位置邻近。连拍、同一场景的重复照片往往紧挨着，一张是废片，旁边几张大概率也是。数量默认 10 张，可在设置里调。

## 设置项

| 设置 | 形式 | 范围 | 默认 |
| --- | --- | --- | --- |
| 每次显示照片数量 | 滑杆 | 1 – 20 | 5 |
| 附近照片数量 | 滑杆 | 5 – 30 | 10 |
| 启用服务 | 开关 | — | 关 |
| 相册选择 | 多选列表 | 设备上全部相册 | 空 |

## 安装

到 [Releases](../../releases) 下载 `PhotosManager-v1.1.apk`，安装时系统会提示"未知来源应用"，允许即可。

覆盖升级要求签名一致，本仓库发布的 APK 从 v1.1 起使用同一把签名密钥（见文末）。

## 权限说明

这个应用要的权限确实比普通应用多，逐条解释清楚：

| 权限 | 用在哪里 | 必需吗 |
| --- | --- | --- |
| 读取照片（`READ_MEDIA_IMAGES` / 旧版存储权限） | 查询相册、显示缩略图 | 必需 |
| 悬浮窗（`SYSTEM_ALERT_WINDOW`） | 亮屏弹出的那张照片卡片就是悬浮窗 | 必需，需手动到系统设置里授予 |
| 通知（`POST_NOTIFICATIONS`） | 三个前台服务的常驻通知 | 必需（Android 13+） |
| 前台服务 `specialUse` | 亮屏监听需要长期在后台运行 | 必需 |
| `WAKE_LOCK` | `BackgroundService` 持有 partial wakelock 保活 | 必需 |
| 忽略电池优化 | 防止被系统杀掉 | 建议，不授权也能用，只是亮屏可能不弹 |
| 自启动 / 后台活动（厂商 ROM） | 同上 | 建议 |

不申请也不需要：相机、定位、通讯录、网络。Manifest 里根本没有声明 `INTERNET` 权限，也就是说应用在系统层面就无法联网，照片、设置、删除记录全部留在本机。

## 兼容性

- 系统要求：Android 7.0（API 24）— Android 16（targetSdk 36）
- 删除方式：Android 11+ 使用系统删除请求；Android 10 及以下直接通过 `ContentResolver` 删除
- 悬浮窗：Android 8+ 用 `TYPE_APPLICATION_OVERLAY`，更低版本回退到 `TYPE_PHONE`
- 国产 ROM（MIUI / EMUI / ColorOS 等）后台限制较严，需要手动放行自启动，否则亮屏监听可能不触发——应用内已有引导提示

## 已知限制

诚实列一下当前版本的不足，都在代码里可查证：

- **没有"看过就不再推"的机制**：每轮都是全量随机抽取，同一张照片可能被反复推给你。
- **应用内没有回收站**：删除即从媒体库移除，只能依赖系统相册自带的"最近删除"。
- **三条常驻通知无法关闭**（屏幕监听 / 悬浮窗 / 后台服务各一条）。
- **大相册一次性全量载入内存**，几万张照片时会有明显延迟。
- 界面文案目前硬编码中文，未做国际化。
- 登录 / 兑换码模块（`auth/`）是未启用的占位代码，`LoginActivity` 连 Manifest 都没注册，不影响任何功能。
- Manifest 里声明了 `DISABLE_KEYGUARD`，但代码中并未使用，属于冗余声明。

## 从源码构建

环境：JDK 17+（实测 JDK 21）、Android SDK Platform 36 + Build-Tools 36.0.0、Gradle 8.13（wrapper 已配置）。

```bash
# 调试包，无需签名材料
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

发布包需要签名材料。`keystore/` 与 `keystore.properties` 已被 `.gitignore` 排除，需自行放置到仓库根目录：

```properties
# keystore.properties
storeFile=keystore/photosmanager-release.jks
storePassword=<你的密钥库口令>
keyAlias=photosmanager-release
keyPassword=<你的密钥口令>
```

```bash
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk（已自动签名）
```

未放置 `keystore.properties` 时构建依然会通过，只是 release 包不签名，无法直接安装。

## 签名

- 密钥别名：`photosmanager-release`，RSA 2048，有效期至 2056-10-23
- 证书 SHA-256：`A0:3F:3E:E2:46:FB:8B:C3:56:A0:03:E8:1E:14:28:AA:B9:56:86:A3:1C:3A:80:B4:47:38:8F:72:6D:B3:ED:F4`
- 密钥库文件与口令不入库，由项目所有者单独保管
- 从 v1.1（首个公开发布版本）起所有 Release 使用同一把密钥；换密钥意味着老用户必须卸载重装

## 技术栈

Kotlin 2.0.21 · Jetpack Compose (BOM 2024.09.00) · Material 3 · Room 2.6.1 · DataStore Preferences 1.1.1 · Coil 2.6.0 · AGP 8.13.1

关键实现：

```
app/src/main/java/com/example/photosmanager/
├── service/     ScreenMonitorService（亮屏监听）、FloatingWindowService（悬浮窗）、BackgroundService（保活）
├── ui/floating/ FloatingWindowManager（窗口管理）、FloatingWindowContent（卡片 UI 与决策逻辑）
├── ui/          confirm（批量确认删除）、settings、onboarding
├── data/        Room 实体与 DAO、PhotoRepository（MediaStore 查询与删除）、PreferencesRepository
└── util/        权限申请、通知
```

## English summary

PhotosManager turns the seconds you spend unlocking your phone into album cleanup. Once the service is enabled, every screen wake pops up a floating card over the lock screen area showing a handful of random photos from the albums you picked. Swipe through with one tap each: keep, mark for deletion, or pull in neighbouring shots (burst photos usually sit next to other junk). At the end of the round you review the marked list and confirm — on Android 11+ the actual deletion goes through the system `MediaStore.createDeleteRequest()` dialog, so nothing is ever removed silently.

No network access, no accounts, no analytics: photos, settings and deletion history stay on the device. Requires the overlay permission and runs foreground services to catch screen-on broadcasts, which is why there are persistent notifications.

## License

暂未声明开源许可证。在明确授权之前，本仓库代码默认保留所有权利（All rights reserved）。

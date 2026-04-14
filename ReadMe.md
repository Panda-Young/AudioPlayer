# AudioPlayer

一个面向 Android 的本地音频播放器工程，当前分支为 `exoplayer`。

## 项目目标

- 提供通用、稳定的本地音频播放能力
- 保持主界面简洁：信息展示 + 播放控制 + 播放列表
- 支持基础实验能力（音效实验室）并与主播放流程解耦
- 保持模块化、可维护、可扩展

## 技术栈

- 语言：Kotlin
- 播放引擎：Media3 ExoPlayer（当前实现）
- 最低版本：`minSdk 28`
- 构建系统：Gradle (KTS)

## 主要功能

### 主播放页

- 顶部音频信息区
	- 封面
	- 标题
	- 艺术家
- 底部播放控制区
	- 进度条（支持拖拽）
	- 当前时间 / 总时长
	- 循环模式切换（单曲循环 / 列表循环 / 随机播放）
	- 上一曲 / 播放暂停 / 下一曲
	- 播放列表开关
- 播放列表
	- 展示可播放条目
	- 当前选中条目高亮
	- 点击条目切换播放

### 音效实验室（二级页面）

- 与主页面分层，便于实验功能迭代
- 展示会话摘要与效果开关
- 已适配系统状态栏与导航栏 inset，避免顶部遮挡

## 数据来源与扫描策略（当前）

- 本地扫描：仅读取外部存储 `Music` 目录下音频
- 不扫描 `Download` 目录
- 播放列表不再使用本地缓存文件，也不再使用进程内列表缓存

## 循环模式定义

- **Repeat One（单曲循环）**：当前曲目结束后继续播放当前曲目
- **Repeat All（列表循环）**：按列表顺序播放，最后一首后回到第一首
- **Shuffle（随机播放）**：随机选择下一首

## 模块结构（核心）

- `app/src/main/java/com/panda/audioplayer/MainActivity.kt`
	- 主页面交互、列表控制、播放状态联动
- `app/src/main/java/com/panda/audioplayer/ExoPlayerManager.kt`
	- ExoPlayer 播放控制与状态持久化
- `app/src/main/java/com/panda/audioplayer/AudioFileManager.kt`
	- 本地音频扫描（Music 目录）
- `app/src/main/java/com/panda/audioplayer/PlaylistManager.kt`
	- 列表与循环策略管理
- `app/src/main/java/com/panda/audioplayer/EffectLabActivity.kt`
	- 音效实验室页面
- `app/src/main/java/com/panda/audioplayer/utils/Logger.kt`
	- 分级日志（文件、行号、方法名）

## 运行与验证

- 打开工程后同步 Gradle
- 构建 `app` 模块并安装到设备
- 验证项建议：
	- 首次进入默认选中第一条可播放音频
	- 循环模式三态切换正确
	- 列表点击切歌、上一曲/下一曲行为正确
	- 进度条拖拽与时间显示同步

## 说明：AudioTrack 与 C 算法处理

当前分支使用 ExoPlayer 作为主播放链路。

若后续需要恢复“AudioTrack + Native(C/C++) 处理”方案，建议遵循：

- 在选定音源后再初始化 `AudioTrackManager`
- 初始化参数需与当前音源采样率/声道/位深匹配
- 避免在应用启动时固定初始化（例如固定 44100Hz）导致冲突

## 后续优化建议

- 增加“播放列表空态/异常态”提示
- 增强短音频 completion 判定，减少误跳曲
- 增加更细粒度的播放错误可视化提示
- 按需补充测试用例（列表切歌、循环模式、恢复播放）

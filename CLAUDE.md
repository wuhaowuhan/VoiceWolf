# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指导。

## 构建命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 运行单元测试
./gradlew test

# 运行 Android 仪器测试（需要连接设备/模拟器）
./gradlew connectedAndroidTest

# 清理构建产物
./gradlew clean
```

## 架构概览

VoiceWolf 是一款用于线下狼人杀游戏中记录发言与投票的 Android 应用。

### 入口流程

`SetupActivity`（启动页）→ 用户先选择游戏人数（12–15），再选择局设预设 → `MainActivity` 通过 Intent 额外参数（`SETUP_NAME`、`PLAYER_COUNT`）接收配置。人数在进入 MainActivity 后即固定，不再动态增减。

### 核心文件

- **SetupActivity.kt**：启动 Activity。顶部展示人数选择按钮（12–15，4个均分宽度），下方展示局设卡片，将人数和局设选择传递给 MainActivity。
- **GameSetup.kt**：`GameSetup` 数据类 + `SetupRole` + 4 个硬编码预设（狼王守卫、预女猎白、狼美骑士、通灵师机械狼）。`hasRole()` 用于过滤发言模板和标记身份弹窗。
- **MainActivity.kt**：全部游戏 UI。玩家卡片渲染、所有弹窗、模板系统、天数导航。
- **GameViewModel.kt**：通过 `LiveData` 管理所有游戏状态。天数计数器、玩家列表、发言/投票记录、显示文本。`initializePlayers(count)` 方法根据人数初始化玩家列表。
- **Player.kt**：`Player`、`SpeechRecord`、`VoteRecord` 数据类，`MarkedRole` 枚举（13 个值），`Role` 枚举（11 个值）。

### MVVM 架构

- ViewModel 中使用 `MutableLiveData`，以不可变 `LiveData` 对外暴露；Activity 负责观察
- 纯内存存储——应用重启后数据清空
- `allRecordsText: LiveData<String>` 在每次数据变化时通过 `updateDisplayInfo()` 重建

### 天数系统

- 第 0 天 = **警上**（游戏开始前的警徽竞选阶段），第 1 天起 = **第X天**
- 始终使用 `getDayDisplayText(day)` 辅助方法——禁止直接展示原始整数
- `currentDay` 初始化为 0；`prevDay()` 下限为 0；重置后回到 0
- 跳转天数弹窗接受 0（即警上）

### 玩家人数

- 范围：12–15 人，默认 12 人；`MIN_PLAYERS = 12`，`MAX_PLAYERS = 15`
- **人数在 SetupActivity 一次性选择确定，进入 MainActivity 后不再增减**
- 主界面无加减按钮——人数已固定
- 重置后返回 SetupActivity 重新选择，或保留当前局设重新开始

### 核心数据模型

- `SpeechRecord(day, playerId, summary, timestamp)` — 每名玩家每天一条（更新插入）
- `VoteRecord(day, voterId, targetId, timestamp)` — `targetId = 0` 表示弃票
- `Player.MarkedRole`：用户分析标记（预言、女巫、狼人等）
- `Player.isAlive`：死亡玩家为 false；`Player.isActive`：是否参与本局（动态人数保留字段）

### 弹窗清单

| 弹窗 | 触发方式 | 用途 |
|---|---|---|
| 玩家操作菜单 | 点击玩家卡片 | 上下文菜单：记录发言、查看投票、标记身份、清除身份标记 |
| 标记身份（`dialog_mark_role.xml`） | → 标记身份 / 铅笔图标 | FlexboxLayout 网格，按好人/狼人阵营分组，按当前局设过滤 |
| 记录发言（`dialog_add_speech.xml`） | 发言按钮 / 玩家操作菜单 | 模板按钮区（按角色分组）+ 玩家 Spinner + 文本输入框 |
| 记录投票（`dialog_add_vote.xml`） | 投票按钮 | 目标 Spinner + 投票人复选框 GridLayout（动态人数） |
| 单选弹窗（`dialog_select_number.xml`） | 模板：银水X、毒X、盾X、保X、踩X 等 | 动态生成当前玩家按钮网格，点击单个玩家后自动插入文本并关闭 |
| 多选弹窗（`dialog_select_numbers_multi.xml`） | 模板：警徽流、保X、踩X、狼在X | 动态生成玩家按钮，可多选，实时显示已选内容，确认后插入 |
| 查验弹窗（`dialog_select_check.xml`） | 模板：查验X | 两步：选玩家 + 选好人/狼人结果，确认后插入如"查验3号 狼人" |
| 历史记录（`dialog_history.xml`） | 菜单 → 查看历史 | ScrollView 展示所有天数；Emoji 分隔符 + 投票统计 |
| 菜单 | 菜单按钮 | 查看历史、跳转到指定天 |
| 跳转天数 | 菜单 → 跳转 | 输入 0–N 直接设置天数 |
| 重置确认 | 重置按钮 | 选择：保留局设重开 或 返回 SetupActivity |

### 发言模板系统

`TemplateButton` 数据类：`text`（按钮文字）、`category`（分组标签）、`role`（关联角色，null = 通用）、`actionType`（操作类型）。

4 种操作类型：
- `DIRECT_INSERT` — 直接插入文本（如：跳预言家）
- `SINGLE_SELECT` — 打开单选弹窗，动态显示当前玩家列表
- `MULTI_SELECT` — 打开多选弹窗，动态显示当前玩家列表
- `CHECK_SELECT` — 打开查验弹窗（选玩家 + 好人/狼人）

模板按角色分组展示，通过 `currentSetup.hasRole()` 过滤：版型中不存在的角色对应模板整组隐藏；通用类模板（保X、踩X、狼在X）始终显示。

`createTemplateButtons(container, setup, editText)` 方法动态生成按钮区，追加文字调用 `appendToEditText()`。

### 玩家卡片交互

- **点击**：弹出玩家操作菜单（记录发言 / 标记身份 / 清除标记）
- **长按**：切换存活/出局状态（`setPlayerAlive()`）
- **铅笔图标**（右上角 `btnMarkRole`）：直接弹出标记身份弹窗，无需经过菜单

### UI 布局

`activity_main.xml` 为三列布局：
- 左列：玩家 1–6（静态，通过 `<include>` 引用 `item_player.xml`）
- 中间：展示记录文本（`allRecordsText`）的可滚动 CardView
- 右列：动态 `LinearLayout`（`rightColumnContainer`），由 `renderRightColumn()` 根据当前人数重建玩家 7–N 的卡片
- 底部：4 个按钮（+ 发言、+ 投票、下一天、重置）；**无加减玩家按钮**

玩家卡片（`item_player.xml`）：51×51dp 的 `MaterialCardView`；死亡遮罩（X 标记）；右上角铅笔按钮（`btnMarkRole`）用于快捷标记身份；底部角色文字徽章（按角色配色）。

`renderRightColumn()` 在以下时机调用：进入 MainActivity 时（`setupPlayerViews()`）、ViewModel 玩家列表变化时。

### SetupActivity 人数选择 UI

- 4 个按钮使用 `layout_weight` 均分屏幕宽度
- 选中状态：亮青色背景（`teal_200`）+ 黑色文字 + 无边框
- 未选中状态：深色背景（`info_panel`）+ 灰色文字 + 边框
- `selectedPlayerCount` 变量记录当前选择，`selectPlayerCount()` 处理选中切换

### 角色徽章颜色

阵营边框：好人 = `#22C55E`（`border_good`），狼人 = `#DC2626`（`border_werewolf`）。各角色独立徽章颜色（蓝=预言家、紫=女巫、红=狼人等）定义于 `MainActivity.kt` 的 `updatePlayerView()` 方法。

## 项目配置

- 使用阿里云/腾讯云 Maven 镜像（settings.gradle.kts）——在中国构建时必须保留
- Kotlin 1.9.20，Android Gradle Plugin 8.2.0
- minSdk 24，targetSdk 34，versionName "0.1"
- 关键依赖：`flexbox:3.0.0`（身份标记弹窗自动换行）、`gridlayout:1.0.0`、Material 1.11.0、Lifecycle 2.7.0
- 仅支持竖屏（AndroidManifest.xml）
- 已启用 ViewBinding——通过 `binding.viewId` 访问视图

## UI 语言

所有面向用户的文字均为中文（狼人杀术语）。新增功能时须保持一致。

### 发言模板（按角色分组）

| 分组 | 模板按钮 | 显示条件 |
|------|---------|---------|
| 预言家 | 跳预言家、查验X号（好人/狼人）、警徽流X号 | 版型含预言家 |
| 通灵师 | 跳通灵师 | 版型含通灵师 |
| 女巫 | 跳女巫、银水X号、毒X号 | 版型含女巫 |
| 守卫 | 跳守卫、盾X号 | 版型含守卫 |
| 猎人 | 跳猎人 | 版型含猎人 |
| 骑士 | 跳骑士 | 版型含骑士 |
| 白痴 | 跳白痴 | 版型含白痴 |
| 狼王 | 跳狼王 | 版型含狼王 |
| 狼美人 | 跳狼美人 | 版型含狼美人 |
| 机械狼 | 跳机械狼 | 版型含机械狼 |
| 通用 | 跳平民、保X号、踩X号、觉得狼在X号 | 始终显示 |

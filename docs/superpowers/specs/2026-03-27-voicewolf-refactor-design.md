---
name: VoiceWolf 架构重构与交互优化
description: 狼人杀面杀辅助器 - Jetpack Compose 架构重构与交互体验优化
type: project
---

# VoiceWolf 架构重构与交互优化设计

## 项目背景

VoiceWolf 是一款狼人杀面杀辅助器，主要功能是记录玩家发言、投票情况和对玩家身份的标记。现有版本存在以下问题：

- **交互繁琐**：添加发言/投票需要多次点击，流程不够流畅
- **信息查看不便**：难以快速回顾玩家发言模式或投票倾向
- **架构臃肿**：MainActivity 450+ 行，职责混乱，难以维护和扩展
- **数据持久化缺失**：游戏数据只在内存中

## 核心目标

1. 优化发言记录交互：标签选择 + 快速摘要
2. 新增玩家画像：单个玩家的完整历史记录
3. 新增投票关系图：可视化投票联盟和矛盾点
4. 架构重构：Jetpack Compose + MVVM + StateFlow

## 一、整体架构

```
┌─────────────────────────────────────────┐
│              UI Layer (Compose)          │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐    │
│  │PlayersTab│ │VoteGraph │ │ History │   │
│  │         │ │   Tab    │ │   Tab   │    │
│  └────┬────┘ └────┬────┘ └────┬────┘    │
│       │           │           │          │
│       └───────────┼───────────┘          │
│                   ▼                      │
│            GameViewModel                 │
│         (单一状态源)                      │
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│              Domain Layer                │
│  ┌──────────┐ ┌──────────┐ ┌─────────┐  │
│  │ Player   │ │ Speech   │ │  Vote   │  │
│  │ Model    │ │ Record   │ │ Record  │  │
│  └──────────┘ └──────────┘ └─────────┘  │
└─────────────────────────────────────────┘
```

**核心原则：**
- **单一 ViewModel** - 所有游戏状态集中管理，避免状态分散
- **单向数据流** - UI 只读状态，通过事件修改状态
- **Compose 状态驱动** - UI 自动响应状态变化

## 二、页面导航设计

采用底部标签页导航，三个主要页面：

| Tab | 名称 | 核心功能 |
|-----|------|----------|
| 1 | 玩家列表 | 玩家卡片、发言记录入口、玩家画像入口 |
| 2 | 投票关系图 | 当天投票可视化、历史投票查看 |
| 3 | 历史记录 | 按天/按玩家查看所有记录 |

## 三、各页面功能设计

### 3.1 玩家列表页（PlayersTab）

**核心功能：**
- 12 个玩家卡片网格布局（3x4 或 2x6）
- 显示：玩家号、身份标记、存活状态
- 点击 → 记录发言（标签+摘要）
- 长按 → 查看玩家画像

**顶部信息栏：**
- 当前天数
- 快捷操作：下一天、重置

### 3.2 投票关系图页（VoteGraphTab）

**核心功能：**
- 12 个玩家节点 3x4 网格排列
- 箭头表示投票方向（A→B = A 投给 B）
- 实时更新，当天投票一目了然
- 天数切换下拉框查看历史

**节点显示：**
- 玩家号 + 身份标记颜色
- 死亡玩家：灰色 + 删除线

**底部统计：**
- 票数排行
- 弃票名单

### 3.3 历史记录页（HistoryTab）

**核心功能：**
- 按天分组展示所有发言和投票
- 支持按玩家筛选
- 投票统计汇总

## 四、发言记录交互设计

### 弹窗结构
```
┌────────────────────────────┐
│  记录发言 - 3号 - 第2天     │
├────────────────────────────┤
│  快捷标签：                 │
│  [跳身份] [站边] [攻击]     │
│  [辩护] [划水] [爆狼]       │
├────────────────────────────┤
│  摘要（可选）：             │
│  ┌──────────────────────┐  │
│  │ 跳预言家，查杀7号     │  │
│  └──────────────────────┘  │
├────────────────────────────┤
│        [取消]    [保存]     │
└────────────────────────────┘
```

### 标签列表
| 标签 | 说明 |
|------|------|
| 跳身份 | 跳预言家、跳女巫、跳猎人等 |
| 站边 | 支持某玩家/阵营 |
| 攻击 | 质疑、指责某玩家 |
| 辩护 | 为自己或他人辩解 |
| 划水 | 发言无实质内容 |
| 爆狼 | 发言有明显狼人特征 |
| 自爆 | 主动暴露身份 |

### 保存格式
```
标签 + 摘要，例如：
"跳身份 | 跳预言家，查杀7号"
"站边 | 站边3号预言家"
"攻击 | 质疑5号逻辑矛盾"
```

## 五、玩家画像设计

### 入口
- 长按玩家卡片 → 弹出玩家画像底部抽屉

### 界面结构
```
┌────────────────────────────┐
│  3号玩家            [编辑标记]│
│  身份标记：预言家 ✓          │
├────────────────────────────┤
│  第1天                      │
│  发言：跳身份 | 跳预言家     │
│  投票：投给7号               │
├────────────────────────────┤
│  第2天                      │
│  发言：攻击 | 质疑5号逻辑    │
│  投票：投给5号               │
├────────────────────────────┤
│  第3天                      │
│  发言：辩护 | 替7号辩解      │
│  投票：弃票                  │
└────────────────────────────┘
```

### 核心信息
- 基本信息：玩家号、当前身份标记、存活状态
- 历史发言：按天列出所有发言记录
- 投票历史：按天列出投票对象
- 快捷操作：修改身份标记

## 六、数据模型设计

### Player（玩家）
```kotlin
data class Player(
    val id: Int,
    val name: String = "玩家$id",
    val isAlive: Boolean = true,
    val markedRole: MarkedRole = MarkedRole.NONE
)

enum class MarkedRole {
    NONE, SEER, GOOD, WEREWOLF, VILLAGER, WITCH, HUNTER, GUARD, MECHANICAL_WOLF
}
```

### SpeechRecord（发言记录）
```kotlin
data class SpeechRecord(
    val day: Int,
    val playerId: Int,
    val tags: List<SpeechTag>,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SpeechTag {
    CLAIM_ROLE,    // 跳身份
    TAKE_SIDE,     // 站边
    ATTACK,        // 攻击
    DEFEND,        // 辩护
    FISHING,       // 划水
    WOLF_TELL,     // 爆狼
    SELF_DESTRUCT  // 自爆
}
```

### VoteRecord（投票记录）
```kotlin
data class VoteRecord(
    val day: Int,
    val voterId: Int,
    val targetId: Int,   // 0 = 弃票
    val timestamp: Long = System.currentTimeMillis()
)
```

### GameState（游戏状态）
```kotlin
data class GameState(
    val currentDay: Int = 1,
    val players: List<Player> = (1..12).map { Player(id = it) },
    val speechRecords: List<SpeechRecord> = emptyList(),
    val voteRecords: List<VoteRecord> = emptyList()
)
```

## 七、ViewModel 设计

```kotlin
class GameViewModel : ViewModel() {

    // 单一状态源
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // 玩家操作
    fun setPlayerMarkedRole(playerId: Int, role: MarkedRole)
    fun setPlayerAlive(playerId: Int, isAlive: Boolean)

    // 发言操作
    fun addSpeechRecord(record: SpeechRecord)
    fun updateSpeechRecord(record: SpeechRecord)
    fun deleteSpeechRecord(day: Int, playerId: Int)

    // 投票操作
    fun recordVote(voterId: Int, targetId: Int, day: Int)
    fun deleteVoteRecord(day: Int, voterId: Int)

    // 天数操作
    fun nextDay()
    fun setDay(day: Int)

    // 游戏操作
    fun resetGame()

    // 查询方法
    fun getPlayerById(id: Int): Player?
    fun getSpeechRecordsForPlayer(playerId: Int): List<SpeechRecord>
    fun getVoteRecordsForPlayer(playerId: Int): List<VoteRecord>
    fun getVoteRecordsForDay(day: Int): List<VoteRecord>
}
```

### 状态更新原则
- 所有状态修改通过方法调用
- 使用 `copy()` 更新不可变状态
- UI 通过 `collectAsState()` 响应变化

## 八、Compose 文件结构

```
app/src/main/java/com/voicewolf/app/
├── VoiceWolfApp.kt              // Application 类
├── MainActivity.kt              // 入口 Activity（精简）
├── GameViewModel.kt             // ViewModel
├── model/
│   ├── Player.kt                // 数据模型
│   ├── SpeechRecord.kt
│   ├── VoteRecord.kt
│   └── GameState.kt
└── ui/
    ├── theme/
    │   ├── Color.kt             // 颜色定义
    │   ├── Theme.kt             // 主题配置
    │   └── Type.kt              // 字体样式
    ├── components/
    │   ├── PlayerCard.kt        // 玩家卡片组件
    │   ├── SpeechDialog.kt      // 发言记录弹窗
    │   ├── PlayerProfileSheet.kt // 玩家画像底部抽屉
    │   ├── VoteGraphView.kt     // 投票关系图
    │   └── SpeechTagChip.kt     // 标签选择组件
    └── screens/
        ├── MainScreen.kt        // 主界面（底部导航）
        ├── PlayersTab.kt        // 玩家列表页
        ├── VoteGraphTab.kt      // 投票关系图页
        └── HistoryTab.kt        // 历史记录页
```

### 职责划分
- **screens/** - 页面级 Composable，负责布局组合
- **components/** - 可复用组件，单一职责
- **theme/** - 视觉风格统一管理

## 九、文件变更清单

| 操作 | 文件/目录 | 说明 |
|------|-----------|------|
| 删除 | MainActivity.kt | 旧实现 450+ 行 |
| 删除 | activity_main.xml | XML 布局文件 |
| 删除 | item_player.xml | XML 布局文件 |
| 删除 | dialog_*.xml | 对话框布局文件 |
| 新增 | ui/screens/*.kt | 3 个页面 |
| 新增 | ui/components/*.kt | 5 个组件 |
| 新增 | ui/theme/*.kt | 主题配置 |
| 重构 | GameViewModel.kt | StateFlow 状态管理 |
| 重构 | model/*.kt | 数据模型 |
| 修改 | build.gradle.kts | 添加 Compose 依赖 |

## 十、技术选型

| 技术 | 用途 | 版本 |
|------|------|------|
| Jetpack Compose | UI 框架 | 最新稳定版 |
| Material3 | 设计系统 | 最新稳定版 |
| Kotlin Flow | 响应式状态 | 内置 |
| ViewModel | 状态持有 | 最新稳定版 |

## 十一、后续规划

以下功能本次不实现，后续迭代考虑：

1. **数据持久化** - Room 本地存储游戏记录
2. **云同步** - 跨设备查看历史对局
3. **AI 分析** - 发言内容智能分析
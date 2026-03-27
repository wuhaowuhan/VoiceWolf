# 狼影 (WolfShadow) 架构重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将狼人杀面杀辅助器从 XML 布局重构为 Jetpack Compose，优化交互体验。

**Architecture:** 单一 GameViewModel + StateFlow 状态管理，三个标签页（玩家列表、投票关系图、历史记录），声明式 UI。

**Tech Stack:** Jetpack Compose, Material3, Kotlin Flow, ViewModel

---

## 文件结构

```
app/src/main/java/com/wolfshadow/app/
├── MainActivity.kt              # 精简的入口 Activity
├── GameViewModel.kt             # 状态管理
├── model/
│   ├── Player.kt                # 玩家模型
│   ├── MarkedRole.kt            # 身份标记枚举
│   ├── SpeechRecord.kt          # 发言记录
│   ├── SpeechTag.kt             # 发言标签枚举
│   ├── VoteRecord.kt            # 投票记录
│   └── GameState.kt             # 游戏状态
└── ui/
    ├── theme/
    │   ├── Color.kt             # 颜色定义
    │   ├── Theme.kt             # 主题配置
    │   └── Type.kt              # 字体样式
    ├── components/
    │   ├── PlayerCard.kt        # 玩家卡片
    │   ├── SpeechDialog.kt      # 发言弹窗
    │   ├── PlayerProfileSheet.kt # 玩家画像
    │   ├── VoteGraphView.kt     # 投票关系图
    │   └── SpeechTagChip.kt     # 标签选择器
    └── screens/
        ├── MainScreen.kt        # 主界面
        ├── PlayersTab.kt        # 玩家列表页
        ├── VoteGraphTab.kt      # 投票关系图页
        └── HistoryTab.kt        # 历史记录页
```

---

## Task 1: 配置 Compose 依赖

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: 更新 build.gradle.kts 添加 Compose 配置**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wolfshadow.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.wolfshadow.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

- [ ] **Step 2: 同步 Gradle**

Run: `cd "D:/Android project/VoiceWolf" && ./gradlew --version`

Expected: Gradle 版本信息

- [ ] **Step 3: 提交**

```bash
git add app/build.gradle.kts
git commit -m "build: 添加 Jetpack Compose 依赖配置

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 2: 创建数据模型

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/model/MarkedRole.kt`
- Create: `app/src/main/java/com/wolfshadow/app/model/Player.kt`
- Create: `app/src/main/java/com/wolfshadow/app/model/SpeechTag.kt`
- Create: `app/src/main/java/com/wolfshadow/app/model/SpeechRecord.kt`
- Create: `app/src/main/java/com/wolfshadow/app/model/VoteRecord.kt`
- Create: `app/src/main/java/com/wolfshadow/app/model/GameState.kt`

- [ ] **Step 1: 创建 model 目录**

```bash
mkdir -p "D:/Android project/VoiceWolf/app/src/main/java/com/wolfshadow/app/model"
```

- [ ] **Step 2: 创建 MarkedRole.kt**

```kotlin
package com.wolfshadow.app.model

/**
 * 玩家身份标记
 */
enum class MarkedRole(val displayName: String, val colorKey: String) {
    NONE("", "gray"),
    SEER("预言", "seer"),
    GOOD("好人", "good"),
    WEREWOLF("狼人", "werewolf"),
    VILLAGER("平民", "villager"),
    WITCH("女巫", "witch"),
    HUNTER("猎人", "hunter"),
    GUARD("守卫", "guard"),
    MECHANICAL_WOLF("机械狼", "mech_wolf");

    fun isEvil(): Boolean = this in listOf(WEREWOLF, MECHANICAL_WOLF)
}
```

- [ ] **Step 3: 创建 Player.kt**

```kotlin
package com.wolfshadow.app.model

/**
 * 玩家模型
 */
data class Player(
    val id: Int,
    val name: String = "玩家$id",
    val isAlive: Boolean = true,
    val markedRole: MarkedRole = MarkedRole.NONE
)
```

- [ ] **Step 4: 创建 SpeechTag.kt**

```kotlin
package com.wolfshadow.app.model

/**
 * 发言标签
 */
enum class SpeechTag(val displayName: String) {
    CLAIM_ROLE("跳身份"),
    TAKE_SIDE("站边"),
    ATTACK("攻击"),
    DEFEND("辩护"),
    FISHING("划水"),
    WOLF_TELL("爆狼"),
    SELF_DESTRUCT("自爆")
}
```

- [ ] **Step 5: 创建 SpeechRecord.kt**

```kotlin
package com.wolfshadow.app.model

/**
 * 发言记录
 */
data class SpeechRecord(
    val day: Int,
    val playerId: Int,
    val tags: List<SpeechTag>,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formatDisplay(): String {
        val tagsText = if (tags.isNotEmpty()) {
            tags.joinToString("、") { it.displayName }
        } else {
            ""
        }
        return if (tagsText.isNotEmpty() && summary.isNotEmpty()) {
            "$tagsText | $summary"
        } else if (tagsText.isNotEmpty()) {
            tagsText
        } else {
            summary
        }
    }
}
```

- [ ] **Step 6: 创建 VoteRecord.kt**

```kotlin
package com.wolfshadow.app.model

/**
 * 投票记录
 */
data class VoteRecord(
    val day: Int,
    val voterId: Int,
    val targetId: Int,  // 0 = 弃票
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isAbstain(): Boolean = targetId == 0
}
```

- [ ] **Step 7: 创建 GameState.kt**

```kotlin
package com.wolfshadow.app.model

/**
 * 游戏状态
 */
data class GameState(
    val currentDay: Int = 1,
    val players: List<Player> = (1..12).map { Player(id = it) },
    val speechRecords: List<SpeechRecord> = emptyList(),
    val voteRecords: List<VoteRecord> = emptyList()
) {
    fun getAllDays(): List<Int> {
        val speechDays = speechRecords.map { it.day }
        val voteDays = voteRecords.map { it.day }
        return (speechDays + voteDays).distinct().sorted()
    }
}
```

- [ ] **Step 8: 删除旧的 Player.kt**

```bash
rm "D:/Android project/VoiceWolf/app/src/main/java/com/wolfshadow/app/Player.kt"
```

- [ ] **Step 9: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/model/
git add app/src/main/java/com/wolfshadow/app/Player.kt
git commit -m "feat: 创建数据模型 (MarkedRole, Player, SpeechTag, SpeechRecord, VoteRecord, GameState)

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 3: 创建 GameViewModel

**Files:**
- Replace: `app/src/main/java/com/wolfshadow/app/GameViewModel.kt`

- [ ] **Step 1: 重写 GameViewModel.kt**

```kotlin
package com.wolfshadow.app

import androidx.lifecycle.ViewModel
import com.wolfshadow.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {

    // 单一状态源
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // 便捷访问
    val currentDay: Int get() = _gameState.value.currentDay
    val players: List<Player> get() = _gameState.value.players
    val speechRecords: List<SpeechRecord> get() = _gameState.value.speechRecords
    val voteRecords: List<VoteRecord> get() = _gameState.value.voteRecords

    // ========== 玩家操作 ==========

    fun setPlayerMarkedRole(playerId: Int, role: MarkedRole) {
        _gameState.update { state ->
            state.copy(
                players = state.players.map { player ->
                    if (player.id == playerId) player.copy(markedRole = role) else player
                }
            )
        }
    }

    fun setPlayerAlive(playerId: Int, isAlive: Boolean) {
        _gameState.update { state ->
            state.copy(
                players = state.players.map { player ->
                    if (player.id == playerId) player.copy(isAlive = isAlive) else player
                }
            )
        }
    }

    // ========== 发言操作 ==========

    fun addSpeechRecord(record: SpeechRecord) {
        _gameState.update { state ->
            // 先删除同一天同一玩家的记录
            val filtered = state.speechRecords.filterNot {
                it.day == record.day && it.playerId == record.playerId
            }
            state.copy(speechRecords = filtered + record)
        }
    }

    fun deleteSpeechRecord(day: Int, playerId: Int) {
        _gameState.update { state ->
            state.copy(
                speechRecords = state.speechRecords.filterNot {
                    it.day == day && it.playerId == playerId
                }
            )
        }
    }

    // ========== 投票操作 ==========

    fun recordVote(voterId: Int, targetId: Int, day: Int) {
        _gameState.update { state ->
            // 先删除该投票者当天的记录
            val filtered = state.voteRecords.filterNot {
                it.voterId == voterId && it.day == day
            }
            // 如果不是弃票，添加新记录
            val newRecords = if (targetId > 0) {
                filtered + VoteRecord(day = day, voterId = voterId, targetId = targetId)
            } else {
                filtered
            }
            state.copy(voteRecords = newRecords)
        }
    }

    fun deleteVoteRecord(day: Int, voterId: Int) {
        _gameState.update { state ->
            state.copy(
                voteRecords = state.voteRecords.filterNot {
                    it.day == day && it.voterId == voterId
                }
            )
        }
    }

    // ========== 天数操作 ==========

    fun nextDay() {
        _gameState.update { state ->
            state.copy(currentDay = state.currentDay + 1)
        }
    }

    fun prevDay() {
        _gameState.update { state ->
            if (state.currentDay > 1) {
                state.copy(currentDay = state.currentDay - 1)
            } else {
                state
            }
        }
    }

    fun setDay(day: Int) {
        if (day >= 1) {
            _gameState.update { state ->
                state.copy(currentDay = day)
            }
        }
    }

    // ========== 游戏操作 ==========

    fun resetGame() {
        _gameState.value = GameState()
    }

    // ========== 查询方法 ==========

    fun getPlayerById(id: Int): Player? {
        return _gameState.value.players.find { it.id == id }
    }

    fun getSpeechRecordsForPlayer(playerId: Int): List<SpeechRecord> {
        return _gameState.value.speechRecords
            .filter { it.playerId == playerId }
            .sortedBy { it.day }
    }

    fun getSpeechRecordsForDay(day: Int): List<SpeechRecord> {
        return _gameState.value.speechRecords
            .filter { it.day == day }
            .sortedBy { it.playerId }
    }

    fun getSpeechRecord(day: Int, playerId: Int): SpeechRecord? {
        return _gameState.value.speechRecords.find {
            it.day == day && it.playerId == playerId
        }
    }

    fun getVoteRecordsForPlayer(playerId: Int): List<VoteRecord> {
        return _gameState.value.voteRecords
            .filter { it.voterId == playerId }
            .sortedBy { it.day }
    }

    fun getVoteRecordsForDay(day: Int): List<VoteRecord> {
        return _gameState.value.voteRecords
            .filter { it.day == day }
            .sortedBy { it.voterId }
    }

    fun getVoteCountsForDay(day: Int): Map<Int, Int> {
        return getVoteRecordsForDay(day)
            .filter { !it.isAbstain() }
            .groupingBy { it.targetId }
            .eachCount()
    }

    fun getAbstainVotersForDay(day: Int): List<Int> {
        return getVoteRecordsForDay(day)
            .filter { it.isAbstain() }
            .map { it.voterId }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/GameViewModel.kt
git commit -m "refactor: 重构 GameViewModel 使用 StateFlow

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 4: 创建主题配置

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/theme/Color.kt`
- Create: `app/src/main/java/com/wolfshadow/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/wolfshadow/app/ui/theme/Type.kt`

- [ ] **Step 1: 创建 theme 目录**

```bash
mkdir -p "D:/Android project/VoiceWolf/app/src/main/java/com/wolfshadow/app/ui/theme"
```

- [ ] **Step 2: 创建 Color.kt**

```kotlin
package com.wolfshadow.app.ui.theme

import androidx.compose.ui.graphics.Color

// 基础色
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// 主题色 - 深色模式
val BackgroundDark = Color(0xFF1A1A2E)
val SurfaceDark = Color(0xFF16213E)
val PrimaryDark = Color(0xFF0F3460)
val SecondaryDark = Color(0xFFE94560)

// 面板背景
val InfoPanel = Color(0xFF1E2A47)

// 文字色
val White = Color(0xFFFFFFFF)
val GrayLight = Color(0xFFB0B0B0)
val GrayDark = Color(0xFF666666)

// 身份角色颜色
val RoleSeer = Color(0xFF4FC3F7)       // 预言家 - 蓝
val RoleWitch = Color(0xFFBA68C8)      // 女巫 - 紫
val RoleHunter = Color(0xFFFF7043)     // 猎人 - 橙
val RoleGuard = Color(0xFF66BB6A)      // 守卫 - 绿
val RoleGood = Color(0xFF81C784)       // 好人 - 浅绿
val RoleVillager = Color(0xFF90A4AE)   // 平民 - 灰蓝
val RoleWerewolf = Color(0xFFE53935)   // 狼人 - 红
val RoleMechWolf = Color(0xFF8B0000)   // 机械狼 - 深红

// 状态颜色
val StatusAlive = Color(0xFF4CAF50)
val StatusDead = Color(0xFF9E9E9E)
```

- [ ] **Step 3: 创建 Theme.kt**

```kotlin
package com.wolfshadow.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    tertiary = Pink80,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = White,
    onSurface = White
)

@Composable
fun WolfShadowTheme(
    darkTheme: Boolean = true,  // 强制深色模式
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

- [ ] **Step 4: 创建 Type.kt**

```kotlin
package com.wolfshadow.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
```

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/theme/
git commit -m "feat: 创建 Compose 主题配置 (Color, Theme, Type)

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 5: 创建玩家卡片组件

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/components/PlayerCard.kt`

- [ ] **Step 1: 创建 components 目录**

```bash
mkdir -p "D:/Android project/VoiceWolf/app/src/main/java/com/wolfshadow/app/ui/components"
```

- [ ] **Step 2: 创建 PlayerCard.kt**

```kotlin
package com.wolfshadow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.MarkedRole
import com.wolfshadow.app.model.Player
import com.wolfshadow.app.ui.theme.*

@Composable
fun PlayerCard(
    player: Player,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (player.markedRole) {
        MarkedRole.SEER -> RoleSeer
        MarkedRole.WITCH -> RoleWitch
        MarkedRole.HUNTER -> RoleHunter
        MarkedRole.GUARD -> RoleGuard
        MarkedRole.GOOD -> RoleGood
        MarkedRole.VILLAGER -> RoleVillager
        MarkedRole.WEREWOLF -> RoleWerewolf
        MarkedRole.MECHANICAL_WOLF -> RoleMechWolf
        MarkedRole.NONE -> InfoPanel
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .then(
                if (!player.isAlive) {
                    Modifier.alpha(0.5f)
                } else {
                    Modifier
                }
            )
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .border(
                width = if (player.markedRole != MarkedRole.NONE) 2.dp else 1.dp,
                color = if (player.markedRole != MarkedRole.NONE) White.copy(alpha = 0.3f) else GrayDark,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 玩家编号
            Text(
                text = "${player.id}号",
                style = MaterialTheme.typography.titleMedium,
                color = if (player.isAlive) White else GrayLight,
                textDecoration = if (!player.isAlive) TextDecoration.LineThrough else null
            )

            // 身份标记
            if (player.markedRole != MarkedRole.NONE) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = player.markedRole.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 死亡标记
            if (!player.isAlive) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "出局",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusDead
                )
            }
        }
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/components/PlayerCard.kt
git commit -m "feat: 创建 PlayerCard 组件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 6: 创建发言标签选择组件

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/components/SpeechTagChip.kt`

- [ ] **Step 1: 创建 SpeechTagChip.kt**

```kotlin
package com.wolfshadow.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.SpeechTag
import com.wolfshadow.app.ui.theme.SecondaryDark
import com.wolfshadow.app.ui.theme.White

@Composable
fun SpeechTagSelector(
    selectedTags: List<SpeechTag>,
    onTagsChanged: (List<SpeechTag>) -> Unit,
    modifier: Modifier = Modifier
) {
    var tags by remember(selectedTags) { mutableStateOf(selectedTags.toMutableList()) }

    Column(modifier = modifier) {
        Text(
            text = "快捷标签：",
            style = MaterialTheme.typography.labelLarge,
            color = White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SpeechTag.entries) { tag ->
                val isSelected = tags.contains(tag)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        tags = if (isSelected) {
                            (tags - tag).toMutableList()
                        } else {
                            (tags + tag).toMutableList()
                        }
                        onTagsChanged(tags)
                    },
                    label = {
                        Text(
                            text = tag.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SecondaryDark,
                        selectedLabelColor = White
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/components/SpeechTagChip.kt
git commit -m "feat: 创建 SpeechTagSelector 标签选择组件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 7: 创建发言记录弹窗

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/components/SpeechDialog.kt`

- [ ] **Step 1: 创建 SpeechDialog.kt**

```kotlin
package com.wolfshadow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.SpeechRecord
import com.wolfshadow.app.model.SpeechTag
import com.wolfshadow.app.ui.theme.*

@Composable
fun SpeechDialog(
    playerId: Int,
    day: Int,
    existingRecord: SpeechRecord? = null,
    onDismiss: () -> Unit,
    onSave: (List<SpeechTag>, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTags by remember { mutableStateOf(existingRecord?.tags ?: emptyList()) }
    var summary by remember { mutableStateOf(existingRecord?.summary ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "记录发言 - ${playerId}号 - 第${day}天",
                color = White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SpeechTagSelector(
                    selectedTags = selectedTags,
                    onTagsChanged = { selectedTags = it }
                )

                Column {
                    Text(
                        text = "摘要（可选）：",
                        style = MaterialTheme.typography.labelLarge,
                        color = White.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BasicTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp)
                            .background(InfoPanel, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = White),
                        cursorBrush = SolidColor(White)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selectedTags, summary) }) {
                Text("保存", color = SecondaryDark)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = GrayLight)
            }
        }
    )
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/components/SpeechDialog.kt
git commit -m "feat: 创建 SpeechDialog 发言记录弹窗

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 8: 创建玩家画像组件

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/components/PlayerProfileSheet.kt`

- [ ] **Step 1: 创建 PlayerProfileSheet.kt**

```kotlin
package com.wolfshadow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.*
import com.wolfshadow.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileSheet(
    player: Player,
    speechRecords: List<SpeechRecord>,
    voteRecords: List<VoteRecord>,
    allDays: List<Int>,
    onMarkRole: (MarkedRole) -> Unit,
    onToggleAlive: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRoleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 头部信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${player.id}号玩家",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (player.isAlive) White else GrayLight,
                    textDecoration = if (!player.isAlive) TextDecoration.LineThrough else null
                )
                if (player.markedRole != MarkedRole.NONE) {
                    Text(
                        text = "身份标记：${player.markedRole.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = White.copy(alpha = 0.7f)
                    )
                }
                if (!player.isAlive) {
                    Text(
                        text = "已出局",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusDead
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showRoleDialog = true },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("标记身份", color = White)
                }
                OutlinedButton(
                    onClick = onToggleAlive,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (player.isAlive) "标记出局" else "复活",
                        color = if (player.isAlive) StatusDead else StatusAlive
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = White.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(16.dp))

        // 历史记录
        if (allDays.isEmpty()) {
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayLight,
                modifier = Modifier.padding(vertical = 32.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allDays) { day ->
                    DayRecordCard(
                        day = day,
                        speechRecord = speechRecords.find { it.day == day },
                        voteRecord = voteRecords.find { it.day == day }
                    )
                }
            }
        }
    }

    // 身份标记对话框
    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            containerColor = SurfaceDark,
            title = { Text("标记身份", color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarkedRole.entries.filter { it != MarkedRole.NONE }.forEach { role ->
                        TextButton(
                            onClick = {
                                onMarkRole(role)
                                showRoleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = role.displayName,
                                color = White,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            onMarkRole(MarkedRole.NONE)
                            showRoleDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清除标记", color = GrayLight)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text("取消", color = GrayLight)
                }
            }
        )
    }
}

@Composable
private fun DayRecordCard(
    day: Int,
    speechRecord: SpeechRecord?,
    voteRecord: VoteRecord?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InfoPanel, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "第${day}天",
            style = MaterialTheme.typography.labelLarge,
            color = SecondaryDark
        )
        Spacer(modifier = Modifier.height(8.dp))

        speechRecord?.let { record ->
            Row {
                Text(
                    text = "发言：",
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayLight
                )
                Text(
                    text = record.formatDisplay(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = White
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        voteRecord?.let { record ->
            Row {
                Text(
                    text = "投票：",
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayLight
                )
                Text(
                    text = if (record.isAbstain()) "弃票" else "投给${record.targetId}号",
                    style = MaterialTheme.typography.bodyMedium,
                    color = White
                )
            }
        }

        if (speechRecord == null && voteRecord == null) {
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.bodySmall,
                color = GrayLight
            )
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/components/PlayerProfileSheet.kt
git commit -m "feat: 创建 PlayerProfileSheet 玩家画像组件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 9: 创建投票关系图组件

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/components/VoteGraphView.kt`

- [ ] **Step 1: 创建 VoteGraphView.kt**

```kotlin
package com.wolfshadow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wolfshadow.app.model.*
import com.wolfshadow.app.ui.theme.*

@Composable
fun VoteGraphView(
    players: List<Player>,
    voteRecords: List<VoteRecord>,
    selectedPlayerId: Int? = null,
    onPlayerClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 构建投票关系映射: targetId -> voters
    val votesByTarget = remember(voteRecords) {
        voteRecords
            .filter { !it.isAbstain() }
            .groupBy { it.targetId }
    }

    // 构建投票关系映射: voterId -> targetId
    val votesByVoter = remember(voteRecords) {
        voteRecords.associate { it.voterId to it.targetId }
    }

    Column(modifier = modifier) {
        // 投票网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(players.size) { index ->
                val player = players[index]
                val votesReceived = votesByTarget[player.id]?.size ?: 0
                val votedFor = votesByVoter[player.id]
                val isSelected = selectedPlayerId == player.id

                VoteNodeCard(
                    player = player,
                    votedFor = votedFor,
                    votesReceived = votesReceived,
                    isSelected = isSelected,
                    onClick = { onPlayerClick(player.id) }
                )
            }
        }

        // 投票统计
        if (voteRecords.isNotEmpty()) {
            VoteStatistics(
                voteRecords = voteRecords,
                players = players,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun VoteNodeCard(
    player: Player,
    votedFor: Int?,
    votesReceived: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (player.markedRole) {
        MarkedRole.SEER -> RoleSeer
        MarkedRole.WITCH -> RoleWitch
        MarkedRole.HUNTER -> RoleHunter
        MarkedRole.GUARD -> RoleGuard
        MarkedRole.GOOD -> RoleGood
        MarkedRole.VILLAGER -> RoleVillager
        MarkedRole.WEREWOLF -> RoleWerewolf
        MarkedRole.MECHANICAL_WOLF -> RoleMechWolf
        MarkedRole.NONE -> InfoPanel
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .then(
                if (!player.isAlive) {
                    Modifier.alpha(0.5f)
                } else {
                    Modifier
                }
            )
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, SecondaryDark, RoundedCornerShape(8.dp))
                } else {
                    Modifier.border(1.dp, GrayDark, RoundedCornerShape(8.dp))
                }
            )
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${player.id}号",
                style = MaterialTheme.typography.titleMedium,
                color = if (player.isAlive) White else GrayLight,
                textDecoration = if (!player.isAlive) TextDecoration.LineThrough else null
            )

            if (player.markedRole != MarkedRole.NONE) {
                Text(
                    text = player.markedRole.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 投票信息
            if (votedFor != null && votedFor > 0) {
                Text(
                    text = "→${votedFor}号",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryDark
                )
            }

            // 票数
            if (votesReceived > 0) {
                Text(
                    text = "${votesReceived}票",
                    style = MaterialTheme.typography.labelSmall,
                    color = White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun VoteStatistics(
    voteRecords: List<VoteRecord>,
    players: List<Player>,
    modifier: Modifier = Modifier
) {
    val validVotes = voteRecords.filter { !it.isAbstain() }
    val abstainVotes = voteRecords.filter { it.isAbstain() }

    // 票数统计
    val voteCounts = validVotes.groupingBy { it.targetId }.eachCount()
        .entries.sortedByDescending { it.value }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoPanel, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "投票统计",
            style = MaterialTheme.typography.labelLarge,
            color = White
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (voteCounts.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                voteCounts.forEach { (targetId, count) ->
                    Text(
                        text = "${targetId}号(${count}票)",
                        style = MaterialTheme.typography.labelMedium,
                        color = SecondaryDark
                    )
                }
            }
        }

        if (abstainVotes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "弃票: ${abstainVotes.joinToString(", ") { "${it.voterId}号" }}",
                style = MaterialTheme.typography.labelMedium,
                color = GrayLight
            )
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/components/VoteGraphView.kt
git commit -m "feat: 创建 VoteGraphView 投票关系图组件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 10: 创建玩家列表页

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/screens/PlayersTab.kt`

- [ ] **Step 1: 创建 screens 目录**

```bash
mkdir -p "D:/Android project/VoiceWolf/app/src/main/java/com/wolfshadow/app/ui/screens"
```

- [ ] **Step 2: 创建 PlayersTab.kt**

```kotlin
package com.wolfshadow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfshadow.app.GameViewModel
import com.wolfshadow.app.model.MarkedRole
import com.wolfshadow.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersTab(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    var showSpeechDialog by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var selectedPlayerId by remember { mutableStateOf<Int?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部信息栏
        TopAppBar(
            title = {
                Text(
                    text = "第${gameState.currentDay}天",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            actions = {
                IconButton(onClick = { viewModel.nextDay() }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一天",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Reset,
                        contentDescription = "重置",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // 玩家网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(gameState.players.size) { index ->
                val player = gameState.players[index]
                PlayerCard(
                    player = player,
                    onClick = {
                        selectedPlayerId = player.id
                        showSpeechDialog = true
                    },
                    onLongClick = {
                        selectedPlayerId = player.id
                        showProfileSheet = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 发言记录弹窗
    if (showSpeechDialog && selectedPlayerId != null) {
        val existingRecord = viewModel.getSpeechRecord(
            day = gameState.currentDay,
            playerId = selectedPlayerId!!
        )
        SpeechDialog(
            playerId = selectedPlayerId!!,
            day = gameState.currentDay,
            existingRecord = existingRecord,
            onDismiss = {
                showSpeechDialog = false
                selectedPlayerId = null
            },
            onSave = { tags, summary ->
                viewModel.addSpeechRecord(
                    com.wolfshadow.app.model.SpeechRecord(
                        day = gameState.currentDay,
                        playerId = selectedPlayerId!!,
                        tags = tags,
                        summary = summary
                    )
                )
                showSpeechDialog = false
                selectedPlayerId = null
            }
        )
    }

    // 玩家画像
    if (showProfileSheet && selectedPlayerId != null) {
        val player = viewModel.getPlayerById(selectedPlayerId!!)
        val speechRecords = viewModel.getSpeechRecordsForPlayer(selectedPlayerId!!)
        val voteRecords = viewModel.getVoteRecordsForPlayer(selectedPlayerId!!)

        if (player != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showProfileSheet = false
                    selectedPlayerId = null
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PlayerProfileSheet(
                    player = player,
                    speechRecords = speechRecords,
                    voteRecords = voteRecords,
                    allDays = gameState.getAllDays(),
                    onMarkRole = { role ->
                        viewModel.setPlayerMarkedRole(selectedPlayerId!!, role)
                    },
                    onToggleAlive = {
                        viewModel.setPlayerAlive(selectedPlayerId!!, !player.isAlive)
                    }
                )
            }
        }
    }

    // 重置确认对话框
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("确认重置") },
            text = { Text("确定要重置游戏吗？所有记录将被清空。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetGame()
                    showResetDialog = false
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/screens/PlayersTab.kt
git commit -m "feat: 创建 PlayersTab 玩家列表页

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 11: 创建投票关系图页

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/screens/VoteGraphTab.kt`

- [ ] **Step 1: 创建 VoteGraphTab.kt**

```kotlin
package com.wolfshadow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfshadow.app.GameViewModel
import com.wolfshadow.app.ui.components.VoteGraphView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteGraphTab(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    var selectedDay by remember(gameState.currentDay) { mutableStateOf(gameState.currentDay) }
    var selectedPlayerId by remember { mutableStateOf<Int?>(null) }

    val allDays = gameState.getAllDays()
    val displayDay = if (allDays.contains(selectedDay)) selectedDay else gameState.currentDay
    val voteRecords = viewModel.getVoteRecordsForDay(displayDay)

    Column(modifier = modifier.fillMaxSize()) {
        // 顶部栏
        TopAppBar(
            title = {
                Text("投票关系图")
            },
            actions = {
                // 天数选择
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (selectedDay > 1) selectedDay--
                        },
                        enabled = selectedDay > 1
                    ) {
                        Icon(Icons.Default.ArrowBack, "前一天")
                    }

                    Text(
                        text = "第${displayDay}天",
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(
                        onClick = {
                            if (selectedDay < gameState.currentDay) selectedDay++
                        },
                        enabled = selectedDay < gameState.currentDay
                    ) {
                        Icon(Icons.Default.ArrowForward, "后一天")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // 投票关系图
        if (voteRecords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "第${displayDay}天暂无投票记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            VoteGraphView(
                players = gameState.players,
                voteRecords = voteRecords,
                selectedPlayerId = selectedPlayerId,
                onPlayerClick = { playerId ->
                    selectedPlayerId = if (selectedPlayerId == playerId) null else playerId
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/screens/VoteGraphTab.kt
git commit -m "feat: 创建 VoteGraphTab 投票关系图页

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 12: 创建历史记录页

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/screens/HistoryTab.kt`

- [ ] **Step 1: 创建 HistoryTab.kt**

```kotlin
package com.wolfshadow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfshadow.app.GameViewModel
import com.wolfshadow.app.ui.theme.InfoPanel
import com.wolfshadow.app.ui.theme.SecondaryDark
import com.wolfshadow.app.ui.theme.White
import com.wolfshadow.app.ui.theme.GrayLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTab(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val allDays = gameState.getAllDays()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("历史记录") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (allDays.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "暂无历史记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(allDays) { day ->
                    DayHistoryCard(
                        day = day,
                        speechRecords = viewModel.getSpeechRecordsForDay(day),
                        voteRecords = viewModel.getVoteRecordsForDay(day)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHistoryCard(
    day: Int,
    speechRecords: List<com.wolfshadow.app.model.SpeechRecord>,
    voteRecords: List<com.wolfshadow.app.model.VoteRecord>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(InfoPanel, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "第${day}天",
            style = MaterialTheme.typography.titleMedium,
            color = SecondaryDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 发言记录
        if (speechRecords.isNotEmpty()) {
            Text(
                text = "【发言】",
                style = MaterialTheme.typography.labelLarge,
                color = White
            )
            Spacer(modifier = Modifier.height(8.dp))
            speechRecords.forEach { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "${record.playerId}号：",
                        style = MaterialTheme.typography.labelMedium,
                        color = SecondaryDark,
                        modifier = Modifier.widthIn(min = 40.dp)
                    )
                    Text(
                        text = record.formatDisplay(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = White
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 投票记录
        if (voteRecords.isNotEmpty()) {
            Text(
                text = "【投票】",
                style = MaterialTheme.typography.labelLarge,
                color = White
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 投票详情
            val validVotes = voteRecords.filter { !it.isAbstain() }
            val abstainVotes = voteRecords.filter { it.isAbstain() }

            if (validVotes.isNotEmpty()) {
                Text(
                    text = validVotes.joinToString("  ") {
                        "${it.voterId}→${it.targetId}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = White
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 投票统计
            val voteCounts = validVotes.groupingBy { it.targetId }.eachCount()
            if (voteCounts.isNotEmpty()) {
                Text(
                    text = "统计：" + voteCounts.entries
                        .sortedByDescending { it.value }
                        .joinToString("  ") { "${it.key}号(${it.value}票)" },
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayLight
                )
            }

            if (abstainVotes.isNotEmpty()) {
                Text(
                    text = "弃票：" + abstainVotes.joinToString(", ") { "${it.voterId}号" },
                    style = MaterialTheme.typography.labelMedium,
                    color = GrayLight
                )
            }
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/screens/HistoryTab.kt
git commit -m "feat: 创建 HistoryTab 历史记录页

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 13: 创建主界面

**Files:**
- Create: `app/src/main/java/com/wolfshadow/app/ui/screens/MainScreen.kt`

- [ ] **Step 1: 创建 MainScreen.kt**

```kotlin
package com.wolfshadow.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wolfshadow.app.GameViewModel

@Composable
fun MainScreen(
    viewModel: GameViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("玩家") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.ThumbUp, contentDescription = null) },
                    label = { Text("投票图") }
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("历史") }
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTabIndex) {
                0 -> PlayersTab(viewModel = viewModel)
                1 -> VoteGraphTab(viewModel = viewModel)
                2 -> HistoryTab(viewModel = viewModel)
            }
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/ui/screens/MainScreen.kt
git commit -m "feat: 创建 MainScreen 主界面（底部导航）

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 14: 更新 MainActivity

**Files:**
- Replace: `app/src/main/java/com/wolfshadow/app/MainActivity.kt`

- [ ] **Step 1: 重写 MainActivity.kt**

```kotlin
package com.wolfshadow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.wolfshadow.app.ui.screens.MainScreen
import com.wolfshadow.app.ui.theme.WolfShadowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WolfShadowTheme {
                MainScreen(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add app/src/main/java/com/wolfshadow/app/MainActivity.kt
git commit -m "refactor: 精简 MainActivity，使用 Compose

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 15: 清理旧文件

**Files:**
- Delete: `app/src/main/res/layout/*.xml`
- Delete: `app/src/main/res/values/themes.xml` (保留 strings.xml, colors.xml)

- [ ] **Step 1: 删除旧的 XML 布局文件**

```bash
rm -rf "D:/Android project/VoiceWolf/app/src/main/res/layout"
```

- [ ] **Step 2: 删除旧的 themes.xml**

```bash
rm "D:/Android project/VoiceWolf/app/src/main/res/values/themes.xml"
```

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "chore: 删除旧的 XML 布局文件

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## Task 16: 构建验证

**Files:**
- None

- [ ] **Step 1: 清理构建缓存**

```bash
cd "D:/Android project/VoiceWolf" && ./gradlew clean
```

- [ ] **Step 2: 构建项目**

```bash
cd "D:/Android project/VoiceWolf" && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交最终版本**

```bash
git add -A
git commit -m "feat: 完成 WolfShadow 架构重构

- Jetpack Compose + MVVM + StateFlow 架构
- 底部标签页导航：玩家列表、投票关系图、历史记录
- 发言记录：标签选择 + 快速摘要
- 玩家画像：单个玩家完整历史
- 投票关系图：网格布局可视化

Co-Authored-By: Claude Opus 4.6 <noreply@anthropic.com>"
```

---

## 自检清单

**Spec 覆盖检查:**
- [x] 发言记录：标签选择 + 快速摘要 → Task 7 (SpeechDialog)
- [x] 玩家画像：单个玩家完整历史 → Task 8 (PlayerProfileSheet)
- [x] 投票关系图：网格布局可视化 → Task 9 (VoteGraphView)
- [x] 底部标签页导航 → Task 13 (MainScreen)
- [x] 数据模型重构 → Task 2
- [x] ViewModel StateFlow → Task 3
- [x] Compose 主题 → Task 4
- [x] 清理旧文件 → Task 15

**占位符检查:**
- [x] 无 "TBD", "TODO", "implement later"
- [x] 所有代码步骤包含完整代码
- [x] 所有命令步骤包含具体命令

**类型一致性检查:**
- [x] Player 使用 MarkedRole 枚举
- [x] SpeechRecord 使用 SpeechTag 枚举列表
- [x] GameState 包含所有必要字段
- [x] ViewModel 方法签名与组件调用一致
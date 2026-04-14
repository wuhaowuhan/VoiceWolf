# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run Android instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build artifacts
./gradlew clean

# Full build (compiles and runs checks)
./gradlew build
```

## Architecture Overview

VoiceWolf is an Android app for recording speech and votes during face-to-face Werewolf games.

### Core Files (3 Kotlin files only)
- **MainActivity.kt**: Single Activity using ViewBinding. Handles UI interactions, dialogs for speech/vote/history.
- **GameViewModel.kt**: Manages game state via LiveData. Contains player count logic (8-15, default 12).
- **Player.kt**: Data classes for Player, SpeechRecord, VoteRecord, plus enums for roles.

### MVVM Architecture
- `LiveData` for reactive UI updates - observe in Activity, update in ViewModel
- In-memory storage (no database) - data resets on app restart
- `MutableLiveData` backing fields exposed as immutable `LiveData`

### Dynamic Player Count
- Range: 8-15 players (default 12)
- Game locks player count once records exist (`hasGameStarted()` check)
- Only 13-15 slots can be removed before game starts
- Reset restores to 12 players

### Key Data Models
- `SpeechRecord`: day, playerId, summary text
- `VoteRecord`: day, voterId, targetId (0 = abstain)
- `Player.MarkedRole`: identity marking (预言家, 好人, 狼人, etc.)
- `Player.Role`: actual role enum (includes KNIGHT, MEDIUM, WEREWOLF_BEAUTY)

### Dialog Layouts
- `dialog_add_speech.xml`: Speech recording with player spinner
- `dialog_add_vote.xml`: Vote recording with target spinner + voter checkboxes grid
- `dialog_history.xml`: ScrollView for all-day history display
- `dialog_select_number.xml`, `dialog_select_numbers_multi.xml`: Template quick-select dialogs
- `dialog_select_check.xml`: Single-select with additional option (e.g., 好人/狼人 for seer check)

### ViewBinding
All layouts use ViewBinding (enabled in build.gradle.kts). Access views via `binding.viewId`.

## Project Configuration

- Uses Alibaba/Tencent Maven mirrors for faster downloads in China
- Kotlin 1.9.20, Android Gradle Plugin 8.2.0
- minSdk 24, targetSdk 34
- Portrait-only orientation (set in AndroidManifest.xml)

## UI Language

All user-facing text is in Chinese (狼人杀 terminology). Maintain this when adding features.

### Speech Templates (Quick Input)
Templates are defined in dialog layouts and dialog logic:
- **预言家类**: 跳预言家, 查验X号(好人/狼人), 警徽流X号
- **女巫类**: 跳女巫, 银水X号, 毒X号
- **守卫类**: 跳守卫, 盾X号
- **通用类**: 跳平民, 保X号, 踩X号, 觉得狼在X号
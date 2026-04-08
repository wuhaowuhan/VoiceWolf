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

### MVVM Architecture
- **MainActivity.kt**: Single Activity using ViewBinding for UI interactions
- **GameViewModel.kt**: Manages game state via LiveData (players, speech records, vote records, current day)
- **Player.kt**: Data class with role enums and helper methods for player state

### State Management
- `LiveData` for reactive UI updates - observe in Activity, update in ViewModel
- In-memory storage (no database) - data resets on app restart
- `MutableLiveData` backing fields exposed as immutable `LiveData`

### UI Structure
- 12-player grid layout (6 left column, 6 right column)
- Center panel shows all-day records (speech + vote summaries)
- Bottom controls: Add Speech, Add Vote, Next Day, Reset
- Uses Material Design components and `CardView` for panels

### Key Data Models
- `SpeechRecord`: day, playerId, summary text
- `VoteRecord`: day, voterId, targetId (0 = abstain)
- `Player.MarkedRole`: identity marking (预言家, 好人, 狼人, etc.)

### ViewBinding
All layouts use ViewBinding (enabled in build.gradle.kts). Access views via `binding.viewId`.

## Project Configuration

- Uses Alibaba/Tencent Maven mirrors for faster downloads in China
- Kotlin 1.9.20, Android Gradle Plugin 8.2.0
- minSdk 24, targetSdk 34
- Portrait-only orientation (set in AndroidManifest.xml)

## UI Language

All user-facing text is in Chinese (狼人杀 terminology). Maintain this when adding features.
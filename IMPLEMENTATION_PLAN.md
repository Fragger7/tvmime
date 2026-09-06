# TVMime Implementation Plan & Architecture Roadmap

## Goal Description
Build a high-performance, modern IPTV player for Android TV and Mobile. 
Foundation models:
- **Backend/Ingestion:** StreamVault (Zero-OOM mass Room DB chunking) & IPTVMine-Pro (Network evasion proxy).
- **Playback:** OwnTV (Dual-engine ExoPlayer/libmpv handoff).
- **UI/UX Aesthetics:** StreamVault (Glassmorphism, D-Pad focus grids) BUT adapted into a **Stateless Compose + Orchestrator** pattern to preserve our KMP vision.

## 🔥 The Phoenix Protocol (V4 Architecture Re-Pivot) 🔥
> **Date of Pivot:** September 2026
> **The Catalyst:** Sprints 16-19 attempted a monolithic "organ transplant" of StreamVault's UI, dragging in Dagger Hilt and coupling our pristine backend to 19 heavily stubbed, alien domain repositories. This caused cascading compile failures and broke the KMP architecture path.
> **The Solution:** We are executing the "Phoenix Protocol". We will rip out Dagger Hilt and all proxy stubs. We will use StreamVault *purely* as a visual inspiration. We will write 100% Stateless Jetpack Compose components and feed them data via a clean Orchestrator Pattern.

### Phase 0: The Hilt Purge (Sprint 20)
- **Objective:** Eradicate the monolithic transplant overhead.
- **Tasks:**
  - Delete `MockDomainModule.kt` and all proxy stubs (e.g., `Stubs.kt` variants).
  - Remove `@HiltAndroidApp` and `@AndroidEntryPoint` annotations.
  - Strip Dagger Hilt dependencies from `build.gradle.kts`.
  - Remove transplanted `domain` layer code that doesn't fit our V3 backend.

### Phase 1: Stateless UI Cloning (Live TV)
- **Objective:** Clone the StreamVault aesthetic without the logic.
- **Tasks:**
  - Create `LiveTvGrid.kt` as a pure, dumb `@Composable`.
  - Steal StreamVault's layout, focus scaling, and colors (mapping to `DesignSystemTokens`).
  - Signature: `@Composable fun LiveTvGrid(categories: List<CategoryEntity>, channels: List<ChannelEntity>, onChannelSelect: (ChannelEntity) -> Unit)`

### Phase 2: The Orchestrator Wiring (The Golden Path)
- **Objective:** Connect our Lamborghini Engine to the Lamborghini Body Kit.
- **Tasks:**
  - Create `LiveTvViewModel.kt` (Standard Android ViewModel). Inject `AppDatabase`.
  - Create `LiveTvRoute.kt` to collect `StateFlow<LiveTvUiState>` and pass it to `LiveTvGrid`.
  - Map `onChannelSelect` to `EngineController.startLivePreview()`.

### Phase 3: Expanding the Ecosystem (Stateless VOD & EPG)
- **Objective:** Expand UI using the proven Phase 1 & 2 formula.
- **Tasks:**
  - `EpgGrid.kt` (Stateless Timeline) wired to `EpgRoute.kt`.
  - `VodGrid.kt` (Stateless TMDB Posters) wired to `VodRoute.kt`.

### Phase 4: Native Advanced Features (KMP Ready)
- **Objective:** Rebuild advanced features natively.
- **Tasks:**
  - **Multi-View:** Stateless 2x2 grid of ExoPlayer surfaces fed by 4 URL streams from the Orchestrator.
  - **Backup/Restore:** Native Kotlin function writing Room DB to a local JSON file.
  - **Parental Controls:** Simple Compose PIN pad updating a local `isLocked` flag in Room.

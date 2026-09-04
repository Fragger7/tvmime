# AI Agent Context (TVMime) 🤖

> **Repository**: `https://github.com/Fragger7/tvmime.git`  
> **Canonical Root**: `/Users/admin/Development/tvmime`  
> **Production Web**: `https://tvmime.vercel.app`  
> **Direct TV APK**: `https://tvmime.vercel.app/tv.apk`  
> **Releases Page**: `https://github.com/Fragger7/tvmime/releases`  

**ATTENTION ALL AI AGENTS:** Read this document before modifying the TVMime codebase.


## 1. Project Paradigm
- **Goal:** Build the fastest, most beautiful IPTV Player for Android TV and Android Mobile.
- **Architecture:** Kotlin Multiplatform (KMP). Do NOT write Android-specific dependencies in the `shared/` module (e.g., no `OkHttp` or `android.util.Log` in the common codebase). Use `Ktor` and Napier for cross-platform support.
- **UI Framework:** Jetpack Compose (Material 3) and `androidx.tv` (Compose for TV). Do NOT use legacy Leanback XML fragments.

## 2. Design System Constraints
- **Visuals:** The app uses a strict "Deep Black & Crimson Red" color palette with flat translucency. 
- **Reference:** Always refer to `mockups/tv_livetv_red_black.jpg` for the canonical look. Do NOT introduce heavy 3D geometries or real-time blurs that will kill 60fps performance on $30 Firesticks.
- **Shared Tokens:** Ensure all Compose UI components use the centralized `DesignSystemTokens` from the `shared` module.

## 3. Critical IPTV Domain Knowledge
- **OOM Prevention (CRITICAL):** IPTV provider JSONs can be 50MB+. You CANNOT use `Gson` or `Moshi` to deserialize the entire response into RAM. You MUST use low-level token-by-token streaming (e.g., `JsonReader`) and insert directly into the Room Database.
- **Network Evasion:** Providers block scraping. All Ktor requests to IPTV portals must spoof user-agents (e.g., `IPTVSmartersPro/1.1.1`).
- **Player Errors:** Handle HTTP 456/884 gracefully as "Stream Egress Disabled", not as a generic crash. ExoPlayer must use a custom DataSource Factory for the User-Agent.

## 4. Current State & Feature Inventory (Build 23 / v1.1.0)
- **Live TV Player (`tvApp/.../ui/player/TvVideoPlayer.kt`)**:
  - Hardware-accelerated Media3 (ExoPlayer) with dynamic buffer profiles.
  - Top Bar: Channel number (`CH X`), Channel Name, Live Resolution Badge (`1080p`, `4K`), Active Playlist badge with active-to-max connection tally (`$activeCons/$maxCons Cons`) and status indicator dot.
  - Dedicated Clock Overlay: Independent corner pill in top-right corner (`h:mm a`), updates every 10s, toggled via settings.
  - Bottom Controls: Play/Pause, Aspect Ratio (`FIT`/`FILL`/`ZOOM`), Audio Track modal sheet, Subtitle modal sheet, Telemetry HUD toggle, Favorite toggle, TV Guide launcher, and 1-Click Stream Issue Reporter to Firestore.
  - Last Channel Quick Zap: Pressing D-Pad Right instantly swaps between current and last channel with animated top-center toast banner.
  - Telemetry HUD: Real-time bitrate, stream host/IP, buffer depth & cached percentage, video decoder, and audio track.
- **TiviMate-Style EPG Grid Guide (`tvApp/.../ui/guide/TvGuideScreen.kt`)**:
  - Interactive grid with 30-minute time intervals, channel rows, program progress indicators, and PIP mini-preview player.
- **Settings & Preferences (`tvApp/.../ui/settings/SettingsScreen.kt`, `TvPreferencesManager.kt`)**:
  - D-Pad scrollable `TvLazyColumn`.
  - Preferences for Clock Overlay, OSD Auto-Hide Timeout (3s / 5s / 10s / Always On), and Last Channel Zap.
  - Hardware & Performance Intelligence display auto-tuned to device RAM/VPU.
  - In-place OTA updater checking GitHub releases.
- **Cloud Presence & Web Admin (`adminWeb/`)**:
  - React 19 + Tailwind CSS v4 running on `https://tvmime.vercel.app`.
  - Firebase Auth & Firestore syncing portal credentials to Room DB.
  - Serverless proxy `/api/test-portal` for CORS/mixed-content IPTV checks.
  - Direct download cards for `tv.apk` and `mobile.apk`.

## 5. Key Files Map for Quick Navigation
| Component | Path |
|---|---|
| TV App Main & Nav | `tvApp/src/main/java/com/tvmime/tv/MainActivity.kt` |
| Video Player & HUD | `tvApp/src/main/java/com/tvmime/tv/ui/player/TvVideoPlayer.kt` |
| TV Guide EPG Grid | `tvApp/src/main/java/com/tvmime/tv/ui/guide/TvGuideScreen.kt` |
| TV Settings Screen | `tvApp/src/main/java/com/tvmime/tv/ui/settings/SettingsScreen.kt` |
| Preferences Manager | `tvApp/src/main/java/com/tvmime/tv/settings/TvPreferencesManager.kt` |
| Main ViewModel | `tvApp/src/main/java/com/tvmime/tv/viewmodel/TvMainViewModel.kt` |
| Shared Design Tokens | `shared/src/commonMain/kotlin/com/tvmime/theme/DesignSystemTokens.kt` |
| Xtream Repo & EPG | `shared/src/commonMain/kotlin/com/tvmime/repository/XtreamRepository.kt` |
| Room Database | `shared/src/commonMain/kotlin/com/tvmime/db/AppDatabase.kt` |
| CI/CD Build Workflow | `.github/workflows/build.yml` |

## 6. Conventional Commits & Automated Versioning Rules
All agents (Antigravity, Claude Code, Cursor, Copilot, web) MUST use Conventional Commit prefixes:
- `feat:` or `feat(scope):` - User-facing features, new screens, new capabilities (Triggers **MINOR** bump: `1.x.0`).
- `fix:` or `fix(scope):` - Bug fixes, error handling, edge cases (Triggers **PATCH** bump: `1.0.x`).
- `perf:` or `perf(scope):` - Performance optimizations, buffer tuning, memory improvements (Triggers **PATCH** bump: `1.0.x`).
- `feat!:` or `BREAKING CHANGE:` - Incompatible schema migrations or protocol breaking changes (Triggers **MAJOR** bump: `x.0.0`).
- `docs:`, `chore:`, `style:`, `refactor:` - Documentation, dependency maintenance, refactors without external impact (No version bump).
- **Android Version Code**: Strictly monotonic integer derived from `git rev-list --count HEAD` so every single commit anywhere guarantees a higher build number than previous builds.

## 7. Next Agent Hand-Off: What Is Left
1. **Physical Device TV Testing**: Test latest `tv.apk` on Android TV / Firestick devices.
2. **Mobile EPG Grid (`androidApp`)**: Implement touch-optimized EPG channel grid and schedule timeline for Android Mobile.
3. **Chromecast Receiver Integration**: Complete `androidx.media3:media3-cast` receiver discovery and playback transfer in `androidApp`.


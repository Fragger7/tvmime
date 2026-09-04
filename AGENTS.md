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

## 4. Current State & Feature Inventory (Build 32 / v1.2.2)
- **Live TV Player & Architecture ("The Player IS The App")**:
  - The video player runs perpetually at Z-index 0. No mobile-style navigation drawer breaks the viewing experience.
  - All secondary interfaces (Channel List, HUD, Settings, EPG Guide) render as translucent glass overlays on top of the live video.
  - Hardware-accelerated Media3 (ExoPlayer) with dynamic buffer profiles (Fast Zap profile with 500ms initial playback vs. Balanced 4K profile).
  - Black Screen Minimizer: Retains previous video frame until new stream decoder initializes.
  - Top Bar: Channel number (`CH X`), Channel Name, Live Resolution Badge (`1080p`, `4K`), Active Playlist badge with active-to-max connection tally (`$activeCons/$maxCons Cons`) and status indicator dot.
  - Dedicated Clock Overlay: Independent corner pill in top-right corner (`h:mm a`), updates every 10s, toggled via settings.
  - Bottom Controls: Play/Pause, Aspect Ratio (`FIT`/`FILL`/`ZOOM`), Audio Track modal sheet, Subtitle modal sheet, Telemetry HUD toggle, Favorite toggle, TV Guide launcher, and 1-Click Stream Issue Reporter to Firestore.
  - Telemetry HUD: Real-time bitrate, stream host/IP, buffer depth & cached percentage, video decoder, and audio track.
  - **Auto-Recovery & Dynamic Failover (v1.2.2)**: Auto-switches between `.ts` and `.m3u8` container extensions upon demuxer/playback errors. Drops active TCP sockets (`stop()`, `clearMediaItems()`) prior to tuning to obey strict `max_connections: 1` IPTV limits.
  - **Visual Error Guidance Card (v1.2.2)**: Immediately catches HTTP 404, 403, and 456/884 egress blocks, dismisses perpetual buffering spinners, and surfaces an on-screen guidance banner prompting user navigation.

- **Streaming Catalog Ingestion & Dummy Header Pruning (v1.2.2)**:
  - `StreamingCatalogParser.kt` uses low-level `JsonReader` token streaming directly into Room DB to prevent OOMs on 50MB+ catalogs.
  - Integrated regex sanitization (`^[#=\-_~*]{3,}.*|.*[#=\-_~*]{3,}$`) to discard fake separator headers (e.g., `##### 4K SPORTS #####`), preventing broken 404 stream links.

- **Multi-Portal Cloud Sync (v1.2.0 - v1.2.2)**:
  - CloudSync overlay pane with Firebase Firestore integration for syncing multiple IPTV portals across devices.
  - Full cleartext traffic configuration (`network_security_config.xml`) enabled for unencrypted edge CDN stream redirects.

- **D-Pad Remote Control Matrix (`tvApp/.../MainActivity.kt`)**:
  - `[ CENTER / OK ]`: When overlays are hidden, brings up the Bottom HUD and playback controls; selects items when an overlay is open.
  - `[ UP / DOWN ]`: While HUD is hidden, immediately zaps to the next or previous channel in the current playlist (`zapNext()` / `zapPrevious()`).
  - `[ LEFT ]`: Slides out the Master Channel List overlay with categories in column 1 and channels in column 2.
  - `[ RIGHT ]`: Last Channel Quick Zap—instantly swaps between current and previously tuned channel with animated floating toast banner.
  - `[ MENU ]`: Opens the Settings overlay directly.
  - `[ BACK ]`: Dismisses active overlays and returns focus to fullscreen playback without exiting the application accidentally.

- **Focus Management & Trap Prevention**:
  - Dedicated `FocusRequester` matrix in `MainActivity.kt` (`playerFocusRequester`, `hudFocusRequester`, `channelListFocusRequester`, `settingsFocusRequester`).
  - `LaunchedEffect(overlayState)` requests focus programmatically on state transitions, preventing lost-focus and unnavigable remotes on Android TV / Fire TV sticks.

- **Category Filtering & Long-Press Hiding**:
  - `TvPreferencesManager.kt` persists user-hidden category IDs (`Set<String>`) in `SharedPreferences`.
  - `TvMainViewModel.kt`'s `categories` StateFlow combines active portal, destination, and hidden categories in real-time.
  - Long-press (`onLongClick`) on category cards in `LiveTvScreen.kt` instantly hides unwanted categories from the catalog.

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
  - Serverless endpoint `/api/version` querying GitHub releases with robust fallbacks.
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

## 7. Next Agent Hand-Off: What Is Left (Prioritized)
### ⚠️ ARCHITECTURE PIVOT & REFERENCE REPO (MUST READ)
We have officially executed the **"Sohva-TV Pivot"**. Do NOT use standard `TvNavigationDrawer` or separate Jetpack Compose destinations for Live TV. We are moving to a "Player IS The App" architecture.
*   **The Blueprint:** If you need to understand how to handle D-Pad focus state machines, ExoPlayer lifecycle hooks, or M3U ingestion, read the reference repository: `https://github.com/Macstered/Sohva-TV`
*   **The Paradigm:** The app is a single `AndroidView` wrapping ExoPlayer at Z-index 0. D-Pad keys are intercepted at the raw View level (`setOnKeyListener`) to mutate booleans (`channelBrowserVisible`, `chromeVisible`). These booleans render lightweight translucent Compose panes on top of the playing video.

### Implementation Tasks (Prioritized):
1. **Pass 4: Systems-Level Architecture (The Premium Tier)**:
   - **Background Sync Workers**: Setup Android `WorkManager` for silent syncing (every 12 hours) and add toggles to SettingsScreen.
   - **EPG Local Time-Shift**: Refactor the EPG logic to auto-map UTC to the local device timezone, and add a manual timezone offset slider in Settings for sloppy providers.
   - **Catch-Up TV (DVR)**: Add the ability to scroll backwards in the EPG and trigger the specialized `/timeshift/` URL streams for past events if `has_archive = 1`.
   - **M3U Fallback Pipeline**: Ensure raw `.m3u` text parsing works alongside our Xtream Codes JSON implementation.
2. **Pass 5: Auth & Polish**:
   - Rip out the broken QR code UI and simplify the login screen.
   - Eliminate the brief login screen flash that happens on every boot.
3. **Pass 6: Mobile App (`androidApp`) Touch EPG**:
   - Implement touch-optimized EPG channel grid and schedule timeline for Android Mobile.

## 8. Development Environment & Testing Guide
- **Web Admin Development**:
  - Dev server runs on port 3000 via root `npm run dev` (proxied to `tvmime/adminWeb`).
  - Check health at `http://localhost:3000`.
- **Android Builds**:
  - Gradle builds are executed remotely via GitHub Actions (`.github/workflows/build.yml`) on tag push or manual workflow dispatch.
  - The local container is a Node.js dev container; do NOT attempt to invoke `./gradlew` locally without an installed Android SDK.
- **Git Push Workflow**:
  - Repository URL: `https://${GITHUB_PAT}@github.com/Fragger7/tvmime.git`.
  - Always verify code compiles and lints with `npm run build` and `npm run lint` before committing.
  - Use conventional commit messages (`feat:`, `fix:`, `perf:`, `docs:`) to maintain automated semver calculation.


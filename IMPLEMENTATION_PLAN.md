## Goal Description
Build a high-performance, modern IPTV player for Android TV (with full D-Pad support) and Android Mobile. The player will support Xtream Codes (Phase 2) and Stalker portals (Future Phase), providing a zippy experience similar to TiviMate and IMPlayer. 

Key features include Live TV, VOD (Movies & Series), EPG with logos, and standard TV watching controls. A critical addition is **Chromecast / Android TV Casting capability from the Mobile app**.

Instead of restraining our architecture to basic lessons, we will model our foundation after state-of-the-art open-source repositories like **StreamVault** and **OwnTV** (both built in Kotlin with Jetpack Compose for TV), adopting their best practices for handling massive M3U/Xtream lists and D-Pad focus management.

## User Review Required

> [!TIP]
> **Firebase Free Tier Viability**
> You asked if the Firebase free tier is enough. **Yes, it is more than enough, provided we use it correctly.**
> We will NOT store the massive 50MB+ channel lists in Firebase. Instead, Firebase Firestore will only store the user's **Portal Credentials** (Xtream URL, Username, Password) and lightweight data (like Favorite Channel IDs). 
> The Android TV / Mobile app will fetch these tiny credentials from Firebase, and then make the heavy API request *directly* to the IPTV provider, caching the massive channel list locally on the device using SQLite (Room). This ensures we stay well within the free 1GB storage and 50k reads/day limits.

## Proposed Changes

### Application Architecture (Future-Proofed for Apple TV)
To ensure we can seamlessly port this application to **Apple TV (tvOS)** and **iOS** in the future, we will build the foundation using **Kotlin Multiplatform (KMP)**. This is a critical strategic choice.

- **Shared Core (KMP):** All business logic, network requests, JSON parsing, and local database caching will be written in a shared Kotlin module. This means when you decide to build the Apple TV version, 90% of the app's code is already done.
  - **Networking:** Ktor (KMP compatible) instead of OkHttp.
  - **Database:** Room (which recently became KMP compatible) or SQLDelight for local caching.
  - **Design System Tokens:** A centralized Kotlin object defining exact Hex Colors, Typography scales, and padding/spacing values. Every platform (Android Mobile, TV, Web) will pull from this single source of truth to guarantee pixel-perfect brand consistency. The canonical design reference for the entire ecosystem (including the Web Admin Portal) is the `tv_livetv_red_black.jpg` mockup—featuring a Deep Black background, vibrant Crimson Red accents, sleek typography, and high-performance subtle translucency.
- **Native UI Layer:**
  - **Android TV / Mobile (Current):** Jetpack Compose (Material 3) & Compose for TV. We will stick with a **Left-side category menu** for the TV UI as it is intuitive and industry standard (TiviMate style).
  - **Apple TV (Future):** We will build a native SwiftUI interface that simply plugs into our shared KMP core.
- **Media Player Interface:** We will create a shared interface for the video player. On Android, it will use hardware-accelerated **AndroidX Media3 (ExoPlayer)**. In the future on Apple TV, it will map to **AVPlayer**.

---

---

### Phase 1: Cloud Presence & Admin Foundation [COMPLETED]
- Built `adminWeb/` React 19 + Vite 6 + Tailwind CSS v4 hosted on Vercel at [tvmime.vercel.app](https://tvmime.vercel.app).
- Firebase Authentication (Email/Password & Anonymous) under `tvmime-65909`.
- Firestore database for `UserPortals` syncing across devices.
- Serverless proxy `/api/test-portal` for CORS and mixed-content IPTV validation.
- Direct TV and Mobile APK download cards on landing page.

---

### Phase 2: Network & Data Layer (Xtream Codes - Shared KMP) [COMPLETED]
- **`shared/src/commonMain/.../repository/XtreamRepository.kt`**: Fetches categories, channels, and EPG data from Xtream Codes APIs with spoofed User-Agent (`IPTVSmartersPro/1.1.1`).
- **`shared/src/commonMain/.../network/StreamingCatalogParser.kt`**: Zero-OOM token-by-token JSON parsing directly into local SQLite database.
- **`shared/src/commonMain/.../db/AppDatabase.kt`**: Multiplatform Room Database caching categories, channels, and EPG schedules.
- **`shared/src/commonMain/.../sync/FirebaseSyncClient.kt`**: Cloud portal synchronization with local Room storage.

---

### Phase 3: Mobile App & Chromecast [IN PROGRESS]
- [x] Scaffold Mobile Jetpack Compose foundation (`androidApp/src/main/java/com/tvmime/mobile/MainActivity.kt`).
- [ ] Implement Mobile EPG and channel grid with touch optimization.
- [ ] Complete `androidx.media3:media3-cast` receiver discovery and remote playback transfer.

---

### Phase 4: Android TV UI (Compose for TV) [COMPLETED]
- **`tvApp/.../ui/navigation/TvNavigationDrawer.kt`**: Collapsible left navigation drawer with D-Pad focus handling.
- **`tvApp/.../ui/live/LiveTvScreen.kt`**: Split channel browser with category column, channel list, and embedded preview player.
- **`tvApp/.../ui/player/TvVideoPlayer.kt`**: Fullscreen hardware-accelerated Media3 player:
  - Top information bar (CH number, name, live resolution, active playlist badge, and active/max connection ratio).
  - Dedicated persistent top-right clock overlay pill.
  - In-playback controls (Play/Pause, Aspect Ratio, Audio Tracks sheet, Subtitles sheet, Telemetry HUD, Favorite toggle, TV Guide launcher, Issue Reporting).
  - D-Pad Right Last Channel Quick Zap with animated top-center toast.
  - Live Telemetry HUD (bitrate, remote host/IP, buffer depth & % cached, decoders).
- **`tvApp/.../ui/guide/TvGuideScreen.kt`**: TiviMate-style EPG grid guide with 30-minute time intervals and PIP mini-preview.
- **`tvApp/.../ui/settings/SettingsScreen.kt` & `TvPreferencesManager.kt`**: Interactive D-Pad preferences (Clock toggle, OSD timeout picker, Last Channel zap toggle).
- **`tvApp/.../ui/onboarding/OnboardingScreen.kt`**: Quick TV pairing wizard & demo portal loader.
- **`tvApp/.../ui/about/AboutScreen.kt`**: Crediting Faraz Ahmad, architecture specifications, and dynamic runtime version code.

---

### Phase 5: CI/CD & Automated Semantic Versioning [COMPLETED]
- Standalone repository: `https://github.com/Fragger7/tvmime.git`.
- GitHub Actions workflow (`.github/workflows/build.yml`) compiling both Android TV and Mobile APKs.
- Automatic semantic version bumping (`v1.x.0` via conventional commits) and strictly monotonic build codes (`git rev-list --count HEAD`).
- Permanent TV APK download link redirecting to `https://tvmime.vercel.app/tv.apk`.
- Cryptographic debug keystore locked to preserve 100% in-place OTA update compatibility.

## Verification Plan

### Automated Tests
- Unit tests for Firebase data mapping.
- Unit tests for Xtream JSON parsing to Room Entities.

### Manual Verification
- Add a portal credential via the Cloud/Mobile UI, launch the TV app, and verify it automatically syncs the portal and begins downloading the channels.
- Cast a Live TV stream from the Android Mobile app to a local Chromecast / Android TV device.
- Navigate the TV UI using only D-Pad arrows, ensuring no focus traps exist.

---

### Sprint 2: Usability, Focus, and Navigation Refinement [COMPLETED]
- **Channel Zapping (UP/DOWN):** Implemented `zapNext()` and `zapPrevious()` in `TvMainViewModel.kt` and mapped them to the `DPAD_UP` and `DPAD_DOWN` hardware keys in `MainActivity.kt` when HUD overlay is hidden.
- **Focus Management (No Lost Remotes):** Solved Compose TV focus traps by instantiating distinct `FocusRequester` instances for each overlay (`CHANNEL_LIST`, `HUD`, `SETTINGS`) and programmatically requesting focus via a `LaunchedEffect` whenever the `overlayState` changes.
- **Long-Press Context Menu & Group Hiding:** Extended `TvPreferencesManager.kt` to persist a `Set<String>` of hidden category IDs to `SharedPreferences`. Modified `TvMainViewModel.kt`'s `categories` StateFlow to intercept and filter out hidden categories in real-time. Wired `onLongClick` in `LiveTvScreen.kt` CategoryCard.
- **Channel State Integrity:** Restored `playChannel` with previous-channel tracking for instant D-Pad Right swap and watch-history tracking in Room.

### Sprint 3: The "Sohva-TV Pivot" (Resilience, Ingestion Hardening & The Player IS The App) [ACTIVE SPRINT]
- **Stream Auto-Recovery & Dummy Header Pruning [COMPLETED]:** Filtered dummy separator headers in `StreamingCatalogParser.kt` to avoid 404 dead stream calls. Implemented proactive socket teardown (`stop()` + `clearMediaItems()`) for single-connection IPTV portals. 
- **[CRITICAL BUG FIX #1] JsonReader Null-Safety in Catalog Ingestion [COMPLETED]:**
  - Added extension `fun JsonReader.nextStringOrNull(): String?` and wrapped individual row parsing with try-catch fallback. This prevents massive 50MB JSON parses from aborting midway.
- **[CRITICAL ARCHITECTURE #2] The "Player IS The App" D-Pad Overhaul [IN PROGRESS]:**
  - *Rationale*: Standard Compose `TvNavigationDrawer` causes recomposition lag and black screens on TV. Following the *Sohva-TV* architecture, we will rip out all navigation drawers.
  - *Design*: The app is a single `AndroidView` wrapping ExoPlayer at Z-index 0. D-Pad keys are intercepted at the raw View level (`setOnKeyListener`) to mutate booleans (`channelBrowserVisible`, `chromeVisible`). These booleans render lightweight translucent Compose panes on top of the playing video.
  - *Playback Engineering*: Implement `LOW_LATENCY` (1000ms) and `STABILITY` (5000ms) `LoadControl` profiles. Hook into the Android Lifecycle to instantly fire `controller.stop()` when backgrounded to prevent "Ghost Connection" lockouts from IPTV providers.
- **[CRITICAL ARCHITECTURE #3] Dual-Model Sync & Multi-Portal Aggregation:**
  - *Rationale*: Vercel free tier cannot process M3U lists. 
  - *Design*: Cloud stores credentials only. TV edge-computes the heavy ingestion. The UI will aggregate channels/categories across all `isActive = true` portals, prefixing IDs (`${portalId}_${type}_${streamId}`) to avoid collisions. The Playlists menu will distinguish between Cloud ☁️ and Local 📺 credentials, and allow bidirectional sync.
- **Mobile Touch EPG [UPCOMING]:** Adapt TV EPG timeline grid into touch-draggable 2D timeline for Android phones/tablets.
---

## Backlog & Future Explorations

> [!NOTE]
> **Radical 3D UI / Game Engine Integration**
> Once the core KMP engine and baseline Jetpack Compose UI are rock-solid, explore ripping off the Compose layer and plugging the shared Kotlin engine into a game engine (**Unity** or **Godot**) via native bridging. This would allow for the realization of the "Spatial Floating Glass" radical mockups—enabling real-time 3D Cover Flow, volumetric smoke, and physics-based UI rendering that standard app frameworks cannot handle, similar to modern automotive EV dashboards.

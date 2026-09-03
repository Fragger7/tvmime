# TVMime Task List 📝

### Phase 0: Planning & Architecture
- [x] Analyze IPTV constraints and domain knowledge.
- [x] Finalize KMP "Shared Core, Native UI" architecture.
- [x] Generate and select UI/UX design language (`tv_livetv_red_black.jpg`).
- [x] Initialize Git repository inside Monorepo.
### Phase 1: Cloud Presence & Admin Foundation
- [x] Initialize Project Structure (`adminWeb` scaffolded with Vite 6 + React 19 + Tailwind CSS v4).
- [x] Setup Firebase Project via Web Console (`tvmime-65909`).
- [x] Create `adminWeb` React project styled with Deep Black & Crimson Red design tokens.
- [x] Implement Firebase Auth & Firestore schema (`UserPortals`).
- [x] Deploy `adminWeb` to Vercel production ([tvmime.vercel.app](https://tvmime.vercel.app)).
- [x] Build serverless proxy (`/api/test-portal`) for zero-CORS / mixed-content IPTV testing.
- [x] Whitelist `tvmime.vercel.app` in Firebase Authentication Authorized Domains.
- [x] Implement Account Profile & Password Management Modal in `adminWeb`.

### Phase 2: Shared Core & Data Layer (Xtream Codes)
- [x] Scaffold KMP Multi-module architecture (`shared`, `tvApp`, `androidApp`, version catalog `libs.versions.toml`).
- [x] Implement centralized Design System Tokens in Kotlin (`DesignSystemTokens.kt`).
- [x] Implement Ktor Network Client with User-Agent spoofing (`IPTVSmartersPro/1.1.1`) and fast-fail hedging (`XtreamClient.kt`).
- [x] Implement zero-OOM token-by-token streaming catalog parser (`StreamingCatalogParser.kt`).
- [x] Implement KMP Room Database for caching channels/EPG (`AppDatabase.kt`, DAOs & Entities).
- [x] Sync User Portals from Firebase to local Room DB (`FirebaseSyncClient.kt`, `XtreamRepository.kt`).

### Phase 3: Android Mobile App & Chromecast
- [x] Scaffold Mobile Jetpack Compose baseline (`MainActivity.kt`).
- [ ] Implement full Mobile EPG and channel grid.
- [ ] Complete `androidx.media3:media3-cast` receiver discovery and remote playback.

### Phase 4: Android TV UI (Compose for TV)
- [x] Scaffold Android TV Leanback Compose baseline (`MainActivity.kt`).
- [x] Implement in-place OTA Updater (`UpdateManager.kt`) and TV settings permissions.
- [x] Implement Left-Side Navigation Drawer with D-Pad focus (`TvNavigationDrawer.kt`).
- [x] Implement Live TV Channel & Category Split Browser (`LiveTvScreen.kt`).
- [x] Implement ExoPlayer hardware-accelerated playback overlay (`TvVideoPlayer.kt`).
- [x] Implement Cloud Portal Sync & Demo loader UI (`CloudSyncScreen.kt`).
- [x] Implement Live TV EPG Timeline (Translucent Strips).
- [x] Implement VOD Section (Netflix style, TMDB metadata - `VodScreen.kt`).
- [x] Implement About TVMime Screen (`AboutScreen.kt` crediting Faraz Ahmad, architecture & portal endpoints).
- [x] Implement First-Run Setup Wizard (`OnboardingScreen.kt`) with Quick TV Pairing & Direct Login.
- [x] Implement Device Capability & Hardware Intelligence Engine (`DeviceCapabilityDetector.kt`).
- [x] Implement Dynamic Buffer LoadControl profiles (Fast Zap vs Balanced 4K).
- [x] Implement 1-Click Stream Issue Reporting with Firestore Logging & Web Admin Console.

### Phase 5: CI/CD Pipeline & Standalone Repository
- [x] Migrate to dedicated standalone repository (`https://github.com/Fragger7/tvmime.git`).
- [x] Create GitHub Actions automated build workflow (`.github/workflows/build.yml`).
- [x] Lock cryptographic signing key (`keystore/debug.keystore`) for 100% in-place update compatibility.
- [x] Configure automated rolling GitHub Releases with raw APKs attached (`tvmime_tv_v1.0.0.apk`, `tv.apk`).
- [x] Set up direct Firestick Downloader redirects (`https://tvmime.vercel.app/tv.apk`).



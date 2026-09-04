# TVMime Task List 📝

### Phase 0-5: Foundation & Scaffolding [COMPLETED]
- Core KMP setup, Xtream parsing, Firebase syncing, basic UI and CI/CD pipelines completed.

### Sprint 1: "The Player IS The App" Architecture [COMPLETED]
- [x] **Z-Index Glass Overlays:** Rip out mobile-style `TvNavigationDrawer`. Player runs permanently at Z-index 0.
- [x] **D-Pad Control Matrix Mapping:** Root-level interception of CENTER, LEFT, RIGHT in `MainActivity.kt`.
- [x] **Hardware & Media3 Tuning:** Implemented Auto Frame Rate (AFR), Audio Passthrough priority, and Fast Zap buffers (500ms initial playback) in `Media3PlayerConfig.kt`.
- [x] **Black Screen Minimizer:** Configured video scaling and frame retention to prevent black flashes between zaps.

### Sprint 2: Usability, Focus, and Navigation Refinement [COMPLETED]
- [x] **Channel Zapping (UP/DOWN):** Implemented `zapNext()` and `zapPrevious()` in `TvMainViewModel.kt` and mapped to `DPAD_UP` and `DPAD_DOWN` in `MainActivity.kt` when HUD is hidden.
- [x] **Focus Management (No Lost Remotes):** Distinct `FocusRequester` instances per overlay (`player`, `hud`, `channelList`, `settings`) managed via `LaunchedEffect(overlayState)` to eliminate focus traps.
- [x] **Long-Press Context Menu & Group Hiding:** `TvPreferencesManager` persists `hiddenCategories` to `SharedPreferences`. `TvMainViewModel` filters them dynamically via `StateFlow`. `LiveTvScreen` CategoryCard triggers hiding on long-click.
- [x] **Previous Channel State Tracking:** Recorded previous channel on playback switch for instant D-Pad Right swap.

### Sprint 3: Resilience, Multi-Portal & Ingestion Fixes [ACTIVE SPRINT]
- [x] **Stream Error Auto-Recovery & Dummy Header Pruning (v1.2.2):** Filter dummy header rows (`##### 4K SPORTS #####`) in `StreamingCatalogParser.kt`. Proactive socket closure on zapping to respect `max_connections: 1`. Container format fallback (`.ts` ⇄ `.m3u8`) and contextual error pill in `TvVideoPlayer.kt`.
- [x] **Cleartext Traffic Configuration:** Added `network_security_config.xml` to support unencrypted IPTV edge CDN redirects.
- [ ] **[BUG #1] Fix JsonReader Null Token Crash during Catalog Ingestion:** Handle `Expected a string but was NULL` by replacing raw `reader.nextString()` with a null-safe reader extension `nextStringOrNull()` in `StreamingCatalogParser.kt`.
- [ ] **[BUG #2 & #4] Fix Channels & Movies List Empty State:** Ensure channel and VOD insertion transactions complete reliably and are not aborted mid-stream by unexpected nulls or missing fields.
- [ ] **[FEATURE / BUG #3] Multi-Portal Unified Content Aggregation:** Support loading and browsing content across all portals marked active, rather than restricting UI to only the top active portal.
- [ ] **Mobile Touch EPG Grid (`androidApp`):** Implement touch-optimized 2D timeline and channel browser for mobile phones/tablets.
- [ ] **Chromecast Receiver Discovery & Transfer (`androidApp`):** Complete Cast SDK integration (`androidx.media3:media3-cast`) to transfer live streams from mobile device to Android TV / Cast hardware.

### Sprint 4: VOD Experience Upgrade [UPCOMING]
- [ ] **TMDB Poster Grid Layout (`VodScreen.kt`):** Transform flat list into responsive poster grid with TMDB backdrop artwork and synopsis cards.
- [ ] **VOD Playback Controls:** Add resume-playback position caching and 10s skip backward/forward.

### Product Backlog & Architectural Roadmap
- **Catch-up (Archive) Support:** Implement Xtream Codes catch-up playback.
- **Multi-Playlist Management:** Allow users to switch between multiple portals seamlessly.
- **Backup/Restore:** Export/import app settings/favorites to Google Drive or Firebase.
- **Parental Controls:** PIN code lock for specific categories (Adult content).

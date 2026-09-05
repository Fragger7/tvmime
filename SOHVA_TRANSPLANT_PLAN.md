# Sohva-TV ➔ TVMime Transplant Plan

This document serves as the exact step-by-step surgical plan for copying the `Sohva-TV` Jetpack Compose architecture into `TVMime` without breaking the TVMime Multi-Portal backend.

## Branch Status
*   **Current Branch:** `feature/sohva-ui-transplant`
*   **Target:** `main`

## Phase 1: Core UI Foundation (In Progress)
- [x] Create `tvApp/src/main/java/com/tvmime/tv/ui/common/`
- [x] Copy `TvUiComponents.kt` from Sohva-TV.
- [x] Copy `FocusScroll.kt` from Sohva-TV.
- [x] Copy `StreamMateTheme.kt` from Sohva-TV.
- [x] Scrub `com.streammate.tv` packages to `com.tvmime.tv.ui.common`.
- [x] Rename `StreamMateTheme` to `TvMimeTheme` and `StreamMateThemeTokens` to `TvMimeThemeTokens`.
- [ ] **NEXT STEP:** Update `MainActivity.kt` and `TVMimeTvApp()` to use `TvMimeTheme` instead of generic Compose Material3 themes.

## Phase 2: The Playback Engine (Service Migration)
Sohva-TV uses a `MediaSessionService` which is vastly superior to our `AndroidView` ExoPlayer binding in Compose.
- [ ] Copy `PlaybackBufferProfile.kt` and `PlaybackBufferPolicy.kt`.
- [ ] Copy `StreamMatePlaybackService.kt` ➔ Rename to `TvMimePlaybackService.kt`.
- [ ] Copy `ResolvingDataSource` logic (this intercepts `streammate://` URLs and resolves them to real IPTV HTTP URLs with User-Agents at the last millisecond). *Crucial for zapping speed.*
- [ ] Wire `TvMimePlaybackService.kt` to the TVMime Room database (it needs to query the `ChannelEntity` to get the real stream URL).
- [ ] Update `AndroidManifest.xml` to declare the `MediaSessionService`.

## Phase 3: The Overlay Screens (Gut & Replace)
- [ ] Delete `tvApp/src/main/java/com/tvmime/tv/ui/live/` (Our old Channel List).
- [ ] Delete `tvApp/src/main/java/com/tvmime/tv/ui/guide/` (Our old EPG).
- [ ] Delete `tvApp/src/main/java/com/tvmime/tv/ui/hud/` (Our old HUD).
- [ ] Copy `iptv/src/main/java/com/streammate/tv/feature/guide/` (Sohva's GuideGrid).
- [ ] Copy `iptv/src/main/java/com/streammate/tv/feature/player/PlayerChrome.kt` (Sohva's HUD).
- [ ] Copy `iptv/src/main/java/com/streammate/tv/feature/catalogue/v2/CatalogueBrowserV2.kt` (Sohva's Channel List).
- [ ] Adapt the ViewModels! Sohva-TV uses a flattened database structure. We must map our relational `CategoryEntity` and `ChannelEntity` flows from `TvMainViewModel` into the state objects that `GuideGrid` and `CatalogueBrowser` expect.

## Phase 4: State Routing & Navigation
- [ ] Rip out TVMime's `TvOverlayState` enum.
- [ ] Copy Sohva-TV's `List<Destination>` manual backstack routing logic into `MainActivity.kt` (or a dedicated `AppNavigation.kt`).
- [ ] Ensure D-Pad key events properly push/pop the destination stack.

## Phase 5: Sportmate Hub Integration
- [ ] Copy the `sportmate` module.
- [ ] Connect `api-sports.io` key input to TVMime Settings screen.
- [ ] Verify local Room caching (`SportsCacheDao`) operates correctly to avoid free-tier API bans.

## Phase 6: Brand Scrubbing & Polish
- [ ] Replace all hardcoded colors in `TvMimeTheme` with TVMime's canonical "Deep Black & Crimson Red" hex codes.
- [ ] Run a final global Regex search for `StreamMate`, `Sohva`, `own.tv`, and remove any lingering strings.

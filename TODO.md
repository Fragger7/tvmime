# TVMime Sprint Backlog

## 🚨 IMMEDIATE ACTION REQUIRED: THE PHOENIX PURGE

**Context:** We are abandoning the Dagger Hilt monolithic UI transplant. We are moving to Stateless Compose UIs powered by Orchestrator Routes.

### Sprint 20: The Purge & The Orchestrator
- [ ] **Purge Hilt:** Remove all Dagger Hilt dependencies, annotations (`@HiltAndroidApp`, `@AndroidEntryPoint`), and the `MockDomainModule.kt` stubs.
- [ ] **Clean AppDatabase Wiring:** Ensure `AppDatabase` (Room) is accessible via standard manual DI or ViewModel factories.
- [ ] **Build Stateless `LiveTvGrid`:** Create a pure, dumb `@Composable` that accepts lists of categories and channels. Clone the visual modifiers (focus, padding) from StreamVault, but use `DesignSystemTokens`.
- [ ] **Wire `LiveTvRoute`:** Create the orchestrator. Collect state from `LiveTvViewModel` (which reads from our Room DB) and pass it to `LiveTvGrid`. Verify clicking a channel hands off the stream URL to ExoPlayer.

### Sprint 21: VOD & EPG (Stateless)
- [ ] **Stateless EPG Grid:** Clone the visual timeline grid from StreamVault. Wire to `EpgRoute`.
- [ ] **Stateless VOD Grid:** Clone TMDB poster grid. Wire to `VodRoute`.
- [ ] **Timeshift/DVR:** Connect `Media3PlayerEngine` timeshift parameters for Catch-up TV.

### Sprint 22: Settings & Polish
- [ ] **DataStore Migration:** Migrate the `MemoryPrefs` settings map to actual Android `DataStore` or `SharedPreferences` so settings persist across reboots.
- [ ] **OTA Verification:** Tag `v3.0.0-RC1` and ensure the GitHub Actions pipeline builds the APK and Vercel OTA URL points to it correctly.

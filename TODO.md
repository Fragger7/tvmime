# TVMime Sprint Backlog

## ✅ Completed (V3 Master Surgery & Sprint 16-19)
- [x] Extracted StreamVault `ui/`, `domain/`, and `player/` packages natively to avoid compiler crashes.
- [x] Stubbed 19 StreamVault Repositories using `java.lang.reflect.Proxy` via `MockDomainModule.kt` to satisfy Dagger Hilt dynamically.
- [x] Built the `PreferencesRepository` memory state map to support the Settings Screen natively.
- [x] Decoupled `AppNavigation` from StreamVault's God-Object `MainActivity` via `StreamVaultNavViewModel`.
- [x] Wired StreamVault UI to TVMime's `AppDatabase` via custom `CategoryRepository` and `ChannelRepository` Proxy Adapters. 
- [x] Routed TVMime's `TvNavigation.kt` to boot into `AppNavigation()` upon successful Firebase Authentication.
- [x] **Sprint 19: Player Execution:** Bridged StreamVault's `@MainPlayerEngine` to TVMime's `EngineController`. StreamVault's `prepare()` and `renewStreamUrl()` now successfully hand off to TVMime's `LivePreviewEngine` and `MainPlayer`!
- [x] **Sprint 19: EPG Data:** Bridged StreamVault's `EpgRepository` to read directly from TVMime's `EpgDao`.

## 🏃 Next Up (Post-MVP Enhancements)
- [ ] Connect `Media3PlayerEngine` timeshift parameters for DVR functionality.
- [ ] Persist the Settings in-memory map to actual Android DataStore.

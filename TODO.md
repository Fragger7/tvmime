# TVMime Sprint Backlog

## ✅ Completed (V3 Master Surgery & Sprint 16-18)
- [x] Extracted StreamVault `ui/`, `domain/`, and `player/` packages natively to avoid compiler crashes.
- [x] Stubbed 19 StreamVault Repositories using `java.lang.reflect.Proxy` via `MockDomainModule.kt` to satisfy Dagger Hilt dynamically.
- [x] Built the `PreferencesRepository` memory state map to support the Settings Screen natively.
- [x] Decoupled `AppNavigation` from StreamVault's God-Object `MainActivity` via `StreamVaultNavViewModel`.
- [x] Wired StreamVault UI to TVMime's `AppDatabase` via custom `CategoryRepository` and `ChannelRepository` Proxy Adapters. 
- [x] Routed TVMime's `TvNavigation.kt` to boot into `AppNavigation()` upon successful Firebase Authentication.

## 🏃 Next Up (Sprint 19: Player Execution & Final Polish)
- [ ] Connect `Media3PlayerEngine` calls from StreamVault UI to TVMime's `TvMimeVideoEngine`.
- [ ] Enable EPG mapping in `EpgRepository` proxy to render the TV Guide natively.

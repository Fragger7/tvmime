# 🚨 STRICT EXECUTION BACKLOG (PHOENIX SPRINT 20-22) 🚨

**AGENT INSTRUCTION:** You are to execute these sprints sequentially. Do not skip steps. Do not modify files outside of these instructions unless absolutely necessary to fix a compile error caused by these steps.

## Sprint 20: The Hilt Purge (Current)
1. **Delete File:** `tvApp/src/main/java/com/tvmime/tv/di/MockDomainModule.kt` (and any related `Stubs.kt` files).
2. **Modify File:** `tvApp/src/main/java/com/tvmime/tv/MainActivity.kt`. Remove the `@AndroidEntryPoint` annotation. Remove any Hilt injection imports.
3. **Modify File:** Find the Application class (e.g., `TvMimeApp.kt` if it exists) and remove `@HiltAndroidApp`.
4. **Modify File:** `tvApp/build.gradle.kts`. Remove `id("dagger.hilt.android.plugin")` and the `implementation("com.google.dagger:hilt-android:...")` dependencies.
5. **Modify File:** Remove Hilt compiler dependencies from the root `build.gradle.kts` if present.

## Sprint 21: Build the Stateless UI Shell
1. **Create File:** `tvApp/src/main/java/com/tvmime/tv/ui/livetv/LiveTvGrid.kt`.
2. **Action:** Write a `@Composable` function. It must take `categories: List<CategoryEntity>` and `channels: List<ChannelEntity>`.
3. **Aesthetics:** Use a left-side `TvLazyColumn` for categories and a center `TvLazyVerticalGrid` for channels. Use `DesignSystemTokens` colors. Look at the previous StreamVault UI for padding/focus ideas, but KEEP IT STATELESS.

## Sprint 22: Build the Orchestrator
1. **Create/Modify File:** `tvApp/src/main/java/com/tvmime/tv/ui/livetv/LiveTvViewModel.kt`. Use a standard `androidx.lifecycle.ViewModel`. Inject `AppDatabase` via a standard ViewModelFactory (no Hilt). Query the database and expose a `StateFlow`.
2. **Create File:** `tvApp/src/main/java/com/tvmime/tv/ui/livetv/LiveTvRoute.kt`. Collect the StateFlow from the ViewModel. Pass the data into `LiveTvGrid`. 
3. **Action:** Pass an `onChannelClick` lambda that triggers `EngineController.startLivePreview(url)`.

---

# 📚 HISTORICAL BACKLOG & FUTURE TASKS
*The following are old tasks or future KMP ideas preserved for historical context.*

# TVMime Sprint Backlog

## ✅ Completed (V3 Master Surgery & Sprint 16-19)
- [x] Extracted StreamVault `ui/`, `domain/`, and `player/` packages natively to avoid compiler crashes.
- [x] Stubbed 19 StreamVault Repositories using `java.lang.reflect.Proxy` via `MockDomainModule.kt` to satisfy Dagger Hilt dynamically.
- [x] Built the `PreferencesRepository` memory state map to support the Settings Screen natively.
- [x] Decoupled `AppNavigation` from StreamVault's God-Object `MainActivity` via `StreamVaultNavViewModel`.
- [x] Wired StreamVault UI to TVMime's `AppDatabase` via custom `CategoryRepository` and `ChannelRepository` Proxy Adapters. 
- [x] Routed TVMime's `TvNavigation.kt` to boot into `AppNavigation()` upon successful Firebase Authentication.
- [x] **Sprint 19: Player Execution:** Bridged StreamVault's `@MainPlayerEngine` to TVMime's `EngineController`.
- [x] **Sprint 19: EPG Data:** Bridged StreamVault's `EpgRepository` to read directly from TVMime's `EpgDao`.
- [x] Fixed all `build.gradle.kts` errors and completely stripped the dead `dev.jdtech.mpv:libmpv` JitPack dependency to unblock the compilation pipeline.
- [x] Bridged all missing StreamVault Android Resources (`ic_launcher_vault_art`, etc) that were breaking AAPT2 packaging.


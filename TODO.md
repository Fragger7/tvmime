# TVMime Master TODO 🎯

## Active Directives
- [x] **Sprint 20: The Hilt Purge** (Execute the Phoenix Protocol: eradicate all Dagger Hilt plugins, dependencies, `@AndroidEntryPoint` and `@HiltAndroidApp` annotations. Delete `MockDomainModule.kt` and all proxy `Stubs.kt`).
- [x] **Sprint 21: Stateless UI Shell** (Create `LiveTvGrid.kt` as a purely stateless Jetpack Compose UI component. Strip out all UI backend dependencies).
- [x] **Sprint 22: The Orchestrator** (Build `LiveTvViewModel.kt` and `LiveTvRoute.kt` using standard `ViewModelProvider.Factory`. Inject `AppDatabase` manually. Collect `StateFlow` and pass it to `LiveTvGrid`).
- [x] **Sprint 23: The Player MVP** (Create `PlayerRoute.kt` and `PlayerOverlay.kt`. Wire it to `EngineController` to start `LivePreviewEngine` on channel click. Purge all remaining StreamVault domain/ui packages to fix unresolved references).
- [x] **Verify V3 Architecture** (Push to CI/CD and confirm a successful `.apk` build!).

## Next Steps (Version 3.1)
- [ ] **Sprint 24: Direct M3U Input UI** (Build a UI screen in `OnboardingScreen` for users to manually type in an M3U url and trigger the `SyncManagerM3uImporter`, rather than hardcoding it in the ViewModel).
- [ ] **Sprint 25: Settings & Customization** (Rebuild a stateless Settings screen to allow users to clear database caches and log out).
